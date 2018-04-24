import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * @author Sean Candon (16321521)
 *
 */
public class PacketContent {
	
	public static byte HEADERLENGTH = 10;
	String string;
	private byte[] srcPortArr = new byte[4], dstPortArr = new byte[4],
	routerNoArr = new byte[4], leftArr = new byte[4], upArr = new byte[4],
	rightArr = new byte[4], downArr = new byte[4], destArr = new byte[4],
	pathPortArr = new byte[4], pathDestArr = new byte[4];
	private int routingNumber, isFirst;
	
	byte[] payload;
	byte[] buffer;
	byte[] header;
	
	public PacketContent(DatagramPacket packet) {
		
		buffer= packet.getData();
		payload= new byte[packet.getLength()-HEADERLENGTH];
		header = new byte[packet.getLength()-payload.length];
		System.arraycopy(buffer, 0, header, 0, packet.getLength()-payload.length);
		System.arraycopy(buffer, HEADERLENGTH, payload, 0, packet.getLength()-HEADERLENGTH);
		string = new String(payload);
		routingNumber = (int)header[4];
		isFirst = (int)header[8];
		
		if(header[4] ==1) {
			for(int i=0; i<header.length; i++) {
		    	if(i<2) srcPortArr[i] = header[i];
		    	else if(i<4) dstPortArr[i-2] = header[i];
		    }
		}
		if(header[5]==1) {
			for(int i=0; i<payload.length; i++) {
				if(i<2) routerNoArr[i] = payload[i];
				else if(i<4) leftArr[i-2] = payload[i];
				else if(i<6) upArr[i-4] = payload[i];
				else if(i<8) rightArr[i-6] = payload[i];
				else if(i<10) downArr[i-8] = payload[i];
				else if(i<12) destArr[i-10] = payload[i];
			}
		}
		if(header[4] == 2) {
			for(int i=0; i<payload.length; i++) {
				if(i<2) pathPortArr[i] = payload[i];
				else if(i<4) pathDestArr[i-2] = payload[i];
			}
		}
	}
	
	public PacketContent(String string) {
		this.string = string;
	}
	public String toString() {
		return string;
	}

	public DatagramPacket toDatagramPacket() {
		DatagramPacket packet= null;
		byte[] buffer1= null;
		byte[] payload1= new byte[payload.length];
		byte[] header1= new byte[header.length];
		
		for(int i=0; i<payload.length; i++) {
			payload1[i] = payload[i];
		}
		for(int j=0; j<header.length; j++) {
			header1[j] = header[j];
		}
		try {
			buffer1= new byte[header1.length+payload1.length];
			System.arraycopy(payload1, 0, buffer1, HEADERLENGTH, payload1.length);
			System.arraycopy(header1, 0, buffer1, 0, header.length);
			packet= new DatagramPacket(buffer1, buffer1.length);
		}
		catch(Exception e) {e.printStackTrace();}
		return packet;
	}

	int wrapHer(byte[] a) {
		ByteBuffer b = ByteBuffer.wrap(a);
		b.order(ByteOrder.LITTLE_ENDIAN);
		return b.getInt();
	}
	int getSrcPort(){
		return wrapHer(srcPortArr);
	}
	int getDstPort(){
		return wrapHer(dstPortArr);
	}
	int getRouterNo() {
		return wrapHer(routerNoArr);
	}
	int getLeft() {
		return wrapHer(leftArr);
	}
	int getUp() {
		return wrapHer(upArr);
	}
	int getRight() {
		return wrapHer(rightArr);
	}
	int getDown() {
		return wrapHer(downArr);
	}
	int getDestPort() {
		return wrapHer(destArr);
	}
	EndUser getDestUser() {
		EndUser ret = null;
		int p = this.getDestPort();
		for(int i=0; i<Node.endUsers.size(); i++) {
			EndUser u = Node.endUsers.get(i);
			if(p == u.getSrcPort()) {
				ret = u;
			}
		}
		return ret;
	}
	int getPathPort() {
		return wrapHer(pathPortArr);
	}
	int getPathDest() {
		return wrapHer(pathDestArr);
	}
	int getRoutingNumber() {
		return routingNumber;
	}
	void setIsFirst(int i) {
		header[8] = (byte)i;
	}
	int getIsFirst() {
		return isFirst;
	}
	boolean isFirst() {
		if(header[7] == 1) {
			return true;
		}
		return false;
	}
	boolean isLast() {
		if(header[7] == 2)
			return true;
		return false;
	}		
}