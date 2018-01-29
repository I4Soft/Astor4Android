package br.ufg.inf.astor4android.worker;

import java.io.File;
import java.io.IOException;
import java.net.Socket;

import org.apache.log4j.Logger;

import fr.inria.astor.core.setup.ConfigurationProperties;
import br.ufg.inf.astor4android.worker.entities.Worker;
import br.ufg.inf.astor4android.worker.enums.TestType;

public class InitialInformationSender implements Runnable {
	private File project;
	private String projectName;
	private Socket infoSocket;
	private Logger logger = Logger.getLogger(InitialInformationSender.class.getName());

	InitialInformationSender (Socket socket, String projectName, File project) {
		this.infoSocket = socket;
		this.project = project;
		this.projectName = projectName;
	}

	@Override
	public void run(){
		try{
			Worker worker = new Worker();
			worker.setupWorkerConnection(infoSocket);
			worker.sendProjectName(projectName);
			worker.sendProject(project);
			worker.sendFailingTests(TestType.INSTRUMENTATION, ConfigurationProperties.getProperty("instrumentationfailing"));
			worker.sendFailingTests(TestType.UNIT, ConfigurationProperties.getProperty("unitfailing"));
			logger.info("Initial information sent to " + worker.toString());
			WorkerCache.putWorker(worker);
		} catch (Exception e){
			logger.info("Unexpected error while sending initial information to " + infoSocket.getInetAddress() + ": " + e.getMessage());
			e.printStackTrace();
		}
	}
}