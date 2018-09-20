package ch.ethz.matsim.students.samark;
 import java.util.ArrayList;
import java.util.List;
 import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
 public class CustomStop {
 	String mode;
	TransitStopFacility transitStopFacility;
	TransitStopFacility originalTransitStopFacility;
	// stopFacility has id="8500562.link:920757". First part is unique to the stop, but it can have several refLinks (second part)
	// The linkList collects all these linkIDs which refer to one stopFacility
	List<Id<Link>> linkRefIds;
	List<String> nextOriginalTransitStopNames;
	Id<Node> newNetworkNode;
	boolean addedToNewSchedule;
	
	public CustomStop(){
		this.mode = "";
		this.linkRefIds = new ArrayList<Id<Link>>();
		this.nextOriginalTransitStopNames = new ArrayList<String>();
		this.newNetworkNode = null;
		this.addedToNewSchedule = false;
	}
 	public CustomStop(TransitStopFacility originalTransitStopFacility, Id<Link> linkRefId, String mode){
		this.mode = mode;
		this.originalTransitStopFacility = originalTransitStopFacility;
		this.nextOriginalTransitStopNames = new ArrayList<String>();
		this.linkRefIds = new ArrayList<Id<Link>>();
		this.linkRefIds.add(linkRefId);
		this.newNetworkNode = null;
		this.addedToNewSchedule = false;
	}
	
	public CustomStop(TransitStopFacility transitStopFacility, Id<Node> networkNode, String mode, boolean addedToSchedule){
		this.mode = mode;
		this.transitStopFacility = transitStopFacility;
//		this.originalTransitStopFacility = originalTransitStopFacility;
		this.nextOriginalTransitStopNames = new ArrayList<String>();
		this.linkRefIds = new ArrayList<Id<Link>>();
		this.newNetworkNode = networkNode;
		this.addedToNewSchedule = addedToSchedule;
	}
	
}