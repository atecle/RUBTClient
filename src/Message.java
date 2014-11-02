import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Arrays;


public class Message {

	private final static byte[] handshake_header = {0x13,'B','i','t','T','o','r','r','e','n','t',' ',
		'p','r','o','t','o','c','o','l',0,0,0,0,0,0,0,0};


	public static final byte KEEP_ALIVE_ID = -1;
	public static final byte CHOKE_ID = 0;
	public static final byte UNCHOKE_ID = 1;
	public static final byte INTERESTED_ID = 2;
	public static final byte UNINTERESTED_ID = 3;
	public static final byte HAVE_ID = 4;
	public static final byte BITFIELD_ID = 5;
	public static final byte REQUEST_ID = 6;
	public static final byte PIECE_ID = 7;

	//non-payload messages
	public static final Message KEEP_ALIVE = new Message(KEEP_ALIVE_ID, 0);
	public static final Message CHOKE = new Message(CHOKE_ID, 1);
	public static final Message UNCHOKE = new Message(UNCHOKE_ID, 1);
	public static final Message INTERESTED = new Message(INTERESTED_ID, 1);
	public static final Message UNINTERESTED = new Message(UNINTERESTED_ID, 1);


	private byte id;
	private int length;


	public static final byte[] keep_alive = {0,0,0,0,0};
	public static final byte[] choke = { 0,0,0,1,0};
	public static final byte[] unchoke = {0,0,0,1,1};
	public static final byte[] interested = {0,0,0,1,2};
	public static final byte[] uninterested = {0,0,0,1,3};

	private boolean isNull;



	protected Message(final byte id, int length) {
		this.id = id;
		this.length = length;
	}

	protected Message() {
		this.isNull = true;
	}

	public byte getID() {
		return id;
	}

	public int getLength() {
		return length;
	}

	public boolean isNull() {
		return isNull;
	}

	/**
	 * Constructs handshake message given peer_id and info_hash
	 * @param peer_id
	 * @param info_hash
	 * @return handshake message to send to remote peer
	 */
	public static byte[] handshake(byte[] peer_id, byte[] info_hash) {

		byte[] handshake = new byte[68];

		System.arraycopy(handshake_header,0,handshake,0,28);
		System.arraycopy(info_hash, 0, handshake,28 , 20);
		System.arraycopy(peer_id, 0, handshake,48 , 20);

		try {
			System.out.println("Peer Id: " + new String(peer_id, "UTF-8"));
			System.out.println("Info hash: " + new String(info_hash, "UTF-8"));
			System.out.println("Handshake: " + new String(handshake, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			System.out.println("Unsupported encoding");
		}


		return handshake;
	}

	public static Message decode(final InputStream in) throws EOFException, IOException {

		DataInputStream fromPeer = new DataInputStream(in);

		int length = fromPeer.readInt();


		if (length == 0) return KEEP_ALIVE;

		int id = fromPeer.readInt();


		switch (id) {

		case CHOKE_ID:
			return CHOKE;
		case UNCHOKE_ID:
			return UNCHOKE;
		case INTERESTED_ID:
			return INTERESTED;
		case UNINTERESTED_ID:
			return UNINTERESTED;
		case HAVE_ID: {
			int pieceIndex = fromPeer.readInt();
			return new HaveMessage(pieceIndex);
		}
		case BITFIELD_ID: {
			//need to do;
			return null;
		}
		case REQUEST_ID: {
			int pieceIndex = fromPeer.readInt();
			int begin = fromPeer.readInt();
			int blockLength  = fromPeer.readInt();
			return new RequestMessage(pieceIndex, begin, blockLength);
		}
		case PIECE_ID: {
			int pieceIndex = fromPeer.readInt();
			int begin = fromPeer.readInt();
			byte[] data = new byte[length - 9];
			fromPeer.readFully(data);
			return new PieceMessage(pieceIndex, begin, data);
		}
		default: 
			throw new IOException("Bad Message ID:" + id);
		}

	}

	public static void encode(final OutputStream out, Message message) 
			throws IOException {

		DataOutputStream toPeer = new DataOutputStream(out);

		System.out.println("Sending message " + message.getID());

		if (message.getID() == KEEP_ALIVE_ID) {
			toPeer.write(message.length);

		} else {

			switch(message.getID()) {

			case INTERESTED_ID: {

				toPeer.writeInt(message.getLength());
				toPeer.writeByte(message.getID());

				break;
			}
			case UNINTERESTED_ID: {
				toPeer.writeInt(message.getLength());
				toPeer.writeByte(message.getID());
				break;
			}
			case CHOKE_ID: {
				toPeer.writeInt(message.getLength());
				toPeer.writeByte(message.getID());
				break;
			}
			case UNCHOKE_ID: {
				toPeer.writeInt(message.getLength());
				toPeer.writeByte(message.getID());
				break;
			}
			case HAVE_ID: {
				HaveMessage mess = (HaveMessage) message;
				toPeer.writeInt(mess.getLength());
				toPeer.writeByte(mess.getID());
				toPeer.writeInt(mess.getPieceIndex());
				break;
			}
			case PIECE_ID: {
				PieceMessage msg = (PieceMessage) message;
				toPeer.writeInt(message.getLength());
				toPeer.writeByte(message.getID());
				toPeer.writeInt(msg.getPieceIndex());
				toPeer.writeInt(msg.getOffset());
				toPeer.write(msg.getPiece());
				break;
			}
			case REQUEST_ID: {
				RequestMessage temp = (RequestMessage) message;
				toPeer.writeInt(temp.getLength());
				toPeer.writeByte(temp.getID());
				toPeer.writeInt(temp.getIndex());
				toPeer.writeInt(temp.getOffset());
				toPeer.writeInt(temp.getLength());
				break;
			}
			case BITFIELD_ID: {
				//TODO
			}

			}

		}	

		toPeer.flush();
	}

	public static class HaveMessage extends Message {

		private int pieceIndex;

		public int getPieceIndex() {
			return pieceIndex;
		}

		public HaveMessage(int index) {
			super(HAVE_ID, 5);
			this.pieceIndex = index;
		}

	}

	public static class PieceMessage extends Message {

		private int pieceIndex;
		private int offset;
		private byte[] data;


		public PieceMessage(int pieceIndex, int begin, byte[] data) {
			super(PIECE_ID, data.length + 9);
			this.offset = begin;
			this.data = data;

		}
		public int getPieceIndex() {
			return pieceIndex;
		}

		public int getOffset() {
			return offset;
		}

		public byte[] getPiece() {
			return data;
		}

		public int getPieceLength() {
			return data.length;
		}

	}

	public static class RequestMessage extends Message {
		private int pieceIndex;
		private int begin;
		private int length;


		public RequestMessage(int pieceIndex, int begin, int length) {
			super(REQUEST_ID, 13);
			this.pieceIndex = pieceIndex;
			this.begin = begin;
			this.length = length;
		}


		public int getIndex() {
			return pieceIndex;
		}

		public int getOffset() {
			return begin;
		}

		public int getLength() {

			return length;
		}


	}

	public static class BitfieldMessage extends Message {
		private byte[] data;


		public BitfieldMessage(byte[] data) {
			super(BITFIELD_ID, data.length + 1);
			this.data = data;
		}

		public byte[] getData() {
			return data;
		}


	}
}

