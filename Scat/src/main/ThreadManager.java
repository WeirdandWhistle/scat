package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class ThreadManager implements Runnable {

	public final int coreCount;
	public int usedThreads = 0;
	public boolean active = true;
	private ServerSocket incoming = null;
	public int port;
	public HashMap<String, String> internalRedirect = new HashMap<>();
	public Thread mainThread;
	public Thread disThread;
	public Distributer dis;
	public File configFile = new File("config.txt");

	public ThreadManager() {

		try (BufferedReader read = new BufferedReader(new FileReader(configFile))) {
			port = Integer.valueOf(read.readLine());
		} catch (IOException e) {
			e.printStackTrace();
		}

		coreCount = Runtime.getRuntime().availableProcessors();
		System.out.println("Core Count: " + coreCount);

		internalRedirect.put("/", "/index.html");

		dis = new Distributer(this);

		try {
			incoming = new ServerSocket(port);
		} catch (IOException e) {
			e.printStackTrace();
		}

		mainThread = new Thread(this);
		disThread = new Thread(dis);
		mainThread.start();
		disThread.start();
	}

	@Override
	public void run() {

		System.out.println("Server now running on 127.0.0.1:" + port);
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("Waiting for connections...");

		while (active) {

			try {
				Socket client = incoming.accept();

				// System.out.println("Something connected!");

				dis.clientStack.offer(client);

			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}
}
