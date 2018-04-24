import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import tcdIO.*;

/**
 * @author Sean Candon
 *
 */
public class Controller extends Node implements Runnable{
	
	int port, port2, destinationPort, destConn;
	InetSocketAddress receivedFrom;
	Terminal terminal;
	ArrayList<Row> rows;
	ArrayList<Router> routersOnPath, allRouters;
	ArrayList<Node> nodes;
	EndUser destination;
	Router destRouter, firstRouter;
	
	Controller(Terminal terminal, int port, int port2){
		try {
			this.port = port;
			this.port2 = port2;
			this.terminal = terminal;
			socket = new DatagramSocket(port);
			rows = new ArrayList<Row>();
			routersOnPath = new ArrayList<Router>();
			allRouters = new ArrayList<Router>();
			nodes = new ArrayList<Node>();
			
			listener.go();
			
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
	
	public synchronized void onReceipt(DatagramPacket packet) {
		
		PacketContent pk = new PacketContent(packet);
		if(pk.getRouterNo()==0) {
			destinationPort = pk.getDestPort();
			destination = pk.getDestUser();
			destRouter = destination.closestRouter;	
			destConn = destination.getConnection();
			receivedFrom = (InetSocketAddress) packet.getSocketAddress();	
		}
		Row r = new Row(getRouterWithNumber(pk.getRouterNo()), pk.getLeft(), pk.getUp(), 
				pk.getRight(), pk.getDown());
		rows.add(r);
		allRouters.add(getRouterWithNumber(pk.getRouterNo()));	
		terminal.println(pk.getRouterNo() + " | " + pk.getLeft() + " | " + pk.getUp()
		 + " | " + pk.getRight() + " | " + pk.getDown());
		terminal.println("Received from " + packet.getPort());
		
		
		if(pk.getRouterNo()==8) {
			setPath();
		}	
	}
	
	public synchronized void start() {
		
		int x=0;
		while(x>=0){
			terminal.println("Waiting for contact");
			try {
				this.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void run() {
		this.start();
	}
	
	void reset() {
		rows = new ArrayList<Row>();
		routersOnPath = new ArrayList<Router>();
		allRouters = new ArrayList<Router>();
		nodes = new ArrayList<Node>();
	}
	
	static Router getRouter(int port) {
		Router ret = null;
		for(int i=0; i<routers.size(); i++) {
			Router r = routers.get(i);
			ArrayList<Integer> ports = r.getPorts();
			for(int j=0; j<ports.size(); j++) {
				if(port == ports.get(j)) {
					ret = r; 
				}
			}	
		}
		return ret;
	}
	
	static EndUser getUser(int port) {
		EndUser ret = null;
		for(int i=0; i<endUsers.size(); i++) {
			EndUser u = endUsers.get(i);
			if(port == u.getSrcPort()) {
				ret = u;
			}	
		}
		return ret;
	}
	
	static Router getRouterWithNumber(int routerNo) {
		
		Router ret = null;
		for(int i=0; i<routers.size(); i++) {
			Router r = routers.get(i);
			if(r.getRouterNumber() == routerNo) {
				ret = r;
			}	
		}
		return ret;	
	}
	
	static EndUser getUserWithNumber(int userNo) {
		
		EndUser ret = null;
		for(int i=0; i<endUsers.size(); i++) {
			EndUser u = endUsers.get(i);
			if(u.getUserNumber() == userNo) {
				ret = u;
			}	
		}
		return ret;	
	}
	
	private class Row {
		
		Router r;
		int left, up, right, down;
		
		Row(Router r, int left, int up, int right, int down) {
			this.r = r;
			this.left = left;
			this.up = up;
			this.right = right;
			this.down = down;
		}
	}
	
	int findExternal(Router r1, Router r2) {
		
		int ret = 0;
		ArrayList<Integer> r2Ports = r2.getPorts();
		
		for(int i=0; i<r2Ports.size(); i++) {
			int internal = r1.searchLinksForInternal(r2Ports.get(i));
			if(internal != -1) {
				ret = r2Ports.get(i);
			}	
		}
		return ret;	
	}
	
	public void setPath() {
		
		ArrayList<Router> routersOnCourse = generatePath();
		
		for(int i=routersOnCourse.size()-1; i>=0; i--) {
			if(i>0) {
				Router r1 = routersOnCourse.get(i);
				Router r2 = routersOnCourse.get(i-1);
				int dest = findExternal(r1, r2);
				sendInstructions(r1, /*r2.getLeft()*/dest);	
			}
			else {
				Router r = routersOnCourse.get(i);
				sendFinalInstructions(r);
			}	
		}
		sendAuthorization(routersOnCourse);	
	}	
	
	public void sendInstructions(Router r, int dest) {
		
		int port = r.searchLinksForInternal(dest);
		byte[] header = new byte[PacketContent.HEADERLENGTH];
		byte[] payload = new byte[10];
		byte[] buffer = new byte[header.length + payload.length];
		
		ByteBuffer bb5 = ByteBuffer.allocate(4);
		bb5.order(ByteOrder.LITTLE_ENDIAN).putInt(port);
	    byte[] p = bb5.array();
	   
	    ByteBuffer bb6 = ByteBuffer.allocate(4);
		bb6.order(ByteOrder.LITTLE_ENDIAN).putInt(dest);
	    byte[] d = bb6.array();
	    
	    System.arraycopy(p, 0, payload, 0, 2);
	    System.arraycopy(d, 0, payload, 2, 2);
	    header[4] = 2;
	    System.arraycopy(header, 0, buffer, 0, header.length);
		System.arraycopy(payload, 0, buffer, header.length, payload.length);
		DatagramPacket packet1;
		packet1 = new DatagramPacket(buffer, buffer.length, 
					new InetSocketAddress(DEFAULT_HOST, r.getSocketPort()));
		try {
			socket.send(packet1);
		} catch (IOException e) {
			terminal.println("Problem sending instructions");
		}	
	}
	
	void sendFinalInstructions(Router r) {
		
		int dest = destination.getSrcPort();
		int port = r.searchLinksForInternal(dest);
		byte[] header = new byte[PacketContent.HEADERLENGTH];
		byte[] payload = new byte[10];
		byte[] buffer = new byte[header.length + payload.length];
		ByteBuffer bb5 = ByteBuffer.allocate(4);
		bb5.order(ByteOrder.LITTLE_ENDIAN).putInt(port);
	    byte[] p = bb5.array();
	    ByteBuffer bb6 = ByteBuffer.allocate(4);
		bb6.order(ByteOrder.LITTLE_ENDIAN).putInt(dest);
	    byte[] d = bb6.array();
	    System.arraycopy(p, 0, payload, 0, 2);
	    System.arraycopy(d, 0, payload, 2, 2);
	    header[4] = 2;
	    header[7] = 2;
	    System.arraycopy(header, 0, buffer, 0, header.length);
		System.arraycopy(payload, 0, buffer, header.length, payload.length);
		DatagramPacket packet1 = new DatagramPacket(buffer, buffer.length, 
				new InetSocketAddress(DEFAULT_HOST, r.getSocketPort()));
		try {
			socket.send(packet1);
		} catch (IOException e) {
			terminal.println("Problem sending instructions");
		}		
	}
	
	void sendAuthorization(ArrayList<Router> rs) {
		
		byte[] header = new byte[PacketContent.HEADERLENGTH];
		byte[] payload = new byte[10];
		byte[] buffer = new byte[header.length + payload.length];
	    header[4] = 3; 
	    System.arraycopy(header, 0, buffer, 0, header.length);
		System.arraycopy(payload, 0, buffer, header.length, payload.length);
		
		for(int i=0; i<rs.size(); i++) {
			Router r = rs.get(i);
			DatagramPacket packet1 = new DatagramPacket(buffer, buffer.length, 
					new InetSocketAddress(DEFAULT_HOST, r.getSocketPort()));
			try {
				socket.send(packet1);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}		
	}
	
	private class Node {
		
		private Router r;
		private int val;
		ArrayList<Node> connections;
		Node base;
		Router baseRouter;
		boolean isFirst, isLast, valHasBeenChanged;
		
		Node(Router r){
			this.r = r;
			this.val = 100;
			this.connections = new ArrayList<Node>();
			base = null;
			baseRouter = null;
			this.valHasBeenChanged = false;
			if(r.isFirstRouter) {
				this.isFirst = true;
			}
			else {
				this.isFirst = false;
			}
			
			if(r.isLastRouter) {
				this.isLast = true;
			}
			else {
				this.isLast = false;
			}
		}
		
		Router getRouter() {
			return r;
		}
		int getVal() {
			return val;
		}
		
		Node getBase() {
			return base;
		}
		
		void setVal(int v) {
			if(v<val) {
				val = v;
				valHasBeenChanged = true;
			}
			else {
				valHasBeenChanged = false;
			}	
		}
		
		void addConnection(Node n) {
			connections.add(n);
			n.setBase(this);
		}
		
		void setBase(Node n) {
			base = n;
			baseRouter = n.getRouter();
		}
		
		boolean isBaseOf(Node n) {
			for(int i=0; i<connections.size(); i++) {
				if(n.equals(connections.get(i))) {
					return true;
				}
			}
			return false;
		}
		
		boolean hasConnections() {
			if(connections.size()!=0) {
				return true;
			}
			return false;
		}
		
		boolean hasBase() {
			if(base!=null) {
				return true;
			}
			return false;
		}
		
		public String toString() {
			String s = new String();
			if(this.base != null) {
				s += "Router: " + this.r.name + " Base: " + this.base.getRouter().name + " Val: " + this.val;
			}
			else {
				s += "Router: " + this.r.name + " Val: " + this.val;
			}
			return s;
		}	
	}
	
	void setNodes() {
		
		for(int i=0; i<allRouters.size(); i++) {
			Router r = allRouters.get(i);
			Node n;
			if(r.isFirstRouter) {
				n = new Node(r);
				n.setVal(0);
				nodes.add(n);
			}
			else {
				n = new Node(r);
				nodes.add(n);
			}	
		}	
	}
	
	void updateNeighbours(Node n) {
		
		Router r = n.getRouter();
		if(!(r.name).equals(destRouter.name)) {
			ArrayList<Integer> conns = r.getConnections();
			
			for(int i=0; i<conns.size(); i++) {
				if(getRouter(conns.get(i)) != null) {
					int ext = conns.get(i);
					int loc = r.searchLinksForInternal(ext);
					int dis = r.getDistanceOfLink(ext, loc);
					
					for(int j=0; j<nodes.size(); j++) {
						Router r1 = nodes.get(j).getRouter();
						ArrayList<Integer> locals = r1.getPorts();
						Node node = nodes.get(j);
						
						for(int k=0; k<locals.size(); k++) {
							if(ext == locals.get(k) && !node.isBaseOf(n)) {
								node.setVal(dis + n.getVal());
								if(node.valHasBeenChanged)
									n.addConnection(node);
								nodes.set(j, node);
							}		
						}
					}
				}
			}
		}	
	}
	
	public ArrayList<Router> generatePath() {
		
		this.setNodes();
		Node first = null;
		for(int i=0; i<nodes.size(); i++) {
			Node n = nodes.get(i);
			if(n.isFirst)
				first = n;
		}
		generatePath(first);
		return getPathFromNodes();
	}
	
	private void generatePath(Node n) {
		
		boolean isLastOne = false;
		if(n.getRouter().name.equals(destRouter.name)) {
			isLastOne =true;
		}	
		updateNeighbours(n);
		if(n.hasConnections() && !isLastOne) {
			for(int i=0; i<n.connections.size(); i++) {
				Node n1 = n.connections.get(i);
				generatePath(n1);
			}
		}
		
	}
	
	private void savePath(Node n, ArrayList<Router> path) {
		
		path.add(n.getRouter());
		if(n.hasBase()) {
			savePath(n.getBase(), path);
		}
	}
	
	ArrayList<Router> getPathFromNodes(){
		
		Node last = null;
		ArrayList<Router> path = new ArrayList<Router>();
		for(int i=0; i<nodes.size(); i++) {
			Node n = nodes.get(i);
			if(n.getRouter().equals(destRouter)) {
				last = n;
			}
		}
		savePath(last, path);
		return path;	
	}
}