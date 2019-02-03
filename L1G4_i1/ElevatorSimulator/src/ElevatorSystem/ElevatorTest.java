/*
 * @author Tashfiq Akhand
 */

package ElevatorSystem;

import static org.junit.Assert.*;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ElevatorTest {
	private Elevator e;
	private ElevatorDirection direction;
	private ElevatorStatus status;
	private ElevatorControl control;

	@Before
	public void setUp() throws Exception {
		
		e = new Elevator("4", direction.E_UP);
		control = new ElevatorControl();
	}

	@After
	public void tearDown() throws Exception {
		e = null;
		control = null;
	}

	@Test
	public void test() {
		assertEquals("4", e.getCurrentFloor());
		assertEquals(4, e.getIntFloor());
		assertEquals(ElevatorDirection.E_UP, e.getDirection());
		Elevator e2 = new Elevator("4", direction.E_HOLD);
		assertEquals(ElevatorStatus.E_IN_USE, e.getStatus());
		assertEquals(4, control.toInt("4"));
		
		String a = "0";
		byte b[] = a.getBytes();
		String ans[] = {"0"};
		assertEquals(ans, control.packetToString(b));
	}
	
	//wont work due to different host IP
//	@Test
//	public void testcreatePacket() throws UnknownHostException {
//		DatagramPacket dp = null;
//		String data = "\0" + "1" + "\0" + "code" + "\0";
//		dp = new DatagramPacket(data.getBytes(), data.getBytes().length, InetAddress.getLocalHost(), 5);
//		assertEquals(dp, control.createPacket("1", "code", 5));
//	}
	
	

}
