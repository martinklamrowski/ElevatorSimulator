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
import java.util.concurrent.*;

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
	public static final String UP_MOVE = "0x35";
	public static final String DOWN_MOVE = "0x34";
	public static final String UP_PICKUP = "0x33";							// going up to pick up
	public static final String UP_DROPOFF = "0x32"; 						// going up dropoff
	public static final String DOWN_PICKUP = "0x31";						// going down to pickup
	public static final String DOWN_DROPOFF = "0x30";						// going down to dropoff
	public static final String DOOR_OPEN = "0x3A";							// open car door
	public static final String DOOR_CLOSE = "0x3B";							// close car door
	public static final String STOP = "0x3C";								// stop elevator car
	public static final String GO_IDLE = "0x3D";
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
	private BlockingQueue<String> serviceQ1;									// queue with down requests (elevator 1)
	private BlockingQueue<String> upQ2;										// queue with up requests (elevator 2)
	private BlockingQueue<String> serviceQ2;									// queue with down requests (elevator 2)
	private BlockingQueue<String> upQ3;										// queue with up requests (elevator 3)
	private BlockingQueue<String> serviceQ3;									// queue with down requests (elevator 3)
	private BlockingQueue<String> upQ4;										// queue with up requests (elevator 4)
	private BlockingQueue<String> serviceQ4;									// queue with down requests (elevator 4)
	
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
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		serviceQ1 = new ArrayBlockingQueue<String>(100);
		serviceQ2 = new ArrayBlockingQueue<String>(100);
		serviceQ3 = new ArrayBlockingQueue<String>(100);
		serviceQ4 = new ArrayBlockingQueue<String>(100);

		Map<Integer,String[]> pupQ1 = new ConcurrentHashMap<Integer,String[]>();
		Map<Integer,String[]> doffQ1 = new ConcurrentHashMap<Integer,String[]>();
//		ArrayList<Integer> doffQ1 = new ArrayList<Integer>();
		Map<Integer,String[]> pupQ2 = new ConcurrentHashMap<Integer,String[]>();
		Map<Integer,String[]> doffQ2 = new ConcurrentHashMap<Integer,String[]>();
//		ArrayList<Integer> doffQ2 = new ArrayList<Integer>();
		Map<Integer,String[]> pupQ3 = new ConcurrentHashMap<Integer,String[]>();
		Map<Integer,String[]> doffQ3 = new ConcurrentHashMap<Integer,String[]>();
//		ArrayList<Integer> doffQ3 = new ArrayList<Integer>();
		Map<Integer,String[]> pupQ4 = new ConcurrentHashMap<Integer,String[]>();
		Map<Integer,String[]> doffQ4 = new ConcurrentHashMap<Integer,String[]>();
