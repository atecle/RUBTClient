
public class Peer {

	private static String ip;
	private static String peer_id;
	private static String port;
	
	

	public Peer(String ip, String peer_id, String port) {
		
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
	
	public String getPort() {
		return port;
	}
	
	
}
