package br.ufg.inf.main.evolution;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Arrays;
import java.util.List;

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
import fr.inria.main.AbstractMain;
import fr.inria.main.evolution.AstorMain;
import fr.inria.astor.core.entities.ProgramVariant;
import fr.inria.astor.core.setup.ConfigurationProperties;
import fr.inria.astor.core.setup.ProjectRepairFacade;
import fr.inria.astor.core.setup.ProjectConfiguration;

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

		//Creating clean copy of the project for the workers
		log.info("Project name: "+projectName);
		File cleanProject = createCleanCopy(location, method, projectName);

		setupHandlers(projectName, cleanProject);

		dependencies = findDependencies(location);

		log.info("Dependencies: "+dependencies);

		projectFacade = getProject(location, projectName, method, null, dependencies, true);
		projectFacade.getProperties().setExperimentName(this.getClass().getSimpleName());

		projectFacade.setupWorkingDirectories(ProgramVariant.DEFAULT_ORIGINAL_VARIANT);

		findMainPackage(projectFacade);

		copyRJava(projectFacade);
	}

	/**
	* Copy R.java from the project to the original variant.
	*
	* @param projectFacade
	*/
	private void copyRJava(ProjectRepairFacade projectFacade) throws Exception {
		File destination = new File(projectFacade.getProperties().getWorkingDirForSource() + File.separator 
				+ ProgramVariant.DEFAULT_ORIGINAL_VARIANT);
		FileUtils.copyFile(
			new File(projectFacade.getProperties().getOriginalProjectRootDir() +
				"/app/build/generated/source/r/release/" +
				ConfigurationProperties.getProperty("package").replaceAll("\\.","/") +
				"/R.java"), 
			new File(destination.getPath()+"/"+ConfigurationProperties.getProperty("package").replaceAll("\\.","/")+"/R.java"));
	}


	/**
	* Opens AndroidManifest.xml and search for the main package of the project.
	*
	* @param projectFacade
	*/
	private void findMainPackage(ProjectRepairFacade projectFacade) throws Exception {
		BufferedReader br = new BufferedReader(new FileReader(new File(projectFacade.getProperties().getOriginalProjectRootDir() + "/app/src/main/AndroidManifest.xml")));
		String line = null;
		String mainPackage = null;

		while((line = br.readLine()) != null){
			if(line.contains("package"))
				mainPackage = line.split("\"")[1];
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

		String key = File.separator + method + "-" + projectIdentifier + File.separator;
		String workingDirForSource = ConfigurationProperties.getProperty("workingDirectory") + key + "/src/";
		String workingDirForBytecode = ConfigurationProperties.getProperty("workingDirectory") + key + "/bin/";
		String originalProjectRoot = location + File.separator;
		
		String libdir = dependencies;

	
		ProjectConfiguration properties = new ProjectConfiguration();
		properties.setWorkingDirForSource(workingDirForSource);
		properties.setWorkingDirForBytecode(workingDirForBytecode);

		properties.setFixid(projectIdentifier);

		properties.setOriginalProjectRootDir(originalProjectRoot);
		
		List<String> src = Arrays.asList(new String[] { "/app/src/main/java/", null });

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
	* Stores a clean copy of the project at the working directory. 
	* This clean copy will be sent to the Workers.
	*
	* @param location Location of the project
	* @param method The name of this class
	* @param projectName Name of the folder of the project
	*/
	private File createCleanCopy(String location, String method, String projectName) throws Exception {
		String key = File.separator + method + "-" + projectName + File.separator;
		String cleanCopy = ConfigurationProperties.getProperty("workingDirectory") + key + "/"+ projectName;
		File cleanProject = new File(cleanCopy);
		cleanProject.mkdirs();
		FileUtils.copyDirectory(new File(location), cleanProject);
		return cleanProject;
	}


	/**
	* Finds the dependencies of the project.
	* This is done by reading the output of the command "./gradlew build".
	*
	* @param location Location of the project
	*/
	private String findDependencies(String location) throws Exception {
		CommandExecutorProcess.execute("./gradlew build",location);

		String dependencies = "";
		List<String> output = CommandExecutorProcess.execute("find . -type f -name *.jar",location+"/app/build/intermediates/");

		for(String entry : output)
			dependencies += location + "/app/build/intermediates/" + entry + ":";

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
		m.execute(args);
		System.exit(0);
	}
}