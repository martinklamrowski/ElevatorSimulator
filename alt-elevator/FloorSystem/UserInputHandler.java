package FloorSystem;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import FloorSystem.FloorSubsystem.Direction;

/**
 * This class is used to handle an input file filled with elevator requests.
 * The requests are placed into an array, and sent to the Scheduler based on the request timestamp
 * 
 * @author Sunjay Panesar
 */

public class UserInputHandler implements Runnable {

	DatagramSocket requestSocket;
	ArrayList<String> serviceReqList = new ArrayList<String>();
	ArrayList<byte[]> serviceReqs = new ArrayList<byte[]>();

	FloorSubsystem fs;
	
	public UserInputHandler(FloorSubsystem floorSubsystem) {
		fs = floorSubsystem;
        try {
        	requestSocket = new DatagramSocket();
        } catch (SocketException se) {
            se.printStackTrace();
            System.exit(1);
        }
	}
	
	/**
	 * Take a flat file and parse lines of text for requests in the following form: 
	 * "XX:XX:XX.XXX X DIRECTION X"
	 * @param filePath, list
	 * 
	 */
	public void parseInputFile(String filePath, ArrayList<String> list) throws FileNotFoundException, IOException {
		
//		Place file into buffered reader, then iterate through each line. If the line string 
//		matches the given pattern, place the string into a String ArrayList
		BufferedReader br = new BufferedReader(new FileReader(filePath));
		String line = null;
		while ((line = br.readLine()) != null) {
			Pattern pattern = Pattern.compile("\\d{2}:\\d{2}:\\d{2}.\\d*\\s\\d{1,2}\\s[A-z]{2,4}\\s\\d{1,2}");
			Matcher matcher = pattern.matcher(line);
			if (matcher.find()) {
				list.add(new String(matcher.group()));
				System.out.println("UserInputHandler: Got a new service request! Adding to queue... " + list.get(list.size() - 1));
			}
		}
		br.close();
	}
	
	/**
	 * Take a list of service request strings and separate each entry into variables, then
	 * send the requests, waiting the appropriate amount of time between each request
	 * 
	 * @param list
	 * 
	 */
	public void parseServiceReqs(ArrayList<String> list) {
		int hour = 0;
		int min = 0;
		int sec = 0;
		int mSec = 0;
		int timeTotal = 0;
		int timeTotalPrev = 0;
		int timeDiff = 0;
		int startFloor = 0;
		int destFloor = 0;
		Direction targetDirection = Direction.IDLE;

//		Parse the previously generated list and separate each line into variables
		for (String s : list) {
			String data[] = s.split(" ");
			String time[] = data[0].split("[:.]");
			hour = Integer.parseInt(time[0]);
			min = Integer.parseInt(time[1]);
			sec = Integer.parseInt(time[2]);
			mSec = Integer.parseInt(time[3]);
			
//			Convert the time variables into a single time value in milliseconds
	        timeTotal = hour*60*60*1000 +
	                    min*60*1000 +
	                    sec*1000 +
	                    mSec;
	        if (timeTotalPrev == 0) timeDiff = 0;
	        else timeDiff = timeTotal - timeTotalPrev;
	        timeTotalPrev = timeTotal;
			startFloor = Integer.parseInt(data[1]);
			if (data[2].toUpperCase().equals("UP")) targetDirection = Direction.UP;
			else if (data[2].toUpperCase().equals("DOWN")) targetDirection = Direction.DOWN;
			destFloor = Integer.parseInt(data[3]);
			
//			Have the thread sleep until the next elevator request is supposed to come in
			if (timeDiff != 0) {
				try {
					Thread.sleep(timeDiff);
				} catch (InterruptedException e ) {
					e.printStackTrace();
					System.exit(1);
				}
			}

			byte[] msg = new byte[100];

//			Iterate through the system's floors to find the start floor, then send the input request to the
//			scheduler and turn the floor's direction lamp on
			for (Floor f : fs.floors) {
				if (f.getFloorNum() == startFloor) {
					fs.sendServiceRequest(data[0], startFloor, destFloor, targetDirection, requestSocket);
					if (targetDirection == Direction.UP) f.setUpLampOn();
					else if (targetDirection == Direction.DOWN) f.setDownLampOn();
				}	
			}
		}
	}

	
	/**
	 * Takes the input string formatted as "XX:XX:XX.XXX X DIRECTION X" and places int and Direction enum
	 * parameters into data object. Time is in values data[0] to data[3], start floor is in data[4],
	 * direction is in data[5], and destination floor is in data[6]
	 * @param s
	 * @return data
	 */
	public void run() {

		try {
			parseInputFile("src/FloorSystem/input.txt", serviceReqList);
		} catch (IOException e) {
			e.printStackTrace();
            System.exit(1);
		}
		
		parseServiceReqs(serviceReqList);
		
	}
	
}