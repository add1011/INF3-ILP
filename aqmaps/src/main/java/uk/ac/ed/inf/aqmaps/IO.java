package uk.ac.ed.inf.aqmaps;

import java.awt.geom.Point2D;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Polygon;

//make this class final to emulate a static class
public final class IO {	
	private static final HttpClient client = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(10))
			.build();
	
	// do not allow this class to be instantiated
	private IO() {}
	
	// reads the sensors to be visited on the given date from the web server at the given port.
	// uses the data from the server to create a list of Sensors which is returned
	public static List<Sensor> readSensors(String day, String month, String year, String port) {
		// create request for data
		var request = HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:"+port+"/maps/"+year+"/"+month+"/"+day+"/air-quality-data.json"))
				.GET()
				.build();		

		// initialise response
		HttpResponse<String> response = null;
		try {
			// send the request and save the response
			response = client.send(request, BodyHandlers.ofString());
		} catch (IOException | InterruptedException e) {
			// if the connection is unsuccessful, print an error and exit the program
			System.out.println("Fatal error: Unable to connect to the server at port " + port + ".");
			System.exit(1);
		}
		
		// if the page cannot be found, print a message stating so and exit the program
		if (response.statusCode() == 404) {
			System.out.println("Unable to find the sensor data for the given date on the server. Terminating...");
			System.exit(1);
		}

		// use gson to parse the response and use it to create a list of Sensors
		var gson = new Gson();
		List<Sensor> sensors = gson.fromJson(response.body(), new TypeToken<List<Sensor>>(){}.getType());
		
		// for every sensor find the coordinates according to what3words and save the coordinates to the Sensor
		for (var sensor : sensors) {
			sensor.setCoordinates(wordsToCoords(port, sensor.getLocation()));
		}
		
		return sensors;
	}
	
	// reads the buildings from the web server at the given port.
	// uses the data from the server to create a list of Obstacles which is returned
	public static List<Obstacle> readBuildings(String port) {
		// create request for data
		var request = HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:"+port+"/buildings/no-fly-zones.geojson"))
				.GET()
				.build();
		
		// initialise response
		HttpResponse<String> response = null;
		try {
			// send the request and save the response
			response = client.send(request, BodyHandlers.ofString());
		} catch (IOException | InterruptedException e) {
			// if the connection is unsuccessful, print an error and exit the program
			System.out.println("Fatal error: Unable to connect to the server at port " + port + ".");
			System.exit(1);
		}
		
		// if the page cannot be found, print a message stating so and exit the program
		if (response.statusCode() == 404) {
			System.out.println("Unable to find the building data on the server. Terminating...");
			System.exit(1);
		}
		
		// convert the response from a string to a FeatureCollection
		var fc = FeatureCollection.fromJson(response.body());
	    // initialise the list
		List<Obstacle> buildings = new ArrayList<Obstacle>();
		
		// for every Feature in the FeatureCollection convert it to a Polygon and create a new Obstacle with it
		for (var f : fc.features()) {
			var gBuilding = f.geometry();
			var pBuilding = (Polygon)gBuilding;
			var b = new Obstacle(pBuilding);
			
			// add the Obstacle to buildings
			buildings.add(b);
		}
		
		return buildings;
	}
	
	// write a readings file using the given FeatureCollection and date
	public static void writeReadings(FeatureCollection fc, String day, String month, String year) {
		PrintWriter out = null;
		try {
			out = new PrintWriter("readings-"+day+"-"+month+"-"+year+".geojson");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		}
		out.print(fc.toJson());
		out.close();
	}
	
	// write a readings file using the given Moves and date
	public static void writeFlightPath(List<Move> flightPath, String day, String month, String year) {
		List<String> lines = new ArrayList<>();
		// use counter to add the line number to each line
		int l = 1;
		// for every move in the flight path
		for (var move : flightPath) {
			// create the line using the line number and all the attributes of the Move
			var line = l + ","
						+ move.getBeforeLng() + ","
						+ move.getBeforeLat() + ","
						+ move.getDirection() + ","
						+ move.getAfterLng() + ","
						+ move.getAfterLat() + ","
						+ move.getWords();
			lines.add(line);
			l++;
		}
		// write the file using the created lines
		var file = Paths.get("flightpath-"+day+"-"+month+"-"+year+".txt");
		try {
			Files.write(file, lines);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////// HELPER FUNCTIONS /////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	// returns the coordinates of given what3words from the server through the given port
	private static Point2D wordsToCoords(String port, String words) {
		// create a list of strings, each cell containing one of the words
		String[] w = words.split("\\.");
		// create request for data
		var request = HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:"+port+"/words/"+w[0]+"/"+w[1]+"/"+w[2]+"/details.json"))
				.GET()
				.build();
		
		// initialise response
		HttpResponse<String> response = null;
		try {
			// send the request and save the response
			response = client.send(request, BodyHandlers.ofString());
		} catch (IOException | InterruptedException e) {
			// if the connection is unsuccessful, print an error and exit the program
			System.out.println("Fatal error: Unable to connect to the server at port " + port + ".");
			System.exit(1);
		}
		
		// if the page cannot be found, print a message stating so and exit the program
		if (response.statusCode() == 404) {
			System.out.println("Unable to find the word data for the given date on the server. Terminating...");
			System.exit(1);
		}
		
		// parse the returned Json string with gson into a JsonObject
		var obj = JsonParser.parseString(response.body()).getAsJsonObject();
		// get the coordinates attribute from the object
		var coordinatesObj = obj.get("coordinates").getAsJsonObject();
		
		// create a Point2D using the lat and lng attributes of the object
		var coordinates = new Point2D.Double(coordinatesObj.get("lat").getAsDouble(), coordinatesObj.get("lng").getAsDouble());
		return coordinates;
	}
}
