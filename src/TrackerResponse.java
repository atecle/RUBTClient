import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;




public class TrackerResponse {
	
	@SuppressWarnings("rawtypes")
	private HashMap response;
	private ArrayList<Peer> peer_list;
	private ArrayList<Peer> valid_peers;
	private Integer interval;
	private Integer min_interval;
	
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
	
	@SuppressWarnings("rawtypes")
	public TrackerResponse(byte[] bencoded_response) {
		
		try {
			this.response = (HashMap) Bencoder2.decode(bencoded_response);
		} catch (BencodingException e) {
			System.err.println("Error decoding tracker response. " + e.getMessage());
		}
		
		this.interval = (Integer) response.get(INTERVAL_KEY);
		this.min_interval = (Integer) response.get(MIN_INTERVAL_KEY);
		
		peer_list = new ArrayList<Peer>(20);
		
		ArrayList templist = (ArrayList) response.get(PEERS_KEY);
		
		for (int i = 0; i < templist.size(); i++) {
			HashMap t = (HashMap) templist.get(i);
			String peer_id = new String(((ByteBuffer)t.get(PEER_ID_KEY)).array());
			String peer_ip = new String(((ByteBuffer)t.get(PEER_IP_KEY)).array());
			int peer_port = (Integer) t.get(PEER_PORT_KEY);
			Peer peer = new Peer(peer_ip, peer_id, peer_port);
			peer_list.add(peer);
		}
		
	}
	
	public ArrayList<Peer> getPeerList() {
		return peer_list;
	}
	
	public int minInterval() {
		return min_interval;
	}
	
	public ArrayList<Peer> getValidPeers() {
		
		ArrayList<Peer> valid_peers = new ArrayList<Peer>(2);
		for (int i = 0; i < peer_list.size(); i++) {
			if (peer_list.get(i).getIP().equals("128.6.171.130") || peer_list.get(i).getIP().equals("128.6.171.131")) {
				valid_peers.add(peer_list.get(i));
			}
		}
		return valid_peers;
	}
	
	public int interval() {
		return interval;
	}
	
	public void printPeers() {
		
		for (int i = 0; i < peer_list.size(); i++) {
			
			System.out.println(peer_list.get(i).getPeerID());
			System.out.println(peer_list.get(i).getIP());
			System.out.println();
		}
	}
	
}
