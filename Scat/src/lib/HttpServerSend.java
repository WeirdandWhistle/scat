package lib;

import java.util.ArrayList;
import java.util.Date;

public class HttpServerSend {
	public static final String HttpVersion = "1.1";
	public static final String Connection = "keep-alive";
	public static final String Encoding = "chunked";

	private int repoCode = 200;
	private String repoStat = "OK";
	private String contentType = "text/plain";
	private ArrayList<String[]> headers = new ArrayList<String[]>();
	private Date date = new Date(System.currentTimeMillis());

	public void ok() {
		repoCode = 200;
		repoStat = "OK";
	}
	public void bad() {
		repoCode = 400;
		repoStat = "Bad Request";
	}
	public void notFound() {
		repoCode = 404;
		repoStat = "Not Found";
	}
	public void error() {
		repoCode = 500;
		repoStat = "Internal Server Error";
	}
	public void setCode(int code, String stat) {
		repoCode = code;
		repoStat = stat;
	}
	public void setCode(int code) {
		setCode(code, "");
	}
	public void setDate(Date date) {
		this.date = date;
	}
	public void setType(String type) {
		contentType = type;
	}
	public void addHeader(String key, String value) {
		headers.add(new String[]{key, value});
	}

	public byte[] create() {
		String out = "HTTP/" + HttpVersion + " " + repoCode + " " + repoStat + "\r\n" + "Date: "
				+ date.toString() + "\r\n" + "Connection: " + Connection + "\r\n" + "Content-Type: "
				+ contentType + "\r\n" + "Transfer-Encoding: " + Encoding + "\r\n";

		for (int i = 0; i < headers.size(); i++) {
			out += headers.get(i)[0] + ": " + headers.get(i)[1] + "\r\n";
		}
		out += "\r\n";

		return out.getBytes();

	}
	public static byte[] send(byte[] message) {
		String length = Long.toHexString(message.length);
		byte[] lengthHeader = (length + "\r\n").getBytes();

		message = Util.add(lengthHeader, message);
		return Util.add(message, "\r\n".getBytes());
	}
	public static byte[] send(String message) {
		return send(message.getBytes());
	}
	public static byte[] end() {
		return "0\r\n\r\n".getBytes();
	}
}