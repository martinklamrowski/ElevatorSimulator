package scheduler;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import static utils.UtilClass.*;


/**
 * Thread class to handle elevator movements. Uses a BlockingQueue to monitor new requests.
 * 
 * @author Martin Klamrowski
 *
 */
class ElevatorHandler extends Thread {
	
	private DatagramSocket eSocket;					// socket to communicate with elevator system
	private int eport;								// port of elevator system
	private int fport;								// port of floor system
	private List<String> pickList;					// ArrayList with requests
	
	protected volatile String currentDirection;		// variable representing current direction of the elevator, to be accessed by the main thread as well
	protected volatile int currentFloor;			// variable representing current floor of the elevator, to be accessed by the main thread as well
	protected volatile String status;				// is elevator out of order or not
	protected int id;								// number identifier
	
	
	/**
	 * Constructor. Takes the elevator port, floor port, request List, starting floor, status, number id.
	 * 
	 * @param eport
	 * @param fport
	 * @param pickList
	 * @param currentFloor
	 * @param status
	 * @param id
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
	 * Main thread process. Executed on thread start. Initiates handling of service requests.
	 */
	@Override
	public void run() {
		
		boolean running = true;
		String request;
		String[] requestParsed;
		String initialDirection = null;
		System.out.println(String.format("sub-%d: a new thread is running.", this.id));		
		
		/*
		 * Sequence for handling service requests:
		 *  run(): if the elevator is "IDLE", the handler takes the first request from the List
		 *  run(): the initialDirection and request are passed to the doStuff() method which performs the communication sequence with the ElevatorSubsystem
		 *   -- the initialDirection is the direction from the elevator, to where the request originated
		 *  doStuff(): reposition the elevator to the source of the first request
		 *  doStuff(): the elevator will pickup the first passenger and travel in the direction of the destination
		 *  doStuff(): along the way, the elevator will stop at a floor if
		 *    -- there is a dropoff for a passenger currently on the elevator there
		 *    -- there is a pickup for a passenger going in the same direction as the elevator is
		 *  doStuff(): end current sequence of service requests if there are no more dropoffs currently in elevator
		 */
		while (running) {
			if (status.equals("IDLE")) {
				if (!pickList.isEmpty()) {
					
					request = pickList.get(0);
					pickList.remove(0);
					
					requestParsed = request.split(" ");
					
					// elevator is above first pickup in sequence
					if (Integer.parseInt(requestParsed[0]) < currentFloor) {
						status = "REPO";
						initialDirection = "DOWN";
					}
					// elevator is below first pickup in sequence
					else if (Integer.parseInt(requestParsed[0]) > currentFloor) {
						status = "REPO";
						initialDirection = "UP";
					}
					// elevator is already at first pickup in sequence
					else {
						status = "REPO";
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
	 * Method to control elevator movement. 
	 * 
	 * @param initialRequest
	 * @param initialDirection
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
		String initialPickup = temp[0];															// location to reposition to
		String dropDirection = temp[1];															// initial dropoff floor
		Set<String> drops = new HashSet<String>();												// list of dropoffs to perform during this sequence
		String elevatorLampChanges = "";														// keeping track of elevator lamp changes
		
		boolean stop = false;																	// need to stop at this floor?
		boolean floorLampUpdate = false;														// did someone get picked up?
		String error = ERROR_STUCK;																// elevator error type
		
		
		try {
			try {				
				// repositioning elevator; sending to first pickup location
				if (!initialDirection.equals("STOP")) {
					cPacket = createPacket(CMD, (initialDirection.equals("UP") ? UP : DOWN), eport);
					
					while (status.equals("REPO")) {					
						System.out.println(String.format("sub-%d: forwarding command packet ( string >> %s, byte array >> %s ).", this.id, new String(cPacket.getData()), cPacket.getData()));
						
						// move towards pickup
						eSocket.send(cPacket);
						
						// listen for ack
						eSocket.receive(rPacket);
						
						// parsing ack
						rPacketParsed = parsePacket(rPacket.getData());				
						System.out.println(String.format("sub-%d: received ack ( string >> %s, byte array >> %s ).", this.id, new String(rPacket.getData()), rPacket.getData()));
						System.out.println(Arrays.toString(rPacketParsed));
						
						// listen for positional update
						eSocket.receive(rPacket);
						
						// parsing positional update
						rPacketParsed = parsePacket(rPacket.getData());				
						System.out.println(String.format("sub-%d: received positional update ( string >> %s, byte array >> %s ).", this.id, new String(rPacket.getData()), rPacket.getData()));
						System.out.println(Arrays.toString(rPacketParsed));
						
						// update currentFloor field
						currentFloor = Integer.parseInt(rPacketParsed[1]);
						
						// acking positional update
						aPacket = createPacket(ACK, rPacketParsed[1], eport);
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
					cPacket = createPacket(CMD, STOP, eport);
				}				
				
				// send stop
				cPacket = createPacket(CMD, STOP, eport);
				System.out.println(String.format("sub-%d: sending stop ( string >> %s, byte array >> %s ).\n", this.id, new String(cPacket.getData()), cPacket.getData()));
				
				eSocket.send(cPacket);
				
				// listen for ack to stop
				eSocket.receive(rPacket);
				
				// parsing ack
				rPacketParsed = parsePacket(rPacket.getData());				
				System.out.println(String.format("sub-%d: received ack ( string >> %s, byte array >> %s ).", this.id, new String(rPacket.getData()), rPacket.getData()));
				System.out.println(Arrays.toString(rPacketParsed));
				
				// send update to floor that the elevator has arrived
				sendPositionToFloor("" + currentFloor);
				
				error = ERROR_DOOR_JAM;
				
				// sending open door
				cPacket = createPacket(CMD, DOOR_OPEN, eport);
				System.out.println(String.format("sub-%d: sending open door ( string >> %s, byte array >> %s ).\n", this.id, new String(cPacket.getData()), cPacket.getData()));
				eSocket.send(cPacket);
				
				// listen for ack to open
				eSocket.receive(rPacket);
				
				// parsing ack
				rPacketParsed = parsePacket(rPacket.getData());				
				System.out.println(String.format("sub-%d: received ack ( string >> %s, byte array >> %s ).", this.id, new String(rPacket.getData()), rPacket.getData()));
				System.out.println(Arrays.toString(rPacketParsed));						
				
				// add initial drop
				drops.add(temp[2]);
				// send elevator lamp updates
				dPacket = createPacket(DATA, temp[2], eport);
				
				System.out.println(String.format("sub-%d: sending elevator button ( string >> %s, byte array >> %s ).", this.id, new String(dPacket.getData()), dPacket.getData()));
				eSocket.send(dPacket);
				
				// listen for ack
				eSocket.receive(rPacket);
				
				// parsing ack
				rPacketParsed = parsePacket(rPacket.getData());				
				System.out.println(String.format("sub-%d: received ack ( string >> %s, byte array >> %s ).", this.id, new String(rPacket.getData()), rPacket.getData()));
				System.out.println(Arrays.toString(rPacketParsed));						
				
				// sending close door
				cPacket = createPacket(CMD, DOOR_CLOSE, eport);
				System.out.println(String.format("sub-%d: sending close door ( string >> %s, byte array >> %s ).\n", this.id, new String(cPacket.getData()), cPacket.getData()));
				eSocket.send(cPacket);
				
				// listen for ack to close
				eSocket.receive(rPacket);
				
				// parsing ack
				rPacketParsed = parsePacket(rPacket.getData());				
				System.out.println(String.format("sub-%d: received ack ( string >> %s, byte array >> %s ).", this.id, new String(rPacket.getData()), rPacket.getData()));
				System.out.println(Arrays.toString(rPacketParsed));
				
				// get ready to perform dropoff, checking for more pickups along the way
				cPacket = createPacket(CMD, (dropDirection.equals("UP") ? UP : DOWN), eport);
				
				while (status.equals("WORKING")) {
					System.out.println(String.format("sub-%d: forwarding command packet ( string >> %s, byte array >> %s ).", this.id, new String(cPacket.getData()), cPacket.getData()));
					
					// send cmd
					eSocket.send(cPacket);
					
					// listen for ack
					eSocket.receive(rPacket);
					
					// parsing ack
					rPacketParsed = parsePacket(rPacket.getData());				
					System.out.println(String.format("sub-%d: received ack ( string >> %s, byte array >> %s ).", this.id, new String(rPacket.getData()), rPacket.getData()));
					System.out.println(Arrays.toString(rPacketParsed));
					
					// listen for positional update
					eSocket.receive(rPacket);
					
					// parsing positional update
					rPacketParsed = parsePacket(rPacket.getData());				
					System.out.println(String.format("sub-%d: received positional update ( string >> %s, byte array >> %s ).", this.id, new String(rPacket.getData()), rPacket.getData()));
					System.out.println(Arrays.toString(rPacketParsed));
					
					// update currentFloor field
					currentFloor = Integer.parseInt(rPacketParsed[1]);
					
					// acking positional update
					aPacket = createPacket(ACK, rPacketParsed[1], eport);
					System.out.println(String.format("sub-%d: acking positional update ( string >> %s, byte array >> %s ).\n", this.id, new String(aPacket.getData()), aPacket.getData()));
					
					eSocket.send(aPacket);
					
					// checking for intermediate stops (must be a pickup going in the same direction as the elevator currently is, or a dropoff from the HashSet)
					if (!pickList.isEmpty()) {						
						for (Iterator<String> iterator = pickList.iterator(); iterator.hasNext();) {
							temp = iterator.next().split(" ");
							if (temp[1].equals(dropDirection) & temp[0].equals(rPacketParsed[1])) {
								stop = true; 				// can pickup this request already
								floorLampUpdate = true;		// update floor lamps
								iterator.remove();			// remove; request is being handled
								
								// turn on elevator lamp at destination floor; do not update if theres already a dropoff for that floor
								if (!drops.contains(temp[2])) {
									drops.add(temp[2]);		// add the dropoff to the HashSet
									elevatorLampChanges += temp[2] + " ";
								}
								
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
						cPacket = createPacket(CMD, STOP, eport);
						System.out.println(String.format("sub-%d: sending stop ( string >> %s, byte array >> %s ).\n", this.id, new String(cPacket.getData()), cPacket.getData()));
						
						eSocket.send(cPacket);
						
						// listen for ack to stop
						eSocket.receive(rPacket);
						
						// parsing ack
						rPacketParsed = parsePacket(rPacket.getData());				
						System.out.println(String.format("sub-%d: received ack ( string >> %s, byte array >> %s ).", this.id, new String(rPacket.getData()), rPacket.getData()));
						System.out.println(Arrays.toString(rPacketParsed));
						
						// send update to floor that the elevator has arrived
						if (floorLampUpdate) {
							sendPositionToFloor("" + currentFloor);
						}
						error = ERROR_DOOR_JAM; // change error type
						
						// sending open door
						cPacket = createPacket(CMD, DOOR_OPEN, eport);
						System.out.println(String.format("sub-%d: sending open door ( string >> %s, byte array >> %s ).\n", this.id, new String(cPacket.getData()), cPacket.getData()));
						eSocket.send(cPacket);
						
						// listen for ack to open
						eSocket.receive(rPacket);
						
						// parsing ack
						rPacketParsed = parsePacket(rPacket.getData());				
						System.out.println(String.format("sub-%d: received ack ( string >> %s, byte array >> %s ).", this.id, new String(rPacket.getData()), rPacket.getData()));
						System.out.println(Arrays.toString(rPacketParsed));						
						
						// need to update lamps?
						if (!elevatorLampChanges.equals("")) {
							// send elevator lamp updates
							dPacket = createPacket(DATA, elevatorLampChanges, eport);
							
							System.out.println(String.format("sub-%d: sending elevator button ( string >> %s, byte array >> %s ).", this.id, new String(dPacket.getData()), dPacket.getData()));
							eSocket.send(dPacket);
							
							// listen for ack
							eSocket.receive(rPacket);
							
							// parsing ack
							rPacketParsed = parsePacket(rPacket.getData());				
							System.out.println(String.format("sub-%d: received ack ( string >> %s, byte array >> %s ).", this.id, new String(rPacket.getData()), rPacket.getData()));
							System.out.println(Arrays.toString(rPacketParsed));
						}
						
						// sending close door
						cPacket = createPacket(CMD, DOOR_CLOSE, eport);
						System.out.println(String.format("sub-%d: sending close door ( string >> %s, byte array >> %s ).\n", this.id, new String(cPacket.getData()), cPacket.getData()));
						eSocket.send(cPacket);
						
						// listen for ack to close
						eSocket.receive(rPacket);
						
						// parsing ack
						rPacketParsed = parsePacket(rPacket.getData());				
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
						cPacket = createPacket(CMD, (dropDirection.equals("UP") ? UP : DOWN), eport);
						floorLampUpdate = false;
						stop = false;
						elevatorLampChanges = "";
						error = ERROR_STUCK;
					}
				}
			
			} catch (SocketTimeoutException ste) {
				System.out.println(String.format("sub-%d: error encountered, taking elevator out of operation.", this.id));
				ePacket = createPacket(ERROR, error, eport); // sending error to elevator subsystem
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
		DatagramPacket sPacket = createPacket(CMD, ELEVATOR_ARRIVED, fport);		
		DatagramPacket rPacket = new DatagramPacket(buffer, buffer.length);
		String[] rPacketParsed;
		
		
		try {
			DatagramSocket tempSocket = new DatagramSocket();
			System.out.println(String.format("sub-%d: sending position update to floor ( string >> %s, byte array >> %s ).\n", this.id, new String(sPacket.getData()), sPacket.getData()));
			tempSocket.send(sPacket);
			
			// listen for ack
			tempSocket.receive(rPacket);
			
			// parsing ack
			rPacketParsed = parsePacket(rPacket.getData());				
			System.out.println(String.format("sub-%d: received ack ( string >> %s, byte array >> %s ).", this.id, new String(rPacket.getData()), rPacket.getData()));
			System.out.println(Arrays.toString(rPacketParsed));
			
			// sending floor number
			sPacket = createPacket(DATA, floor, fport);
			System.out.println(String.format("sub-%d: sending floor number ( string >> %s, byte array >> %s ).\n", this.id, new String(sPacket.getData()), sPacket.getData()));
			tempSocket.send(sPacket);
			
			// listen for ack
			tempSocket.receive(rPacket);
			
			// parsing ack
			rPacketParsed = parsePacket(rPacket.getData());				
			System.out.println(String.format("sub-%d: received ack - done ( string >> %s, byte array >> %s ).", this.id, new String(rPacket.getData()), rPacket.getData()));
			System.out.println(Arrays.toString(rPacketParsed));
			
			tempSocket.close();
			return;
			
		} catch (Exception e) {			
			e.printStackTrace();
		}		
	}
}



