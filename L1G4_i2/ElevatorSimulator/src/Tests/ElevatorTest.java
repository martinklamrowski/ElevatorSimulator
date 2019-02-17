/*
 * @author Tashfiq Akhand
 * Test Case for ElevatorSystem
 */

package Tests;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import ElevatorSystem.*;
import junit.framework.*;

public class ElevatorTest extends TestCase{
	//declaring instance variables
	private Elevator e;
	private ElevatorDirection direction;
	private ElevatorStatus status;
	private ElevatorControl control;

	//Initializing the objects being tested
	public void setUp() throws Exception {
		
		e = new Elevator("4", direction.E_UP);
		control = new ElevatorControl(5000, 3, "4");
	}
	
	//Closing the objests being tested
	public void tearDown() throws Exception {
		e = null;
		control = null;
	}

	//testing all return type methods in Elevator.java and the toInt(String) method in the ElevatorControl.java
	public void test() {
		assertEquals("4", e.getCurrentFloor());
		assertEquals(4, e.getIntFloor());
		assertEquals(ElevatorDirection.E_UP, e.getDirection());
		Elevator e2 = new Elevator("4", direction.E_HOLD); //creating second elevator object
		assertEquals(ElevatorStatus.E_IN_USE, e.getStatus());
		assertEquals(4, control.toInt("4"));
		
//		String a = "0";
//		byte b[] = a.getBytes();
//		String ans = b.toString();
//		String[] msg = ans.replaceFirst("\0", "").split("\0");
//		assertEquals(msg, control.packetToString(b));
	}
	
	//Testing the created packeted 
	public void TestcreatePacketData() throws UnknownHostException {
		
		String data = "\0" + "1" + "\0" + "3" + "\0"; //packetType = 1
		byte[] dataB = data.getBytes(); //getting bytes from the data string
		DatagramPacket packet = new DatagramPacket(data.getBytes(), data.getBytes().length, InetAddress.getLocalHost(), 5000);
		assertEquals(packet, control.createPacket("1", "3", 5000));
	}
	
	

}
