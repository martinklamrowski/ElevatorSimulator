/*
 * @author Tashfiq Akhand
 * Test Case for the utils
 */


package tests;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import junit.framework.*;
import utils.*;

public class UtilTest extends TestCase{
	//declaring instance variables
	private UtilClass util;
	
	//Initializing Objects being tested
	public void setUp() throws Exception {
		util = new UtilClass();
	}

	//Closing objects being tested
	public void tearDown() throws Exception {
		util = null;
	}

	//testing UtilClass
	public void Test() throws UnknownHostException {
		//testing parsePacket method
		String a = "Group 4";
		byte b[] = a.getBytes();
		String ans[] = {"Group 4"};
		assertEquals(ans, util.parsePacket(b));
		
		//testing creatPacket method
		String data = "\0" + "1" + "\0" + "3" + "\0";
		byte[] dataB = data.getBytes();
		DatagramPacket packet = new DatagramPacket(data.getBytes(), data.getBytes().length, InetAddress.getLocalHost(), 8008);
		assertEquals(packet, util.createPacket("1", "3", 8008));
		
		//testing calculateSuitability method
		assertEquals(8, util.calculateSuitability(7, 5, 5, "IDLE", "IDLE", "WORKING"));
		assertEquals(1, util.calculateSuitability(7, 5, 6, "DOWN", "IDLE", "WORKING"));
		assertEquals(7, util.calculateSuitability(7, 5, 3, "DOWN", "DOWN", "WORKING"));
		assertEquals(6, util.calculateSuitability(7, 5, 3, "DOWN", "IDLE", "WORKING"));
		assertEquals(1, util.calculateSuitability(7, 2, 1, "UP", "UP", "WORKING"));
		assertEquals(5, util.calculateSuitability(7, 2, 5, "UP", "IDLE", "WORKING"));
		assertEquals(6, util.calculateSuitability(7, 2, 5, "UP", "UP", "WORKING"));
		
	}
	
}
