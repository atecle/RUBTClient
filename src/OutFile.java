import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
;


public class OutFile {

	private RandomAccessFile file;
	private TorrentInfo torrent;
	private byte[] client_bitfield;
	public Piece[] pieces;
	private RUBTClient client;
	private int incomplete;
	private int file_size;
	private String filename;

	public Completed[] completed; 

	public class Completed {
		public boolean first;
		public boolean second;
		public Completed() {
			this.first = false;
			this.second = false;
		}
	}


	public OutFile(TorrentInfo torrent) {
		this.torrent = torrent;
		file_size = torrent.file_length;
		incomplete = file_size;
		filename = torrent.file_name;

		pieces = new Piece[torrent.piece_hashes.length];

		int i;
		for (i = 0; i < pieces.length - 1; i++) {
			pieces[i] = new Piece(torrent.piece_length);
		}

		int last_piece_length = torrent.file_length % torrent.piece_length;

		if (last_piece_length == 0) {

			pieces[i] = new Piece(torrent.piece_length);
		}  else  {
			pieces[i] = new Piece(last_piece_length);
		}

		if (pieces.length % 8 == 0) {
			client_bitfield = new byte[pieces.length/8];
		} else {
			client_bitfield = new byte[pieces.length/8 + 1];
		}

		initializeBitField();

		this.completed = new Completed[torrent.piece_hashes.length];
		for (i = 0; i < this.completed.length; i ++) {
			this.completed[i] = new Completed();
		}

		try {
			file = new RandomAccessFile(filename, "rw");

		} catch (FileNotFoundException e) {
			System.out.println("FileNotFoundException initializing RAF " + e.getMessage());
		} 


	}

	public void setClient(RUBTClient client) {
		this.client = client;
	}


	private void initializeBitField() {


		for (int i = 0; i < client_bitfield.length; i++) {

			for (int j = 1; j <= 8; j++) {
				client_bitfield[i] &= ~(1  << j); 
			}
		}

	}
	public void addBlock(Message.PieceMessage pMessage) {



		pieces[pMessage.getPieceIndex()].addPiece(pMessage.getOffset(), pMessage.getPiece());

		if (pMessage.getOffset() == 0) {
			completed[pMessage.getPieceIndex()].first = true;
		} else {
			completed[pMessage.getPieceIndex()].second = true;
		}



	}

	public int needPiece(byte[] peer_bitfield) {

		int piece_index = 0;

		for (int i = 0; i < peer_bitfield.length; i++) {
			for (int j = 1; j <= 8; j++) {
				boolean peer_has = ((peer_bitfield[i] >> j) & 1) == 1;
				boolean client_has = ((client_bitfield[i] >> j) & 1) == 1;

				if (peer_has && !client_has && (!completed[piece_index].first && !completed[piece_index].second)) { 
					System.out.println("need piece " + piece_index + " from peer.");
					
					return piece_index;

				}
				piece_index++;
			}
		}

		return -1;
	}

	public boolean write(int piece_index) {

		if (!verifyPiece(pieces[piece_index].getData())) {
			return false;
		}
		try {
			System.out.println("WRITING PIECE " + piece_index);
			file.seek((long)piece_index*torrent.piece_length);
			file.write(pieces[piece_index].getData());
			completed[piece_index].second = true;

			incomplete -= pieces[piece_index].getData().length;
			updateBitfield();
			if (incomplete <= 0 || piece_index == 435) {
				//ready to seed
				close();
				System.exit(1);
			}


			return true;
		} catch(IOException e) {
			System.err.println("IO exception writing to RAF " + e.getMessage());
		}

		return false;
	}


	private void close() {

		try {
			file.close();
		} catch(IOException e) {
			System.err.println("IOException closing RAF " + e.getMessage());
		}
	}


	private void updateBitfield() {

		for (int i = 0; i < pieces.length; i++) {
			int m = i%8;
			int byte_index = (i-m)/8;
			if (completed[i].first && completed[i].second) {
				
				client_bitfield[byte_index] |= (1 << (7-m));
			} else {
				client_bitfield[byte_index] &= ~(1 << (7-m));
			}

		}
	}
	
	private boolean verifyPiece(byte[] message) {

		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			System.err.println("No such algorithm " + e.getMessage());
			return false;
		}


		byte[] piece_hash = md.digest(message);


		md.update(piece_hash);

		for (int i = 0; i < torrent.piece_hashes.length; i++) {
			if (Arrays.equals(piece_hash, torrent.piece_hashes[i].array())) {
				return true;
			}
		}


		return false;
	}

}


