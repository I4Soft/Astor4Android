package br.ufg.inf.astor4android.worker.entities;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

import org.apache.log4j.Logger;

import br.ufg.inf.astor4android.utils.ZipUtils;
import br.ufg.inf.astor4android.worker.enums.TestType;
import br.ufg.inf.astorworker.faultlocalization.entities.Line;
import fr.inria.astor.core.validation.validators.TestCasesProgramValidationResult;


/**
* Worker is the representation of an instance of AstorWorker.
* This class is used to send/receive information to/from an instance
* of AstorWorker.
*
* @author Kayque de Sousa Teixeira, kayque23@gmail.com
*
*/
public class Worker {
	private String IP;
	private int port;
	private Socket dataSocket;
	private Socket infoSocket;
	private ZipUtils zipUtils;
	private PrintWriter printWriter;
	private static Logger logger = Logger.getLogger(Worker.class.getName());


	public Worker() throws IOException {
		this.zipUtils = new ZipUtils();
	}


	/**
	* Gets the IP address and the port of the AstorWorker.
	*
	* @param socket Socket created when the AstorWorker connected.
	* @see ConnectionHandler#run()
	*/
	public void setupWorkerConnection(Socket socket) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		printWriter = new PrintWriter(socket.getOutputStream(), true);

		//Receiving information from AstorWorker
		String workerInfo = br.readLine();

		this.IP = workerInfo.split(":")[0];
		this.port = Integer.parseInt(workerInfo.split(":")[1]);
		this.infoSocket = socket;
	}


	/**
	* Sends a clean copy of the project to the AstorWorker.
	*
	* @param project File object referring the clean copy of the project 
	* @see Astor4AndroidMain#createCleanCopy(String, String, String)
	* @see #sendFolder(File)
	*/
	public void sendProject(File project) throws IOException, FileNotFoundException, InterruptedException {
		sendMessage("SEND_PROJECT");
		sendFolder(project);
	}


	/**
	* Sends a folder to the AstorWorker.
	*
	* @param folder File object referring the folder
	* @see ZipUtils#sendFolder(OutputStream, File)
	*/
	public void sendFolder(File folder) throws IOException, FileNotFoundException, InterruptedException {
		synchronized(Worker.class){
			dataSocket = null;
			while(true){
				try{
					dataSocket = new Socket(this.IP, this.port);
					if (dataSocket != null) break; 
				}
				catch (IOException e) { Thread.sleep(1000); }
			}
			zipUtils.sendFolder(dataSocket.getOutputStream(), folder);
		}
	}


	/**
	* Sends a variant to the AstorWorker.
	* AstorWorker will process the variant and send back the results.
	*
	* @param variant File object referring the variant
	* @see ZipUtils#sendFolder(OutputStream, File)
	* @see #getProgramValidationResult()
	*/
	public TestCasesProgramValidationResult processVariant(File variant) throws IOException, ClassNotFoundException, InterruptedException {
		sendMessage("PROCESS_VARIANT");
		sendFolder(variant);
		return getProgramValidationResult();
	}


	/**
	* Receives the validation result from AstorWorker.
	*
	* @see #processVariant(File)
	*/
	private TestCasesProgramValidationResult getProgramValidationResult() throws IOException, ClassNotFoundException, InterruptedException {
		dataSocket = null;
		while(true){
			try{
				dataSocket = new Socket(this.IP, this.port);
				if (dataSocket != null) break; 
			}
			catch (IOException e) { Thread.sleep(1000); }
		}
		ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(dataSocket.getInputStream()));
		TestCasesProgramValidationResult results = (TestCasesProgramValidationResult) ois.readObject();
		ois.close();
		return results;
	}


	/** 
	* Sends the project name to the AstorWorker.
	*
	* @param projectName Name of the folder of the project
	*/
	public void sendProjectName(String projectName) throws IOException, InterruptedException {
		sendMessage("SEND_PROJECT_NAME");
		sendMessage(projectName);
	}


	/**
	* Tell the AstorWorker that the repair has finished.
	*/
	public void finish() throws IOException {
		sendMessage("END");
	}

	@Override
	public String toString(){
		return "[" + IP + ":" + port + "]";
	}

	/**
	* Sends a message to the AstorWorker.
	*/
	public void sendMessage(String message) {
		printWriter.println(message);
		logger.debug("Message sent to " + toString() + ": \"" + message + "\"");
	}


	/**
	* Requests that the AstorWorker do search for suspicious lines regarding a test class.
	* This is part of the fault localization process.
	*
	* @param test The test class
	* @param type Type of the test
	* @param passing Flags whether the test is positive(true) or negative(false)
	* @return List of lines considered suspicious by the fault localization process used by 
	*		  the AstorWorker
	* @see TestType
	* @see Line
	*/
	public List<Line> searchSuspicious(String test, TestType type, Boolean passing) throws IOException, InterruptedException, ClassNotFoundException {
		sendMessage("FAULT_LOCALIZATION");
		sendMessage(type.name() + ":" + test + ":" + passing.toString());

		dataSocket = null;
		while(true){
			try{
				dataSocket = new Socket(this.IP, this.port);
				if (dataSocket != null) break; 
			}
			catch (IOException e) { Thread.sleep(1000); }
		}

		ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(dataSocket.getInputStream()));
		List<Line> candidates = (List<Line>) ois.readObject();
		ois.close();
		return candidates;
	}

	public void sendFailingTests(TestType type, String tests) throws IOException {
		if(tests == null || tests == "") return;
		sendMessage("SEND_FAILING_TEST");
		sendMessage(type.name() + "@" + tests);
	}
	
}