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
	// moves the drone towards the given sensor. Once it is close enough, it reads the sensor.
	public Boolean getToSensor(Sensor s) {
		// keep trying to move until an exit condition is met
		while (true) {
			// get the angle to towards the point
			var angle = PathFinder.getAngle(this.coordinates, s.getCoordinates());
			// move towards the point once using the calculated angle
			var move = this.move(angle);
			
			// if move is null, then the drone is out of moves
			if (move == null) {
				// return false to indicate to 'App' that the drone has run out of moves
				return false;
			}
			
			// if the drone is outside of 0.0002 of the sensor, keep moving towards it
			// either way, add the move to the flight path
			if (this.coordinates.distance(s.getCoordinates()) > 0.0002) {
				this.flightPath.add(move);
			} else {
				// if the drone is close enough, read the sensor
				this.checkSensor(s);
				// add the sensor words to the move before adding it to the flight path
				move.setWords(s.getLocation());
				this.flightPath.add(move);
				// return true to indicate the drone reached the sensor and read it
				return true;
			}
		}
	}
	
	// moves the drone towards the given point
	public Boolean getToPoint(Point2D p) {
		while (this.coordinates.distance(p) > 0.0002) {
			// get the angle to towards the point
			var angle = PathFinder.getAngle(this.coordinates, p);
			// move towards the point once using the calculated angle
			var move = this.move(angle);
			
			// if move is null then the drone is out of moves so return false to indicate that
			if (move == null) {
				return false;
			}
			
			// add the move to the flight path
			this.flightPath.add(move);
		}
		return true;
	}
	
	// merges 'points' and 'featureList' into a FeatureCollection and returns it
	public FeatureCollection buildReadings() {
		var path = LineString.fromLngLats(this.points);
		var fPath = Feature.fromGeometry((Geometry)path);
		this.featureList.add(fPath);
		return FeatureCollection.fromFeatures(this.featureList);
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////// HELPER FUNCTIONS /////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// moves the drone towards the closest multiple of 10 towards the given angle by a distance of 0.0003.
	// returns a Move object consisting of the features of this movement. Returns null if the drone is out of moves.
	private Move move(double angle) {
		// do not execute if the drone does not have any moves left
		if (this.movesLeft < 1) {
			return null;
		}
		// round the angle to the nearest ten as that is the range in which the drone can move
		var direction = (int)(Math.round(angle/10.0) * 10);
		
		// ensure the direction of movement is within range using the modulo operator
		direction = Math.abs(direction % 360);
		
		// find the new location of the drone if it moved in the given direction
		var p = this.getNewLocation(direction);
		var x = p.getX();
		var y = p.getY();
		
		//////////////////////////////////////////////////////////////////////////////////////////////////
		/** if the drone is about to move out of bounds to get to the target, set the next best direction/
		/*  according to which end of the boundary the drone is at									   **/
		var whatWay = PathFinder.isOutofBounds(new Point2D.Double(x, y), angle);
		
		if (whatWay != 0) {
			while (true) {
				// move the angle to the either clockwise or anti-clockwise direction
				// also do maths to do modulo 360 for negative doubles
				angle = (angle + whatWay) - Math.floor((angle + whatWay)/360.0) * 360.0;
				
				// get the next location to move to
				var nextLocation = this.getNextBestMove(angle, whatWay);
				x = nextLocation.getX();
				y = nextLocation.getY();
				
				if (PathFinder.isOutofBounds(nextLocation, 0) == 0) {
					break;
				}
			}
		}
		//////////////////////////////////////////////////////////////////////////////////////////////////
		
		//////////////////////////////////////////////////////////////////////////////////////////////////
		var b = this.isMoveinObstacle(new Point2D.Double(x, y));
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
			
			while (true) {
				// move the angle to the either clockwise or anti-clockwise direction
				// also do maths to do modulo 360 for negative doubles
				angle = (angle + whatWay) - Math.floor((angle + whatWay)/360.0) * 360.0;
				
				// get the next location to move to
			    var nextLocation = this.getNextBestMove(angle, whatWay);
			    x = nextLocation.getX();
			    y = nextLocation.getY();
			    
				// if the new path exits the boundary, try another angle
				if (PathFinder.isOutofBounds(nextLocation, 0) != 0) {
					continue;
				}
				
				// create a path from the calculated direction to check if it intersects with a building
				b = this.isMoveinObstacle(nextLocation);
				// if the new location does not enter a no-fly-zone and does not exit the boundaries, break the loop
				if (b == null) {
					break;
				}
			}
		}
		//////////////////////////////////////////////////////////////////////////////////////////////////
		
		// update the location of the drone
		this.coordinates.setLocation(x, y);
		
		// add this new location to the list of points the drone has been at
		this.points.add(Point.fromLngLat(y, x));
		
		// remove one move from the number of moves left
		this.movesLeft -= 1;
				
		return new Move(this.coordinates.getY(),this.coordinates.getX(),direction,x,y);
	}
	
	// checks to see if a desired move intersects with an obstacle. If so, returns the obstacls it intersects with.
	private Obstacle isMoveinObstacle(Point2D p) {
		// create a path from the input coordinates to check if it intersects with a building line string
		List<Point> ps = new ArrayList<>();
		ps.add(Point.fromLngLat(this.coordinates.getY(), this.coordinates.getX()));
		ps.add(Point.fromLngLat(p.getY(), p.getX()));
		var path = LineString.fromLngLats(ps);
		return PathFinder.checkIllegalMove(path);
	}
	
	// returns if the drone is about to go back to a point it has been in the last 4 moves
	// If so, alter the direction by +-30 according to whatWay to avoid getting stuck.
	// return where the drone ends up after moving
	private Point2D getNextBestMove(double angle, int whatWay) {		
		// round the angle to the nearest ten as that is the range in which the drone can move
		var direction = (int)(Math.round(angle/10.0) * 10);
		
		// ensure the direction of movement is within range use the modulo operator
		direction = Math.abs(direction % 360);
		
		// find the new location of the drone if it moved in the given direction
		var p = this.getNewLocation(direction);
		var x = p.getX();
		var y = p.getY();
		
		// check that the drone has visited 4 points first
		if (this.points.size() > 3) {
			// for all last 4 points
			for (var point : this.points.subList(this.points.size()-4, this.points.size())) {
				var previouspoint = new Point2D.Double(point.latitude(), point.longitude());
				// check to see if the new point is the same as a previous one
				if (previouspoint.equals(new Point2D.Double(x, y))) {
					// if so, alter direction
					direction = Math.abs((direction + 3 * whatWay) % 360);
					
					// find the new location of the drone if it moved in the given direction
					p = this.getNewLocation(direction);
					x = p.getX();
					y = p.getY();
				}
			}
		}
		
		return new Point2D.Double(x, y);
	}
	
	// calculates where the drone will end up after moving in a given direction
	private Point2D getNewLocation(int direction) {
		// use trigonometry to figure out where the new coordinates are
		var x = this.coordinates.getX() + 0.0003 * Math.sin(Math.toRadians(direction));
		var y = this.coordinates.getY() + 0.0003 * Math.cos(Math.toRadians(direction));
		
		return new Point2D.Double(x, y);
	}
	
	// read the given sensor and create a Feature with it's details. Add the Feature to featureList
	private void checkSensor(Sensor s) {		
		var colour = "";
		var markerSymbol = "";
		var reading = 0.0;
		var battery = s.getBattery();
		
		// make sure the reading is not null or NaN
		if (s.getReading().equals("null") == false && s.getReading().equals("NaN") == false) {
			reading = Double.parseDouble(s.getReading());
		}
		
		// according to the reading or battery level, set the colour and symbol of the marker
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
