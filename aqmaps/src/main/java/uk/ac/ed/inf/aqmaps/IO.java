package uk.ac.ed.inf.aqmaps;

import java.awt.geom.Point2D;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
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

public class IO {	
	private static HttpClient client = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(10))
			.build();
	
	public static List<Sensor> readSensors(String day, String month, String year, String port) throws IOException, InterruptedException {
		var request = HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:"+port+"/maps/"+year+"/"+month+"/"+day+"/air-quality-data.json"))
				.GET()
				.build();
		var response = client.send(request, BodyHandlers.ofString());

		var gson = new Gson();
				
		List<Sensor> sensors = gson.fromJson(response.body(), new TypeToken<List<Sensor>>(){}.getType());
		
		for (var sensor : sensors) {
			sensor.setCoordinates(wordsToCoords(port, sensor.getLocation()));
		}
		
		return sensors;
	}
	
	public static List<Obstacle> readBuildings(String port) throws IOException, InterruptedException {
		var request = HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:"+port+"/buildings/no-fly-zones.geojson"))
				.GET()
				.build();
		var response = client.send(request, BodyHandlers.ofString());
		
		var fc = FeatureCollection.fromJson(response.body());
		
		List<Obstacle> buildings = new ArrayList<Obstacle>();
		
		for (var f : fc.features()) {
			var gBuilding = f.geometry();
			var pBuilding = (Polygon)gBuilding;
			var b = new Obstacle(pBuilding);
			
			buildings.add(b);
		}
		
		return buildings;
	}
	
	public static void writeReadings(FeatureCollection fc, String day, String month, String year) throws FileNotFoundException {
		var out = new PrintWriter("readings-"+day+"-"+month+"-"+year+".geojson");
		out.print(fc.toJson());
		out.close();
	}
	
	public static void writeFlightPath(List<Move> flightPath, String day, String month, String year) throws IOException {
		List<String> lines = new ArrayList<>();
		int l = 1;
		for (var move : flightPath) {
			var line = l + ","
						+ move.beforeLng + ","
						+ move.beforeLat + ","
						+ move.direction + ","
						+ move.afterLng + ","
						+ move.afterLat + ","
						+ move.words;
			lines.add(line);
			l++;
		}
		var file = Paths.get("flightpath-"+day+"-"+month+"-"+year+".txt");
		Files.write(file, lines);
	}
	
	private static Point2D wordsToCoords(String port, String words) throws IOException, InterruptedException {
		String[] w = words.split("\\.");
		var request = HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:"+port+"/words/"+w[0]+"/"+w[1]+"/"+w[2]+"/details.json"))
				.GET()
				.build();
		var response = client.send(request, BodyHandlers.ofString());
		
		//var parser = new JsonParser();
		var obj = JsonParser.parseString(response.body()).getAsJsonObject();
		var coordinatesObj = obj.get("coordinates").getAsJsonObject();
		
		var coordinates = new Point2D.Double(coordinatesObj.get("lat").getAsDouble(), coordinatesObj.get("lng").getAsDouble());
		return coordinates;
	}
}
