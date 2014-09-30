
import java.net.*;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.util.Random;


public class Tracker {

	private static final String ALPHA_NUMERIC_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

	private TorrentInfo torrent;


	public Tracker(TorrentInfo torr) {
		this.torrent = torr;
	}

	public String[] getPeerList() {



		return null;
	}

	public String sendGet() throws Exception {

		URL trackerURL = torrent.announce_url;
		System.out.println(randomAlphaNumeric());
		HttpURLConnection conn = (HttpURLConnection) trackerURL.openConnection();

		conn.setRequestMethod("GET");
		conn.connect();
		System.out.println(conn.getResponseCode());
		return null;
	}


	public static String randomAlphaNumeric() {
		StringBuilder builder = new StringBuilder();
		int count = 20;
		while (count-- != 0) {
			int character = (int)(Math.random()*ALPHA_NUMERIC_STRING.length());
			builder.append(ALPHA_NUMERIC_STRING.charAt(character));
		}
		return builder.toString();
	}
}
