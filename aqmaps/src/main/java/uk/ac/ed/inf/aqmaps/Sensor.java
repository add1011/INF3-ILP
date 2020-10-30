package uk.ac.ed.inf.aqmaps;

import java.awt.geom.Point2D;

public class Sensor {
	// every sensor has a location, battery level and reading
	private String location;
	private double battery;
	private String reading;
	// co-ordinates of sensor will be calculated using the what3words system
	private Point2D coordinates;
	
	
	// GETTERS //
	public String getLocation() {
		return this.location;
	}
	
	public double getBattery() {
		return this.battery;
	}
	
	public String getReading() {
		return this.reading;
	}
	
	public Point2D getCoordinates() {
		return this.coordinates;
	}
	
	// SETTERS //
	public void setCoordinates(Point2D point) {
		this.coordinates = point;
	}
}
