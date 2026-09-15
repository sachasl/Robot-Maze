/*
* File: Ex1.java
* Created: 
* Author: Sacha Loi

Preamble:

When I created the methods passageExits, nonwallExits, deadEnd and junction, I used a for loop to loop through each possible 
direction that the robot could move in therfore reducing the need for repeated code when using if statements for example.
But when implementing the corridor method I required a different order of checks therefore I implemented if statements which 
allowed me to specify which directions to check (as I didn't check behind as I want to keep going forward) and their order.

In the robotData class I recorded junctions in an array called junctions, this implementation works but isn't memory efficient 
as an arrayList would dynamically store the junctions rather than 10,000 indexes no matter the scenario. The junctions array stored 
items of data type JunctionRecorder where I created a class that allows a single junction to have attributes that represented the 
junctions x and y coordinates as well as the initial direction it entered the junction at. Other methods in the RobotData class 
include the printJunction method to keep track of junctions helping with debugging, as well as searchJunction which simply 
retrieved the heading intially entered at a junction using the implemntation of the JunctionRecorder class.

I have used explorerMode and backtrackControl to allow for normal explorer behaviour initially, but when the robot is at a
deadend it enables bactrack mode. If it backtracks to a junction or crossroads with passage exits it will switch back to 
explorer mode and continue exploring, otherwise it will leave the junction the way it entered it in. This implementation works
but results in repeated code where I call the same (or similar) methods in backtrackControl and explorerMode to face the robot 
accordingly. Therfore it could've been implemented in a more efficient approach.

When I created the junction and crossroad methods I realised they used the same logic therfore to reduce repeated code I decided
to only keep one of the two methods (the junctions method), for simpler and more readable code.

In the worst case analysis the robot will explore each square that is not a wall a ceratin number of times depending on it's state.
It will explore each corrdor square twice, once while exploring and another when backtracking, it will explore each deadend exactly
once as once it gets to a deadend it won't backtrack to the same explored square, it will explore each crossroad three times as it 
will have to backtrack to the crossroad three times and similarly it will explore each junction four times as it will have to 
backtrack to the junction four times. Therefore it will take the largest number of steps with a maze full of junctions (also known
as a blank maze) as it will have to backtrack to the junctions repeatedly. On an average maze the sum of these junctions, 
crossroads deadends and corridors will average out to roughly double the number of explorable squares.

*/


// Import packages for robot and dynamic list
import uk.ac.warwick.dcs.maze.logic.IRobot;
import java.util.List;
import java.util.ArrayList;


public class Ex1 {
    // Incremented after each pass
    private int pollRun = 0;
    // Data store for junctions
    private RobotData robotData;
    // Stores robots state (Exploring or Backtracking)
    private int explorerMode = 1; // 1 = explore, 0 = backtrack

    // Create controlRobot method to interact with the maze environment
    public void controlRobot(IRobot robot) {
        
        // On the first move of the first run of a new maze
        if ((robot.getRuns() == 0) && (pollRun == 0)) {
            robotData = new RobotData(); //reset the data store
        }

        // Increment pollRun so that the data is not reset each time the robot moves
        pollRun++; 

        // Call the exploreControl method or backtrackControl method depending on explorerMode
        if (explorerMode == 1){
            exploreControl(robot);
        }
        else {
            backtrackControl(robot);
        }
    }

    // Method for when robot  exploring
    private void exploreControl(IRobot robot) {

        // Determine position based on num of nonwallExits
        int exits = nonwallExits(robot);
        // Default direction initialisation 
        int direction = IRobot.CENTRE;

        // Enable backtrack if at deadend or choose direction otherwise
        if (exits < 2) {
            direction = deadEnd(robot);
            explorerMode = 0; // Set mode to backtrack
        } else if (exits == 2){
            direction = corridor(robot);
        } else {
            direction = junction(robot);
        }

        // Face the robot in the intended direction
        robot.face(direction);
    }

    // Method for when robot backtracking
    private void backtrackControl(IRobot robot) {
        
        // Determine position based on num of nonwallExits
        int exits = nonwallExits(robot);
        // Default direction initialisation 
        int direction = IRobot.CENTRE;
        // Find how many passage exits and store in a variable
        int pExits = passageExits(robot);

        if (exits == 1) {
            direction = deadEnd(robot);
        } else if (exits == 2){
            direction = corridor(robot);
        } else {
            // If there is passage exits switch to explorerMode
            if (pExits > 0) {
                direction = junction(robot);
                explorerMode = 1;
            // If no passage exits
            } else {
                // Get the absolute direction the robot entered
                int directionEntered = robotData.searchJunction(robot.getLocation().x, robot.getLocation().y);
                // Find opposite direction to entered junction 
                int oppositeDirection = IRobot.NORTH + ((directionEntered - IRobot.NORTH) + 2) % 4;
                // Convert relative direction robot entered into relative direction
                direction = absoluteToRelative(robot, oppositeDirection);
            }
        }

        robot.face(direction);
    }

    private int absoluteToRelative(IRobot robot, int absHeading) {

		// Retirieve the current absolute heading of the robot
		int currentHeading = robot.getHeading();
		// Find the difference between the robot current direction and its desired direction
		int difference = absHeading - currentHeading;
		// Validation to make sure relative direction stays in range
		if (difference < 0) difference += 4;
		// Convert the absolute heading to a realtive direction
		int relativeDirection = IRobot.AHEAD + difference;
		// Face the robot in this direction
		return relativeDirection;
    }

