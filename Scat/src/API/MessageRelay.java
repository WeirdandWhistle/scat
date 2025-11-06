package API;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import lib.HttpResponse;
import lib.HttpServerSend;
import lib.Util;
import main.ThreadManager;
import objects.Message;

public class MessageRelay implements Runnable {

	private ThreadManager tm;
	public ConcurrentHashMap<String, Socket> clientList = new ConcurrentHashMap<String, Socket>();
	public Queue<String> messageStack = new LinkedBlockingQueue<>();
	public File DB1;
	public AtomicInteger DB1Count = new AtomicInteger(0);

	public final String allowedCharsInUsername = "abcdefghijklmnopqrstuvwrxyzABCDEFGHIJKLMNOPQRSTUVWRXYZ1234567890";

	public MessageRelay(ThreadManager tm) {
		this.tm = tm;
		DB1 = new File("DB\\DB1.json");
		try {
			DB1.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try (BufferedReader read = new BufferedReader(new FileReader(DB1))) {
			while (read.readLine() != null) {
				DB1Count.incrementAndGet();
			}
		} catch (IOException e) {
		}
		System.out.println(DB1Count.get());
	}

	@Override
	public void run() {
		System.out.println("starting run method in message relay");
		while (tm.active) {

			if (messageStack.peek() != null) {
				String message = messageStack.poll();

				Iterator<Socket> TheList = clientList.values().iterator();
				System.out.println("nice" + clientList.mappingCount());
				byte[] messageOut = HttpServerSend.send(message);
				try {
					while (TheList.hasNext()) {
						Socket client = TheList.next();
						System.out.println("one client sent out");

						if (client.isClosed()) {
							System.out.println("removed client");
							boolean worked = clientList
									.remove(Util.getKeyByValue(clientList, client), client);
							if (!worked) {
								System.err.println(
										"Big Error! MASSIVE FOOKING ERROR! client ID's and Sockets are out of SINK! *clear throat* sync. the SOCKESTS AND CLIENT ID'S ARE OUT OF SYNC!");
							}
						} else {
							// System.out.println("snippy snap! good prgrammin!
							// message sent: "
							// + Util.fromBytes(messageOut));
							client.getOutputStream().write(messageOut);
						}

					}
				} catch (IOException e) {
					e.printStackTrace();
				}

			} else {
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

		}

	}
	public void writeDB(File db, String content) {
		synchronized (db) {
			try (FileWriter write = new FileWriter(db, true)) {
				write.write(content + "\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	public String readDB(File db, int line) {
		synchronized (db) {
			try (BufferedReader read = new BufferedReader(new FileReader(db))) {
				for (int i = 0; i < line; i++) {
					read.readLine();
				}
				return read.readLine();

			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}
	}
	public void chat(Socket client, String[] metaData) throws IOException, URISyntaxException {
		System.out.println("chat");
		URI url = new URI(metaData[1]);
		switch (metaData[0]) {
			case "PUT" :
				String headers = Util.getHeaders(client);
				String ID = Util.getCookiesFromHeaders(headers).get("name");
				int contentLength = Integer
						.valueOf(Util.getHeaderFromBody(headers, "Content-Length"));

				String body = Util
						.fromBytes(Util.readStream(client.getInputStream(), contentLength));

				JsonObject json = (JsonObject) JsonParser.parseString(body);

				Message mes = new Message();
				mes.content = json.get("message").getAsString();
				mes.username = ID;
				mes.generateID();
				String mesJson = new Gson().toJson(mes);
				writeDB(DB1, mesJson);
				DB1Count.getAndIncrement();

				messageStack.offer(mesJson);

				client.getOutputStream().write(new HttpResponse().create());
				client.close();

				break;
			case "GET" :
				System.out.println("handeling stfff");
				int count = DB1Count.get();

				HashMap<String, String> query = Util.parseQuery(url.getQuery());

				int start = count - Integer.valueOf(query.get("start"))
						- Integer.valueOf(query.get("depth"));
				int stop = start + Integer.valueOf(query.get("depth"));

				// System.out.println("start " + start + " stop " + stop + "
				// count " + count);

				start = Math.clamp(start, 0, count);
				stop = Math.clamp(stop, start, count);

				String[] lines = new String[stop - start];

				for (int i = start; i < stop; i++) {
					lines[i - start] = readDB(DB1, i);
				}

				JsonObject jsonList = new JsonObject();
				JsonArray arr = new JsonArray();

				for (String line : lines) {
					arr.add(JsonParser.parseString(line));
				}
				jsonList.add("list", arr);

				HttpResponse res = new HttpResponse();
				res.setType("text/json");
				res.setBody(jsonList.toString());
				client.getOutputStream().write(res.create());
				client.close();

				break;
		}

	}

}