//		ArrayList<Integer> doffQ4 = new ArrayList<Integer>();
		
		handler1 = new ElevatorHandler(EPORT1, FPORT, serviceQ1, pupQ1, doffQ1, 1, "IDLE", 1);
		handler2 = new ElevatorHandler(EPORT2, FPORT, serviceQ2, pupQ2, doffQ2, 10, "IDLE", 2);
		handler3 = new ElevatorHandler(EPORT3, FPORT, serviceQ3, pupQ3, doffQ3, 10, "IDLE", 3);
		handler4 = new ElevatorHandler(EPORT4, FPORT, serviceQ4, pupQ4, doffQ4, 20, "IDLE", 4);
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
//					temp[0] = time, temp[1] = start floor, temp[2] = direction, temp[3] = dest floor
					
					int FS1 = calculateSuitability(FLOORS, handler1.currentFloor, Integer.parseInt(temp[1]), handler1.currentDirection, temp[2]);
					int FS2 = calculateSuitability(FLOORS, handler2.currentFloor, Integer.parseInt(temp[1]), handler2.currentDirection, temp[2]);
					int FS3 = calculateSuitability(FLOORS, handler3.currentFloor, Integer.parseInt(temp[1]), handler3.currentDirection, temp[2]);
					int FS4 = calculateSuitability(FLOORS, handler4.currentFloor, Integer.parseInt(temp[1]), handler4.currentDirection, temp[2]);
					int maxFS = Math.max(Math.max(FS1, FS2), Math.max(FS3, FS4));
					
					if (maxFS == FS1) {
						System.out.println("main: giving service request to sub-1");
//						synchronized (handler1) {
//							handler1.notifyAll();
//						}
						serviceQ1.add(dPacketParsed[1]);	
					}
					else if (maxFS == FS2) {
						//notifyAll();
//						synchronized (handler2) {
//							handler2.notifyAll();
//						}
						System.out.println("main: giving service request to sub-2");
						serviceQ2.add(dPacketParsed[1]);
					}
					else if (maxFS == FS3) {
						//notifyAll();
//						synchronized (handler3) {
//							handler3.notifyAll();
//						}
						System.out.println("main: giving service request to sub-3");
						serviceQ3.add(dPacketParsed[1]);		
					}
					else {
						//notifyAll();
//						synchronized (handler4) {
//							handler4.notifyAll();
//						}
						System.out.println("main: giving service request to sub-4");
						serviceQ4.add(dPacketParsed[1]);						
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
	public static int calculateSuitability(int n, int currentFloor, int requestedFloor, String currentElevatorDir, String requestedDir) {
		
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
				calculated = n - n/2 - distance;
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
				calculated = n - n/2 - distance;
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
	private BlockingQueue<String> serviceQ;			// queue with requests
	private Map<Integer,String[]> pickupQ;			// pickup queue containing Integer start floor key, and string array holding destination and direction
	private Map<Integer,String[]> dropoffQ;			// dropoff queue containing Integer destination floors
	private String newCommand = "";
	private String specialCommand = "";
	private int targetSrc = 0;
	private int targetDest;
	private ArrayList<Integer> srcFloors = new ArrayList<Integer>();
	private ArrayList<Integer> srcFloorsDiff = new ArrayList<Integer>();
	private boolean keepMoving;
	
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
	 * @param serviceQ
	 */
	public ElevatorHandler(int eport, int fport, BlockingQueue<String> serviceQ, 
			Map<Integer,String[]> pupQ, Map<Integer,String[]> doffQ, int currentFloor, String currentDirection, int id) {
		super("request thread");
		try {
			eSocket = new DatagramSocket();
			this.eport = eport;
			this.fport = fport;
			this.serviceQ = serviceQ;
			this.pickupQ = pupQ;
			this.dropoffQ = doffQ;
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
	 * The thread will check for service requests from given by the floor at thread start, and 
	 * whenever the current floor is changed. The request floors are placed in pickup and dropoff queues.
	 * If the elevator passes pickup or dropoff floors, new commands to the elevator are given for the next loop
	 */
	@Override
	public synchronized void run() {
		
		boolean running = true;
		String ins;
		String srcFloor = "";
		String destFloor = "";
		String direction = "";
		String[] parsedData;
		System.out.println("sub: a new thread is running.");	
		
		while (running) {

			String request = serviceQ.peek();	

			/*
			 *  determining which way the elevator needs to go to pickup and dropoff
			 *	DATA packet format: '\3\"time src_floor direction dest_floor"\'
			 *	
			 *	if elevator is below src_floor - need to move elevator up
			 *	if elevator is above src_floor - need to move elevator down 
			 *
			 *	if dest_floor > src_floor - go up after pickup
			 *	if dest_floor < src_floor - go down after pickup
			 */		
			if (request != null) {
				serviceQ.poll();
				parsedData = request.split(" ");
				srcFloor = parsedData[1];
				destFloor = parsedData[3];
				direction = parsedData[2];			
				if (dropoffQ.isEmpty()) {
					System.out.printf("Parsed start = %s, dest = %s, and dir = %s \n", srcFloor, destFloor, direction);

					if (Integer.parseInt(srcFloor) > currentFloor) {
						ins = Scheduler.UP_MOVE;
						currentDirection = "UP";
						targetSrc = Integer.parseInt(srcFloor);
					}
					else if (Integer.parseInt(srcFloor) < currentFloor) {
						ins = Scheduler.DOWN_MOVE;
						currentDirection = "DOWN";
						targetSrc = Integer.parseInt(srcFloor);
					}				
					else {
						ins = Scheduler.STOP;	// elevator is already there
						if (direction.equals("UP")) newCommand = Scheduler.UP_PICKUP;
						if (direction.equals("DOWN")) newCommand = Scheduler.DOWN_PICKUP;
						targetDest = Integer.parseInt(destFloor);
					}
					String[] srcAndDir = { srcFloor, direction };
					String[] destAndDir = { destFloor, direction };
					pickupQ.put(Integer.parseInt(srcFloor), destAndDir);
					dropoffQ.put(Integer.parseInt(destFloor), srcAndDir);
					moving(ins, Integer.toString(targetSrc), destFloor);
					targetSrc = 0;
				}
			}
			
//			If a new command has been marked for the next while loop, this code will be used
			if (!newCommand.equals("")) {
//				Special case for pickup/dropoff on same floor
				if (!specialCommand.equals("")) {
					ins = specialCommand;
					goIdle(ins);
					specialCommand = "";
				}
				else {
					System.out.println(String.format("sub-%d: newCommand is: %s", this.id, newCommand));
					ins = newCommand;
					newCommand = "";
					if (ins == Scheduler.UP_PICKUP || ins == Scheduler.UP_DROPOFF) currentDirection = "UP";
					if (ins == Scheduler.DOWN_PICKUP || ins == Scheduler.DOWN_DROPOFF) currentDirection = "DOWN";
					
					if (ins == Scheduler.GO_IDLE) goIdle(ins);
					else moving(ins, Integer.toString(targetSrc), Integer.toString(targetDest));
					targetSrc = 0;
				}
			}
		
//			Should only be called for cases where an elevator has gone idle, and had received a new request
//			while previously in transit
//			DropoffQ and pickupQ size will be equal if the elevator was idle
			if (!pickupQ.isEmpty() && pickupQ.size() == dropoffQ.size()) {
				System.out.println(String.format("sub-%d: pickupQ not empty", this.id));
//				Iterate over the pickup queue, placing difference scores in srcFloorsDiff
//				and their corresponding floors in srcFloors, in order to find the floor closest to elevator
				pickupQ.forEach((k, v) -> {
					System.out.println(String.format("sub-%d: pickupQ floor: %d", this.id, k));
					srcFloorsDiff.add(Scheduler.calculateSuitability(Scheduler.FLOORS, k, currentFloor, currentDirection, v[1]));
					srcFloors.add(k);
				});
				// targetSrc is assigned the floor value closest to the elevator
				targetSrc = srcFloors.get(srcFloorsDiff.indexOf(Collections.max(srcFloorsDiff)));
				srcFloorsDiff.clear();
				srcFloors.clear();
//				If closest floor is above elevator instruct to move up, if below move down
//				Otherwise the elevator is at the floor, so send up or down pickup
				if (targetSrc > currentFloor) {
					newCommand = Scheduler.UP_MOVE;
					currentDirection = "UP";
				}
				else if (targetSrc < currentFloor) {
					newCommand = Scheduler.DOWN_MOVE;
					currentDirection = "DOWN";
				}				
				else {
//					newCommand = Scheduler.STOP;	// elevator is already there
					if (pickupQ.get(targetSrc)[1].equals("UP")) newCommand = Scheduler.UP_PICKUP;
					if (pickupQ.get(targetSrc)[1].equals("DOWN")) newCommand = Scheduler.DOWN_PICKUP;
					targetDest = Integer.parseInt(pickupQ.get(targetSrc)[0]);
					targetSrc = 0;
				}
			}
			
//			Wait a minimal amount of time between each loop
			try {
				sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}			
		eSocket.close();
		return;
	}
	
	
	public void goIdle(String ins) {
		byte[] buffer = new byte[8];
		DatagramPacket cPacket = Scheduler.createPacket(Scheduler.CMD, ins, eport);
		DatagramPacket aPacket = new DatagramPacket(buffer, buffer.length);			
		// send cmd
		try {
			eSocket.send(cPacket);
			// listen for ack
			eSocket.receive(aPacket);
			
			// parsing ack
			String[] aPacketParsed = Scheduler.parsePacket(aPacket.getData());				
			System.out.println(String.format("sub-%d: received ack ( string >> %s, byte array >> %s ).", this.id, new String(aPacket.getData()), aPacket.getData()));
			System.out.println(Arrays.toString(aPacketParsed));
		} catch (IOException e) {
			System.out.printf("sub-%d: unable to communicate with elevator system, aborting request.", this.id);
			e.printStackTrace();
		}
		
	}
	
	
	public void moving(String ins, String targetFloor, String destFloor) {
		String[] parsedData;
		Boolean singleRemoval = false;
		Boolean doubleRemoval = false;
		keepMoving = (ins == Scheduler.STOP || ins == Scheduler.GO_IDLE ? false : true); // if the elevator is already there no need for positional updates
		Boolean onlyMove = (Integer.parseInt(targetFloor) > 0);
		String newRequest = "";	
		String[] newParsedData;
		String newDirection = "";
		
		
		byte[] buffer = new byte[8];
		DatagramPacket cPacket = Scheduler.createPacket(Scheduler.CMD, ins, eport);
		DatagramPacket aPacket = new DatagramPacket(buffer, buffer.length);
		DatagramPacket dPacket = Scheduler.createPacket(Scheduler.DATA, destFloor, eport);
		DatagramPacket rPacket = new DatagramPacket(buffer, buffer.length);
		String[] rPacketParsed;
		
		System.out.println(String.format("sub-%d: forwarding command packet ( string >> %s, byte array >> %s ).", this.id, new String(cPacket.getData()), cPacket.getData()));
		
		try {
			if (ins.equals(Scheduler.UP_DROPOFF) || ins.equals(Scheduler.DOWN_DROPOFF)) {
				dropoffQ.remove((Object) currentFloor);
			System.out.printf("sub-%d: removed %d from dropoff list\n", this.id, currentFloor);
			}
			
			// send cmd
			eSocket.send(cPacket);
			
			// listen for ack
			eSocket.receive(aPacket);
			
			// parsing ack
			String[] aPacketParsed = Scheduler.parsePacket(aPacket.getData());				
			System.out.println(String.format("sub-%d: received ack ( string >> %s, byte array >> %s ).", this.id, new String(aPacket.getData()), aPacket.getData()));
			System.out.println(Arrays.toString(aPacketParsed));
			
			if (ins.equals(Scheduler.UP_PICKUP) || ins.equals(Scheduler.DOWN_PICKUP)) {

				System.out.printf("\nsub-%d: removed %d from pickup list\n", this.id, currentFloor);
				pickupQ.remove((Object) currentFloor);
				
				// send elevator destination
				System.out.println(String.format("sub-%d: sending elevator button ( string >> %s, byte array >> %s ).", this.id, new String(dPacket.getData()), dPacket.getData()));
				eSocket.send(dPacket);
				
				// listen for ack
				eSocket.receive(aPacket);
				
				// parsing ack
				aPacketParsed = Scheduler.parsePacket(aPacket.getData());				
				System.out.println(String.format("sub-%d: received ack ( string >> %s, byte array >> %s ).", this.id, new String(aPacket.getData()), aPacket.getData()));
				System.out.println(Arrays.toString(aPacketParsed));
			}
			
			
			
			// positional updates
			while (keepMoving) {
				eSocket.receive(rPacket);
				
				rPacketParsed = Scheduler.parsePacket(rPacket.getData());				
				System.out.println(String.format("sub-%d: received positional update ( string >> %s, byte array >> %s ).", this.id, new String(rPacket.getData()), rPacket.getData()));
				System.out.println(Arrays.toString(rPacketParsed));
				
				currentFloor = Integer.parseInt(rPacketParsed[1]);
//				Checks if service is in queue, if so pop and add to start/dest lists
//				Checks if current floor is in dropoff list each time
				String request = serviceQ.poll();
				if (request != null) {
					newRequest = request;	
					newParsedData = newRequest.split(" ");		
					System.out.println(String.format("sub-%d: newly polled request ( string >> %s ).", this.id, request));
					String[] srcAndDir = { newParsedData[1], newParsedData[2] };
					String[] destAndDir = { newParsedData[3], newParsedData[2] };
					pickupQ.put(Integer.parseInt(newParsedData[1]), destAndDir);
					dropoffQ.put(Integer.parseInt(newParsedData[3]), srcAndDir);
				}
				

				if (!onlyMove) {
	//				Check the pickupQ with each elevator floor update while moving
					System.out.printf("sub-%d: checking pickupQ - current floors: ", this.id);
					for (Integer p : pickupQ.keySet()) {
						System.out.printf("%d ", p);
						if (p == currentFloor) {
	//						Handle intermediate pickup by sending UPPICKUP or DOWNPICKUP after STOPOPENCLOSE
							if (pickupQ.get(p)[1].equals("UP")) newCommand = Scheduler.UP_PICKUP;
							if (pickupQ.get(p)[1].equals("DOWN")) newCommand = Scheduler.DOWN_PICKUP;
							keepMoving = false;
							targetDest = Integer.parseInt(pickupQ.get(p)[0]);
							pickupQ.remove(p);
							System.out.printf("\nsub-%d: removed %d from pickup list\n", this.id, p);
							singleRemoval = true;
						}
					}
					System.out.println();

//					Check the dropoffQ with each elevator floor update while moving
					System.out.printf("sub-%d: checking dropoffQ - current floors: ", this.id);
					dropoffloop:
					for (Integer d : dropoffQ.keySet()) {
						System.out.printf("%d ", d);
						if (d == currentFloor) {
							if (singleRemoval) doubleRemoval = true;
							
//							Parse through pickupQ, and if the dropoff Floor being passed 
//							matches any floor in the pickupQ, break loop and ignore removal
							for (Integer p : pickupQ.keySet()) {
								if (p == d) break dropoffloop;
							}
							
//							if (ins.equals(Scheduler.UP_MOVE) || ins.equals(Scheduler.DOWN_MOVE)) break;
							keepMoving = false;
							dropoffQ.remove(d);
							System.out.printf("\nsub-%d: removed %d from dropoff list\n", this.id, d);
		//					Handle intermediate dropoff by sending UPDROPOFF or DOWNDROPOFF after STOPOPENCLOSE
//							if (currentDirection.equals("UP") && !doubleRemoval && !dropoffQ.isEmpty()) newCommand = Scheduler.UP_DROPOFF;
							if (currentDirection.equals("DOWN") && !doubleRemoval && !dropoffQ.isEmpty()) newCommand = Scheduler.DOWN_DROPOFF;

						}
					}
					System.out.println();
				}
				
				// Should only trigger if in DOWNMOVE or UPMOVE
				// Done so the elevator can ignore the queues and just move to pick up target passenger,
				// when then elevator was previously idle
				if (Integer.parseInt(targetFloor) == currentFloor) {
					keepMoving = false;
					if (pickupQ.get(currentFloor)[1].equals("UP")) newCommand = Scheduler.UP_PICKUP;
					if (pickupQ.get(currentFloor)[1].equals("DOWN")) newCommand = Scheduler.DOWN_PICKUP;
					targetDest = Integer.parseInt(pickupQ.get(currentFloor)[0]);
					System.out.printf("\nsub-%d: removed %d from pickup list\n", this.id, currentFloor);
					pickupQ.remove(currentFloor);
				}
				
				// if elevator is at required floor then stop
				if (!keepMoving) {
					cPacket = Scheduler.createPacket(Scheduler.CMD, Scheduler.STOP, eport);
					System.out.println(String.format("sub-%d: sending stop ( string >> %s, byte array >> %s ).\n", this.id, new String(cPacket.getData()), cPacket.getData()));
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
			if (ins.equals(Scheduler.UP_PICKUP) || ins.equals(Scheduler.DOWN_PICKUP)) 
				sendPositionToFloor(Integer.toString(currentFloor));
			
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
			
			if (pickupQ.isEmpty() && dropoffQ.isEmpty() && !doubleRemoval) {
				System.out.println(String.format("sub-%d: Queues are empty, instructing elevator to go idle.\n", this.id));
				newCommand = Scheduler.GO_IDLE;
			}
			
//			Special case: if the same floor is removed from each queue, use a special command that will send GO_IDLE,
//			followed by the UP_PICKUP/DOWN_PICKUP that was issued to newCommand.
//			This will tell the elevator to go idle because a passenger has been dropped off, then instruct
//			it to pickup another passenger in the queue
			if (doubleRemoval) specialCommand = Scheduler.GO_IDLE;
		}
		catch (Exception e) {
			System.out.printf("sub-%d: unable to communicate with elevator system, aborting request.", this.id);
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


