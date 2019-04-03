package ElevatorSystem;


import java.awt.Color;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * this is the function where receive request from scheduler and control corresponding elevator to do the request
 * @author ariannashi
 * @coauthor Angie Byun
 *
 */
public class ElevatorControl extends Thread{
	private View v;
	
	/* ## HEADER AND COMMAND IDENTIFIERS ## */
	private static final String ERROR = "0";
	private static final String ACK = "1";
	private static final String CMD = "2";
	private static final String DATA = "3";	

	private static final String UP_PICKUP = "0x33";		// going up to pick up
	private static final String UP_DROPOFF = "0x32"; 	// going
	private static final String DOWN_PICKUP = "0x31";
	private static final String DOWN_DROPOFF = "0x30";
	private static final String DOOR_OPEN = "0x3A";
	private static final String DOOR_CLOSE = "0x3B";
	private static final String STOP = "0x3C";
	private static final String ERROR_DOOR_JAM = "0xE0";
	private static final String ERROR_STUCK = "0xE1";
	public static ElevatorControl Elv_1, Elv_2, Elv_3, Elv_4;
	
	
	/*	variable identifies	 */
	private static int num_elevator; 
	public static int getNum_elevator() {
		return num_elevator;
	}

	public void setNum_elevator(int num_elevator) {
		this.num_elevator = num_elevator;
	}

	private int num_lamp = 0;
	private static int send = 0;
	private static int s_elevator = 0;		//elevator status	1: pickup, lamp ON		0:drop off, lamp OFF
	private DatagramSocket sendSocket, receiveSocket;
	private DatagramPacket sendPacket, receivePacket, sendAPacket;
	private Elevator elevator;
	
	
	/**
	 * constructor, initialize socket and elevator lamp
	 */
	public ElevatorControl(int port, int num_elevator, String floor) {
		this.num_elevator = num_elevator;
		elevator = new Elevator(floor, ElevatorDirection.E_HOLD);
		/*--- INITIALIZE socket ---*/
		try {
			sendSocket = new DatagramSocket();
	        	receiveSocket = new DatagramSocket(port);
		} catch (SocketException se) {
			se.printStackTrace();
	        System.exit(1);
		}
		
		/*--- INITILIZE Elevator Lamp ---*
		for(int i =0; i<MAX_FLOOR; i++) {
			Lamp[i] = 0;
		}
*/		
	}
	
	public void run() {
		control();
	}
	
	/**
	 * function create DatagramPacket using packetType: HEADER, code: COMMAND IDENTIFIERS, and port
	 * @param packetType
	 * @param code
	 * @param port
	 * @return
	 */
	public static DatagramPacket createPacket(String packetType, String code, int port) {
		String data = "";
		DatagramPacket packet = null;

		if (packetType.equals("1")) {				// ACK
			data = "\0" + ACK + "\0" + code + "\0";
		} else if (packetType.equals("2")) {				//CMD
			data = "\0" + CMD + "\0" + code + "\0";
		} else if (packetType.equals("3")) {				//CMD
			data = "\0" + DATA + "\0" + code + "\0";
		} else if (packetType.equals("0")) {
			data = "\0" + ERROR + "\0" + code + "\0"; // ERROR
		}

		try {			
			packet = new DatagramPacket(data.getBytes(), data.getBytes().length, InetAddress.getLocalHost(), port);
		}catch (UnknownHostException uhe) {	
			System.out.println("ELEVATOR: unable to create packet (UnknownHostException), exiting.");
			System.exit(1);
		}

		return packet;
	}
	
	/**
	 * function release packet into string[]
	 * @param data
	 * @return
	 */
	public static String[] packetToString(byte[] data) {
		String info = new String(data);
		String[] msg = info.replaceFirst("\0", "").split("\0");
		return msg;
	}
	
