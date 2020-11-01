package uk.ac.ed.inf.aqmaps;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.List;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.Point;

public class App 
{	
	public static int moves;
	// Execute the program
    public static void main(String[] args) throws IOException, InterruptedException {
        run(args);
    }
    
    public static void run(String[] args) throws IOException, InterruptedException {
    	var day = args[0];
    	if (day.length() == 1) {
    		day = "0" + day;
    	}
        var month = args[1];
    	if (month.length() == 1) {
    		month = "0" + day;
    	}
        var year = args[2];
        var startCoordinates = new Point2D.Double(Double.parseDouble(args[3]), Double.parseDouble(args[4]));
    	@SuppressWarnings("unused")
        var seed = args[5];
        var port = args[6];
        
        List<Sensor> sensors = IO.readSensors(day, month, year, port);
        List<Obstacle> buildings = IO.readBuildings(port);
        Drone drone = new Drone(startCoordinates, buildings);

        System.out.println("Calculating order to visit sensors...");
        List<Sensor> flightPlan = PathFinder.nearestNeighbor(startCoordinates, sensors);
        flightPlan = PathFinder.twoOpt(startCoordinates, flightPlan);
        
        Boolean canStillMove = true;
        
        System.out.println("The drone is executing the flight plan!");
        while (flightPlan.isEmpty() != true) {
        	// getToSensor will return false if the drone ran out of moves or tried to go out of bounds.
        	canStillMove = drone.getToSensor(flightPlan.get(0));
        	
        	if (canStillMove == false) {
        		// print appropriate message
        		if (drone.getMovesLeft() < 1) {
            		System.out.println("The drone did not complete the plan as it ran out of moves...");
        		} else {
        			System.out.println("The drone tried to exit boundaries, sending it back to the starting coordinates.");
        		}
        		// fill out the remaining sensors as did not visit
        		while (flightPlan.isEmpty() != true) {
        			var sensor = Point.fromLngLat(flightPlan.get(0).getCoordinates().getY(), flightPlan.get(0).getCoordinates().getX());
        			var fSensor = Feature.fromGeometry((Geometry)sensor);
        			fSensor.addStringProperty("location", flightPlan.get(0).getLocation());
        			fSensor.addStringProperty("rgb-string", "#aaaaaa");
        			fSensor.addStringProperty("marker-color", "#aaaaaa");
        			fSensor.addStringProperty("marker-symbol", "");
        			drone.getFeatureList().add(fSensor);
        			flightPlan.remove(0);
        		}
        	} else {
        		// remove the just visited sensor from the flightPlan
        		flightPlan.remove(0);
        	}
        }
        
        if (canStillMove == true || drone.getMovesLeft() > 0) {
        	// tell the drone to go back to the starting coordinates
            canStillMove = drone.getToPoint(startCoordinates);
            
            if (canStillMove == false) {
        		// print appropriate message
        		if (drone.getMovesLeft() < 1) {
            		System.out.println("The drone visited each sensor but ran out of moves on it's way back home...");
        		} else {
        			System.out.println("The drone tried to exit boundaries when trying to get home, terminating.");
        		}
            } else {
                System.out.println("The drone is finished!");
            }
        }
        
        drone.buildReadings();
        
        System.out.println("Creating files...");
        IO.writeReadings(drone.getReadings(), day, month, year);
        IO.writeFlightPath(drone.getFlightPath(), day, month, year);
        
        moves = 150 - drone.getMovesLeft();
		System.out.println("Number of moves = " + (150 - drone.getMovesLeft()));
        System.out.println("Done!");
    }
    
    ///////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////
    
    public static int runTest(String[] args) throws IOException, InterruptedException {
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
        Drone drone = new Drone(startCoordinates, buildings);

        List<Sensor> flightPlan = PathFinder.nearestNeighbor(startCoordinates, sensors);
        flightPlan = PathFinder.twoOpt(startCoordinates, flightPlan);
        
        Boolean canStillMove = true;
        
        while (flightPlan.isEmpty() != true) {
        	// getToSensor will return false if the drone ran out of moves or tried to go out of bounds.
        	canStillMove = drone.getToSensor(flightPlan.get(0));
        	
        	if (canStillMove == false) {
        		// print appropriate message
        		if (drone.getMovesLeft() < 1) {
            		System.out.println("The drone did not complete the plan as it ran out of moves...");
        		} else {
        			System.out.println("The drone tried to exit boundaries, terminating.");
        		}
        		return 150;
        	} else {
        		// remove the just visited sensor from the flightPlan
        		flightPlan.remove(0);
        	}
        }
        
        // tell the drone to go back to the starting coordinates
        canStillMove = drone.getToPoint(startCoordinates);
        if (canStillMove == false) {
    		// print appropriate message
    		if (drone.getMovesLeft() < 1) {
        		System.out.println("The drone visited each sensor but ran out of moves on it's way back home...");
    		} else {
    			System.out.println("The drone tried to exit boundaries when trying to get home, terminating.");
    		}
        }
        
        moves = 150 - drone.getMovesLeft();
		//System.out.println("Number of moves = " + (150 - drone.getMovesLeft()));
        return moves;
    }
}
