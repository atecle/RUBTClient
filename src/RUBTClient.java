
//Adam Tecle & Matt Robinson

import java.io.*;
import java.nio.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Queue;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.List;
import java.util.ArrayList;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentLinkedQueue;


/**
 * Main client app. Gets command line arguments for torrent file path and output file destination.Spawns threads to begin p2p connection
 * @author Tecle
 *
 */
public class RUBTClient implements Runnable {

	/**
	 * Client's tracker
	 */
	public Tracker tracker;

	public int uploaded;
	public int downloaded;
	public static boolean seeding;


	private static boolean keepRunning;
	private static int HEADER_SIZE = 68;

	public Queue<Peer> peer_queue;

	public String outputFile;

	public OutFile outfile;

	public List<Peer> peerList;
	public List<Peer> interested_peers;

	public class Completed {
		public boolean first;
		public boolean second;
		public Completed() {
			this.first = false;
			this.second = false;
		}
	}

	public Thread peerListener;

	private static Timer trackerTimer = new Timer("trackerTimer", true);
	private static TrackerAnnounce announce;

	/**
	 * Constructor for RUBTClient obj
	 * @param tracker
	 * @param outputFile
	 */

	public RUBTClient(Tracker tracker, String outputFile) {
		this.tracker = tracker;

		this.outputFile = outputFile;
		outfile = new OutFile(tracker.getTorrentInfo());
		keepRunning = true;
		seeding = false;
	}

	/**
	 * Initializes fields and begins worker threads
	 * @param args
	 * @throws Exception
	 */

	public static void main(String[] args) throws Exception {

		if (args.length != 2) {
			System.out.println("Usage: java -cp . RUBTClient <torrent-file> <outputfile> ");
			System.exit(0);
		}

		String torrent_file = args[0];
		String output_file = args[1];

		byte[] torrent_bytes = getBytesFromFile(torrent_file);
		TorrentInfo torrent = null;

		try
		{
			torrent = new TorrentInfo(torrent_bytes);

		} catch(BencodingException e) {
			e.printStackTrace();
		}

		Tracker tracker = new Tracker(torrent);
		ByteBuffer[] piece_hashes = tracker.getTorrentInfo().piece_hashes;
		int num_pieces = piece_hashes.length;


		TrackerResponse response = new TrackerResponse(tracker.sendEvent("started"));

		RUBTClient client = new RUBTClient(tracker, output_file);

		client.peerList = response.getValidPeers();
		client.peer_queue = new ConcurrentLinkedQueue<Peer>();

		Peer peer = client.peerList.get(0);
		peer.setClient(client);
		client.peer_queue.add(peer);
		
		peer = client.peerList.get(1);
		peer.setClient(client);
		
		client.peer_queue.add(peer);
		
		
	




		client.outfile.setClient(client);

		

		File file = new File(output_file);
		
		int complete = -1;
		if (file.exists()) {
			complete = client.outfile.loadState();

		} else {
			client.outfile.createFile();
		}

		if (complete == 1) seeding = true;
		
		 (new Thread(new Listener(client))).start();
		 (new Thread(new PeerListener(client))).start();
		 
		 System.out.println(torrent.file_length%torrent.piece_length);
		System.out.println(response.interval());
		announce = new TrackerAnnounce(client);
		trackerTimer.schedule(announce, response.interval() * 1000 );
		
		while (true) {
			Peer p = client.peer_queue.poll();
			if (p == null) continue;
			p.connectToPeer();
			p.startThreads();
		}
		
		


	}

	private static class PeerListener implements Runnable {
		
		private RUBTClient client;
		
		public PeerListener(RUBTClient client) {
			this.client = client;
		}
		public void run() {
			int port = 6881;
			while (true) {
				try {
					ServerSocket serverSocket = new ServerSocket(port);
					Socket clientSocket = serverSocket.accept();
					DataInputStream fromPeer = new DataInputStream(clientSocket.getInputStream());

					byte[] response_id = new byte[20];
					byte[] response = new byte[HEADER_SIZE];

					boolean validHandshake = true;

					try {


						fromPeer.read(response);
						try {
							System.out.println("Response: " + new String(response, "UTF-8"));
						} catch (UnsupportedEncodingException e) {
							System.out.println("Unsupported Encoding");
						}

						System.arraycopy(response, 48, response_id, 0, 20);
						for (Peer peer : client.peerList) {
							boolean equal = true;
							for (int i = 0; i < 20; i++) {
								if (response_id[i] != peer.getPeerId().getBytes()[i])
									equal = false;
							}
							if (equal) {
								validHandshake = validHandshake && equal;
								break;
							}
						}
					} catch(EOFException e) {
						System.err.println("EOF Exception " + e.getMessage());
						break;
					} catch (IOException e) {
						System.err.println("IOException " + e.getMessage());
						break;
					}
					if (validHandshake) {
						Peer peer = new Peer(clientSocket.getInetAddress().toString(), new String(response_id, "UTF-8"), clientSocket.getPort());
						peer.setClient(client);
						peer.startThreads();
						client.peer_queue.add(peer);
					}
				} catch (SocketTimeoutException e) {
					port++;
					if (port > 6998)
						port = 6881;
				} catch (IOException e) {
					System.out.println("Caught IOException listening for new peers");
					break;
				}
			}
		}
	}


	private static class Listener implements Runnable {
		
		RUBTClient client;
		