	/**
	 *  function transfers string to int 
	 * @param string
	 * @return
	 */
	public int toInt(String string) {
		int a = 0;
		try {
			a = Integer.parseInt(string);
		} catch (NumberFormatException e) {
			System.out.println("ELEVATOR " + num_elevator + "ERROR current floor");
		}	
		return a;
	}
	
		
	/**
	 * function called in main to control corresponding elevator do the received request
	 */
	public void control() {
		byte _data[] = new byte[100];
		receivePacket = new DatagramPacket(_data, _data.length);
		String[] ins = packetToString(receivePacket.getData());
		String[] cmd = null;
		String[] data = null;
		String[] error = null;
		
		while(true) {
			try {
				System.out.println("ELEVATOR " + num_elevator + ": Elevator Waiting ..."); // so we know we're waiting
		        	receiveSocket.receive(receivePacket);
		        	System.out.print("ELEVATOR " + num_elevator + ": received ");
		    	} catch (IOException e) {
		    		System.out.print("ELEVATOR " + num_elevator + ": IO Exception: likely:");
		        	System.out.println("Receive Socket Timed Out.\n" + e);
		        	e.printStackTrace();
		        	System.exit(1);
		    	}
			
			ins = packetToString(receivePacket.getData());
			
			switch (ins[0]) {
				
			case CMD:
				/*====== CMD packet received ======*/
				cmd = ins;
				
				switch (cmd[1]) {
				/*--- cases for different CMD code ---**/
				case UP_DROPOFF:
					System.out.println("cmd, UP for drop off");
					elevator.direction = ElevatorDirection.E_UP;	//move up
					elevator.run();
					sendPacket = createPacket(DATA, elevator.getCurrentFloor(),receivePacket.getPort());
					System.out.println("ELEVATOR " + num_elevator + ": Elevator moved up, now at Floor " + elevator.getCurrentFloor());
					send = 1;	// send elevator location
					s_elevator = 0;		// elevator job drop off
					break;		// end UP_DROPOFF				

				case UP_PICKUP:	
					System.out.println("cmd, UP for pick up");
					for(int i=0; i<2; i++) {
						v.getElevatorDirections(i, num_elevator).setBackground(Color.WHITE);
					}
					v.getElevatorDirections(0, num_elevator).setBackground(Color.GREEN);
					System.out.println("ELEVATOR " + num_elevator + ": wait for elevator data ");
					s_elevator = 1;		// elevator job pick up
					send = 0;
					break;		// end UP_PICKUP

				case DOWN_DROPOFF:
					System.out.println("cmd, DOWN for drop off");
					elevator.direction = ElevatorDirection.E_DOWN;	//move down
					elevator.run();
					System.out.println("ELEVATOR " + num_elevator + ": Elevator move DOWN, now at Floor " + elevator.getCurrentFloor());
					sendPacket = createPacket(DATA, elevator.getCurrentFloor(),receivePacket.getPort());
					send = 1;				// send elevator location
					s_elevator = 0;			// elevator job drop off
					break;		// end DOWN_DROPOFF

				case DOWN_PICKUP:
					System.out.println("cmd, DOWN for pick up");
					for(int i=0; i<2; i++) {
						v.getElevatorDirections(i, num_elevator).setBackground(Color.WHITE);
					}
					v.getElevatorDirections(1, num_elevator).setBackground(Color.GREEN);
					System.out.println("ELEVATOR " + num_elevator + ": wait for elevator data ");
					s_elevator = 1;		// elevator job pick up
					send = 0;
					break;		// end DOWN_PICKUP

				case DOOR_OPEN:	
					System.out.println("cmd, OPEN door");
					elevator.open(num_elevator);
					send = 0;
					
					/* ### THERE IS A FAULTY SENSOR IN SHAFT 3 AT FLOOR 5 ### */
					if (num_elevator == 3 & elevator.getCurrentFloor().equals("5")) {
						try {
							System.out.println("ELEVATOR " + num_elevator + " is STUCK at Floor " + elevator.getCurrentFloor());
							v.getElevatorfloors(Integer.parseInt(elevator.getCurrentFloor()), num_elevator).setBackground(Color.RED);
							Thread.sleep(10000);
						}
						catch (Exception e) {
							e.printStackTrace();
						}
					}
					/* ### END OF ERROR INJECTION ### */
						
					else if  (s_elevator != -1) {
						if (s_elevator == 1) {
							/*--- door open for pick up, elevator lamp ON ---*/
							elevator.Lamp[num_lamp-1] = s_elevator;
							System.out.println("ELEVATOR " + num_elevator + ": Elevator Lamp ON at " + num_lamp);
						} else if (s_elevator == 0) {
							/*--- door open for drop off, elevator lamp OFF ---*/
							elevator.Lamp[elevator.getIntFloor()-1] = s_elevator;
							System.out.println("ELEVATOR " + num_elevator + ": Elevator Lamp OFF at " + num_lamp);
						} 
						s_elevator = -1;		// elevator job open door
					}// end if not -1
						
					
					break;		// end DOOR_OPEN				

				case DOOR_CLOSE:
					System.out.println("cmd, CLOSE door");
					elevator.close(num_elevator);
					send = 0;
					s_elevator = -1;		// elevator job open close
					break;		//end DOOR_CLOSE

				case STOP:
					System.out.println("cmd, STOP");
					elevator.direction = ElevatorDirection.E_HOLD;
					elevator.run();
					System.out.println("ELEVATOR " + num_elevator + ": Elevator STOPPED at " + elevator.getCurrentFloor());
					send = 0;
					break;		// end STOP

				}// end CMD switch
				
				/*--- send ACK for CMD ---**/
				sendAPacket= createPacket(ACK, ins[1], receivePacket.getPort());
				try {
					sendSocket.send(sendAPacket);
				} catch (IOException e1) {
					e1.printStackTrace();
				    System.exit(1);
				}
				
				/*--- send elevator location ---*/
				if (send == 1) {
					try {
						sendSocket.send(sendPacket);
					} catch (IOException e1) {
						e1.printStackTrace();
						System.exit(1);
					}
					send = 0;
				}// end if (location update)
				break;		// end CMD			

			case ACK:
				System.out.println("ack");
				/*----- ACK packet received -----*/
				switch (cmd[1]) {
				case UP_PICKUP:	
					elevator.direction = ElevatorDirection.E_UP;		// move up
					elevator.run();
					System.out.println("ELEVATOR " + num_elevator + ": Elevator moved UP, now at Floor " + elevator.getCurrentFloor());
					sendPacket = createPacket(DATA, elevator.getCurrentFloor(),receivePacket.getPort()); 		// send elevator location
					s_elevator = 1;				
					break;		// end UP_PICKUP

				case DOWN_PICKUP:
					elevator.direction = ElevatorDirection.E_DOWN;		// move down
					elevator.run();
					System.out.println("ELEVATOR " + num_elevator + ": Elevator moved DOWN, now at Floor " + elevator.getCurrentFloor());
					sendPacket = createPacket(DATA, elevator.getCurrentFloor(),receivePacket.getPort()); 
					s_elevator = 1;
					break;		// end DOWN_PICKUP

				case UP_DROPOFF:
					elevator.direction = ElevatorDirection.E_UP;	//move up
					elevator.run();
					System.out.println("ELEVATOR " + num_elevator + ": Elevator moved UP, now at Floor " + elevator.getCurrentFloor());
					sendPacket = createPacket(DATA, elevator.getCurrentFloor(),receivePacket.getPort());
					s_elevator = 0;
					break;		// end UP_DROPOFF

				case DOWN_DROPOFF:
					elevator.direction = ElevatorDirection.E_DOWN;	//move down
					elevator.run();
					System.out.println("ELEVATOR " + num_elevator + ": Elevator moved DOWN, now at Floor " + elevator.getCurrentFloor());
					sendPacket = createPacket(DATA, elevator.getCurrentFloor(),receivePacket.getPort());
					s_elevator = 0;		// elevator job drop off
					break;		// end DOWN_DROPOFF
					
				
				}// end CMD switch
				
				/*--- send elevator location message ---*/
				try {
					sendSocket.send(sendPacket);
				} catch (IOException e1) {
					e1.printStackTrace();
					System.exit(1);
				}
				
				/* ### THERE IS A STUCKY THING IN ELEVATOR SHAFT 4 AT FLOOR 15 ### */
				if (num_elevator == 4 & elevator.getCurrentFloor().equals("15")) {
					try {
						System.out.println("ELEVATOR " + num_elevator + " is STUCK at Floor " + elevator.getCurrentFloor());
						v.getElevatorfloors(Integer.parseInt(elevator.getCurrentFloor()), num_elevator).setBackground(Color.RED);
						Thread.sleep(10000);
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}
				/* ### END OF ERROR INJECTION ### */
				
				break;		// end ACK
			
			case DATA:
				System.out.println("data");
				/*----- DATA packet received -----*/
				data = ins;
				switch (cmd[1]) {
				case UP_PICKUP:
					elevator.direction = ElevatorDirection.E_UP;	//move up
					elevator.run();
					sendPacket = createPacket(DATA, elevator.getCurrentFloor(),receivePacket.getPort());
					System.out.println("ELEVATOR " + num_elevator + ": Elevator moved UP, now at Floor " + elevator.getCurrentFloor());
					s_elevator = 1;							// elevator job pick up
					num_lamp = toInt(data[1]); 				// record elevator lamp
					send = 1;
					break;

				case DOWN_PICKUP:
					elevator.direction = ElevatorDirection.E_DOWN;	//move down
					elevator.run();
					sendPacket = createPacket(DATA, elevator.getCurrentFloor(),receivePacket.getPort());
					System.out.println("ELEVATOR " + num_elevator + ": Elevator moved DOWN, now at Floor " + elevator.getCurrentFloor());
					s_elevator = 1;							// elevator job pick up
					num_lamp = toInt(data[1]); 				// record elevator lamp
					send = 1;
					break;

				case STOP:
					s_elevator = 1;							// elevator job pick up
					num_lamp = toInt(data[1]); 				// record elevator lamp
					send = 0;
					break;				
				}// end CMD switch
					
				/*--- send ACK message ---*/
				sendAPacket = createPacket(ACK, data[1], receivePacket.getPort());
				try {
					sendSocket.send(sendAPacket);
				} catch (IOException e1) {
					e1.printStackTrace();
				    System.exit(1);
				}
				/*--- send elevator location ---*/
				if (send == 1) {
					try {
						sendSocket.send(sendPacket);
					} catch (IOException e1) {
						e1.printStackTrace();
						System.exit(1);
					}
				}// end update location
				break;
			
			case ERROR:
				/*----- ERROR packet received -----*/
				error = ins;
				switch (error[1]) {
				case ERROR_DOOR_JAM:
					System.out.println("!!!ERROR DOOR JAM!!! ELEVATOR SERVICE POSTPONED");
					v.getElevatorfloors(Integer.parseInt(elevator.getCurrentFloor()), num_elevator).setBackground(Color.RED);
					break;
				case ERROR_STUCK:
					System.out.println("!!!ERROR ELEVATOR STUCK!!! ELEVATOR SERVICE POSTPONED");
					v.getElevatorfloors(Integer.parseInt(elevator.getCurrentFloor()), num_elevator).setBackground(Color.RED);
					break;
				}				
				try {					
					sendPacket = createPacket(ACK, error[1], receivePacket.getPort());		
				}
				catch (Exception e) {
					e.printStackTrace();
				}				
				break;
				
			default: 
				break;
				
			}//end header switch
		}//end while (true)
	}// end control()
	
	/**
	 *  main function of the ElevatorControl class 
	 */
	public static void main(String[] args) {
		View v = new View();
		
		Elv_1 = new ElevatorControl(3137, 1, "1");
		Elv_2 = new ElevatorControl(3237, 2, "1");
		Elv_3 = new ElevatorControl(3337, 3, "10");
		Elv_4 = new ElevatorControl(3437, 4, "20");
		
		Elv_1.start();
		Elv_2.start();
		Elv_3.start();
		Elv_4.start();

	}// end main	

}// end class
