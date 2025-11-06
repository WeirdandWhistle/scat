package main;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import API.APIHandeler;
import lib.HttpResponse;
import lib.Util;

public class Distributer implements Runnable {

	ThreadManager tm;
	APIHandeler api;
	public Queue<Socket> clientStack = new LinkedBlockingQueue<>();

	public Distributer(ThreadManager tm) {
		this.tm = tm;
		api = new APIHandeler(tm);
		Thread APIThread = new Thread(api);
		APIThread.start();
	}

	@Override
	public void run() {

		while (tm.active) {
			Socket client = clientStack.poll();

			if (client == null) {
				try {
					// System.out.println("nahda");
					Thread.sleep(1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} else {
				try {
					// System.out.println("smothin to do!");
					InputStream is = client.getInputStream();

					String firstLine = Util.readLine(is);
					System.out.println(firstLine);
					String[] metaData = firstLine.split(" ");
					URI url = new URI(metaData[1]);

					if (url.getPath().startsWith("/api/")) {
						api.APIStack.offer(new Object[]{client, firstLine});
					} else if (url.getPath().startsWith("/pfp/")) {
						OutputStream out = client.getOutputStream();

						switch (metaData[0]) {
							case "GET" :
								File file = new File(url.getPath().replaceFirst("/", ""));
								if (file.exists()) {
									HttpResponse res = new HttpResponse();
									res.setType(Util.memeType(file));
									res.setBody(Files.readAllBytes(file.toPath()));
									out.write(res.create());
								} else {
									file = new File("pfp\\default_pfp.png");
									HttpResponse res = new HttpResponse();
									res.setType(Util.memeType(file));
									res.setBody(Files.readAllBytes(file.toPath()));
									out.write(res.create());
								}
								break;
							case "PUT" :
								HashMap<String, String> query = Util.parseQuery(url.getQuery());
								String name = query.get("name");
								String headers = Util.getHeaders(client);
								int length = Integer.valueOf(
										Util.getHttpHeaders(headers).get("Content-Length"));
								byte[] content = Util.readStream(is, length);

								File des = new File("pfp\\" + name + ".png");
								des.createNewFile();

								try (OutputStream writer = new FileOutputStream(des)) {
									writer.write(content);
								} catch (IOException e) {
									e.printStackTrace();
								}

						}

						client.close();
					} else {
						String path = tm.internalRedirect.get(metaData[1]);
						path = path == null ? metaData[1] : path;
						Util.serverFile(new URI(path), client);
					}

				} catch (IOException e) {
					e.printStackTrace();
				} catch (URISyntaxException e) {
					e.printStackTrace();
				} catch (NullPointerException e) {
					e.printStackTrace();
				}
			}
		}

	}

}
