
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

	final int FLOORPORT = 6520;					// port of floor subsystem
	final int SCHEDPORT = 8008;					// port of scheduler
	
	int numFloors;
	int upperFloor;
	int lowerFloor;
	
	int floorMin = 0;
	int floorTotal = 10;
	int elevatorTotal = 1;
	
	static ArrayList<Floor> floors;

	public FloorSubsystem() {
		
		ArrayList<byte[]> serviceReqs = new ArrayList<byte[]>();
		floors = new ArrayList<Floor>();
		
        try {
            sendReceiveSocket = new DatagramSocket(FLOORPORT);
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
		
		// Take a flat file and parse lines of text for requests in the following form: 
		// "XX:XX:XX.XXX X DIRECTION X"
		// Method is not used for this iteration
		int hour = 0;
		int min = 0;
		int sec = 0;
		int mSec = 0;
		int startFloor = 0;
		int destFloor = 0;
		Direction targetDirection = Direction.IDLE;
		
		ArrayList<String> serviceReqList = new ArrayList<String>();

		BufferedReader br = new BufferedReader(new FileReader(filePath));
		String line = null;
		while ((line = br.readLine()) != null) {
			Pattern pattern = Pattern.compile("'\\d{2}:\\d{2}:\\d{2}.\\d*\\s\\d{1,2}\\s[A-z]{2,4}\\s\\d{1,2}'");
			Matcher matcher = pattern.matcher(line);
			if (matcher.find()) {
				serviceReqList.add(new String(matcher.group()));
				System.out.println("Got a new service request! " + serviceReqList.get(serviceReqList.size() - 1));
			}
		}
		br.close();
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
					createServiceRequest(startFloor, destFloor, targetDirection);
					if (targetDirection == Direction.UP) f.setUpLampOn();
					else if (targetDirection == Direction.DOWN) f.setDownLampOn();
				}	
			}
		}


	}
	
	public Object[] getInputData(String s) {
		// Takes the input string formatted as "XX:XX:XX.XXX X DIRECTION X" and places int and Direction enum
		// parameters into data object. Time is in values data[0] to data[3], start floor is in data[4],
		// direction is in data[5], and destination floor is in data[6]
		int numParameters = 7;
		Object[] data = new Object[numParameters];
		String[] input = s.split(" ");
		String[] time = input[0].split("[:.]");
		data[0] = Integer.parseInt(time[0]);
		data[1] = Integer.parseInt(time[1]);
		data[2] = Integer.parseInt(time[2]);
		data[3] = Integer.parseInt(time[3]);
		data[4] = Integer.parseInt(input[1]);
		if (input[2].toUpperCase().equals("UP")) data[5] = Direction.UP;
		else if (input[2].toUpperCase().equals("DOWN")) data[5] = Direction.DOWN;
		data[6] = Integer.parseInt(input[3]);
		
		return data;
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
		// data[0] is the header, data[1] is the data or command (ex "0x10")
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
	
	public void send(byte[] msg, int port) {
		
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
	        sendReceiveSocket.send(sendPacket);

	     } catch (IOException e) {
	        e.printStackTrace();
	        System.exit(1);
	     }
		 System.out.println("Floor Subsystem: Sending ");
		 System.out.println(String.format("Floor Subsystem: Sending packet ( string >> %s, byte array >> %s ).", 
				 new String(sendPacket.getData()), sendPacket.getData()));
		 
		
	}
	
	public void ackRequest(String[] msg) {
		
		byte[] buffer = new byte[100];
		byte[] response = new byte[100];
		String[] data = new String[2];
		String[] acknowledgment = new String[2];
		if (Integer.parseInt(msg[0]) == ACK) {
			if (msg[1].equals("0x10")) {
				System.out.println("Floor Subsystem: Acknowledgment received. Sending input to Scheduler");
				response = createServiceRequest(1, 6, Direction.UP);
				send(response, SCHEDPORT);
				receive(sendReceiveSocket, buffer);
				data = readPacketData(buffer);
				acknowledgment = readPacketData(response);
				if (!data[1].equals(acknowledgment[1])) {
					System.out.println("Floor Subsystem: Data not the same. Restarting exchange");
					send(createPacketData(CMD,"0x10"), SCHEDPORT);
					receive(sendReceiveSocket, buffer);
					data = readPacketData(buffer);
					ackRequest(data);
				}
				else {
					System.out.println("Floor Subsystem: Data packet acknowledged");
				}
			}
		}
	}
	
	public void cmdRequest(String[] msg, int tempPort) {
		
		byte[] buffer = new byte[100];
		byte[] response = new byte[100];
		String[] data = new String[2];
		if (Integer.parseInt(msg[0]) == CMD) {
			if (msg[1].equals("0x11")) {
				System.out.println("Floor Subsystem: Elevator departure message received. Sending acknowledgment");
				response = createPacketData(ACK,"0x11");
				send(response, tempPort);
				receive(sendReceiveSocket, buffer);
				data = readPacketData(buffer);
				//acknowledgment = readPacketData(response);
				floors.get(Integer.parseInt(data[1])).setUpLampOff();
				System.out.println("Floor Subsystem: Floor number received. Turning direction lamp off and sending acknowledgment");
				response = createPacketData(ACK,data[1]);
				send(response, tempPort);
			}
		}
	}
	
	public void running() {
		boolean listening = true;
		byte[] buffer = new byte[100];
		byte[] msg = new byte[100];
		String[] data = new String[2];
		int tempPort = 0;
		msg = createPacketData(CMD, "0x10");
		send(msg, SCHEDPORT);
		System.out.println("Floor Subsystem: Waiting for acknowledgment");
		
		while (listening) {
			try {
				tempPort = receive(sendReceiveSocket, buffer).getPort();
				data = readPacketData(buffer);
	
				if (Integer.parseInt(data[0]) == ACK) {
					ackRequest(data);
				}
				else if (Integer.parseInt(data[0]) == CMD) {
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
		
		for (int i = 0; i < floorSubsystem.floorTotal; i++) {
			if (i == floorSubsystem.floorMin) floors.add(new Floor(1, i+1, false, true));
			else if (i == floorSubsystem.floorTotal - floorSubsystem.floorMin) floors.add(new Floor(1, i+1, true, false));
			else floors.add(new Floor(1, i+1, false, false));
		}
		
		floorSubsystem.running();
		
		//floorSubsystem.parseInputFile("test.txt");
		
		//for (Floor f : floors) { }
   }
}
