/*
 * SYSC3303 - Scheduler
 * Author: Martin Klamrowski
 * Date: 29/1/2019 
 */

import java.net.*;
import java.io.*;
import java.util.*;

/**
 * 
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
	
	public static final String FLOOR_BUTTON = "0x10";		// floor button
	public static final String ELEVATOR_BUTTON = "0x20"; 	// elevator button
	/* ## ------------------------------ ## */

	public static int CURRENT_FLOOR = 0;
	
	final int HOSTPORT = 8008;
	final int EPORT = 3137;
	
	DatagramSocket hostSocket;				// receiving from floor system on this socket
	//TODO DatagramSocket eSocket;			// sending and receiving from elevator system on this socket
	
	
	/**
	 * Constructor.
	 */
	public Scheduler() {
		
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
		
		byte[] buffer = new byte[12];
		DatagramPacket rPacket = new DatagramPacket(buffer, buffer.length);
		
		while (listening) {
			try {
				hostSocket.receive(rPacket);
				System.out.println(String.format("main: received packet ( string >> %s, byte array >> %s ).\n", new String(rPacket.getData()), rPacket.getData()));
				
				// handling packet
				String[] rPacketParsed = parsePacket(rPacket.getData());				
				System.out.println(Arrays.toString(rPacketParsed));
				
				// CMD
				if (rPacketParsed[0].equals(CMD)) {					
					
					HandleRequest handler = new HandleRequest(EPORT, rPacketParsed);
					handler.start();
				}
				// something else
				else {
					System.out.println("main: error");
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
		Scheduler scheduler = new Scheduler();
		scheduler.run();
	}
}




/**
 * Thread class to handle requests received.
 * 
 * @author Marti
 *
 */
class HandleRequest extends Thread {
	
	private DatagramSocket eSocket;			// socket to communicate with elevator system
	private int port;						// port of elevator system
	private String[] request;				// initial request that was sent
	
	
	/**
	 * Constructor. Takes a port number and packet.
	 * 
	 * @param port
	 * @param packet
	 */
	public HandleRequest(int port, String[] packet) {
		super("request thread");
		try {
			eSocket = new DatagramSocket();
			this.request = packet;
			this.port = port;
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
		System.out.println("sub: a new thread is running.");
		
		/*
		 *  determining instruction to send to elevator.
		 *	FLOOR_BUTTON CMD packet format: '\2\0x10\floor_number\'  
		 */
		if (request[1].equals(Scheduler.FLOOR_BUTTON)) {
			if (Integer.parseInt(request[2]) > Scheduler.CURRENT_FLOOR) {
				ins = "up";
			}
			else if (Integer.parseInt(request[2]) < Scheduler.CURRENT_FLOOR) {
				ins = "down";
			}
			else {
				ins = "stay";
			}
		}
		else {
			ins = "fuck";
		}
		
		DatagramPacket fPacket = Scheduler.createPacket(Scheduler.CMD, ins, port);		
		System.out.println(String.format("sub: forwarding packet ( string >> %s, byte array >> %s ).", new String(fPacket.getData()), fPacket.getData()));
		
		try {					
			eSocket.send(fPacket);
		}
		catch (IOException ioe) {
			System.out.println("sub: unable to forward packet (IOException), exiting.");
			//TODO running = false;				
		}		
		eSocket.close();
		return;
	}	
} 