import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.io.*;
import java.net.*;
import java.util.Arrays;

public class PeerConnection {
	private String ip;
	private int port;
	private String peerId;
	private Tracker tracker;

	private Socket peerSocket;
	private DataOutputStream out;
	private DataInputStream in;

	private static int HEADER_SIZE = 68;

	private static String PROTOCOL = "BitTorrent protocol";

	public PeerConnection(Peer peer, Tracker tracker) {
		this.ip = peer.getIP();
		this.port = peer.getPort();
		this.peerId = peer.getPeerID();
		this.tracker = tracker;
	}

	public static byte[] makeHeader(Tracker tracker) throws UnsupportedEncodingException {
		ByteArrayOutputStream outBytes = null;
		try {
			outBytes = new ByteArrayOutputStream();
			outBytes.write(PROTOCOL.length());
			outBytes.write(PROTOCOL.getBytes("UTF-8"));

			for (int i = 0; i < 8; i++)
				outBytes.write(0);

			outBytes.write(tracker.getTorrentInfo().info_hash.array());
			outBytes.write(tracker.getPeerId().getBytes());

			System.out.println("SHA: " + tracker.getTorrentInfo().info_hash.array());
			System.out.println("My Peer ID: " + tracker.getPeerId());
		} catch (Exception e) {e.printStackTrace();}

		return outBytes.toByteArray();
	}

	public void openConnection() {
		try {
			peerSocket = new Socket(ip, port);
			out = new DataOutputStream(peerSocket.getOutputStream());
			in = new DataInputStream(peerSocket.getInputStream());
		} catch (UnknownHostException e) {
			System.err.println("Not a valid host!");
		} catch (IOException e) {
			System.err.println("IO error: " + e.getMessage());
		}

	}

	public boolean doHandShake() {
		try {
			System.out.println("Starting handshake...");
			out.write(makeHeader(tracker));
			
			System.out.println("Sent handshake");

			System.out.println("Getting response...");
			byte[] response = new byte[HEADER_SIZE];
			in.read(response);
			ByteBuffer hs = ByteBuffer.wrap(response);
			System.out.println("Recieved response " + response);
			System.out.println(Arrays.toString(response));
			int length = hs.get();
			System.out.println("Length: " + length);

			byte[] protocol = new byte[PROTOCOL.length()];
			hs.get(protocol);
			String protocolStr = new String(protocol, "UTF-8");
			System.out.println("Protocol: " + protocolStr);

			byte[] reserved = new byte[8];
			hs.get(reserved);

			byte[] sha = new byte[20];
			hs.get(sha);
			String shaStr = new String(sha, "UTF-8");
			System.out.println("SHA: " + shaStr);

			byte[] peerIdByte = new byte[20];
			hs.get(peerIdByte);
			String peerIdStr = new String(peerIdByte, "UTF-8");
			System.out.println("Peer ID: " + peerIdStr);

			if (shaStr.equals(tracker.getTorrentInfo().info_hash) && peerIdStr.equals(peerId))
				return true;
			else
				return false;
		} catch (IOException e) {
			System.err.println("IO error: " + e.getMessage());
			return false;
		}
	}

	public void get() {
		try {
			int length = in.readInt();
			byte[] response = new byte[length];
			in.read(response);
			ByteBuffer res = ByteBuffer.allocate(length);
			res.put(response);
			int messageId = res.get();
		} catch (IOException e) {
			System.err.println("IO error: " + e.getMessage());
		}
	}

	public void sendKeepAlive() {
		ByteBuffer ka = ByteBuffer.allocate(4);
		ka.putInt(0);
		out.print(ka.array());
	}

	public void sendChoke() {
		ByteBuffer c = ByteBuffer.allocate(5);
		c.putInt(1);
		c.put((byte)0);
		out.print(c.array());
	}

	public void sendUnChoke() {
		ByteBuffer uc = ByteBuffer.allocate(5);
		uc.putInt(1);
		uc.put((byte)1);
		out.print(uc.array());
	}

	public void sendInterested() {
		ByteBuffer i = ByteBuffer.allocate(5);
		i.putInt(1);
		i.put((byte)2);
		out.print(i.array());
	}

	public void sendUnInterested() {
		ByteBuffer ui = ByteBuffer.allocate(5);
		ui.putInt(1);
		ui.put((byte)3);
		out.print(ui.array());
	}

	public void sendHave(int index) {
		ByteBuffer h = ByteBuffer.allocate(9);
		h.putInt(5);
		h.put((byte)4);
		h.putInt(index);
		out.print(h.array());
	}

	public void sendRequest(int index, int begin) {
		sendRequest(index, begin, 16384);
	}

	public void sendRequest(int index, int begin, int length) {
		ByteBuffer r = ByteBuffer.allocate(17);
		r.putInt(13);
		r.put((byte)6);
		r.putInt(index);
		r.putInt(begin);
		r.putInt(length);
		out.print(r.array());
	}

	public void sendPiece(int index, int begin, byte[] block) {
		ByteBuffer p = ByteBuffer.allocate(13 + block.length);
		p.putInt(9 + block.length);
		p.put((byte)7);
		p.putInt(index);
		p.putInt(begin);
		p.put(block);
		out.print(p.array());
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
