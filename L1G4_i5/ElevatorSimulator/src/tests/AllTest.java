package tests;
import junit.framework.*;
import tests.*;


public class AllTest extends TestCase {
	
	public static void main(String[] args) { 
		junit.textui.TestRunner.run(AllTest.class);
	} 

	public static Test suite() {
		TestSuite suite = new TestSuite("Test fpr Elevator Simulator system"); 
		suite.addTest(new TestSuite(FloorTest.class)); 	
		suite.addTest(new TestSuite(UtilTest.class));
		return suite;
	} 

}
