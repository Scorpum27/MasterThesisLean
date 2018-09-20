package ch.ethz.matsim.students.samark;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class CustomMetroLinkAttributes {

	TransitStopFacility fromNodeStopFacility;
	TransitStopFacility toNodeStopFacility;
	TransitStopFacility singeRefStopFacility;
	Id<Link> originalLinkId;
	double cost;
	String type;
	
	public CustomMetroLinkAttributes() {
		this.fromNodeStopFacility = null;
		this.toNodeStopFacility = null;
		this.singeRefStopFacility = null;
		this.originalLinkId = null;
		this.cost = Double.MAX_VALUE;
	}
	
	public CustomMetroLinkAttributes(String type) {
		this.fromNodeStopFacility = null;
		this.toNodeStopFacility = null;
		this.singeRefStopFacility = null;
		this.originalLinkId = null;
		this.cost = Double.MAX_VALUE;
		this.type = type;
	}
	
	public CustomMetroLinkAttributes(String type, Id<Link> originalLinkId) {
		this.fromNodeStopFacility = null;
		this.toNodeStopFacility = null;
		this.singeRefStopFacility = null;
		this.originalLinkId = originalLinkId;
		this.cost = Double.MAX_VALUE;
		this.type = type;
	}
	
}
