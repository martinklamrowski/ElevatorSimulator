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
public class ElevatorControl extends Thread{
	/* ## HEADER AND COMMAND IDENTIFIERS ## */
	private static final String ACK = "1";
	private static final String CMD = "2";
	private static final String DATA = "3";	

	private static final String UP_MOVE = "0x35";
	private static final String DOWN_MOVE = "0x34";
	private static final String UP_PICKUP = "0x33";		// going up to pick up
	private static final String UP_DROPOFF = "0x32"; 	// going
	private static final String DOWN_PICKUP = "0x31";
	private static final String DOWN_DROPOFF = "0x30";
	private static final String DOOR_OPEN = "0x3A";
	private static final String DOOR_CLOSE = "0x3B";
	private static final String STOP = "0x3C";
	private static final String GO_IDLE = "0x3D";
	public static ElevatorControl Elv_1, Elv_2, Elv_3, Elv_4;
	
	
	/*	variable identifies	 */
	private int num_elevator = 0; 
	private int num_lamp = 0;
	private static int send = 0;
	private static int s_elevator = 0;		//elevator status	1: pickup, lamp ON		0:drop off, lamp OFF
	private DatagramSocket eSocket;
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
			eSocket = new DatagramSocket(port);
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
		operate();
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
			System.out.println("ELEVATOR " + num_elevator + "ERROR current floor");
		}	
		return a;
	}
	
