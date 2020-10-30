package uk.ac.ed.inf.aqmaps;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.List;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.Point;

public class App 
{
	private static Drone drone;
	private static List<Sensor> sensors;
	private static List<Obstacle> buildings;
	
	private static String day;
	private static String month;
	private static String year;
	private static Point2D startCoordinates;
	@SuppressWarnings("unused")
	private static String seed;
	private static String port;
	
    public static void main(String[] args) throws IOException, InterruptedException {
        day = args[0];
        month = args[1];
        year = args[2];
        startCoordinates = new Point2D.Double(Double.parseDouble(args[3]), Double.parseDouble(args[4]));
        seed = args[5];
        port = args[6];
        
        sensors = IO.readSensors(day, month, year, port);
        buildings = IO.readBuildings(port);
        drone = new Drone(startCoordinates, buildings);

        System.out.println("Calculating order to visit sensors...");
        List<Sensor> flightPlan = PathFinder.nearestNeighbor(startCoordinates, sensors);
        flightPlan = PathFinder.tsp(startCoordinates, flightPlan);
        
        System.out.println("The drone is executing the flight plan!");
        while (flightPlan.isEmpty() != true) {
        	// getToSensor will return false if the drone ran out of moves or tried to go out of bounds.
        	Boolean canStillMove = drone.getToSensor(flightPlan.get(0));
        	
        	if (canStillMove == false) {
        		// print appropriate message
        		if (drone.getMovesLeft() < 1) {
            		System.out.println("The drone did not complete the plan as it ran out of moves...");
        		} else {
        			System.out.println("The drone tried to exit boundaries, terminating.");
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
        
        drone.getToPoint(startCoordinates);
        System.out.println("The drone is finished!");
        
        drone.buildReadings();
        
        System.out.println("Creating files...");
        IO.writeReadings(drone.getReadings(), day, month, year);
        IO.writeFlightPath(drone.getFlightPath(), day, month, year);
        System.out.println("Done!");
    }
}
