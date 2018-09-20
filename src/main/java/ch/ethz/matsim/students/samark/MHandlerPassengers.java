package ch.ethz.matsim.students.samark;

import java.io.IOException;
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

	Map<String, Map<String, Double>> travelStats;	// Map< PersonID, Map< RouteName, TravelDistance > >
	double totalBeelineKM;
	//Map<String, Double> transitPersonKM;			// Map<RouteName, TotalPersonKM>
	// Map<String, Double> transitPersonBeelineKM;	// reactivate this section for beeline distances // Map<RouteName, TotalPersonKM>
	Map<String, Integer> routeBoardingCounter;		// Map<RouteName, nBoardingsOnThatRoute>
	double totalPtTransitPersonKM;
	Network network;
	TransitSchedule transitSchedule;
	
	public MHandlerPassengers(Network network, TransitSchedule transitSchedule) {
		this.network = network;
		this.transitSchedule = transitSchedule;
		this.travelStats = new HashMap<String, Map<String, Double>>();
		this.totalBeelineKM = 0.0;
		this.routeBoardingCounter = new HashMap<String, Integer>();
		this.totalPtTransitPersonKM = 0.0;
	}

	
	@Override
	public void handleEvent(GenericEvent event) {
//		TransitRouteStop accessStop = null;
//		TransitRouteStop egressStop = null;
		
		if (event.getEventType().contains("pt_transit")) {	// first add distance for every pt_transit movement
			double distance = Double.parseDouble(event.getAttributes().get("travelDistance"));
			this.totalPtTransitPersonKM += distance;
			
			if (event.getAttributes().get("accessStop").contains("metro")) { // then add distance specifically for every METRO pt_transit movement
				
				// --- this subsection updates the distance by weighting links within city zone twice (CityZone= [Center=2683114/1248092], [Radius=4400])
				// CAUTION: Several links touch the access/egressStops, but only one is referred to as refLinkId of the stop.
				// So we can't just find access/egress links by linkRefIds as in this.transitSchedule.getFacilities().get(accessStopId).getLinkId();
				// Instead go through each link and check if StopFacility is at one of its nodes!
				Id<TransitStopFacility> stopId1 = (Id<TransitStopFacility>) Id.create(event.getAttributes().get("accessStop"), TransitStopFacility.class);
				Id<TransitStopFacility> stopId2 = (Id<TransitStopFacility>) Id.create(event.getAttributes().get("egressStop"), TransitStopFacility.class);
				TransitLine eventTransitLine = this.transitSchedule.getTransitLines().get(Id.create(event.getAttributes().get("line"), TransitLine.class));
				TransitRoute eventTransitRoute = eventTransitLine.getRoutes().get(Id.create(event.getAttributes().get("route"), TransitRoute.class));
				List<Id<Link>> routeLinks = NetworkEvolutionImpl.NetworkRoute2LinkIdList(eventTransitRoute.getRoute());
				Id<TransitStopFacility> egressStopId = null;
				Id<Link> accessLinkId = null;
				Id<Link> egressLinkId = null;
				boolean accessLinkFound = false;
				boolean egressLinkFound = false;
				
				try {
					for (Id<Link> linkId : routeLinks) {
						if (accessLinkFound == true) {
							break;
						}
						Coord linkIdFromCoord = this.network.getLinks().get(linkId).getFromNode().getCoord();   // CAUTION: Use fromNode
						for (TransitStopFacility tsf : this.transitSchedule.getFacilities().values()) {
							if (tsf.getCoord().equals(linkIdFromCoord)) {
								if(tsf.getId().equals(stopId1)) {
									accessLinkId = linkId;
									egressStopId = stopId2;
									accessLinkFound = true;
									break;
								}
								else if (tsf.getId().equals(stopId2)) {
									accessLinkId = linkId;
									egressStopId = stopId1;
									accessLinkFound = true;
									break;
								}
							}
						}
					}
					
					for (Id<Link> linkId : routeLinks.subList(routeLinks.indexOf(accessLinkId), routeLinks.size())) { // continue at toNode from link where accessStop was found at fromNode
						if (egressLinkFound == true) {
							break;
						}
						Coord linkIdToCoord = this.network.getLinks().get(linkId).getToNode().getCoord();   // CAUTION: Use toNode
						for (TransitStopFacility tsf : this.transitSchedule.getFacilities().values()) {
							if (tsf.getCoord().equals(linkIdToCoord) && tsf.getId().equals(egressStopId)) {
									egressLinkId = linkId;
									egressLinkFound = true;
									break;
							}
						}
					}

					if (accessLinkId == null || egressLinkId == null) {	// if something fails and they cannot be found
							Log.write("CAUTION: Stop facility not found. Not increasing distance value for within city for this metro access/egress");
					}
					else { // if both, access & egress, have been found
//							Log.write("Access StopFacility = "+accessStopId.toString());
//							Log.write("Egress StopFacility = "+egressStopId.toString());
//							Log.write("Access & Egress have been found: Access Link = "+accessLinkId.toString());
//							Log.write("Access & Egress have been found: Egress Link= "+egressLinkId.toString());
//							Log.write("Transit Route = "+ eventTransitRoute.getId().toString());
//							Log.write("Route Links = "+ routeLinks.toString());
					}
					List<Id<Link>> travelledLinks = routeLinks.subList(routeLinks.indexOf(accessLinkId), routeLinks.indexOf(egressLinkId)+1);
					for (Id<Link> thisTravelledLinkId : travelledLinks) {
						Link thisTravelledLink = this.network.getLinks().get(thisTravelledLinkId);
						if (GeomDistance.calculate(new Coord(2683114.0,1248092.0), thisTravelledLink.getFromNode().getCoord()) < 4400.0) {
							distance += thisTravelledLink.getLength();
							Log.write("Adding an extra inner city CountDoubleDistance="+thisTravelledLink.getLength()+" --> Total="+distance);
						}
					}
				}catch (IOException e) {e.printStackTrace();}
				// ---
				
				String personId = event.getAttributes().get("person");
				String route = event.getAttributes().get("route");
				//System.out.println("PT_Transit on Route "+ route + " --> "+ event.getAttributes().get("travelDistance") +" [m] travelled");
				Map<String, Double> routeDistances = new HashMap<String, Double>();
				if(this.travelStats.containsKey(personId)) {
					routeDistances = this.travelStats.get(personId);
					if(routeDistances.containsKey(route)) {
						double oldDistance = routeDistances.get(route);
						routeDistances.put(route, oldDistance+distance);
					}
					else {
						routeDistances.put(route, distance);
					}
				}
				else {
					routeDistances.put(route, distance);
				}
				this.travelStats.put(personId, routeDistances);
				//DELETE WHEN YOU SEE THIS: System.out.println("New Total on Route "+ route + " = "+ this.travelStats.get(personId).get(route));
				
				if (this.routeBoardingCounter.containsKey(route)) {
					this.routeBoardingCounter.put(route, this.routeBoardingCounter.get(route)+1);				
				}
				else {this.routeBoardingCounter.put(route, 1);}			
				// System.out.println("And added one boarding for "+route+" to "+this.routeBoardingCounter.get(route));
			}
		}
		// Code BEFORE 15.09.2018 for earlier attempts and additional code snippets:

	}

	
	
}