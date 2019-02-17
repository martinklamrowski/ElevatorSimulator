/*
 * @author Tashfiq Akhand
 * Test Case for the Scheduler
 */

package Tests;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;


import Scheduler.*;
import junit.framework.*;

public class SchedulerTest extends TestCase{
	
	private Scheduler s;

	public void setUp() throws Exception {
		s = new Scheduler();
		
	}

	public void tearDown() throws Exception {
		s = null;
	}
	
	public void Test() throws UnknownHostException {
		String data = "\0" + "1" + "\0" + "3" + "\0";
		byte[] dataB = data.getBytes();
		DatagramPacket packet = new DatagramPacket(data.getBytes(), data.getBytes().length, InetAddress.getLocalHost(), 8008);
		assertEquals(packet, s.createPacket("1", "3", 8008));
		assertEquals(8, s.calculateSuitability(7, 5, 5, "IDLE", "IDLE"));
		assertEquals(1, s.calculateSuitability(7, 5, 6, "DOWN", "IDLE"));
		assertEquals(7, s.calculateSuitability(7, 5, 3, "DOWN", "DOWN"));
		assertEquals(6, s.calculateSuitability(7, 5, 3, "DOWN", "IDLE"));
		assertEquals(1, s.calculateSuitability(7, 2, 1, "UP", "UP"));
		assertEquals(5, s.calculateSuitability(7, 2, 5, "UP", "IDLE"));
		assertEquals(6, s.calculateSuitability(7, 2, 5, "UP", "UP"));
		String a = "Group 4";
		byte b[] = a.getBytes();
		String ans[] = {"Group 4"};
		assertEquals(ans, s.parsePacket(b));
	}
}
