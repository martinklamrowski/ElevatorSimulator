import java.io.*;



public class Elevator implements Runnable {
	private int currentFloor;
	private ElevatorDirection direction;
	public boolean _exit = false;

	public Elevator(int currentFloor, ElevatorDirection direction, ElevatorStatus status) {
		this.currentFloor = currentFloor;
		this.direction = direction;
		
	}
	 
	@Override
	public void run() {
    	while(!_exit)
    	{
     		if (direction == ElevatorDirection.E_UP && this.currentFloor < 22)
     		{
     			this.currentFloor += 1;
     			// Send pos
     		}
     		else if ( direction == ElevatorDirection.E_DOWN && this.currentFloor > 1 )
     		{
     			this.currentFloor -= 1;
     			// send pos
     		}
     		else
     		{
     			//fuck
     		}
     		
     		try {
				Thread.sleep(6000);
			} catch (InterruptedException e) {
				e.printStackTrace();
				System.exit(1);
			}     		
     	}   		
		
		return;
	}

	/*
	 * return the location of the elevator
	 */
	public int getCurrentFloor() {
		return this.currentFloor;
	}

	/*
	 * increase current floor by 1
	 * @see elevatorFactor#moveUp()
	 */
	
	public void moveUp() {
		
		currentFloor+=1;
	}
	
	/*
	 * reduce the current floor by 1
	 * @see elevatorFactor#moveDown()
	 */
	public void moveDown() {
		currentFloor-=1;
	}
	
	/*
	 * return the current moving direction of elevator
	 */
	public ElevatorDirection getDirection() {
		/*
		if (destFloors.size() > 0) {
			if(currentFloor < destFloors.peek()) {
				return ElevatorDirection.E_UP;
			}else if (currentFloor > destFloors.peek()) {
				return ElevatorDirection.E_DOWN;
			}
		}
		*/
		return this.direction;
	}
	
	/*
	 * return the current status for the elevator
	 */
	public ElevatorStatus getStatus() {
		//return (destFloors.size() > 0)?ElevatorStatus.E_IN_USE:ElevatorStatus.E_EMPTY;
		return (this.getDirection().equals(ElevatorDirection.E_HOLD))?ElevatorStatus.E_EMPTY: ElevatorStatus.E_IN_USE;
	}
	

	public void open() {
		System.out.println("doors are opened");
	}
	
	public void close() {
		System.out.println("doors are closed");
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



















