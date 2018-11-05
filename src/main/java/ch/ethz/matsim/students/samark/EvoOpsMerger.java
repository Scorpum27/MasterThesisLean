package ch.ethz.matsim.students.samark;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import com.google.common.collect.Sets;

public class EvoOpsMerger {

	
	/* MEGRER PROCEDURE
	 * > FOR crossing routes (do this for all route combinations to link all crossings to stops):
	 * 		take crossing point - check if crossing point is a stop facility
	 * 			if it is, do nothing 
	 * 			if it is not, continue as follows
	 * 				for route A check which of the two adjacent stops had higher original traffic or which is closer and set that stop as new crossing point
	 * 				take both adjacent stops of route B and connect them by means of shortest paths with the route A crossing stop
	 * > FOR routes without crossing:
	 * 		take terminal closer to city center and connect to closest stop serviced by another route by shortest path if does not exceed certain limit.
	 * 		Else, just leave as such!
	 * */
	
	public static void mergeRoutes(MNetworkPop newPopulation, Network globalNetwork, Double maxConnectingDistance,
			Map<Id<Link>, CustomMetroLinkAttributes> metroLinkAttributes, String eliteNetworkID, Double maxCrossingAngle) throws IOException {
		
		for (MNetwork mn : newPopulation.networkMap.values()) {
			if (mn.networkID.equals(eliteNetworkID)) {
				continue;
			}
			Boolean mergerOccurred = false;
			List<List<String>> crossedRoutePairs = new ArrayList<List<String>>();
			for (MRoute r1 : mn.routeMap.values()) {
				InnerRouteLoop:
				for (MRoute r2 : mn.routeMap.values()) {
					if (r1.routeID.equals(r2.routeID)) {
						continue;
					}
					for (List<String> routePair : crossedRoutePairs) {
						if (routePair.contains(r1.routeID) && routePair.contains(r2.routeID)) {
//							Log.write("0X - Combination has already been paired = [ "+r1.routeID + " x " + r2.routeID + " ]  Try next"); 
							continue InnerRouteLoop;
						}
					}
//					Log.write("01 - Do routes cross [ "+r1.routeID + " x " + r2.routeID + " ] ??");
					for (Id<Link> link1Id : r1.linkList.subList(0, r1.linkList.size()/2)) {
						for (Id<Link> link2Id : r2.linkList.subList(0, r2.linkList.size()/2)) {
							Link crossLink1 = globalNetwork.getLinks().get(link1Id);
							Link crossLink2 = globalNetwork.getLinks().get(link2Id);
//							Log.write("crossLink1 = "+ crossLink1.getId().toString() + "=" + link1Id.toString());
//							Log.write("crossLink2 = "+ crossLink2.getId().toString() + "=" + link2Id.toString());
							if ((link1Id.equals(link2Id) && EvoOpsMutator.returnAllStopFacilitiesOnLink(metroLinkAttributes, globalNetwork.getLinks().get(link1Id)).size()>0)
								|| (crossLink1.getFromNode().getId().equals(crossLink2.getToNode().getId()) && metroLinkAttributes.get(link1Id).fromNodeStopFacility != null)
								|| (crossLink1.getToNode().getId().equals(crossLink2.getFromNode().getId()) && metroLinkAttributes.get(link1Id).toNodeStopFacility != null)
									){
//								Log.write("01a - Yes, Routes do cross BY OVERLAPPING LINK/NODE with a facility [ "+r1.routeID + " x " + r2.routeID + " ]");
								mergerOccurred = true;
								List<String> newPair =  new ArrayList<String>(Arrays.asList(r1.routeID,r2.routeID));
								if ( ! crossedRoutePairs.contains(newPair)) {
									crossedRoutePairs.add(newPair);										
								}
								continue InnerRouteLoop;
							}
							if (EvoOpsMerger.linksDoCross(crossLink1, crossLink2)) {
//								Log.write("01a - Yes, Routes do cross [ "+r1.routeID + " x " + r2.routeID + " ]");
								// we know that the routes intersect. One route is modified slightly
								// so that the intersection occurs at a stop, where passengers can switch lines
								Boolean mergerSuccessful = EvoOpsMerger.linkRoutes(r1, crossLink1, r2, crossLink2, metroLinkAttributes, globalNetwork, maxCrossingAngle);
								if (mergerSuccessful) {
									mergerOccurred = true;
									List<String> newPair =  new ArrayList<String>(Arrays.asList(r1.routeID,r2.routeID));
									if ( ! crossedRoutePairs.contains(newPair)) {
										crossedRoutePairs.add(newPair);										
									}
//									Log.write("01b - Pair is def. linked min. once [ "+r1.routeID + " x " + r2.routeID + " ]");
								}
								continue InnerRouteLoop;
							}
						}
					}
				}
			}
			
//			Log.write("02 - Initial crossing complete: Crossed route pairs are = "+crossedRoutePairs.toString() + "-------------------------------");
			
			// check for all routes if they have been part of at least one merger pair. If not, try to connect their endings.
			// XXX Or connect their endings anyways!
			
//			Log.write("crossedRoutePairs = "+crossedRoutePairs);
			for (MRoute mr : mn.routeMap.values()) {
				Boolean routeIsConnected = false;
				for (List<String> routePair : crossedRoutePairs) {
					if (routePair.contains(mr.routeID)) {
						routeIsConnected = true;
						break;
					}
				}
				if (routeIsConnected.equals(false)) {
					// can add a cost penalty here for that mRoute (make new field in mRoute)
					// try to connect one of the route terminals
					connectRouteToNetwork(mr, mn, maxConnectingDistance, globalNetwork, metroLinkAttributes, maxCrossingAngle);
				}
				else {
					// can also connect terminals of connected routes without big effort (smaller distance, 400m, and smaller angles, 90°, only!)
					connectRouteToNetwork(mr, mn, 500.0, globalNetwork, metroLinkAttributes, 90.0);
				}
			}
			
			// after having tried to connect single routes, check if several networks / routes stay unconnected
			// if they still are, try to merge them by closest link...
			List<List<String>> autonomousMetroSubnetworks = new ArrayList<List<String>>();
			routesLoop:
			for (MRoute mr : mn.routeMap.values()) {
				for (List<String> subnetwork : autonomousMetroSubnetworks) {
					if (subnetwork.contains(mr.routeID)) {
						continue routesLoop;	// is already in an autonomous subnetwork
					}
				}
				// if the route has not been identified in a subnetwork,
				// make a new subnetwork with this unassigned route and add all routes it has a connection to (digging search algorithm)!
				List<String> initialSubnetworkList = new ArrayList<String>(Arrays.asList(mr.routeID));
				List<String> newSubnetwork = getAllConnectedRoutes(initialSubnetworkList, crossedRoutePairs);
				autonomousMetroSubnetworks.add(newSubnetwork);
			}
			// Display here how many autonomous subnetworks there are
			Log.write("The routes of "+mn.networkID+" form #independentNetworks=" + autonomousMetroSubnetworks.size());
			
			// XXX May try to connect these subnetworks by closest distance link: Possible approach:
				// for all individual mroutes of one network, apply them to connectRouteToNetwork(mr, mn, maxConnectingDistance, globalNetwork, metroLinkAttributes, maxCrossingAngle);
				// CAUTION: mn must be only the other mroutes, not all of them including those of this subnetwork!
			
			
			if (mergerOccurred && !(newPopulation.modifiedNetworksInLastEvolution.contains(mn.networkID))) {
				newPopulation.modifiedNetworksInLastEvolution.add(mn.networkID);
			}
			
		}
		
	}
	
