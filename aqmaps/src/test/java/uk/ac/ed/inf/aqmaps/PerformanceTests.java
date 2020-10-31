package uk.ac.ed.inf.aqmaps;


import java.io.IOException;
import java.time.LocalDate;

import org.junit.Before;
import org.junit.Test;

public class PerformanceTests {
	private double averageMoves;
	private int timesFailed;
	private int testsDone;
	
	@Before
	public void setUp() {
		averageMoves = 0;
		timesFailed = 0;
		testsDone = 0;
	}
	
	@Test
	public void testPerformance() throws IOException, InterruptedException {
		for (int year = 2020; year < 2021; year++) {
			for (int month = 1; month < 3; month++) {
				int daysInMonth = LocalDate.of(year, month, 1).lengthOfMonth();
				for (int day = 1; day < daysInMonth + 1; day++) {
					
					String d = String.valueOf(day);
					String m = String.valueOf(month);
					String y = String.valueOf(year);
					
					System.out.println(d + " : " + m + " : " + y);
					
					System.out.println("------");
					
					for (int startPos = 0; startPos < 5; startPos++) {
						
						String startLat = "";
						String startLng = "";
						String pos = "";
						
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
												
						int moves = App.runTest(args);
						
						if (moves == 150) {
							timesFailed++;
						}
						
						System.out.println(pos);
						System.out.println("------");
						
						averageMoves += moves;
						testsDone++;
					}
					System.out.println("-------------------------------");
				}
			}
		}
		
		averageMoves = averageMoves / testsDone;
		
		System.out.println(averageMoves);
		System.out.println("Tests done : " + testsDone);
		System.out.println("Times the drone Failed : " + timesFailed);
	}
}
