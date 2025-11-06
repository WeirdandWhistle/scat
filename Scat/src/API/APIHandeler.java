package API;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import lib.HttpResponse;
import lib.HttpServerSend;
import lib.Util;
import main.ThreadManager;

public class APIHandeler implements Runnable {

	public Queue<Object[]> APIStack = new LinkedBlockingQueue<>();
	ThreadManager tm;
	public MessageRelay mr;

	public APIHandeler(ThreadManager tm) {
		this.tm = tm;
		mr = new MessageRelay(tm);
		Thread messageThread = new Thread(mr);
		messageThread.start();

	}

	@Override
	public void run() {
		while (tm.active) {
			if (APIStack.peek() != null) {
				try {
					// System.out.println("mad it to API");
					Object[] request = APIStack.poll();

					Socket client = (Socket) request[0];
					String firstLine = (String) request[1];
					String[] metaData = firstLine.split(" ");
					URI url = new URI(metaData[1]);

					switch (url.getPath().split("/")[2]) {
						case "test" :
							System.out.println("test!");
							HttpResponse rep = new HttpResponse();
							OutputStream out = client.getOutputStream();

							// rep.setBody("hell yeah! science BITCH!");
							rep.useChuckedEncoding(true);
							rep.setConnection("keep-alive");

							byte[] wrinting = rep.create();

							System.out.println(Util.fromBytes(wrinting));

							out.write(wrinting);

							Thread.sleep(1000 * 5);
							System.out.println("part 2");

							out.write("5\r\nthats\r\n".getBytes());

							Thread.sleep(1000);
							System.out.println("part 3");
							out.write("3\r\nthe\r\n".getBytes());

							Thread.sleep(1000);
							System.out.println("part 4");
							out.write("7\r\nbomb yo\r\n".getBytes());
							Thread.sleep(1000);

							out.write("0\r\n\r\n".getBytes());

							client.close();
							break;
						case "chatroom" :
							if (!metaData[0].equals("GET")) {
								break;
							}

							String headers = Util.getHeaders(client);
							String ID = Util.getCookiesFromHeaders(headers).get("name");

							HttpServerSend ss = new HttpServerSend();
							System.out.println(Util.fromBytes(ss.create()));
							client.getOutputStream().write(ss.create());

							mr.clientList.put(ID, client);

							System.out.println("send off and looking good!");

							break;
						case "chat" :
							mr.chat(client, metaData);
							break;
					}
					// System.out.println("finshed API");
				} catch (NullPointerException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (URISyntaxException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ArrayIndexOutOfBoundsException e) {
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

}
