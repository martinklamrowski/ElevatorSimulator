/*
 * SYSC3303 - Scheduler
 * Author: Martin Klamrowski
 * CoAuthor: Angie Byun
 * Date: 01/04/2019 
 */
package scheduler;

import java.net.*;
import java.util.*;
import static utils.UtilClass.*;


/**
 * Class acting as an intermediate host for the FloorSubsystem. Processes requests from the FloorSubsystem and controls the ElevatorSubsystem.
 * 
 * @author Martin Klamrowski
 *
 */
public class Scheduler {	
	
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
	private List<String> pickList1;											// List containing requests (elevator 1)
	private List<String> pickList2;											// List containing requests (elevator 2)
	private List<String> pickList3;											// List containing requests (elevator 3)
	private List<String> pickList4;											// List containing requests (elevator 4)
	
	// objects performing elevator movement calculations for each elevator in separate thread
	private ElevatorHandler handler1;
	private ElevatorHandler handler2;
	private ElevatorHandler handler3;
	private ElevatorHandler handler4;
	/* ## ---------------------------- ## */
	
	
	/**
	 * Constructor. Creates four handlers.
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
		
		// initialize with corresponding elevator port, floor port, request List, starting floor, status, ID
		handler1 = new ElevatorHandler(EPORT1, FPORT, pickList1, 1, "IDLE", 1);
		handler2 = new ElevatorHandler(EPORT2, FPORT, pickList2, 1, "IDLE", 2);
		handler3 = new ElevatorHandler(EPORT3, FPORT, pickList3, 10, "IDLE", 3);
		handler4 = new ElevatorHandler(EPORT4, FPORT, pickList4, 20, "IDLE", 4);
	}
	
	
	/**
	 * Main process. Create the handlers, listen for service requests.
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
		
		// receive requests from FloorSubsystem
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
	 * Method to handle a command received from a floor. Computes the suitability of each elevator to service a floor request, gives the request to the corresponding elevator.
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
					
					// determine which elevator List to add request to
					
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
	 * Main.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Scheduler scheduler = new Scheduler();
		scheduler.run();
	}
}





