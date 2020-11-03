package uk.ac.ed.inf.aqmaps;

public class Move {
	private double beforeLng;
	private double beforeLat;
	private int direction;
	private double afterLng;
	private double afterLat;
	private String words;
	
	public Move(double beforeLng, double beforeLat, int direction, double afterLng, double afterLat) {
		this.beforeLng = beforeLng;
		this.beforeLat = beforeLat;
		this.direction = direction;
		this.afterLng = afterLng;
		this.afterLat = afterLat;
	}
	
	public void setWords(String words) {
		this.words = words;
	}
	
	public double getBeforeLng() {
		return beforeLng;
	}

	public double getBeforeLat() {
		return beforeLat;
	}

	public int getDirection() {
		return direction;
	}

	public double getAfterLng() {
		return afterLng;
	}

	public double getAfterLat() {
		return afterLat;
	}

	public String getWords() {
		return words;
	}	
}
