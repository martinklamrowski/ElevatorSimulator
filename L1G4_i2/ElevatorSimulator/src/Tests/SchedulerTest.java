/*
 * @author Tashfiq Akhand
 */

package Tests;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import Scheduler.*;
import junit.framework.*;

public class SchedulerTest extends TestCase{
	
	private Scheduler s;
	private ElevatorHandler handler;

	public void setUp() throws Exception {
		s = new Scheduler();
		BlockingQueue<String> upQ = new ArrayBlockingQueue<String>(100);
		BlockingQueue<String> downQ = new ArrayBlockingQueue<String>(100);
		handler = new ElevatorHandler(3137, 6520, upQ, downQ, 3, "UP", 2);
		//s = new Scheduler(10, 20, "","");
	}

	public void tearDown() throws Exception {
		s = null;
	}
	
	//different host IP so wont work
//	@Test
//	public void TestcreatePacket() throws UnknownHostException {
//		String ins = "3";
//		String data = "\0" + host.ACK + "\0" + ins + "\0";
//		byte[] dataB = data.getBytes();
//		DatagramPacket dp = new DatagramPacket(data.getBytes(), data.getBytes().length, InetAddress.getLocalHost(), 6);
//		assertEquals(dp, Host.createPacket("1", "3", 6));
//	}
	
	public void testparsePacket() {
		String a = "Group 4";
		byte b[] = a.getBytes();
		String ans[] = {"Group 4"};
		assertEquals(ans, s.parsePacket(b));
	}
	
	public void testSuitabilityCalculation() {
		assertEquals(6, handler.calculateSuitability(7, 5, 5, "IDLE", "IDLE"));
		assertEquals(7, handler.calculateSuitability(7, 5, 3, "DOWN", "DOWN"));
		assertEquals(6, handler.calculateSuitability(7, 5, 3, "DOWN", "IDLE"));
		assertEquals(1, handler.calculateSuitability(7, 5, 6, "DOWN", "IDLE"));
		assertEquals(1, handler.calculateSuitability(7, 2, 1, "UP", "UP"));
		assertEquals(5, handler.calculateSuitability(7, 2, 5, "UP", "IDLE"));
		assertEquals(6, handler.calculateSuitability(7, 2, 5, "UP", "UP"));
	}
	
	
	

}
