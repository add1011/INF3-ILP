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
	private List<Obstacle> noFlyZones;
	private List<Move> flightPath;
	private FeatureCollection readings;
	private List<Feature> featureList;
	private List<Point> points;
	private Point2D coordinates;
	private int movesLeft;
	
	// CONSTRUCTOR //
	public Drone(Point2D startCoordinates, List<Obstacle> noFlyZones) {
		this.movesLeft = 150;
		this.noFlyZones = noFlyZones;
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
		direction = direction % 360;
		
		// use basic planar trigonometry to find the position of the new point
		var x = this.coordinates.getX() + 0.0003 * Math.sin(Math.toRadians(direction));
		var y = this.coordinates.getY() + 0.0003 * Math.cos(Math.toRadians(direction));
		
		Boolean intersectsBoundaries = false;
		Boolean intersectsBuilding = false;
		
		// if (y >= -3.184319 || y <= -3.192473 || x >= 55.946233 || x <= 55.942617)
		
		
		var whatWay = 0;
		var validMove = true;
		// if the drone is about to move out of bounds to get to the target, try the next best direction
		// breaching right boundary
		if (y >= -3.184319) {
			intersectsBoundaries = false;
			if (angle < 180) {
				whatWay = 10;
			} else {
				whatWay = -10;
			}
		// breaching left boundary
		} else if (y <= -3.192473) {
			intersectsBoundaries = false;
			if (angle < 180) {
				whatWay = -10;
			} else {
				whatWay = 10;
			}
		// breaching top boundary
		} else if (x >= 55.946233) {
			intersectsBoundaries = false;
			if (angle < 270 && 90 <= angle) {
				whatWay = 10;
			} else {
				whatWay = -10;
			}
		// breaching bottom boundary
		} else if (x <= 55.942617) {
			intersectsBoundaries = false;
			if (angle < 270 && 90 <= angle) {
				whatWay = -10;
			} else {
				whatWay = 10;
			}
		}
		
		while (intersectsBoundaries != false) {
			angle += whatWay;
			
			// round the angle to the nearest ten as that is the range in which the drone can move
			direction = (int)(Math.round(angle/10.0) * 10);
			
			// ensure the direction of movement is within range use the modulo operator
			direction = direction % 360;
			
			// use basic planar trigonometry to find the position of the new point
			x = this.coordinates.getX() + 0.0003 * Math.sin(Math.toRadians(direction));
			y = this.coordinates.getY() + 0.0003 * Math.cos(Math.toRadians(direction));
			
			if (y < -3.184319 && y > -3.192473 && x < 55.946233 && x > 55.942617) {
				validMove = true;
			}
			
			System.out.println("BOUNDARIES");
		}
		
		
		
		List<Point> p = new ArrayList<>();
		p.add(Point.fromLngLat(this.coordinates.getY(), this.coordinates.getX()));
		p.add(Point.fromLngLat(y, x));
		
		var path = LineString.fromLngLats(p);
		
		// if the planned move is illegal (flies the drone into a no-fly zone) then find the next best direction to go
		var b = PathFinder.checkIllegalMove(path, this.noFlyZones);
		if (b != null) {
			intersectsBuilding = true;
		}
		
		if (intersectsBuilding != false) {
			// find the angle from the drone to the centre of the building
			var angleToBuilding = PathFinder.getAngle(this.coordinates, b.getCentre());
			var relativeAngle = (angle + (360 - angleToBuilding)) % 360;
			
			/**
			System.out.println("angle:               " + angle);
			System.out.println("relativeAngle:       " + relativeAngle);
			System.out.println("angleToBuilding:     " + angleToBuilding);
			System.out.println("angleToBuilding+180: " + ((angleToBuilding + 180) % 360));
			System.out.println("-------------------------------------------");
			**/
			// if the angle from the drone to the goal is less than the angle to the building then go clockwise around the building
			// 
			if (relativeAngle > 180) {
				whatWay = -10;
			} else {
				whatWay = 10;
			}
			
			while (intersectsBuilding != false) {
				angle += whatWay;
				
				// round the angle to the nearest ten as that is the range in which the drone can move
				direction = (int)(Math.round(angle/10.0) * 10);
					
				// ensure the direction of movement is within range use the modulo operator
				direction = direction % 360;
					
				// use basic planar trigonometry to find the position of the new point
				x = this.coordinates.getX() + 0.0003 * Math.sin(Math.toRadians(direction));
				y = this.coordinates.getY() + 0.0003 * Math.cos(Math.toRadians(direction));
				
				
				p = new ArrayList<>();
				p.add(Point.fromLngLat(this.coordinates.getY(), this.coordinates.getX()));
				p.add(Point.fromLngLat(y, x));
				path = LineString.fromLngLats(p);
					
				b = PathFinder.checkIllegalMove(path, this.noFlyZones);
					
				if (b == null) {
					intersectsBuilding = false;
				}
			}
		}

		// update the location of the drone
		this.coordinates.setLocation(x, y);
		
		this.points.add(Point.fromLngLat(y, x));
		
		// remove one move from the number of moves left
		this.movesLeft -= 1;
		
		return new Move(this.coordinates.getY(),this.coordinates.getX(),direction,x,y);
	}
	
	public void checkSensor(Sensor s) {
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
	
	public void buildReadings() {
		var path = LineString.fromLngLats(this.points);
		var fPath = Feature.fromGeometry((Geometry)path);
		
		this.featureList.add(fPath);
		
		this.readings = FeatureCollection.fromFeatures(this.featureList);
	}
	
	// GETTERS AND SETTERS //
	
	public List<Move> getFlightPath() {
		return this.flightPath;
	}
	
	public FeatureCollection getReadings() {
		return this.readings;
	}
	
	public List<Feature> getFeatureList() {
		return this.featureList;
	}
	
	public int getMovesLeft() {
		return this.movesLeft;
	}
}
