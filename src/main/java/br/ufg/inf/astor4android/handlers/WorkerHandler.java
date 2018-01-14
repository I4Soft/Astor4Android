package br.ufg.inf.astor4android.handlers;

import java.net.Socket;
import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.BufferedWriter;
import java.io.FileWriter;


import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.collections4.map.AbstractHashedMap;
import org.apache.commons.collections4.MapIterator;
import org.apache.log4j.Logger;

import fr.inria.astor.core.setup.ConfigurationProperties;
import fr.inria.astor.core.faultlocalization.entity.SuspiciousCode;
import br.ufg.inf.astorworker.faultlocalization.entities.Line;
import br.ufg.inf.astor4android.handlers.entities.Worker;
import br.ufg.inf.astor4android.handlers.entities.TestType;
import br.ufg.inf.astor4android.utils.FileSystemUtils;
import br.ufg.inf.astor4android.entities.AndroidProject;




/**
* WorkerHandler sets up and manages all the Workers.
* Uses the producer-consumer paradigm, as a Worker can only be used by one thread
* at a time.
*
* @author Kayque de Sousa Teixeira, kayque23@gmail.com
*
*/
public class WorkerHandler {
	
	private static File project;
	private static String projectName;
	private static BlockingQueue<Worker> workers;
	private static AbstractHashedMap<String, Line> faulty;
	private static AbstractHashedMap<TestType, List<String>> failingTests;
	private static AbstractHashedMap<TestType, List<String>> tests;
	private static AbstractHashedMap<String, String> classes;
	private static Logger logger = Logger.getLogger(WorkerHandler.class.getName());
	private static Pattern functionPattern; 

	static {
		workers = new LinkedBlockingQueue<Worker>(); 
		faulty = new HashedMap();
		failingTests = new HashedMap(2);
		classes = new HashedMap(2);
		tests = new HashedMap(2);
		functionPattern = Pattern.compile("\\s*(@\\w+\\s*)*\\s*((public|private|protected)\\s+)?(static\\s+)?([a-zA-Z_0-9<>\\[\\]]+)(\\s+)(\\w+)\\s*\\(.*?\\)\\s*(throws\\s*\\w+(\\s*,\\s*\\w+)*)?\\s*\\{?\\s*");
	}


	/**
	* Sets the initial information about the project under repair.
	*
	* @param pName  Name of the folder of the project
	* @param initialProject File object referring the clean copy of the project 
	*/
	public static void setProject(String pName, File initialProject){
		project = initialProject;
		projectName = pName;
	}
	


	/**
	* Creates a new worker and sends the project.
	*
	* @param socket Socket created when the AstorWorker connected
	* @see ConnectionHandler#run()
	* @see #ProjectSender
	*/
	public static void createNewWorker(Socket socket) throws IOException, InterruptedException {
		synchronized(WorkerHandler.class){
			Worker worker = new Worker();
			ProjectSender projectSender = new ProjectSender(worker, socket, projectName, project);
			projectSender.start();
		}
	}

	public static Worker getWorker() throws InterruptedException {
		synchronized(workers){
			while(workers.isEmpty())
				workers.wait();
			return workers.poll();
		}
	}	

	public static void putWorker(Worker w) throws InterruptedException {
		synchronized(workers){
			workers.put(w);
			if(workers.size() == 1)
				workers.notify();
		}
	}

	public static boolean hasWorkers(){
		return !workers.isEmpty();
	}

	public static void finishAllWorkers() throws IOException, InterruptedException {
		synchronized(workers){
			while(hasWorkers()){
				Worker worker = getWorker();
				worker.finish();
			}
		}
	}

	private static List<String> getFunctionNames(String file) throws Exception {
		BufferedReader br = new BufferedReader(new FileReader(file));
		ArrayList<String> functionNames = new ArrayList<String>();
		String line;

		while((line = br.readLine()) != null){
			Matcher m = functionPattern.matcher(line);
			if(m.matches())
				functionNames.add(m.group(7));
		}

		return functionNames;
	} 

	private static List<String> getInstrumentationTests(String projectLocation) throws Exception {
		List<String> testNames = null;
		File testFolder = null;
		try {
			testNames = new ArrayList<String>();
			testFolder = new File(projectLocation + "/" + AndroidProject.getInstance().getMainFolder() + "/src/androidTest/java/");

			if(testFolder.exists()){
				List<String> instrumentationTests = FileSystemUtils.findFilesWithExtension(testFolder, "java", true);
			
				for(String test : instrumentationTests){
					List<String> functionNames = getFunctionNames(test);
					String testName = test.split("." + AndroidProject.getInstance().getMainFolder() + ".src.androidTest.java.")[1].replaceAll("\\./|\\.\\\\","").split(".java")[0];

					for(String function : functionNames){
						logger.info("Instrumentation test: " + testName.replaceAll("/|\\\\", "\\.") + "#" + function);
						testNames.add(testName.replaceAll("/|\\\\", "\\.") + "#" + function);
					}
					
				}
			}
		} catch(IOException ex){
			logger.info("There are no instrumentation tests");
		}
		
		return testNames;
	}	

	private static List<String> getUnitTests(String projectLocation) throws Exception {
		List<String> testNames = null;
		File testFolder = null;

		try {
			testNames = new ArrayList<String>();
			testFolder = new File(projectLocation + "/" + AndroidProject.getInstance().getMainFolder() + "/src/test/java/");

			if(testFolder.exists()){
				List<String> unitTests = FileSystemUtils.findFilesWithExtension(testFolder, "java", true);
			
				for(String test: unitTests){
					List<String> functionNames = getFunctionNames(test);
					String testName = test.split("." + AndroidProject.getInstance().getMainFolder() + ".src.test.java.")[1].replaceAll("\\./|\\.\\\\","").split(".java")[0];

					for(String function : functionNames){
						logger.info("Unit test: " + testName.replaceAll("/|\\\\", "\\.") + "#" + function);
						testNames.add(testName.replaceAll("/|\\\\", "\\.") + "#" + function);
					}
				}
			}	
		} catch(IOException ex){
			logger.info("There are no unit tests");
		}
		
		return testNames;
	}

