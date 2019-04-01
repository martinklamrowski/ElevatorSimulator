~~~
Authors: Martin Klamrowski, Angie Byun, Tashfiq Akhand, Arianna Shi, Sunjay Panesar
Last modified: 01/04/2019
~~~

********** ITERATION 5 - L1 Group 4 **********
__________________________
TEAM MEMBERS CONTRIBUTIONS:

i5:
===
~ Arianna Shi - 
~ Sunjay Panesar - 
~ Tashfiq Akhand - GUI
~ Martin Klamrowski - 
~ Angie Byun - 

i4:
===
~ Arianna Shi - timing diagram
~ Sunjay Panesar - code for Scheduler, code for Elevator Subsystem
~ Tashfiq Akhand - planning for GUI
~ Martin Klamrowski - 
~ Angie Byun - 

i3:
===
~ Arianna Shi - 
~ Sunjay Panesar - 
~ Tashfiq Akhand - 
~ Martin Klamrowski - code for Scheduler, code for Elevator Subsystem
~ Angie Byun - code for Elevator Subsystem, timing diagram

i2:
===
~ Arianna Shi - code for Elevator Subsystem
~ Sunjay Panesar - code for Floor Subsystem, code for Elevator Subsystem, code for Scheduler
~ Tashfiq Akhand - Timing Diagram, JUnit Testing
~ Martin Klamrowski - code for Scheduler 
~ Angie Byun - code for Scheduler, Class Diagram

i1:
===
~ Arianna Shi - code for Elevator Subsystem
~ Sunjay Panesar - code for Floor Subsystem
~ Tashfiq Akhand - JUnit testing
~ Martin Klamrowski - code for Scheduler 
~ Angie Byun - class diagram, state machine diagram, code for Scheduler
__________________________________________
THE ELEVATOR SIMULATOR PROJECT CONSISTS OF: 

	- 3 subsystems/directories located in L1G4_i3/ElevatorSimulator/src/ (ElevatorSystem, FloorSystem, Scheduler) 
	- there are a total of 8 .java source files. 
	- timing diagram for error scenario (PDF) located in /ElevatorSimulator/
	- timing diagram for measurement (PDF) located in /ElevatorSimulator/
	- text file containing elapsed and initial time values for requests, per scheduler thread
	- updated class diagram (PDF) located in /ElevatorSimulator/
	- instructions for how to open and run files. 
_____
SETUP:
(FOR ECLIPSE)
	
	- extract the L1G4_i5.zip file
	- in Eclipse navigate to File>Open Projects from File System
	- click 'Directory'
	- find the L1G4_i5/ElevatorSimulator/ directory and click 'Finish'
	- the main files for the system are ElevatorSubsystem.java, Scheduler.java, FloorSubsystem.java
_______
RUNNING: 
note: the FloorSubsystem provides the requests immediately on so ensure the Scheduler and ElevatorSubsystem are running before 

	- in Eclipse, run the Scheduler.java and ElevatorSubsystem.java files, and then the FloorSubsystem.java file
	- the input requests provided by the FloorSubsystem are: 9-4, 5-1, 3-18, 1-7, 1-21, 7-1, 1-4, 18-1
	- you can open 3 consoles to show the output of all 3 systems:
		> the ElevatorSubsystem prints what floor the elevator is at, lamp information and door opening and closing actions
		> the FloorSubsystem shows what floor lamps have been lit and the outgoing requests sent to the Scheduler with time stamps
		> the Scheduler shows data transfer between all the systems
_______
TESTING:
	
	- JUnit testing included for each subsystem
	- to run all tests, run AllTests.java in Eclipse

____________________________________
ELEVATOR TIMING DATA AND ASSUMPTIONS:
(data obtained from Herzberg)
~ approx. 6s per floor
~ approx. 2s for door opening and closing
~ assuming 50% of requests are initiated from the ground floor

__________________
ELEVATOR ALGORITHM:

	- there are four elevators with 22 floors
	- each elevator will position itself at a different floor when idle in order to reduce wait time
	- two elevators are positioned at ground floor when idle, one elevator at floor 20, and one elevator at floor 10 
	- when the request for an elevator floor is requested, suitability is calculated for each elevator,
	  the elevator with the highest calculated suitability will be the elevator assigned to pick the person up/ drop the person off
	- the calculated suitability depends on the current state of the elevator, which was sectioned into 4 parts
		> the elevator car is moving towards a destination and the request is in the same direction
		> the elevator car is moving towards a destination and the request is in the opposite direction
		> the elevator car is already moving away from the requested destination
		> the elevator is idle

______________
TIMING DIAGRAM:

The timing diagram taken into the event of the elevator moving towards the designated floor to where the user resides. 
The lines of the diagram that are horizonal represents the remaining in the state described in the second column and the vertical line 
represents the shifting of states, the line is associated to a timeline presented below and the diagram shows all 3 system 
synchronization. The error scenario labelled on the horizontal lines is where the possibility of an error can occur 
which would stop the timing diagram and not continue furthur. If the error did not occur, then timing diagram would resume.

____________
ERROR STATES:

The error state shown in the timing diagram was implemented in iteration 3. The error was injected through the Elevator Subsystem.
In this scenario, there are two error scenarios:

	- Error 1: there is a faulty sensor in shaft 3 (elevator 3) at floor 5
	- Error 2: elevator 4 gets stuck between floor 15 and 14, i.e. stuck between floors

In both scenarios the Scheduler will detect the error and take the corresponding elevator out of service and will not give it anymore 
service requests. The errors are injected through the Elevator Subsystem; lines 202 and 315 of ElevatorControl.java. 

____________
MEASUREMENTS:

Code for measuring time has been implemented for iteration 3. Elapsed time values are stored in three HashMap structures. Each structure 
contains a Long with elapsed time as the key, and the initial time of the request as its corresponding value. Measured requests are as 
follows:
	
	-elevatorStoppedTimes: state machine has sent STOP to an elevator
	-elevatorArrivalTimes: an elevator's arrival sensor has been triggered
	-floorRequestTimes: the floor subsystem has given the scheduler a service request
	
The acquired measurements were taken under the conditions of the previous iteration. Error scenarios 1 & 2, as described above, were in 
effect. As a result, elevators 3 & 4 encounter faults, and elevators 1 & 2 pick up a majority of the work.

The file Scheduler Timing Elapsed and Start.txt contains elapsed and start time values contained in the three previously mentioned 
HashMaps on program termination.





