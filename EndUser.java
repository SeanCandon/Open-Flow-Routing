import java.net.*;
import java.io.*;
import java.nio.*;
import tcdIO.*;
import java.util.*;

/**
 * @author Sean Candon
 *
 */
public class EndUser extends Node implements Runnable {
	
	private int dstPort, srcPort, userNumber, routerPort;
	String name;
	Terminal terminal;
	InetSocketAddress dstAddress;
	Router closestRouter;
	EndUser des;
	boolean isSending = false;
	boolean isWaiting = false;
	boolean hasReceived = false;
	
	EndUser(Terminal terminal, int srcPort, int userNumber){
		try {
			this.terminal= terminal;
			this.srcPort = srcPort;
			this.userNumber = userNumber;
			this.name = "U" + userNumber;
			socket= new DatagramSocket(srcPort);
			listener.go();
		}
		catch(java.lang.Exception e) {e.printStackTrace();}
	}
	
	void setConnection(int routerPort) {
		this.routerPort = routerPort;
		dstAddress= new InetSocketAddress(DEFAULT_HOST, routerPort);
		
		for(int i=0; i<routers.size(); i++) {
			Router r = routers.get(i);
			ArrayList<Integer> routerPorts = r.getPorts();
			for(int j=0; j<routerPorts.size(); j++) {
				if(this.routerPort == routerPorts.get(j)) {
					this.closestRouter = r;
				}
			}
		}
	}
	
	void setIsWaiting(){
		isWaiting = true;
	}
	
	void setDestination(EndUser dest) {
		this.dstPort = dest.getSrcPort();
		isSending = true;
		dest.setIsWaiting();
		dest.hasReceived = false;
	}
	
	int getDst() {
		return dstPort;
	}
	int getConnection() {
		return this.routerPort;
	}
	int getSrcPort() {
		return srcPort;
	}
	int getUserNumber() {
		return userNumber;
	}

	@Override
	public synchronized void onReceipt(DatagramPacket packet) {
		
		System.out.println(this.name + " has received");
		this.notify();
		PacketContent content= new PacketContent(packet);
		terminal.println(content.toString());	
		hasReceived = true;
		
		for(int i=0; i<routers.size(); i++) {
			Router r = routers.get(i);
			r.hasSentAll = false;
		}
	}
	
