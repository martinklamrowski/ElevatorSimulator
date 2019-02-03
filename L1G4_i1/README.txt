~~~
Authors: Angie Byun, Martin Klamrowski
Last modified: 2/2/2019
~~~

********** ITERATION 1 - L1 Group 4 **********
______________________
TEAM MEMBERS AND ROLES:
~ Arianna Shi - code for Elevator Subsystem
~ Sunjay Panesar - code for Floor Subsystem
~ Tashfiq Akhand - JUnit testing
~ Martin Klamrowski - code for Scheduler 
~ Angie Byun - class diagram, state machine diagram, code for Scheduler
_________________________________________
THE ELEVATORSIMULATOR PROJECT CONSISTS OF: 

	- 3 subsystems/directories located in /ElevatorSimulator/src/ (ElevatorSystem, FloorSystem, Scheduler) 
	- there are a total of 8 .java source files. 
	- class Diagram and a State Machine Diagram (PDF) located in /ElevatorSimulator/
	- instructions for how to open and run files. 
_____
SETUP:
(FOR ECLIPSE)
	
	- extract the L1G4_i1.zip file into the Eclipse Workspace
	- in Eclipse navigate to File>Open Projects from File System
	- click 'Directory'
	- find the ElevatorSystem directory and click 'Finish'
	- the main files for the system are ElevatorControl.java, Host.java, FloorSubsystem.java
_______
RUNNING: 
note: the FloorSubsystem provides the requests immediately on so ensure the Host and ElevatorControl are running before 

	- in Eclipse, run the Host.java and ElevatorControl.java files, and then the FloorSubsystem.java file
	- the input request sequence provided by the FloorSubsystem is: 1-6, 6-5, 4-8, 1-5, 7-1
	- you can open 3 consoles to show the output of all 3 systems; the ElevatorControl prints what floor the elevator is at, lamp information and door opening and closing actions
	- the FloorSubsystem shows what floor lamps have been lit and the outgoing requests sent to the Host/Scheduler
_______________
ELEVATOR TIMING:
(data taken from Herzberg)
~ approx. 6s per floor
~ approx. 2s for door opening and closing 
