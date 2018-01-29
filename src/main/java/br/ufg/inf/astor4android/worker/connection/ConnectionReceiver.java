package br.ufg.inf.astor4android.worker.connection;

import java.net.Socket;
import java.net.ServerSocket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

import org.apache.log4j.Logger;

import br.ufg.inf.astor4android.worker.WorkerCache;

public class ConnectionReceiver implements Runnable {
	private int port;
	private Logger logger = Logger.getLogger(ConnectionReceiver.class.getName());
	private ServerSocket server;

	public ConnectionReceiver(int port){
		this.port = port;
	}

	@Override
	public void run(){
		try {
			server = new ServerSocket(port);
		} catch (IOException e) {
			logger.info("Could not create server socket using port " + port + ":" + e.getMessage());
			System.exit(1);
		}

		while(true) {
			try{
				Socket socket = server.accept();
				WorkerCache.createNewWorker(socket);
			} catch(Exception e){
				logger.info("Unexpected error while receiving new connection :" + e.getMessage());
				e.printStackTrace();
			}
		}
	}
}