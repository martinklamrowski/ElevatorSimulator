package ElevatorSystem;

/**
 * this is the class declare the elevator object 
 * @author ariannashi
 *
 */
public class Elevator implements Runnable {
	public String currentFloor;
	public ElevatorDirection direction;
	//public boolean _exit = false;
	private static final int MAX_FLOOR = 22;		// maximum floor number
	private int id;

	ElevatorControl ec;
	
	public Elevator(String currentFloor, ElevatorDirection direction, ElevatorControl elevatorControl, int idNo) {
		ec = elevatorControl;
		id = idNo;
		this.currentFloor = currentFloor;
		this.direction = direction;
		
	}
	 	
	/**
	 *  function used for thread to make the elevator move and act
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
	 * return the location of the elevator in string
	 * @return
	 */
	public String getCurrentFloor() {
		return this.currentFloor;
	}
	
	/**
	* return the location of elevator in integer
	* @return 
	*/
	public int getIntFloor() {
		int pos = 0;
		try {
			pos = Integer.parseInt(this.currentFloor);
		} catch (NumberFormatException e) {
			System.out.println("ELEVATOR: ERROR current floor");
		}	
		return pos;
	}
	
	public int getId() {
		return id;
	}

	/**
	 * return the current direction of elevator
	 * move up: E_UP
	 * move down: E_DOWN
	 * stop: E_HOLD
	 * @return
	 */
	public ElevatorDirection getDirection() {
		
		return this.direction;
	}
	
	/**
	 * return the current status for the elevator
	 * elevator acting: E_IN_USE
	 * elevator is stopping: E_EMPTY
	 * @return
	 */
	public ElevatorStatus getStatus() {
		return (this.getDirection().equals(ElevatorDirection.E_HOLD))?ElevatorStatus.E_EMPTY: ElevatorStatus.E_IN_USE;
	}
	
	/**
	 * open the door of the elevator
	 */
	public void open() {
		// wait 2 seconds and open door
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
		    	e.printStackTrace();
		    	System.exit(1);
		}
		System.out.println("ELEVATOR " + id + ": Door OPENED at " + this.getCurrentFloor());
	}
	
	/**
	 * close elevator door
	 */
	public void close() {
		// wait 2 seconds and close door
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
		    	e.printStackTrace();
		    	System.exit(1);
		}

		System.out.println("ELEVATOR " + id + ": Door CLOSED at " + this.getCurrentFloor());
	}
	

}
