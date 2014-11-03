import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

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

	private Thread producer;
	private Thread consumer;

	private Queue<Message> jobQueue;

	private boolean stopProducing;

	private boolean[] peerCompleted;

	private class Producer implements Runnable {
		private FileOutputStream f = new FileOutputStream(client.outputFile);
		private FileInputStream fo = new FileInputStream(client.outputFile);
		public void run() {
			while (true) {
				Message message;
				try {
					message = Message.decode(fromPeer);
				} catch (EOFException e) {
					continue;
				} catch (IOException e) {
					System.out.println("Caught IO Exception trying to decode message: " + e.getMessage());
					f.close();
					break;
				}
				switch (message.getID()) {
					case Message.CHOKE_ID:
						System.out.println("Got choke message");
						choked = true;
						notifyAll();
						break;
					case Message.UNCHOKE_ID:
						System.out.println("Got unchoke message");
						choked = false;
						break;
					case Message.INTERESTED_ID:
						System.out.println("Got interested message");
						interested = true;
						break;
					case Message.UNINTERESTED_ID:
						System.out.println("Got uninterested message");
						interested = false;
						break;
					case Message.HAVE_ID:
						System.out.println("Got have message");
						HaveMessage hMessage = (HaveMessage)message;
						peerCompleted[hMessage.getPieceIndex()] = true;
						break;
					case Message.BITFIELD_ID:
						System.out.println("Got bitfield message");
						break;
					case Message.REQUEST_ID:
						System.out.println("Got request message");
						RequestMessage rMessage = (RequestMessage)message;
						int fileOffset = rMessage.getPieceIndex() * client.tracker.getTorrentInfo().piece_length + rMessage.getOffset();
						byte[] data = new byte[rMessage.getLength()];
						fo.read(data, fileOffset, data.length);
						Message piece = new PieceMessage(rMessage.getPieceIndex(), rMessage.getOffset(), data);
						jobQueue.offer(piece);
						break;
					case Message.PIECE_ID:
						System.out.println("Got piece message");
						PieceMessage pMessage = (PieceMessage)message;
						if (pMessage.getOffset() == 0) {
							client.completed[pMessage.getPieceIndex()].first = true;
						} else {
							client.completed[pMessage.getPieceIndex()].second = true;
						}
						f.write(pMessage.getData(),
								pMessage.getPieceIndex() * client.tracker.getTorrentInfo().piece_length + pMessage.getOffset(),
								pMessage.getData().length);
						break;
				}
			}
		}
	}

	private class Consumer implements Runnable {
		public void run() {
			while (true) {
				Message message = jobQueue.poll();
				if (message != null) { //Queue is not empty
					if (message.isNull()) {	//Signal to close thread
						stopProducing = true;
						break;
					}
					while (message.getID() != Message.INTERESTED_ID && choked)
						try { wait(); } catch (InterruptedException e) {
							System.out.println("INTERRUPTED");
							break;
						}
					try {
						Message.encode(toPeer, message);
					} catch (IOException e) {
						System.out.println("Caught IO Exception trying to encode message");
						break;
					}
				}
			}
		}
	}

	public void startThreads() {
		this.producer = new Thread(this.new Producer());
		this.consumer = new Thread(this.new Consumer());
		this.jobQueue = new ConcurrentLinkedQueue<Message>();
		this.producer.start();
		this.consumer.start();
	}

	public boolean addJob(Message message) {
		return jobQueue.offer(message);
	}

	public Peer(String ip, String peer_id, int port) {

		this.peer_ip = ip;
		this.peer_id = peer_id;
		this.port = port;
		this.choked = true;
		this.peer_choking = true;
		this.connected = false;
		this.interested = false;
		this.stopProducing = false;
	}

	public void setClient(RUBTClient client) {
		this.client = client;
		this.peerCompleted = new boolean[client.completed.length];
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
			try {
				System.out.println("Response: " + new String(response, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				System.out.println("Unsupported Encoding");
			}


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
