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
	private int lastMove;
	
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
				move.words = s.getLocation();
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
	
	public Move move(double angle) {
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
		
		/**
		for (Point point : this.points) {
			Point2D pointt = new Point2D.Double(point.latitude(), point.longitude());
			if (pointt.equals(new Point2D.Double(x, y))) {
				direction = Math.abs((direction + 30) % 360);
			}
		}
		
		// use basic planar trigonometry to find the position of the new point
		x = this.coordinates.getX() + 0.0003 * Math.sin(Math.toRadians(direction));
		y = this.coordinates.getY() + 0.0003 * Math.cos(Math.toRadians(direction));
		**/
		//////////////////////////////////////////////////////////////////////////////////////////////////
		/** if the drone is about to move out of bounds to get to the target, set the next best direction/
		/*  according to which end of the boundary the drone is at.									   **/
		var intersectsBoundary = false;		
		var whatWay = 0;

		// breaching right boundary
		if (y >= -3.184319) {
			intersectsBoundary = true;
			if (angle < 180) {
				whatWay = 10;
			} else {
				whatWay = -10;
			}
		// breaching left boundary
		} else if (y <= -3.192473) {
			intersectsBoundary = true;
			if (angle < 180) {
				whatWay = -10;
			} else {
				whatWay = 10;
			}
		// breaching top boundary
		} else if (x >= 55.946233) {
			intersectsBoundary = true;
			if (angle < 270 && 90 <= angle) {
				whatWay = 10;
			} else {
				whatWay = -10;
			}
		// breaching bottom boundary
		} else if (x <= 55.942617) {
			intersectsBoundary = true;
			if (angle < 270 && 90 <= angle) {
				whatWay = -10;
			} else {
				whatWay = 10;
			}
		}
		
		while (intersectsBoundary != false) {
			angle += whatWay;
			
			// round the angle to the nearest ten as that is the range in which the drone can move
			direction = (int)(Math.round(angle/10.0) * 10);
			
			// ensure the direction of movement is within range use the modulo operator
			direction = Math.abs(direction % 360);
			
			// if the next best direction is back the way it came, try three more than that to not get caught in a loop
			//if (direction == ((this.lastMove + 180) % 360)) {
			//	direction = Math.abs((direction + 3 * whatWay) % 360);
			//}
			
			// use basic planar trigonometry to find the position of the new point
			x = this.coordinates.getX() + 0.0003 * Math.sin(Math.toRadians(direction));
			y = this.coordinates.getY() + 0.0003 * Math.cos(Math.toRadians(direction));
			
			if (this.points.size() > 3) {
				for (Point point : this.points.subList(this.points.size()-4, this.points.size())) {
					Point2D pointt = new Point2D.Double(point.latitude(), point.longitude());
					if (pointt.equals(new Point2D.Double(x, y))) {
						direction = Math.abs((direction + 3 * whatWay) % 360);
						
						// use basic planar trigonometry to find the position of the new point
						x = this.coordinates.getX() + 0.0003 * Math.sin(Math.toRadians(direction));
						y = this.coordinates.getY() + 0.0003 * Math.cos(Math.toRadians(direction));
					}
				}
			}
			
			if (y < -3.184319 && y > -3.192473 && x < 55.946233 && x > 55.942617) {
				intersectsBoundary = false;
			}
		}
		//////////////////////////////////////////////////////////////////////////////////////////////////
		
		
		List<Point> p = new ArrayList<>();
		p.add(Point.fromLngLat(this.coordinates.getY(), this.coordinates.getX()));
		p.add(Point.fromLngLat(y, x));
		
		var path = LineString.fromLngLats(p);
		
		// if the planned move is illegal (flies the drone into a no-fly zone) then find the next best direction to go
		var b = PathFinder.checkIllegalMove(path);
		
		if (b != null) {
			// find the angle from the drone to the centre of the building
			var angleToBuilding = PathFinder.getAngle(this.coordinates, b.getCentre());
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
				// move the angle to the calculated next direction to go to
				angle += whatWay;
				
				// round the angle to the nearest ten as that is the range in which the drone can move
				direction = (int)(Math.round(angle/10.0) * 10);
					
				// ensure the direction of movement is within range use the modulo operator
				direction = Math.abs(direction % 360);
					
				// use basic planar trigonometry to find the position of the new point
				x = this.coordinates.getX() + 0.0003 * Math.sin(Math.toRadians(direction));
				y = this.coordinates.getY() + 0.0003 * Math.cos(Math.toRadians(direction));
				
				
				if (this.points.size() > 3) {
					for (Point point : this.points.subList(this.points.size()-4, this.points.size())) {
						Point2D pointt = new Point2D.Double(point.latitude(), point.longitude());
						if (pointt.equals(new Point2D.Double(x, y))) {
							direction = Math.abs((direction + 3 * whatWay) % 360);
							
							// use basic planar trigonometry to find the position of the new point
							x = this.coordinates.getX() + 0.0003 * Math.sin(Math.toRadians(direction));
							y = this.coordinates.getY() + 0.0003 * Math.cos(Math.toRadians(direction));
						}
					}
				}
				
				// if the new path intersects the boundary, go the other way around the building
				if ((y >= -3.184319 || y <= -3.192473 || x >= 55.946233 || x <= 55.942617) && keepDir == false) {
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
				if (b == null && y < -3.184319 && y > -3.192473 && x < 55.946233 && x > 55.942617) {
					break;
				}
			}
		}
				
		// update the location of the drone
		this.coordinates.setLocation(x, y);
		
		this.points.add(Point.fromLngLat(y, x));
		
		// remove one move from the number of moves left
		this.movesLeft -= 1;
		
		this.lastMove = direction;
		
		return new Move(this.coordinates.getY(),this.coordinates.getX(),direction,x,y);
	}
	
	private void checkSensor(Sensor s) {
		var sensor = Point.fromLngLat(s.getCoordinates().getY(), s.getCoordinates().getX());
		var fSensor = Feature.fromGeometry((Geometry)sensor);
		
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
		
		fSensor.addStringProperty("location", s.getLocation());
		fSensor.addStringProperty("rgb-string", colour);
		fSensor.addStringProperty("marker-color", colour);
		fSensor.addStringProperty("marker-symbol", markerSymbol);
		this.featureList.add(fSensor);
	}
	
	public FeatureCollection buildReadings() {
		var path = LineString.fromLngLats(this.points);
		var fPath = Feature.fromGeometry((Geometry)path);
		
		this.featureList.add(fPath);
		
		return FeatureCollection.fromFeatures(this.featureList);
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
