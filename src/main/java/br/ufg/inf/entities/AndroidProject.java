package br.ufg.inf.entities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import br.ufg.inf.executors.CommandExecutorProcess;
import br.ufg.inf.executors.AndroidToolsExecutorProcess;
import fr.inria.astor.core.setup.ConfigurationProperties;
import br.ufg.inf.utils.FileSystemUtils;

public class AndroidProject {
	private static AndroidProject instance = null;
	private String mainFolder;
	private String mainPackage;
	private String testPackage;
	private String flavor;
	private File projectDirectory;
	private String projectName;
	private String dependencies;
	private String projectAbsolutePath;
	private String unitTestTask;
	private String instrumentationTestTask;
	private String buildVersion;
	private String compileVersion;
	private List<String> failingInstrumentationTestCases;
	private List<String> failingUnitTestCases;
	private boolean unitRegressionTestCasesExist;
	private boolean instrumentationRegressionTestCasesExist;
	private Logger logger = Logger.getLogger(AndroidProject.class);

	private Pattern unitTaskPattern = Pattern.compile("\\s*(test)([a-zA-Z0-9]+)(unittest)\\s-\\s(.*?)\\s*");
	private Pattern instrumentationTaskPattern = Pattern.compile("\\s*(connected)(androidtest[a-zA-Z0-9]+|[a-zA-Z0-9]+androidtest)\\s-\\s(.*?)\\s*");

	private AndroidProject() {}

	public static AndroidProject getInstance() {
		if(instance == null)
			instance = new AndroidProject();
		
		return instance;
	}
	
	public void setup(File projectDirectory) throws Exception {
		this.projectDirectory = projectDirectory;
		logger.info("Getting project information");

		if(!AndroidToolsExecutorProcess.getOperatingSystem().equals("Windows"))
			FileSystemUtils.getPermissionsForDirectory(projectDirectory);
		projectAbsolutePath = projectDirectory.getAbsolutePath();
		
		projectName = projectDirectory.getName();
		logger.info("Project name: " + projectName);

		mainFolder = findMainFolder();
		logger.info("Main folder: " + mainFolder);

		mainPackage = findMainPackage();
		logger.info("Main package: " + mainPackage);

		testPackage = findTestPackage();
		logger.info("Test package: " + testPackage);

		buildVersion = findBuildVersion();
		logger.info("Build tools version: " + buildVersion);

		compileVersion = findCompileVersion();
		logger.info("Compile version: " + compileVersion);
		

		findDependencies();	
	}


	private void extractAAR(String libLocation) throws Exception {
		List<String> output = FileSystemUtils.findFilesWithExtension(new File(libLocation), "aar", false);

		for(String aar : output){
			String aarFolder = aar.split(".aar")[0];
			File aarDirectory = new File(FileSystemUtils.fixPath(libLocation + "/" + aarFolder));
			FileUtils.moveFileToDirectory(new File(FileSystemUtils.fixPath(libLocation + "/" + aar)), aarDirectory, true);
			CommandExecutorProcess.execute("jar xf " + aar, FileSystemUtils.fixPath(libLocation + "/" + aarFolder));
			File jarsDirectory = new File(FileSystemUtils.fixPath(libLocation + "/" + aarFolder + "/" + "jars"));
			FileUtils.moveFileToDirectory(new File(FileSystemUtils.fixPath(libLocation + "/" + aarFolder + "/classes.jar")), jarsDirectory, true);
			FileUtils.forceDelete(new File(FileSystemUtils.fixPath( libLocation + "/" + aarFolder + "/" + aar)));
		}
	}

