package br.ufg.inf.executors;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

import org.apache.log4j.Logger;

public class CommandExecutorProcess  {
	private static Logger logger = Logger.getLogger(CommandExecutorProcess.class);

	public static List<String> execute(String command, String location) throws IOException, InterruptedException {
		long t_start = System.currentTimeMillis();
		ProcessBuilder pb = new ProcessBuilder(command.split(" "));
		File errorFile = File.createTempFile("error", ".tmp");
		File standardFile = File.createTempFile("standard", ".tmp");
		pb.redirectOutput(standardFile);
		pb.redirectError(errorFile);	

		
		pb.directory(new File(location));
		Process p = pb.start();
		p.waitFor();
		p.exitValue();


		// Getting output
		List<String> output = new ArrayList<String>();
		output = addOutput(standardFile, output);
		output = addOutput(errorFile, output);
		
		p.destroy();

		long t_end = System.currentTimeMillis();
		logger.debug("Execution time " + ((t_end - t_start) / 1000) + " seconds");
		return output;
	}

	public static List<String> execute(String command) throws IOException, InterruptedException {
		long t_start = System.currentTimeMillis();
		ProcessBuilder pb = new ProcessBuilder(command.split(" "));
		File errorFile = File.createTempFile("error", ".tmp");
		File standardFile = File.createTempFile("standard", ".tmp");
		pb.redirectOutput(standardFile);
		pb.redirectError(errorFile);

		Process p = pb.start();
		p.waitFor();
		p.exitValue();


		// Getting output
		List<String> output = new ArrayList<String>();
		output = addOutput(standardFile, output);
		output = addOutput(errorFile, output);
		
		p.destroy();

		long t_end = System.currentTimeMillis();
		logger.debug("Execution time " + ((t_end - t_start) / 1000) + " seconds");
		return output;
	}

	private static List<String> addOutput(File file, List<String> out) throws IOException {
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line = null;

			while ((line = br.readLine()) != null)
				out.add(line);

			br.close();
			return out;
		}

		catch(FileNotFoundException ex){
			logger.info("Erro reading process output");
			ex.printStackTrace();
		}

		return null;
	}

}