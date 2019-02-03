import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import ElevatorSystem.ElevatorTest;
import FloorSystem.FloorTest;
import Scheduler.HostTest;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

@RunWith(Suite.class)
@SuiteClasses({ 
	FloorTest.class, 
	HostTest.class,
	ElevatorTest.class
})
public class AllTests extends TestCase {
	
	public static void main(String[] args) { 
		junit.textui.TestRunner.run(AllTests.class);
	} 

	public static Test suite() {
		TestSuite suite = new TestSuite(); 
		suite.addTest(new TestSuite(FloorTest.class)); 	
		suite.addTest(new TestSuite(HostTest.class));
		suite.addTest(new TestSuite(ElevatorTest.class)); 
		return suite;
	} 

}
