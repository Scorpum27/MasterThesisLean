package ch.ethz.matsim.students.samark;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class EvoOpsMutator {

	public EvoOpsMutator() {
	}

	
	@SuppressWarnings("unchecked")
	public static MNetworkPop applyMutations(Integer currentGEN, MNetworkPop newPopulation, Network globalNetwork, Coord zurich_NetworkCenterCoord, int lastIterationOriginal,
			double pMutation, double pBigChange, double pSmallChange, Double routeDisutilityLimit,
			double maxCrossingAngle, String eliteNetworkName, Map<Id<Link>, CustomMetroLinkAttributes> metroLinkAttributes) throws IOException {
		Map<String, CustomStop> allMetroStops = new HashMap<String, CustomStop>();
		allMetroStops.putAll(XMLOps.readFromFile(allMetroStops.getClass(), "zurich_1pm/Evolution/Population/BaseInfrastructure/metroStopAttributes.xml"));

		if (currentGEN >= 27) {
			pMutation = 0.30;
			pBigChange = 0.30;
			pSmallChange = 1-pBigChange;
		}
		if (currentGEN >= 39) {
			pMutation = 0.25;
			pBigChange = 0.20;
			pSmallChange = 1-pBigChange;
		}
		
		List<String> mutatedNetworks = new ArrayList<String>();
		List<Id<Link>> linkListMutate;
		for (MNetwork mNetwork : newPopulation.networkMap.values()) {
			String mNetworkName = mNetwork.networkID;
			boolean hasHadMutation = false;
			if (mNetworkName.equals(eliteNetworkName)) {
				continue;
			}
			CBPII cbpOriginal =
					XMLOps.readFromFile((new CBPII()).getClass(), 
							"zurich_1pm/cbpParametersOriginal/cbpParametersOriginalGlobal.xml");
			CBPII cbpNew = XMLOps.readFromFile((new CBPII()).getClass(), 
					"zurich_1pm/Evolution/Population/"+mNetwork.networkID+"/cbpParametersAveraged"+lastIterationOriginal+".xml");
			Map<String, Double> routeScoreMap = new HashMap<String, Double>();
			Map<String, Double> routeMutationProbabilitiesMap = new HashMap<String, Double>();
			for (MRoute mRoute : mNetwork.routeMap.values()) {
				// CAUTION: If a route has undergone Crossover, its value will be -Double.MAX_VALUE and will therefore not be ranked automatically in "sortRoutes"!
				// Take in account by increasing value slightly so it is higher than -Double.MAX_VALUE
				if (mRoute.utilityBalance < -1.0E20) {
					mRoute.utilityBalance = -1.0E20;
				}
				routeScoreMap.put(mRoute.routeID, mRoute.utilityBalance);
			}
//			double averageRouletteScore = 0.0;
			List<String> rankedRoutes = EvoOpsMutator.sortRoutesByScore(routeScoreMap);	// highest first
			int N = rankedRoutes.size();
			for (int n=0; n<N; n++) {
				routeMutationProbabilitiesMap.put(rankedRoutes.get(n), 2.0*(n+1)/(N+1));
				// 1-p(n), because highest score should least likely be mutated
			}
			Double pMutationUptodate;
			if (pMutation>1.0*(N+1)/(2*N)) {
				pMutationUptodate = 1.0*(N+1)/(2*N);
				Log.write("CAUTION: pMutation too high for the number of remaining routes. Lowering to"+pMutationUptodate);
			}
			else {
				pMutationUptodate = pMutation;
			}
			Iterator<Entry<String, MRoute>> mrouteIter = mNetwork.routeMap.entrySet().iterator();
			while (mrouteIter.hasNext()) {
				Entry<String, MRoute> mrouteEntry = mrouteIter.next();
				MRoute mRoute = mrouteEntry.getValue();
				Random rMutation = new Random();
				if (rMutation.nextDouble() < routeMutationProbabilitiesMap.get(mRoute.routeID) * pMutationUptodate) {
					// meanMutationRate=0.5 by nature of rankMethod. xpMutation for bringing down overall mutation rate to a desired value.
					if ((new Random()).nextDouble() < 0.5) {
						// do this to give 50/50 chance of taking either direction (this increases randomness e.g. for crawling along route when inserting new nodes)
						linkListMutate = mRoute.linkList.subList(0, mRoute.linkList.size()/2);
					}
					else {
						linkListMutate = mRoute.linkList.subList(mRoute.linkList.size()/2, mRoute.linkList.size());
					}
					Log.writeAndDisplay("  > Mutating route = "+mRoute.getId());
					Random rBig = new Random();
					if (rBig.nextDouble() < pBigChange) { // make big change
						if(linkListMutate.size()>2) {
							Log.writeAndDisplay("  >> Attempting big change");
							hasHadMutation = EvoOpsMutator.applyBigChange2(allMetroStops, linkListMutate, globalNetwork, maxCrossingAngle, zurich_NetworkCenterCoord,
									mRoute, mrouteIter, metroLinkAttributes);
						}
					}
					else{ // make small change
						Log.writeAndDisplay("  >> Attempting small change");
						boolean smallChangeSucceeded = EvoOpsMutator.applySmallChange(cbpOriginal, cbpNew, mrouteIter, linkListMutate, 
								globalNetwork, maxCrossingAngle, mRoute, metroLinkAttributes, routeDisutilityLimit);
						if (smallChangeSucceeded == false) {
							continue;
						}
						else {
							hasHadMutation = true;
						}
					}
				}
				else {
					continue;
				}
			}
			if (hasHadMutation) {
				if (mutatedNetworks.contains(mNetworkName)==false) {
					mutatedNetworks.add(mNetworkName);
				}
			}
		}
		for(String networkName : mutatedNetworks) {
			if (newPopulation.modifiedNetworksInLastEvolution.contains(networkName)==false) {
				newPopulation.modifiedNetworksInLastEvolution.add(networkName);
			}
		}
		
		NetworkWriter nw = new NetworkWriter(globalNetwork);
		nw.write("zurich_1pm/Evolution/Population/BaseInfrastructure/GlobalNetwork.xml");
		return newPopulation;
	}
	
	
	
	public static List<String> sortRoutesByScore(Map<String, Double> routeScoreMap) throws IOException {
		List<String> sortedNetworks = new ArrayList<String>();
		sortedNetworks.add(""); // do this just as a helper to start off with so that valueArray we compare to is not empty
		List<Double> sortedValues = new ArrayList<Double>();
		sortedValues.add(-Double.MAX_VALUE);
		for (Entry<String, Double> entry : routeScoreMap.entrySet()) {
			for (int index = 0; index < sortedNetworks.size(); index++) {
				if (entry.getValue() > sortedValues.get(index)) {
					sortedNetworks.add(index, entry.getKey());
					sortedValues.add(index, entry.getValue());
					break;
				}
			}
		}
		sortedNetworks.remove(sortedNetworks.size() - 1); // removing the "" entry at the end again.
		// sortedValues.remove(sortedValues.size() - 1); // not necessary, just here for completion and possible extensions
		return sortedNetworks;
	}


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

	
	public static boolean applySmallChange(CBPII refCase, CBPII newCase,
			Iterator<Entry<String, MRoute>> mrouteIter, List<Id<Link>> linkListMutate, Network globalNetwork, Double maxCrossingAngle, MRoute mRoute,
			Map<Id<Link>, CustomMetroLinkAttributes> metroLinkAttributes, Double routeDisutilityLimit) throws IOException {

		if (mRoute.utilityBalance > routeDisutilityLimit) { // extend route
			extendRoute(mrouteIter, linkListMutate, globalNetwork, maxCrossingAngle, mRoute, metroLinkAttributes);
			// with 50% chance try another extension
			if (linkListMutate.size() < 2) {
				Log.write("CAUTION: RouteLength = " + linkListMutate.size() + " --> Deleting "+mRoute.routeID);
				mrouteIter.remove();
				return false;
			}
			else if ((new Random()).nextDouble()<0.5) {
				extendRoute(mrouteIter, linkListMutate, globalNetwork, maxCrossingAngle, mRoute, metroLinkAttributes);				
			}
		}
		else { // shorten route
			shortenRoute(mrouteIter, linkListMutate, globalNetwork, maxCrossingAngle, mRoute, metroLinkAttributes);
			// with 100% chance try another shortening
			if (linkListMutate.size() < 2) {
				Log.write("CAUTION: RouteLength = " + linkListMutate.size() + " --> Deleting "+mRoute.routeID);
				mrouteIter.remove();
				return false;
			}
			else if ((new Random()).nextDouble()<0.5) {
				shortenRoute(mrouteIter, linkListMutate, globalNetwork, maxCrossingAngle, mRoute, metroLinkAttributes);
			}
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
			double maxCrossingAngle, MRoute mRoute, Map<Id<Link>, CustomMetroLinkAttributes> metroLinkAttributes) throws IOException {
		Log.writeAndDisplay("  > Attempting route shortening for "+mRoute.getId());
		Random rEnd = new Random();
		if(rEnd.nextDouble() < 0.5) { // shorten on start link
			Id<Link> nextLinkWithFacility = null;
			for (Id<Link> linkId : linkListMutate.subList(1, linkListMutate.size())) {
				if (EvoOpsMutator.searchStopFacilitiesOnLink(metroLinkAttributes, globalNetwork.getLinks().get(linkId)) != null) {
					nextLinkWithFacility = linkId;
					break;
				}
			}
// 			ALTERNATIVE: DELETE 2 STOPS instead of 1: TODO check if written correctly
//			int stopCountStart = 0;
//			for (Id<Link> linkId : linkListMutate.subList(1, linkListMutate.size())) {
//				if (EvoOpsMutator.searchStopFacilitiesOnLink(metroLinkAttributes, globalNetwork.getLinks().get(linkId)) != null) {
//					stopCountStart++;
//					if (stopCountStart == 2) {
//						nextLinkWithFacility = linkId;
//						break;
//					}
//				}
//			}
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
				if (EvoOpsMutator.searchStopFacilitiesOnLink(metroLinkAttributes, globalNetwork.getLinks().get(linkId)) != null) {
					lastLinkWithFacility = linkId;
					break;
				}
//	 			ALTERNATIVE: DELETE 2 STOPS instead of 1: TODO check if written correctly
//				int stopCountEnd = 0;
//				for (Id<Link> linkId : linkListMutate.subList(1, linkListMutate.size())) {
//					if (EvoOpsMutator.searchStopFacilitiesOnLink(metroLinkAttributes, globalNetwork.getLinks().get(linkId)) != null) {
//						stopCountEnd++;
//						if (stopCountEnd == 2) {
//							lastLinkWithFacility = linkId;
//							break;
//						}
//					}
//				}
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
		mRoute.hasBeenShortened = true;	// do this to tell frequencyModifier that he can further remove a vehicle!
		Log.writeAndDisplay("  > Susscessul SHORTENING for "+mRoute.getId());
	}
	
	
	// %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%  HELPER METHODS %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
	
	public static void extendRoute(Iterator<Entry<String, MRoute>> mrouteIter, List<Id<Link>> linkListMutate, Network globalNetwork, 
			double maxCrossingAngle, MRoute mRoute,
			Map<Id<Link>, CustomMetroLinkAttributes> metroLinkAttributes) throws IOException {
		Log.writeAndDisplay("  > Attempting route extension for "+mRoute.getId());
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

	public static Boolean applyBigChange2(Map<String, CustomStop> allMetroStops, List<Id<Link>> linkListMutate, Network globalNetwork, double maxCrossingAngle, 
			Coord zurich_NetworkCenterCoord, MRoute mRoute, Iterator<Entry<String, MRoute>> mrouteIter, Map<Id<Link>,CustomMetroLinkAttributes> metroLinkAttributes) throws IOException {

		List<Id<Link>> originalLinkListMutate = Clone.list(linkListMutate);
		
//		Log.write("extraLog.txt", "Performing big change2");
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
		if (linkListMutateFacilityLinks.size() < 2) {
			Log.write("CAUTION: Number of links with facilities = " + linkListMutateFacilityLinks.size() + " --> Removing"+mRoute.routeID);
			mrouteIter.remove();
			return true;
		}
		
		
		double pDelete = 0.05;
		if ((new Random()).nextDouble() < pDelete) {	// DELETE ONE STOP: so that this stop is not serviced
			Log.writeAndDisplay("  > Attempting stop skipper for "+mRoute.getId());
			// do not service one stop facility: this facility will be blocked when running stopAddingRoutine along route and it will not be added as a route stop

			Id<Link> blockedLink = linkListMutateFacilityLinks.get((new Random()).nextInt(linkListMutateFacilityLinks.size()-2)+1);
			mRoute.facilityBlockedLinks.add(blockedLink);
			mRoute.facilityBlockedLinks.add(NetworkEvolutionImpl.ReverseLink(blockedLink));
//			Log.write("Blocking facility links for servicing an active stop:  "+blockedLink.toString() + "  &  " + NetworkEvolutionImpl.ReverseLink(blockedLink));
			Log.writeAndDisplay("  > Delete a stop for = "+mRoute.getId());
		}
		else { /// PERFORM OTHER BIG MODIFICATION
			if ((new Random()).nextDouble() < 0.5) {	// CUT OPEN AND SWITCH ONE TERMINAL to another stopfacility within constraints				
				Log.writeAndDisplay("  > Attempting terminal switch for "+mRoute.getId());
//				Log.write("extraLog.txt", "X, it's happening...");

				List<Integer> cutIndices = new ArrayList<Integer>();
				Id<Link> cutOpenLinkId;
				Link cutOpenLink;
				int cutIndex;
				int n = 0;
				FindCutLoop:
				while(cutIndices.size() < linkListMutate.size()-2) {	// while not all possible cut indices have been exploited
//					Log.write("extraLog.txt", "CutIndices attempted="+cutIndices.toString());
					n++;
//					Log.write("extraLog.txt", "n="+n);
					if (n>1000) {
						Log.write("Failing to find a reasonable route switcher. Making no big change. This is no problem.");
						break FindCutLoop; 
					}
					cutIndex = (new Random()).nextInt(linkListMutate.size()-2)+1;
					if (cutIndices.contains(cutIndex)) {
						continue;
					}
					else {
						cutIndices.add(cutIndex);
//						Log.write("extraLog.txt", "Attempting cutIndex="+cutIndex);
					}
//					Log.write("extraLog.txt", "cutIndex worked: "+cutIndex);
					cutOpenLinkId = linkListMutate.get(cutIndex);
					cutOpenLink = globalNetwork.getLinks().get(cutOpenLinkId);
					if ( !(returnAllStopFacilitiesOnLink(metroLinkAttributes, cutOpenLink).size()>0) ) {
						continue;
					}
					Double dist2oldTerminal = 
							GeomDistance.betweenNodes(cutOpenLink.getToNode(), globalNetwork.getLinks().get(linkListMutate.get(linkListMutate.size()-1)).getToNode());
//					Log.write("extraLog.txt", "Distance between old nodes="+dist2oldTerminal);
					// do NOT decide if start or end of route is to be switched (doesn't matter which one is closer - big change can be very big)
					for (CustomStop stopAttr : allMetroStops.values()) {
//						Log.write("extraLog.txt", "Trying facility="+stopAttr.transitStopFacility.toString());
						// use this stopFacility if(terminal is within opening angle)|(in range of prior dist(cut2terminal)*2.5|*0.4)|(shortestPath available)
						Id<Node> stopNodeId = stopAttr.newNetworkNode;
						Node stopNode = globalNetwork.getNodes().get(stopNodeId);
						if (stopNode == null) {
							Log.write("CAUTION XXXXX: newNetworkNode-StopNode not found in globalNetwork. Trying to set terminal to another facility. ");
							Log.write("CAUTION XXXXX: StopNode="+stopNodeId.toString()+"   stopName="+stopAttr.transitStopFacility.getName()+
									"   stopId="+stopAttr.transitStopFacility.getId()+"   stopCoord="+stopAttr.transitStopFacility.getCoord().toString());
							continue;
						}
						Double dist2newTerminal = GeomDistance.betweenNodes(cutOpenLink.getToNode(), stopNode);
//						Log.write("extraLog.txt", "Distance between new nodes="+dist2newTerminal);
//						Log.write("extraLog.txt", "Opening angle = "+Double.toString(180.0-GeomDistance.angleBetweenPoints(globalNetwork.getLinks().get(linkListMutate.get(0)).getFromNode().getCoord(), cutOpenLink.getToNode().getCoord(), stopNode.getCoord())));
						if (0.4*dist2oldTerminal < dist2newTerminal && dist2newTerminal < 2.5*dist2oldTerminal &&
								GeomDistance.angleBetweenPoints(globalNetwork.getLinks().get(
										linkListMutate.get(0)).getFromNode().getCoord(), cutOpenLink.getToNode().getCoord(), stopNode.getCoord()) 
								> 180-thisMaxCrossingAngle(maxCrossingAngle, dist2newTerminal)) {
								// increase in thisMaxCrossingAngle(...) permitted angle if terminal is far away so it is more likely that
								// a smooth turning of the route can be designed
							List<Node> newNodePath = DemoDijkstra.calculateShortestPath(globalNetwork, cutOpenLink.getToNode().getId(), stopNode.getId());
							if (newNodePath == null || newNodePath.size() < 3) {
								continue;
							}
//							Log.write("extraLog.txt", "New node path Dijkstra="+newNodePath.toString());
							List<Link> newLinkPath = NetworkEvolutionImpl.nodeListToNetworkLinkList2(globalNetwork, newNodePath);
							newLinkPath.add(0, cutOpenLink);
//							Log.write("extendedLinkPathOpen undergoing maxTurningAngleCheck = "+extendedLinkPathOpen.toString());
							if (checkIfTurningAnglesOk(maxCrossingAngle, newLinkPath) == false) {
//								Log.write("extraLog.txt", "Turning angles NOT OK! Start from beginning.");
								continue;
							}
							else {
								linkListMutate.removeAll(linkListMutate.subList(cutIndex+1, linkListMutate.size()));
//								Log.write("extraLog.txt", "linkListMutateCut="+linkListMutate.toString());
								List<Id<Link>> newLinkPathIds = NetworkEvolutionImpl.nodeListToNetworkLinkList(globalNetwork, newNodePath);
//								Log.write("extraLog.txt", "newLinkPathIds="+linkListMutate.toString());
								linkListMutate.addAll(newLinkPathIds);
//								Log.write("extraLog.txt", "Success: final linkList="+linkListMutate.toString());
								Log.writeAndDisplay("  > Successul TERMINAL SWITCH for "+mRoute.getId());
								break FindCutLoop;
							}
						}
						else {
							continue;
						}
					}
				}
			}
			else {
				Log.writeAndDisplay("  > Attempting stop insertion for "+mRoute.getId());
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
					boolean foundFeasibleCutCandidate = true;
					do {
						cutIndex = (new Random()).nextInt(linkListMutateFacilityLinks.size()-2)+1;
						cutOpenLinkId = linkListMutateFacilityLinks.get(cutIndex);
						cutOpenLink = globalNetwork.getLinks().get(cutOpenLinkId);
						n++;
						if (n>50) {
	//						Log.write("Failing to find a route stop insertion point within (metro) city. Trying to add in reverse direction.");
							foundFeasibleCutCandidate = false;
							break; 
						}
					} while(GeomDistance.calculate(cutOpenLink.getFromNode().getCoord(), zurich_NetworkCenterCoord) > 5000.0);
					int facilitiesUntilEndTerminal = linkListMutateFacilityLinks.size()-1-cutIndex;
					
					if (foundFeasibleCutCandidate == true) {
						for (int i=1; i<=Math.min(facilitiesUntilEndTerminal, 4.0); i++) {
							Id<Link> cutCloseLinkId = linkListMutateFacilityLinks.get(cutIndex + i);
							Link cutCloseLink = globalNetwork.getLinks().get(cutCloseLinkId);
							if (GeomDistance.calculate(cutCloseLink.getFromNode().getCoord(), cutOpenLink.getFromNode().getCoord()) > 3500) {
								continue;
							}
	//						Log.write("Testing close node nPositions DOWN the route: n="+i);
							List<Id<Link>> reroutedLinkRoute = insertNewStopInRoute(servicedFacilities, cutOpenLinkId, cutCloseLinkId, globalNetwork,
									allMetroStops, linkListMutate, linkListMutateFacilityLinks, maxCrossingAngle);
							if (reroutedLinkRoute == null) {
								continue;
							} else {
								linkListMutate = reroutedLinkRoute;
	//							Log.write("    >> New facility node was inserted successfully!");
								break InsertionLoop;
							}
						}
					}
					
					// do in reverse direction
					List<Id<Link>>linkListMutateReverse = NetworkEvolutionImpl.OppositeLinkListOf(linkListMutate);				
					List<Id<Link>>linkListMutateFacilityLinksReverse = NetworkEvolutionImpl.OppositeLinkListOf(linkListMutateFacilityLinks);
					
					Id<Link> cutOpenLinkIdReverse;
					Link cutOpenLinkReverse;
					int cutIndexReverse;
					n = 0;
					do {
						cutIndexReverse = (new Random()).nextInt(linkListMutateFacilityLinksReverse.size()-2)+1;
						cutOpenLinkIdReverse = linkListMutateFacilityLinksReverse.get(cutIndexReverse);
						cutOpenLinkReverse = globalNetwork.getLinks().get(cutOpenLinkIdReverse);
						n++;
						if (n>50) {
							Log.write("Failing to find a route stop insertion point within (metro) city. Not inserting any node and proceeding to next route.");
							break InsertionLoop; 
						}
					} while(GeomDistance.calculate(cutOpenLinkReverse.getFromNode().getCoord(), zurich_NetworkCenterCoord) > 5000.0);
					int facilitiesUntilEndTerminalReverse = linkListMutateFacilityLinksReverse.size()-1-cutIndexReverse;
					
					for (int i=1; i<=Math.min(facilitiesUntilEndTerminalReverse, 4.0); i++) {
						Id<Link> cutCloseLinkIdReverse = linkListMutateFacilityLinksReverse.get(cutIndexReverse + i);
						Link cutCloseLinkReverse = globalNetwork.getLinks().get(cutCloseLinkIdReverse);
						if (GeomDistance.calculate(cutCloseLinkReverse.getFromNode().getCoord(), cutOpenLinkReverse.getFromNode().getCoord()) > 3500) {
							continue;
						}
						Log.write("Testing close node nPositions UP the route: n="+i);
						List<Id<Link>> reroutedLinkRouteReverse = insertNewStopInRoute(servicedFacilities, cutOpenLinkIdReverse, cutCloseLinkIdReverse, globalNetwork,
								allMetroStops, linkListMutateReverse, linkListMutateFacilityLinksReverse, maxCrossingAngle);
						if (reroutedLinkRouteReverse == null) {
							continue;
						} else {
							linkListMutate = reroutedLinkRouteReverse;
							break InsertionLoop;
						}
					}
					
					// if not succeeded to insert node in for loops by now, no insertion is applied
					Log.write("Failed to insert node. No big problem, but no big mutation has been applied to "+mRoute.routeID);
					break;
				} while(true);
			}
		}
		
		if (linkListMutate.size() == 0) {
			Log.write("ERROR: Mutated Route has Size=0! --> CHECK CODE ...");
		}
		linkListMutate.addAll(NetworkEvolutionImpl.OppositeLinkListOf(linkListMutate));
		mRoute.linkList = Clone.list(linkListMutate);
		mRoute.networkRoute = RouteUtils.createNetworkRoute(linkListMutate, globalNetwork);
		if (linkListMutate.size() < 2) {
			Log.write("CAUTION: RouteLength = " + linkListMutate.size() + " --> Deleting after stop insertion route "+mRoute.routeID);
			mrouteIter.remove();
			return true;
		}
		else if ( ! linkListMutate.equals(originalLinkListMutate)) {
			mRoute.significantRouteModOccured = true;
			Log.writeAndDisplay("  > Susscessul STOP INSERTION for "+mRoute.getId());
			return true;
		}
		else {
			return false;			
		}
	}
	

	public static double thisMaxCrossingAngle(double maxCrossingAngle, Double dist2newTerminal) {
		double permittedAngle = maxCrossingAngle;
		permittedAngle += Math.min(30.0, Math.max(0.0, 30.0*(dist2newTerminal-3000)/3000.0)); // after dist=3000m the permitted angle shall increase by 10°/1000m up to 6000m
		return permittedAngle;
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
//						Log.write("Calculating OPENPATH shortest path between " + cutOpenNode.getId() + "  &  " + insertNodeId);
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
//						Log.write("Calculating CLOSEPATH shortest path between " + insertNodeId + "  &  " + cutCloseNode.getId());
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
						Log.write("   >> Successful node insertion between " + cutOpenLinkId.toString() + " & " + cutCloseLinkId.toString());
//						Log.write("   >> New leg connecting insert node "+insertNodeId.toString() + " is: " + newLegIds.toString());
						return reroutedLinks;
			}
			else {
				continue;
			}
			
		}
		return null;
	}


	public static boolean checkIfTurningAnglesOk(double maxCrossingAngle, List<Link> linkPath) throws IOException {
		if (linkPath.size() == 0) {
			Log.write("Link path size = 0. Therefore, turning angles inherently OK. Returning true.");
			return true;
		}
		Link lastLink = linkPath.get(0);
		for (Link thisLink : linkPath.subList(1, linkPath.size())) {
			if (GeomDistance.angleBetweenLinks(lastLink, thisLink) > maxCrossingAngle) {
				return false;
			}
			lastLink = thisLink;
		}
		return true;
	}
	
	public static boolean checkIfTurningAnglesOkIdOnly(double maxCrossingAngle, List<Id<Link>> linkPathIds, Network network) throws IOException {
		List<Link> linkPath = new ArrayList<Link>();
		for (Id<Link> linkId : linkPathIds) {
			linkPath.add(network.getLinks().get(linkId));
		}
		return checkIfTurningAnglesOk(maxCrossingAngle, linkPath);
	}


	public static List<Id<Link>> dig4facilitiesIN(int lowestTreeLevel, List<Id<Link>> routeLinksIn, double maxCrossingAngle, Network globalNetwork, Map<Id<Link>, CustomMetroLinkAttributes> metroLinkAttributes) throws IOException {
		List<Id<Link>> routeLinks = new ArrayList<Id<Link>>();
		routeLinks.addAll(routeLinksIn);
		Link startLink = globalNetwork.getLinks().get(routeLinks.get(0));
		Node startNode = globalNetwork.getNodes().get(startLink.getFromNode().getId());
		if (lowestTreeLevel == 1) {
			for (Link previousLink : startNode.getInLinks().values()) {	// first for loop to favor rail2metro links for extension (with 80% chance)
				if ( metroLinkAttributes.get(startLink.getId()) != null &&  metroLinkAttributes.get(previousLink.getId()) != null &&
						metroLinkAttributes.get(previousLink.getId()).type.equals("rail2newMetro") && (new Random()).nextDouble()<0.8  &&
						searchAcceptableStopFacilitiesOnLink(metroLinkAttributes, previousLink, metroLinkAttributes.get(startLink.getId()).fromNodeStopFacility) != null) {
					// second condition makes sure that a link is not added, which has same facility on FromNode as end link on ToNode given that they touch.
					routeLinks.add(previousLink.getId());
					return routeLinks;
				}
			}
			for (Link previousLink : startNode.getInLinks().values()) {
				if (GeomDistance.angleBetweenLinks(previousLink, startLink) > maxCrossingAngle || metroLinkAttributes.get(startLink.getId()) == null) {
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
			for (Link nextLink : endNode.getOutLinks().values()) {	// first for loop to favor rail2metro links for extension (with 80% chance)
				if (metroLinkAttributes.get(endLink.getId()) != null && metroLinkAttributes.get(nextLink.getId()) != null && 
						metroLinkAttributes.get(nextLink.getId()).type.equals("rail2newMetro") && (new Random()).nextDouble()<0.8  &&
						searchAcceptableStopFacilitiesOnLink(metroLinkAttributes, nextLink, metroLinkAttributes.get(endLink.getId()).toNodeStopFacility) != null) {
					// second condition makes sure that a link is not added, which has same facility on FromNode as end link on ToNode given that they touch.
					routeLinks.add(nextLink.getId());
					return routeLinks;
				}
			}
			for (Link nextLink : endNode.getOutLinks().values()) {	// second for loop to consider any links for extension (first hit with 100% chance)
				if (GeomDistance.angleBetweenLinks(nextLink, endLink) > maxCrossingAngle || metroLinkAttributes.get(endLink.getId()) == null) {
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
