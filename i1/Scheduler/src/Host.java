/*
 * SYSC3303 - Scheduler
 * Author: Martin Klamrowski
 * Date: 29/1/2019 
 */

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * 
 * 
 * @author Marti
 *
 */
public class Host {
	
	/* ## HEADER AND COMMAND IDENTIFIERS ## */
	public static final String ACK = "1";
	public static final String CMD = "2";
	public static final String DATA = "3";
	public static final String ERROR = "0";
	
	public static final String FLOOR_BUTTON = "0x10";						// button was pressed on floor
	public static final String ELEVATOR_ARRIVED = "0x11"; 					// elevator arrived
	public static final String UP_PICKUP = "0x33";							// going up to pick up
	public static final String UP_DROPOFF = "0x32"; 						// going up dropoff
	public static final String DOWN_PICKUP = "0x31";						// going down to pickup
	public static final String DOWN_DROPOFF = "0x30";						// going down to dropoff
	public static final String DOOR_OPEN = "0x3A";							// open car door
	public static final String DOOR_CLOSE = "0x3B";							// close car door
	public static final String STOP = "0x3C";								// stop elevator car
	/* ## ------------------------------ ## */

	public static int currentFloor = 0;
	BlockingQueue<String> upQ = new ArrayBlockingQueue<String>(100);		// queue with up requests
	BlockingQueue<String> downQ = new ArrayBlockingQueue<String>(100);		// queue with down requests
	
	
	final int HOSTPORT = 8008;
	final int EPORT = 3137;
	final int FPORT = 6520;
	DatagramSocket hostSocket;												// receiving from floor system on this socket
	DatagramSocket floorSocket;												// sending to floor on this socket
	
	
	/**
	 * Constructor.
	 */
	public Host() {
		
		try {
			hostSocket = new DatagramSocket(HOSTPORT);
			//TODO eSocket = new DatagramSocket();
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}		
	}
	
	
	/**
	 * Run this shit.
	 */
	public void run() {
		System.out.println("main: running.");
		boolean listening = true;
		
		byte[] buffer = new byte[8];
		DatagramPacket rPacket = new DatagramPacket(buffer, buffer.length);			// received packet
		
		// object performing elevator movement calculations in separate thread
		Scheduler scheduler = new Scheduler(EPORT, FPORT, upQ, downQ);
		scheduler.start();
		
		
		while (listening) {
			try {
				hostSocket.receive(rPacket);
				System.out.println(String.format("main: received packet ( string >> %s, byte array >> %s ).\n", new String(rPacket.getData()), rPacket.getData()));
				
				// handling packet
				String[] rPacketParsed = parsePacket(rPacket.getData());
				
				// CMD
				if (rPacketParsed[0].equals(CMD)) {					
					
					handleFloorCommand(rPacketParsed[1], rPacket.getPort());					
					
				}
				// something else
				else {
					System.out.println("main: don't know what i got.");
				}
			}
			catch (Exception e) {
				e.printStackTrace();
				listening = false;
			}			
		}
		hostSocket.close();
		return;
	}
	
	
	/**
	 * Method to handle command received from floor. Mainly to keep code tidy.
	 * 
	 * @param cmd
	 * @param port
	 */
	public void handleFloorCommand(String cmd, int port) {
		
		byte[] data = new byte[1024];
		DatagramPacket aPacket = createPacket(ACK, cmd, port);		
		DatagramPacket dPacket = new DatagramPacket(data, data.length);
				
		if (cmd.equals(FLOOR_BUTTON)) {
			try {
				floorSocket = new DatagramSocket();
				System.out.println("main: acking.");
				floorSocket.send(aPacket);
				
				// get data
				hostSocket.receive(dPacket);
				
				// handling packet
				String[] dPacketParsed = parsePacket(dPacket.getData());				
				System.out.println(String.format("main: received data ( string >> %s, byte array >> %s ).\n", new String(dPacket.getData()), dPacket.getData()));
				System.out.println(Arrays.toString(dPacketParsed));
				
				if (dPacketParsed[0].equals(DATA)) {
					aPacket = createPacket(ACK, dPacketParsed[1], port);
					System.out.println("main: acking data.");
					floorSocket.send(aPacket);
					
					// determine which queue to add request to
					String[] temp = dPacketParsed[1].split(" ");
					if (temp[2].equals("UP")) {
						upQ.add(dPacketParsed[1]);						
					}
					else {
						downQ.add(dPacketParsed[1]);
					}
				}
			}
			catch (Exception e) {
				e.printStackTrace();
				System.exit(1);				
			}						
		}		
	}
	
	
	/**
	 * Method to parse a packet. Returns a String array.
	 * 
	 * @param bytes
	 * @return string array of data elements
	 */
	public static String[] parsePacket(byte[] bytes) {
		
		String packet = new String(bytes);
		String[] parsed = packet.replaceFirst("\0", "").split("\0");
		
		return parsed;		
	}
	
	
	/**
	 * Creates and returns a DatagramPacket.
	 * 
	 * @param packetType
	 * @return packet with data
	 */
	public static DatagramPacket createPacket(String packetType, String ins, int port) {
		String data;
		DatagramPacket packet = null;
		
		// error
		if (packetType == "0") {			
			data = "\0" + ERROR + "\0" + ins + "\0";					
		}
		// ack
		else if (packetType == "1") {			
			data = "\0" + ACK + "\0" + ins + "\0";
		}
		// cmd
		else if (packetType == "2") {
			data = "\0" + CMD + "\0" + ins + "\0";
		}
		// data
		else {
			data = "\0" + DATA + "\0" + ins + "\0";
		}		
		
		try {			
			packet = new DatagramPacket(data.getBytes(), data.getBytes().length, InetAddress.getLocalHost(), port);
		}
		catch (UnknownHostException uhe) {			
			System.out.println("unable to create packet (UnknownHostException), exiting.");
			System.exit(1);
		}
		return packet;
	}

	
	/**
	 * Main.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Host host = new Host();
		host.run();
	}
}




/**
 * Thread class to handle requests received.
 * 
 * @author Marti
 *
 */
class Scheduler extends Thread {
	
