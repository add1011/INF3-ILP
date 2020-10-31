package uk.ac.ed.inf.aqmaps;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;

public class PathFinder {
			
	public static void main(String[] args) throws IOException, InterruptedException {
		//var b = IO.readBuildings("80");
		
		var s = IO.readSensors("01", "01", "2020", "80");
		var p = new Point2D.Double(55.9444, -3.1878);
		/**
		distanceMatrix = new long[s.size()][s.size()];
		for (int i = 0; i < s.size(); i++) {
			for (int j = 0; j < s.size(); j++) {
				distanceMatrix[i][j] = (long) (s.get(i).getCoordinates().distance
						(s.get(j).getCoordinates())  * 1000000000);
			}
		}
		**/
		
		tsp(p, s);
		/**
		for (int i = 0; i < distanceMatrix.length/2; i++) {
			System.out.println();
            // Loop through all elements of current row 
            for (int j = 0; j < distanceMatrix.length/2; j++) {
            	if (i == j) {
            		System.out.print("   0    | ");
            	} else {
            		System.out.print(distanceMatrix[i][j] + " | "); 

            	}
            }

            System.out.println("...");
		}
		for (int j = 0; j < distanceMatrix.length/2; j++) {
			System.out.print("  ...   | "); 
        }
        **/
		//System.out.println(p.coordinates());
		
		
		System.out.println(calcRouteLength(p, s));
		
		s = nearestNeighbor(p, s);
		
		System.out.println(calcRouteLength(p, s));
		
		s = tsp(p, s);
		
		System.out.println(calcRouteLength(p, s));
	}
	
	public static List<Sensor> tsp(Point2D startCoordinates, List<Sensor> s) {
		/**
		distanceMatrix = new long[requiredSensors.size()][requiredSensors.size()];
		for (int i = 0; i < requiredSensors.size(); i++) {
			for (int j = 0; j < requiredSensors.size(); j++) {
				distanceMatrix[i][j] = (long) (requiredSensors.get(i).getCoordinates().distance
						(requiredSensors.get(j).getCoordinates())  * 1000000000);
			}
		}
		**/
		List<Sensor> newRoute;
		double newRouteLength;
		var bestRouteLength = calcRouteLength(startCoordinates, s);
		var improvementMade = true;
		
		// repeat until no improvements are made
		while (improvementMade == true) {
			improvementMade = false;
			
			for(var i = 1; i < s.size()-2; i++) {
				for (var k = i+1; k < s.size()-1; k++) {
					
					if ((s.get(i).getCoordinates().distance(s.get(k + 1).getCoordinates()) + s.get(i - 1).getCoordinates().distance(s.get(k).getCoordinates())) <= 
						(s.get(i).getCoordinates().distance(s.get(i - 1).getCoordinates()) + s.get(k + 1).getCoordinates().distance(s.get(k).getCoordinates()))) {
						newRoute = twoOptSwap(s, i, k);
						newRouteLength = calcRouteLength(startCoordinates, newRoute);
						
						if (newRouteLength < bestRouteLength) {
							s = newRoute;
							bestRouteLength = newRouteLength;
							improvementMade = true;
						}
					}
					
				}
			}
		}
		return s;
	}
	
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
	
	public static List<Sensor> nearestNeighbor(Point2D startCoords, List<Sensor> requiredSensors) {
		var calculatedRoute = new ArrayList<Sensor>();
		var currentCoords = startCoords;
		
		while (requiredSensors.size() > 0) {
			var closestSensor = findClosestSensor(requiredSensors, currentCoords);
			currentCoords = closestSensor.getCoordinates();
			calculatedRoute.add(closestSensor);
			requiredSensors.remove(closestSensor);
		}
		return calculatedRoute;
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
	
	public static Obstacle checkIllegalMove(LineString move, List<Obstacle> noFlyZones) {
		for (var building : noFlyZones) {
			var buildingLineString = building.getShape().outer();
			if (lineIntersectsBuilding(move, buildingLineString)) {
				return building;
			}
		}
		return null;
	}
	
	private static Boolean lineIntersectsBuilding(LineString l, LineString building) {
		var intersects = false;
    	var p1 = l.coordinates().get(0);
    	var p2 = l.coordinates().get(1);

	    for (var i = 0; i < building.coordinates().size(); i++) {
	    	var q1 = building.coordinates().get(i);
	    	Point q2 = null;
	    	if (i < building.coordinates().size()-1) {
	    		q2 = building.coordinates().get(i+1);
	    	} else {
	    		q2 = building.coordinates().get(0);
	    	}
	    		
	    	var ua_t = (q2.longitude() - q1.longitude())*(p1.latitude() - q1.latitude()) - (q2.latitude() - q1.latitude()) * (p1.longitude() - q1.longitude());
	    	var ub_t = (p2.longitude() - p1.longitude()) * (p1.latitude() - q1.latitude()) - (p2.latitude() - p1.latitude()) * (p1.longitude() - q1.longitude());
	    	var u_b = (q2.latitude() - q1.latitude()) * (p2.longitude() - p1.longitude()) - (q2.longitude() - q1.longitude()) * (p2.latitude() - p1.latitude());
	    	
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
	
	private static Sensor findClosestSensor(List<Sensor> possibleSensors, Point2D coords) {
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
}
