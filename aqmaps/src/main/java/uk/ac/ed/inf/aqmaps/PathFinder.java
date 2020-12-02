package uk.ac.ed.inf.aqmaps;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;

// make this class final to emulate a static class
public final class PathFinder {
	// the coordinates of the limits are stored as arrays, with the first value being the longitude and the second being the latitude
	// North West corner
	private static final double[] NW = {-3.192473, 55.946233};
	// North East corner
	private static final double[] NE = {-3.184319, 55.946233};
	// South East corner
	private static final double[] SE = {-3.184319, 55.942617};
	// South West corner
	private static final double[] SW = {-3.192473, 55.942617};
	
	private static List<Obstacle> noFlyZones;
	
	// do not allow this class to be instantiated
	private PathFinder() {}
	
	// use the 2-opt algorithm to determine which order the best route to visit the sensors are given a start (and end) position
	// returns a list of Sensors
	public static List<Sensor> twoOpt(Point2D startCoordinates, List<Sensor> s) {
		var iLimit = s.size() - 2;
		var kLimit = s.size() - 1;
		
		// declare variables that are going to be used
		List<Sensor> newRoute;
		double newRouteLength;
		Boolean improvementMade;
		// calculate the route length of the given route
		var bestRouteLength = calcRouteLength(startCoordinates, s);
		
		// repeat until no improvements are made
		while (true) {
			improvementMade = false;
			
			for(var i = 1; i < iLimit; i++) {
				for (var k = i+1; k < kLimit; k++) {
					if ((s.get(i).getCoordinates().distance(s.get(k+1).getCoordinates()) + s.get(i-1).getCoordinates().distance(s.get(k).getCoordinates())) <= 
						(s.get(i).getCoordinates().distance(s.get(i-1).getCoordinates()) + s.get(k+1).getCoordinates().distance(s.get(k).getCoordinates()))) {
						newRoute = twoOptSwap(s, i, k);
						newRouteLength = calcRouteLength(startCoordinates, newRoute);
						
						// if swapping the nodes improved the route length then set this new found route as the best route so far
						if (newRouteLength < bestRouteLength) {
							s = newRoute;
							bestRouteLength = newRouteLength;
							improvementMade = true;
						}
					}
					
				}
			}
			
			if (improvementMade == false) {
				return s;
			}
		}
	}
	
	// use the greedy/nearestNeighbor algorithm to determine which order the best route to visit the sensors are given a start (and end) position
	// returns a list of Sensors
	public static List<Sensor> nearestNeighbor(Point2D startCoords, List<Sensor> requiredSensors) {
		var calculatedRoute = new ArrayList<Sensor>();
		var currentCoords = startCoords;
		
		while (requiredSensors.size() > 0) {
			var closestSensor = findClosestSensor(currentCoords, requiredSensors);
			currentCoords = closestSensor.getCoordinates();
			calculatedRoute.add(closestSensor);
			requiredSensors.remove(closestSensor);
		}
		return calculatedRoute;
	}
	
	// creates a mapbox Feature using the given arguments
	public static Feature makeSensorFeature(double x, double y, String location, String colour, String symbol) {
		var sensor = Point.fromLngLat(y, x);
		var fSensor = Feature.fromGeometry((Geometry)sensor);
		fSensor.addStringProperty("location", location);
		fSensor.addStringProperty("rgb-string", colour);
		fSensor.addStringProperty("marker-color", colour);
		fSensor.addStringProperty("marker-symbol", symbol);
		return fSensor;
	}
	
	// finds the angle between two points relative to the east of 'start'
	public static double getAngle(Point2D start, Point2D target) {
		// use atan2 function to get the angle to the given point
		var angle = Math.toDegrees(Math.atan2(target.getX() - start.getX(), target.getY() - start.getY()));
	    // correct angles which are negative(which occurs if target is positioned below the current position)
		if(angle < 0){
	        angle += 360;
	    }
	    return angle;
	}
	
	// checks to see if the given LineString intersects with any of the Obstacles in noFlyZones
	// if so, returns the Obstacle it intersects with
	public static Obstacle checkIllegalMove(LineString move) {
		// for every obstacle
		for (var obstacle : noFlyZones) {
			// create a LineString out of the obstacle
			var obstacleLineString = obstacle.getShape().outer();
			// check if the two LineStrings intersect
			if (lineIntersectsObstacle(move, obstacleLineString)) {
				// if so return the obstacle 
				return obstacle;
			}
		}
		// if no obstacles are intersected return null
		return null;
	}

