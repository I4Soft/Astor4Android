package br.ufg.inf.handlers;

import java.net.Socket;
import java.net.ServerSocket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

public class ConnectionHandler extends Thread {
	private int port;

	public ConnectionHandler(int port){
		this.port = port;
	}

	@Override
	public void run(){
		
		try{
			ServerSocket server = new ServerSocket(port);
			while(true){
				Socket socket = server.accept();
				WorkerHandler.createNewWorker(socket);
			}
		} catch(IOException | InterruptedException e){
			e.printStackTrace();
		}
	}
}