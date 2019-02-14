~~~
Authors: Martin Klamrowski
Last modified: 12/2/2019
~~~

********** ITERATION 2 - L1 Group 4 **********
______________________
TEAM MEMBERS AND ROLES:
~ Arianna Shi - code for Elevator Subsystem
~ Sunjay Panesar - code for Floor Subsystem
~ Tashfiq Akhand - 
~ Martin Klamrowski - code for Scheduler 
~ Angie Byun - 
_________________________________________
THE ELEVATOR SIMULATOR PROJECT CONSISTS OF: 

	- 
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

	- 
_______
TESTING:
	
	- 
____________________________________
ELEVATOR TIMING DATA AND ASSUMPTIONS:
(data taken from Herzberg)
~ approx. 6s per floor
~ approx. 2s for door opening and closing
~ assuming 50% of requests are initiated from the ground floor

__________________
ELEVATOR ALGORITHM:

	- there are four elevators
	- each elevator will position itself at a different floor when idle in order to reduce wait time
		- two elevators are positioned at ground floor when idle, one elevator at floor 20, and one elevator at floor 10 

