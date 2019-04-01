package floorSubsystem;

import java.io.*;
import java.net.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class Floor {
	
	int floorNum;
	int numElevators;
	boolean upLamp;
	boolean downLamp;
	boolean isUpperFloor;
	boolean isLowerFloor;
	
	
	enum Direction {
		UP,
		DOWN,
		IDLE
	}
	
	Direction[] lampDirection;
	
	
	public Floor(int numElev, int floorNo, boolean upper, boolean lower) {
		floorNum = floorNo;
		numElevators = numElev;
		upLamp = false;
		downLamp = false;
        lampDirection = new Direction[numElev];
		for (int i = 0; i < numElev; i++) lampDirection[i] = Direction.IDLE;
        
		
        
		
	}	
	
	public int getFloorNum() { return(this.floorNum); }
	public int getElevNum() { return(this.numElevators); }
	public boolean getUpLampStatus() { return(this.upLamp); }
	public boolean getDownLampStatus() { return(this.downLamp); }
	
	public Direction getDirection() { return(this.lampDirection[0]); }
	
	public void setUpLampOn() { upLamp = true; }
	public void setDownLampOn() { downLamp = true; }
	public void setUpLampOff() { upLamp = false; }
	public void setDownLampOff() { downLamp = false; }
	
	
}