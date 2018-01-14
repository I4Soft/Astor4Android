package br.ufg.inf.astorworker.faultlocalization;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Collections;
import java.util.Scanner;
import java.util.InputMismatchException;
import java.util.Locale;
import java.io.BufferedWriter;
import java.io.FileWriter;

import org.apache.log4j.Logger;

import br.ufg.inf.astor4android.handlers.WorkerHandler;
import fr.inria.astor.core.faultlocalization.entity.SuspiciousCode;
import fr.inria.astor.core.faultlocalization.FaultLocalizationStrategy;
import fr.inria.astor.core.faultlocalization.FaultLocalizationResult;
import fr.inria.astor.core.setup.ConfigurationProperties;

public class AndroidFaultLocalization implements FaultLocalizationStrategy {
	Logger log = Logger.getLogger(AndroidFaultLocalization.class.getName());

	public FaultLocalizationResult searchSuspicious(String location, List<String> testsToExecute,
			List<String> toInstrument, Set<String> cp, String srcFolder) throws Exception {

		List<SuspiciousCode> suspicious = WorkerHandler.runFaultLocalization();
		Collections.sort(suspicious, (o1, o2) -> Double.compare(o2.getSuspiciousValue(), o1.getSuspiciousValue()));
		saveFaultLocalization(suspicious);
		List<SuspiciousCode> candidates = new ArrayList<>();
		double thr = Double.valueOf(ConfigurationProperties.properties.getProperty("flthreshold"));
		Scanner in = new Scanner(System.in);
		int max;

		while(true) {
			for(SuspiciousCode candidate : suspicious){
				if(candidate.getSuspiciousValue() >= thr)
					candidates.add(candidate);
				else break;
			}

			int maxSuspCandidates = ConfigurationProperties.getPropertyInt("maxsuspcandidates");
			max = (candidates.size() < maxSuspCandidates) ? candidates.size() : maxSuspCandidates;

			Collections.sort(candidates, (o1, o2) -> Double.compare(o2.getSuspiciousValue(), o1.getSuspiciousValue()));
			
			log.info("AndroidFaultLocalization found: " + candidates.size() + " with susp > " + thr + ", we consider: " + max);

			if(candidates.size() == 0) {
				System.out.println("\nThe flthreshold seems to be too high. The highest suspicious value found for a line was " 
					+ suspicious.get(0).getSuspiciousValue());

				while(true) {
					System.out.print("Do you wanna enter a new flthreshold? (y/n): ");
					System.out.flush();
					String option = in.nextLine();

					if(option.toLowerCase().contains("y")) {
						while(true) {
							try{
								System.out.print("Enter the new flthreshold (value between 0 and 1.0): ");
								System.out.flush();
								in = new Scanner(System.in);
								in.useLocale(Locale.ENGLISH);
								double flth = in.nextDouble();

								if(flth >= 0 && flth <= 1){
									thr = flth;
									break;
								}

								else System.out.println("Invalid value."); 
							} catch(InputMismatchException ex) {
								System.out.println("Invalid value."); 
							}
						}
						break;
					}

					else if(option.toLowerCase().contains("n"))
						return new FaultLocalizationResult(candidates.subList(0, max), null);

					else System.out.println("Invalid option.");
				}

				continue;
			}

			break;
		}

		return new FaultLocalizationResult(candidates.subList(0, max), null);
	}


	private void saveFaultLocalization(List<SuspiciousCode> suspicious) throws Exception {
		BufferedWriter out = new BufferedWriter(new FileWriter(ConfigurationProperties.getProperty("projectWorkingDirectory") + "/faultlocalization.log"));
		for(SuspiciousCode sc : suspicious){
			out.write(sc.toString());
			out.newLine();
		}
		out.close();
	}
	
}