	public synchronized void start() {
		
		terminal.println("Port = " + srcPort + "\nConnection = " + routerPort + "\ndst = " + dstPort);
		
		int x=0;
		while(x>=0){	
			if(this.isSending) {	
				DatagramPacket packet= null;
				byte[] payload= null;
				byte[] header= null;
				byte[] buffer= null;	
				payload= (terminal.readString("String to send: ")).getBytes();
				header= new byte[PacketContent.HEADERLENGTH];
						
				ByteBuffer bb = ByteBuffer.allocate(4);
				bb.order(ByteOrder.LITTLE_ENDIAN).putInt(srcPort);
			    byte[] p1 = bb.array();
			    ByteBuffer bb2 = ByteBuffer.allocate(4);
				bb2.order(ByteOrder.LITTLE_ENDIAN).putInt(dstPort);
				byte[] p2 = bb2.array();
					
				System.arraycopy(p1, 0, header, 0, 2);
				System.arraycopy(p2, 0, header, 2, 2);    
			    header[4] = 1;
			    header[8] = 1;		
				buffer= new byte[header.length + payload.length];
				System.arraycopy(header, 0, buffer, 0, header.length);
				System.arraycopy(payload, 0, buffer, header.length, payload.length);		
				terminal.println("Sending packet...");
				packet= new DatagramPacket(buffer, buffer.length, dstAddress);
				try {
					socket.send(packet);
					terminal.println("Packet sent to " + routerPort);
				} catch (IOException e) {
					e.printStackTrace();
				}
				isSending = false;
			}
		
			else {
				try {
					this.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}	
		}
	}
	
	public static void main(String[] args) {
		
		Terminal t0 = new Terminal("Start");	
		int no = 9;
		int port = START_PORT;
		for(int i=0; i<no; i++) {
			Terminal t = new Terminal("Router " + i);
			Router r = new Router(t, port, port+1, port+2, port+3, port+4, i);
			routers.add(r);
			port+=5;
		}
		int no2 = 12;
		int start = 3000;
		for(int i=0; i<no2; i++) {
			Terminal t = new Terminal("End User " + i);
			EndUser u = new EndUser(t, start+i, i);
			endUsers.add(u);
		}
		Router r = routers.get(0);
		r.addConnection(endUsers.get(0).getSrcPort(), r.getLeft());
		r.addConnection(endUsers.get(1).getSrcPort(), r.getUp());
		r.addConnection(routers.get(1).getLeft(), r.getRight());
		r.addConnection(routers.get(3).getUp(), r.getDown());
		EndUser u = endUsers.get(0);
		u.setConnection(r.getLeft());
		u = endUsers.get(1);
		u.setConnection(r.getUp());
		
		r = routers.get(1);
		r.addConnection(routers.get(0).getRight(), r.getLeft());
		r.addConnection(endUsers.get(2).getSrcPort(), r.getUp());
		r.addConnection(routers.get(2).getLeft(), r.getRight());
		r.addConnection(routers.get(4).getUp(), r.getDown());
		u = endUsers.get(2);
		u.setConnection(r.getUp());
		
		r = routers.get(2);
		r.addConnection(routers.get(1).getRight(), r.getLeft());
		r.addConnection(endUsers.get(3).getSrcPort(), r.getUp());
		r.addConnection(endUsers.get(4).getSrcPort(), r.getRight());
		r.addConnection(routers.get(5).getUp(), r.getDown());
		u = endUsers.get(3);
		u.setConnection(r.getUp());
		u = endUsers.get(4);
		u.setConnection(r.getRight());
		
		r = routers.get(3);
		r.addConnection(endUsers.get(11).getSrcPort(), r.getLeft());
		r.addConnection(routers.get(0).getDown(), r.getUp());
		r.addConnection(routers.get(4).getLeft(), r.getRight());
		r.addConnection(routers.get(6).getUp(), r.getDown());
		u = endUsers.get(11);
		u.setConnection(r.getLeft());
		
		r = routers.get(4);
		r.addConnection(routers.get(3).getRight(), r.getLeft());
		r.addConnection(routers.get(1).getDown(), r.getUp());
		r.addConnection(routers.get(5).getLeft(), r.getRight());
		r.addConnection(routers.get(7).getUp(), r.getDown());
		
		r = routers.get(5);
		r.addConnection(routers.get(4).getRight(), r.getLeft());
		r.addConnection(routers.get(2).getDown(), r.getUp());
		r.addConnection(endUsers.get(5).getSrcPort(), r.getRight());
		r.addConnection(routers.get(8).getUp(), r.getDown());
		u = endUsers.get(5);
		u.setConnection(r.getRight());
		
		r = routers.get(6);
		r.addConnection(endUsers.get(10).getSrcPort(), r.getLeft());
		r.addConnection(routers.get(3).getDown(), r.getUp());
		r.addConnection(routers.get(7).getLeft(), r.getRight());
		r.addConnection(endUsers.get(9).getSrcPort(), r.getDown());
		u = endUsers.get(10);
		u.setConnection(r.getLeft());
		u = endUsers.get(9);
		u.setConnection(r.getDown());
		
		r = routers.get(7);
		r.addConnection(routers.get(6).getRight(), r.getLeft());
		r.addConnection(routers.get(4).getDown(), r.getUp());
		r.addConnection(routers.get(8).getLeft(), r.getRight());
		r.addConnection(endUsers.get(8).getSrcPort(), r.getDown());
		u = endUsers.get(8);
		u.setConnection(r.getDown());

		r = routers.get(8);
		r.addConnection(routers.get(7).getRight(), r.getLeft());
		r.addConnection(routers.get(5).getDown(), r.getUp());
		r.addConnection(endUsers.get(6).getSrcPort(), r.getRight());
		r.addConnection(endUsers.get(7).getSrcPort(), r.getDown());
		u = endUsers.get(7);
		u.setConnection(r.getDown());
		u = endUsers.get(6);
		u.setConnection(r.getRight());
			
		for(int i=0; i<routers.size(); i++) {
			Thread thread = new Thread(routers.get(i));
			thread.start();
		}
		for(int i=0; i<endUsers.size(); i++) {
			Thread thread2 = new Thread(endUsers.get(i));
			thread2.start();
		}
		Terminal tc = new Terminal("Controller");
		Controller c = new Controller(tc, CONTROLLER_PORT, CONTROLLER_PORT+1);
		Thread contThread = new Thread(c);
		contThread.start();
		boolean b = true;
		boolean canSend = true;
		EndUser u1 = null;
		EndUser u2 = null;
		
		while(b) {
			
			if(canSend) {
				String s1 = t0.readString("Start user? ");
				int startUser = Integer.parseInt(s1);
				s1 = t0.readString("End user? ");
				int endUser = Integer.parseInt(s1);
				canSend = false;
				u1 =endUsers.get(startUser);
				u2 = endUsers.get(endUser);
				u1.setDestination(u2);
				Thread t = new Thread(u1);
				t.start();
				
			}
			boolean b1 = true;
			while(b1) {
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
				}
				if(u2.hasReceived) {
					canSend = true;
					b1 = false;
					c.reset();
					contThread = new Thread(c);
					contThread.start();
				}
			}
		}					
	}

	@Override
	public void run() {
		this.start();
	}
}