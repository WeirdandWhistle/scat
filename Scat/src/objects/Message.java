package objects;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;

public class Message {

	public String MessageID; // Base64
	public String date = new Date(System.currentTimeMillis()).toString();
	public String username;
	public String content;

	public void generateID() {
		try {
			MessageDigest hash = MessageDigest.getInstance("SHA256");

			String date = new Date(System.currentTimeMillis()).toString();
			String rando = String.valueOf(Math.random()) + System.nanoTime();
			String more = "if you are reading this I ask you one question. WHY? are you going through some randome source code made by a mad man? this might make its way to github but I doubt it. so why are you reading this instead of working on your own projects? Do SmTh WiTh Ur LiFe... I guess I can talk I mean I took the time to write this lol";

			MessageID = Base64.getUrlEncoder()
					.encodeToString(hash.digest((content + rando + more + date).getBytes()));
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

	}

}
