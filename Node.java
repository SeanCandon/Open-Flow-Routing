import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

public abstract class Node {
	
	static final int PACKETSIZE = 65536;
	public static final int END_USERS = 14;
	public static final int START_PORT = 1025;
	public static final int CONTROLLER_PORT = 40000;
	public static ArrayList<Router> routers = new ArrayList<Router>();
	public static ArrayList<EndUser> endUsers = new ArrayList<EndUser>();
	public static boolean send_all = false;
	public static boolean instructions_received = false;
	public static int counter = 0;
	public static final String DEFAULT_HOST = "localhost";
	DatagramSocket socket;
	Listener listener;
	CountDownLatch latch;
	
	Node() {
		
		latch= new CountDownLatch(1);
		listener= new Listener();
		listener.setDaemon(true);
		listener.start();
	}
	
	
	public abstract void onReceipt(DatagramPacket packet);
	
	/**
	 *
	 * Listener thread
	 * 
	 * Listens for incoming packets on a datagram socket and informs registered receivers about incoming packets.
	 */
	class Listener extends Thread {
		
		/*
		 *  Telling the listener that the socket has been initialized 
		 */
		public void go() {
			latch.countDown();
		}
		
		/*
		 * Listen for incoming packets and inform receivers
		 */
		public void run() {
			try {
				latch.await();
				// Endless loop: attempt to receive packet, notify receivers, etc
				while(true) {
					DatagramPacket packet = new DatagramPacket(new byte[PACKETSIZE], PACKETSIZE);
					
					socket.receive(packet);
					
					onReceipt(packet);
				}
			} catch (Exception e) {if (!(e instanceof SocketException)) e.printStackTrace();}
		}
		
	}
}