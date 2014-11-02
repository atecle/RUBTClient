import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;


public class Peer {

	private RUBTClient client;
	private String peer_ip;
	private String peer_id;
	private int port;

	private static int HEADER_SIZE = 68;
	private static String PROTOCOL = "BitTorrent protocol";

	private boolean choked;
	private boolean peer_choking;
	private boolean connected;
	private boolean interested;
	private boolean peer_interested;

	private byte[] response;


	private Socket peerSocket;
	private DataInputStream fromPeer;
	private DataOutputStream	toPeer;



	public Peer(String ip, String peer_id, int port) {

		this.peer_ip = ip;
		this.peer_id = peer_id;
		this.port = port;
		this.choked = true;
		this.peer_choking = true;
		this.connected = false;
		this.interested = false;


	}

	public void setClient(RUBTClient client) {
		this.client = client;
	}


	public String getIP() {
		return peer_ip;
	}

	public String getPeerID() {
		return peer_id;
	}

	public int getPort() {
		return port;
	}

	public void unchokeMe() {
		choked = false;
	}


	public boolean listenForUnchoke() {
		
		try {
			if (fromPeer.read() == 1 && fromPeer.read() == 1) {

				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}


	public boolean connectToPeer() {

		try {

			this.peerSocket = new Socket(peer_ip, port);
			this.peerSocket.setSoTimeout(180*1000);				//3 minute timeout
			this.toPeer = new DataOutputStream(peerSocket.getOutputStream());
			this.fromPeer = new DataInputStream(peerSocket.getInputStream());
		} catch(UnknownHostException e) {
			System.err.println("Unknown Host " + e.getMessage());
			return false;
		} catch(IOException e) {
			System.err.println("IO Exception " + e.getMessage());
			return false;
		}

		connected = true;
		return true;
	}

	public void close() {

		try {

			if (peerSocket != null) peerSocket.close();

			if (toPeer != null) toPeer.close();

			if (fromPeer != null) fromPeer.close();

			connected = false;

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void sendMessage(byte[] message) {

		try {

			this.toPeer.write(message);

		} catch(IOException e) {
			System.err.println("IO Exception in sendMessage " + e.getMessage());
		}


	}

	public void sendKeepAlive() {

		sendMessage(Message.keep_alive);
	}

	public void sendInterested() {

		sendMessage(Message.interested);

	}

	public void sendUninterested() {

		sendMessage(Message.uninterested);

	}



	public Message listen() {

		try {
			
			Message m = Message.decode(fromPeer);

			return m;
		} catch (EOFException e) {
			System.err.println("EOF Exception in listen " + e.getMessage());
			return null;
		} catch(IOException e) {
			System.err.println("IO Exception in listen" + e.getMessage());
			return null;
		}
	}

	public void doHandshake() {

		sendMessage(Message.handshake(peer_id.getBytes(), client.tracker.getTorrentInfo().info_hash.array()));

	}


	public boolean checkHandshake(byte[] info_hash) {

		byte[] response_hash = new byte[20];
		byte[] response = new byte[HEADER_SIZE];

		try {


			fromPeer.read(response);


			System.arraycopy(response, 28, response_hash, 0, 20);
			for (int i = 0; i < 20; i++) {
				if (response_hash[i] != info_hash[i]) {
					System.out.println(Arrays.toString(response_hash));
					return false;
				}
			}
		} catch(EOFException e) {
			System.err.println("EOF Exception " + e.getMessage());
			return false;
		} catch (IOException e) {
			System.err.println("IOException " + e.getMessage());
			return false;
		}

		return true;
	}

	public boolean handshakeCheck(byte[] peer_handshake) {

		byte[] peer_info_hash = new byte[20];

		System.arraycopy(peer_handshake, 28, peer_info_hash, 0, 20);

		byte[] peer_id = new byte[20];

		System.arraycopy(peer_handshake,48,peer_id,0,20);//copies the peer id.

		if (Arrays.equals(peer_info_hash, this.client.tracker.getTorrentInfo().info_hash.array())) return true;

		return false;

	}





	@Override
	public String toString() {
		return String.format("ID: %s\nIP: %s\nPort: %d", peer_id, peer_ip, port);
	}
}
