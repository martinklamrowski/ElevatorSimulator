package utils;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;

public final class UtilClass {
	
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
}
