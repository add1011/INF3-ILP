package uk.ac.ed.inf.aqmaps;

import java.awt.geom.Point2D;

import com.mapbox.geojson.Polygon;

public class Obstacle {
	private Polygon shape;
	private Point2D centre;
	
	public Obstacle(Polygon shape) {
		this.shape = shape;
		
		// use the polygon to determine the centre of the obstacle
		var lowestX = Double.POSITIVE_INFINITY;
		var lowestY = Double.POSITIVE_INFINITY;
		var largestX = Double.NEGATIVE_INFINITY;
		var largestY = Double.NEGATIVE_INFINITY;
		var points = shape.coordinates().get(0);
		for (var point : points) {
			var xx = point.coordinates().get(0);
			var yy = point.coordinates().get(1);
			if (yy < lowestY) {
				lowestY = yy;
			} else if (yy > largestY){
				largestY = yy;
			}
			if (xx < lowestX) {
				lowestX = xx;
			} else if (xx > largestX){
				largestX = xx;
			}
		}
		
		// save the centre as an attribute
		this.centre = new Point2D.Double(((largestY + lowestY) / 2), ((largestX + lowestX) / 2));
	}
	
	public Polygon getShape() {
		return this.shape;
	}
	
	public Point2D getCentre() {
		return this.centre;
	}
}
