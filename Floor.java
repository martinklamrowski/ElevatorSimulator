package floorsubsystem;

import java.io.*;
import java.net.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class Floor {
	
	int floorNum;
	int numElevators;
	boolean upButton;
	boolean downButton;
	boolean isUpperFloor;
	boolean isLowerFloor;
	
	Thread arrivalSensor;
	
	enum Direction {
		UP,
		DOWN,
		IDLE
	}
	
	Direction[] lampDirection;
	
	
	public Floor(int floorNo, int numElev, boolean upper, boolean lower) {
		floorNum = floorNo;
		numElevators = numElev;
		upButton = false;
		downButton = false;
        lampDirection = new Direction[numElev];
		for (int i = 0; i < numElev; i++) lampDirection[i] = Direction.IDLE;
        
		
		arrivalSensor = new Thread(new ArrivalSensor(this));
		arrivalSensor.start();
        
		
	}	
	
	public int getFloorNum() { return(this.floorNum); }
	
	public int getElevNum() { return(this.numElevators); }
	
	public Direction getDirection() { return(this.lampDirection[0]); }
	
	public void setUpButtonOn() { upButton = true; }
	public void setDownButtonOn() { downButton = true; }
	public void setUpButtonOff() { upButton = false; }
	public void setDownButtonOff() { downButton = false; }
	
	
}