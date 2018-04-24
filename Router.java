import java.net.*;
import java.io.*;
import java.nio.*;
import java.util.*;

import tcdIO.*;

/**
 * @author Sean Candon
 *
 */

public class Router implements Runnable{
	
	private int routerNumber, left, up, right, down, controller, controller2, receivedAt;
	int pathPort, pathDest, destinationPort;
	DatagramPacket thePacket;
	PacketContent thePacketContent;
	String name;
	Terminal terminal;
	InetSocketAddress dstAddress;
	boolean hasThePacket = false;
	boolean isFirstRouter = false;
	boolean isLastRouter = false;
	boolean waitingForPacket = true;
	boolean hasReceivedInstructions = false;
	boolean canSendPacket = false;
	boolean alreadyKnowsRoute = false;
	boolean send_all = false;
	boolean hasSentAll = false;
	DatagramSocket socket, socket2, socket3, socket4, socket5;
	ArrayList<Integer> ports, connections;
	ArrayList<DatagramSocket> sockets;
	ArrayList<Route> routes;
	ArrayList<Link> links;
	
	Router(Terminal terminal, int left, int up, int right, int down, int controller, int routerNumber){
		
		try {
			this.terminal= terminal;
			this.left = left; this.up = up;
			this.right = right; this.down = down;
			this.controller = controller;
			this.routerNumber = routerNumber;
			
			ports = new ArrayList<Integer>();
			ports.add(left); ports.add(up); 
			ports.add(right);ports.add(down); 
			
			socket = new DatagramSocket(left);
			socket2 = new DatagramSocket(up);
			socket3 = new DatagramSocket(right);
			socket4 = new DatagramSocket(down);
			socket5 = new DatagramSocket(controller);
			sockets = new ArrayList<DatagramSocket>();
			sockets.add(socket); sockets.add(socket2);
			sockets.add(socket3); sockets.add(socket4);
			sockets.add(socket5);
			
			links = new ArrayList<Link>();
			connections = new ArrayList<Integer>();
			routes = new ArrayList<Route>();
			thePacket = null;
			this.name = "R" + this.routerNumber;
			isFirstRouter = false;
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	public synchronized void onReceipt(DatagramPacket packet) {
		
		PacketContent receivedPacket = new PacketContent(packet);
		String s = receivedPacket.toString();
		int routingNumber = receivedPacket.getRoutingNumber();
		if(receivedPacket.isLast()) {
			this.isLastRouter = true;
		}
		if(routingNumber==1) {
			terminal.println(s);
			waitingForPacket = false;
			hasThePacket = true;
			thePacket = packet;
			thePacketContent = receivedPacket;
			this.destinationPort = receivedPacket.getDstPort();
			receivedAt = packet.getPort();
			if(checkRoutes(Controller.getUser(destinationPort), this)) {
				Route route = getRoute(Controller.getUser(destinationPort));
				pathPort = route.getLocalPort();
				pathDest = route.getExtPort();
				alreadyKnowsRoute = true;
				this.canSendPacket = true;
				this.hasReceivedInstructions = true;
				//hommmmeeeeeeee
			}
			else {
				if(!hasSentAll) {
					send_all = true;
				}
			}
			if(thePacketContent.getIsFirst() == 1) {
				thePacketContent.setIsFirst(0);
				thePacket = thePacketContent.toDatagramPacket();
				thePacketContent = new PacketContent(thePacket);
				isFirstRouter = true;
			}
			else {
				isFirstRouter = false;
			}
			packet = null;
		}
		if(send_all) {
			for(int i=0; i<Node.routers.size(); i++) {
				Router r = Node.routers.get(i);
				r.sendAll(this.destinationPort);
			}
			send_all = false;
		}
		if(routingNumber == 2 && !alreadyKnowsRoute) {
			pathPort = receivedPacket.getPathPort();
			pathDest = receivedPacket.getPathDest();
			hasReceivedInstructions = true;
			send_all = false;
			packet = null;
		}
		if(routingNumber == 3 && !alreadyKnowsRoute) {
			canSendPacket = true;
			packet = null;
		}
		if(this.hasThePacket && this.hasReceivedInstructions && this.canSendPacket) {
			if(!alreadyKnowsRoute) {
				Route r = new Route(Controller.getUser(this.destinationPort), this, pathPort, pathDest);
				routes.add(r);
			}
			dstAddress = new InetSocketAddress(Node.DEFAULT_HOST, pathDest); 
			DatagramSocket sendSocket = null;
			
			for(int i=0; i<sockets.size(); i++) {
				DatagramSocket temp = sockets.get(i);
				if(temp.getLocalPort() == pathPort) {
					sendSocket = temp;
				}
			}
			thePacket.setSocketAddress(dstAddress);
			if(sendSocket!=null) {
				try {
					sendSocket.send(thePacket); 
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			alreadyKnowsRoute = false;
			hasThePacket = false;
			hasReceivedInstructions = false;
			canSendPacket = false;
			isFirstRouter = false;
			isLastRouter = false;
			send_all = false;
			hasSentAll = false;
		}		
	}
	
	void trySocket(DatagramSocket socket, DatagramPacket packet) {
		
		try {
			socket.setSoTimeout(5);
		} catch (SocketException e) {
		}
		try {
			socket.receive(packet);
			onReceipt(packet);
		} catch (IOException e) {
		}	
	}
	
	int getSocketPort() {
		return this.socket.getLocalPort();
	}
	
	private class Route {
		
		private EndUser dest = null;
		private Router router = null;
		private int localPort = 0;
		private int extPort = 0;
		
		Route(EndUser dest, Router router, int localPort, int extPort){
			this.dest = dest;
			this.router = router;
			this.localPort = localPort;
			this.extPort = extPort;
		}
		EndUser getDest() {
			return dest;
		}
		Router getRouter() {
			return router;
		}
		int getLocalPort() {
			return localPort;
		}
		int getExtPort() {
			return this.extPort;
		}		
	}
	
	boolean checkRoutes(EndUser u, Router r1) {
		
		for(int i=0; i<routes.size(); i++) {
			Route r = routes.get(i);
			if(r.getDest() == null) return false;
			else if(r.getRouter() == null) return false;
			if(r.getDest().equals(u) && r.getRouter().equals(r1)) {
				return true;
			}
		}
		return false;	
	}
	
	Route getRoute(EndUser u) {
		
		Route ret = null;
		
		for(int i=0; i<routes.size(); i++) {
			Route r = routes.get(i);
			if((r.getDest().equals(u))) {
				ret = r;
			}
		}
		return ret;	
	}
	
	public synchronized void start() {
		
		for(int i=0; i<links.size(); i++) {
			Link l = links.get(i);
			terminal.println(l.internal + " -> " + l.external);
		}
		int x=0;
		while(x>=0){
			terminal.println("Waiting for contact");
			boolean b = true;
			while(b) {
				for(int i=0; i<sockets.size(); i++) {
					DatagramPacket pack = new DatagramPacket(new byte[Node.PACKETSIZE], Node.PACKETSIZE);
					DatagramSocket sock = sockets.get(i);
					trySocket(sock, pack);
				}
			}
			try {
				this.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void run() {
		this.start();
	}
	
	boolean isFirst() {
		return isFirstRouter;
	}
	
	void addConnection(int external, int internal) {
		int distance = 1 + (int)(Math.random() * ((20 - 1) + 1));
		Link l = new Link(external, internal, distance);
		links.add(l);
		connections.add(external);
	}
	
	int getDistanceOfLink(int external, int internal) {
		
		Link l = null;
		for(int i=0; i<links.size(); i++) {
			Link l1 = links.get(i);
			if((l1.getExternal() == external) && (l1.getInternal() == internal)) {
				l = l1;
			}
		}
		return l.getDistance();
	}
	
	private class Link{
		int external;
		int internal;
		int distance;
		
		Link(int external, int internal, int distance){
			this.external = external;
			this.internal = internal;
			this.distance = distance;
		}
		int getExternal() {
			return external;
		}
		int getInternal() {
			return internal;
		}
		int getDistance() {
			return distance;
		}
	}
	
	int getUp() {
		return up;
	}
	int getDown() {
		return down;
	}
	int getLeft() {
		return left;
	}
	int getRight() {
		return right;
	}
	int getController() {
		return controller;
	}
	int getController2() {
		return controller2;
	}
	
	String getConnection(int localPort) {
		String ret = null;
		Link l = null;
		for(int i=0; i<links.size(); i++) {
			if(links.get(i).internal == localPort) {
				l = links.get(i);
			}
		}
		if(l!=null) {
			int ext = l.external;
			for(int i=0; i<Node.routers.size(); i++) {
				Router r = Node.routers.get(i);
				ArrayList<Integer> ports = r.getPorts();
				for(int j=0; j<ports.size(); j++) {
					if(ext==ports.get(j)) {
						ret = r.name;
					}
				}
			}
			for(int i=0; i<Node.endUsers.size(); i++) {
				EndUser u = Node.endUsers.get(i);
				int port = u.getSrcPort();
				if(ext==port) {
					ret = u.name;
				}
			}
		}
		return ret;
	}
	
	String getConnectionLeft() {
		return getConnection(left);
	}
	
	String getConnectionUp() {
		return getConnection(up);	
	}
	
	String getConnectionRight() {
		return getConnection(right);
	}
	
	String getConnectionDown() {
		return getConnection(down);
	}
	
	ArrayList<Integer> getPorts(){
		return ports;
	}
	int getRouterNumber() {
		return routerNumber;
	}
	ArrayList<Integer> getConnections(){
		return connections;
	}
	
	void sendAll(int destFinal) {
		
		DatagramPacket packetToSend = null;
		
		byte[] payload= new byte[100];
		byte[] header= new byte[PacketContent.HEADERLENGTH];
		byte[] buffer= new byte[payload.length + header.length];
	
		dstAddress = new InetSocketAddress(Node.DEFAULT_HOST, Node.CONTROLLER_PORT);
			
		if(connections.size()==4) {
			ByteBuffer bb = ByteBuffer.allocate(4);
			bb.order(ByteOrder.LITTLE_ENDIAN).putInt(routerNumber);
		    byte[] routerNo = bb.array();
			    
		    ByteBuffer bb2 = ByteBuffer.allocate(4);
			bb2.order(ByteOrder.LITTLE_ENDIAN).putInt(links.get(0).external);
			byte[] conn1 = bb2.array();
			
			ByteBuffer bb3 = ByteBuffer.allocate(4);
			bb3.order(ByteOrder.LITTLE_ENDIAN).putInt(links.get(1).external);
		    byte[] conn2 = bb3.array();
			    
		    ByteBuffer bb4 = ByteBuffer.allocate(4);
			bb4.order(ByteOrder.LITTLE_ENDIAN).putInt(links.get(2).external);
			byte[] conn3 = bb4.array();
				
			ByteBuffer bb5 = ByteBuffer.allocate(4);
			bb5.order(ByteOrder.LITTLE_ENDIAN).putInt(links.get(3).external);
		    byte[] conn4 = bb5.array();
		    
		    ByteBuffer bb6 = ByteBuffer.allocate(4);
			bb6.order(ByteOrder.LITTLE_ENDIAN).putInt(destFinal);
		    byte[] dest = bb6.array();
		    
		    System.arraycopy(routerNo, 0, payload, 0, 2);
		    System.arraycopy(conn1, 0, payload, 2, 2);
		    System.arraycopy(conn2, 0, payload, 4, 2);
		    System.arraycopy(conn3, 0, payload, 6, 2);
		    System.arraycopy(conn4, 0, payload, 8, 2);
		    System.arraycopy(dest, 0, payload, 10, 2);
		    header[5] = 1;
		    if(isFirstRouter) {
		    	header[7] = 1;
		    }
		    System.arraycopy(header, 0, buffer, 0, header.length);
			System.arraycopy(payload, 0, buffer, header.length, payload.length);
				
			packetToSend= new DatagramPacket(buffer, buffer.length, dstAddress);
			try {
				socket5.send(packetToSend);
				terminal.println("Sent to Controller");
				this.hasSentAll = true;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else {
			terminal.println("Wrong number of connections");
		}	
		if(routerNumber==8) {
			send_all = false;
		}
	}
	
	public int searchLinksForInternal(int external) {
		
		int ret = -1;
		for(int i=0; i<links.size(); i++) {
			Link l = links.get(i);
			if(external == l.external) {
				ret = l.internal;
			}
		}
		return ret;
	}	
}