	public static void connectRouteToNetwork(MRoute mr, MNetwork mn, Double maxConnectingDistance, Network globalNetwork,
			Map<Id<Link>, CustomMetroLinkAttributes> metroLinkAttributes, Double maxCrossingAngle) throws IOException {
		// get all transitStopFacilities that the other routes cover --> find closest one that can connect to within reach and with maxTurningAngle
		List<TransitStopFacility> possibleIntersectFacilities = new ArrayList<TransitStopFacility>();
		for (String routeId : mn.routeMap.keySet()) {
			MRoute otherRoute = null;
			if (routeId.equals(mr.routeID)) {
				continue;
			}
			else {
				otherRoute = mn.routeMap.get(routeId);
			}
			for (Id<Link> routeLinkId : otherRoute.linkList) {
				List<TransitStopFacility> thisLinkStopFacilities =
						EvoOpsMutator.returnAllStopFacilitiesOnLink(metroLinkAttributes, globalNetwork.getLinks().get(routeLinkId));
				for (TransitStopFacility tsf : thisLinkStopFacilities) {
					if ( ! possibleIntersectFacilities.contains(tsf)) {
						possibleIntersectFacilities.add(tsf);
//						Log.write("Adding possible intersect facility: "+tsf.getName());
					}
				}
			}
		}
//		List<TransitStopFacility> unfeasibleIntersectFacilities = new ArrayList<TransitStopFacility>();
		Link routeStartTerminal = globalNetwork.getLinks().get(mr.linkList.get(0));
		Link routeEndTerminal = globalNetwork.getLinks().get(mr.linkList.get(mr.linkList.size()-1));
		Boolean connectionLinkFound = false;
		while (connectionLinkFound.equals(false)) {
			TransitStopFacility closestFacility = null;
			Double distance;
			Double closestFacilityDistance = Double.MAX_VALUE;
			for (TransitStopFacility tsf : possibleIntersectFacilities) {
				distance = GeomDistance.calculate(tsf.getCoord(), routeStartTerminal.getFromNode().getCoord());
				if (distance < closestFacilityDistance) {
					closestFacility = tsf;
					closestFacilityDistance = distance;
				}
				distance = GeomDistance.calculate(tsf.getCoord(), routeEndTerminal.getToNode().getCoord());
				if (distance < closestFacilityDistance) {
					closestFacility = tsf;
					closestFacilityDistance = distance;
				}
			}
			if (closestFacility == null) {
//				Log.write("No feasible connection points (with facilities) identified. Not connecting unattached route "+mr.routeID);
				return;
			}
//			Log.write("The closest feasible stopFacilityDistance="+closestFacilityDistance+ " (stop="+closestFacility.getName()+")");
			if (closestFacilityDistance > maxConnectingDistance) {
//				Log.write("Distance of closest feasible stop is too high for reasonable connecting. Will therefore not connect unattached route="+mr.routeID);
				return;
			}
			Node connectingNode = NetworkOperators.getNodeOfFacility(closestFacility, globalNetwork);
//			Log.write("Trying to connect a close facility "+closestFacility.getName());
			if (GeomDistance.calculate(closestFacility.getCoord(), routeEndTerminal.getToNode().getCoord()) 
				 < GeomDistance.calculate(closestFacility.getCoord(), routeStartTerminal.getFromNode().getCoord()) ){
//				Log.write(" ... to end terminal facility "+metroLinkAttributes.get(routeEndTerminal.getId()).toNodeStopFacility.getName());
				List<Node> connectingNodeList = DemoDijkstra.calculateShortestPath(globalNetwork, routeEndTerminal.getToNode().getId(), connectingNode.getId());
				if (connectingNodeList.size() == 0) {
					possibleIntersectFacilities.remove(closestFacility);
//					Log.write(" --> No shortest path. Removing from possible facilities "+closestFacility.getName());
					continue;
				}
				List<Id<Link>> connectingLinkList = NetworkEvolutionImpl.nodeListToNetworkLinkList(globalNetwork, connectingNodeList);
				List<Id<Link>> newRouteLinkList = new ArrayList<Id<Link>>();
				newRouteLinkList.addAll(mr.linkList.subList(0, mr.linkList.size()/2));
				newRouteLinkList.addAll(connectingLinkList);
				if (EvoOpsMutator.checkIfTurningAnglesOkIdOnly(maxCrossingAngle, newRouteLinkList, globalNetwork)) {
					connectionLinkFound = true;
					newRouteLinkList.addAll(NetworkEvolutionImpl.OppositeLinkListOf(newRouteLinkList));
					mr.linkList = newRouteLinkList;
					mr.networkRoute = RouteUtils.createNetworkRoute(mr.linkList, globalNetwork);
					Log.write("Successfully attached possibly autonomous route="+mr.routeID.toString() + " to facility="+closestFacility.getName());
					Integer generationNr = 1;
					String historyFileLocation = "zurich_1pm/Evolution/Population/HistoryLog/Generation"+(generationNr)+"/MRoutes";
					NetworkEvolutionImpl.MRouteToNetwork(mr, globalNetwork,  Sets.newHashSet("pt"), historyFileLocation+"/"+mr.routeID+"_NetworkFileAttached.xml");
					break;
				}
				else {
					//unfeasibleIntersectFacilities.add(closestFacility);
					possibleIntersectFacilities.remove(closestFacility);
					Log.write(possibleIntersectFacilities.toString());
					Log.write(closestFacility.toString());
//					Log.write(" --> Turning angles not ok. Removing from possible facilities "+closestFacility.getName());
					continue;
				}
			}
			else {
//				Log.write(" ... to start terminal facility "+metroLinkAttributes.get(routeStartTerminal.getId()).fromNodeStopFacility.getName());
				List<Node> connectingNodeList = DemoDijkstra.calculateShortestPath(globalNetwork, connectingNode.getId(), routeStartTerminal.getFromNode().getId());
				if (connectingNodeList.size() == 0) {
					possibleIntersectFacilities.remove(closestFacility);
//					Log.write(" --> No shortest path. Removing from possible facilities "+closestFacility.getName());
					continue;
				}
				List<Id<Link>> connectingLinkList = NetworkEvolutionImpl.nodeListToNetworkLinkList(globalNetwork, connectingNodeList);
				List<Id<Link>> newRouteLinkList = new ArrayList<Id<Link>>();
				newRouteLinkList.addAll(connectingLinkList);
				newRouteLinkList.addAll(mr.linkList.subList(0, mr.linkList.size()/2));
				if (EvoOpsMutator.checkIfTurningAnglesOkIdOnly(maxCrossingAngle, newRouteLinkList, globalNetwork)) {
					connectionLinkFound = true;
					newRouteLinkList.addAll(NetworkEvolutionImpl.OppositeLinkListOf(newRouteLinkList));
					mr.linkList = newRouteLinkList;
					mr.networkRoute = RouteUtils.createNetworkRoute(mr.linkList, globalNetwork);
//					Log.write("Successfully attached possibly autonomous route="+mr.routeID.toString() + " to facility="+closestFacility.getName());
					Log.write("Successfully attached possibly autonomous route="+mr.routeID.toString());
					Integer generationNr = 1;
					String historyFileLocation = "zurich_1pm/Evolution/Population/HistoryLog/Generation"+(generationNr)+"/MRoutes";
					NetworkEvolutionImpl.MRouteToNetwork(mr, globalNetwork,  Sets.newHashSet("pt"), historyFileLocation+"/"+mr.routeID+"_NetworkFileAttached.xml");
					break;
				}
				else {
					//unfeasibleIntersectFacilities.add(closestFacility);
//					Log.write(" --> Turning angles not ok. Removing from possible facilities "+closestFacility.getName());
					Log.write(closestFacility.toString());
					Log.write(possibleIntersectFacilities.toString());
					possibleIntersectFacilities.remove(closestFacility);
					continue;
				}
			}
		}
		
	}

