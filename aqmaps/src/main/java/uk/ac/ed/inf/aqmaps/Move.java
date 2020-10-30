package uk.ac.ed.inf.aqmaps;

public class Move {
	public double beforeLng;
	public double beforeLat;
	public int direction;
	public double afterLng;
	public double afterLat;
	public String words;
	
	public Move(double beforeLng, double beforeLat, int direction, double afterLng, double afterLat, String words) {
		this.beforeLng = beforeLng;
		this.beforeLat = beforeLat;
		this.direction = direction;
		this.afterLng = afterLng;
		this.afterLat = afterLat;
		this.words = words;
	}
	
	public Move(double beforeLng, double beforeLat, int direction, double afterLng, double afterLat) {
		this.beforeLng = beforeLng;
		this.beforeLat = beforeLat;
		this.direction = direction;
		this.afterLng = afterLng;
		this.afterLat = afterLat;
	}
}
