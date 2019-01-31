package floorsubsystem;

import java.io.*;
import java.net.*;

public class ArrivalSensor implements Runnable{
	
	DatagramPacket sendPacket, receivePacket;
	DatagramSocket sendReceiveSocket;
	
	Floor floor;
	   
	public ArrivalSensor(Floor f)
	{
		floor = f;
		try {
			sendReceiveSocket = new DatagramSocket();
		} catch (SocketException se) {   // Can't create the socket.
			se.printStackTrace();
			System.exit(1);
		}
	}

	@Override
	public void run() {

		while(true) {
			
			byte data[] = new byte[100];
			receivePacket = new DatagramPacket(data, data.length);
			try {
				sendReceiveSocket.receive(receivePacket);

			} catch (IOException e) {
		         e.printStackTrace();
		         System.exit(1);
			}
			
			System.out.println("Arrival Sensor: Packet recieved");
			
			
		}
		
	}
}