//	When CMD is received to move
	public void operate() {
		boolean keepMoving = false;
		boolean relocating = false;
		boolean droppedOff = false;
		ElevatorDirection targetDir = ElevatorDirection.E_HOLD;
		byte _data[] = new byte[100];
		receivePacket = new DatagramPacket(_data, _data.length);
		String[] ins;
		String[] data;
		String dir = "";
		String action = "";
		System.out.println("ELEVATOR " + num_elevator + ": Elevator Waiting ..."); // so we know we're waiting
		while (true) {
//			Move this to control()
			elevReceive(receivePacket);
			ins  = packetToString(receivePacket.getData());
			System.out.printf("cmd, %s \n", ins[1]);
			switch (ins[1]) {
			case UP_MOVE: 
				targetDir = ElevatorDirection.E_UP;
				dir = "UP";
				relocating = true;
				keepMoving = true;
				sendAck(receivePacket, ins);
				break;
			case DOWN_MOVE: 
				targetDir = ElevatorDirection.E_DOWN;
				dir = "DOWN";
				relocating = true;
				keepMoving = true;
				sendAck(receivePacket, ins);
				break;
			case UP_DROPOFF: 
				targetDir = ElevatorDirection.E_UP;
				dir = "UP";
				action = "dropped off";
				keepMoving = true;
				droppedOff = true;
//				Something to turn current floor lamp off
				elevator.Lamp[elevator.getIntFloor() - 1] = 0;
				System.out.println("ELEVATOR " + num_elevator + ": Elevator Lamp OFF at " + elevator.getIntFloor());
				sendAck(receivePacket, ins);
				break;
			case DOWN_DROPOFF: 
				targetDir = ElevatorDirection.E_DOWN;
				dir = "DOWN";
				action = "dropped off";
				keepMoving = true;
				droppedOff = true;
				//		Something to turn current floor lamp off
				elevator.Lamp[elevator.getIntFloor() - 1] = 0;
				System.out.println("ELEVATOR " + num_elevator + ": Elevator Lamp OFF at " + elevator.getIntFloor());
				sendAck(receivePacket, ins);
				break;
			case GO_IDLE: 
				targetDir = ElevatorDirection.E_HOLD;
				dir = "DOWN";
				action = "dropped off";
				droppedOff = true;
				//		Something to turn current floor lamp off
				elevator.Lamp[elevator.getIntFloor() - 1] = 0;
				System.out.println("ELEVATOR " + num_elevator + ": Elevator Lamp OFF and stopping at " + elevator.getIntFloor());
				sendAck(receivePacket, ins);
				break;
			case UP_PICKUP: 
				targetDir = ElevatorDirection.E_UP;
				dir = "UP";
				action = "picked up";
				keepMoving = true;
				sendAck(receivePacket, ins);
				elevReceive(receivePacket);
				System.out.printf("cmd, passenger %s at %s, moving %s \n", action, elevator.getCurrentFloor(), dir);
				data = packetToString(receivePacket.getData());
//				System.out.printf("cmd, %s \n", data[1]);
			//	Something to turn target floor lamp on using data
				elevator.Lamp[elevator.getIntFloor() - 1] = 0;
				num_lamp = Integer.parseInt(data[1]);
				elevator.Lamp[num_lamp - 1] = 1;
				System.out.println("ELEVATOR " + num_elevator + ": Elevator Lamp ON at " + num_lamp);
				sendAck(receivePacket, data);
				break;
			case DOWN_PICKUP: 
				targetDir = ElevatorDirection.E_DOWN;
				dir = "UP";
				action = "picked up";
				keepMoving = true;
				sendAck(receivePacket, ins);
				elevReceive(receivePacket);
				System.out.printf("cmd, passenger %s at %s, moving %s \n", action, elevator.getCurrentFloor(), dir);
				data = packetToString(receivePacket.getData());
				elevator.Lamp[elevator.getIntFloor() - 1] = 0;
			//	Something to turn target floor lamp on using data
				num_lamp = Integer.parseInt(data[1]);
				elevator.Lamp[num_lamp - 1] = 1;
				System.out.println("ELEVATOR " + num_elevator + ": Elevator Lamp ON at " + num_lamp);
				sendAck(receivePacket, data);
				break;
			case STOP: 
				stopOpenClose(receivePacket);
				break;
			}

			if (relocating) System.out.printf("cmd, moving %s to pickup passenger\n", dir);
			else if (droppedOff) System.out.printf("cmd, passenger %s at %s, moving %s \n", action, elevator.getCurrentFloor(), dir);
			
			while (keepMoving) {
				elevator.direction = targetDir;	//move up
				elevator.run();
				sendPacket = createPacket(DATA, elevator.getCurrentFloor(), receivePacket.getPort());
				System.out.println("ELEVATOR " + num_elevator + ": Elevator moved " + dir + ", now at Floor " + elevator.getCurrentFloor());
				send = 1;	// send elevator location				
				/*--- send elevator location ---*/
				if (send == 1) {
					try {
						System.out.println(String.format("ELEVATOR %d: sending pos ( string >> %s, byte array >> %s ).", 
								num_elevator, new String(sendPacket.getData()), sendPacket.getData()));
						eSocket.send(sendPacket);
					} catch (IOException e1) {
						e1.printStackTrace();
						System.exit(1);
					}
					send = 0;
				}// end if (location update)
				elevReceive(receivePacket);
				data = packetToString(receivePacket.getData());
				if (data[1].equals(STOP)) {
					System.out.println("STOP, stopping elevator");
					keepMoving = false;
					stopOpenClose(receivePacket);
				}
				else {
					System.out.println("CONTINUE, elevator moving");
					System.out.println(String.format("ELEVATOR %d: received cmd ( string >> %s, byte array >> %s ).", 
							num_elevator, new String(receivePacket.getData()), receivePacket.getData()));
//					System.out.printf("previous command received is: %s \n" , data[1]);
//					sendAck(receivePacket, data);
				}
			}
		}
	}
	
	public void elevStop(DatagramPacket receive) {
		String[] ins = packetToString(receive.getData());
		System.out.println("cmd, STOP");
		elevator.direction = ElevatorDirection.E_HOLD;
		elevator.run();
		System.out.println("ELEVATOR " + num_elevator + ": Elevator STOPPED at " + elevator.getCurrentFloor());
		sendAck(receive, ins);
	}
	
	public void elevOpen(DatagramPacket receive) {
		String[] ins = packetToString(receive.getData());
		ins = packetToString(receive.getData());
		System.out.println("cmd, OPEN door");
		if (ins[1].equals(DOOR_OPEN)) elevator.open(num_elevator);
		else System.out.println("Unable to open door");
		
		sendAck(receive, ins);
	}
	
	public void elevClose(DatagramPacket receive) {
		String[] ins = packetToString(receive.getData());
		ins = packetToString(receive.getData());
		System.out.println("cmd, CLOSE door");
//		System.out.println("CLOSE door command is: " + ins[1]);
		if (ins[1].equals(DOOR_CLOSE)) elevator.close(num_elevator);
		else System.out.println("Unable to close door");
		sendAck(receive, ins);
	}
	
	// Use when CMD is received to stop
	public void stopOpenClose(DatagramPacket receive) {
//		String[] ins = packetToString(receive.getData());
//		if (ins[1] == STOP)
		elevStop(receive);
		elevReceive(receive);
		elevOpen(receive);
		elevReceive(receive);
		elevClose(receive);
	}
	
	public void elevReceive(DatagramPacket receive) {
		try {
//			System.out.println("ELEVATOR " + num_elevator + ": Elevator Waiting ..."); // so we know we're waiting
        	eSocket.receive(receive);
        	System.out.print("ELEVATOR " + num_elevator + ": received ");
	    } catch (IOException e) {
    		System.out.print("ELEVATOR " + num_elevator + ": IO Exception: likely:");
        	System.out.println("Receive Socket Timed Out.\n" + e);
        	e.printStackTrace();
        	System.exit(1);
	    }
	}

	public void sendAck(DatagramPacket receive, String[] ins) {
		sendAPacket = createPacket(ACK, ins[1], receive.getPort());
		try {
//        	System.out.println("ELEVATOR " + num_elevator + ": sending ack ");
			System.out.println(String.format("ELEVATOR %d: sending ack ( string >> %s, byte array >> %s ).", 
					num_elevator, new String(receive.getData()), receive.getData()));
			eSocket.send(sendAPacket);
		} catch (IOException e1) {
			e1.printStackTrace();
		    System.exit(1);
		}
	}
	
		
