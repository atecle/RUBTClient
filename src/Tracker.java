
import java.net.*;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Random;


public class Tracker {

	private static final String ALPHA_NUMERIC_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

	private static String info_hash;
	private static String peer_id;
	private static int port;
	private static int uploaded;
	private static int downloaded;
	private static int left;


	private static TorrentInfo torrent;


	public Tracker(TorrentInfo torr) {
		this.torrent = torr;
		this.peer_id = randomAlphaNumeric();
		this.uploaded = 0;
		this.downloaded = 0;
		this.port = 6881;
	}

	public String[] getPeerList() {


		HttpURLConnection conn = sendGet();
		BufferedReader in = null;
		StringBuffer response = null;

		try
		{
			in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

			String inputLine;
			response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}

			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}


		//print result
		System.out.println(response.toString());

		return null;
	}

	private static HttpURLConnection sendGet() {

		URL trackerURL = null;
		HttpURLConnection conn = null;

		try 
		{
			trackerURL = constructURL(torrent);
			trackerURL.openConnection();

			conn = (HttpURLConnection) trackerURL.openConnection();

			conn.setRequestMethod("GET");

			return conn;
		} catch (MalformedURLException e) {

			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
		} 

		return null;
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

	private static URL constructURL(TorrentInfo torrent) throws MalformedURLException {

		String base_url = torrent.announce_url.toString();
		String info_hash = bytesToHex(torrent.info_hash.array());
		String get_url = base_url + "?" + "info_hash=" + info_hash + "?" + "peer_id=" + peer_id + "?" + "port=" + port + "?" + "downloaded=0" +"left=0";
		//	System.out.println(get_url);
		return new URL(get_url);
	}

	public static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for ( int j = 0; j < bytes.length; j++ ) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}
}
