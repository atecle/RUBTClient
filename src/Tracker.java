
/**
 * @author Adam Tecle
 * @author Matthew Robinson
 * 
 */
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;



/**
 * Maintains state information about Client - Tracker connection
 * Maintains connection info between Client - tracker
 *
 */
public class Tracker {

	private static final String ALPHA_NUMERIC_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

	private String info_hash;
	private String URL;
	private String peer_id;
	private int port;
	private int uploaded;
	private int downloaded;
	private TorrentInfo torrent;

	/**
	 * Constructor takes TorrentInfo obj
	 * @param torr
	 */
	public Tracker(TorrentInfo torr) {
		this.torrent = torr;
		this.peer_id = randomAlphaNumeric();
		this.uploaded = 0;
		this.downloaded = 0;
		this.port = 6881;
		constructURL("");
	}

	/**
	 * Get number of bytes uploaded so far
	 * @return
	 */
	public int getUploaded() {
		return uploaded;
	}

	/**
	 * Update download + update fields 
	 * @param uploaded
	 * @param downloaded
	 */
	public void update(int uploaded, int downloaded) {
		this.uploaded = uploaded;
		this.downloaded = downloaded;
	}

	/**
	 * Get number of bytes downloaded so far
	 * @return
	 */
	public int getDownloaded() {
		return downloaded;
	}

	/**
	 * Get port this client is listening on
	 * @return
	 */
	public int getPort() {
		return port;
	}


	/**
	 * Set port listening on
	 * @param port
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * get Info hash associated with torrent
	 * @return
	 */

	public String getInfoHash() {
		return info_hash;
	}

	/**
	 * TorrentInfo obj supplied with constructor
	 * @return
	 */
	public TorrentInfo getTorrentInfo() {
		return torrent;
	}

	/**
	 * Unique alphanumeric ID identifying client 
	 * @return
	 */
	public String getPeerId() {
		return peer_id;
	}


	/**
	 * Send 'completed' 'started' 'stopped' or empty message to tracker
	 * Must be sent at interval specified by Tracker 
	 */
	public byte[] sendEvent(String event) {

		HttpURLConnection connection = sendGet(event);

		DataInputStream in = null;
		ByteArrayOutputStream bencoded_response = null;
		try {
			in  = new DataInputStream(connection.getInputStream());
			bencoded_response = new ByteArrayOutputStream();

			int r;
			while ((r = in.read()) != -1) {
				bencoded_response.write(r);
			}

			bencoded_response.close();

		} catch (IOException e) {
			System.err.println("IO Exception " + e.getMessage());
		}


		return bencoded_response.toByteArray();
	}


	
	private HttpURLConnection sendGet(String event) {

		URL trackerURL = null;
		HttpURLConnection conn = null;

		try 
		{
			trackerURL = new URL(this.URL);
			trackerURL.openConnection();

			conn = (HttpURLConnection) trackerURL.openConnection();
			conn.setRequestMethod("GET");
			System.out.println("Response code: " + conn.getResponseCode());


			return conn;
		} catch (MalformedURLException e) {

			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
		} 

		return conn;
	}

	/**
	 * 
	 * @param Event maps to "started", "completed", "stopped", or can be empty ""
	 * @throws MalformedURLException
	 */
	public void constructURL(String event) {

		if (!event.equals("started") && !event.equals("completed") && !event.equals("stopped")) {
			event = "";
		}

		String base_url = torrent.announce_url.toString();

		String escaped_info_hash = byteArrayToURLString(torrent.info_hash.array());
		String escaped_peer_id = byteArrayToURLString(peer_id.getBytes());
		String query = "?info_hash=" + escaped_info_hash + 
				"&peer_id=" + escaped_peer_id +
				"&downloaded=" + downloaded +
				"&left=" + (torrent.file_length - downloaded) +
				"&port=" + port +
				"&event=" + event +
				"&uploaded=" + uploaded;

		this.URL = base_url + query;
	}

	private static String randomAlphaNumeric() {

		StringBuilder builder = new StringBuilder();
		int count = 20;
		while (count-- != 0) {
			int character = (int)(Math.random()*ALPHA_NUMERIC_STRING.length());
			builder.append(ALPHA_NUMERIC_STRING.charAt(character));
		}
		return builder.toString();

	}

	/**
	 * Used to escape SHA-1 info_hash so it can be sent to tracker via http get
	 * @param A byte[] array
	 * @return A URL safe string
	 */
	private static String byteArrayToURLString(byte in[]) {
		byte ch = 0x00;
		int i = 0;
		if (in == null || in.length <= 0)
			return null;

		String pseudo[] = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
				"A", "B", "C", "D", "E", "F" };
		StringBuffer out = new StringBuffer(in.length * 2);

		while (i < in.length) {
			// First check to see if we need ASCII or HEX
			if ((in[i] >= '0' && in[i] <= '9')
					|| (in[i] >= 'a' && in[i] <= 'z')
					|| (in[i] >= 'A' && in[i] <= 'Z') || in[i] == '$'
					|| in[i] == '-' || in[i] == '_' || in[i] == '.'
					|| in[i] == '!') {
				out.append((char) in[i]);
				i++;
			} else {
				out.append('%');
				ch = (byte) (in[i] & 0xF0); // Strip off high nibble
				ch = (byte) (ch >>> 4); // shift the bits down
				ch = (byte) (ch & 0x0F); // must do this is high order bit is
				// on!
				out.append(pseudo[(int) ch]); // convert the nibble to a
				// String Character
				ch = (byte) (in[i] & 0x0F); // Strip off low nibble
				out.append(pseudo[(int) ch]); // convert the nibble to a
				// String Character
				i++;
			}
		}

		String rslt = new String(out);

		return rslt;

	}
}
