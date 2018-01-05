package br.ufg.inf.main.evolution;

import java.io.File;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.UnrecognizedOptionException;


import br.ufg.inf.handlers.WorkerHandler;
import br.ufg.inf.handlers.ConnectionHandler;
import br.ufg.inf.executors.CommandExecutorProcess;
import br.ufg.inf.executors.AndroidToolsExecutorProcess;
import fr.inria.main.AbstractMain;
import fr.inria.main.evolution.AstorMain;
import fr.inria.astor.core.entities.ProgramVariant;
import fr.inria.astor.core.setup.ConfigurationProperties;
import fr.inria.astor.core.setup.ProjectRepairFacade;
import fr.inria.astor.core.setup.ProjectConfiguration;
import br.inf.ufg.astor4android.utils.FileSystemUtils;

/**
 * The main class of the system.
 * 
 * @author Kayque de Sousa Teixeira, kayque23@gmail.com
 *
 */
public class Astor4AndroidMain extends AstorMain {

	protected Logger log = Logger.getLogger(Astor4AndroidMain.class.getName());
	private final int DEFAULT_PORT = 6665;

	CommandLineParser parser = new BasicParser();

	static {
		options.addOption("androidsdk", true, "Android SDK location");

		options.addOption("androidjar", true, "android.jar location from Android SDK");
		
		options.addOption("port", true, "Port that the workers will connected on");

		options.addOption("unitfailing", true,
				"failing unit test cases, separated by Path separator char (: in linux/mac  and ; in windows)");

		options.addOption("instrumentationfailing", true,
				"failing instrumentation test cases, separated by Path separator char (: in linux/mac  and ; in windows)");
	}
	

	@Override
	public void initProject(String location, String projectName, String dependencies, String packageToInstrument,
			double thfl, String failing) throws Exception {

		String method = this.getClass().getSimpleName();

		if (projectName == null || projectName.equals("")) {
			File locFile = new File(location);
			projectName = locFile.getName();
		}

		createWorkingDirectory(projectName, method);

		//Creating clean copy of the project for the workers
		log.info("Project name: " + projectName);
		String projectCopy = createProjectCopy(location, "", method, projectName);
		String cleanCopy = createProjectCopy(location, "clean", method, projectName);

		setupHandlers(projectName, new File(cleanCopy));

		dependencies = findDependencies(projectCopy);

		log.info("Dependencies: " + dependencies);

		projectFacade = getProject(projectCopy, projectName, method, null, dependencies, true);
		projectFacade.getProperties().setExperimentName(this.getClass().getSimpleName());

		projectFacade.setupWorkingDirectories(ProgramVariant.DEFAULT_ORIGINAL_VARIANT);

		findMainPackage(projectFacade);
	}


	/**
	* Opens AndroidManifest.xml and search for the main package of the project.
	*
	* @param projectFacade
	*/
	private void findMainPackage(ProjectRepairFacade projectFacade) throws Exception {
		Pattern packagePattern = Pattern.compile("\\s*(package)\\s*(=)\\s*(\'|\")([ .a-zA-Z0-9]+)(\'|\")\\s*(.*?)\\s*");

		BufferedReader br = new BufferedReader(
				new FileReader(new File(projectFacade.getProperties().getOriginalProjectRootDir() + "/app/src/main/AndroidManifest.xml")));
		
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

		ConfigurationProperties.properties.setProperty("package", mainPackage);
		log.info("Main package: "+mainPackage);
	}

	@Override
	protected ProjectRepairFacade getProject(String location, String projectIdentifier, String method,
			List<String> failingTestCases, String dependencies, boolean srcWithMain) throws Exception {

		if (projectIdentifier == null || projectIdentifier.equals("")) {
			File locFile = new File(location);
			projectIdentifier = locFile.getName();
		}

		String key = "/" + method + "-" + projectIdentifier + "/";
		String workingDirForSource = ConfigurationProperties.getProperty("workingDirectory") + key + "/src/";
		String workingDirForBytecode = ConfigurationProperties.getProperty("workingDirectory") + key + "/bin/";
		String originalProjectRoot = location + "/";
		
	
		ProjectConfiguration properties = new ProjectConfiguration();
		properties.setWorkingDirForSource(FileSystemUtils.fixPath(workingDirForSource));
		properties.setWorkingDirForBytecode(FileSystemUtils.fixPath(workingDirForBytecode));

		properties.setFixid(projectIdentifier);

		properties.setOriginalProjectRootDir(FileSystemUtils.fixPath(originalProjectRoot));
		
		List<String> src = Arrays.asList(new String[] { FileSystemUtils.fixPath("/app/src/main/java/"), null });

		properties.setOriginalDirSrc(src);

		if (dependencies != null) {
			properties.setDependencies(dependencies);
		}

		properties.setPackageToInstrument(ConfigurationProperties.getProperty("packageToInstrument"));
		
		//properties.setDataFolder(ConfigurationProperties.getProperty("resourcesfolder"));

		ProjectRepairFacade ce = new ProjectRepairFacade(properties);

		return ce;
	}


