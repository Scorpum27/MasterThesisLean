package ch.ethz.matsim.students.samark;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class NetworkMutationImpl {

	public static void applyBigChange(Map<String, CustomStop> allMetroStops, List<Id<Link>> linkListMutate, Network globalNetwork, double maxCrossingAngle, MRoute mRoute) throws IOException {

		boolean feasibleCutLinkFound = false;
		Id<Link> openCutLinkId;
		Link openCutLink;
		Link linkBeforeCutLink;
		Link nextAfterOpenLink;
		Id<Link> connectingLinkId;
		Link connectingLink;
		Link connectingLinkReverse = null;
		List<Id<Link>> linkListMutateComplete = new ArrayList<Id<Link>>();
		
		do {
			// choose randomly where (at) which link route shall be cut open  
				// -2 because we don't choose start or end node for insertion
				// +1 because we start from second node, and not from start node
			Random r = new Random();
			openCutLinkId = linkListMutate.get(r.nextInt(linkListMutate.size()-2)+1); 
			openCutLink = globalNetwork.getLinks().get(openCutLinkId);
			// check all outLinks from open cut from node if we can insert a node there with our constraints
			for (Link outLinkFrom : openCutLink.getFromNode().getOutLinks().values()) {
				linkBeforeCutLink = globalNetwork.getLinks().get(linkListMutate.get(linkListMutate.indexOf(openCutLinkId)-1));
				if(outLinkFrom.equals(openCutLink) == false  &&  GeomDistance.angleBetweenLinks(linkBeforeCutLink, outLinkFrom) < maxCrossingAngle) {
					// do this to check if new node can also be connected to second part of initial route (linkList)
					for (Id<Link> nextAfterOpenLinkId : linkListMutate.subList((linkListMutate.indexOf(openCutLinkId)+1), linkListMutate.size())) {
						nextAfterOpenLink = globalNetwork.getLinks().get(nextAfterOpenLinkId);
						// create connecting Link (check if already exists)
						connectingLinkId = Id.createLinkId(outLinkFrom.getToNode().getId().toString()+"_"+nextAfterOpenLink.getFromNode().getId().toString());
						if(globalNetwork.getLinks().containsKey(connectingLinkId)==false) {
							connectingLink = globalNetwork.getFactory().createLink(connectingLinkId, outLinkFrom.getToNode(), nextAfterOpenLink.getFromNode());
							connectingLinkReverse = globalNetwork.getFactory().createLink(NetworkEvolutionImpl.ReverseLink(connectingLinkId), nextAfterOpenLink.getFromNode(), outLinkFrom.getToNode());  
						}
						else {
							connectingLink = globalNetwork.getLinks().get(connectingLinkId);
						}
						if(GeomDistance.angleBetweenLinks(connectingLink, nextAfterOpenLink) < maxCrossingAngle) {	// can make this condition harder!!
							feasibleCutLinkFound = true;
							Log.write("   >> Modifying route by node insertion at link = "+openCutLinkId.toString());
							linkListMutateComplete.addAll(linkListMutate.subList(0, linkListMutate.indexOf(openCutLinkId)));
							linkListMutateComplete.add(outLinkFrom.getId());
							linkListMutateComplete.add(connectingLinkId);
							linkListMutateComplete.addAll(linkListMutate.subList(linkListMutate.indexOf(nextAfterOpenLinkId), linkListMutate.size()));
							if(globalNetwork.getLinks().containsKey(connectingLinkId)==false) {
								globalNetwork.addLink(connectingLink);
								globalNetwork.addLink(connectingLinkReverse);
							}
							break;
						}
					}
					if (feasibleCutLinkFound == true) {
						break;
					}
				}
				else {
					continue;
				}
			}
		} while(feasibleCutLinkFound == false);
		if (linkListMutateComplete.size() == 0) {
			Log.write("ERROR: Mutated Route has Size=0! --> CHECK CODE ...");
		}
		linkListMutateComplete.addAll(NetworkEvolutionImpl.OppositeLinkListOf(linkListMutateComplete));
		mRoute.linkList = Clone.list(linkListMutateComplete);
		mRoute.networkRoute = RouteUtils.createNetworkRoute(linkListMutateComplete, globalNetwork);
	}

	
	public static boolean applySmallChange(Iterator<Entry<String, MRoute>> mrouteIter, List<Id<Link>> linkListMutate, Network globalNetwork,
			double maxCrossingAngle, MRoute mRoute, double averageRouletteScore,
			Map<Id<Link>, CustomMetroLinkAttributes> metroLinkAttributes, MNetwork mNetwork) throws IOException {
		double weakeningFactor = 0.8; // factor to weaken dominant routes, which would just extend and extend (maybe make here like network score with exp function)
		double thisRouletteScore = mRoute.personMetroKM/mRoute.drivenKM;
		double pExtend = weakeningFactor*thisRouletteScore / (weakeningFactor*thisRouletteScore + averageRouletteScore);
		Random rExt = new Random();
		double rExtDouble = rExt.nextDouble();
		if (rExtDouble < pExtend) { // extend route // TODO this should be done with better condition e.g. abs. profitability instead of rel. performance!
			extendRoute(mrouteIter, linkListMutate, globalNetwork, maxCrossingAngle, mRoute, averageRouletteScore, metroLinkAttributes, mNetwork);
		}
		else { // shorten route
			shortenRoute(mrouteIter, linkListMutate, globalNetwork, maxCrossingAngle, mRoute, averageRouletteScore, metroLinkAttributes, mNetwork);
		}
		if (linkListMutate.size() < 2) {
			Log.write("CAUTION: RouteLength = " + linkListMutate.size() + " --> Deleting "+mRoute.routeID);
			mrouteIter.remove();
			return false;
		}
		linkListMutate.addAll(NetworkEvolutionImpl.OppositeLinkListOf(linkListMutate));
		mRoute.linkList = Clone.list(linkListMutate);
		mRoute.networkRoute = RouteUtils.createNetworkRoute(linkListMutate, globalNetwork);
		return true;
	}
	
	public static void shortenRoute(Iterator<Entry<String, MRoute>> mrouteIter, List<Id<Link>> linkListMutate, Network globalNetwork, 
			double maxCrossingAngle, MRoute mRoute, double averageRouletteScore,
			Map<Id<Link>, CustomMetroLinkAttributes> metroLinkAttributes, MNetwork mNetwork) throws IOException {
		Random rEnd = new Random();
		if(rEnd.nextDouble() < 0.5) { // shorten on start link
			Id<Link> nextLinkWithFacility = null;
			for (Id<Link> linkId : linkListMutate.subList(1, linkListMutate.size())) {
				if (NetworkMutationImpl.searchStopFacilitiesOnLink(metroLinkAttributes, globalNetwork.getLinks().get(linkId)) != null) {
					nextLinkWithFacility = linkId;
					break;
				}
			}
			List<Id<Link>> linksToKillTillNextFacility = new ArrayList<Id<Link>>();
			if (nextLinkWithFacility != null) {
				if (nextLinkWithFacility.equals(linkListMutate.get(linkListMutate.size()-1)) == false) {
					linksToKillTillNextFacility = linkListMutate.subList(0, linkListMutate.indexOf(nextLinkWithFacility));
					Log.write("   >> Modifying route by removing links between start terminal and first stop with link = "+nextLinkWithFacility.toString());
				}
				else {
					linksToKillTillNextFacility.addAll(linkListMutate);
					Log.write("   >> Deleting route due to unprofitability and no intermediate stops between terminals ...");
				}
			}
			else {
				linksToKillTillNextFacility.addAll(linkListMutate);
				Log.write("   >> Deleting route due to unprofitability and no intermediate stops between terminals ...");
			}
			linkListMutate.removeAll(linksToKillTillNextFacility);
		}
		else {  //	shorten on end link
			Id<Link> lastLinkWithFacility = null;
			for (int l=2; l<linkListMutate.size(); l++) {
				Id<Link> linkId = linkListMutate.get(linkListMutate.size()-l);
				if (NetworkMutationImpl.searchStopFacilitiesOnLink(metroLinkAttributes, globalNetwork.getLinks().get(linkId)) != null) {
					lastLinkWithFacility = linkId;
					break;
				}
			}
			List<Id<Link>> linksToKillTillNextFacility = new ArrayList<Id<Link>>();
			if (lastLinkWithFacility != null) {
				if (lastLinkWithFacility.equals(linkListMutate.get(0)) == false) {
					linksToKillTillNextFacility = linkListMutate.subList(linkListMutate.indexOf(lastLinkWithFacility)+1, linkListMutate.size());
					Log.write("   >> Modifying route by removing links between end terminal and last stop before terminal with link = "+lastLinkWithFacility.toString());
				}
				else {
					linksToKillTillNextFacility.addAll(linkListMutate);
					Log.write("   >> Deleting route due to unprofitability and no intermediate stops between terminals ...");
				}
			}
			else {
				linksToKillTillNextFacility.addAll(linkListMutate);
				Log.write("   >> Deleting route due to unprofitability and no intermediate stops between terminals ...");
			}
			linkListMutate.removeAll(linksToKillTillNextFacility);
		}
	}
	
	public static void extendRoute(Iterator<Entry<String, MRoute>> mrouteIter, List<Id<Link>> linkListMutate, Network globalNetwork, 
			double maxCrossingAngle, MRoute mRoute, double averageRouletteScore,
			Map<Id<Link>, CustomMetroLinkAttributes> metroLinkAttributes, MNetwork mNetwork) throws IOException {
		Random rEnd = new Random();
		if(rEnd.nextDouble() < 0.5) { // add on start link
//			Log.write("Trying to add extension before start link");
			List<Id<Link>> linkListMutateExtended = findExtensionToCloseFacilities("IN", linkListMutate, maxCrossingAngle, globalNetwork, metroLinkAttributes);
			if (linkListMutateExtended == null) {
//				Log.write("Failed to add extension before start link ... trying to add after end link");
				linkListMutateExtended = findExtensionToCloseFacilities("OUT", linkListMutate, maxCrossingAngle, globalNetwork, metroLinkAttributes);
				if (linkListMutateExtended == null) {
					Log.write("Failed to add extension after end link and before start link --> Route cannot be extended any more and is left as is. ");
				}
				else {
					linkListMutate = linkListMutateExtended;	// set linkListMutate (which will be returned) to be the new linkList
					Log.write("Failed to add extension before start link, but succeeded to add after end link.");
				}
			}
			else {
				linkListMutate = linkListMutateExtended;	// set linkListMutate (which will be returned) to be the new linkList
				Log.write("Succeeded to add extension before start link.");
			}
		}
		else {  //	add on end link
//			Log.write("Trying to add extension after end link");
			List<Id<Link>> linkListMutateExtended = findExtensionToCloseFacilities("OUT", linkListMutate, maxCrossingAngle, globalNetwork, metroLinkAttributes);
			if (linkListMutateExtended == null) {
//				Log.write("Failed to add extension after end link ... trying to add before start link");
				linkListMutateExtended = findExtensionToCloseFacilities("IN", linkListMutate, maxCrossingAngle, globalNetwork, metroLinkAttributes);
				if (linkListMutateExtended == null) {
					Log.write("Failed to add extension after end link and before start link --> Route cannot be extended any more and is left as is. ");
				}
				else {
					linkListMutate = linkListMutateExtended;	// set linkListMutate (which will be returned) to be the new linkList
					Log.write("Failed to add extension after end link, but succeeded to add before start link.");
				}
			}
			else {
				linkListMutate = linkListMutateExtended;	// set linkListMutate (which will be returned) to be the new linkList
				Log.write("Succeeded to add extension after end link.");
			}
		}
	}
	
	public static List<Id<Link>> findExtensionToCloseFacilities(String strategy, List<Id<Link>> routeLinks, double maxCrossingAngle, Network globalNetwork, Map<Id<Link>, CustomMetroLinkAttributes> metroLinkAttributes) throws IOException {
		List<Id<Link>> extendedRouteLinks = null;
		for (int lowestTreeLevel=1; lowestTreeLevel<100; lowestTreeLevel++) {
			if (strategy.equals("IN")) {
				extendedRouteLinks = dig4facilitiesIN(lowestTreeLevel, routeLinks, maxCrossingAngle, globalNetwork, metroLinkAttributes);				
			}
			else if (strategy.equals("OUT")) {
				extendedRouteLinks = dig4facilitiesOUT(lowestTreeLevel, routeLinks, maxCrossingAngle, globalNetwork, metroLinkAttributes);				
			}
			if (extendedRouteLinks != null) {
				return extendedRouteLinks;
			}
		}
		// return null if nothing found by digging for next facilities
		return null;
	}

	public static void applyBigChange2(Map<String, CustomStop> allMetroStops, List<Id<Link>> linkListMutate, Network globalNetwork, double maxCrossingAngle, 
			Coord zurich_NetworkCenterCoord, MRoute mRoute, Map<Id<Link>, CustomMetroLinkAttributes> metroLinkAttributes) throws IOException {
		
		List<TransitStopFacility> servicedFacilities = new ArrayList<TransitStopFacility>();
		// these facilities are serviced by original route and should not be reinserted as "new"
		List<Id<Link>> linkListMutateFacilityLinks = new ArrayList<Id<Link>>();
		// these links hold one or more active stop facilities
		for (Id<Link> linkId : linkListMutate) {
			List<TransitStopFacility> facilitiesOnLink = null;
			facilitiesOnLink = returnAllStopFacilitiesOnLink(metroLinkAttributes, globalNetwork.getLinks().get(linkId));
			if (facilitiesOnLink.size() > 0) {
				linkListMutateFacilityLinks.add(linkId);
				servicedFacilities.addAll(facilitiesOnLink);
			}
		}
		
		double pDelete = 0.2;
		if ((new Random()).nextDouble() < pDelete) {
			// do not service one stop facility: this facility will be blocked when running stopAddingRoutine along route and it will not be added as a route stop
			
			Id<Link> blockedLink = linkListMutateFacilityLinks.get((new Random()).nextInt(linkListMutateFacilityLinks.size()-2)+1);
			mRoute.facilityBlockedLinks.add(blockedLink);
			Log.write("Blocking facility link for servicing an active stop: "+blockedLink.toString());
		}
		else {
			// insert an additional stop facility (facility insertion)
			// if it can't just be inserted, remove existing legs between stops to be able to reasonably connect new stop facility links
			// removing one existing leg between two stops and connecting loose ends to new stop instead is identical to "replacing a stop"
			InsertionLoop:
			do {
				// Choose randomly where (at) which link route shall be cut open initially. Cut will be moved up and down route if insertion is not possible. 
				// -2 because we don't choose start or end node for insertion
				// +1 because we start from second node, and not from start node
				
				Id<Link> cutOpenLinkId;
				Link cutOpenLink;
				int cutIndex;
				int n = 0;
				do {
					cutIndex = (new Random()).nextInt(linkListMutateFacilityLinks.size()-2)+1;
					cutOpenLinkId = linkListMutateFacilityLinks.get(cutIndex);
					cutOpenLink = globalNetwork.getLinks().get(cutOpenLinkId);
					n++;
					if (n>50) {
						Log.write("Failing to find a route stop insertion point within (metro) city. Not inserting any node and proceeding to next route.");
						break InsertionLoop; 
					}
				} while(GeomDistance.calculate(cutOpenLink.getFromNode().getCoord(), zurich_NetworkCenterCoord) > 5000.0);
				int facilitiesUntilEndTerminal = linkListMutateFacilityLinks.size()-1-cutIndex;
				int facilitiesUntilStartTerminal = cutIndex;
				
				for (int i=1; i<=Math.min(facilitiesUntilEndTerminal, 4.0); i++) {
					Id<Link> cutCloseLinkId = linkListMutateFacilityLinks.get(cutIndex + i);
					Link cutCloseLink = globalNetwork.getLinks().get(cutCloseLinkId);
					if (GeomDistance.calculate(cutCloseLink.getFromNode().getCoord(), cutOpenLink.getFromNode().getCoord()) > 3500) {
						continue;
					}
					Log.write("Testing close node nPositions DOWN the route: n="+i);
					List<Id<Link>> reroutedLinkRoute = insertNewStopInRoute(servicedFacilities, cutOpenLinkId, cutCloseLinkId, globalNetwork,
							allMetroStops, linkListMutate, linkListMutateFacilityLinks, maxCrossingAngle);
					if (reroutedLinkRoute == null) {
						continue;
					} else {
						linkListMutate = reroutedLinkRoute;
						Log.write("A new facility node was inserted successfully!");
						break InsertionLoop;
					}
				}
				
				// do in reverse direction
				List<Id<Link>>linkListMutateReverse = NetworkEvolutionImpl.OppositeLinkListOf(linkListMutate);				
				List<Id<Link>>linkListMutateFacilityLinksReverse = NetworkEvolutionImpl.OppositeLinkListOf(linkListMutateFacilityLinks);
				
				
				for (int i=1; i<=Math.min(facilitiesUntilStartTerminal, 4.0); i++) {
					Id<Link> cutCloseLinkId = linkListMutateFacilityLinks.get(cutIndex - i);
					Link cutCloseLink = globalNetwork.getLinks().get(cutCloseLinkId);
					if (GeomDistance.calculate(cutCloseLink.getFromNode().getCoord(), cutOpenLink.getFromNode().getCoord()) > 3500) {
						continue;
					}
					Log.write("Testing close node nPositions UP the route: n="+i);
					List<Id<Link>> reroutedLinkRoute = insertNewStopInRoute(servicedFacilities, cutOpenLinkId, cutCloseLinkId, globalNetwork,
							allMetroStops, linkListMutate, linkListMutateFacilityLinks, maxCrossingAngle);
					if (reroutedLinkRoute == null) {
						continue;
					} else {
						linkListMutate = reroutedLinkRoute;
						break InsertionLoop;
					}
				}
				
				// if not succeeded to insert node in for loops by now, no insertion is applied
				Log.write("Failed to insert node. No big problem, but no big mutation has been applied to "+mRoute.routeID);
				break;
			} while(true);
		}
		
		if (linkListMutate.size() == 0) {
			Log.write("ERROR: Mutated Route has Size=0! --> CHECK CODE ...");
		}
		linkListMutate.addAll(NetworkEvolutionImpl.OppositeLinkListOf(linkListMutate));
		mRoute.linkList = Clone.list(linkListMutate);
		mRoute.networkRoute = RouteUtils.createNetworkRoute(linkListMutate, globalNetwork);
	}
	

	public static List<TransitStopFacility> returnAllStopFacilitiesOnLink( Map<Id<Link>, CustomMetroLinkAttributes> metroLinkAttributes, Link currentLink) {
		CustomMetroLinkAttributes customMetroLinkAttributes = metroLinkAttributes.get(currentLink.getId());
		List<TransitStopFacility> facilitiesOnLink = new ArrayList<TransitStopFacility>();
		if (customMetroLinkAttributes == null) {
			// Log.write("No metro link attributes found for link = "+currentLink.getId().toString());
			return facilitiesOnLink;
		}
		if (customMetroLinkAttributes.singleRefStopFacility != null) {
			facilitiesOnLink.add(customMetroLinkAttributes.singleRefStopFacility);
		}
		if(customMetroLinkAttributes.fromNodeStopFacility != null) {
			facilitiesOnLink.add(customMetroLinkAttributes.fromNodeStopFacility);
		}
		if(customMetroLinkAttributes.toNodeStopFacility != null) {
			facilitiesOnLink.add(customMetroLinkAttributes.toNodeStopFacility);
		}
		return facilitiesOnLink;
	}


	public static List<Id<Link>> insertNewStopInRoute(List<TransitStopFacility> servicedFacilities, Id<Link> cutOpenLinkId, Id<Link> cutCloseLinkId,
			Network globalNetwork, Map<String, CustomStop> allMetroStops, List<Id<Link>> linkListMutate,
			List<Id<Link>> linkListMutateFacilityLinks, double maxCrossingAngle) throws IOException {
		
		Link cutOpenLink = globalNetwork.getLinks().get(cutOpenLinkId);
		Link cutCloseLink = globalNetwork.getLinks().get(cutCloseLinkId);
		Node cutOpenNode = cutOpenLink.getToNode();
		Node cutCloseNode = cutCloseLink.getFromNode();
		
		// check all stops
		stopsLoop:
		for (CustomStop stop : allMetroStops.values()) {
			TransitStopFacility insertStopFacility = stop.transitStopFacility;
			if (insertStopFacility == null) {
				Log.write("For unknown reasons the transitStopFacility was not set on original stop "+stop.originalMainTransitStopFacility.getName());
				continue;
			}
			for (TransitStopFacility servicedFacility : servicedFacilities) {
				if (insertStopFacility.getId().equals(servicedFacility.getId())) {
//					Log.write("InsertStopFacilityCandidate " + insertStopFacility.getName() + " is already serviced by original route. Try next candidate...");
					continue stopsLoop;
				}	
				else {
//					Log.write("Found unserviced facility = "+insertStopFacility.getName());
				}
			}
			Id<Node> insertNodeId = stop.newNetworkNode;
			Coord insertCoord = insertStopFacility.getCoord();
			if (globalNetwork.getNodes().containsKey(insertNodeId) == false) {
				continue;
			}
			// check if such stop is within bounds
			// Make permitted new insertFacility zone according to constraints below (2.5 distance and 180-maxCrossingAngle opening projection)
				// if yes: check if it can be connected by a Dijkstra with feasible angles
					// if yes: add shortest connecting path to linkListMutate
					// if no:  try cutting out one more link around open link (first top one if exists, then bottom one if exists)
			if (GeomDistance.angleBetweenPoints(cutOpenNode.getCoord(), cutCloseNode.getCoord(), insertCoord) > 90
					&& GeomDistance.angleBetweenPoints(cutCloseNode.getCoord(), cutOpenNode.getCoord(), insertCoord) > 90
					&& GeomDistance.calculate(cutOpenNode.getCoord(), insertCoord) < Math.min(1.5*GeomDistance.betweenNodes(cutOpenNode, cutCloseNode), 3500.0)
					) { // check if new facility node insertion makes sense and continue if not reasonable // more options then [<80] is > [(180 - maxCrossingAngle)]
						Log.write("Calculating OPENPATH shortest path between " + cutOpenNode.getId() + "  &  " + insertNodeId);
						List<Node> nodePathOpen = DemoDijkstra.calculateShortestPath(globalNetwork, cutOpenNode.getId(), insertNodeId);
						if (nodePathOpen == null || nodePathOpen.contains(cutCloseNode)) {
							continue;
						}
						List<Link> extendedLinkPathOpen = NetworkEvolutionImpl.nodeListToNetworkLinkList2(globalNetwork, nodePathOpen);
						extendedLinkPathOpen.add(0, cutOpenLink);
//						Log.write("extendedLinkPathOpen undergoing maxTurningAngleCheck = "+extendedLinkPathOpen.toString());
						if (checkIfTurningAnglesOk(maxCrossingAngle, extendedLinkPathOpen) == false) {
							continue;
						}
						else {
							extendedLinkPathOpen.remove(cutOpenLink);
						}
						Log.write("Calculating CLOSEPATH shortest path between " + insertNodeId + "  &  " + cutCloseNode.getId());
						List<Node> nodePathClose = DemoDijkstra.calculateShortestPath(globalNetwork, insertNodeId, cutCloseNode.getId());
						if (nodePathClose == null || nodePathClose.size() < 2 || nodePathClose.contains(cutOpenNode)) {
							continue;
						}
						List<Link> extendedLinkPathClose = NetworkEvolutionImpl.nodeListToNetworkLinkList2(globalNetwork, nodePathClose);
						extendedLinkPathClose.add(cutCloseLink);
//						Log.write("extendedLinkPathClose undergoing maxTurningAngleCheck = "+extendedLinkPathClose.toString());
						if (checkIfTurningAnglesOk(maxCrossingAngle, extendedLinkPathClose) == false) {
							continue;
						}
						else {
							extendedLinkPathClose.remove(cutCloseLink);
						}
						if (GeomDistance.angleBetweenLinks(extendedLinkPathOpen.get(extendedLinkPathOpen.size()-1), extendedLinkPathClose.get(0)) > maxCrossingAngle) {
							continue;
						}
						
						List<Link> newLeg = new ArrayList<Link>();
						newLeg.addAll(extendedLinkPathOpen);
						newLeg.addAll(extendedLinkPathClose);
						List<Id<Link>> newLegIds = new ArrayList<Id<Link>>();
						for (Link legLink : newLeg) {
							newLegIds.add(legLink.getId());
						}
						List<Id<Link>> reroutedLinks = new ArrayList<Id<Link>>();
						reroutedLinks.addAll(linkListMutate.subList(0, linkListMutate.indexOf(cutOpenLink.getId())+1));
						reroutedLinks.addAll(newLegIds);
						reroutedLinks.addAll(linkListMutate.subList(linkListMutate.indexOf(cutCloseLink.getId()), linkListMutate.size()));
						Log.write("Node insertion between " + cutOpenLinkId.toString() + " & " + cutCloseLinkId.toString());
						Log.write(" >> New leg connecting insert node "+insertNodeId.toString() + " is: " + newLegIds.toString());
						return reroutedLinks;
			}
			else {
				continue;
			}
			
		}
		return null;
	}


	public static boolean checkIfTurningAnglesOk(double maxCrossingAngle, List<Link> linkPath) {
		Link lastLink = linkPath.get(0);
		for (Link thisLink : linkPath.subList(1, linkPath.size())) {
			if (GeomDistance.angleBetweenLinks(lastLink, thisLink) > maxCrossingAngle) {
				return false;
			}
			lastLink = thisLink;
		}
		return true;
	}


	public static List<Id<Link>> dig4facilitiesIN(int lowestTreeLevel, List<Id<Link>> routeLinksIn, double maxCrossingAngle, Network globalNetwork, Map<Id<Link>, CustomMetroLinkAttributes> metroLinkAttributes) throws IOException {
		List<Id<Link>> routeLinks = new ArrayList<Id<Link>>();
		routeLinks.addAll(routeLinksIn);
		Link startLink = globalNetwork.getLinks().get(routeLinks.get(0));
		Node startNode = globalNetwork.getNodes().get(startLink.getFromNode().getId());
		if (lowestTreeLevel == 1) {
			for (Link previousLink : startNode.getInLinks().values()) {
				if (GeomDistance.angleBetweenLinks(previousLink, startLink) > maxCrossingAngle) {
					continue;
				}
				if (searchAcceptableStopFacilitiesOnLink(metroLinkAttributes, previousLink, metroLinkAttributes.get(startLink.getId()).fromNodeStopFacility) != null) {
					// second condition makes sure that a link is not added, which has same facility on FromNode as end link on ToNode
					// given that they share that node and therefore also a possible stopFacility on that node!
					routeLinks.add(0, previousLink.getId());
					return routeLinks;
				}
			}
		}
		else {
			for (Link previousLink : globalNetwork.getNodes().get(globalNetwork.getLinks().get(routeLinks.get(0)).getFromNode().getId()).getInLinks().values()) {
				if (GeomDistance.angleBetweenLinks(previousLink, startLink) > maxCrossingAngle) {
					continue;
				}
				List<Id<Link>> extRouteList = new ArrayList<Id<Link>>();
				extRouteList.addAll(routeLinks);
				extRouteList.add(0, previousLink.getId());
				List<Id<Link>> extExtRouteList = dig4facilitiesIN(lowestTreeLevel-1, extRouteList, maxCrossingAngle, globalNetwork, metroLinkAttributes);
				if (extExtRouteList != null) {
					return extExtRouteList;
				}
			}			
		}
		// if nothing has been returned by this point, extend search to next level of link tree
		return null;
	}

	public static List<Id<Link>> dig4facilitiesOUT(int lowestTreeLevel, List<Id<Link>> routeLinksIn, double maxCrossingAngle, Network globalNetwork, Map<Id<Link>, CustomMetroLinkAttributes> metroLinkAttributes) throws IOException {
		List<Id<Link>> routeLinks = new ArrayList<Id<Link>>();
		routeLinks.addAll(routeLinksIn);
		Link endLink = globalNetwork.getLinks().get(routeLinks.get(routeLinks.size()-1));
		Node endNode = globalNetwork.getNodes().get(endLink.getToNode().getId());
		if (lowestTreeLevel == 1) {
			for (Link nextLink : endNode.getOutLinks().values()) {
				if (GeomDistance.angleBetweenLinks(nextLink, endLink) > maxCrossingAngle) {
					continue;
				}
				if (searchAcceptableStopFacilitiesOnLink(metroLinkAttributes, nextLink, metroLinkAttributes.get(endLink.getId()).toNodeStopFacility) != null) {
					// second condition makes sure that a link is not added, which has same facility on FromNode as end link on ToNode given that they touch.
					routeLinks.add(nextLink.getId());
					return routeLinks;
				}
			}
		}
		else {
			for (Link nextLink : globalNetwork.getNodes().get(globalNetwork.getLinks().get(routeLinks.get(routeLinks.size()-1)).getToNode().getId()).getOutLinks().values()) {
				if (GeomDistance.angleBetweenLinks(nextLink, endLink) > maxCrossingAngle) {
					continue;
				}
				List<Id<Link>> extRouteList = new ArrayList<Id<Link>>();
				extRouteList.addAll(routeLinks);
				extRouteList.add(nextLink.getId());
				List<Id<Link>> extExtRouteList = dig4facilitiesOUT(lowestTreeLevel-1, extRouteList, maxCrossingAngle, globalNetwork, metroLinkAttributes);
				if (extExtRouteList != null) {
					return extExtRouteList;
				}
			}			
		}
		// if nothing has been returned by this point, extend search to next level of link tree
		return null;
	}
	
	public static TransitStopFacility searchStopFacilitiesOnLink(Map<Id<Link>,CustomMetroLinkAttributes> metroLinkAttributes, Link currentLink) throws IOException {
		CustomMetroLinkAttributes customMetroLinkAttributes = metroLinkAttributes.get(currentLink.getId());
		if (customMetroLinkAttributes == null) {
//			Log.write("No metro link attributes found for link = "+currentLink.getId().toString());
			return null;
		}
		else if (customMetroLinkAttributes.singleRefStopFacility != null) {
//			Log.write("Found SINGLE REF FACILITY " +  customMetroLinkAttributes.singleRefStopFacility.getName() + " on link="+currentLink.getId().toString());
			return customMetroLinkAttributes.singleRefStopFacility;
		}
		else if(customMetroLinkAttributes.fromNodeStopFacility != null) {
//			Log.write("Found FROM NODE FACILITY " +  customMetroLinkAttributes.fromNodeStopFacility.getName() + " on link="+currentLink.getId().toString());
			return customMetroLinkAttributes.fromNodeStopFacility;
		}
		else if(customMetroLinkAttributes.toNodeStopFacility != null) {
//			Log.write("Found TO NODE FACILITY " +  customMetroLinkAttributes.toNodeStopFacility.getName() + " on link="+currentLink.getId().toString());
			return customMetroLinkAttributes.toNodeStopFacility;
		}
		else {
//			Log.write("Found no facility on link= "+currentLink.getId().toString() + "... Try next link...");
			return null;
		}
	}
	
	public static TransitStopFacility searchAcceptableStopFacilitiesOnLink(Map<Id<Link>,CustomMetroLinkAttributes> metroLinkAttributes,
			Link currentLink, TransitStopFacility blockedFacility) throws IOException {
		CustomMetroLinkAttributes customMetroLinkAttributes = metroLinkAttributes.get(currentLink.getId());
		if (customMetroLinkAttributes == null) {
//			Log.write("No metro link attributes found for link = "+currentLink.getId().toString());
			return null;
		}
		else if (customMetroLinkAttributes.singleRefStopFacility != null && customMetroLinkAttributes.singleRefStopFacility != blockedFacility) {
//			Log.write("Found SINGLE REF FACILITY " +  customMetroLinkAttributes.singleRefStopFacility.getName() + " on link="+currentLink.getId().toString());
			return customMetroLinkAttributes.singleRefStopFacility;
		}
		else if(customMetroLinkAttributes.fromNodeStopFacility != null && customMetroLinkAttributes.fromNodeStopFacility != blockedFacility) {
//			Log.write("Found FROM NODE FACILITY " +  customMetroLinkAttributes.fromNodeStopFacility.getName() + " on link="+currentLink.getId().toString());
			return customMetroLinkAttributes.fromNodeStopFacility;
		}
		else if(customMetroLinkAttributes.toNodeStopFacility != null && customMetroLinkAttributes.toNodeStopFacility != blockedFacility) {
//			Log.write("Found TO NODE FACILITY " +  customMetroLinkAttributes.toNodeStopFacility.getName() + " on link="+currentLink.getId().toString());
			return customMetroLinkAttributes.toNodeStopFacility;
		}
		else {
//			Log.write("Found no new facility on link= "+currentLink.getId().toString() + "... Try next link...");
			return null;
		}
	}
	
	
}
