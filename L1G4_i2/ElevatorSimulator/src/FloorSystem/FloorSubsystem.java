
// FloorSubsystem.java
// This class is the FloorSubsystem side for a simple echo server based on
// UDP/IP. The FloorSubsystem sends a character string to the echo server, then waits 
// for the server to send it back to the FloorSubsystem.
// Last edited January 9th, 2016
package FloorSystem;


import java.io.*;
import java.net.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import FloorSystem.FloorSubsystem.Direction;

/**
 * 
 * 
 * @author Sunjay
 *
 */

public class FloorSubsystem {

	DatagramPacket sendPacket;
	DatagramPacket receivePacket;
	DatagramSocket floorSocket;
	/* ## HEADER AND COMMAND IDENTIFIERS ## */
	final int ACK = 1;
	final int CMD = 2;
	final int DATA = 3;
	final int ERROR = 0;

	final int FLOORPORT = 6520;					// port of floor subsystem
	final int REQUESTPORT = 6521;					// port of floor subsystem
	final int SCHEDPORT = 8008;					// port of scheduler
	
	public enum Direction {
		UP,
		DOWN,
		IDLE
	}
	
	int numFloors;
	int upperFloor;
	int lowerFloor;
	
	int floorMin = 0;
	int floorTotal = 10;
	int elevatorTotal = 1;
	
	Direction currentDirection = Direction.IDLE;
	int requestCount = 0;
	
	ArrayList<Floor> floors = new ArrayList<Floor>();	

	public FloorSubsystem() {
			
        try {
        	floorSocket = new DatagramSocket(FLOORPORT);
        } catch (SocketException se) {
            se.printStackTrace();
            System.exit(1);
        }
		
	}
			
			

	/**
	 * Places string in a byte array for sending
	 * 
	 * @param packetType, ins
	 * @param byte array of data
	 */
	public byte[] createPacketData(int packetType, String ins) {
		
		String data;
		
		// error
		if (packetType == 0) {			
			data = "\0" + ERROR + "\0" + ins + "\0";					
		}
		// ack
		else if (packetType == 1) {			
			data = "\0" + ACK + "\0" + ins + "\0";
		}
		// cmd
		else if (packetType == 2) {
			data = "\0" + CMD + "\0" + ins + "\0";
		}
		// data
		else {
			data = "\0" + DATA + "\0" + ins + "\0";
		}		
		
		return data.getBytes();
	}	

	/**
	 * Converts byte array into string array using default charset.
	 * data[0] is the header, data[1] is the data or command (ex "0x10")
	 * 
	 * @param msg
	 * @return str
	 */
	public String[] readPacketData(byte[] msg) {
		// Converts byte array into string array using default charset.
		// data[0] is the header, data[1] is the data or command (ex "0x10")
		String data = new String(msg);
		String[] str;
		str = data.replaceFirst("\0", "").split("\0");
		
		return str;
	}
	

