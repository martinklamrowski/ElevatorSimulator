package ElevatorSystem;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * this is the function where receive request from scheduler and control corresponding elevator to do the request
 * @author ariannashi
 *
 */
public class ElevatorControl {
	/* ## HEADER AND COMMAND IDENTIFIERS ## */
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
	
	/* ## CONSTANT VARIABLE IDENTIFIES ## */
	private static final int aport = 3137;
	private static final int MAX_FLOOR = 10;
	
	/*	variable identifies	 */
	//private static int s_lamp = 0;		// 1: lamp on	0: lamp off
	private static int num_lamp = 0;
	private static int send = 0;
	private static int s_elevator = 0;		//elevator status	1: pickup, lamp ON		0:drop off, lamp OFF
	int[] Lamp = new int[MAX_FLOOR];
	private DatagramSocket sendSocket, receiveSocket;
	private DatagramPacket sendPacket, receivePacket, sendAPacket;
	
	
	//private static final int MAX_ELEVATORS = 0;
	//private int num_of_elevator;
	private Elevator elevator = new Elevator("0", ElevatorDirection.E_HOLD);
	
	
	/**
	 * constructor, initalize socket and elevator lamp
	 */
	public ElevatorControl() {
		/*--- INITIALIZE socket ---*/
		try {
			sendSocket = new DatagramSocket();
	        	receiveSocket = new DatagramSocket(aport);	        
		} catch (SocketException se) {
			se.printStackTrace();
	        System.exit(1);
		}
		
		/*--- INITILIZE Elevator Lamp ---*/
		for(int i =0; i<MAX_FLOOR; i++) {
			Lamp[i] = 0;
		}
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
			System.out.println("ERROR current floor");
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
		
		while(true) {
			try {
				System.out.println("ELEVATOR: Waiting ..."); // so we know we're waiting
		        	receiveSocket.receive(receivePacket);
		        	System.out.print("ELEVATOR: received ");
		    	} catch (IOException e) {
		    		System.out.print("ELEVATOR: IO Exception: likely:");
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
					System.out.println("ELEVATOR:Elevator moved up, now at Floor " + elevator.getCurrentFloor());
					send = 1;	// send elevator location
					s_elevator = 0;		// elevator job drop off
					break;		// end UP_DROPOFF				
				case UP_PICKUP:	
					System.out.println("cmd, UP for pick up");
					System.out.println("ELEVATOR: wait for elevator data ");
					s_elevator = 1;		// elevator job pick up
					send = 0;
					break;		// end UP_PICKUP
				case DOWN_DROPOFF:
					System.out.println("cmd, DOWN for drop off");
					elevator.direction = ElevatorDirection.E_DOWN;	//move down
					elevator.run();
					System.out.println("ELEVATOR:Elevator move DOWN, now at Floor " + elevator.getCurrentFloor());
					sendPacket = createPacket(DATA, elevator.getCurrentFloor(),receivePacket.getPort());
					send = 1;	// send elevator location
					s_elevator = 0;		// elevator job drop off
					break;		// end DOWN_DROPOFF
				case DOWN_PICKUP:
					System.out.println("cmd, UP for drop off");
					System.out.println("ELEVATOR: wait for elevator data ");
					s_elevator = 1;		// elevator job pick up
					send = 0;
					break;		// end DOWN_PICKUP
				case DOOR_OPEN:	
					System.out.println("cmd, OPEN door");
					elevator.open();
					send = 0;
					if (s_elevator != -1) {
						if (s_elevator == 1) {
							/*--- door open for pick up, elevator lamp ON ---*/
							Lamp[num_lamp-1] = s_elevator;
							System.out.println("ELEVATOR: Elevator Lamp ON at " + num_lamp);
						}else if (s_elevator == 0) {
							/*--- door open for drop off, elevator lamp OFF ---*/
							Lamp[elevator.getIntFloor()-1] = s_elevator;
							System.out.println("ELEVATOR: Elevator Lamp OFF at " + num_lamp);
						}else {
							System.out.println("ELEVATOR: ERROR elevator status");
						}
						s_elevator = -1;		// elevator job open door
					}// end if not -1
					break;		// end DOOR_OPEN				
				case DOOR_CLOSE:
					System.out.println("cmd, CLOSE door");
					elevator.close();
					send = 0;
					s_elevator = -1;		// elevator job open close
					break;		//end DOOR_CLOSE
				case STOP:
					System.out.println("cmd, STOP");
					elevator.direction = ElevatorDirection.E_HOLD;
					elevator.run();
					System.out.println("ELEVATOR:Elevator STOPPED at " + elevator.getCurrentFloor());
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
					System.out.println("ELEVATOR:Elevator moved UP, now at Floor " + elevator.getCurrentFloor());
					sendPacket = createPacket(DATA, elevator.getCurrentFloor(),receivePacket.getPort()); 		// send elevator location
					s_elevator = 1;					
					break;		// end UP_PICKUP
				case DOWN_PICKUP:
					elevator.direction = ElevatorDirection.E_DOWN;		// move down
					elevator.run();
					System.out.println("ELEVATOR:Elevator moved DOWN, now at Floor " + elevator.getCurrentFloor());
					sendPacket = createPacket(DATA, elevator.getCurrentFloor(),receivePacket.getPort()); 		
					s_elevator = 1;
					break;		// end DOWN_PICKUP
				case UP_DROPOFF:
					elevator.direction = ElevatorDirection.E_UP;	//move up
					elevator.run();
					System.out.println("ELEVATOR:Elevator moved UP, now at Floor " + elevator.getCurrentFloor());
					sendPacket = createPacket(DATA, elevator.getCurrentFloor(),receivePacket.getPort());
					s_elevator = 0;
					break;		// end UP_DROPOFF
				case DOWN_DROPOFF:
					elevator.direction = ElevatorDirection.E_DOWN;	//move down
					elevator.run();
					System.out.println("ELEVATOR:Elevator moved DOWN, now at Floor " + elevator.getCurrentFloor());
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
					System.out.println("ELEVATOR:Elevator moved UP, now at Floor " + elevator.getCurrentFloor());
					s_elevator = 1;		// elevator job pick up
					num_lamp = toInt(data[1]); 	// record elevator lamp
					break;
				case DOWN_PICKUP:
					elevator.direction = ElevatorDirection.E_DOWN;	//move down
					elevator.run();
					sendPacket = createPacket(DATA, elevator.getCurrentFloor(),receivePacket.getPort());
					System.out.println("ELEVATOR:Elevator moved DOWN, now at Floor " + elevator.getCurrentFloor());
					s_elevator = 1;		// elevator job pick up
					num_lamp = toInt(data[1]); 	// record elevator lamp
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
				try {
					sendSocket.send(sendPacket);
				} catch (IOException e1) {
					e1.printStackTrace();
					System.exit(1);
				}
			}//end header switch
		}//end while (true)
	}// end control()
	
	/**
	 *  main function of the ElevatorControl class 
	 */
	public static void main(String[] args) {
		ElevatorControl cElv = new ElevatorControl();
		cElv.control();

	}// end main	
}// end class


















