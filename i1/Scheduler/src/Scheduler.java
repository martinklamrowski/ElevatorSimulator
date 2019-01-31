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
		int id = 0;
		
		byte[] buffer = new byte[8];
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
				else {
					System.out.println("main: error");
				}
			}
			catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}			
		}		
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
 * Threaded class to handle requests received.
 * 
 * @author Marti
 *
 */
class HandleRequest extends Thread {
	
	private DatagramSocket eSocket;			// socket to communicate with elevator system
	private int PORT;						// port of elevator system
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
			this.PORT = port;
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}	
	
	
	@Override
	public void run() {
		DatagramPacket fPacket = Scheduler.createPacket(Scheduler.CMD, "frwd", PORT);		
		
		System.out.println("sub: a new thread is running.");
		
		while (true) {		
			// creating new packet to forward to server
			System.out.println(String.format("sub: forwarding packet ( string >> %s, byte array >> %s ).", new String(fPacket.getData()), fPacket.getData()));
			
			try {					
				eSocket.send(fPacket);
			}
			catch (IOException ioe) {
				System.out.println("sub: unable to forward packet (IOException), exiting.");
				System.exit(1);					
			}
			

			
			
			
			return;
		}
	}	
} 