/*
 * SYSC3303 - ElevatorHandler
 * Author: Martin Klamrowski
 * CoAuthor: Angie Byun
 * Date: 29/1/2019 
 */
package Scheduler;

import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.math.*;

/**
 * Class acting as the interface for the FloorSubsystem. Submits new requests to the ElevatorHandler.
 * 
 * @author Marti
 *
 */
public class Scheduler {
	
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
	
	/* ## NETWORK INFO ## */
	final int HOSTPORT = 8008;
	final int EPORT1 = 3137;
	final int EPORT2 = 3237;
	final int EPORT3 = 3337;
	final int EPORT4 = 3437;
	final int FPORT = 6520;
	DatagramSocket hostSocket;												// receiving from floor system on this socket
	DatagramSocket floorSocket;												// sending to floor on this socket
	/* ## ------------ ## */
	
	/* ## ELEVATOR SYSTEM CONSTITUENTS ## */
	public static final int FLOORS = 22;									// floors are 1-22
	private BlockingQueue<String> upQ1;										// queue with up requests (elevator 1)
	private BlockingQueue<String> downQ1;									// queue with down requests (elevator 1)
	private BlockingQueue<String> upQ2;										// queue with up requests (elevator 2)
	private BlockingQueue<String> downQ2;									// queue with down requests (elevator 2)
	private BlockingQueue<String> upQ3;										// queue with up requests (elevator 3)
	private BlockingQueue<String> downQ3;									// queue with down requests (elevator 3)
	private BlockingQueue<String> upQ4;										// queue with up requests (elevator 4)
	private BlockingQueue<String> downQ4;									// queue with down requests (elevator 4)
	
