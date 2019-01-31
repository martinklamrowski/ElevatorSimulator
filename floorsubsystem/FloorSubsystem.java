
// FloorSubsystem.java
// This class is the FloorSubsystem side for a simple echo server based on
// UDP/IP. The FloorSubsystem sends a character string to the echo server, then waits 
// for the server to send it back to the FloorSubsystem.
// Last edited January 9th, 2016
package floorsubsystem;


import java.io.*;
import java.net.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FloorSubsystem {

	DatagramPacket sendPacket;
	DatagramPacket receivePacket;
	DatagramSocket sendReceiveSocket;
	/* ## HEADER AND COMMAND IDENTIFIERS ## */
	final int ACK = 1;
	final int CMD = 2;
	final int DATA = 3;
	final int ERROR = 0;
	
	final int PORT = 6520;					// port of floor subsystem
	
	int numFloors;
	int upperFloor;
	int lowerFloor;
	
	int floorMin = 0;
	int floorTotal = 9;
	int elevatorTotal = 1;
	
	static ArrayList<Floor> floors;

	public FloorSubsystem() {
		
		ArrayList<byte[]> serviceReqs = new ArrayList<byte[]>();
		floors = new ArrayList<Floor>();
		
        try {
            sendReceiveSocket = new DatagramSocket();
        } catch (SocketException se) {
            se.printStackTrace();
            System.exit(1);
        }
		
		
	}

	public enum Direction {
		UP,
		DOWN,
		IDLE
	}
	public void parseInputFile(String filePath) throws FileNotFoundException, IOException {
		
		
		int hour = 0;
		int min = 0;
		int sec = 0;
		int mSec = 0;
		int startFloor = 0;
		int destFloor = 0;
		Direction targetDirection = Direction.IDLE;
		
		ArrayList<String> serviceReqList = new ArrayList<String>();

		BufferedReader br = new BufferedReader(new FileReader(filePath));
		StringBuilder sb = new StringBuilder();
		String line = null;
		while ((line = br.readLine()) != null) {
			Pattern pattern = Pattern.compile("'\\d{2}:\\d{2}:\\d{2}.\\d*\\s\\d{1,2}\\s[A-z]{2,4}\\s\\d{1,2}'");
			Matcher matcher = pattern.matcher(line);
			if (matcher.find()) {
				serviceReqList.add(new String(matcher.group()));
				System.out.println("Got a new service request! " + serviceReqList.get(serviceReqList.size() - 1));
			}
		}
		for (String s : serviceReqList) {
			String data[] = s.split(" ");
			String time[] = data[0].split("[:.]");
			hour = Integer.parseInt(time[0]);
			min = Integer.parseInt(time[1]);
			sec = Integer.parseInt(time[2]);
			mSec = Integer.parseInt(time[3]);
			startFloor = Integer.parseInt(data[1]);
			if (data[2].toUpperCase().equals("UP")) targetDirection = Direction.UP;
			else if (data[2].toUpperCase().equals("DOWN")) targetDirection = Direction.DOWN;
			destFloor = Integer.parseInt(data[3]);
			
			for (Floor f : floors) {
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e ) {
					e.printStackTrace();
					System.exit(1);
				}
				if (f.getFloorNum() == startFloor) {
					sendServiceRequest(startFloor, destFloor, targetDirection);
					if (targetDirection == Direction.UP) f.setUpButtonOn();
					else if (targetDirection == Direction.DOWN) f.setDownButtonOn();
				}	
			}
		}


	}
	public byte[] createPacketData(int packetType, String ins) {
		// Places string in a byte array for sending
		
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
	
	public String[] readPacketData(byte[] msg) {
		// Converts byte array into string array using default charset.
		String data = new String(msg);
		String[] str;
		str = data.replaceFirst("\0", "").split("\0");
		
		return str;
	}
	
	
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


	public void sendServiceRequest(int start, int dest, Direction direction) {
		
		byte msg[] = new byte[100];
		String message = "00:00:00.0 " + start + " " + direction + " " + dest;
		msg = createPacketData(2, message);

		if (direction == direction.UP) floors.get(start).setUpButtonOn();
		else if (direction == direction.DOWN) floors.get(start).setUpButtonOn();
		
		//create a service request message
		try {
	        sendPacket = new DatagramPacket(msg, msg.length,
	                                        InetAddress.getLocalHost(), 6520);
	     } catch (UnknownHostException e) {
	        e.printStackTrace();
	        System.exit(1);
	     }
		 
		//send the service request message
		try {
	        sendReceiveSocket.send(sendPacket);

	     } catch (IOException e) {
	        e.printStackTrace();
	        System.exit(1);
	     }
		 System.out.println("Floor Subsystem: Sending elevator request to go to floor " + dest + " heading " + direction);
		
		
	}
	public void listening() {
		boolean listening = true;
		byte[] buffer = new byte[100];
		while (listening) {
			receivePacket = receive(sendReceiveSocket, buffer);
			
			
		}
	}

	public static void main(String args[]) throws IOException {
		
		FloorSubsystem floorSubsystem = new FloorSubsystem();
		
		for (int i = 0; i < floorSubsystem.floorTotal; i++) {
			if (i == floorSubsystem.floorMin) floors.set(i, new Floor(1, i, false, true));
			else if (i == floorSubsystem.floorTotal - floorSubsystem.floorMin) floors.set(i, new Floor(1, i, true, false));
			else floors.set(i, new Floor(1, i, false, false));
		}
		
		//floorSubsystem.parseInputFile("test.txt");
		
		//for (Floor f : floors) { }
   }
}
