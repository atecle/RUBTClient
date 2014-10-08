import java.io.*;
import java.net.URL;
import java.util.ArrayList;



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
		ArrayList<Peer> test = obj.getPeerList();
		Peer test_peer = new Peer("128.6.171.131", "ayylmao", 61350);
		PeerConnection peerConnection = new PeerConnection(test_peer, obj);
		peerConnection.openConnection();
		peerConnection.doHandShake();
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
