package br.ufg.inf.astor4android.worker;

import java.net.Socket;
import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.FileInputStream;
import java.io.ObjectInputStream;


import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.collections4.map.AbstractHashedMap;
import org.apache.commons.collections4.MapIterator;
import org.apache.log4j.Logger;

import fr.inria.astor.core.setup.ConfigurationProperties;
import fr.inria.astor.core.faultlocalization.entity.SuspiciousCode;
import br.ufg.inf.astorworker.faultlocalization.entities.Line;
import br.ufg.inf.astor4android.worker.entities.Worker;
import br.ufg.inf.astor4android.worker.enums.TestType;
import br.ufg.inf.astor4android.utils.FileSystemUtils;
import br.ufg.inf.astor4android.entities.AndroidProject;
import br.ufg.inf.astor4android.faultlocalization.FaultLocalizationFormulaDelegate;




/**
* Stores all the Workers.
* Uses the producer-consumer paradigm, as a Worker can only be used by one thread
* at a time.
*
* @author Kayque de Sousa Teixeira, kayque23@gmail.com
*
*/
public class WorkerCache {
	private static File project;
	private static String projectName;
	private static BlockingQueue<Worker> workers = new LinkedBlockingQueue<Worker>(); 
	private static Logger logger = Logger.getLogger(WorkerCache.class.getName());

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
	* @see #InitialInformationSender
	*/
	public static void createNewWorker(Socket socket) throws IOException, InterruptedException {
		synchronized(WorkerCache.class){
			Thread sender = new Thread(new InitialInformationSender(socket, projectName, project));
			sender.start();
		}
	}

	public static Worker getWorker() throws InterruptedException {
		synchronized(workers){
			while(workers.isEmpty())
				workers.wait();
			Worker worker = workers.poll();
			logger.info(worker.toString() + " removed from WorkerCache");
			return worker;
		}
	}	

	public static void putWorker(Worker worker) throws InterruptedException {
		synchronized(workers){
			workers.put(worker);
			if(workers.size() == 1)
				workers.notify();
			logger.info(worker.toString() + " added to WorkerCache");
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

	public static void sendMessageToAllWorkers(String message) {
		synchronized(workers){
			for(Worker worker : workers)
				worker.sendMessage(message);
		}
	}
}