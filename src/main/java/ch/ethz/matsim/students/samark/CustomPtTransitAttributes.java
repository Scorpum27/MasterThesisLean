package ch.ethz.matsim.students.samark;

import java.util.HashMap;
import java.util.Map;

// this method is OBSOLETE (samark'19.08.2018)

public class CustomPtTransitAttributes {

	public CustomPtTransitAttributes() {
		this.routeBoardings = new HashMap<String, Double>();
	}
	
	String personId;
	Map<String, Double> routeBoardings;		// String=routeName, Double=boardingTime
	
	// double ptTransitEventTime;
	// String route;
	// double distanceTravelled;
	
}
