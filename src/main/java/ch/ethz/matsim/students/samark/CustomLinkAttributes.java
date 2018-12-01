package ch.ethz.matsim.students.samark;

import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class CustomLinkAttributes {

	Double totalTraffic;
	//Double railTraffic;
	//Double tramTraffic;
	//Double busTraffic;
	String dominantMode;
	TransitStopFacility dominantStopFacility;
	TransitStopFacility nextRailwayStopFacility;
	double distance2nextRailwayStopFacility;
	
	public CustomLinkAttributes() {
		this.totalTraffic = 0.0;
		this.dominantMode = null;
		this.dominantStopFacility = null;
		this.nextRailwayStopFacility = null;
		this.distance2nextRailwayStopFacility = Double.MAX_VALUE;
	}
	
	public double getTotalTraffic() {
		return this.totalTraffic;
	}
	
	public void setTotalTraffic(double totalTrafficNew) {
		this.totalTraffic = totalTrafficNew;
	}
	
	public String getDominantMode() {
		return this.dominantMode;
	}
	

	public void setDominantMode(String dominantModeNew) {
		this.dominantMode = dominantModeNew;
	}
	
	public TransitStopFacility getDominantStopFacility() {
		return this.dominantStopFacility;
	}
	

	public void setDominantStopFacility(TransitStopFacility dominantStopFacilityNew) {
		this.dominantStopFacility = dominantStopFacilityNew;
	}
}