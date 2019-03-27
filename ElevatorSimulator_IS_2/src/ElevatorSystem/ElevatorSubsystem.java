package ElevatorSystem;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class ElevatorSubsystem {
	
	/* ## HEADER AND COMMAND IDENTIFIERS ## */
	public static final String ACK = "1";
	public static final String CMD = "2";
	public static final String DATA = "3";
	public static final String ERROR = "0";
	
	public static final String DOWN = "0x31";								// going down
	public static final String UP = "0x30";									// going up
	public static final String DOOR_OPEN = "0x3A";							// open car door
	public static final String DOOR_CLOSE = "0x3B";							// close car door
	public static final String STOP = "0x3C";								// stop elevator car
	public static final String ERROR_DOOR_JAM = "0xE0";						// error door jam
	public static final String ERROR_STUCK = "0xE1";						// elevator stuck
	/* ## ------------------------------ ## */
	
	/* ## NETWORK INFO ## */
	final int EPORT1 = 3137;
	final int EPORT2 = 3237;
	final int EPORT3 = 3337;
	final int EPORT4 = 3437;	
	
	private Elevator e1;
	private Elevator e2;
	private Elevator e3;
	private Elevator e4;
	
	
	
	
	public ElevatorSubsystem() {
		e1 = new Elevator(EPORT1, 1, 1);
		e2 = new Elevator(EPORT2, 2, 1);
		e3 = new Elevator(EPORT3, 3, 10);
		e4 = new Elevator(EPORT4, 4, 20);
	}
	
	
	public void run() {
		e1.start();
		e2.start();
		e3.start();
		e4.start();		
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
		ElevatorSubsystem elevatorSubsystem = new ElevatorSubsystem();
		elevatorSubsystem.run();	
	}
	
	private class Elevator extends Thread {		
		
		final int PORT;
		final int ID;
		int currentFloor;
		Set<String> lampSet = new HashSet<>();
		
		DatagramSocket elSocket;
		
		Elevator(int port, int id, int startingFloor) {
			PORT = port;
			ID = id;
			currentFloor = startingFloor;
			
			try {
				elSocket = new DatagramSocket(PORT);
			} catch (SocketException e) {
				e.printStackTrace();
			}
		}
		
		
		@Override
		public void run() {
			
			boolean running = true;

			DatagramPacket aPacket; 
			DatagramPacket dPacket;
			
			byte[] buffer = new byte[8];
			DatagramPacket rPacket = new DatagramPacket(buffer, buffer.length);							// what we're receiving
			String[] rPacketParsed;
			
			boolean floorChange = false;																// was there a floor change?
			boolean lampChange = false;																	// was there a lamp change?
			
			System.out.println("ELEVATOR " + ID + ": Elevator Waiting ...");							// so we know we're waiting
			
			while (running) {
				try {					 					
		        	elSocket.receive(rPacket);
			    } catch (IOException e) {
		    		System.out.print("ELEVATOR " + ID + ": IOException");
		        	e.printStackTrace();
		        	System.exit(1);
			    }
				
				rPacketParsed = parsePacket(rPacket.getData());
				
				switch (rPacketParsed[0]) {					
					case CMD:
						/*====== START CMD packet received ======*/						
						switch (rPacketParsed[1]) {
							/*--- cases for different CMD code ---**/
							case UP:								
								floorChange = true;														// send elevator location
								break;		// end UP				
		
							case DOWN:								
								floorChange = true;
								break;		// end DOWN
		
							case DOOR_OPEN:	
								open();
								System.out.println("ELEVATOR " + ID + ": OPENING at " + currentFloor);								
								
								floorChange = false;
								break;		// end DOOR_OPEN				
		
							case DOOR_CLOSE:
								close();
								System.out.println("ELEVATOR " + ID + ": CLOSING at " + currentFloor);
								
								floorChange = false;
								break;		//end DOOR_CLOSE
		
							case STOP:
								System.out.println("ELEVATOR " + ID + ": STOPPED at " + currentFloor);
								
								floorChange = false;								
								break;		// end STOP		
						}
						
						/*--- send ACK for CMD ---*/
						move(rPacketParsed[1]);
						aPacket = createPacket(ACK, rPacketParsed[1], rPacket.getPort());
						try {
							elSocket.send(aPacket);
						} catch (IOException ioe) {
							ioe.printStackTrace();
						    System.exit(1);
						}
						
						if (floorChange) {
							dPacket = createPacket(DATA, "" + currentFloor, rPacket.getPort());
							try {
								elSocket.send(dPacket);
							} catch (IOException ioe) {
								ioe.printStackTrace();
							    System.exit(1);
							}
							System.out.println("ELEVATOR " + ID + ": " + (rPacketParsed[1].equals(UP) ? "UP" : "DOWN") + " to " + currentFloor);
						}						
						
						break;						
						/*====== END CMD packet received ======*/
	
					case ACK:
						/*====== START ACK packet received ======*/
						
						/*--- only get an ACK on a position update ---*/
						floorChange = false;
						break;
						/*====== END ACK packet received ======*/
						
					case DATA:
						/*====== START DATA packet received ======*/
						String[] lamps = rPacketParsed[1].split(" ");
						
						for (int i = 0; i < lamps.length; i++) {
							if (!lampSet.remove(lamps[i])) {
								lampSet.add(lamps[i]);							
							}
						}						
						lampChange = true;
							
						/*--- send ACK message ---*/
						aPacket = createPacket(ACK, rPacketParsed[1], rPacket.getPort());
						try {
							elSocket.send(aPacket);
						} catch (IOException ioe) {
							ioe.printStackTrace();
						    System.exit(1);
						}
						
						if (lampChange) {
							System.out.println("ELEVATOR " + ID + ": lamps on at [" + lampSet.toString() + "]");
							lampChange = false;
						}
						
						break;
						/*====== END DATA packet received ======*/
						
					case ERROR:
						/*====== START ERROR packet received ======*/
						switch (rPacketParsed[1]) {
							case ERROR_DOOR_JAM:
								System.out.println("!!!ERROR DOOR JAM!!! ELEVATOR SERVICE POSTPONED");
								break;
								
							case ERROR_STUCK:
								System.out.println("!!!ERROR ELEVATOR STUCK!!! ELEVATOR SERVICE POSTPONED");
								break;
						}				
						try {					
							aPacket = createPacket(ACK, rPacketParsed[1], rPacket.getPort());		
						} catch (Exception e) {
							e.printStackTrace();
						}
						
						break;
						/*====== END ERROR packet received ======*/
				}
			}			
		}
		
		
		/**
		 * Method to simulate the elevators movement. Sleeps for 6 seconds on move or 2 seconds on door operations.
		 * 
		 * @param cmd
		 * @return
		 */
		int move(String cmd) {
			
			if (cmd.equals(UP) | cmd.equals(DOWN)) {
				try {
					Thread.sleep(6000);
				} catch (InterruptedException ie) {
					ie.printStackTrace();
				}
				currentFloor = (cmd.equals(UP) ? currentFloor + 1 : currentFloor - 1);
				
			}
			else if (cmd.equals(DOOR_OPEN) | cmd.equals(DOOR_CLOSE)) {
				try {
					Thread.sleep(2000);
				} catch (InterruptedException ie) {
					ie.printStackTrace();
				}
			}
			
			/* ### THERE IS A STUCKY THING IN ELEVATOR SHAFT 4 AT FLOOR 15 ###
			if (ID == 4 & elevator.getCurrentFloor().equals("15")) {
				try {
					System.out.println("ELEVATOR " + ID + " is STUCK at Floor " + currentFloor);
					Thread.sleep(10000);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
			### END OF ERROR INJECTION ### */
			
			/* ### THERE IS A FAULTY SENSOR IN SHAFT 3 AT FLOOR 5 ###
			if (ID == 3 & currentFloor == 5) {
				try {
					System.out.println("ELEVATOR " + ID + " is STUCK at Floor " + currentFloor);
					Thread.sleep(10000);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
			### END OF ERROR INJECTION ### */
			
			return 1;
		}
		
		
		int open() {
			
			return 1;
		}
		
		
		int close() {
			
			return 1;
		}
	}
}
