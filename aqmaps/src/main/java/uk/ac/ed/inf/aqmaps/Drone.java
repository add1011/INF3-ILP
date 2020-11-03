package uk.ac.ed.inf.aqmaps;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;

public class Drone {
	// ATTRIBUTES //
	private List<Move> flightPath;
	private List<Feature> featureList;
	private List<Point> points;
	private Point2D coordinates;
	private int movesLeft;
	
	// CONSTRUCTOR //
	public Drone(Point2D startCoordinates) {
		this.movesLeft = 150;
		this.flightPath = new ArrayList<>();
		this.featureList = new ArrayList<>();
		this.points = new ArrayList<>();
		this.points.add(Point.fromLngLat(startCoordinates.getY(), startCoordinates.getX()));
		// current position stored as lat, lng
		this.coordinates = (Point2D) startCoordinates.clone();
	}
	
	// METHODS //
	public Boolean getToSensor(Sensor s) {
		while (this.coordinates.distance(s.getCoordinates()) > 0.0002) {
			// get the angle to towards the point
			var angle = PathFinder.getAngle(this.coordinates, s.getCoordinates());
			// move towards the point once using the calculated angle
			var move = this.move(angle);
			
			// if move is null, then the drone is either out of moves or tried to go out of bounds
			if (move == null) {
				// return false to indicate to App that the drone will no longer be moving
				return false;
			}
			
			// if the drone is outside of 0.0002 of the sensor, keep moving towards it
			// if the drone is close enough, read the sensor
			if (this.coordinates.distance(s.getCoordinates()) > 0.0002) {
				this.flightPath.add(move);
			} else {
				this.checkSensor(s);
				move.setWords(s.getLocation());
				this.flightPath.add(move);
			}
		}
		return true;
	}
	
	public Boolean getToPoint(Point2D p) {
		while (this.coordinates.distance(p) > 0.0002) {
			// get the angle to towards the point
			var angle = PathFinder.getAngle(this.coordinates, p);
			// move towards the point once using the calculated angle
			var move = this.move(angle);
			
			if (move == null) {
				return false;
			}
			
			this.flightPath.add(move);
		}
		return true;
	}
	
	private Move move(double angle) {
		// do not execute if the drone does not have any moves left
		if (this.movesLeft < 1) {
			return null;
		}
		// round the angle to the nearest ten as that is the range in which the drone can move
		var direction = (int)(Math.round(angle/10.0) * 10);
		
		// ensure the direction of movement is within range use the modulo operator
		direction = Math.abs(direction % 360);
		
		// use basic planar trigonometry to find the position of the new point
		var x = this.coordinates.getX() + 0.0003 * Math.sin(Math.toRadians(direction));
		var y = this.coordinates.getY() + 0.0003 * Math.cos(Math.toRadians(direction));
		
		
		//////////////////////////////////////////////////////////////////////////////////////////////////
		/** if the drone is about to move out of bounds to get to the target, set the next best direction/
		/*  according to which end of the boundary the drone is at,									   **/
		var intersectsBoundary = false;		
		var whatWay = PathFinder.isOutofBounds(new Point2D.Double(x, y), angle);
		
		if (whatWay != 0) {
			intersectsBoundary = true;
		}
				
		while (intersectsBoundary != false) {
			// move the angle to the either clockwise or anti-clockwise
			angle += whatWay;
			
			// get the next location to move to
			var point = this.getNextBestMove(angle, whatWay);
			
			x = point.getX();
			y = point.getY();
			
			if (y < -3.184319 && y > -3.192473 && x < 55.946233 && x > 55.942617) {
				intersectsBoundary = false;
			}
		}
		//////////////////////////////////////////////////////////////////////////////////////////////////
		
		//////////////////////////////////////////////////////////////////////////////////////////////////
		List<Point> p = new ArrayList<>();
		p.add(Point.fromLngLat(this.coordinates.getY(), this.coordinates.getX()));
		p.add(Point.fromLngLat(y, x));
		var path = LineString.fromLngLats(p);
		var b = PathFinder.checkIllegalMove(path);
		
		// if the planned move is illegal (flies the drone into a no-fly zone) then find the next best direction to go
		if (b != null) {
			// find the angle from the drone to the centre of the building
			var angleToBuilding = PathFinder.getAngle(this.coordinates, b.getCentre());
			// find the angle to the goal relative to the angle to the building
			var relativeAngle = (angle + (360 - angleToBuilding)) % 360;

			// if the angle from the drone to the goal is less than the angle to the building then go clockwise around the building
			if (relativeAngle > 180) {
				whatWay = -10;
			} else {
				whatWay = 10;
			}
			
			/** used in the following while loop to commit to one direction to eliminate the risk of getting stuck going/
			/*  back and forth. 																					  **/
			var keepDir = false;
			
			while (true) {
				// move the angle to the either clockwise or anti-clockwise
				angle += whatWay;
				
				// get the next location to move to
			    var nextLocation = this.getNextBestMove(angle, whatWay);
			    
			    x = nextLocation.getX();
			    y = nextLocation.getY();
			    			
				// if the new path exits the boundary, go the other way around the building
				if (PathFinder.isOutofBounds(nextLocation, 0) != 0 && keepDir == false) {
					keepDir = true;
					whatWay *= -1;
					continue;
				}
				
				// create a path from the calculated direction to check if it intersects with a building
				p = new ArrayList<>();
				p.add(Point.fromLngLat(this.coordinates.getY(), this.coordinates.getX()));
				p.add(Point.fromLngLat(y, x));
				path = LineString.fromLngLats(p);
				
				b = PathFinder.checkIllegalMove(path);

				// if the new location does not enter a no-fly-zone and does not exit the boundaries, break the loop
				if (b == null && PathFinder.isOutofBounds(nextLocation, 0) == 0) {
					break;
				}
			}
		}
		//////////////////////////////////////////////////////////////////////////////////////////////////

		
		// update the location of the drone
		this.coordinates.setLocation(x, y);
		
		this.points.add(Point.fromLngLat(y, x));
		
		// remove one move from the number of moves left
		this.movesLeft -= 1;
				
		return new Move(this.coordinates.getY(),this.coordinates.getX(),direction,x,y);
	}
	
