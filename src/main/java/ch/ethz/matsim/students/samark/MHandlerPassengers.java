package ch.ethz.matsim.students.samark;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.events.handler.GenericEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class MHandlerPassengers implements GenericEventHandler{
	// TODO change TransitLine Numbering and Naming from TransitLine_Nr4 to Network2Route4

	Map<String,Double> routeDistances;
	List<String> metroPassengers;
	
	public MHandlerPassengers() {
		this.routeDistances = new HashMap<String,Double>();
		this.metroPassengers = new ArrayList<String>();
	}

	
	@Override
	public void handleEvent(GenericEvent event) {
//		TransitRouteStop accessStop = null;
//		TransitRouteStop egressStop = null;
		
		if (event.getEventType().contains("pt_transit")) {	// first add distance for every pt_transit movement
			double distance = Double.parseDouble(event.getAttributes().get("travelDistance"));			
			if (event.getAttributes().get("accessStop").contains("metro")) { // then add distance specifically for every METRO pt_transit movement
				
				// --- This subsection updates the distance by weighting links within city zone twice (CityZone= [Center=2683114/1248092], [Radius=4400])
				// CAUTION: Several links touch the access/egressStops, but only one is referred to as refLinkId of the stop.
				// So we can't just find access/egress links by linkRefIds as in this.transitSchedule.getFacilities().get(accessStopId).getLinkId();
				// Instead go through each link and check if StopFacility is at one of its nodes!
				// --- This subsection has been removed on 17.10: See versions before 17.10 for extended module.
								
				String personId = event.getAttributes().get("person");
				String route = event.getAttributes().get("route");
				
				if( ! metroPassengers.contains(personId)) {
					metroPassengers.add(personId);
				}
				if ( ! routeDistances.keySet().contains(route)) {
					routeDistances.put(route, distance);
				}
				else {
					double oldDistance = routeDistances.get(route);
					routeDistances.put(route, oldDistance+distance);
				}
			}
		}
		// Code BEFORE 15.09.2018 for earlier attempts and additional code snippets
		// Code BEFORE 17.10.2018 for extended module with exact access/egress stops

	}

	
	
}