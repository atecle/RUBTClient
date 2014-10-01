
import java.net.*;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;


public class Tracker {

	private static final String ALPHA_NUMERIC_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

	private String info_hash;
	private String peer_id;
	private String[] peer_list;
	private int port;
	private int uploaded;
	private int downloaded;
	private int left;
	private TorrentInfo torrent;

	public Tracker(TorrentInfo torr) {
		this.torrent = torr;
		this.peer_id = randomAlphaNumeric();
		this.uploaded = 0;
		this.downloaded = 0;
		this.left = 0;
		this.port = 6881;
		this.peer_list = getPeerList();
	}

	public TorrentInfo getTorrent() {
		return torrent;
	}

	public String getPeerID() {
		return peer_id;
	}
	
	public int getUploaded() {
		return uploaded;
	}
	
	public int getDownloaded() {
		return downloaded;
	}
	
	public int getPort() {
		return port;
	}
	
	public int getLeft() {
		return left;
	}
	
	private String[] getPeerList() {

		Bencoder2 decoder = new Bencoder2();
		String[] peer_list = null;
		HttpURLConnection conn = sendGet();
		InputStream in = null;
		HashMap<Object, Object> map = null;
		try {
		in = conn.getInputStream();

		byte[] response_bytes = new byte[in.available()];

		in.read(response_bytes);
		in.close();

		map = (HashMap<Object, Object>) decoder.decode(response_bytes);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (BencodingException e) {
			e.printStackTrace();
		}
		
		ToolKit kit = new ToolKit();
		kit.print(map);

		//I'm seeing a Dictionary, the last key containing another dictionary with a key called peer_id, only showing one key though
		//and it doesn't start with RUBT11.
		//Line 54 might be the source of the issue, not sure if that's how I should be representing decoded response_bytes


		//System.out.println("HTTP Response: " + response.toString());

		return peer_list;
	}

	public TorrentInfo getTorrentInfo() {
		return torrent;
	}

	public String getPeerId() {
		return peer_id;
	}


	private HttpURLConnection sendGet() {

		URL trackerURL = null;
		HttpURLConnection conn = null;

		try 
		{
			trackerURL = constructURL("started");
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
	 * 
	 * @param Event maps to "started", "completed", "stopped", or can be empty ""
	 * @return URL to be sent to tracker
	 * @throws MalformedURLException
	 */
	private URL constructURL(String event) throws MalformedURLException {

		if (!event.equals("started") && !event.equals("completed") && !event.equals("stopped")) {
			event = "";
		}
		
		String base_url = torrent.announce_url.toString();
		String escaped_info_hash = byteArrayToURLString(torrent.info_hash.array());

		String query = "?info_hash=" + escaped_info_hash + 
				"&peer_id=" + peer_id +
				"&downloaded=" + downloaded +
				"&left=" + left +
				"&port=" + port +
				"&event=" + event +
				"&uploaded=" + uploaded;

		return new URL(base_url + query);
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