	// objects performing elevator movement calculations for each elevator in separate thread
	//private ArrayList<ElevatorHandler> handlers;
	private ElevatorHandler handler1;
	private ElevatorHandler handler2;
	private ElevatorHandler handler3;
	private ElevatorHandler handler4;
	/* ## ---------------------------- ## */
	
	
	/**
	 * Constructor.
	 */
	public Scheduler() {
		
		try {
			hostSocket = new DatagramSocket(HOSTPORT);
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		upQ1 = new ArrayBlockingQueue<String>(100);
		downQ1 = new ArrayBlockingQueue<String>(100);
		upQ2 = new ArrayBlockingQueue<String>(100);
		downQ2 = new ArrayBlockingQueue<String>(100);
		upQ3 = new ArrayBlockingQueue<String>(100);
		downQ3 = new ArrayBlockingQueue<String>(100);
		upQ4 = new ArrayBlockingQueue<String>(100);
		downQ4 = new ArrayBlockingQueue<String>(100);
		
		handler1 = new ElevatorHandler(EPORT1, FPORT, upQ1, downQ1, 1, "IDLE", 1);
		handler2 = new ElevatorHandler(EPORT2, FPORT, upQ2, downQ2, 1, "IDLE", 2);
		handler3 = new ElevatorHandler(EPORT3, FPORT, upQ3, downQ3, 10, "IDLE", 3);
		handler4 = new ElevatorHandler(EPORT4, FPORT, upQ4, downQ4, 20, "IDLE", 4);
	}
	
	
	/**
	 * Main process.
	 */
	public void run() {
		System.out.println("main: running.");
		boolean listening = true;
		
		byte[] buffer = new byte[8];
		DatagramPacket rPacket = new DatagramPacket(buffer, buffer.length);			// received packet		
		
		handler1.start();
		handler2.start();
		handler3.start();
		handler4.start();
		
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
					System.out.println("main: unknown packet.");
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
	 * Method to handle command received from floor. Computes the suitability of each elevator to service a floor request, adds the request to the corresponding elevator.
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
					
					// determine which elevator queue to add request to
					
					/* ###################################################### */
					/* MAIN ELEVATOR SCHEDULING ALGORITHM //TODO */
					/* ###################################################### */				
					
					String[] temp = dPacketParsed[1].split(" ");
					
					int FS1 = calculateSuitability(FLOORS, handler1.currentFloor, Integer.parseInt(temp[1]), handler1.currentDirection, temp[2]);
					int FS2 = calculateSuitability(FLOORS, handler2.currentFloor, Integer.parseInt(temp[1]), handler2.currentDirection, temp[2]);
					int FS3 = calculateSuitability(FLOORS, handler3.currentFloor, Integer.parseInt(temp[1]), handler3.currentDirection, temp[2]);
					int FS4 = calculateSuitability(FLOORS, handler4.currentFloor, Integer.parseInt(temp[1]), handler4.currentDirection, temp[2]);
					int maxFS = Math.max(Math.max(FS1, FS2), Math.max(FS3, FS4));
					
					if (maxFS == FS1) {
						if (temp[2].equals("UP")) {
							upQ1.add(dPacketParsed[1]);
						}
						else {
							downQ1.add(dPacketParsed[1]);
						}						
					}
					else if (maxFS == FS2) {
						if (temp[2].equals("UP")) {
							upQ2.add(dPacketParsed[1]);
						}
						else {
							downQ2.add(dPacketParsed[1]);
						}						
					}
					else if (maxFS == FS3) {
						if (temp[2].equals("UP")) {
							upQ3.add(dPacketParsed[1]);
						}
						else {
							downQ3.add(dPacketParsed[1]);
						}						
					}
					else {
						if (temp[2].equals("UP")) {
							upQ4.add(dPacketParsed[1]);
						}
						else {
							downQ4.add(dPacketParsed[1]);
						}						
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
	 * Calculates the suitability of an elevator to service a request based on its distance and direction.
	 * 
	 * @param n - number of floors
	 * @param currentFloor - current floor
	 * @param requestedFloor - requested floor
	 * @param currentElevatorDir - current direction of elevator
	 * @param requestedDir - requested direction
	 * @return calculated
	 */
	public int calculateSuitability(int n, int currentFloor, int requestedFloor, String currentElevatorDir, String requestedDir) {
		
		int calculated = 0;
		int distance = currentFloor - requestedFloor; // if -ive, call is above, if +ive, call is below
		
		// current direction of elevator is IDLE
		if (currentElevatorDir.equals("IDLE")) {
			distance = Math.abs(distance);
			calculated = n + 1 - distance;
		}
		// current direction of elevator is DOWN
		else if (currentElevatorDir.equals("DOWN")) {
			if (distance < 0) {
				calculated = 1;
			}
			// request is below and request direction is same as elevator direction
			else if (distance > 0 && requestedDir.equals(currentElevatorDir)) {
				calculated = n + 2 - distance;
			}
			// request is below and request direction is NOT the same as elevator direction
			else if (distance > 0 && !requestedDir.equals(currentElevatorDir)) {
				calculated = n + 1 - distance;
			}
		}
		// current direction of elevator is UP
		else if (currentElevatorDir.equals("UP")) {
			// request is below
			if (distance > 0) {
				calculated = 1;
			}
			// request is above and request direction is same as elevator direction 
			else if (distance < 0 && requestedDir.equals(currentElevatorDir)) {
				distance = Math.abs(distance);
				calculated = n + 2 - distance;
			}
			// request is above an request direction is NOT the same as elevator direction
			else if (distance < 0 && !requestedDir.equals(currentElevatorDir)) {
				distance = Math.abs(distance);
				calculated = n + 1 - distance;
			}
		}
		return calculated; // return calculated suitability
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
			System.out.println("unable to create packet (UnknownSchedulerException), exiting.");
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
		Scheduler scheduler = new Scheduler();
		scheduler.run();
	}
}




/**
 * Thread class to handle elevator movements. Uses a BlockingQueue to monitor new requests.
 * 
 * @author Marti
 *
 */
class ElevatorHandler extends Thread {
	
	private DatagramSocket eSocket;					// socket to communicate with elevator system
	private int eport;								// port of elevator system
	private int fport;								// port of floor system
	private BlockingQueue<String> upQ;				// queue with up requests
	private BlockingQueue<String> downQ;			// queue with down requests
	
	protected volatile String currentDirection;		// variable representing current direction of the elevator, to be accessed by the main thread as well
	protected volatile int currentFloor;			// variable representing current floor of the elevator, to be accessed by the main thread as well
	protected volatile int currentDestination;		// variable representing the last stop in the current request sequence
	protected int id;
	
	
	/**
	 * Constructor. Takes a port number, packet and an up and down request Queue.
	 * 
	 * @param port
	 * @param packet
	 * @param upQ
	 * @param downQ
	 */
	public ElevatorHandler(int eport, int fport, BlockingQueue<String> upQ, BlockingQueue<String> downQ, int currentFloor, String currentDirection, int id) {
		super("request thread");
		try {
			eSocket = new DatagramSocket();
			this.eport = eport;
			this.fport = fport;
			this.upQ = upQ;
			this.downQ = downQ;
			this.currentDirection = currentDirection;
			this.currentFloor = currentFloor;
			this.id = id;
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}	
	
	
	/**
	 * Main thread process. Executed on thread start.
	 */
	@Override
	public void run() {
		
		boolean running = true;
		String pIns, dIns;
		String srcFloor, direction;
		String[] parsedData;
		System.out.println("sub: a new thread is running.");		
				
		while (running) {
			
			String uq = upQ.poll();
			String dq = downQ.poll();
			String request = (dq == null) ? uq : dq;			
			
			if (request != null) {
				parsedData = request.split(" ");
				srcFloor = parsedData[1];
				direction = parsedData[2];
				
				/*
				 *  determining which way the elevator needs to go to pickup and dropoff //TODO
				 *	DATA packet format: '\3\"time src_floor direction dest_floor"\'
				 *	
				 *	if elevator is below src_floor - need to move elevator up
				 *	if elevator is above src_floor - need to move elevator down 
				 *
				 *	if dest_floor > src_floor - go up after pickup
				 *	if dest_floor < src_floor - go down after pickup
				 */				
				if (Integer.parseInt(srcFloor) > currentFloor) {
					pIns = Scheduler.UP_PICKUP;
					currentDirection = "UP";
				}
				else if (Integer.parseInt(srcFloor) < currentFloor) {
					pIns = Scheduler.DOWN_PICKUP;
					currentDirection = "DOWN";
				}
				else {
					pIns = Scheduler.STOP;	// elevator is already there
				}
				performPickup(pIns, request);
				
				// drop off direction
				if (direction.equals("UP")) {
					dIns = Scheduler.UP_DROPOFF;
					currentDirection = "UP";
				}
				else { //down
					dIns = Scheduler.DOWN_DROPOFF;
					currentDirection = "DOWN";
				}		
				performDropoff(dIns, request);
			}
			else {
				currentDirection = "IDLE";
				continue;
			}
		}			
		eSocket.close();
		return;
	}
	
	
	/**
	 * Method doing the bulk of the socket operations for a pickup.
	 * 
	 * @param ins
	 * @param request
	 */
	public void performPickup(String ins, String request) {
		
		String srcFloor, destFloor;
		String[] parsedData;
		boolean keepMoving = (ins.equals(Scheduler.STOP) ? false : true); // if the elevator is already there no need for positional updates//TODO (ins.equals(Scheduler.STOP) ? false : true)
		
		parsedData = request.split(" ");
		srcFloor = parsedData[1];
		destFloor = parsedData[3];
		
		byte[] buffer = new byte[8];
		DatagramPacket cPacket = Scheduler.createPacket(Scheduler.CMD, ins, eport); //TODO make this less fugly
		DatagramPacket aPacket = new DatagramPacket(buffer, buffer.length);
		DatagramPacket dPacket = Scheduler.createPacket(Scheduler.DATA, destFloor, eport);
		DatagramPacket rPacket = new DatagramPacket(buffer, buffer.length);
		String[] rPacketParsed;
		
		System.out.println(String.format("sub-%d: forwarding command packet ( string >> %s, byte array >> %s ).", this.id, new String(cPacket.getData()), cPacket.getData()));
		
		try {
			// send cmd
			eSocket.send(cPacket);
			
			// listen for ack
			eSocket.receive(aPacket);
			
			// parsing ack
			String[] aPacketParsed = Scheduler.parsePacket(aPacket.getData());				
			System.out.println(String.format("sub-%d: received ack ( string >> %s, byte array >> %s ).", this.id, new String(aPacket.getData()), aPacket.getData()));
			System.out.println(Arrays.toString(aPacketParsed));
			
			// send elevator destination
			System.out.println(String.format("sub-%d: sending elevator button ( string >> %s, byte array >> %s ).", this.id, new String(dPacket.getData()), dPacket.getData()));
			eSocket.send(dPacket);
			
			// listen for ack
			eSocket.receive(aPacket);
			
			// parsing ack
			aPacketParsed = Scheduler.parsePacket(aPacket.getData());				
			System.out.println(String.format("sub-%d: received ack ( string >> %s, byte array >> %s ).", this.id, new String(aPacket.getData()), aPacket.getData()));
			System.out.println(Arrays.toString(aPacketParsed));
			
			// positional updates
			while (keepMoving) {
				eSocket.receive(rPacket);
				
				rPacketParsed = Scheduler.parsePacket(rPacket.getData());				
				System.out.println(String.format("sub-%d: received positional update ( string >> %s, byte array >> %s ).", this.id, new String(rPacket.getData()), rPacket.getData()));
				System.out.println(Arrays.toString(rPacketParsed));
				
				currentFloor = Integer.parseInt(rPacketParsed[1]);
				// if elevator is at required floor then stop
				if (rPacketParsed[1].equals(srcFloor)) {
					cPacket = Scheduler.createPacket(Scheduler.CMD, Scheduler.STOP, eport);
					System.out.println(String.format("sub-%d: sending stop ( string >> %s, byte array >> %s ).\n", this.id, new String(cPacket.getData()), cPacket.getData()));
					keepMoving = false;
				}
				else {
					cPacket = Scheduler.createPacket(Scheduler.ACK, rPacketParsed[1], eport);
					System.out.println(String.format("sub-%d: sending continue ( string >> %s, byte array >> %s ).\n", this.id, new String(cPacket.getData()), cPacket.getData()));					
				}				
				eSocket.send(cPacket);
				
				if (!keepMoving) {
					// listen for ack to stop
					eSocket.receive(aPacket);
					
					// parsing ack
					aPacketParsed = Scheduler.parsePacket(aPacket.getData());				
					System.out.println(String.format("sub-%d: received ack ( string >> %s, byte array >> %s ).", this.id, new String(aPacket.getData()), aPacket.getData()));
					System.out.println(Arrays.toString(aPacketParsed));	
				}
			}					
			
			// send update to floor that the elevator has arrived
			sendPositionToFloor(srcFloor);
			
			// sending open door
			cPacket = Scheduler.createPacket(Scheduler.CMD, Scheduler.DOOR_OPEN, eport);
			System.out.println(String.format("sub-%d: sending open door ( string >> %s, byte array >> %s ).\n", this.id, new String(cPacket.getData()), cPacket.getData()));
			eSocket.send(cPacket);
			
			// listen for ack to open
			eSocket.receive(aPacket);
			
			// parsing ack
			aPacketParsed = Scheduler.parsePacket(aPacket.getData());				
			System.out.println(String.format("sub-%d: received ack ( string >> %s, byte array >> %s ).", this.id, new String(aPacket.getData()), aPacket.getData()));
			System.out.println(Arrays.toString(aPacketParsed));
			
			// sending close door
			cPacket = Scheduler.createPacket(Scheduler.CMD, Scheduler.DOOR_CLOSE, eport);
			System.out.println(String.format("sub-%d: sending close door ( string >> %s, byte array >> %s ).\n", this.id, new String(cPacket.getData()), cPacket.getData()));
			eSocket.send(cPacket);
			
			// listen for ack to close
			eSocket.receive(aPacket);
			
			// parsing ack
			aPacketParsed = Scheduler.parsePacket(aPacket.getData());				
			System.out.println(String.format("sub-%d: received ack - done ( string >> %s, byte array >> %s ).", this.id, new String(aPacket.getData()), aPacket.getData()));
			System.out.println(Arrays.toString(aPacketParsed));			
		}
		catch (Exception e) {
			System.out.println("sub: unable to communicate with elevator system, aborting request.");
			e.printStackTrace();
			return;
		}		
	}
	
	
	/**
	 * Method doing the bulk of the socket operations for a dropoff.
	 * 
	 * @param ins
	 * @param request
	 */
	public void performDropoff(String ins, String request) {
		String destFloor;
		String[] parsedData;
		boolean keepMoving = true;
		
		parsedData = request.split(" ");
		destFloor = parsedData[3];
		
		byte[] buffer = new byte[8];
		DatagramPacket cPacket = Scheduler.createPacket(Scheduler.CMD, ins, eport); //TODO make this less fugly
		DatagramPacket aPacket = new DatagramPacket(buffer, buffer.length);
		DatagramPacket rPacket = new DatagramPacket(buffer, buffer.length);
		String[] rPacketParsed;
		
		System.out.println(String.format("sub-%d: forwarding command packet ( string >> %s, byte array >> %s ).", this.id, new String(cPacket.getData()), cPacket.getData()));
		
		try {
			// send cmd
			eSocket.send(cPacket);
			
			// listen for ack
			eSocket.receive(aPacket);
			
			// parsing ack
			String[] aPacketParsed = Scheduler.parsePacket(aPacket.getData());				
			System.out.println(String.format("sub-%d: received ack ( string >> %s, byte array >> %s ).", this.id, new String(aPacket.getData()), aPacket.getData()));
			System.out.println(Arrays.toString(aPacketParsed));
			
			// positional updates
			while (keepMoving) {
				eSocket.receive(rPacket);
				
				rPacketParsed = Scheduler.parsePacket(rPacket.getData());				
				System.out.println(String.format("sub-%d: received positional update ( string >> %s, byte array >> %s ).", this.id, new String(rPacket.getData()), rPacket.getData()));
				System.out.println(Arrays.toString(rPacketParsed));
				
				currentFloor = Integer.parseInt(rPacketParsed[1]);
				// if elevator is at required floor then stop
				if (rPacketParsed[1].equals(destFloor)) {
					cPacket = Scheduler.createPacket(Scheduler.CMD, Scheduler.STOP, eport);
					System.out.println(String.format("sub-%d: sending stop ( string >> %s, byte array >> %s ).\n", this.id, new String(cPacket.getData()), cPacket.getData()));
					keepMoving = false;
				}
				else {
					cPacket = Scheduler.createPacket(Scheduler.ACK, rPacketParsed[1], eport);
					System.out.println(String.format("sub-%d: sending continue ( string >> %s, byte array >> %s ).\n", this.id, new String(cPacket.getData()), cPacket.getData()));					
				}				
				eSocket.send(cPacket);
			}
			
			// listen for ack to stop
			eSocket.receive(aPacket);
			
			// parsing ack
			aPacketParsed = Scheduler.parsePacket(aPacket.getData());				
			System.out.println(String.format("sub-%d: received ack ( string >> %s, byte array >> %s ).", this.id, new String(aPacket.getData()), aPacket.getData()));
			System.out.println(Arrays.toString(aPacketParsed));
			
			// sending open door
			cPacket = Scheduler.createPacket(Scheduler.CMD, Scheduler.DOOR_OPEN, eport);
			System.out.println(String.format("sub-%d: sending open door ( string >> %s, byte array >> %s ).\n", this.id, new String(cPacket.getData()), cPacket.getData()));
			eSocket.send(cPacket);
			
			// listen for ack to open
			eSocket.receive(aPacket);
			
			// parsing ack
			aPacketParsed = Scheduler.parsePacket(aPacket.getData());				
			System.out.println(String.format("sub-%d: received ack ( string >> %s, byte array >> %s ).", this.id, new String(aPacket.getData()), aPacket.getData()));
			System.out.println(Arrays.toString(aPacketParsed));
			
			// sending close door
			cPacket = Scheduler.createPacket(Scheduler.CMD, Scheduler.DOOR_CLOSE, eport);
			System.out.println(String.format("sub-%d: sending close door ( string >> %s, byte array >> %s ).\n", this.id, new String(cPacket.getData()), cPacket.getData()));
			eSocket.send(cPacket);
			
			// listen for ack to close
			eSocket.receive(aPacket);
			
			// parsing ack
			aPacketParsed = Scheduler.parsePacket(aPacket.getData());				
			System.out.println(String.format("sub-%d: received ack - done ( string >> %s, byte array >> %s ).", this.id, new String(aPacket.getData()), aPacket.getData()));
			System.out.println(Arrays.toString(aPacketParsed));			
		}
		catch (Exception e) {
			System.out.println(String.format("sub-%d: unable to communicate with elevator system, aborting request.", this.id));
			e.printStackTrace();
			return;
		}		
	}
	
	
	/**
	 * Send position to floor system so the lamp can be turned off.
	 * 
	 * @param floor
	 */
	public void sendPositionToFloor(String floor) {
		
		byte[] buffer = new byte[8];
		DatagramPacket sPacket = Scheduler.createPacket(Scheduler.CMD, Scheduler.ELEVATOR_ARRIVED, fport);		
		DatagramPacket rPacket = new DatagramPacket(buffer, buffer.length);
		String[] rPacketParsed;
		
		
		try {
			DatagramSocket tempSocket = new DatagramSocket();
			System.out.println(String.format("sub-%d: sending position update to floor ( string >> %s, byte array >> %s ).\n", this.id, new String(sPacket.getData()), sPacket.getData()));
			tempSocket.send(sPacket);
			
			// listen for ack
			tempSocket.receive(rPacket);
			
			// parsing ack
			rPacketParsed = Scheduler.parsePacket(rPacket.getData());				
			System.out.println(String.format("sub-%d: received ack ( string >> %s, byte array >> %s ).", this.id, new String(rPacket.getData()), rPacket.getData()));
			System.out.println(Arrays.toString(rPacketParsed));
			
			// sending floor number
			sPacket = Scheduler.createPacket(Scheduler.DATA, floor, fport);
			System.out.println(String.format("sub-%d: sending floor number ( string >> %s, byte array >> %s ).\n", this.id, new String(sPacket.getData()), sPacket.getData()));
			tempSocket.send(sPacket);
			
			// listen for ack
			tempSocket.receive(rPacket);
			
			// parsing ack
			rPacketParsed = Scheduler.parsePacket(rPacket.getData());				
			System.out.println(String.format("sub-%d: received ack - done ( string >> %s, byte array >> %s ).", this.id, new String(rPacket.getData()), rPacket.getData()));
			System.out.println(Arrays.toString(rPacketParsed));
			
			tempSocket.close();
			return;
		} 
		catch (Exception e) {			
			e.printStackTrace();
		}		
	}
}


