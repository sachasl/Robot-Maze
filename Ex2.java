/*
* File: Ex2.java
* Created: 
* Author: Sacha Loi

Preamble: 
I implemented a stack so when the robot visits a new junction it adds the direction they entered the junction into the stack. 
When the robot returns to a junction which has no passage exits the stack is popped providing the heading the robot entered 
the current junction in therfore the robot can return back in the opposite direction it entered in.

This implementation works as a stack is a LIFO data structure therfore it enables to backtrack to the most recent junction 
that has been visited, and it also saves space compared to exercise 1 as it doesn't require the x and y coordinates of
each junction therefore reducing the memory usage to a single stack of junction entrance directions which can be dynamically
operated on. So the stack will only be as large as the number of junctions that directly lead to the path when the target is found.

*/


// Import packages for robot and dynamic list
import uk.ac.warwick.dcs.maze.logic.IRobot;
import java.util.List;
import java.util.ArrayList;
// Adding a stack so that I don't have to store junction data
import java.util.Stack;


public class Ex2 {
    // Stores robots state (Exploring or Backtracking)
    private int explorerMode = 1; // 1 = explore, 0 = backtrack
    // Create a stack to store visited junctions
    Stack<Integer> junctionStack = new Stack<Integer>();

    // Create controlRobot method to interact with the maze environment
    public void controlRobot(IRobot robot) { 

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
        } else if (exits == 2) {
            direction = corridor(robot);
        } else {
            // If at a junction or a crossroad pick a direction and add the heading entered into a stack
            direction = junction(robot);
            junctionStack.push(robot.getHeading());
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

        if (exits < 2) {
            direction = deadEnd(robot);
        } else if (exits == 2) {
            direction = corridor(robot);
        } else {
            // If at a junction or crossroads
            int passageExits = passageExits(robot);
            // If there are any available passage choose one a switch to explorer mode
            if (passageExits != 0) {
                direction = junction(robot);
                explorerMode = 1;
            } else {
                // If there are no available passages pop the original heading off the stack
                int originalHeading = junctionStack.pop();
                // Find opposite direction to entered junction 
                int oppositeDirection = IRobot.NORTH + ((originalHeading - IRobot.NORTH) + 2) % 4;
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
}