	@Override
	public boolean processArguments(String[] args) throws Exception {
		super.processArguments(args);

		CommandLine cmd = null;
		try {
			cmd = parser.parse(options, args);
		} catch (UnrecognizedOptionException e) {
			System.out.println("Error: " + e.getMessage());
			help();
			return false;
		}
		if (cmd.hasOption("help")) {
			help();
			return false;
		}

		
		String unitFailing = cmd.getOptionValue("unitfailing");
		String instrumentationFailing = cmd.getOptionValue("instrumentationfailing");
		String location = cmd.getOptionValue("location");
		String port = cmd.getOptionValue("port");


		// Process mandatory parameters.
		if ((unitFailing == null && instrumentationFailing == null) || location == null) {
			help();
			return false;
		}

		if(unitFailing != null) {
			ConfigurationProperties.properties.setProperty("unitfailing", unitFailing);
			WorkerHandler.setUnitFailing(unitFailing);
		}
		if(instrumentationFailing != null) {
			ConfigurationProperties.properties.setProperty("instrumentationfailing", instrumentationFailing);
			WorkerHandler.setInstrumentationFailing(instrumentationFailing);
		}
		
		ConfigurationProperties.properties.setProperty("location", location);
		if(port == null)
			ConfigurationProperties.properties.setProperty("port", String.valueOf(DEFAULT_PORT));
		else
			ConfigurationProperties.properties.setProperty("port", port);
		

		if (cmd.hasOption("androidjar"))
			ConfigurationProperties.properties.setProperty("androidjar", cmd.getOptionValue("androidjar"));	

		if (cmd.hasOption("androidsdk"))
			ConfigurationProperties.properties.setProperty("androidsdk", cmd.getOptionValue("androidsdk"));	

		if (cmd.hasOption("package"))
			ConfigurationProperties.properties.setProperty("package", cmd.getOptionValue("package"));

		return true;
	}



	private static void help() {

		HelpFormatter formater = new HelpFormatter();
		formater.printHelp("Main", options);
		System.out.println("More options and default values at 'configuration.properties' file");

		System.exit(0);

	}

	/**
	* Stores a copy of the project at the working directory. 
	* This copy will be used to build and find dependencies.
	*
	* @param location Location of the project
	* @param method The name of this class
	* @param projectName Name of the folder of the project
	* @return Location of the copy
	*/
	private String createProjectCopy(String location, String workingDirSubLocation, String method, String projectName) 
			throws Exception {
		String key = method + "-" + projectName;
		String copy = ConfigurationProperties.getProperty("workingDirectory") + "/" + key + "/" + workingDirSubLocation + "/" + projectName;
		File projectCopy = new File(copy);
		projectCopy.mkdirs();
		FileUtils.copyDirectory(new File(location), projectCopy);
		if(!AndroidToolsExecutorProcess.getOperatingSystem().equals("Windows"))
			FileSystemUtils.getPermissionsForDirectory(projectCopy);
		return copy;
	}


	private void createWorkingDirectory(String projectName, String method) throws Exception {
		String oldDir = method + "-" + projectName;
		String oldWorkingDir = ConfigurationProperties.getProperty("workingDirectory") + File.separator + oldDir;
		File tmp = new File(oldWorkingDir);
		FileUtils.deleteDirectory(tmp);
		tmp.mkdirs();
	}

	private void saveDependenciesLocally(String location) throws Exception {
		String repositoryFormat = "\n\tmaven {\n\t\turl '%s'\n\t}\n";

		List<String> m2repositories = Arrays.asList(new String[] { 
				ConfigurationProperties.getProperty("androidsdk") + FileSystemUtils.fixPath("/extras/android/m2repository/")
			  , ConfigurationProperties.getProperty("androidsdk") + FileSystemUtils.fixPath("/extras/google/m2repository/") });

		BufferedWriter out = new BufferedWriter(new FileWriter(new File(location + "/app/build.gradle"), true));

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

   		AndroidToolsExecutorProcess.runGradleTask(location, "saveDependencies");

   		extractAAR(location + "/app/localrepo");
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


	/**
	* Finds the dependencies of the project.
	* This is done by reading the output of the command "./gradlew build".
	*
	* @param location Location of the project
	*/
	private String findDependencies(String location) throws Exception {
		saveDependenciesLocally(location);

		String dependencies = "";
		List<String> output = FileSystemUtils.findFilesWithExtension(new File(location), "jar", true);

		for(String entry : output)
			dependencies += entry + System.getProperty("path.separator");

		AndroidToolsExecutorProcess.compileProject(location);

		output = FileSystemUtils.listContentsDirectory(location + "/app/build/intermediates/classes/");

		for(String entry : output){
			if(entry.equals("debug"))
				dependencies += FileSystemUtils.fixPath(location + "/app/build/intermediates/classes/debug/") + System.getProperty("path.separator");

			else if(!entry.equals("release")){
				dependencies += FileSystemUtils.fixPath(location + "/app/build/intermediates/classes/" + entry + "/debug/") + System.getProperty("path.separator");
			}
		}

		return dependencies + ConfigurationProperties.getProperty("androidjar");
	}


	/**
	* Sets up WorkerHandler and ConnectionHandler.
	* WorkerHandler will start sending the project to all connected instances of AstorWorker.
	* ConnectionHandler will start receiving new connections from instances of AstorWorker.
	*
	* @param projectName Name of the folder of the project
	* @param cleanProject File object referring the clean copy of the project 
	* @see WorkerHander#setProject(String, File)
	* @see ConnectionHandler#Constructor(int)
	*/
	private void setupHandlers(String projectName, File cleanProject) throws Exception {
		WorkerHandler.setProject(projectName, cleanProject);
		ConnectionHandler connectionHandler = new ConnectionHandler(Integer.parseInt(ConfigurationProperties.getProperty("port")));
		connectionHandler.start();
	}

	public static void main(String[] args) throws Exception {
		Astor4AndroidMain m = new Astor4AndroidMain();
		AndroidToolsExecutorProcess.setup(ConfigurationProperties.getProperty("androidsdk"));
		m.execute(args);
		System.exit(0);
	}
}
