/*
 * @author Tashfiq Akhand
 * Test Case for FloorSystem 
 */

package Tests;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.io.*;
import FloorSystem.*;
import FloorSystem.FloorSubsystem.Direction;
import junit.framework.*;

public class FloorTest extends TestCase{
	//declaring instance variables
	private Floor floor;
	private FloorSubsystem fs;
	private UserInputHandler uih;
	
	//Initializing Objects being tested
	public void setUp() throws Exception {
		floor = new Floor(2, 5, true, false);
		fs = new FloorSubsystem();
		uih = new UserInputHandler(fs);
	}

	//Closing objects being tested
	public void tearDown() throws Exception {
		floor = null;
		fs = null;
	}

	//testing Floor class
	public void testFloor() {
		int floorNo = 5;
		int numElev = 2;
		assertEquals(floorNo, floor.getFloorNum());
		assertEquals(numElev, floor.getElevNum());
	}
	
	public void TestparseInputFile() throws FileNotFoundException, IOException {
		//fs.parseInputFile("00:00:45:674 1 UP 6");
	}
//	
//	//Testing getInputData(String) method in UserInputHandler class
//	public void TestgetInputData() {
//		Direction d = null;
//		Object[] obj = new Object[7]; //initializing array of objects
//		obj[0] = 00;
//		obj[1] = 00;
//		obj[2] = 45;
//		obj[3] = 674;
//		obj[4] = 1;
//		obj[5] = d.UP;
//		obj[6] = 6;
//		String s = "00:00:45:674 1 UP 6";
//		assertEquals(obj, uih.getInputData(s));
//	}
	
	public void TestcreatePacketData() {
		String ins = "3";
		String data = "\0" + 1 + "\0" + ins + "\0";
		byte[] dataB = data.getBytes();
		assertEquals(dataB, fs.createPacketData(1, "3"));
	}
	
	public void TestreadPacketData() {
		String a = "Group 4";
		byte b[] = a.getBytes();
		String ans[] = {"Group 4"};
		assertEquals(ans, fs.readPacketData(b));
	}
	
//	public void testReceive() throws SocketException {
//		String s = "Group4";
//		byte msg[] = s.getBytes();
//		DatagramPacket packet = new DatagramPacket(msg, msg.length);
//		DatagramSocket socket = new DatagramSocket();
//		assertEquals(packet, fs.receive(socket, msg));
//	}
	
}
