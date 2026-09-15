/*
* File: GrandFinale.java
* Created: 
* Author: Sacha Loi

Preamble:

I used a similar alternative to Route B where instead I created a new stack called junctionStackLeave which tracked at any junction 
where the robot should leave the junction for the optimal route. I implemented this by adding the direction the robot leaves a
junction by pushing to the stack, I did this at many different instances. The first instance was when the robot was in explorer 
mode and encounters a new junction, where the direction chosen is pushed to the stack. I also pushed to the stack in backtrackmode
when the robot backtracks to a junction that hasn't explored all passages, therefore the new direction picked is pushed to the stack.
I also encountered an error where if the robot started with two potential routes to explore it would pick a route at random leading
to errors as I require this decision to act like a crossroad or junction, therfore I added a special case for this where in this
specific scenario this first decision (which tracks as a corridor) will act like a junction. Due to this extra redundancy in the
program my robot can start anywhere in the prim maze and find the optimal solution.

I popped from the junctionStackLeave in backtrack mode at a junction no matter the scenario, this is because if the robot
is required to backtrack to a junction it means that the junction leads to a deadend, making the path it left most recently
redundant. By this logic the stack only stores the direction at each junction so that the robot doesn't reach any deadends, 
in other word the optimum route.

My grand finale isn't able to find the optimum route of a loopy maze but is able to run in loopy mazes and doesn't encounter any
runtime errors when the robot is searching for the target. With the information of the first run the junctionStackLeave helps 
find a more optimal route than the first random search, but this solution is not fully optimal and would require much further 
development to implement this feature.

At the start of the second run all the contents of the stack junctionStackLeave from the first run is copied onto an arrraylist, 
and because popping items from a stack reverses the input order I used a library to reverse the array list. Therefore to make sure
my program used the optimal solution for repeated runs after the second run I iterated over this arraylist so each time the robot
reaches a junction it will take the optimal exit. Originally I attempted to pop the items into another stack but this didn't
allow for sucuessive runs after the second run as the contents of the stack would be emptied, due to popping all items off it. 

For the first exploratory run I used my logic from exercise 2 because it didn't store unecesary data and didn't break in loopy 
mazes threfore was clearly the better option to build from.

*/


// Import packages for robot and dynamic list
import uk.ac.warwick.dcs.maze.logic.IRobot;
import java.util.List;
import java.util.ArrayList;
// Adding a stack so that I don't have to store junction data
import java.util.Stack;
// Allows to reverse arraylist
import java.util.Collections;


public class GrandFinale {
    // Stores robots state (Exploring or Backtracking)
    private int explorerMode = 1; // 1 = explore, 0 = backtrack
    // Create a stack to store visited junctions
    Stack<Integer> junctionStackEnter = new Stack<Integer>();
    // Stack to store where robot leaves junctions
    Stack<Integer> junctionStackLeave = new Stack<Integer>();
    // ArrayList for solved maze
    List<Integer> solvedArray = new ArrayList<Integer>();
    // When navigating the solved maze
    private int junctionCounter = 0;
    // Moves in a run
    private int moves;
    // Start x co-ordinate
    int startX;
    //Start y co-ordinate
    int startY;

    // When reset button pressed reset moves and the junctionCounter
    public void reset() {
        moves = 0;
        junctionCounter = 0;
    }

    // Create controlRobot method to interact with the maze environment
    public void controlRobot(IRobot robot) { 
        // On the first run explore the maze
        if ((robot.getRuns() == 0)) {
            // Call the exploreControl method or backtrackControl method depending on explorerMode
            if (explorerMode == 1){
                exploreControl(robot);
            }
            else {
                backtrackControl(robot);
            }
            // Calculates the start position in case it starts at a 'corner junction'
            if (moves == 0) {
                startX = robot.getLocation().x;
                startY = robot.getLocation().y;
            }
        } else {
            // After the first run use optimal path
            solvedMaze(robot);
        }
        // Increment moves every pass
        moves++;
    }

