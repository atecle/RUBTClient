//Adam Tecle & Matt Robinson
public class Peer {

	private String ip;
	private String peer_id;
	private int port;
	
	private static int HEADER_SIZE = 68;

	private static String PROTOCOL = "BitTorrent protocol";
	

	public Peer(String ip, String peer_id, int port) {
		
		this.ip = ip;
		this.peer_id = peer_id;
		this.port = port;
	}
	
	public String getIP() {
		return ip;
	}
	
	public String getPeerID() {
		return peer_id;
	}
	
	public int getPort() {
		return port;
	}
	
	@Override
	public String toString() {
		return String.format("ID: %s\nIP: %s\nPort: %d", peer_id, ip, port);
	}
}
