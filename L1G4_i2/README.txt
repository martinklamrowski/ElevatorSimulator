~~~
Authors: Martin Klamrowski, Angie Byun
Last modified: 16/2/2019
~~~

********** ITERATION 2 - L1 Group 4 **********
______________________
TEAM MEMBERS AND ROLES:
~ Arianna Shi - code for Elevator Subsystem
~ Sunjay Panesar - code for Floor Subsystem
~ Tashfiq Akhand - Timing Diagram, JUnit Testing
~ Martin Klamrowski - code for Scheduler 
~ Angie Byun - code for Scheduler 
_________________________________________
THE ELEVATOR SIMULATOR PROJECT CONSISTS OF: 

	- 3 subsystems/directories located in /ElevatorSimulator/src/ (ElevatorSystem, FloorSystem, Scheduler) 
	- there are a total of 8 .java source files. 
	- Timing Diagram for Error Scenario (PDF) located in /ElevatorSimulator/
	- instructions for how to open and run files. 
_____
SETUP:
(FOR ECLIPSE)
	
	- extract the L1G4_i2.zip file into the Eclipse Workspace
	- in Eclipse navigate to File>Open Projects from File System
	- click 'Directory'
	- find the ElevatorSystem directory and click 'Finish'
	- the main files for the system are ElevatorControl.java, Scheduler.java, FloorSubsystem.java
_______
RUNNING: 
note: the FloorSubsystem provides the requests immediately on so ensure the Scheduler and ElevatorControl are running before 

	- in Eclipse, run the Scheduler.java and ElevatorControl.java files, and then the FloorSubsystem.java file
	- the input request sequence provided by the FloorSubsystem is: 1-6, 6-5, 4-8, 1-5, 7-1 ***************
	- you can open 3 consoles to show the output of all 3 systems:
		> the ElevatorControl prints what floor the elevator is at, lamp information and door opening and closing actions
		> the FloorSubsystem shows what floor lamps have been lit and the outgoing requests sent to the Scheduler with time stamps
		> the Scheduler shows elevator and floor information with time stamps
_______
TESTING:
	
	- JUnit testing included for each subsystem
	- to run all tests, run AllTests.java in Eclipse
	- ensure the JUnit is included in the project build path
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











