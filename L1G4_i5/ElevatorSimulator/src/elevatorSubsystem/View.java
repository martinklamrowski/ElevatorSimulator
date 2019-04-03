package elevatorSubsystem;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

public class View extends JFrame{
	
	private JPanel panel;
	private JTextField[][] elevatorfloors = new JTextField[22][4];
	private JTextField[][] elevatorDirections = new JTextField[2][4];
//	private JTextField[] lamps = new JTextField[4];
		
	

	public View() {
		panel = new JPanel();
		panel.setLayout(new GridLayout(28,4,20,0));
				
		JLabel E1 = new JLabel("Elevator 1");
		panel.add(E1);
		
		JLabel E2 = new JLabel("Elevator 2");
		panel.add(E2);
		
		JLabel E3 = new JLabel("Elevator 3");
		panel.add(E3);
		
		JLabel E4 = new JLabel("Elevator 4");
		panel.add(E4);
		
		
		for (int i = 0; i<22; i++) {
			for(int j=0; j<4; j++) {
				elevatorfloors[i][j] = new JTextField();
				elevatorfloors[i][j].setBackground(Color.BLUE);
				panel.add(elevatorfloors[i][j]);
				elevatorfloors[i][j].setEnabled(false);
			}
			
		}
		for (int i = 0; i<2; i++) {
			for(int j=0; j<4; j++) {
				if(i%2==0) {
					elevatorDirections[i][j] = new JTextField("^");
				}
				else {
					elevatorDirections[i][j] = new JTextField("v");
				}
				panel.add(elevatorDirections[i][j]);
				elevatorDirections[i][j].setEnabled(false);
			}
			
		}
//		for(int i = 0; i<4; i++) {
//			lamps[i] = new JTextField("Lamp");
//			panel.add(lamps[i]);
//			lamps[i].setEnabled(false);
//		}
		this.setVisible(true);
		panel.setVisible(true);
		panel.setBackground(Color.GRAY);
		this.add(panel);
		this.setTitle("Elevator Simulator");
		this.setBackground(Color.BLACK);
		this.setSize(400, 750);
		this.setResizable(true);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
	}
	
	public JTextField getElevatorfloors(int i, int j) {
		return elevatorfloors[i][j];
	}

	public void setElevatorfloors(JTextField[][] elevatorfloors) { 
		this.elevatorfloors = elevatorfloors;
	}
	
	public JTextField getElevatorDirections(int i, int j) {
		return elevatorDirections[i][j];
	}

	public void setElevatorDirections(JTextField[][] elevatorDirections) {
		this.elevatorDirections = elevatorDirections;
	}

//	public JTextField getLamps(int i) {
//		return lamps[i];
//	}
//
//	public void setLamps(JTextField[] lamps) {
//		this.lamps = lamps;
//	}

}
