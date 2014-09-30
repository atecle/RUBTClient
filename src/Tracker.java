
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
		this.left = 0;
		this.port = 6881;
	}

	public String[] getPeerList() throws IOException, BencodingException {

		Bencoder2 decoder = new Bencoder2();
		
		HttpURLConnection conn = sendGet();

		InputStream in = conn.getInputStream();
		BufferedInputStream buffer = new BufferedInputStream(in);
		
		StringBuffer response = null;
		byte[] response_bytes = new byte[in.available()];
		
		in.read(response_bytes);
		in.close();
		
		HashMap<Object, Object> map = (HashMap<Object, Object>) decoder.decode(response_bytes);
		ToolKit kit = new ToolKit();
		kit.print(map);
		
		//I'm seeing a Dictionary, the last key containing another dictionary with a key called peer_id, only showing one key though
		//and it doesn't start with RUBT11.
		//Line 54 might be the source of the issue, not sure if that's how I should be representing decoded response_bytes
		
		
		//System.out.println("HTTP Response: " + response.toString());

		return null;
	}

	private static HttpURLConnection sendGet() {

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

	private static URL constructURL(String event) throws MalformedURLException {

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

	//Found online here - http://www.java2s.com/Code/Android/Network/ConvertabytearraytoaURLencodedstring.htm 
	public static String byteArrayToURLString(byte in[]) {
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