    // Method to check number of non wall exits
    private int nonwallExits(IRobot robot) {
        int exits = 4;
        // Lopp through all directions
        for (int i = 0; i < 4; i++) {
            int direction = IRobot.AHEAD + i;
            if (robot.look(direction) ==  IRobot.WALL) exits --;
        }
        return exits;
    }

    // Method to check number of passages
    private int passageExits(IRobot robot) {
        int pExits = 0;
        // Loop through all directions
        for (int i = 0; i < 4; i++) {
            int direction = IRobot.AHEAD + i;
            if (robot.look(direction) ==  IRobot.PASSAGE) pExits ++;
        }
        return pExits;        
    }

    // Method to choose a direction at deadend
    private int deadEnd(IRobot robot) {
        // Loop through all the directions
        for (int i = 0; i < 4; i++) {
            int direction = IRobot.AHEAD + i;
            if (robot.look(direction) !=  IRobot.WALL) return direction;
        }
        // Defualt return state
        return IRobot.CENTRE;
    }

    // Method to choose a direction at corridor (no for loop as don't want to check behind)
    private int corridor(IRobot robot) {
        if (robot.look(IRobot.AHEAD) !=  IRobot.WALL) return IRobot.AHEAD;
        if (robot.look(IRobot.LEFT) !=  IRobot.WALL) return IRobot.LEFT;
        if (robot.look(IRobot.RIGHT) !=  IRobot.WALL) return IRobot.RIGHT;
        // Defualt return state (to prevent syntax error)
        return IRobot.CENTRE;
    }

    // Method to choose a direction at junction and crossroads (as they exhibit the same behaviour)
    private int junction(IRobot robot) {

        // Record the junction data
        robotData.recordJunction(robot);
        // Create a list which stores possible directions for the robot
        List<Integer> headings = new ArrayList<Integer>();

        // Checks if there are any unexplored routes the robot hasn't taken
        for (int i = 0; i < 4; i++) {
            int direction = IRobot.AHEAD + i;
            if (robot.look(direction) ==  IRobot.PASSAGE) headings.add(direction);
        }

        // If there are no unexplored routes append all nonwallExits
        if (headings.isEmpty()) {
            for (int i = 0; i < 4; i++) {
                int direction = IRobot.AHEAD + i;
                if (robot.look(direction) !=  IRobot.WALL) headings.add(direction);
            }
        }

        // Randomly choose between the valid directions
        int choice = (int) (Math.random() * headings.size());
        return headings.get(choice);
    }

    // Allows interaction with the RobotData class
    public void reset() {
        robotData.resetJunctionCounter();
    }
}

// Class to store robot data at junctions
class RobotData {

    private static int maxJunctions = 10000; // Max number of junctions likely to occur
    private static int junctionCounter; // No. of junctions stored
    private JunctionRecorder[] junctions = new JunctionRecorder[maxJunctions]; // Array to store all junctions data

    // Method to record junction data
    public void recordJunction(IRobot robot) {
        int juncX = robot.getLocation().x;
        int juncY = robot.getLocation().y;

        // If encountering a new junction exit method
        if (searchJunction(juncX, juncY) != -1) {
            return;
        }
        
        // Get direction robot arrived junction at
        int arrived = robot.getHeading();
        // Creates an instance of JunctionRecorder(specific junctions details) and stores it in the junctions array
        junctions[junctionCounter] = new JunctionRecorder(juncX, juncY, arrived);
        junctionCounter ++;
        // Print junction data to console (using the printJunction method)
        printJunction(junctionCounter, juncX, juncY, arrived);
    }

    // Method to print junction data to console
    public void printJunction(int junctionCounter, int juncX, int juncY, int arrived) {
        
        // Convert hard coded integer to compass directions
        String direction;
        switch (arrived) {
            case IRobot.NORTH: 
                direction = "NORTH";
                break;
            case IRobot.EAST: 
                direction = "EAST";
                break;
            case IRobot.SOUTH: 
                direction = "SOUTH";
                break;
            case IRobot.WEST: 
                direction = "WEST";
                break;
            default:
                direction = "UNKNOWN";
        }
        // Print the junction data to console
        System.out.println("Junction " + junctionCounter + " (x=" + juncX + ", y=" + juncY + ") heading " + direction);
    }

    // Method to find a heading for specific x and y coordinates
    public int searchJunction(int juncX, int juncY) {

        // For loop to loop through junction instances in the array
        for(int i=0; i<junctionCounter; i++) {
            int currentX = junctions[i].getX();
            int currentY = junctions[i].getY();
            // If coordiantes match required junction retrieve the heading entered the junction
            if (currentX == juncX && currentY == juncY) {
                return junctions[i].getArrived();
            }
        }
        // If encounters a new junction
        return -1;
    }

    // Allows interaction with the Explorer class
    public void resetJunctionCounter() {
        junctionCounter = 0;
    }

}

// Class to store junction data for a specific junction (instance)
class JunctionRecorder {

    private int juncX; // X-coordinates of the junctions
    private int juncY; // Y-coordinates of the junctions
    private int arrived; // Heading the robot first arrived from

    // Constructor method to initialise the attributes
    public JunctionRecorder(int juncX, int juncY, int arrived) {
        this.juncX = juncX;
        this.juncY = juncY;
        this.arrived = arrived;
    }
    
    // Get methods for the attributes
    public int getX() { return juncX; }
    public int getY() { return juncY; }
    public int getArrived() { return arrived; }
}
