package br.ufg.inf.astor4android.faultlocalization;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.ArrayList;

import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.collections4.map.AbstractHashedMap;
import org.apache.commons.collections4.MapIterator;
import org.apache.log4j.Logger;

import fr.inria.astor.core.setup.ConfigurationProperties;
import fr.inria.astor.core.faultlocalization.entity.SuspiciousCode;
import br.ufg.inf.astorworker.faultlocalization.entities.Line;
import br.ufg.inf.astor4android.worker.enums.TestType;
import br.ufg.inf.astor4android.worker.WorkerCache;
import br.ufg.inf.astor4android.entities.AndroidProject;
import br.ufg.inf.astor4android.faultlocalization.FaultLocalizationFormulaDelegate;

public class FaultLocalizationManager {
	private static BufferedWriter logWriter = null;
	private static List<Line> linesToSave = new ArrayList<>();
	private static AbstractHashedMap<String, Line> suspiciousLines = new HashedMap();
	private static List<Thread> searcherThreads = new ArrayList<>();
	private static Logger logger = Logger.getLogger(FaultLocalizationManager.class.getName());

	private static void writeToLogFile(Line line) throws Exception {
		if(logWriter== null)
			logWriter = new BufferedWriter(new FileWriter(ConfigurationProperties.getProperty("projectWorkingDirectory") + "/faultlocalization.log"));
		logWriter.write(line.toString());
		logWriter.newLine();
		logger.info(line.toString());
	}

	private static void saveLine(Line line) {
		linesToSave.add(line);
	}

	private static void saveLogFile() throws Exception {
		logWriter.close();
	}

	private static void createFaultLocalizationSaveFile() throws Exception {
		logger.info("Creating fault localization save file");
		FileOutputStream fos = new FileOutputStream(ConfigurationProperties.getProperty("projectWorkingDirectory") + "/" + AndroidProject.getInstance().getProjectName() + ".flsave");
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(linesToSave);
		oos.close();
	}

	private static List<Line> loadFaultLocalizationSaveFile() throws Exception {
		logger.info("Loading fault localization save file");
		FileInputStream fis = new FileInputStream(ConfigurationProperties.getProperty("loadflsave"));
		ObjectInputStream ois = new ObjectInputStream(fis);
		List<Line> lines = (List<Line>) ois.readObject();
		ois.close();
		return lines;
	}

	private static void createSuspiciousSearchers(List<String> testCases, TestType type) throws Exception {
		if(testCases == null) return;

		for(String testCase : testCases){
			Thread searcher = new Thread(new SuspiciousSearcher(testCase, type, !AndroidProject.getInstance().isFailingTestCase(testCase)));
			searcher.start();
			searcherThreads.add(searcher);
		}	
	}

	private static void waitForAllSuspiciousSearchers() throws Exception {
		for(Thread searcher : searcherThreads)
			searcher.join();
	}

	private static void updatePassingTestCasesList() {
		MapIterator it = suspiciousLines.mapIterator();
		while (it.hasNext()) {
			it.next();
			Line line = (Line) it.getValue();
			AndroidProject.getInstance().updatePassingTestCases(line.getTestList());
		}
	}

	private static Line updateLine(Line line) throws Exception {
		if(AndroidProject.getInstance().failingInstrumentationTestCasesExists()){
			for(String test : AndroidProject.getInstance().getFailingInstrumentationTestCases())
				if(!line.wasHitBy(test))
					line.incrementFailingNotExecuted(); 
		}

		if(AndroidProject.getInstance().failingUnitTestCasesExists()) {
			for(String test : AndroidProject.getInstance().getFailingUnitTestCases()) 				
				if(!line.wasHitBy(test))
					line.incrementFailingNotExecuted();
		}
		
		line.setTotalPassing(AndroidProject.getInstance().getNumberOfPassingTestCases());
		line.setSuspiciousValue(FaultLocalizationFormulaDelegate.applyFormula(line));
		writeToLogFile(line);
		saveLine(line);
		return line;
	}

	public static void addSuspiciousLines(List<Line> lines) throws Exception {
		synchronized(suspiciousLines){
			for(Line line : lines){
				logger.info("Line received: [" + line.getClassName() + " : " + line.getNumber() + "]");
				Line existingLine = suspiciousLines.get(line.getClassName() + ":" + line.getNumber());
			
				if(existingLine == null)
					suspiciousLines.put(line.getClassName() + ":" + line.getNumber(), line);
			
				else {
					List<String> testList = line.getTestList();
					for(String test : testList)
						existingLine.addTest(test, !AndroidProject.getInstance().isFailingTestCase(test));
				}
			}
		}
	}

	public static List<SuspiciousCode> runFaultLocalization() throws Exception {
		if(ConfigurationProperties.getProperty("loadflsave") != null) {
			
			List<Line> lines = loadFaultLocalizationSaveFile();

			List<SuspiciousCode> candidates = new ArrayList<>();
			for(Line line : lines) {
				line.setSuspiciousValue(FaultLocalizationFormulaDelegate.applyFormula(line));
				candidates.add(new SuspiciousCode(AndroidProject.getInstance().getFullyQualifiedClassName(line.getClassName()), null, line.getNumber(), line.getSuspiciousValue(), null));
				writeToLogFile(line);
			}
			saveLogFile();

			return candidates;
		}

		createSuspiciousSearchers(AndroidProject.getInstance().getUnitTestCases(), TestType.UNIT);
		createSuspiciousSearchers(AndroidProject.getInstance().getInstrumentationTestCases(), TestType.INSTRUMENTATION);
		waitForAllSuspiciousSearchers();
		updatePassingTestCasesList();

		List<SuspiciousCode> candidates = new ArrayList<>();

		MapIterator it = suspiciousLines.mapIterator();
		while (it.hasNext()) {
			it.next();
			Line line = (Line) it.getValue();
			line = updateLine(line);
			candidates.add(new SuspiciousCode(AndroidProject.getInstance().getFullyQualifiedClassName(line.getClassName()), 
					null, line.getNumber(), line.getSuspiciousValue(), null));
		}

		createFaultLocalizationSaveFile();
		saveLogFile();
		WorkerCache.sendMessageToAllWorkers("END_FAULT_LOCALIZATION");
		return candidates;
	}
}