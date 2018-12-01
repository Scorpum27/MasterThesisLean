package ch.ethz.matsim.students.samark;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class CustomRailStop {

	String mode;
	TransitStopFacility transitStopFacility;
	// stopFacility has id="8500562.link:920757". First part is unique to the stop, but it can have several refLinks (second part)
	// The linkList collects all these linkIDs which refer to one stopFacility
	List<Id<Link>> linkRefIds;

	
	public CustomRailStop(){
		this.mode = "";
		this.linkRefIds = new ArrayList<Id<Link>>();
	}


	public CustomRailStop(TransitStopFacility transitStopFacility, Id<Link> linkRefId, String mode){
		this.mode = mode;
		this.transitStopFacility = transitStopFacility;
		this.linkRefIds = new ArrayList<Id<Link>>();
		this.linkRefIds.add(linkRefId);
	}
	
}