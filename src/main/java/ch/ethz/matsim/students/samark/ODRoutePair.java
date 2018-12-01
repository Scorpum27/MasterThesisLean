package ch.ethz.matsim.students.samark;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class ODRoutePair {

	Id<TransitStopFacility> O;
	Id<TransitStopFacility> D;
	Double time;
	
	public ODRoutePair() {
	}
	
	public ODRoutePair(Id<TransitStopFacility> Origin, Id<TransitStopFacility> Destination) {
		this.O = Origin;
		this.D = Destination;
	}
	
	public ODRoutePair(Id<TransitStopFacility> Origin, Id<TransitStopFacility> Destination, Double travelTime) {
		this.O = Origin;
		this.D = Destination;
		this.time = travelTime;
	}
	
}
