/*
 * @author Tashfiq Akhand
 */

package FloorSystem;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import FloorSystem.Floor.Direction;
import junit.framework.TestCase;

public class FloorTest extends TestCase{
	private Floor floor;
	private FloorSubsystem fs;
	
	@Before
	public void setUp() throws Exception {
		floor = new Floor(5, 2, true, false);
		fs = new FloorSubsystem();
	}

	@After
	public void tearDown() throws Exception {
		floor = null;
		fs = null;
	}

	@Test
	public void test() {
		int floorNo = 5;
		int numElev = 2;
		assertEquals(floorNo, floor.getFloorNum());
		assertEquals(numElev, floor.getElevNum());
		floor.setUpLampOn();
		assertTrue(floor.upLamp);
		floor.setDownLampOn();
		assertTrue(floor.downLamp);
		floor.setUpLampOff();
		assertFalse(floor.upLamp);
		floor.setDownLampOff();
		assertFalse(floor.downLamp);
		
	}
	
	@Test
	public void TestparseInputFile() throws FileNotFoundException, IOException {
		fs.parseInputFile("00:00:45:674 1 UP 6");
	}
	
	@Test
	public void TestgetInputData() {
		Direction d = null;
		Object[] obj = new Object[7];
		obj[0] = 00;
		obj[1] = 00;
		obj[2] = 45;
		obj[3] = 674;
		obj[4] = 1;
		obj[5] = d.UP;
		obj[6] = 6;
		String s = "00:00:45:674 1 UP 6";
		assertEquals(obj, fs.getInputData(s));
	}
	
	@Test
	public void TestcreatePacketData() {
		String ins = "3";
		String data = "\0" + fs.ACK + "\0" + ins + "\0";
		byte[] dataB = data.getBytes();
		assertEquals(dataB, fs.createPacketData(1, "3"));
	}
	
	@Test
	public void TestreadPacketData() {
		String a = "Group 4";
		byte b[] = a.getBytes();
		String ans[] = {"Group 4"};
		assertEquals(ans, fs.readPacketData(b));
	}
	

}
