import java.net.*;
import java.io.*;
import java.util.*;


/**
 * Dummy floor subsystem to debug scheduler.
 * 
 * @author Marti
 *
 */
public class TestScheduler {
	
	/* ## HEADER AND COMMAND IDENTIFIERS ## */
	final int ACK = 1;
	final int CMD = 2;
	final int DATA = 3;
	final int ERROR = 0;
	
	final String FLOOR_BUTTON = "0x10";		// floor button
	final String ELEVATOR_BUTTON = "0x20"; 	// elevator button
	/* ## ------------------------------ ## */
	
	
	
	final int PORT = 8008;					// port of scheduler
	DatagramSocket socket = null;			// socket used to communicate with scheduler
	
	public TestScheduler() {
		try {
			socket = new DatagramSocket();
		}
		catch (SocketException se) {
			System.out.println("could not create client socket.");
			System.exit(1);
		}		
	}	
	
	
	/**
	 * Running shit.
	 * 
	 */
	public void run() {
		
		int iterations = -1;
		
		// sending one of each packet header type
		while (iterations++ < 3) {
			System.out.println(iterations);
			DatagramPacket sendPacket = createPacket(iterations, FLOOR_BUTTON);
			
			// sending packet
			try {
				socket.send(sendPacket);
				System.out.println(String.format("packet sent to ( %s, %s ).", sendPacket.getAddress().getHostAddress(), sendPacket.getPort()));
			}
			catch (IOException ioe) {
				System.out.println("unable to send packet (IOException), exiting.");
				System.exit(1);
			}
			
			// slow it dooowwwwwwnnnnnnnnn
			try {
				Thread.sleep(2000);
			}
			catch (InterruptedException ie) {
				System.exit(1);
			}
		}
		socket.close();		
		return;
	}
		
	
	/**
	 * Creates and returns a DatagramPacket.
	 * 
	 * @param packetType
	 * @return packet with data
	 */
	public DatagramPacket createPacket(int packetType, String ins) {
		String data;
		DatagramPacket packet = null;
		
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
		
		try {			
			packet = new DatagramPacket(data.getBytes(), data.getBytes().length, InetAddress.getLocalHost(), PORT);
		}
		catch (UnknownHostException uhe) {			
			System.out.println("unable to create packet (UnknownHostException), exiting.");
			System.exit(1);
		}
		return packet;
	}	
	
	
	public static void main(String[] args) {
		TestScheduler tester = new TestScheduler();
		tester.run();
	}
}
