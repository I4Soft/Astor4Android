package br.ufg.inf.astor4android.faultlocalization;

import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;

import br.ufg.inf.astor4android.worker.enums.TestType;
import br.ufg.inf.astor4android.worker.WorkerFacade;
import br.ufg.inf.astorworker.faultlocalization.entities.Line;

public class SuspiciousSearcher implements Runnable {
	private TestType type;
	private String testCase;
	private boolean passing;
	private static Logger logger = Logger.getLogger(SuspiciousSearcher.class.getName());

	public SuspiciousSearcher(String testCase, TestType type, boolean passing) {
		this.type = type;
		this.testCase = testCase;
		this.passing = passing;
	}

	@Override
	public void run() {
		try{
			WorkerFacade workerFacade = new WorkerFacade();
			List<Line> suspiciousLines = workerFacade.searchSuspicious(testCase, type, passing);

			if(suspiciousLines == null || suspiciousLines.isEmpty())
				logger.info("Worker " + workerFacade.getLastWorkerID() + " did not return any suspicious line for " + testCase);
			else logger.info("Worker " + workerFacade.getLastWorkerID() + " returned suspicious lines for " + testCase);
			
			FaultLocalizationManager.addSuspiciousLines(suspiciousLines);
		} catch (Exception e){
			logger.info("Unexpected error while searching for suspicious line on " + testCase + ": " + e.getMessage());
			e.printStackTrace();
		}
	}
}