	public static List<String> getAllConnectedRoutes(List<String> connectedRoutes, List<List<String>> crossedRoutePairs){
		List<String> newConnections = getAllNewConnections(connectedRoutes, crossedRoutePairs);
		if (newConnections.size()==0) {
			return connectedRoutes;
		}
		else {
			connectedRoutes.addAll(newConnections);
			return getAllConnectedRoutes(connectedRoutes, crossedRoutePairs);
		}
	}
	
	public static List<String> getAllNewConnections(List<String> connectedRoutes, List<List<String>> crossedRoutePairs) {
		List<String> newConnections = new ArrayList<String>();
		for (String connectedRoute : connectedRoutes) {
			for (List<String> routePair : crossedRoutePairs) {
				if (routePair.contains(connectedRoute)) {
					if ( ! connectedRoutes.contains(routePair.get(0))) {
						newConnections.add(routePair.get(0));
					}
					if ( ! connectedRoutes.contains(routePair.get(1))) {
						newConnections.add(routePair.get(1));
					}
				}
			}
		}
		return newConnections;
	}
	
	

	public static Boolean linkRoutes(MRoute r1, Link link1, MRoute r2, Link link2, Map<Id<Link>, CustomMetroLinkAttributes> metroLinkAttributes,
			Network globalNetwork, Double maxCrossingAngle) throws IOException {
		Node crossingTSFNode = null;
//		Log.write(" 01-1 Now trying to actually link routes [ "+r1.routeID + " x " + r2.routeID + " ]");
		// check if the routes have a common stop already. If they do, we can consider this route as well-merged already
		for (TransitStopFacility r1intersectLinkTSF : EvoOpsMutator.returnAllStopFacilitiesOnLink(metroLinkAttributes, link1)) {
			for (TransitStopFacility r2intersectLinkTSF : EvoOpsMutator.returnAllStopFacilitiesOnLink(metroLinkAttributes, link2)) {
				if (r1intersectLinkTSF.getId().equals(r2intersectLinkTSF.getId())) {
					crossingTSFNode = NetworkOperators.getNodeOfFacility(r1intersectLinkTSF, globalNetwork);
//					Log.write(" 01-1a Already existing common facility = [ "+ r1intersectLinkTSF.getName() + " ]");
					
					// no merger modification required, but unblock any blocked facilityLinks so that detected intersection has functionality
					for (Id<Link> linkId : r1.linkList) {
						Link link = globalNetwork.getLinks().get(linkId);
						if (link.getFromNode().getId().equals(crossingTSFNode.getId()) || link.getToNode().getId().equals(crossingTSFNode.getId())) {
							if (r1.facilityBlockedLinks.contains(link.getId())) {
								r1.facilityBlockedLinks.remove(link.getId());
							}
						}
					}
					for (Id<Link> linkId : r2.linkList) {
						Link link = globalNetwork.getLinks().get(linkId);
						if (link.getFromNode().getId().equals(crossingTSFNode.getId()) || link.getToNode().getId().equals(crossingTSFNode.getId())) {
							if (r2.facilityBlockedLinks.contains(link.getId())) {
								r2.facilityBlockedLinks.remove(link.getId());
							}
						}
					}
					// return true because may have unblocked links and because we want to mark r1/r2 as a successfully intersecting route pair in caller method!
//					Log.write(" 01-2a Routes already have a common Facility [ "+r1.routeID + " x " + r2.routeID + " ] ... No merger modification required!");
					return true;
				}
			}
		}
		
//		Log.write(" 01-2b Routes DO NOT have a common Facility. Will be attempting modifications.");
		
		// if they to not have a common link already, we modify route 2 so that it can be merged to route 1 by generating intersection points directly on a stop
		List<Id<Link>> r1linkList = r1.linkList.subList(0, r1.linkList.size()/2); // half the link list here
		List<Id<Link>> r2linkList = r2.linkList.subList(0, r2.linkList.size()/2); // half the link list here
		TransitStopFacility r1crossingTSF = null;
//		Log.write("Route 1 (oneway): = "+ r1linkList.toString());
//		Log.write("Route 2 (oneway): = "+ r2linkList.toString());
		
//		Node r1crossingNode = null;
		TransitStopFacility r1nextTSFup = null;
		TransitStopFacility r1nextTSFdown = null;
		// if crossingLink of route1 has a facility already, take that as crossing link and crossing facility
		TransitStopFacility thisIntersectionLinkFacilityR1 = Metro_TransitScheduleImpl.selectStopFacilityOnLink(metroLinkAttributes, link1, null);
		if (thisIntersectionLinkFacilityR1 != null) {
			r1crossingTSF = thisIntersectionLinkFacilityR1;
//			Log.write("Route1 already has facility on crossing link = "+thisIntersectionLinkFacilityR1.getName());
		}
		else {
			// find next link with facility
			for (Id<Link> routeLinkIdUP : r1linkList.subList(Math.min(r1linkList.size(),r1linkList.indexOf(link1.getId())+1), r1linkList.size())) {
				Link routeLinkUP = globalNetwork.getLinks().get(routeLinkIdUP);
				if (GeomDistance.betweenNodes(link1.getToNode(), routeLinkUP.getFromNode()) > 2000.0) {	// break if too much of a detour to connect stops
//					Log.write("X - Route1 did not find close upstream facility. No linking possible. Proceeding to next route...");
					break;
				}
				TransitStopFacility thisRouteLinkFacility = Metro_TransitScheduleImpl.selectStopFacilityOnLink(metroLinkAttributes, routeLinkUP, null);
				if (thisRouteLinkFacility != null) {
					r1nextTSFup = thisRouteLinkFacility;
//					Log.write("Route1 found close upstream facility = "+r1nextTSFup.getName());
					break;
				}
			}
			// do the same for the other direction. CAUTION: REVERSE link list this time
			for (Id<Link> routeLinkIdDOWN : NetworkEvolutionImpl.OppositeLinkListOf(r1linkList.subList(0, Math.max(0,r1linkList.indexOf(link1.getId()))))) {
				Link routeLinkDOWN = globalNetwork.getLinks().get(routeLinkIdDOWN);
				if (GeomDistance.betweenNodes(link1.getFromNode(), routeLinkDOWN.getFromNode()) > 2000.0) {	// break if too much of a detour to connect stops
//					Log.write("X - Route1 did not find close downstream facility. No linking possible. Proceeding to next route...");
					break;
				}
				TransitStopFacility thisRouteLinkFacility = Metro_TransitScheduleImpl.selectStopFacilityOnLink(metroLinkAttributes, routeLinkDOWN, null);
				if (thisRouteLinkFacility != null) {
					r1nextTSFdown = thisRouteLinkFacility;
//					Log.write("Route1 found close downstream facility = "+r1nextTSFdown.getName());
					break;
				}
			}
			// choose between both sides, which facility is closer to crossing or which one has higher original traffic (choose closest one for now)
			if (r1nextTSFup != null && r1nextTSFdown == null) {
				r1crossingTSF = r1nextTSFup;
			}
			else if (r1nextTSFdown != null && r1nextTSFup == null) {
				r1crossingTSF = r1nextTSFdown;
			}
			else if (r1nextTSFdown != null && r1nextTSFup != null ) {
				if (GeomDistance.calculate(link1.getToNode().getCoord(), r1nextTSFup.getCoord())
						< GeomDistance.calculate(link1.getFromNode().getCoord(), r1nextTSFdown.getCoord())) {
					r1crossingTSF = r1nextTSFup;
				}
				else {
					r1crossingTSF = r1nextTSFdown;
				}
			}
			else {
				return false;	// linking not reasonable due to no available TSFs within close enough range of intersection
			}
//			Log.write("Route1 final selected r1crossingTSF = "+r1crossingTSF.getName());

		}		
		// take both adjacent stops of route B and connect them by means of shortest paths with the route A crossing stop(=r1crossingTSF)
		// may try all in/outbound links from the crossing facility to facilitate the merging
		crossingTSFNode = NetworkOperators.getNodeOfFacility(r1crossingTSF, globalNetwork);
//		Log.write("Route1 r1crossingTSFNode = "+crossingTSFNode.getId().toString());
		if (crossingTSFNode == null) {
			Log.write("Suddenly, crossingTSFNode = null");
			return false;
		}
		Id<Link> r2nextFacilityLinkUP;
		Id<Link> r2nextFacilityLinkDOWN;
		Integer attemptedStopsCounterUpstream = 0;
		Integer attemptedStopsCounterDownstream = 0;
		List<Id<Link>> mergerLinkListDOWN = null;
		List<Id<Link>> mergerLinkListUP = null;

		for (Id<Link> routeLinkIdUP : r2linkList.subList(r2linkList.indexOf(link2.getId()), r2linkList.size())) {
//			Log.write("Trying next route link UP = "+routeLinkIdUP.toString());
			Link routeLinkUP = globalNetwork.getLinks().get(routeLinkIdUP);
			if (metroLinkAttributes.get(routeLinkUP.getId()).toNodeStopFacility != null) {
//				Log.write("route link UP has a facility = "+routeLinkIdUP.toString());
				attemptedStopsCounterUpstream++;
				List<Node> nodeList = null;
				nodeList = DemoDijkstra.calculateShortestPath(globalNetwork, crossingTSFNode.getId(), routeLinkUP.getToNode().getId());
				if (nodeList.size() > 0) {
					r2nextFacilityLinkUP = routeLinkUP.getId();
					mergerLinkListUP = NetworkEvolutionImpl.nodeListToNetworkLinkList(globalNetwork, nodeList);	
//					Log.write("Route 2: Found a shortest path UP within reach: " + mergerLinkListUP.toString());
					mergerLinkListUP.addAll(r2linkList.subList(Math.min(r2linkList.indexOf(r2nextFacilityLinkUP)+1,r2linkList.size()), r2linkList.size()));
				}
				else {
//					Log.write("Route 2: Did not find a shortest path UP within reach");
					continue;
				}
				if (EvoOpsMutator.checkIfTurningAnglesOkIdOnly(maxCrossingAngle, mergerLinkListUP, globalNetwork)) {
					//r2nextTSFup = metroLinkAttributes.get(routeLinkUP.getId()).toNodeStopFacility;
					break;
				}
				if (attemptedStopsCounterUpstream==2) {
//					Log.write("Too far upstream. No merger possible. Proceeding to next route.");
					return false;
				}
			}
		}
		List<Id<Link>> r2linkListRearranged = flipLinkList(r2linkList);
		for (Id<Link> routeLinkIdDOWN : r2linkListRearranged.subList(r2linkListRearranged.indexOf(link2.getId()), r2linkListRearranged.size())) {
//			Log.write("Trying next route link DOWN = "+routeLinkIdDOWN.toString());
			Link routeLinkDOWN = globalNetwork.getLinks().get(routeLinkIdDOWN);
			if (metroLinkAttributes.get(routeLinkDOWN.getId()).fromNodeStopFacility != null) {
//				Log.write("route link DOWN has a facility = "+routeLinkIdDOWN.toString());
				attemptedStopsCounterDownstream++;
				List<Node> nodeList = null;
				nodeList = DemoDijkstra.calculateShortestPath(globalNetwork, routeLinkDOWN.getFromNode().getId(), crossingTSFNode.getId());
				if (nodeList.size() > 1) {
					r2nextFacilityLinkDOWN = routeLinkDOWN.getId();
					mergerLinkListDOWN = NetworkEvolutionImpl.nodeListToNetworkLinkList(globalNetwork, nodeList);
//					Log.write("Route 2: Found a shortest path DOWN within reach: " + mergerLinkListDOWN.toString());
					mergerLinkListDOWN.addAll(0, r2linkList.subList(0, r2linkList.indexOf(r2nextFacilityLinkDOWN)));
				}
				else {
//					Log.write("Route 2: Did not find a shortest path DOWN within reach");
					continue;
				}
				if (EvoOpsMutator.checkIfTurningAnglesOkIdOnly(maxCrossingAngle, mergerLinkListDOWN, globalNetwork)) {
					break;
				}
				if (attemptedStopsCounterDownstream==2) {
//					Log.write("Too far downstream. No merger possible. Proceeding to next route.");
					return false;
				}
			}
		}
		
		// add up the two route parts adding up to a rerouted line intersecting route 1 at a mutual stopFacility!
		List<Id<Link>> reroutedLinkList = new ArrayList<Id<Link>>();
		reroutedLinkList.addAll(mergerLinkListDOWN);
		reroutedLinkList.addAll(mergerLinkListUP);
		// check again entire route for turning constraint. This also avoids strange shortest paths along original line with U-turns!
		if (!EvoOpsMutator.checkIfTurningAnglesOkIdOnly(maxCrossingAngle, reroutedLinkList, globalNetwork)) {
			return false;
		}
		reroutedLinkList.addAll(NetworkEvolutionImpl.OppositeLinkListOf(reroutedLinkList));
		// save new linkLists with adding opposite and then updating mRoute's networkRoute
//		Log.write("Old linkList = "+r2.linkList.subList(0, r2.linkList.size()/2).toString());
//		Log.write("MergerLinkList DOWN = "+mergerLinkListDOWN.toString());
//		Log.write("MergerLinkList UP = "+mergerLinkListUP.toString());
		r2.linkList = reroutedLinkList;
		r2.networkRoute = RouteUtils.createNetworkRoute(r2.linkList, globalNetwork);
//		Log.write("New linkList = "+r2.linkList.subList(0, r2.linkList.size()/2).toString());
		Integer generationNr = 1;
		String historyFileLocation = "zurich_1pm/Evolution/Population/HistoryLog/Generation"+(generationNr)+"/MRoutes";
		NetworkEvolutionImpl.MRouteToNetwork(r2, globalNetwork,  Sets.newHashSet("pt"), historyFileLocation+"/"+r2.routeID+"_NetworkFileMergedTest.xml");
		// unblock any blocked links around the intersection for unchanged r1
		// (a blocked link would not permit a stop on that link and therefore make the intersection obsolete)
		for (Id<Link> linkId : r1.linkList) {
			Link link = globalNetwork.getLinks().get(linkId);
			if (link.getFromNode().getId().equals(crossingTSFNode.getId()) || link.getToNode().getId().equals(crossingTSFNode.getId())) {
				if (r1.facilityBlockedLinks.contains(link.getId())) {
					r1.facilityBlockedLinks.remove(link.getId());
				}
			}
		}
		Log.write(" Routes intersection modification successful: [ "+r1.routeID + " x " + r2.routeID + " ]");
		return true;
	}

