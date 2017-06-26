package br.ufg.inf.astorworker.faultlocalization;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Collections;

import org.apache.log4j.Logger;

import br.ufg.inf.handlers.WorkerHandler;
import fr.inria.astor.core.faultlocalization.entity.SuspiciousCode;
import fr.inria.astor.core.faultlocalization.FaultLocalizationStrategy;
import fr.inria.astor.core.faultlocalization.FaultLocalizationResult;
import fr.inria.astor.core.setup.ConfigurationProperties;

public class AndroidFaultLocalization implements FaultLocalizationStrategy {
	Logger log = Logger.getLogger(AndroidFaultLocalization.class.getName());

	public FaultLocalizationResult searchSuspicious(String location, List<String> testsToExecute,
			List<String> toInstrument, Set<String> cp, String srcFolder) throws Exception {

		List<SuspiciousCode> suspicious = WorkerHandler.runFaultLocalization();
		List<SuspiciousCode> candidates = new ArrayList<>();
		double thr = Double.valueOf(ConfigurationProperties.properties.getProperty("flthreshold"));

		for(SuspiciousCode candidate : suspicious){
			if(candidate.getSuspiciousValue() >= thr)
				candidates.add(candidate);
		}

		int maxSuspCandidates = ConfigurationProperties.getPropertyInt("maxsuspcandidates");
		int max = (candidates.size() < maxSuspCandidates) ? candidates.size() : maxSuspCandidates;

		Collections.sort(candidates, (o1, o2) -> Double.compare(o2.getSuspiciousValue(), o1.getSuspiciousValue()));
		
		log.info("AndroidFaultLocalization found: " + candidates.size() + " with susp > " + thr + ", we consider: " + max);

		return new FaultLocalizationResult(candidates.subList(0, max), null);
	}
	
}