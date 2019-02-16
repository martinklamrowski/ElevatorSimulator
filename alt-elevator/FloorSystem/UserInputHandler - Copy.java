//package FloorSystem;
//
//import java.io.BufferedReader;
//import java.io.FileNotFoundException;
//import java.io.FileReader;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//public class UserInputHandler extends FloorSubsystem implements Runnable {
//
//	ArrayList<String> serviceReqList = new ArrayList<String>();
//	ArrayList<byte[]> serviceReqs = new ArrayList<byte[]>();
//	
//	public UserInputHandler() {
//		
//	}
//	
//	/**
//	 * Take a flat file and parse lines of text for requests in the following form: 
//	 * "XX:XX:XX.XXX X DIRECTION X"
//	 * @param filePath
//	 * 
//	 */
//	public void parseInputFile(String filePath, ArrayList<String> list) throws FileNotFoundException, IOException {
//		
//		BufferedReader br = new BufferedReader(new FileReader(filePath));
//		String line = null;
//		while ((line = br.readLine()) != null) {
//			Pattern pattern = Pattern.compile("\\d{2}:\\d{2}:\\d{2}.\\d*\\s\\d{1,2}\\s[A-z]{2,4}\\s\\d{1,2}");
//			Matcher matcher = pattern.matcher(line);
//			if (matcher.find()) {
//				list.add(new String(matcher.group()));
//				System.out.println("UserInputHandler: Got a new service request! Adding to queue... " + list.get(list.size() - 1));
//			}
//		}
//		br.close();
//	}
//	
//	public void parseServiceReqs(ArrayList<String> list) {
//		int hour = 0;
//		int min = 0;
//		int sec = 0;
//		int mSec = 0;
//		int timeTotal = 0;
//		int timeTotalPrev = 0;
//		int timeDiff = 0;
//		int startFloor = 0;
//		int destFloor = 0;
//		Direction targetDirection = Direction.IDLE;
//
//		for (String s : list) {
//			String data[] = s.split(" ");
//			String time[] = data[0].split("[:.]");
//			hour = Integer.parseInt(time[0]);
//			min = Integer.parseInt(time[1]);
//			sec = Integer.parseInt(time[2]);
//			mSec = Integer.parseInt(time[3]);
//	        timeTotal = hour*60*60*1000 +
//	                    min*60*1000 +
//	                    sec*1000 +
//	                    mSec;
//	        if (timeTotalPrev == 0) timeDiff = 0;
//	        else timeDiff = timeTotal - timeTotalPrev;
//	        timeTotalPrev = timeTotal;
//			startFloor = Integer.parseInt(data[1]);
//			if (data[2].toUpperCase().equals("UP")) targetDirection = Direction.UP;
//			else if (data[2].toUpperCase().equals("DOWN")) targetDirection = Direction.DOWN;
//			destFloor = Integer.parseInt(data[3]);
//			
////			Have the thread sleep until the next elevator request is supposed to come in
//			if (timeDiff != 0) {
//				try {
//					Thread.sleep(timeDiff);
//				} catch (InterruptedException e ) {
//					e.printStackTrace();
//					System.exit(1);
//				}
//			}
//
//			byte[] msg = new byte[100];
//
////			Iterate through the system's floors to find the start floor, then send the input request to the
////			scheduler and turn the floor's direction lamp on
//			for (Floor f : super.floors) {
//				if (f.getFloorNum() == startFloor) {
//					sendServiceRequest(startFloor, destFloor, targetDirection);
//					if (targetDirection == Direction.UP) f.setUpLampOn();
//					else if (targetDirection == Direction.DOWN) f.setDownLampOn();
//				}	
//			}
//		}
//	}
//
//	/**
//	 * Takes the input string formatted as "XX:XX:XX.XXX X DIRECTION X" and places int and Direction enum
//	 * parameters into data object. Time is in values data[0] to data[3], start floor is in data[4],
//	 * direction is in data[5], and destination floor is in data[6]
//	 * @param s
//	 * @return data
//	 */
//	
//	public Object[] getInputData(String s) {
//		int numParameters = 7;
//		Object[] data = new Object[numParameters];
//		String[] input = s.split(" ");
//		String[] time = input[0].split("[:.]");
//		data[0] = Integer.parseInt(time[0]);
//		data[1] = Integer.parseInt(time[1]);
//		data[2] = Integer.parseInt(time[2]);
//		data[3] = Integer.parseInt(time[3]);
//		data[4] = Integer.parseInt(input[1]);
//		if (input[2].toUpperCase().equals("UP")) data[5] = Direction.UP;
//		else if (input[2].toUpperCase().equals("DOWN")) data[5] = Direction.DOWN;
//		data[6] = Integer.parseInt(input[3]);
//		
//		return data;
//	}
//	
//	@Override
//	public void run() {
//
//		try {
//			parseInputFile("src/FloorSystem/input.txt", serviceReqList);
//		} catch (IOException e) {
//			e.printStackTrace();
//            System.exit(1);
//		}
//		
//		parseServiceReqs(serviceReqList);
//		
//	}
//	
//}