	// checks to see if a given point is out of bounds according to NW, NE, SE, SW
	// if it is out of bounds, the given angle is checked to see if the drone should go clockwise or counterclockwise based on which boundary
	// is being intersected
	public static int isOutofBounds(Point2D p, double angle) {
		// breaching east boundary
		if (p.getY() >= NE[0]) {
			// if the drone is trying to go north
			if (angle < 180) {
				// tell it to go counterclockwise
				return 10;
			// if the drone is trying to go south
			} else {
				// tell it to go clockwise
				return -10;
			}
		// breaching west boundary
		} else if (p.getY() <= SW[0]) {
			// if the drone is trying to go north
			if (angle < 180) {
				// tell it to go clockwise
				return -10;
			// if the drone is trying to go south
			} else {
				// tell it to go counterclockwise
				return 10;
			}
		// breaching north boundary
		} else if (p.getX() >= NW[1]) {
			// if the drone is trying to go west
			if (angle < 270 && 90 <= angle) {
				// tell it to go counterclockwise
				return 10;
			// if the drone is trying to go east
			} else {
				// tell it to go clockwise
				return -10;
			}
		// breaching south boundary
		} else if (p.getX() <= SE[1]) {
			// if the drone is trying to go west
			if (angle < 270 && 90 <= angle) {
				// tell it to go clockwise
				return -10;
				// if the drone is trying to go east
			} else {
				// tell it to go counterclockwise
				return 10;
			}
		// if none of the boundaries have been breached
		} else {
			// return 0
			return 0;
		}
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////// HELPER FUNCTIONS /////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	// a helper function for 2-opt, swaps every node in given 'route' between indices i and k
	// returns the resulting route after the swap
	private static List<Sensor> twoOptSwap(List<Sensor> route, int i, int k) {
		List<Sensor> newRoute = new ArrayList<>();
		
		// take node 0 to node i-1 and add to the new route
		for (var n = 0; n <= i - 1; n++) {
			newRoute.add(route.get(n));
		}
		
		// take node i to route k and add to the new route in reverse order
		int reverseIndex = 0;
		for (int n = i; n <= k; n++) {
            newRoute.add(route.get(k - reverseIndex));
            reverseIndex++;
        }
		
		// take the rest of the nodes and add them to the new route
		for (int n = k + 1; n < route.size(); n++) {
            newRoute.add(route.get(n));
        }
		
		return newRoute;
	}
	
	// a helper function for nearestNeighbor, finds the sensor in 'possibleSensors' which is closest to the point 'coords'
	// return the Sensor which is closest
	private static Sensor findClosestSensor(Point2D coords, List<Sensor> possibleSensors) {
		Sensor closestSensor = null;
		var closestDistance = Double.MAX_VALUE;
		
		// for every possible sensor
		for (var sensor : possibleSensors) {
			var distance = coords.distance(sensor.getCoordinates());
			// if the distance is smaller than the current best distance, update the best distance and best sensor
			if (distance < closestDistance ) {
				closestSensor = sensor;
				closestDistance = distance;
			}
		}
		return closestSensor;
	}
	
	// a helper function for checkIllegalMove, if both of the given LineStrings intersect, it returns true. Else it returns false
	private static Boolean lineIntersectsObstacle(LineString l, LineString obstacle) {
		// get current and end point of line l
    	var m1 = l.coordinates().get(0);
    	var m2 = l.coordinates().get(1);
    		
    	// for every pair of points in the LineString obstacle
	    for (var i = 0; i < obstacle.coordinates().size(); i++) {
	    	var o1 = obstacle.coordinates().get(i);
	    	Point o2 = null;
	    	// if the current point is the last one then make the next point the first one as the LineString is assumed to be a closed obstacle
	    	if (i < obstacle.coordinates().size()-1) {
	    		o2 = obstacle.coordinates().get(i+1);
	    	} else {
	    		o2 = obstacle.coordinates().get(0);
	    	}
	    		
	    	// do some maths to figure out if the lines intersect. If they do, return true
	    	var ua_t = (o2.longitude() - o1.longitude())*(m1.latitude() - o1.latitude()) - (o2.latitude() - o1.latitude()) * (m1.longitude() - o1.longitude());
	    	var ub_t = (m2.longitude() - m1.longitude()) * (m1.latitude() - o1.latitude()) - (m2.latitude() - m1.latitude()) * (m1.longitude() - o1.longitude());
	    	var u_b = (o2.latitude() - o1.latitude()) * (m2.longitude() - m1.longitude()) - (o2.longitude() - o1.longitude()) * (m2.latitude() - m1.latitude());
	    	
	    	if (u_b != 0) {
	    		var ua = ua_t / u_b;
	    		var ub = ub_t / u_b;
	    		if (0 <= ua && ua <= 1 && 0 <= ub && ub <= 1) {
	    			return true;
	    		}
	    	}
	    }
	    // if the lines don't intersect, return false
	    return false;
	}
	
	// a helper function for 2-opt, calculates the total euclidean distance of a route. It takes a start point along with a list of
	// sensors. Visits them all chronologically.
	private static double calcRouteLength(Point2D startCoordinates, List<Sensor> sensors) {
		// initialise as 0
		double routeLength = 0;
		
		// first add the distance from the start point to the first sensor
		routeLength += startCoordinates.distance(sensors.get(0).getCoordinates());
		
		// for every consecutive sensor, add the distance between them
		for (int i = 0; i < sensors.size()-1; i++) {
			int j = i + 1;
			routeLength += sensors.get(i).getCoordinates().distance(sensors.get(j).getCoordinates());
		}
		
		// finally add the distance from the last visited sensor to the start point
		routeLength += sensors.get(sensors.size()-1).getCoordinates().distance(startCoordinates);
		
		return routeLength;
	}
	
	// SETTERS //
	public static void setNoFlyZones(List<Obstacle> noFlyZonesInput) {
		noFlyZones = noFlyZonesInput;
	}
}
