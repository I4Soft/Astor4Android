package br.ufg.inf.astor4android.worker;

import java.util.List;
import java.net.ConnectException;
import java.io.File;

import org.apache.log4j.Logger;

import br.ufg.inf.astor4android.worker.enums.TestType;
import br.ufg.inf.astor4android.worker.entities.Worker;
import br.ufg.inf.astorworker.faultlocalization.entities.Line;
import fr.inria.astor.core.validation.validators.TestCasesProgramValidationResult;

public class WorkerFacade {
	private static Logger logger = Logger.getLogger(WorkerFacade.class.getName());
	private String lastWorkerID;

	public List<Line> searchSuspicious(String testCase, TestType type, boolean passing) throws Exception {
		boolean searchDone = false;
		List<Line> candidates = null;
		while(!searchDone) {
			try {
				Worker worker = WorkerCache.getWorker();
				lastWorkerID = worker.toString();
				logger.info("Worker " + worker.toString() + " is searching for suspicious lines for " + testCase);
				candidates = worker.searchSuspicious(testCase, type, passing);
				WorkerCache.putWorker(worker);
				searchDone = true;
			} catch (ConnectException e) {
				logger.info("Worker " + lastWorkerID + " disconnected");
			}
		}

		return candidates;
	}

	public TestCasesProgramValidationResult processVariant(File variant) throws Exception {
		boolean variantProcessed = false;
		TestCasesProgramValidationResult validationResult = null;

		while(!variantProcessed) {
			try {
				Worker worker = WorkerCache.getWorker();
				lastWorkerID = worker.toString();
				logger.info("Worker " + worker.toString() + " is processing " + variant.getName());
				validationResult = worker.processVariant(variant);

				if(validationResult.isCompilationSuccess())
					logger.info("Worker " + worker.toString() + " processed " + variant.getName() + ". Number of failing test cases: " + validationResult.getFailureCount());
				else
				logger.info("Worker " + worker.toString() + " processed " + variant.getName() + ". Variant did not compile.");

				WorkerCache.putWorker(worker);
				variantProcessed = true;	
			} catch (ConnectException e) {
				logger.info("Worker " + lastWorkerID + " disconnected");
			}
		}

		return validationResult;
	}

	public String getLastWorkerID() {
		return lastWorkerID;
	}
	
}