	private DatagramSocket eSocket;				// socket to communicate with elevator system
	private int eport;							// port of elevator system
	private int fport;							// port of floor system
	private BlockingQueue<String> upQ;			// queue with up requests
	private BlockingQueue<String> downQ;		// queue with down requests
	
	
	/**
	 * Constructor. Takes a port number and packet.
	 * 
	 * @param port
	 * @param packet
	 */
	public Scheduler(int eport, int fport, BlockingQueue<String> upQ, BlockingQueue<String> downQ) {
		super("request thread");
		try {
			eSocket = new DatagramSocket();
			this.upQ = upQ;
			this.downQ = downQ;
			this.eport = eport;
			this.fport = fport;
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}	
	
	
	@Override
	public void run() {
		
		boolean running = true;
		String ins;
		String srcFloor, direction, destFloor;
		String[] parsedData;
		System.out.println("sub: a new thread is running.");
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} //TODO remove
		
				
		while (running) {
			
			String request = upQ.poll();
			
			if (request != null) {
				parsedData = request.split(" ");
				srcFloor = parsedData[1];
				direction = parsedData[2];
				destFloor = parsedData[3];
				
				/*
				 *  determining which way the elevator needs to go to pickup
				 *	DATA packet format: '\3\"time src_floor direction dest_floor"\'
				 *	
				 *	if elevator is below src_floor - need to move elevator up
				 *	if elevator is above src_floor - need to move elevator down 
				 */
				if (direction.equals("UP")) {
					if (Integer.parseInt(srcFloor) > Host.currentFloor) {
						ins = Host.UP_PICKUP;
					}
					else if (Integer.parseInt(srcFloor) < Host.currentFloor) {
						ins = Host.DOWN_PICKUP;
					}
					else {
						ins = Host.STOP;	//TODO special case not handled
					}
				}
				// down
				else {
					if (Integer.parseInt(srcFloor) > Host.currentFloor) {
						ins = Host.UP_PICKUP;
					}
					else if (Integer.parseInt(srcFloor) < Host.currentFloor) {
						ins = Host.DOWN_PICKUP;
					}
					else {
						ins = Host.STOP;	//TODO special case not handled
					}
				}				
				handleRequest(ins, request);
			}
			else {
				continue;
			}
		}			
		eSocket.close();
		return;
	}
	
	
	/**
	 * 
	 * @param ins
	 * @param port
	 */
	public void handleRequest(String ins, String request) {
		
		String srcFloor, direction, destFloor;
		String[] parsedData;
		boolean keepMoving = true;
		
		parsedData = request.split(" ");
		srcFloor = parsedData[1];
		direction = parsedData[2];
		destFloor = parsedData[3];
		
		byte[] buffer = new byte[8];
		DatagramPacket cPacket = Host.createPacket(Host.CMD, ins, eport);
		DatagramPacket aPacket = new DatagramPacket(buffer, buffer.length);
		DatagramPacket dPacket = Host.createPacket(Host.DATA, srcFloor, eport);
		DatagramPacket rPacket = new DatagramPacket(buffer, buffer.length);
		String[] rPacketParsed;
		
		System.out.println(String.format("sub: forwarding command packet ( string >> %s, byte array >> %s ).", new String(cPacket.getData()), cPacket.getData()));
		
		try {
			// send cmd
			eSocket.send(cPacket);
			
			// listen for ack
			eSocket.receive(aPacket);
			
			// parsing ack
			String[] aPacketParsed = Host.parsePacket(aPacket.getData());				
			System.out.println(String.format("sub: received ack ( string >> %s, byte array >> %s ).", new String(aPacket.getData()), aPacket.getData()));
			System.out.println(Arrays.toString(aPacketParsed));
			
			// send elevator destination
			System.out.println(String.format("sub: sending elevator button ( string >> %s, byte array >> %s ).", new String(dPacket.getData()), dPacket.getData()));
			eSocket.send(dPacket);
			
			// listen for ack
			eSocket.receive(aPacket);
			
			// parsing ack
			aPacketParsed = Host.parsePacket(aPacket.getData());				
			System.out.println(String.format("sub: received ack ( string >> %s, byte array >> %s ).", new String(aPacket.getData()), aPacket.getData()));
			System.out.println(Arrays.toString(aPacketParsed));
			
			// positional updates
			while (keepMoving) {
				eSocket.receive(rPacket);
				
				rPacketParsed = Host.parsePacket(rPacket.getData());				
				System.out.println(String.format("sub: received positional update ( string >> %s, byte array >> %s ).", new String(rPacket.getData()), rPacket.getData()));
				System.out.println(Arrays.toString(rPacketParsed));
				
				// if elevator is at required floor then stop
				if (rPacketParsed[1].equals(srcFloor)) {
					cPacket = Host.createPacket(Host.CMD, Host.STOP, eport);
					System.out.println(String.format("sub: sending stop ( string >> %s, byte array >> %s ).\n", new String(cPacket.getData()), cPacket.getData()));
					keepMoving = false;
				}
				else {
					System.out.println(String.format("sub: sending continue ( string >> %s, byte array >> %s ).\n", new String(cPacket.getData()), cPacket.getData()));
					cPacket = Host.createPacket(Host.ACK, rPacketParsed[1], eport);
				}				
				eSocket.send(cPacket);
			}
			
			// listen for ack to stop
			eSocket.receive(aPacket);
			
			// parsing ack
			aPacketParsed = Host.parsePacket(aPacket.getData());				
			System.out.println(String.format("sub: received ack ( string >> %s, byte array >> %s ).", new String(aPacket.getData()), aPacket.getData()));
			System.out.println(Arrays.toString(aPacketParsed));
			
			// send update to floor that the elevator has arrived
			sendPositionToFloor(srcFloor);			
			
			// sending open door
			cPacket = Host.createPacket(Host.CMD, Host.DOOR_OPEN, eport);
			System.out.println(String.format("sub: sending open door ( string >> %s, byte array >> %s ).\n", new String(cPacket.getData()), cPacket.getData()));
			eSocket.send(cPacket);
			
			// listen for ack to open
			eSocket.receive(aPacket);
			
			// parsing ack
			aPacketParsed = Host.parsePacket(aPacket.getData());				
			System.out.println(String.format("sub: received ack ( string >> %s, byte array >> %s ).", new String(aPacket.getData()), aPacket.getData()));
			System.out.println(Arrays.toString(aPacketParsed));
			
			// sending close door
			cPacket = Host.createPacket(Host.CMD, Host.DOOR_CLOSE, eport);
			System.out.println(String.format("sub: sending close door ( string >> %s, byte array >> %s ).\n", new String(cPacket.getData()), cPacket.getData()));
			eSocket.send(cPacket);
			
			// listen for ack to close
			eSocket.receive(aPacket);
			
			// parsing ack
			aPacketParsed = Host.parsePacket(aPacket.getData());				
			System.out.println(String.format("sub: received ack - done ( string >> %s, byte array >> %s ).", new String(aPacket.getData()), aPacket.getData()));
			System.out.println(Arrays.toString(aPacketParsed));
			
			
		}
		catch (Exception e) {
			System.out.println("sub: unable to communicate with elevator system, aborting request.");
			e.printStackTrace();
			return;
		}		
	}
	
	
	/**
	 * Send position to floor.
	 * 
	 * @param floor
	 */
	public void sendPositionToFloor(String floor) {
		
		byte[] buffer = new byte[8];
		DatagramPacket sPacket = Host.createPacket(Host.CMD, Host.ELEVATOR_ARRIVED, fport);		
		DatagramPacket rPacket = new DatagramPacket(buffer, buffer.length);
		String[] rPacketParsed;
		
		
		try {
			DatagramSocket tempSocket = new DatagramSocket();
			System.out.println(String.format("sub: sending position update to floor ( string >> %s, byte array >> %s ).\n", new String(sPacket.getData()), sPacket.getData()));
			tempSocket.send(sPacket);
			
			// listen for ack
			tempSocket.receive(rPacket);
			
			// parsing ack
			rPacketParsed = Host.parsePacket(rPacket.getData());				
			System.out.println(String.format("sub: received ack ( string >> %s, byte array >> %s ).", new String(rPacket.getData()), rPacket.getData()));
			System.out.println(Arrays.toString(rPacketParsed));
			
			// sending floor number
			sPacket = Host.createPacket(Host.DATA, floor, fport);
			System.out.println(String.format("sub: sending floor number ( string >> %s, byte array >> %s ).\n", new String(sPacket.getData()), sPacket.getData()));
			tempSocket.send(sPacket);
			
			// listen for ack
			tempSocket.receive(rPacket);
			
			// parsing ack
			rPacketParsed = Host.parsePacket(rPacket.getData());				
			System.out.println(String.format("sub: received ack - done ( string >> %s, byte array >> %s ).", new String(rPacket.getData()), rPacket.getData()));
			System.out.println(Arrays.toString(rPacketParsed));
			
			tempSocket.close();
			return;
		} 
		catch (Exception e) {			
			e.printStackTrace();
		}		
	}
} 