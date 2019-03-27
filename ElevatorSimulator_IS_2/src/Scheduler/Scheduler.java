/*
 * SYSC3303 - ElevatorHandler
 * Author: Martin Klamrowski
 * CoAuthor: Angie Byun
 * Date: 29/1/2019 
 */
package Scheduler;

import java.io.IOException;
import java.net.*;
import java.util.*;

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
	public static final String DOWN = "0x31";								// going down
	public static final String UP = "0x30";									// going up
	public static final String DOOR_OPEN = "0x3A";							// open car door
	public static final String DOOR_CLOSE = "0x3B";							// close car door
	public static final String STOP = "0x3C";								// stop elevator car
	public static final String ERROR_DOOR_JAM = "0xE0";						// error door jam
	public static final String ERROR_STUCK = "0xE1";						// elevator stuck
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
	private List<String> pickList1;											//TODO rename queue with up requests (elevator 1)
	private List<String> pickList2;											// queue with up requests (elevator 2)
	private List<String> pickList3;											// queue with up requests (elevator 3)
	private List<String> pickList4;											// queue with up requests (elevator 4)
	
	// objects performing elevator movement calculations for each elevator in separate thread
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
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		pickList1 = Collections.synchronizedList(new ArrayList<String>());
		pickList2 = Collections.synchronizedList(new ArrayList<String>());
		pickList3 = Collections.synchronizedList(new ArrayList<String>());
		pickList4 = Collections.synchronizedList(new ArrayList<String>());
		
		handler1 = new ElevatorHandler(EPORT1, FPORT, pickList1, 1, "IDLE", 1);
		handler2 = new ElevatorHandler(EPORT2, FPORT, pickList2, 1, "IDLE", 2);
		handler3 = new ElevatorHandler(EPORT3, FPORT, pickList3, 10, "IDLE", 3);
		handler4 = new ElevatorHandler(EPORT4, FPORT, pickList4, 20, "IDLE", 4);
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
					/* MAIN ELEVATOR SCHEDULING ALGORITHM */
					/* ###################################################### */					
					String[] temp = dPacketParsed[1].split(" ");
					
					int FS1 = calculateSuitability(FLOORS, handler1.currentFloor, Integer.parseInt(temp[1]), handler1.currentDirection, temp[2], handler1.status);
					int FS2 = calculateSuitability(FLOORS, handler2.currentFloor, Integer.parseInt(temp[1]), handler2.currentDirection, temp[2], handler2.status);
					int FS3 = calculateSuitability(FLOORS, handler3.currentFloor, Integer.parseInt(temp[1]), handler3.currentDirection, temp[2], handler3.status);
					int FS4 = calculateSuitability(FLOORS, handler4.currentFloor, Integer.parseInt(temp[1]), handler4.currentDirection, temp[2], handler4.status);
					int maxFS = Math.max(Math.max(FS1, FS2), Math.max(FS3, FS4));
					
					if (maxFS == FS1) {
						System.out.println("SUB 1 HANDLING IT");
						pickList1.add(temp[1] + " " + temp[2] + " " + temp[3]);			
					}
					else if (maxFS == FS2) {
						System.out.println("SUB 2 HANDLING IT");
						pickList2.add(temp[1] + " " + temp[2] + " " + temp[3]);
					}
					else if (maxFS == FS3) {
						System.out.println("SUB 3 HANDLING IT");
						pickList3.add(temp[1] + " " + temp[2] + " " + temp[3]);				
					}
					else {
						System.out.println("SUB 4 HANDLING IT");
						pickList4.add(temp[1] + " " + temp[2] + " " + temp[3]);				
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
	public static int calculateSuitability(int n, int currentFloor, int requestedFloor, String currentElevatorDir, String requestedDir, String status) {
		
		int calculated = 0;
		int distance = currentFloor - requestedFloor; // if -ive, call is above, if +ive, call is below
		
		if (status.equals("SUSPENDED")) {
			return -1;
		}
		
		// current direction of elevator is IDLE
		if (status.equals("IDLE")) {
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
	private List<String> pickList;					//TODO rename queue with up requests
	
	protected volatile String currentDirection;		// variable representing current direction of the elevator, to be accessed by the main thread as well
	protected volatile int currentFloor;			// variable representing current floor of the elevator, to be accessed by the main thread as well
	protected volatile String status;				// is elevator out of order or not
	protected int id;
	
	
	/**
	 * Constructor. Takes a port number, packet and an up and down request Queue.
	 * 
	 * @param port
	 * @param packet
	 * @param upQ
	 * @param downQ
	 */
	public ElevatorHandler(int eport, int fport, List<String> pickList, int currentFloor, String status, int id) {
		super("request thread");
		try {
			eSocket = new DatagramSocket();
			eSocket.setSoTimeout(10000);
			this.eport = eport;
			this.fport = fport;
			this.pickList = pickList;
			this.currentFloor = currentFloor;
			this.currentDirection = "";
			this.status = status;
			this.id = id;
			
		} catch (Exception e) {
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
		String request;
		String[] requestParsed;
		String initialDirection = null;
		System.out.println(String.format("sub-%d: a new thread is running.", this.id));		
				
		while (running) {
			if (status.equals("IDLE")) {
				if (!pickList.isEmpty()) {
					
					request = pickList.get(0);
					pickList.remove(0);
					
					requestParsed = request.split(" ");
					
					// elevator is above first pickup in sequence
					if (Integer.parseInt(requestParsed[0]) < currentFloor) {
						//currentDirection = requestParsed[1];
						status = "REPO";
						initialDirection = "DOWN";
					}
					// elevator is below first pickup in sequence
					else if (Integer.parseInt(requestParsed[0]) > currentFloor) {
						//currentDirection = requestParsed[1];
						status = "REPO";
						initialDirection = "UP";
					}
					// elevator is already at first pickup in sequence
					else {
						status = "REPO"; //TODO not handled yet
						initialDirection = "STOP";
					}
					
					// pass the first pickup floor and direction to get there, and the dropoff associated for that pickup
					doStuff(request, initialDirection);
				}
			}
		}			
		eSocket.close();
		return;
	}
	
	
	/**
	 * New service routine with intermediate stops. Elevator continues to move up until there are no more requests above.
	 * 
	 * @param ins
	 * @param request
	 */
	public void doStuff(String initialRequest, String initialDirection) {
				
		byte[] buffer = new byte[8];
		DatagramPacket cPacket;
		DatagramPacket aPacket;
		DatagramPacket ePacket;	
		DatagramPacket dPacket;
		DatagramPacket rPacket = new DatagramPacket(buffer, buffer.length);
		String[] rPacketParsed;
		
		String[] temp = initialRequest.split(" ");
		String initialPickup = temp[0];
		String dropDirection = temp[1];
		Set<String> drops = new HashSet<String>();
		String elevatorLampChanges = "";
		
		boolean stop = false;																	// need to stop at this floor?
		boolean floorLampUpdate = false;														// did someone get picked up?
		String error = Scheduler.ERROR_STUCK;		
		
		
		try {
			try {				
				// repositioning elevator; sending to first pickup location
				if (!initialDirection.equals("STOP")) {
					cPacket = Scheduler.createPacket(Scheduler.CMD, (initialDirection.equals("UP") ? Scheduler.UP : Scheduler.DOWN), eport);
					
					while (status.equals("REPO")) {					
						System.out.println(String.format("sub-%d: forwarding command packet ( string >> %s, byte array >> %s ).", this.id, new String(cPacket.getData()), cPacket.getData()));
						
						// move towards pickup
						eSocket.send(cPacket);
						
						// listen for ack
						eSocket.receive(rPacket);
						
						// parsing ack
						rPacketParsed = Scheduler.parsePacket(rPacket.getData());				
						System.out.println(String.format("sub-%d: received ack ( string >> %s, byte array >> %s ).", this.id, new String(rPacket.getData()), rPacket.getData()));
						System.out.println(Arrays.toString(rPacketParsed));
						
						// listen for positional update
						eSocket.receive(rPacket);
						
						// parsing positional update
						rPacketParsed = Scheduler.parsePacket(rPacket.getData());				
						System.out.println(String.format("sub-%d: received positional update ( string >> %s, byte array >> %s ).", this.id, new String(rPacket.getData()), rPacket.getData()));
						System.out.println(Arrays.toString(rPacketParsed));
						
						// update currentFloor field
						currentFloor = Integer.parseInt(rPacketParsed[1]);
						
						// acking positional update
						aPacket = Scheduler.createPacket(Scheduler.ACK, rPacketParsed[1], eport);
						System.out.println(String.format("sub-%d: acking positional update ( string >> %s, byte array >> %s ).\n", this.id, new String(aPacket.getData()), aPacket.getData()));
						
						eSocket.send(aPacket);
						
						if (rPacketParsed[1].equals(initialPickup)) {
							status = "WORKING";
							currentDirection = dropDirection;							
						}
					}
				}
				else {
					status = "WORKING";
					currentDirection = dropDirection;
					cPacket = Scheduler.createPacket(Scheduler.CMD, Scheduler.STOP, eport);
				}				
				
				// send stop
				cPacket = Scheduler.createPacket(Scheduler.CMD, Scheduler.STOP, eport);
				System.out.println(String.format("sub-%d: sending stop ( string >> %s, byte array >> %s ).\n", this.id, new String(cPacket.getData()), cPacket.getData()));
				
				eSocket.send(cPacket);
				
				// listen for ack to stop
				eSocket.receive(rPacket);
				
				// parsing ack
				rPacketParsed = Scheduler.parsePacket(rPacket.getData());				
				System.out.println(String.format("sub-%d: received ack ( string >> %s, byte array >> %s ).", this.id, new String(rPacket.getData()), rPacket.getData()));
				System.out.println(Arrays.toString(rPacketParsed));
				
				// send update to floor that the elevator has arrived
				sendPositionToFloor("" + currentFloor);
				
				error = Scheduler.ERROR_DOOR_JAM;
				
				// sending open door
				cPacket = Scheduler.createPacket(Scheduler.CMD, Scheduler.DOOR_OPEN, eport);
				System.out.println(String.format("sub-%d: sending open door ( string >> %s, byte array >> %s ).\n", this.id, new String(cPacket.getData()), cPacket.getData()));
				eSocket.send(cPacket);
				
				// listen for ack to open
				eSocket.receive(rPacket);
				
				// parsing ack
				rPacketParsed = Scheduler.parsePacket(rPacket.getData());				
				System.out.println(String.format("sub-%d: received ack ( string >> %s, byte array >> %s ).", this.id, new String(rPacket.getData()), rPacket.getData()));
				System.out.println(Arrays.toString(rPacketParsed));						
				
				// add initial drop
				drops.add(temp[2]);
				// send elevator lamp updates
				dPacket = Scheduler.createPacket(Scheduler.DATA, temp[2], eport);
				
				System.out.println(String.format("sub-%d: sending elevator button ( string >> %s, byte array >> %s ).", this.id, new String(dPacket.getData()), dPacket.getData()));
				eSocket.send(dPacket);
				
				// listen for ack
				eSocket.receive(rPacket);
				
				// parsing ack
				rPacketParsed = Scheduler.parsePacket(rPacket.getData());				
				System.out.println(String.format("sub-%d: received ack ( string >> %s, byte array >> %s ).", this.id, new String(rPacket.getData()), rPacket.getData()));
				System.out.println(Arrays.toString(rPacketParsed));						
				
				// sending close door
				cPacket = Scheduler.createPacket(Scheduler.CMD, Scheduler.DOOR_CLOSE, eport);
				System.out.println(String.format("sub-%d: sending close door ( string >> %s, byte array >> %s ).\n", this.id, new String(cPacket.getData()), cPacket.getData()));
				eSocket.send(cPacket);
				
				// listen for ack to close
				eSocket.receive(rPacket);
				
				// parsing ack
				rPacketParsed = Scheduler.parsePacket(rPacket.getData());				
				System.out.println(String.format("sub-%d: received ack ( string >> %s, byte array >> %s ).", this.id, new String(rPacket.getData()), rPacket.getData()));
				System.out.println(Arrays.toString(rPacketParsed));
				
				// get ready to perform dropoff, checking for more pickups along the way
				cPacket = Scheduler.createPacket(Scheduler.CMD, (dropDirection.equals("UP") ? Scheduler.UP : Scheduler.DOWN), eport);
				
				while (status.equals("WORKING")) {
					System.out.println(String.format("sub-%d: forwarding command packet ( string >> %s, byte array >> %s ).", this.id, new String(cPacket.getData()), cPacket.getData()));
					
					// send cmd
					eSocket.send(cPacket);
					
					// listen for ack
					eSocket.receive(rPacket);
					
					// parsing ack
					rPacketParsed = Scheduler.parsePacket(rPacket.getData());				
					System.out.println(String.format("sub-%d: received ack ( string >> %s, byte array >> %s ).", this.id, new String(rPacket.getData()), rPacket.getData()));
					System.out.println(Arrays.toString(rPacketParsed));
					
					// listen for positional update
					eSocket.receive(rPacket);
					
					// parsing positional update
					rPacketParsed = Scheduler.parsePacket(rPacket.getData());				
					System.out.println(String.format("sub-%d: received positional update ( string >> %s, byte array >> %s ).", this.id, new String(rPacket.getData()), rPacket.getData()));
					System.out.println(Arrays.toString(rPacketParsed));
					
					// update currentFloor field
					currentFloor = Integer.parseInt(rPacketParsed[1]);
					
					// acking positional update
					aPacket = Scheduler.createPacket(Scheduler.ACK, rPacketParsed[1], eport);
					System.out.println(String.format("sub-%d: acking positional update ( string >> %s, byte array >> %s ).\n", this.id, new String(aPacket.getData()), aPacket.getData()));
					
					eSocket.send(aPacket);
					
					// checking for intermediate stops (must be a pickup going in the same direction as the elevator currently is, or a dropoff from the HashSet)
					if (!pickList.isEmpty()) {						
						for (Iterator<String> iterator = pickList.iterator(); iterator.hasNext();) {
							temp = iterator.next().split(" ");
							if (temp[1].equals(dropDirection) & temp[0].equals(rPacketParsed[1])) {
								stop = true; 			// can pickup this request already
								floorLampUpdate = true;	// update floor lamps
								drops.add(temp[2]);		// add the dropoff to the HashSet
								iterator.remove();		// remove; request is being handled
								
								// turn on elevator lamp at destination floor
								elevatorLampChanges += temp[2] + " ";
							}
						}
					}
					// are there dropoffs for this floor?
					if (drops.contains(rPacketParsed[1])) {
						stop = true;					// will need to stop
						drops.remove(rPacketParsed[1]);	// remove; dropoff being handled
						
						// turn off elevator lamp at this floor
						elevatorLampChanges += rPacketParsed[1] + " ";
					}
					
					// perform operations based on above results and then reset
					if (stop) {						
						// send stop
						cPacket = Scheduler.createPacket(Scheduler.CMD, Scheduler.STOP, eport);
						System.out.println(String.format("sub-%d: sending stop ( string >> %s, byte array >> %s ).\n", this.id, new String(cPacket.getData()), cPacket.getData()));
						
						eSocket.send(cPacket);
						
						// listen for ack to stop
						eSocket.receive(rPacket);
						
						// parsing ack
						rPacketParsed = Scheduler.parsePacket(rPacket.getData());				
						System.out.println(String.format("sub-%d: received ack ( string >> %s, byte array >> %s ).", this.id, new String(rPacket.getData()), rPacket.getData()));
						System.out.println(Arrays.toString(rPacketParsed));
						
						// send update to floor that the elevator has arrived
						if (floorLampUpdate) {
							sendPositionToFloor("" + currentFloor);
						}
						error = Scheduler.ERROR_DOOR_JAM;
						
						// sending open door
						cPacket = Scheduler.createPacket(Scheduler.CMD, Scheduler.DOOR_OPEN, eport);
						System.out.println(String.format("sub-%d: sending open door ( string >> %s, byte array >> %s ).\n", this.id, new String(cPacket.getData()), cPacket.getData()));
						eSocket.send(cPacket);
						
						// listen for ack to open
						eSocket.receive(rPacket);
						
						// parsing ack
						rPacketParsed = Scheduler.parsePacket(rPacket.getData());				
						System.out.println(String.format("sub-%d: received ack ( string >> %s, byte array >> %s ).", this.id, new String(rPacket.getData()), rPacket.getData()));
						System.out.println(Arrays.toString(rPacketParsed));						
						
						if (!elevatorLampChanges.equals("")) {
							// send elevator lamp updates
							dPacket = Scheduler.createPacket(Scheduler.DATA, elevatorLampChanges, eport);
							
							System.out.println(String.format("sub-%d: sending elevator button ( string >> %s, byte array >> %s ).", this.id, new String(dPacket.getData()), dPacket.getData()));
							eSocket.send(dPacket);
							
							// listen for ack
							eSocket.receive(rPacket);
							
							// parsing ack
							rPacketParsed = Scheduler.parsePacket(rPacket.getData());				
							System.out.println(String.format("sub-%d: received ack ( string >> %s, byte array >> %s ).", this.id, new String(rPacket.getData()), rPacket.getData()));
							System.out.println(Arrays.toString(rPacketParsed));
						}
						
						// sending close door
						cPacket = Scheduler.createPacket(Scheduler.CMD, Scheduler.DOOR_CLOSE, eport);
						System.out.println(String.format("sub-%d: sending close door ( string >> %s, byte array >> %s ).\n", this.id, new String(cPacket.getData()), cPacket.getData()));
						eSocket.send(cPacket);
						
						// listen for ack to close
						eSocket.receive(rPacket);
						
						// parsing ack
						rPacketParsed = Scheduler.parsePacket(rPacket.getData());				
						System.out.println(String.format("sub-%d: received ack ( string >> %s, byte array >> %s ).", this.id, new String(rPacket.getData()), rPacket.getData()));
						System.out.println(Arrays.toString(rPacketParsed));						
					}
					
					// is the elevator empty?
					if (drops.isEmpty()) {							
						// end sequence
						status = "IDLE";
					}
					else {
						System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@RESET");
						cPacket = Scheduler.createPacket(Scheduler.CMD, (dropDirection.equals("UP") ? Scheduler.UP : Scheduler.DOWN), eport);
						floorLampUpdate = false;
						stop = false;
						elevatorLampChanges = "";
					}
				}
			
			} catch (SocketTimeoutException ste) {
				System.out.println(String.format("sub-%d: error encountered, taking elevator out of operation.", this.id));
				ePacket = Scheduler.createPacket(Scheduler.ERROR, error, eport); // sending error to elevator subsystem
				eSocket.send(ePacket);
				
				eSocket.receive(rPacket);
				status = "SUSPENDED";
				
				return;
			}
		} catch (IOException ioe) {
			System.out.println(String.format("sub-%d: unable to communicate with elevator system, quitting.", this.id));
			ioe.printStackTrace();
			System.exit(1);
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
			
		} catch (Exception e) {			
			e.printStackTrace();
		}		
	}
}


