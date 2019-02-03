/*
 * @author Tashfiq Akhand
 */

package Scheduler;

import static org.junit.Assert.*;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class HostTest {
	
	private Host host;
	private Scheduler s;

	@Before
	public void setUp() throws Exception {
		host = new Host();
		//s = new Scheduler(10, 20, "","");
	}

	@After
	public void tearDown() throws Exception {
		host = null;
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
	
	@Test	
	public void testparsePacket() {
		String a = "Group 4";
		byte b[] = a.getBytes();
		String ans[] = {"Group 4"};
		assertEquals(ans, host.parsePacket(b));
	}
	
	
	

}