    // Method for when robot  exploring
    private void exploreControl(IRobot robot) {
        
        // Determine position based on num of nonwallExits
        int exits = nonwallExits(robot);
        // Default direction initialisation 
        int direction = IRobot.CENTRE;

        // Enable backtrack if at deadend or choose direction otherwise
        if (exits == 1) {
            direction = deadEnd(robot);
            if (moves > 0) explorerMode = 0; // Set mode to backtrack only after first move
        } else if (exits == 2 && moves != 0) {  // Makes sure it works when start with two passages to exit
            direction = corridor(robot);
        } else {
            direction = junction(robot);
            // Push to stack where it enters the junction
            junctionStackEnter.push(robot.getHeading());
            // Push to stack direction leaving junction
            int absHeading = relativeToAbsolute(robot, direction);
            junctionStackLeave.push(absHeading);
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

        // Choose branch depending on number of non-wall exits
        if (exits == 1) {
            direction = deadEnd(robot);
        } else if (exits == 2) {
            // If back to start position, treat as a junction if it has two exits
            if (robot.getLocation().x == startX && robot.getLocation().y == startY) {
                // If at a junction or crossroads pop from stack as had to backtrack to the junction
                int passageExits = passageExits(robot);
                junctionStackLeave.pop();
                direction = junction(robot);
                // Push to stack when picking a new direction
                int absHeading = relativeToAbsolute(robot, direction);
                junctionStackLeave.push(absHeading);
                explorerMode = 1;  
            }
            direction = corridor(robot);
        } else {
            // If at a junction or crossroads pop from stack as had to backtrack to the junction
            junctionStackLeave.pop();
            int passageExits = passageExits(robot);
            // If there are any available passages choose one and switch to explorer mode
            if (passageExits != 0) {
                direction = junction(robot);
                // Push to stack new direction leaving the junction in
                int absHeading = relativeToAbsolute(robot, direction);
                junctionStackLeave.push(absHeading);
                explorerMode = 1;  
            } else {
                // If there are no available passages pop the original heading off the stack
                int originalHeading = junctionStackEnter.pop();
                // Find opposite direction to entered junction 
                int oppositeDirection = IRobot.NORTH + ((originalHeading - IRobot.NORTH) + 2) % 4;
                // Convert relative direction robot entered into relative direction
                direction = absoluteToRelative(robot, oppositeDirection);
            }
        }
        robot.face(direction);
    }

    // Optimal route second time around
    private void solvedMaze(IRobot robot) {

        // Copy contents over from stack into an array at first move of second run
        if ((robot.getRuns() == 1 && moves == 0)) {
            while (!junctionStackLeave.isEmpty()) {
                int absHeading = junctionStackLeave.pop();
                solvedArray.add(absHeading);
            }
            // Reverse array as popping reverses order of stack
            Collections.reverse(solvedArray);
        }

        int direction = IRobot.CENTRE;
        int exits = nonwallExits(robot);
        // Failsafe if reaches a deadend
        if (exits == 1) {
            direction = deadEnd(robot);
        } else if (exits == 2 && moves != 0) {
            direction = corridor(robot);
        } else {
            // If at a junction access direction in the array for the optimal route
            int relativeDirection = solvedArray.get(junctionCounter);
            direction = absoluteToRelative(robot, relativeDirection);
            // Increment junction counter for next junction
            junctionCounter ++;
        }
        robot.face(direction);
    }

    // Method to convert relative direction to absolute direction
    private int relativeToAbsolute(IRobot robot, int heading) {

        // Get difference between current heading and ahead
        int currentHeading = robot.getHeading();       
        int difference = heading - IRobot.AHEAD;
        // Validation to make sure direction stays in range
        if (difference < 0) difference += 4;
        // Return absolute direction
        return ((currentHeading + difference) % 4) + IRobot.NORTH;
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
        int correctDirection = IRobot.CENTRE;
        //Loop through all the directions
        if (moves == 0) {
            for (int i = 0; i < 4; i++) {
                int direction = IRobot.AHEAD + i;
                if (robot.look(direction) !=  IRobot.WALL) correctDirection = direction;
            }
            explorerMode = 1;
            return correctDirection;
        }
        //Defualt return state
        return IRobot.BEHIND;
    }

    // Method to choose a direction at corridor (no for loop as don't want to check behind)
    private int corridor(IRobot robot) {
        //Loop through all the directions
        if (robot.look(IRobot.AHEAD) !=  IRobot.WALL) return IRobot.AHEAD;
        if (robot.look(IRobot.RIGHT) !=  IRobot.WALL) return IRobot.RIGHT;
        if (robot.look(IRobot.LEFT) !=  IRobot.WALL) return IRobot.LEFT;
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