	private void saveDependenciesLocally() throws Exception {
		String repositoryFormat = "\n\tmaven {\n\t\turl '%s'\n\t}\n";

		List<String> m2repositories = Arrays.asList(new String[] { 
				ConfigurationProperties.getProperty("androidsdk") + FileSystemUtils.fixPath("/extras/android/m2repository/")
			  , ConfigurationProperties.getProperty("androidsdk") + FileSystemUtils.fixPath("/extras/google/m2repository/") });

		BufferedWriter out = new BufferedWriter(new FileWriter(new File(projectAbsolutePath + "/" + mainFolder + "/build.gradle"), true));

		out.write("\n\nrepositories {");
		for(String repository : m2repositories)
			out.write(String.format(repositoryFormat, repository.replace("\\", "\\\\")));
		out.write("\n\tmavenLocal()\n}\n\n");
		

		BufferedReader in = new BufferedReader(new FileReader("save.gradle"));
		String line;
		while ((line = in.readLine()) != null) 
            out.write("\n" + line);

        in.close();
   		out.close();

   		AndroidToolsExecutorProcess.runGradleTask(projectAbsolutePath, "saveDependencies");

   		extractAAR(projectAbsolutePath + "/" + mainFolder + "/localrepo");
	}

	private void findDependencies() throws Exception {
		try {
			logger.info("Finding dependencies");
			saveDependenciesLocally();

			dependencies = "";
			List<String> output = FileSystemUtils.findFilesWithExtension(new File(projectAbsolutePath), "jar", true);

			for(String entry : output)
				dependencies += entry + System.getProperty("path.separator");

			AndroidToolsExecutorProcess.compileProject(projectAbsolutePath);

			output = FileSystemUtils.listContentsDirectory(new File(projectAbsolutePath + "/" + mainFolder + "/build/intermediates/classes/"));

			for(String entry : output){
				if(entry.equals("debug"))
					dependencies += FileSystemUtils.fixPath(projectAbsolutePath + "/" + mainFolder + "/build/intermediates/classes/debug/") + System.getProperty("path.separator");

				else if(!entry.equals("release")){
					dependencies += FileSystemUtils.fixPath(projectAbsolutePath + "/" + mainFolder + "/build/intermediates/classes/" + entry + "/debug/") + System.getProperty("path.separator");
				}
			}

			dependencies += ConfigurationProperties.getProperty("androidjar");
		} catch(FileNotFoundException ex) {
			logger.error("A file could not be found: " + ex.getMessage());
			ex.printStackTrace();
			System.exit(1);
		}
	}



	private String findBuildVersion() throws Exception {
		Pattern buildVersionPattern = Pattern.compile("\\s*(buildtoolsversion)\\s*(\'|\")([ .0-9]+)(\'|\")\\s*");
		BufferedReader br = new BufferedReader(new FileReader(new File(projectAbsolutePath + "/" + mainFolder + "/build.gradle")));

		String line = null;
		String buildToolsVersion = null ;

		while((line = br.readLine()) != null){
			Matcher buildVersionMatcher = buildVersionPattern.matcher(line.toLowerCase());

			if (buildVersionMatcher.matches()) {
				buildToolsVersion = buildVersionMatcher.group(3);
				break;
			}
		}
		
		br.close();
		return buildToolsVersion;
	}

	private String findCompileVersion() throws Exception {
		Pattern compileVersionPattern = Pattern.compile("\\s*(compilesdkversion)\\s*([0-9]+)\\s*");
		BufferedReader br = new BufferedReader(new FileReader(new File(projectAbsolutePath + "/" + mainFolder + "/build.gradle")));

		String line = null;
		String compileVersion = null ;

		while((line = br.readLine()) != null){
			Matcher compileVersionMatcher = compileVersionPattern.matcher(line.toLowerCase());

			if (compileVersionMatcher.matches()) {
				compileVersion = compileVersionMatcher.group(2);
				break;
			}
		}
		
		br.close();
		return compileVersion;
	}

