import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

public class View extends JFrame{
	
	private JPanel panel;
	private JTextField[][] elevatorfloors = new JTextField[22][4];
	private JTextField text;
	
	

	public View() {
		panel = new JPanel();
		panel.setLayout(new GridLayout(26,4,20,0));
				
		//text = new JTextField();
		//text.setBackground(Color.CYAN);
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
		//panel.add(text);
		this.setVisible(true);
		panel.setVisible(true);
		panel.setBackground(Color.GRAY);
		this.add(panel);
		this.setTitle("Elevator Simulator");
		this.setBackground(Color.BLACK);
		this.setSize(400, 800);
		this.setResizable(true);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
	}
	
	public JTextField[][] getElevatorfloors() {
		return elevatorfloors;
	}

	public void setElevatorfloors(JTextField[][] elevatorfloors) { 
		this.elevatorfloors = elevatorfloors;
	}

	
	public JTextField getText() {
		return text;
	}

	public void setText(JTextField text) {
		this.text = text;
	}
	
	public static void main(String args[]) {
		View view = new View();
	}


}
