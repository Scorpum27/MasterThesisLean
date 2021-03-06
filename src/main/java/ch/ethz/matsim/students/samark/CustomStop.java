package ch.ethz.matsim.students.samark;
 import java.util.ArrayList;
import java.util.List;
 import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
 public class CustomStop {
	 public String mode;
	 public TransitStopFacility transitStopFacility;
	 public List<TransitStopFacility> originalTransitStopFacilities;
	 public TransitStopFacility originalMainTransitStopFacility;
	// stopFacility has id="8500562.link:920757". First part is unique to the stop, but it can have several refLinks (second part)
	// The linkList collects all these linkIDs which refer to one stopFacility
	 public List<Id<Link>> linkRefIds;
	 public List<String> nextOriginalTransitStopNames;
	 public Id<Node> newNetworkNode;
	 public boolean addedToNewSchedule;
	 public boolean used;
	
	public CustomStop(){
		this.mode = "";
		this.linkRefIds = new ArrayList<Id<Link>>();
		this.nextOriginalTransitStopNames = new ArrayList<String>();
		this.newNetworkNode = null;
		this.addedToNewSchedule = false;
		this.used = false;
	}
 	public CustomStop(TransitStopFacility originalTransitStopFacility, Id<Link> linkRefId, String mode){
		this.mode = mode;
		this.originalTransitStopFacilities = new ArrayList<TransitStopFacility>();
		this.originalMainTransitStopFacility = originalTransitStopFacility;
		this.originalTransitStopFacilities.add(originalTransitStopFacility);
		this.nextOriginalTransitStopNames = new ArrayList<String>();
		this.linkRefIds = new ArrayList<Id<Link>>();
		this.linkRefIds.add(linkRefId);
		this.newNetworkNode = null;
		this.addedToNewSchedule = false;
		this.used = false;
	}
	
	public CustomStop(TransitStopFacility transitStopFacility, Id<Node> networkNode, String mode, boolean addedToSchedule){
		this.mode = mode;
		this.transitStopFacility = transitStopFacility;
		this.originalMainTransitStopFacility = null;
		this.originalTransitStopFacilities = null;
		this.nextOriginalTransitStopNames = new ArrayList<String>();
		this.linkRefIds = new ArrayList<Id<Link>>();
		this.newNetworkNode = networkNode;
		this.addedToNewSchedule = addedToSchedule;
		this.used = false;
	}
	
}