		public Listener(RUBTClient client) {
			this.client = client;
		}
		public void run(){
			Scanner scanner = new Scanner(System.in);
			while(true){
				if(scanner.nextLine().equals("quit")){
					client.outfile.close();
					System.exit(1);
				}else{
					System.out.println("incorrect input. try typing \"quit\"");
				}
			}
		}
	}

	public synchronized void setDownloaded(int down) { 
		this.downloaded += down;
	}
	private static class TrackerAnnounce extends TimerTask {

		private final RUBTClient client;

		public TrackerAnnounce(RUBTClient client) {
			this.client = client;
		}

		public void run() {

			System.out.println("Sending announce to Tracker");
			this.client.tracker.update(this.client.uploaded, this.client.downloaded);
			this.client.tracker.constructURL("");
			TrackerResponse response = new TrackerResponse(this.client.tracker.sendEvent(""));

			int interval = response.interval();

			if (interval < 60 || interval > 180) {
				interval = 180;
			}

			this.client.trackerTimer.schedule(new TrackerAnnounce(this.client), interval * 1000);

		}
	}

private static class OptimisticChoke extends TimerTask {
		
		private final RUBTClient client;
		
		public OptimisticChoke(RUBTClient client) {
			this.client = client;
		}
		
		public void run() {
			
			double rand = Math.random() * 3;
			Math.round(rand);
			
			int total = 0;
			int max = Integer.MIN_VALUE;
			int max_index = -1;
			int min = Integer.MAX_VALUE;
			int min_index = -1;
			System.out.println("====== Optimistic Choke Task Beginning ======");
			for (int i = 0; i < client.peerList.size(); i++) {
				Peer peer = client.peerList.get(i);
				if (!peer.isChoked()) {
					total = peer.getLastUploaded() + peer.getLastDownloaded(); 
					peer.setLastDownloaded(0);
					peer.setLastUploaded(0);
					min_index = total < min ? i : min_index;
				}
			}
			
			for (int i = 0; i < client.interested_peers.size(); i++) {
				Peer peer = client.interested_peers.get(i);
				total = peer.getLastDownloaded() + peer.getLastUploaded();
				peer.setLastUploaded(0);
				peer.setLastDownloaded(0);
				max_index = total > max ? i : max_index;
			}
			
			Peer add_peer;
			Peer drop_peer;
			
			add_peer = max_index != -1 ? client.interested_peers.get(max_index) : null;
			drop_peer = min_index != -1 ? client.interested_peers.get(min_index) : null;
			
			if (add_peer == null || drop_peer == null) {
				return;
			} else {
				System.out.println("Unchoking peer " + add_peer.getPeerId());
				add_peer.addJob(Message.UNCHOKE);
				add_peer.setChoked(false);
				client.interested_peers.remove(add_peer);
				
				System.out.println("Choking peer " + drop_peer.getPeerId());
				drop_peer.addJob(Message.CHOKE);
				drop_peer.setChoked(true);
			}
			
			System.out.println("====== Optimistic Choke Task Ending ========");
		}
	}


	/**
	 * 
	 * @param file_name 
	 * @return
	 */
	private static byte[] getBytesFromFile(String file_name)
	{
		File file = new File(file_name);
		long file_size_long = -1;
		byte[] file_bytes = null;
		InputStream file_stream;

		try
		{
			file_stream = new FileInputStream(file);

			// Verify that the file exists
			if (!file.exists())
			{
				System.err
				.println("Error: [TorrentFileHandler.java] The file \""
						+ file_name
						+ "\" does not exist. Please make sure you have the correct path to the file.");
				file_stream.close();
				return null;
			}

			// Verify that the file is readable
			if (!file.canRead())
			{
				System.err
				.println("Error: [TorrentFileHandler.java] Cannot read from \""
						+ file_name
						+ "\". Please make sure the file permissions are set correctly.");
				file_stream.close();
				return null;
			}

			// The following code was derived from
			// http://javaalmanac.com/egs/java.io/File2ByteArray.html
			file_size_long = file.length();

			// Avoid overflow in the file length
			if (file_size_long > Integer.MAX_VALUE)
			{
				System.err.println("Error: [TorrentFileHandler.java] The file \"" + file_name
						+ "\" is too large to be read by this class.");

				file_stream.close();
				return null;
			}

			// Initialize the byte array for the file's data
			file_bytes = new byte[(int) file_size_long];

			int file_offset = 0;
			int bytes_read = 0;

			// Read from the file
			while (file_offset < file_bytes.length
					&& (bytes_read = file_stream.read(file_bytes, file_offset,
							file_bytes.length - file_offset)) >= 0)
			{
				file_offset += bytes_read;
			}

			// Verify that we read everything from the file
			if (file_offset < file_bytes.length)
			{
				throw new IOException("Could not completely read file \""
						+ file.getName() + "\".");
			}
			// End of code from
			// http://javaalmanac.com/egs/java.io/File2ByteArray.html

			file_stream.close();

		}
		catch (FileNotFoundException e)
		{
			System.err
			.println("Error: [TorrentFileHandler.java] The file \""
					+ file_name
					+ "\" does not exist. Please make sure you have the correct path to the file.");
			return null;
		}
		catch (IOException e)
		{
			System.err
			.println("Error: [TorrentFileHandler.java] There was a general, unrecoverable I/O error while reading from \""
					+ file_name + "\".");
			System.err.println(e.getMessage());
		}

		return file_bytes;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub

	}

}
