/**
 * 
 * @author ariannashi
 *
 */
public class Elevator implements Runnable {
	public String currentFloor;
	public ElevatorDirection direction;
	//public boolean _exit = false;
	private static final int MAX_FLOOR = 10;		// maximum floor number

	public Elevator(String currentFloor, ElevatorDirection direction) {
		this.currentFloor = currentFloor;
		this.direction = direction;
		
	}
	 	
	/**
	 * 
	 */
	@Override
	public void run() {
		int pos = 0;
		try {
			pos = Integer.parseInt(this.currentFloor);		    			
		} catch (NumberFormatException e) {
			System.out.println("ELEVATOR: ERROR current floor");
		}	
		
	    if (direction == ElevatorDirection.E_UP && pos < MAX_FLOOR) {
	    	// wait 6 seconds and move
		    try {
		    	Thread.sleep(6000);
		    } catch (InterruptedException e) {
		    	e.printStackTrace();
		    	System.exit(1);
		    }
	    	pos += 1;
	        this.currentFloor = Integer.toString(pos);
	        // Send pos
	    } else if (direction == ElevatorDirection.E_DOWN && pos > 1) {
	    	// wait 6 seconds and move
		    try {
		    	Thread.sleep(6000);
		    } catch (InterruptedException e) {
		    	e.printStackTrace();
		    	System.exit(1);
		    }
	    	pos -= 1;
	        this.currentFloor = Integer.toString(pos);
	        // send pos
	    } else if ( direction == ElevatorDirection.E_HOLD) {
	    	this.currentFloor = this.getCurrentFloor();
	    }else {
	    	System.out.println("ELEVATOR: ERROR elevator direction");
	    }
	}

	/**
	 * return the location of the elevator
	 * @return
	 */
	public String getCurrentFloor() {
		return this.currentFloor;
	}
	
	public int getIntFloor() {
		int pos = 0;
		try {
			pos = Integer.parseInt(this.currentFloor);
		} catch (NumberFormatException e) {
			System.out.println("ELEVATOR: ERROR current floor");
		}	
		return pos;
	}

	/**
	 * return the current moving direction of elevator
	 * @return
	 */
	public ElevatorDirection getDirection() {
		
		return this.direction;
	}
	
	/**
	 * return the current status for the elevator
	 * @return
	 */
	public ElevatorStatus getStatus() {
		return (this.getDirection().equals(ElevatorDirection.E_HOLD))?ElevatorStatus.E_EMPTY: ElevatorStatus.E_IN_USE;
	}
	
	/**
	 * return the current status for the elevator
	 */
	public void open() {
		System.out.println("ELEVATOR: Door OPENED at " + this.getCurrentFloor());
	}
	
	/**
	 * 
	 */
	public void close() {
		System.out.println("ELEVATOR: Door CLOSED at " + this.getCurrentFloor());
	}
	
	
	/*
	 * remove the arrived destination from the destFloors list
	 */
	/*
	public void removeDestination(int dest) {
		this.destFloors.remove(dest);
	}
	*/
	
	/*
	 * add new destination to the destFloors list in numeric order
	 */
	/*
	public void addDestination(int dest) {
		for (int i = 0; i < destFloors.size(); i++) {
			if (this.destFloors.get(i+1) > dest) {
				this.destFloors.add(i+1,dest);
			}
		}
		
	}
	*/
	
	
	/*
	 * check the next destination of the elevator
	 */
	/*
	public int getNextDestination() {
		int next_dest = 0;
		switch (this.getDirection()) {
		case E_DOWN:
			for (int i = 0; i < destFloors.size(); i++) {
				if (this.destFloors.get(i+1) > this.currentFloor) {
					next_dest = this.destFloors.get(i);
				}
			}
			break;

		case E_UP:
			for (int i = destFloors.size()-1; i < destFloors.size(); i++) {
				if (this.destFloors.get(i-1) < this.currentFloor) {
					next_dest = this.destFloors.get(i);
				}
			}
			break;
		case E_HOLD:
			next_dest = -0;
			break;
		
		}
		return next_dest;
		
	}
	
	*/
	
	

}



















