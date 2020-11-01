package uk.ac.ed.inf.aqmaps;


import java.io.IOException;
import java.time.LocalDate;

import org.junit.Test;

public class PerformanceTests {
	@Test
	public void testPerformance() throws IOException, InterruptedException {
		var averageMoves = 0.0;
		var timesFailed = 0;
		var worstPerformance = 0;
		var testsDone = 0;
		
		long startTime = System.currentTimeMillis();

		for (var year = 2020; year < 2022; year++) {
			for (var month = 1; month < 13; month++) {
				var daysInMonth = LocalDate.of(year, month, 1).lengthOfMonth();
				for (var day = 1; day < daysInMonth + 1; day++) {
					
					var d = String.valueOf(day);
					var m = String.valueOf(month);
					var y = String.valueOf(year);
					
					System.out.println(d + " : " + m + " : " + y);
										
					for (var startPos = 0; startPos < 5; startPos++) {
						
						var startLat = "";
						var startLng = "";
						var pos = "";
						
						if (startPos == 0) {
							startLat = "55.9444";
							startLng = "-3.1878";
							pos = "Centre";
						} else if (startPos == 1) {
							startLat = "55.946232";
							startLng = "-3.192472";
							pos = "Top Left";
						} else if (startPos == 2) {
							startLat = "55.946232";
							startLng = "-3.184320";
							pos = "Top Right";
						} else if (startPos == 3) {
							startLat = "55.942618";
							startLng = "-3.192472";
							pos = "Bottom Left";
						} else if (startPos == 4) {
							startLat = "55.942618";
							startLng = "-3.184320";
							pos = "Bottom Right";
						}
						
						String[] args = new String[] {d, m, y, startLat, startLng, "", "80"};
												
						var moves = App.runTest(args);
						
						if (moves == 150) {
							timesFailed++;
							System.out.println(d + " : " + m + " : " + y);
							System.out.println(pos);
							System.out.println("------");
						}
						
						//System.out.println(pos);
						//System.out.println("------");
						
						if (moves > worstPerformance && moves != 150) {
							worstPerformance = moves;
						}
						
						averageMoves += moves;
						testsDone++;
					}
					System.out.println("------");
					//System.out.println("-------------------------------");
				}
			}
		}
		
		long stopTime = System.currentTimeMillis();
		System.out.println("Runtime : " + (stopTime - startTime)/1000 + " seconds");
		
		averageMoves = averageMoves / testsDone;
		
		System.out.println("Average moves : " + averageMoves);
		System.out.println("Tests done : " + testsDone);
		System.out.println("Worst successful performance : " + worstPerformance);
		System.out.println("Times the drone Failed : " + timesFailed);
	}
}
