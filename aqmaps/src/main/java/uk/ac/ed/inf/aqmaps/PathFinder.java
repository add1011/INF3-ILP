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
	
	public static Feature makeSensorFeature(double x, double y, String location, String colour, String symbol) {
		var sensor = Point.fromLngLat(y, x);
		var fSensor = Feature.fromGeometry((Geometry)sensor);
		fSensor.addStringProperty("location", location);
		fSensor.addStringProperty("rgb-string", colour);
		fSensor.addStringProperty("marker-color", colour);
		fSensor.addStringProperty("marker-symbol", symbol);
		return fSensor;
	}
	
	public static double getAngle(Point2D start, Point2D target) {
		// use atan2 function to get the angle to the given point
		var angle = Math.toDegrees(Math.atan2(target.getX() - start.getX(), target.getY() - start.getY()));
	    // correct angles which are negative(which occurs if target is positioned below the current position)
		if(angle < 0){
	        angle += 360;
	    }
	    
	    return angle;
	}
	
	public static Obstacle checkIllegalMove(LineString move) {
		for (var obstacle : noFlyZones) {
			var obstacleLineString = obstacle.getShape().outer();
			if (lineIntersectsObstacle(move, obstacleLineString)) {
				return obstacle;
			}
		}
		return null;
	}
	
	public static int isOutofBounds(Point2D p, double angle) {
		if (p.getY() >= NE[0]) {
			if (angle < 180) {
				return 10;
			} else {
				return -10;
			}
		// breaching left boundary
		} else if (p.getY() <= SW[0]) {
			if (angle < 180) {
				return -10;
			} else {
				return 10;
			}
		// breaching top boundary
		} else if (p.getX() >= NW[1]) {
			if (angle < 270 && 90 <= angle) {
				return 10;
			} else {
				return -10;
			}
		// breaching bottom boundary
		} else if (p.getX() <= SE[1]) {
			if (angle < 270 && 90 <= angle) {
				return -10;
			} else {
				return 10;
			}
		} else {
			return 0;
		}
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////// HELPER FUNCTIONS /////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
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
	
	private static Sensor findClosestSensor(Point2D coords, List<Sensor> possibleSensors) {
		Sensor closestSensor = null;
		var closestDistance = Double.MAX_VALUE;
		
		for (var sensor : possibleSensors) {
			var distance = coords.distance(sensor.getCoordinates());
			if (distance < closestDistance ) {
				closestSensor = sensor;
				closestDistance = distance;
			}
		}
		return closestSensor;
	}
	
	private static Boolean lineIntersectsObstacle(LineString l, LineString obstacle) {
		var intersects = false;
    	var m1 = l.coordinates().get(0);
    	var m2 = l.coordinates().get(1);

	    for (var i = 0; i < obstacle.coordinates().size(); i++) {
	    	var o1 = obstacle.coordinates().get(i);
	    	Point o2 = null;
	    	if (i < obstacle.coordinates().size()-1) {
	    		o2 = obstacle.coordinates().get(i+1);
	    	} else {
	    		o2 = obstacle.coordinates().get(0);
	    	}
	    		
	    	var ua_t = (o2.longitude() - o1.longitude())*(m1.latitude() - o1.latitude()) - (o2.latitude() - o1.latitude()) * (m1.longitude() - o1.longitude());
	    	var ub_t = (m2.longitude() - m1.longitude()) * (m1.latitude() - o1.latitude()) - (m2.latitude() - m1.latitude()) * (m1.longitude() - o1.longitude());
	    	var u_b = (o2.latitude() - o1.latitude()) * (m2.longitude() - m1.longitude()) - (o2.longitude() - o1.longitude()) * (m2.latitude() - m1.latitude());
	    	
	    	if (u_b != 0) {
	    		var ua = ua_t / u_b;
	    		var ub = ub_t / u_b;
	    		if (0 <= ua && ua <= 1 && 0 <= ub && ub <= 1) {
	    			intersects = true;
	    		}
	    	}
	    }
	    return intersects;
	}
	
	private static double calcRouteLength(Point2D startCoordinates, List<Sensor> sensors) {
		double routeLength = 0;
		
		routeLength += startCoordinates.distance(sensors.get(0).getCoordinates());
		
		for (int i = 0; i < sensors.size(); i++) {
			int j = i + 1;
			if ((i + 1) == sensors.size()) {
				j = 0;
			}
			routeLength += sensors.get(i).getCoordinates().distance(sensors.get(j).getCoordinates());
		}
		
		routeLength += sensors.get(sensors.size()-1).getCoordinates().distance(startCoordinates);
		
		return routeLength;
	}
	
	// SETTERS //
	
	public static void setNoFlyZones(List<Obstacle> noFlyZonesInput) {
		noFlyZones = noFlyZonesInput;
	}
}