	public FeatureCollection buildReadings() {
		var path = LineString.fromLngLats(this.points);
		var fPath = Feature.fromGeometry((Geometry)path);
		
		this.featureList.add(fPath);
		
		return FeatureCollection.fromFeatures(this.featureList);
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////// HELPER FUNCTIONS /////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private Point2D avoidBuilding() {
		return null;
	}
	
	private Point2D getNextBestMove(double angle, int whatWay) {		
		// round the angle to the nearest ten as that is the range in which the drone can move
		var direction = (int)(Math.round(angle/10.0) * 10);
		
		// ensure the direction of movement is within range use the modulo operator
		direction = Math.abs(direction % 360);
		
		// use basic planar trigonometry to find the position of the new point
		var x = this.coordinates.getX() + 0.0003 * Math.sin(Math.toRadians(direction));
		var y = this.coordinates.getY() + 0.0003 * Math.cos(Math.toRadians(direction));
		
		if (this.points.size() > 3) {
			for (var point : this.points.subList(this.points.size()-4, this.points.size())) {
				var pointt = new Point2D.Double(point.latitude(), point.longitude());
				if (pointt.equals(new Point2D.Double(x, y))) {
					direction = Math.abs((direction + 3 * whatWay) % 360);
					
					// use basic planar trigonometry to find the position of the new point
					x = this.coordinates.getX() + 0.0003 * Math.sin(Math.toRadians(direction));
					y = this.coordinates.getY() + 0.0003 * Math.cos(Math.toRadians(direction));
				}
			}
		}
		
		return new Point2D.Double(x, y);
	}
	
	private void checkSensor(Sensor s) {		
		var colour = "";
		var markerSymbol = "";
		var reading = 0.0;
		var battery = s.getBattery();
		
		// make sure the reading is not null or NaN
		if (s.getReading().equals("null") == false && s.getReading().equals("NaN") == false) {
			reading = Double.parseDouble(s.getReading());
		}
		
		if (reading < 0 || 256 < reading || battery < 10) {
			colour = "#000000";
			markerSymbol = "cross";
		} else if (reading < 32) {
			colour = "#00ff00";
			markerSymbol = "lighthouse";
		} else if (reading < 64) {
			colour = "#40ff00";
			markerSymbol = "lighthouse";
		} else if (reading < 96) {
			colour = "#80ff00";
			markerSymbol = "lighthouse";
		} else if (reading < 128) {
			colour = "#c0ff00";
			markerSymbol = "lighthouse";
		} else if (reading < 160) {
			colour = "#ffc000";
			markerSymbol = "danger";
		} else if (reading < 192) {
			colour = "#ff8000";
			markerSymbol = "danger";
		} else if (reading < 224) {
			colour = "#ff4000";
			markerSymbol = "danger";
		} else if (reading < 256) {
			colour = "#ff0000";
			markerSymbol = "danger";
		}
		
		var sensorFeature = PathFinder.makeSensorFeature(s.getCoordinates().getX(), s.getCoordinates().getY(), s.getLocation(), colour, markerSymbol);
		this.featureList.add(sensorFeature);
	}
	
	// GETTERS AND SETTERS //
	
	public List<Move> getFlightPath() {
		return this.flightPath;
	}
	
	public List<Feature> getFeatureList() {
		return this.featureList;
	}
	
	public int getMovesLeft() {
		return this.movesLeft;
	}
}
