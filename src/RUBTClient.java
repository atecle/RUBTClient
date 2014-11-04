
//Adam Tecle & Matt Robinson

import java.io.*;
import java.nio.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;



public class RUBTClient implements Runnable {

	/**
	 * Client's tracker
	 */
	public Tracker tracker;

	public int uploaded;
	public int downloaded;
	
	
	private static boolean keepRunning;

	public ArrayList<Peer> peer_list;

	public Completed[] completed;

	public String outputFile;

	public class Completed {
		public boolean first;
		public boolean second;
		public Completed() {
			this.first = false;
			this.second = false;
		}
	}

	private static Timer trackerTimer = new Timer("trackerTimer", true);
	private static TrackerAnnounce announce;
	

	public RUBTClient(Tracker tracker, String outputFile) {
		this.tracker = tracker;
		this.completed = new Completed[tracker.getTorrentInfo().piece_hashes.length];
		for (int i = 0; i < this.completed.length; i ++) {
			this.completed[i] = new Completed();
		}
		this.outputFile = outputFile;
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

		Peer peer = response.getValidPeers().get(0);
		peer.setClient(client);
		System.out.println("Connected " + peer.connectToPeer());

		peer.doHandshake();
		if (!peer.checkHandshake(tracker.getTorrentInfo().info_hash.array())) {
			System.out.println("handshake failed");
			System.exit(1);
		}

		
		System.out.println(response.interval());
		announce = new TrackerAnnounce(client);
		trackerTimer.schedule(announce, response.interval() * 1000 );
		
		while (true) {
			
		}

	}


	public void run() {
	
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
