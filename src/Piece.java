
public class Piece {

	private byte[] data;
	int offset;
	
	
	public Piece(int size) {
		data = new byte[size];
		this.offset = 0;
	}
	
	
	public void addPiece(int offset, byte[] data) {
		
		for (int i = 0; i < data.length; i++) {
			data[offset+i] = data[i];
		}
		
		this.offset = offset;
	}
	
	public byte[] getData() {
		return data;
	}
	
	public int getOffset() {
		return offset;
	}

}
