//Adam Tecle & Matt Robinson

/*
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

	private boolean choked;
	private boolean interested;

	public enum MessageType {
		CHOKE, UNCHOKE, INTERESTED, NOT_INTERESTED,
		HAVE, BITFIELD, REQUEST, PIECE, CANCEL
	}

	public PeerConnection(Peer peer, Tracker tracker) {
		this.ip = peer.getIP();
		this.port = peer.getPort();
		this.peerId = peer.getPeerID();
		this.tracker = tracker;
		this.choked = false;
		this.interested = false;
	}

	public class Producer implements Runnable {
		private Thread t;
	}

	public static byte[] makeHeader(Tracker tracker) throws UnsupportedEncodingException {
		
		ByteArrayOutputStream outBytes = null;
		try {
			outBytes = new ByteArrayOutputStream();
			outBytes.write((byte)PROTOCOL.length());
			outBytes.write(PROTOCOL.getBytes("UTF-8"));

			for (int i = 0; i < 8; i++)
				outBytes.write(0);

			outBytes.write(tracker.getTorrentInfo().info_hash.array());
			outBytes.write(tracker.getPeerId().getBytes());

			System.out.println("SHA: " + tracker.getTorrentInfo().info_hash.array());
			System.out.println("My Peer ID: " + tracker.getPeerId());
		} catch (IOException e) {
			System.err.println("IO error: " + e.getMessage());
		}

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
			System.out.println(new String(response, "UTF-8"));

			int length = hs.get();
			System.out.println("Length: " + length);

			byte[] protocol = new byte[PROTOCOL.length()];
			hs.get(protocol);
			String protocolStr = new String(protocol, "UTF-8");
			System.out.println("Protocol: " + protocolStr);

			byte[] reserved = new byte[8];
			hs.get(reserved);
			System.out.println("Reserved bytes " + Arrays.toString(reserved));

			byte[] sha = new byte[20];
			hs.get(sha);
			String shaStr = new String(sha, "UTF-8");
			System.out.println("SHA: " + shaStr);

			byte[] peerIdByte = new byte[20];
			hs.get(peerIdByte);
			String peerIdStr = new String(peerIdByte, "UTF-8");
			System.out.println("Peer ID: " + peerIdStr);

			boolean sameSha = true;

			if (sha.length != tracker.getTorrentInfo().info_hash.array().length)
				sameSha = false;
 
			for (int i = 0; i < sha.length; i++) {
				if (sha[i] != tracker.getTorrentInfo().info_hash.array()[i])
					sameSha = false;
			}

			if (sameSha && peerIdStr.equals(peerId))
				return true;
			else
				return false;
		} catch (IOException e) {
			System.err.println("IO error: " + e.getMessage());
			return false;
		}
	}

	public void getMessage(int length, ByteBuffer res) {
		try {
			int length = in.readInt();
			byte[] response = new byte[length];
			in.read(response);
			ByteBuffer res = ByteBuffer.wrap(response);
			int messageId = res.get();
			switch (messageId) {
				case CHOKE:
					this.choked = true;
				case UNCHOKE:
					this.choked = false;;
				case INTERESTED:
					this.interested = true;
				case NOT_INTERESTED:
					this.interested = false;
				case HAVE:
					break;
				case BITFIELD:
					break;
				case REQUEST:
					break;
				case PIECE:
					break;
			}
		} catch (IOException e) {
			System.err.println("IO error: " + e.getMessage());
		}
	}

	public byte[] getPiece() {
		byte[] block = null;
		try {
			int length = in.readInt();
			byte[] response = new byte[16393];
			in.read(response);
			ByteBuffer res = ByteBuffer.wrap(response);
			int messageId = res.get();
			int index = res.getInt();
			int begin = res.getInt();
			block = new byte[16393 - 9];
			res.get(block);
		} catch (IOException e) {
			System.err.println("IO error: " + e.getMessage());
		}
		return block;
	}

	public static ByteBuffer sendKeepAlive() {
		return ByteBuffer.allocate(4)
			.putInt(0);
	}

	public static ByteBuffer sendChoke() {
		return ByteBuffer.allocate(5)
			.putInt(1);
			.put((byte)0);
	}

	public static ByteBuffer sendUnChoke() {
		return ByteBuffer.allocate(5)
			.putInt(1);
			.put((byte)1);
	}

	public static ByteBuffer sendInterested() {
		return ByteBuffer.allocate(4)
			.putInt(1)
			.put((byte)2);
	}

	public static void sendUnInterested() {
		return ByteBuffer.allocate(5)
			.putInt(1)
			.put((byte)3);
	}

	public static void sendHave(int index) {
		return ByteBuffer.allocate(9)
			.putInt(5);
			.put((byte)4);
			.putInt(index);
	}

	public static void sendRequest(int index, int begin) {
		sendRequest(index, begin, 16384);
	}

	public static void sendRequest(int index, int begin, int length) {
		return ByteBuffer.allocate(17)
			.putInt(13)
			.put((byte)6)
			.putInt(index)
			.putInt(begin)
			.putInt(length);
	}

	public static void sendPiece(int index, int begin, byte[] block) {
		return ByteBuffer.allocate(13 + block.length);
			.putInt(9 + block.length)
			.put((byte)7)
			.putInt(index)
			.putInt(begin)
			.put(block);
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
*/