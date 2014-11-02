
//Adam Tecle & Matt Robinson

import java.io.*;
import java.nio.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
	
	public ArrayList<Peer> peer_list;
	
	
	private Timer trackerTimer = new Timer("trackerTimer", true);
	
	public RUBTClient(Tracker tracker) {
		this.tracker = tracker;
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

		RUBTClient client = new RUBTClient(tracker);
	
		Peer peer = response.getValidPeers().get(0);
		peer.setClient(client);
		System.out.println("Connected" + peer.connectToPeer());
		
		peer.doHandshake();
		if (!peer.checkHandshake(tracker.getTorrentInfo().info_hash.array())) {
			System.out.println("handshake failed");
			System.exit(1);
		}
		

		peer.sendInterested();		
		peer.listenForUnchoke();				//throwing an error but shouldn't.
		
		
		

	}


	
	public void run() {

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
