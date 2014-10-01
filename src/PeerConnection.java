import java.nio.ByteBuffer;
import java.io.*;
import java.net.*;

public class PeerConnection {
	private String ip;
	private int port;
	private Tracker tracker;

	private Socket peerSocket;
	private PrintWriter out;
	private BufferedReader in;

	private static int HEADER_SIZE = 68;

	public PeerConnection(String ip, int port, Tracker tracker) {
		this.ip = ip;
		this.port = port;
		this.tracker = tracker;
	}

	public static ByteBuffer makeHeader(Tracker tracker) {
		ByteBuffer header = ByteBuffer.allocate(HEADER_SIZE);
		header.put((byte)19);
		header.put("BitTorrent Protocol".getBytes());
		for (int i = 0; i < 8; i++)
			header.put((byte)0);
		header.put(tracker.getTorrentInfo().info_hash.array());
		header.put(tracker.getPeerId().getBytes());
		return header;
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
			out.println(makeHeader(tracker).array());
			System.out.println(in.read());
		} catch (IOException e) {
			System.err.println("IO error: " + e.getMessage());
		}
	}

	public void keepAlive() {
		ByteBuffer ka = ByteBuffer.allocate(4);
		ka.putInt(0);
		out.println(ka);
	}

	public void choke() {
		ByteBuffer c = ByteBuffer.allocate(8);
		c.putInt(1);
		c.put((byte)0);
		out.println(c);
	}

	public void unChoke() {
		ByteBuffer uc = ByteBuffer.allocate(8);
		uc.putInt(1);
		uc.put((byte)1);
		out.println(uc);
	}

	public void interested() {
		ByteBuffer i = ByteBuffer.allocate(8);
		i.putInt(1);
		i.put((byte)2);
		out.println(i);
	}

	public void unInterested() {
		ByteBuffer ui = ByteBuffer.allocate(8);
		ui.putInt(1);
		ui.put((byte)3);
		out.println(ui);
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