//	/**
//	 * function called in main to control corresponding elevator do the received request
//	 */
//	public void control() {
//		byte _data[] = new byte[100];
//		receivePacket = new DatagramPacket(_data, _data.length);
//		String[] ins = packetToString(receivePacket.getData());
//		String[] cmd = null;
//		String[] data = null;	
//		
//		while(true) {
//			try {
//					System.out.println("ELEVATOR " + num_elevator + ": Elevator Waiting ..."); // so we know we're waiting
//		        	eSocket.receive(receivePacket);
//		        	System.out.print("ELEVATOR " + num_elevator + ": received ");
//		    	} catch (IOException e) {
//		    		System.out.print("ELEVATOR " + num_elevator + ": IO Exception: likely:");
//		        	System.out.println("Receive Socket Timed Out.\n" + e);
//		        	e.printStackTrace();
//		        	System.exit(1);
//		    	}
//			
//			ins = packetToString(receivePacket.getData());
//			
//			switch (ins[0]) {
//			case CMD:
//				/*====== CMD packet received ======*/
//				cmd = ins;
//				
//				switch (cmd[1]) {
//				/*--- cases for different CMD code ---**/
//				case UP_DROPOFF:
//					System.out.println("cmd, UP for drop off");
//					elevator.direction = ElevatorDirection.E_UP;	//move up
//					elevator.run();
//					sendPacket = createPacket(DATA, elevator.getCurrentFloor(),receivePacket.getPort());
//					System.out.println("ELEVATOR " + num_elevator + ": Elevator moved up, now at Floor " + elevator.getCurrentFloor());
//					send = 1;	// send elevator location
//					s_elevator = 0;		// elevator job drop off
//					break;		// end UP_DROPOFF				
//
//				case UP_PICKUP:	
//					System.out.println("cmd, UP for pick up");
//					System.out.println("ELEVATOR " + num_elevator + ": wait for elevator data ");
//					s_elevator = 1;		// elevator job pick up
//					send = 0;
//					break;		// end UP_PICKUP
//
//				case DOWN_DROPOFF:
//					System.out.println("cmd, DOWN for drop off");
//					elevator.direction = ElevatorDirection.E_DOWN;	//move down
//					elevator.run();
//					System.out.println("ELEVATOR " + num_elevator + ": Elevator move DOWN, now at Floor " + elevator.getCurrentFloor());
//					sendPacket = createPacket(DATA, elevator.getCurrentFloor(),receivePacket.getPort());
//					send = 1;	// send elevator location
//					s_elevator = 0;		// elevator job drop off
//					break;		// end DOWN_DROPOFF
//
//				case DOWN_PICKUP:
//					System.out.println("cmd, UP for drop off");
//					System.out.println("ELEVATOR " + num_elevator + ": wait for elevator data ");
//					s_elevator = 1;		// elevator job pick up
//					send = 0;
//					break;		// end DOWN_PICKUP
//
//				case DOOR_OPEN:	
//					System.out.println("cmd, OPEN door");
//					elevator.open(num_elevator);
//					send = 0;
//					if (s_elevator != -1) {
//						if (s_elevator == 1) {
//							/*--- door open for pick up, elevator lamp ON ---*/
//							elevator.Lamp[num_lamp-1] = s_elevator;
//							System.out.println("ELEVATOR " + num_elevator + ": Elevator Lamp ON at " + num_lamp);
//						}else if (s_elevator == 0) {
//							/*--- door open for drop off, elevator lamp OFF ---*/
//							elevator.Lamp[elevator.getIntFloor()-1] = s_elevator;
//							System.out.println("ELEVATOR " + num_elevator + ": Elevator Lamp OFF at " + num_lamp);
//						}else {
//							System.out.println("ELEVATOR " + num_elevator + ": ERROR elevator status");
//						}
//						s_elevator = -1;		// elevator job open door
//					}// end if not -1
//					break;		// end DOOR_OPEN				
//
//				case DOOR_CLOSE:
//					System.out.println("cmd, CLOSE door");
//					elevator.close(num_elevator);
//					send = 0;
//					s_elevator = -1;		// elevator job open close
//					break;		//end DOOR_CLOSE
//
//				case STOP:
//					System.out.println("cmd, STOP");
//					elevator.direction = ElevatorDirection.E_HOLD;
//					elevator.run();
//					System.out.println("ELEVATOR " + num_elevator + ":Elevator STOPPED at " + elevator.getCurrentFloor());
//					send = 0;
//					break;		// end STOP
//
//				}// end CMD switch
//				
//				/*--- send ACK for CMD ---**/
//				sendAPacket= createPacket(ACK, ins[1], receivePacket.getPort());
//				try {
//					eSocket.send(sendAPacket);
//				} catch (IOException e1) {
//					e1.printStackTrace();
//				    System.exit(1);
//				}
//				
//				/*--- send elevator location ---*/
//				if (send == 1) {
//					try {
//						eSocket.send(sendPacket);
//					} catch (IOException e1) {
//						e1.printStackTrace();
//						System.exit(1);
//					}
//					send = 0;
//				}// end if (location update)
//				break;		// end CMD			
//
//			case ACK:
//				System.out.println("ack");
//				/*----- ACK packet received -----*/
//				switch (cmd[1]) {
//				case UP_PICKUP:	
//					elevator.direction = ElevatorDirection.E_UP;		// move up
//					elevator.run();
//					System.out.println("ELEVATOR " + num_elevator + ":Elevator moved UP, now at Floor " + elevator.getCurrentFloor());
//					sendPacket = createPacket(DATA, elevator.getCurrentFloor(),receivePacket.getPort()); 		// send elevator location
//					s_elevator = 1;				
//					break;		// end UP_PICKUP
//
//				case DOWN_PICKUP:
//					elevator.direction = ElevatorDirection.E_DOWN;		// move down
//					elevator.run();
//					System.out.println("ELEVATOR " + num_elevator + ":Elevator moved DOWN, now at Floor " + elevator.getCurrentFloor());
//					sendPacket = createPacket(DATA, elevator.getCurrentFloor(),receivePacket.getPort()); 
//					s_elevator = 1;
//					break;		// end DOWN_PICKUP
//
//				case UP_DROPOFF:
//					elevator.direction = ElevatorDirection.E_UP;	//move up
//					elevator.run();
//					System.out.println("ELEVATOR " + num_elevator + ":Elevator moved UP, now at Floor " + elevator.getCurrentFloor());
//					sendPacket = createPacket(DATA, elevator.getCurrentFloor(),receivePacket.getPort());
//					s_elevator = 0;
//					break;		// end UP_DROPOFF
//
//				case DOWN_DROPOFF:
//					elevator.direction = ElevatorDirection.E_DOWN;	//move down
//					elevator.run();
//					System.out.println("ELEVATOR " + num_elevator + ":Elevator moved DOWN, now at Floor " + elevator.getCurrentFloor());
//					sendPacket = createPacket(DATA, elevator.getCurrentFloor(),receivePacket.getPort());
//					s_elevator = 0;		// elevator job drop off
//					break;		// end DOWN_DROPOFF
//				}// end CMD switch
//				
//				/*--- send elevator location message ---*/
//				try {
//					eSocket.send(sendPacket);
//				} catch (IOException e1) {
//					e1.printStackTrace();
//					System.exit(1);
//				}
//				break;		// end ACK
//			
//			case DATA:
//				System.out.println("data");
//				/*----- DATA packet received -----*/
//				data = ins;
//				switch (cmd[1]) {
//				case UP_PICKUP:
//					elevator.direction = ElevatorDirection.E_UP;	//move up
//					elevator.run();
//					sendPacket = createPacket(DATA, elevator.getCurrentFloor(),receivePacket.getPort());
//					System.out.println("ELEVATOR " + num_elevator + ":Elevator moved UP, now at Floor " + elevator.getCurrentFloor());
//					s_elevator = 1;		// elevator job pick up
//					num_lamp = toInt(data[1]); 	// record elevator lamp
//					send = 1;
//					break;
//
//				case DOWN_PICKUP:
//					elevator.direction = ElevatorDirection.E_DOWN;	//move down
//					elevator.run();
//					sendPacket = createPacket(DATA, elevator.getCurrentFloor(),receivePacket.getPort());
//					System.out.println("ELEVATOR " + num_elevator + ":Elevator moved DOWN, now at Floor " + elevator.getCurrentFloor());
//					s_elevator = 1;		// elevator job pick up
//					num_lamp = toInt(data[1]); 	// record elevator lamp
//					send = 1;
//					break;
//
//				case STOP:
//					s_elevator = 1;		// elevator job pick up
//					num_lamp = toInt(data[1]); 	// record elevator lamp
//					send = 0;
//					break;				}// end CMD switch
//					
//				/*--- send ACK message ---*/
//				sendAPacket = createPacket(ACK, data[1], receivePacket.getPort());
//				try {
//					eSocket.send(sendAPacket);
//				} catch (IOException e1) {
//					e1.printStackTrace();
//				    System.exit(1);
//				}
//				/*--- send elevator location ---*/
//				if (send == 1) {
//					try {
//						eSocket.send(sendPacket);
//					} catch (IOException e1) {
//						e1.printStackTrace();
//						System.exit(1);
//					}
//				}// end update location
//			}//end header switch
//		}//end while (true)
//	}// end control()
	
	/**
	 *  main function of the ElevatorControl class 
	 */
	public static void main(String[] args) {
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
