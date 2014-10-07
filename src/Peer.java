
public class Peer {

	private static String ip;
	private static String peer_id;
	private static int port;
	
	

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
	
	
}
