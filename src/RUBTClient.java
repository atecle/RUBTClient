
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



public class RUBTClient implements Runnable {

	/**
	 * Client's tracker
	 */
	public Tracker tracker;

	public int uploaded;
	public int downloaded;


	private static boolean keepRunning;
	private static int HEADER_SIZE = 68;

	public Queue<Peer> peer_queue;

	public String outputFile;
	
	public OutFile outfile;

	public List<Peer> peerList;

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


	public RUBTClient(Tracker tracker, String outputFile) {
		this.tracker = tracker;
		
		this.outputFile = outputFile;
		outfile = new OutFile(tracker.getTorrentInfo());
		keepRunning = true;
	}

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
		Peer peer = client.peerList.get(0);
		peer.setClient(client);
		System.out.println("Connected " + peer.connectToPeer());

	

		
		client.outfile.setClient(client);

		client.peer_queue = new ConcurrentLinkedQueue<Peer>();
		client.peer_queue.add(peer);

		System.out.println(torrent.file_length%torrent.piece_length);
		System.out.println(response.interval());
		announce = new TrackerAnnounce(client);
		trackerTimer.schedule(announce, response.interval() * 1000 );
		peer.startThreads();

		client.peerListener = new Thread(client.new PeerListener());
		client.peerListener.start();

		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			String input;
			while ((input = br.readLine()) != null) {
				if (input.equals("quit")) {
					for (Peer currPeer : client.peer_queue) {
						currPeer.close();
					}
					tracker.sendEvent("stopped");
					System.exit(1);
				}
			}
		} catch (IOException e) {
			System.out.print(e.getMessage());
		}
	
		
		
		/*int i;
		for (i = 0; i < num_pieces - 1; i++) {
			System.out.println("Getting piece " + i + " + block 1");
			peer.addJob(new Message.RequestMessage(i, 0, 16384));

			System.out.println("Getting piece " + i + " + block 2");
			peer.addJob(new Message.RequestMessage(i, 16384, 16384));
		}


		peer.addJob(new Message.RequestMessage(i, 0, 16384));

		int last_piece = tracker.getTorrentInfo().file_length % tracker.getTorrentInfo().piece_length;
		System.out.println(tracker.getTorrentInfo().piece_length);
		peer.addJob(new Message.RequestMessage(i, 16384, last_piece));

		RandomAccessFile file = new RandomAccessFile(client.outputFile, "rw");

		for ( i = 0; i < num_pieces; i++) {
			file.write(peer.pieces[i].getData());


			file.close();
			peer.close();
		}
*/

	}
	public void run() {

	}

	private class PeerListener implements Runnable {
		public void run() {
			int port = 6881;
			while (true) {
				try {
					ServerSocket serverSocket = new ServerSocket(port);
					Socket clientSocket = serverSocket.accept();
					DataInputStream fromPeer = new DataInputStream(clientSocket.getInputStream());

					byte[] response_hash = new byte[20];
					byte[] response_id = new byte[20];
					byte[] response = new byte[HEADER_SIZE];
					byte[] info_hash = tracker.getInfoHash().getBytes();

					boolean validHandshake = true;

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
								validHandshake = false;
							}
						}

						System.arraycopy(response, 48, response_id, 0, 20);
						for (Peer peer : peerList) {
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
						peer.setClient(RUBTClient.this);
						peer.startThreads();
						peer_queue.add(peer);
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

		public void run(){
			Scanner scanner = new Scanner(System.in);
			while(true){
				if(scanner.nextLine().equals("quit")){
					System.exit(1);
				}else{
					System.out.println("incorrect input. try typing \"quit\"");
				}
			}
		}
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

}
