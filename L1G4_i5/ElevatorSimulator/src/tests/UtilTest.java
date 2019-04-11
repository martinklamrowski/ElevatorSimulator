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
	public void test() throws UnknownHostException {
		//testing calculateSuitability method
		assertEquals(1, UtilClass.calculateSuitability(7, 5, 6, "DOWN", "IDLE", "WORKING"));
		assertEquals(7, UtilClass.calculateSuitability(7, 5, 3, "DOWN", "DOWN", "WORKING"));
		assertEquals(6, UtilClass.calculateSuitability(7, 5, 3, "DOWN", "IDLE", "WORKING"));
		assertEquals(1, UtilClass.calculateSuitability(7, 2, 1, "UP", "UP", "WORKING"));
		assertEquals(5, UtilClass.calculateSuitability(7, 2, 5, "UP", "IDLE", "WORKING"));
		assertEquals(6, UtilClass.calculateSuitability(7, 2, 5, "UP", "UP", "WORKING"));
		assertEquals(-1, UtilClass.calculateSuitability(7, 2, 5, "UP", "UP", "SUSPENDED"));

		
	}
	
}
