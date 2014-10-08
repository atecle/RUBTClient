import java.io.*;
import java.nio.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;



public class RUBTClient {

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
		
		Tracker obj = new Tracker(torrent);
		int file_length = obj.getTorrentInfo().file_length;
		int piece_length = obj.getTorrentInfo().piece_length;
		ByteBuffer[] piece_hashes = obj.getTorrentInfo().piece_hashes;
		int num_pieces = piece_hashes.length;
		int last_piece_size = file_length - piece_length * (num_pieces - 1);
		System.out.println("File size: " + file_length);
		System.out.println("Piece size: " + piece_length);
		System.out.println("Number of pieces: " + num_pieces);
		System.out.println("Number of blocks: " + piece_length/16384);
		System.out.println("Last piece size: " + last_piece_size);
		ArrayList<Peer> test = obj.getPeerList();
		Peer test_peer = new Peer("128.6.171.131", "-AZ5400-Py3jGhZ69hR4", 61350);
		PeerConnection peerConnection = new PeerConnection(test_peer, obj);
		peerConnection.openConnection();
		peerConnection.doHandShake();
		peerConnection.get();
		peerConnection.sendInterested();
		peerConnection.get();
		FileOutputStream f = new FileOutputStream(output_file, true);
		for (int i = 0; i < num_pieces; i++) {
			byte[] pieceSHA = piece_hashes[i].array();

			System.out.println("Getting piece " + i + ", block 1");
			peerConnection.sendRequest(i, 0);
			byte[] block1 = peerConnection.getPiece();

			System.out.println("Getting piece " + i + ", block 2");
			int block2size = 16384;
			if (i == num_pieces - 1)
				block2size = last_piece_size - block2size;
			peerConnection.sendRequest(i, block2size);
			byte[] block2 = peerConnection.getPiece();

			ByteArrayOutputStream bo = new ByteArrayOutputStream();
			bo.write(block1);
			bo.write(block2);

			byte[] block = bo.toByteArray();

			try {
				MessageDigest digest = MessageDigest.getInstance("SHA-1");
				digest.update(block);
				byte[] info_hash = digest.digest();
				for (int j = 0; j < block.length; j++) {
					if (info_hash[j] != pieceSHA[j]) {
						System.out.println("ERROR at index " + j);
						return;
					}
				}
			}
			catch(NoSuchAlgorithmException e) {
				System.out.println("Error: " + e.getMessage());
				return;
			}
			f.write(block);
		}
		f.close();
		peerConnection.closeConnection();
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