	public static List<SuspiciousCode> runFaultLocalization() throws Exception {
		//Getting name of all classes

		List<String> output = FileSystemUtils.findFilesWithExtension(new File(project.getAbsolutePath() + "/" + AndroidProject.getInstance().getMainFolder() + "/src/main/java/"), "java", true);
		for(String clss : output){
			clss = clss.split("." + AndroidProject.getInstance().getMainFolder() + ".src.main.java.")[1].replaceAll("\\./|\\.\\\\", "").split(".java")[0].replaceAll("/|\\\\", "\\.");
			String[] tokens = clss.split("\\.");
			classes.put(tokens[tokens.length-1], clss);
			logger.info("["+tokens[tokens.length-1]+" , "+clss+"]");
		}

		tests.put(TestType.UNIT, getUnitTests(project.getAbsolutePath()));
		tests.put(TestType.INSTRUMENTATION, getInstrumentationTests(project.getAbsolutePath()));

		List<FaultLocalizationExecutor> threads = new ArrayList<>();
		

		for(TestType type : TestType.values()){

			List<String> testList = tests.get(type);
			
			if(testList == null) 
				continue;

			List<String> failingTestList = failingTests.get(type);

			for(String test : testList){
				Worker worker = getWorker();
				boolean passing = failingTestList == null || !failingTestList.contains(test);
				FaultLocalizationExecutor fle = new FaultLocalizationExecutor(worker, type, test, passing);
				fle.start();
				threads.add(fle);
			}	
		}
		

		for(FaultLocalizationExecutor fle : threads)
			fle.join();

		List<String> unitFailing = failingTests.get(TestType.UNIT);
		List<String> instrumentationFailing = failingTests.get(TestType.INSTRUMENTATION);

		List<SuspiciousCode> candidates = new ArrayList<>();
		BufferedWriter out = new BufferedWriter(new FileWriter(ConfigurationProperties.getProperty("projectWorkingDirectory") + "/faultlocalization.log"));

		MapIterator it = faulty.mapIterator();
		while (it.hasNext()) {
			it.next();
			Line line = (Line) it.getValue();

			if(instrumentationFailing != null){
				for(String test : instrumentationFailing)
					if(!line.wasHitBy(test))
						line.incrementFailingNotExecuted(); 
			}
			 
			if(unitFailing != null){
				for(String test : unitFailing) 				
					if(!line.wasHitBy(test))
						line.incrementFailingNotExecuted();
			}

			logger.info(line.toString());
			out.write(line.toString());
			out.newLine();

			candidates.add(new SuspiciousCode(classes.get(line.getClassName()), null, line.getNumber(), line.getSuspiciousValue(), null));
		}
		out.close();

		synchronized(workers){
			for(Worker worker : workers)
				worker.finishFaultLocalization();
		}

 		return candidates;
	}



	private static class ProjectSender extends Thread {
		private Worker worker;
		private File project;
		private String projectName;
		private Socket infoSocket;

		ProjectSender(Worker worker, Socket socket, String projectName, File project){
			this.worker = worker;
			this.infoSocket = socket;
			this.project = project;
			this.projectName = projectName;
		}

		@Override
		public void run(){
			try{
				worker.setupWorkerConnection(infoSocket);
				worker.sendProjectName(projectName);
				worker.sendProject(project);
				worker.sendFailingTests(TestType.INSTRUMENTATION, ConfigurationProperties.getProperty("instrumentationfailing"));
				worker.sendFailingTests(TestType.UNIT, ConfigurationProperties.getProperty("unitfailing"));
				WorkerHandler.putWorker(worker);
			} catch (InterruptedException | IOException e){
				e.printStackTrace(); 
			}
		}

	}

	private static class FaultLocalizationExecutor extends Thread {
		private Worker worker;
		private TestType type;
		private String test;
		private boolean passing;

		FaultLocalizationExecutor(Worker worker, TestType type, String test,  boolean passing){
			this.worker = worker;
			this.type = type;
			this.test = test;
			this.passing = passing;
		}

		@Override
		public void run(){
			try{
				List<Line> candidates = worker.searchSuspicious(test, type, passing);
				List<String> failingTestList = failingTests.get(type);

				synchronized(faulty){
					for(Line line : candidates){
						logger.info("Line received "+line.getClassName()+":"+line.getNumber());
						Line existingLine = faulty.get(line.getClassName()+":"+line.getNumber());
					
						if(existingLine == null)
							faulty.put(line.getClassName()+":"+line.getNumber(), line);
					
						else {
							List<String> testList = line.getTestList();
							for(String test : testList)
								existingLine.addTest(test, failingTestList == null || !failingTestList.contains(test));
						}
					}
				}

				WorkerHandler.putWorker(worker);
				logger.info(Thread.currentThread().getName()+" returned the worker "+worker.toString()+" to the queue");
				
			} catch (InterruptedException | IOException | ClassNotFoundException e){
				e.printStackTrace(); 
			}
		}

	}

	public static void setInstrumentationFailing(String failing){
		failingTests.put(TestType.INSTRUMENTATION, Arrays.asList(failing.split(":")));
	}	

	public static void setUnitFailing(String failing){
		failingTests.put(TestType.UNIT, Arrays.asList(failing.split(":")));
	}

}