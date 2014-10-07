
import java.net.*;
import java.nio.ByteBuffer;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;


public class Tracker {

	private static final String ALPHA_NUMERIC_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

	private String info_hash;
	private String peer_id;
	private ArrayList<Peer> peer_list;
	private int port;
	private int uploaded;
	private int downloaded;
	private int left;
	private TorrentInfo torrent;


	public final static ByteBuffer INTERVAL_KEY = ByteBuffer.wrap(new byte[]{ 'i', 'n', 't', 'e','r','v','a','l' });

	/**
	 * Key used to retrieve the incomplete value from the tracker response.
	 */

	public final static ByteBuffer INCOMPLETE_KEY = ByteBuffer.wrap(new byte[]{ 'i', 'n', 'c', 'o','m','p','l','e','t','e' });

	/**
	 * Key used to retrieve the complete value from the tracker response.
	 */
	public final static ByteBuffer COMPLETE_KEY = ByteBuffer.wrap(new byte[]{ 'c', 'o', 'm', 'p','l','e','t','e' });

	/**
	 * Key used to retrieve the peer list from the tracker response.
	 */
	public final static ByteBuffer PEERS_KEY = ByteBuffer.wrap(new byte[]{ 'p', 'e', 'e', 'r','s'});

	/**
	 * Key used to retrieve the peer list from the tracker response.
	 */
	public final static ByteBuffer DOWNLOADED_KEY = ByteBuffer.wrap(new byte[]{ 'd', 'o', 'w', 'n','l','o','a','d','e','d'});

	/**
	 * Key used to retrieve the peer list from the tracker response.
	 */
	public final static ByteBuffer MIN_INTERVAL_KEY = ByteBuffer.wrap(new byte[]{ 'm', 'i', 'n', ' ','i','n','t','e','r','v','a','l'});

	/**
	 * Key used to retrieve the peer id from the tracker response.
	 */
	public final static ByteBuffer PEER_ID_KEY = ByteBuffer.wrap(new byte[]{ 'p', 'e', 'e', 'r',' ','i','d'});

	/**
	 * Key used to retrieve the peer port from the tracker response.
	 */
	public final static ByteBuffer PEER_PORT_KEY = ByteBuffer.wrap(new byte[]{ 'p', 'o', 'r', 't'});

	/**
	 * Key used to retrieve the peer ip from the tracker response.
	 */
	public final static ByteBuffer PEER_IP_KEY = ByteBuffer.wrap(new byte[]{ 'i', 'p'});


	public Tracker(TorrentInfo torr) {
		this.torrent = torr;
		this.peer_id = randomAlphaNumeric();
		this.uploaded = 0;
		this.downloaded = 0;
		this.left = 0;
		this.port = 6881;
		this.peer_list = getPeers();
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
	
	public ArrayList<Peer> getPeerList() {
		return peer_list;
	}
	
	public HashMap connect() {
		
		return null;
	}

	private ArrayList<Peer> getPeers() {
		
		InputStream in = null;
		ArrayList<Peer> peer_list = new ArrayList<Peer>(20);
		HashMap map = null;
		HttpURLConnection conn = sendGet();

		byte[] b = null;
		try {

			in = conn.getInputStream();

			byte[] response_bytes = new byte[in.available()];

			in.read(response_bytes);
			b = response_bytes;
			in.close();


			map = (HashMap<Object, Object>) Bencoder2.decode(response_bytes);
			

		} catch (IOException e) {
			e.printStackTrace();
		} catch (BencodingException e) {
			e.printStackTrace();
		}

	
		ArrayList list = (ArrayList)map.get(PEERS_KEY);
		
		for (int i = 0; i < list.size(); i++) {
			HashMap t = (HashMap) list.get(i);
			String peer_id = new String(((ByteBuffer)t.get(PEER_ID_KEY)).array());
			String peer_ip = new String(((ByteBuffer)t.get(PEER_IP_KEY)).array());
			int peer_port = Integer.parseInt(new String(((ByteBuffer)t.get(PEER_IP_KEY)).array()));
			Peer peer = new Peer(peer_ip, peer_id, peer_port);
			peer_list.add(peer);
		}


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
		String escaped_peer_id = byteArrayToURLString(peer_id.getBytes());
		String query = "?info_hash=" + escaped_info_hash + 
				"&peer_id=" + escaped_peer_id +
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