	private String findTestPackage() throws Exception {
		Pattern testPackagePattern = Pattern.compile("\\s*(testApplicationId)\\s*(\'|\")([ .a-zA-Z0-9]+)(\'|\")\\s*");

		BufferedReader br = new BufferedReader(new FileReader(new File(projectAbsolutePath + "/" + mainFolder + "/build.gradle")));
		String line = null;
		String testPackage = null;


		while((line = br.readLine()) != null){
			Matcher testPackageMatcher = testPackagePattern.matcher(line);

			if (testPackageMatcher.matches()) {
				testPackage = testPackageMatcher.group(3);
				break;
			}
		}

		if(testPackage == null)
			testPackage = this.mainPackage + ".test";

		br.close();
		return testPackage;
	}


	/**
	* Opens AndroidManifest.xml and search for the main package of the project.
	*
	* @param projectFacade
	*/
	private String findMainPackage() throws Exception {
		Pattern packagePattern = Pattern.compile("\\s*(package)\\s*(=)\\s*(\'|\")([ .a-zA-Z0-9]+)(\'|\")\\s*(.*?)\\s*");

		BufferedReader br = new BufferedReader(new FileReader(
				new File(projectAbsolutePath + "/" + mainFolder + "/src/main/AndroidManifest.xml")));

		String line = null;
		String mainPackage = null ;

		while((line = br.readLine()) != null){
			Matcher packageMatcher = packagePattern.matcher(line);

			if (packageMatcher.matches()) {
				mainPackage = packageMatcher.group(4);
				break;
			}
		}
		
		br.close();
		return mainPackage;
	}

	private String findMainFolder() throws Exception {
		List<String> output = AndroidToolsExecutorProcess.runGradleTask(projectAbsolutePath, "-q projects");
		//Pattern p = Pattern.compile("include\\s*\\'\\:([a-zA-Z0-9]+)\\'\\s*\\,?+(.*?)\\s*");
		Pattern p = Pattern.compile("\\s*.---\\s*Project\\s*\\':(\\[-a-zA-Z_0-9]+)\\'\\s*");

		for(String line : output){
			Matcher m = p.matcher(line);
			if(m.matches())
				return m.group(1);
		}

		return "app";
	}

	public boolean instrumentationTestsExist() throws Exception {
		if(!(new File(projectAbsolutePath + "/" + mainFolder + "/src/androidTest").exists()))
			return false;

		List<String> instrumentationTests = FileSystemUtils.findFilesWithExtension(
				new File(projectAbsolutePath + "/" + mainFolder + "/src/androidTest"), "java", true);

		return !instrumentationTests.isEmpty();
	}	

	public boolean unitTestsExist() throws Exception {
		if(!(new File(projectAbsolutePath + "/" + mainFolder + "/src/test").exists()))
			return false;

		List<String> unitTests = FileSystemUtils.findFilesWithExtension(
				new File(projectAbsolutePath + "/" + mainFolder + "/src/test"), "java", true);

		return !unitTests.isEmpty();
	}


	public void setFailingInstrumentationTestCases(String tests) {
		failingInstrumentationTestCases = Arrays.asList(tests.split(":"));
	}

	public void setFailingUnitTestCases(String tests) {
		failingUnitTestCases = Arrays.asList(tests.split(":"));
	}

	public String getProjectName(){
		return projectName;
	}

	public String getDependencies(){
		return dependencies;
	}

	public List<String> getFailingUnitTestCases(){
		return failingUnitTestCases;
	}

	public List<String> getFailingInstrumentationTestCases(){
		return failingInstrumentationTestCases;
	}

	public String getLocation() {
		return projectAbsolutePath;
	}

	public String getMainPackage() {
		return mainPackage;
	}

	public String getTestPackage() {
		return testPackage;
	}

	public String getMainFolder() {
		return mainFolder;
	}

	public String getCompileVersion() {
		return compileVersion;
	}

	public String getBuildVersion() {
		return buildVersion;
	}

	public boolean unitRegressionTestCasesExist(){
		return unitRegressionTestCasesExist;
	}

	public boolean instrumentationRegressionTestCasesExist(){
		return instrumentationRegressionTestCasesExist;
	}
}