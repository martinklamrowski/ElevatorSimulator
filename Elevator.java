import java.util.LinkedList;


public class Elevator {
	
	private int currentFloor;
	private LinkedList<Integer> destFloors;

	public Elevator(int currentFloor) {
		this.currentFloor = currentFloor;
		this.destFloors = new LinkedList<Integer>();
		
	}
	/*
	 * check the next destination of the elevator
	 */
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
	
	/*
	 * return the location of the elevator
	 */
	public int getCurrentFloor() {
		return this.currentFloor;
	}
	
	/*
	 * remove the arrived destination from the destFloors list
	 */
	public void removeDestination(int dest) {
		this.destFloors.remove(dest);
	}
	
	/*
	 * add new destination to the destFloors list in numeric order
	 */
	public void addDestination(int dest) {
		for (int i = 0; i < destFloors.size(); i++) {
			if (this.destFloors.get(i+1) > dest) {
				this.destFloors.add(i+1,dest);
			}
		}
		
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
		if (destFloors.size() > 0) {
			if(currentFloor < destFloors.peek()) {
				return ElevatorDirection.E_UP;
			}else if (currentFloor > destFloors.peek()) {
				return ElevatorDirection.E_DOWN;
			}
		}
		return ElevatorDirection.E_HOLD;
	}
	
	/*
	 * return the current status for the elevator
	 */
	public ElevatorStatus getStatus() {
		return (destFloors.size() > 0)?ElevatorStatus.E_IN_USE:ElevatorStatus.E_EMPTY;
		
	}
	
/*	
	public byte sensor() {
		if(this.getDirection().equals(ElevatorDirection.E_UP)) {
			try {
				Thread.sleep(1000);
				
			}catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		return 0;
		
	}
*/

}



















