package uk.ac.ed.inf.aqmaps;

import java.awt.geom.Point2D;
import java.util.List;

//make this class final to emulate a static class
public final class App {	
	// do not allow this class to be instantiated
	private App() {}
	
	// Execute the program
    public static void main(String[] args) {
        run(args);
    }
    
    public static void run(String[] args) {
    	var day = args[0];
    	// make sure the day argument has starts with 0 if it is single digit as that is how it is stored on the web server
    	if (day.length() == 1) {
    		day = "0" + day;
    	}
    	// make sure the month argument has starts with 0 if it is single digit as that is how it is stored on the web server
        var month = args[1];
    	if (month.length() == 1) {
    		month = "0" + day;
    	}
        var year = args[2];
        var startCoordinates = new Point2D.Double(Double.parseDouble(args[3]), Double.parseDouble(args[4]));
        
        // if the start position is out of bounds throw an illegal argument exception
        if (PathFinder.isOutofBounds(startCoordinates, 0) != 0) {
        	throw new IllegalArgumentException("Given start position is out of bounds");
        }
        // seed is not used in this implementation
    	@SuppressWarnings("unused")
        var seed = args[5];
        var port = args[6];
        
        // get the sensors to be visited for the given date
        List<Sensor> sensors = IO.readSensors(day, month, year, port);
        // get the building that should be avoided
        List<Obstacle> buildings = IO.readBuildings(port);
        // set the no-fly-zones of PathFinder as the buildings
        PathFinder.setNoFlyZones(buildings);
        // instantiate a Drone object to control
        Drone drone = new Drone(startCoordinates);
        
        System.out.println("Calculating order to visit sensors...");
        // use nearestNeighbor from PathFinder search first to get a basic order of sensors to visit
        List<Sensor> flightPlan = PathFinder.nearestNeighbor(startCoordinates, sensors);
        // give the nearestNeighbor solution to twoOpt and use the output as the route
        flightPlan = PathFinder.twoOpt(startCoordinates, flightPlan);
        
        // initialise this boolean which represents whether the drone has run out of moves
        Boolean canStillMove = true;
        
        System.out.println("The drone is executing the flight plan!");
        // repeat until every sensor in the route has been visited
        while (flightPlan.isEmpty() != true) {
        	// getToSensor will return false if the drone ran out of moves or tried to go out of bounds.
        	canStillMove = drone.getToSensor(flightPlan.get(0));
        	// if the drone is out of moves, add the remaining sensors
        	if (canStillMove == false) {
        		// print appropriate message
            	System.out.println("The drone did not complete the plan as it ran out of moves...");
        		// fill out the remaining sensors as they were not visited
        		while (flightPlan.isEmpty() != true) {
        			var sensorFeature = PathFinder.makeSensorFeature(flightPlan.get(0).getCoordinates().getX(),
        					flightPlan.get(0).getCoordinates().getY(),
        					flightPlan.get(0).getLocation(),
        					"#aaaaaa",
        					"");
        			drone.getFeatureList().add(sensorFeature);
        			flightPlan.remove(0);
        		}
        	} else {
        		// remove the just visited sensor from the flightPlan
        		flightPlan.remove(0);
        	}
        }
        // if the drone still has moves after visiting every sensor
        if (canStillMove == true) {
        	// tell the drone to go back to the starting coordinates
            canStillMove = drone.getToPoint(startCoordinates);
            // if the drone ran out moves on the way back print a message indicating so
            if (canStillMove == false) {
            	System.out.println("The drone visited each sensor but ran out of moves on it's way back home...");
            } else {
                System.out.println("The drone is finished!");
            }
        }
                
        System.out.println("Creating files...");
        // create the readings file
        IO.writeReadings(drone.buildReadings(), day, month, year);
        // create the flight path file
        IO.writeFlightPath(drone.getFlightPath(), day, month, year);
        
		System.out.println("The drone took " + (150 - drone.getMovesLeft()) + "moves to complete the route.");
        System.out.println("Done!");
    }
    
    ///////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////
    
    // This method is used for testing. Identical to run(), except it doesn't write files and does return the number
    // of moves the drone took
    public static int runTest(String[] args) {
    	var day = args[0];
    	if (day.length() == 1) {
    		day = "0" + day;
    	}
        var month = args[1];
    	if (month.length() == 1) {
    		month = "0" + month;
    	}
        var year = args[2];
        var startCoordinates = new Point2D.Double(Double.parseDouble(args[3]), Double.parseDouble(args[4]));
    	@SuppressWarnings("unused")
        var seed = args[5];
        var port = args[6];
                
        List<Sensor> sensors = IO.readSensors(day, month, year, port);
        List<Obstacle> buildings = IO.readBuildings(port);
        PathFinder.setNoFlyZones(buildings);
        Drone drone = new Drone(startCoordinates);

        List<Sensor> flightPlan = PathFinder.nearestNeighbor(startCoordinates, sensors);
        flightPlan = PathFinder.twoOpt(startCoordinates, flightPlan);
        
        Boolean canStillMove = true;
        
        while (flightPlan.isEmpty() != true) {
        	// getToSensor will return false if the drone ran out of moves or tried to go out of bounds.
        	canStillMove = drone.getToSensor(flightPlan.get(0));
        	
        	if (canStillMove == false) {
        		// print appropriate message
            	System.out.println("The drone did not complete the plan as it ran out of moves...");
        		return 150;
        	} else {
        		// remove the just visited sensor from the flightPlan
        		flightPlan.remove(0);
        	}
        }
        
        if (canStillMove == true) {
        	// tell the drone to go back to the starting coordinates
            canStillMove = drone.getToPoint(startCoordinates);
            if (canStillMove == false) {
            	System.out.println("The drone visited each sensor but ran out of moves on it's way back home...");
            }
        }
        
        var moves = 150 - drone.getMovesLeft();
		//System.out.println("Number of moves = " + moves);
        return moves;
    }
}