	public static List<Id<Link>> flipLinkList(List<Id<Link>> linkList) {
		List<Id<Link>> flippedLinkList = new ArrayList<Id<Link>>();
		for (Id<Link> linkId : linkList) {
			flippedLinkList.add(0, linkId);
		}
		return flippedLinkList;
	}



	public static boolean linksDoCross(Link link1, Link link2) throws IOException {
		// if the links are the same or reverse ones, crossing is inherently given
		if (link1.getId().equals(link2.getId()) || link1.getId().equals(NetworkEvolutionImpl.ReverseLink(link2.getId()))) {
			return true;
		}
		double x1A = link1.getFromNode().getCoord().getX();
		double y1A = link1.getFromNode().getCoord().getY();
		double x1B = link1.getToNode().getCoord().getX();
		double y1B = link1.getToNode().getCoord().getY();
		double x2A = link2.getFromNode().getCoord().getX();
		double y2A = link2.getFromNode().getCoord().getY();
		double x2B = link2.getToNode().getCoord().getX();
		double y2B = link2.getToNode().getCoord().getY();
		double q1;
		double m1;
		double q2;
		double m2;
		double xCross;
		double yCross;
		
		// linear functions for links
		if (x1A == x1B) {
			m1 = Double.POSITIVE_INFINITY;
			q1 = Double.POSITIVE_INFINITY;
		}
		else {
			m1 = (y1A-y1B) / (x1A-x1B);
			q1 = (x1A*y1B-x1B*y1A) / (x1A-x1B);
		}
		if (x2A == x2B) {
			m2 = Double.POSITIVE_INFINITY;
			q2 = Double.POSITIVE_INFINITY;
		}
		else {
			m2 = (y2A-y2B) / (x2A-x2B);
			q2 = (x2A*y2B-x2B*y2A) / (x2A-x2B);
		}
		// if parallel, they do not intersect. For the unlikely case that they coincide,
		// we can assume that the neighboring links of the routes are at an angle to each other so that they will yield non-zero angle intersection  
		if (m1 == m2) {
			return false;
		}
		// mathematical exception case for one vertical link
		if (m1 == Double.POSITIVE_INFINITY) {
			xCross = x1A;
			yCross = m2*xCross + q2;
		}
		else if (m2 == Double.POSITIVE_INFINITY) {
			xCross = x2A;
			yCross = m1*xCross + q1;
		}
		else {
			xCross = (q2-q1) / (m1-m2);
			yCross = (m1*q2-m2*q1) / (m1-m2);
		}
		
		if (((x1A-1 <= xCross && xCross <= x1B+1) || (x1B-1 <= xCross && xCross <= x1A+1)) && ((x2A-1 <= xCross && xCross <= x2B+1) || (x2B-1 <= xCross && xCross <= x2A+1)) &&
				((y1A-1 <= yCross && yCross <= y1B+1) || (y1B-1 <= yCross && yCross <= y1A+1)) && ((y2A-1 <= yCross && yCross <= y2B+1) || (y2B-1 <= yCross && yCross <= y2A+1))) {						
//			Log.write("   x1A = "+x1A);
//			Log.write("   x1B = "+x1B);
//			Log.write("   x2A = "+x2A);
//			Log.write("   x2B = "+x2B);
//			Log.write("   xCross = "+xCross);
//			Log.write("   y1A = "+y1A);
//			Log.write("   y1B = "+y1B);
//			Log.write("   y2A = "+y2A);
//			Log.write("   y2B = "+y2B);
//			Log.write("   yCross = "+yCross);
//			Log.write("   Returning TRUE= CROSS");
			return true;
		}
		else {
//			Log.write("   Returning FALSE= NO cross");
			return false;
		}
	}

	
	
	
	
	
	@Deprecated
	public static boolean linksDoCrossOld(Link link1, Link link2) {
		// if the links are the same or reverse ones, crossing is inherently given
		if (link1.getId().equals(link2.getId()) || link1.getId().equals(NetworkEvolutionImpl.ReverseLink(link2.getId()))) {
			return true;
		}
		double x1A = link1.getFromNode().getCoord().getX();
		double y1A = link1.getFromNode().getCoord().getY();
		double x1B = link1.getToNode().getCoord().getX();
		double y1B = link1.getToNode().getCoord().getY();
		double x2A = link2.getFromNode().getCoord().getX();
		double y2A = link2.getFromNode().getCoord().getY();
		double x2B = link2.getToNode().getCoord().getX();
		double y2B = link2.getToNode().getCoord().getY();
		double q1;
		double m1;
		double q2;
		double m2;
//		if (GeomDistance.angleBetweenLinks(link1, link2) == 0.0 || GeomDistance.angleBetweenLinks(link1, link2) == 180.0) {
//			if ()
//		}
		// special section for parallel horizontal/vertical links
		if (x1A == x1B) {	// vertical link1
			if (x2A == x2B) { // parallel vertical links
				if (x2A == x1A) {	// links coincide --> must find out if the have overlap (Check both directions !!)
					if ((y2A <= y1A && y1A <= y2B) || (y2B <= y1A && y1A <= y2A) || (y2A <= y1B && y1B <= y2B) || (y2B <= y1B && y1B <= y2A) ||
							(y1A <= y2A && y2A <= y1B) || (y1B <= y2A && y2A <= y1A) || (y1A <= y2B && y2B <= y1B) || (y1B <= y2B && y2B <= y1A)) {						
						return true;
					}
					else {
						return false;
					}
				}
				else {
					return false;	// parallel. but no overlap --> don't touch!
				}
			}
		}
		if (y1A == y1B) {	// horizontal link1
			if (y2A == y2B) { // parallel horizontal links
				if (y2A == y1A) {	// links coincide --> must find out if the have overlap (Check both directions !!)
					if ((x2A <= x1A && x1A <= x2B) || (x2B <= x1A && x1A <= x2A) || (x2A <= x1B && x1B <= x2B) || (x2B <= x1B && x1B <= x2A) ||
							(x1A <= x2A && x2A <= x1B) || (x1B <= x2A && x2A <= x1A) || (x1A <= x2B && x2B <= x1B) || (x1B <= x2B && x2B <= x1A)) {						
						return true;
					}
					else {
						return false;
					}
				}
				else {
					return false;	// parallel. but no overlap --> don't touch!
				}
			}
		}
		if (x1A == x1B) {
			m1 = Double.POSITIVE_INFINITY;
			q1 = Double.POSITIVE_INFINITY;
		}
		else {
			m1 = (y1A-y1B) / (x1A-x1B);
			q1 = (x1A*y1B-x1B*y1A) / (x1A-x1B);
		}
		if (x2A == x2B) {
			m2 = Double.POSITIVE_INFINITY;
			q2 = Double.POSITIVE_INFINITY;
		}
		else {
			m2 = (y2A-y2B) / (x2A-x2B);
			q2 = (x2A*y2B-x2B*y2A) / (x2A-x2B);
		}
		// mathematical exception case for one vertical link
		
		// exception for parallel NON-horizontal/NON-vertical links
		if (m1 == m2) {
			if (q1 == q2) {
				return true;
			}
		}
		double xCross = (q2-q1) / (m1-m2);
		double yCross = (m1*q2-m2*q1) / (m1-m2);
		if ((x2A <= x1A && x1A <= x2B) && (x2B <= x1A && x1A <= x2A) && (x2A <= x1B && x1B <= x2B) && (x2B <= x1B && x1B <= x2A) &&
				(x1A <= x2A && x2A <= x1B) && (x1B <= x2A && x2A <= x1A) && (x1A <= x2B && x2B <= x1B) && (x1B <= x2B && x2B <= x1A)) {						
			return true;
		}
		else {
			return false;
		}
	}

	
	
	
	
}
