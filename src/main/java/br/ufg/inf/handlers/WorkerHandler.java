package br.ufg.inf.handlers;

import java.net.Socket;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.collections4.map.AbstractHashedMap;
import org.apache.commons.collections4.MapIterator;
import org.apache.log4j.Logger;

import br.ufg.inf.handlers.entities.Worker;
import br.ufg.inf.handlers.entities.TestType;
import br.ufg.inf.astorworker.faultlocalization.entities.Line;
import br.ufg.inf.executors.CommandExecutorProcess;
import fr.inria.astor.core.faultlocalization.entity.SuspiciousCode;




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

	static {
		workers = new LinkedBlockingQueue<Worker>(); 
		faulty = new HashedMap();
		failingTests = new HashedMap(2);
		classes = new HashedMap(2);
		tests = new HashedMap(2);
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
			putWorker(worker);
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

	private static List<String> getInstrumentationTests(String projectLocation) throws IOException, InterruptedException {
		List<String> instrumentationTests = CommandExecutorProcess.execute("find . -name *.java", projectLocation+"/app/src/androidTest/java/");
		List<String> testNames = new ArrayList<String>();
		
		for(String test: instrumentationTests){
			String testName = test.replaceAll("\\./","").split(".java")[0];
			logger.info("Itest:"+testName.replaceAll("/", "."));
			testNames.add(testName.replaceAll("/", "."));
		}
		
		return testNames;
	}	

	private static List<String> getUnitTests(String projectLocation) throws IOException, InterruptedException {
		List<String> unitTests = CommandExecutorProcess.execute("find . -name *.java", projectLocation+"/app/src/test/java/");
		List<String> testNames = new ArrayList<String>();
		
		for(String test: unitTests){
			String testName = test.replaceAll("\\./","").split(".java")[0];
			logger.info("utest:"+testName.replaceAll("/", "."));
			testNames.add(testName.replaceAll("/", "."));
		}
		
		return testNames;
	}

	public static List<SuspiciousCode> runFaultLocalization() throws Exception {
		synchronized(workers){
			for(Worker worker : workers)
				worker.setupFaultLocalization();
		}

		//Getting name of all classes
		List<String> output = CommandExecutorProcess.execute("find . -name *.java", project.getAbsolutePath()+"/app/src/main/java/");
		for(String clss : output){
			clss = clss.replaceAll("\\./", "").split(".java")[0].replaceAll("/", ".");
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

			candidates.add(new SuspiciousCode(classes.get(line.getClassName()), null, line.getNumber(), line.getSuspiciousValue(), null));
		}

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