	/**
	 * Set a datagram packet and block until message is received
	 * 
	 * @param socket, msg
	 * @return packet
	 */
	public DatagramPacket receive(DatagramSocket socket, byte[] msg) {
		DatagramPacket packet = new DatagramPacket(msg, msg.length);

		// Block until a datagram packet is received
		try {
			socket.receive(packet);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		return packet;
	}


	/**
	 * Create a service request to send to the scheduler, setting lamps as necessary
	 * 
	 * @param start, dest, direction
	 * @return msg
	 */
	public byte[] createServiceRequest(int start, int dest, Direction direction) {
		
		byte msg[] = new byte[100];
		String message = "00:00:00.0 " + start + " " + direction + " " + dest;
		msg = createPacketData(DATA, message);

		if (direction == direction.UP) floors.get(start).setUpLampOn();
		else if (direction == direction.DOWN) floors.get(start).setDownLampOn();
		
		System.out.println("Floor Subsystem: Sending elevator request to go from floor " + 
								start + " to " + dest + ", heading " + direction + ". Turning direction lamp on.");
		 
		return msg;
	}	

	/**
	 * Create and send a message
	 * 
	 * @param msg, port
	 * 
	 */
	public void send(byte[] msg, int port, DatagramSocket socket) {
		
		//create a service request message
		try {
	        sendPacket = new DatagramPacket(msg, msg.length,
	                                        InetAddress.getLocalHost(), port);
	     } catch (UnknownHostException e) {
	        e.printStackTrace();
	        System.exit(1);
	     }
		 
		//send the service request message
		try {
	        socket.send(sendPacket);

	     } catch (IOException e) {
	        e.printStackTrace();
	        System.exit(1);
	     }
		 //System.out.println("Floor Subsystem: Sending ");
		 System.out.println(String.format("Floor Subsystem: Sending packet ( string >> %s, byte array >> %s ).", 
				 new String(sendPacket.getData()), sendPacket.getData()));
		 
		
	}
	

	/**
	 * Send a service request containing the start floor, destination floor and target direction
	 * 
	 * @param start, dest, dir
	 * 
	 */
	public void sendServiceRequest(int start, int dest, Direction dir, DatagramSocket socket) {
		
		byte[] buffer = new byte[100];
		byte[] response = new byte[100];
		String[] msg = new String[2];
		String[] data = new String[2];
		String[] acknowledgment = new String[2];
		buffer = createPacketData(CMD, "0x10");
		send(buffer, SCHEDPORT, socket);
		System.out.println("Floor Subsystem: Requesting to send elevator input. Waiting for acknowledgment...");
		receive(socket, buffer);
		msg = readPacketData(buffer);
		if (Integer.parseInt(msg[0]) == ACK) {
			if (msg[1].equals("0x10")) {
				System.out.println("Floor Subsystem: CMD acknowledgment received. Sending input to Scheduler");
				response = createServiceRequest(start, dest, dir);
				send(response, SCHEDPORT, socket);
//				System.out.println(readPacketData(response)[0]);
//				System.out.println(readPacketData(response)[1]);
				System.out.println("Floor Subsystem: Waiting for acknowledgment of data packet...");
				receive(socket, buffer);
				data = readPacketData(buffer);
				acknowledgment = readPacketData(response);
//				if (!data[1].equals(acknowledgment[1])) {
//					System.out.println("Floor Subsystem: Data not the same. Restarting exchange");
//					sendServiceRequest(start, dest, dir);
//				}
//				else {
					System.out.println("Floor Subsystem: Data packet acknowledged. Scheduler data is: " + data[1]);
//				}
			}
		}
	}

	/**
	 * Respond to an incoming CMD message
	 * 
	 * @param msg, tempPort
	 * 
	 */
	public void cmdRequest(String[] msg, int tempPort) {
		
		byte[] buffer = new byte[100];
		byte[] response = new byte[100];
		String[] data = new String[2];
		String direction = "";
		if (Integer.parseInt(msg[0]) == CMD) {
			if (msg[1].equals("0x11")) {
				System.out.println("Floor Subsystem: Elevator departure message received. Sending acknowledgment");
				response = createPacketData(ACK,"0x11");
				send(response, tempPort, floorSocket);
				System.out.println("Floor Subsystem: Waiting for floor number...");
				receive(floorSocket, buffer);
				data = readPacketData(buffer);
				//acknowledgment = readPacketData(response);
				if (currentDirection == Direction.UP) {
					floors.get(Integer.parseInt(data[1])).setUpLampOff();
					direction = "up";
				}
				else if (currentDirection == Direction.DOWN) {
					floors.get(Integer.parseInt(data[1])).setDownLampOff();
					direction = "down";
				}
				System.out.printf("Floor Subsystem: Floor number received. Turning %s direction lamp off for floor %d and sending acknowledgment \n", direction, Integer.parseInt(data[1]));
				response = createPacketData(ACK,data[1]);
				send(response, tempPort, floorSocket);
			}
		}
	}
	
	/**
	 * Run the bulk of the subsystem
	 * 
	 * @param ins
	 * @param request
	 */
	public void running() {
		boolean listening = true;
		byte[] buffer = new byte[100];
		String[] data = new String[2];
		int tempPort = 0;
		
		while (listening) {
			try {
//				System.out.println("I'm listening");
				tempPort = receive(floorSocket, buffer).getPort();
				data = readPacketData(buffer);
				if (Integer.parseInt(data[0]) == CMD) {
					cmdRequest(data, tempPort);
					
				}
			} catch (Exception e) {
				e.printStackTrace();
				listening = false;
			}
		}
	}	
	


	public static void main(String args[]) throws IOException {
		
		FloorSubsystem floorSubsystem = new FloorSubsystem();
		Thread inputHandler;
		

		for (int i = 0; i < floorSubsystem.floorTotal; i++) {
			if (i == floorSubsystem.floorMin) floorSubsystem.floors.add(new Floor(1, i+1, false, true));
			else if (i == floorSubsystem.floorTotal - floorSubsystem.floorMin) floorSubsystem.floors.add(new Floor(1, i+1, true, false));
			else floorSubsystem.floors.add(new Floor(1, i+1, false, false));
		}
		System.out.println(System.getProperty("user.dir"));
		inputHandler = new Thread(new UserInputHandler(floorSubsystem), "User Input Handler");
		inputHandler.start();
		System.out.println("Starting Floor Subsystem");
		floorSubsystem.running();
		
		
		//for (Floor f : floors) { }
   }
}



