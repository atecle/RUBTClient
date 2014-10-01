import java.nio.ByteBuffer;
import java.io.*;
import java.net.*;

public class PeerConnection {
	private ByteBuffer header;
	private String ip;
	private int port;

	private Socket peerSocket;
	private PrintWriter out;
	private BufferedReader in;

	private static int HEADER_SIZE = 68;

	public PeerConnection(String ip, int port, Tracker tracker) {
		header = ByteBuffer.allocate(HEADER_SIZE);
		makeHeader(header, tracker);
		this.ip = ip;
		this.port = port;
	}

	public static void makeHeader(ByteBuffer header, Tracker tracker) {
		header.put((byte)19);
		header.put("BitTorrent Protocol".getBytes());
		for (int i = 0; i < 8; i++)
			header.put((byte)0);
		header.put(tracker.getTorrentInfo().info_hash.array());
		header.put(tracker.getPeerId().getBytes());
	}

	public void openConnection() {
		try {
			peerSocket = new Socket(ip, port);
			out = new PrintWriter(peerSocket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(peerSocket.getInputStream()));
		} catch (UnknownHostException e) {
			System.err.println("Not a valid host!");
		} catch (IOException e) {
			System.err.println("IO error: " + e.getMessage());
		}
	}

	public void doHandShake() {
		try {
			out.println(header.array());
			System.out.println(in.read());
		} catch (IOException e) {
			System.err.println("IO error: " + e.getMessage());
		}
	}

	public void closeConnection() {
		try {
			peerSocket.close();
			out.close();
			in.close();
		} catch (IOException e) {
			System.err.println("IO error: " + e.getMessage());
		}
	}
}
