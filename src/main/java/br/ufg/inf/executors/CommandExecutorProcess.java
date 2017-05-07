package br.ufg.inf.executors;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

import org.apache.log4j.Logger;

public class CommandExecutorProcess  {
	private static Logger logger = Logger.getLogger(CommandExecutorProcess.class);

	public static List<String> execute(String command, String location) throws IOException, InterruptedException {
		long t_start = System.currentTimeMillis();
		ProcessBuilder pb = new ProcessBuilder(command.split(" "));
		pb.redirectOutput();
		pb.redirectErrorStream(true);
		pb.directory(new File(location));
		Process p = pb.start();
		p.waitFor();
		p.exitValue();


		// Getting output
		BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
		List<String> output = new ArrayList<String>();
		String line = null;

		while (br.ready() && (line = br.readLine()) != null)
			output.add(line);
		
		p.destroy();

		long t_end = System.currentTimeMillis();
		logger.info("Execution time " + ((t_end - t_start) / 1000) + " seconds");
		return output;
	}

	public static List<String> execute(String command) throws IOException, InterruptedException {
		long t_start = System.currentTimeMillis();
		ProcessBuilder pb = new ProcessBuilder(command.split(" "));
		pb.redirectOutput();
		pb.redirectErrorStream(true);
		Process p = pb.start();
		p.waitFor();
		p.exitValue();


		// Getting output
		BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
		List<String> output = new ArrayList<String>();
		String line = null;

		while (br.ready() && (line = br.readLine()) != null)
			output.add(line);
		
		p.destroy();

		long t_end = System.currentTimeMillis();
		logger.info("Execution time " + ((t_end - t_start) / 1000) + " seconds");
		return output;
	}

}