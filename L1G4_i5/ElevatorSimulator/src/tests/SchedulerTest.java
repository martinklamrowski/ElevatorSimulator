/*
 * @author Tashfiq Akhand
 * Test Case for the Scheduler
 */

package tests;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import junit.framework.*;
import scheduler.*;

public class SchedulerTest extends TestCase{
	//Declaring instance variables
	private Scheduler s;
	
	//Initializing object being tested
	public void setUp() throws Exception {
		s = new Scheduler();
		
	}

	//closing object being tested
	public void tearDown() throws Exception {
		s = null;
	}
	
	//testing all return type methods in Scheduler.java
	public void Test() throws UnknownHostException {
		
		//Testing createPacker method in Scheduler class
		String data = "\0" + "1" + "\0" + "3" + "\0";
		byte[] dataB = data.getBytes();
		DatagramPacket packet = new DatagramPacket(data.getBytes(), data.getBytes().length, InetAddress.getLocalHost(), 8008);
		assertEquals(packet, s.createPacket("1", "3", 8008));
		
		//testing calculateSuitability method in Scheduler class
		assertEquals(8, s.calculateSuitability(7, 5, 5, "IDLE", "IDLE", "WORKING"));
		assertEquals(1, s.calculateSuitability(7, 5, 6, "DOWN", "IDLE", "WORKING"));
		assertEquals(7, s.calculateSuitability(7, 5, 3, "DOWN", "DOWN", "WORKING"));
		assertEquals(6, s.calculateSuitability(7, 5, 3, "DOWN", "IDLE", "WORKING"));
		assertEquals(1, s.calculateSuitability(7, 2, 1, "UP", "UP", "WORKING"));
		assertEquals(5, s.calculateSuitability(7, 2, 5, "UP", "IDLE", "WORKING"));
		assertEquals(6, s.calculateSuitability(7, 2, 5, "UP", "UP", "WORKING"));
		
		//testing parsePacket method in Scheduler class
		String a = "Group 4";
		byte b[] = a.getBytes();
		String ans[] = {"Group 4"};
		assertEquals(ans, s.parsePacket(b));
	}
}
