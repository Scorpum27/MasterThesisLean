package ch.ethz.matsim.students.samark;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.vehicles.VehicleType;
import com.google.common.collect.Sets;

import ch.ethz.matsim.baseline_scenario.transit.routing.DefaultEnrichedTransitRoute;
import ch.ethz.matsim.baseline_scenario.transit.routing.DefaultEnrichedTransitRouteFactory;

public class NetworkEvolutionImpl {

	public static MNetwork createMNetworkRoutes(String thisNewNetworkName, int routesPerNetwork, String initialRouteType, int iterationToReadOriginalNetwork,
			double minMetroRadiusFromCenter, double maxMetroRadiusFromCenter, Coord zurich_NetworkCenterCoord, double metroCityRadius, 
			int nMostFrequentLinks, double maxNewMetroLinkDistance, double minTerminalRadiusFromCenter, double maxTerminalRadiusFromCenter,
			double minTerminalDistance, double odConsiderationThreshold, double xOffset, double yOffset,String vehicleTypeName, double vehicleLength, double maxVelocity,
			int vehicleSeats, int vehicleStandingRoom,String defaultPtMode, boolean blocksLane, double stopTime, double maxVehicleSpeed,
			double tFirstDep, double tLastDep, double depSpacing, int nDepartures,
			double metroOpsCostPerKM, double metroConstructionCostPerKmOverground, double metroConstructionCostPerKmUnderground ) {

		MNetwork mNetwork = new MNetwork(thisNewNetworkName);
		String mNetworkPath = "zurich_1pm/Evolution/Population/"+thisNewNetworkName;
		new File(mNetworkPath).mkdirs();
		
		Config originalConfig = ConfigUtils.loadConfig("zurich_1pm/zurich_config.xml");
		Scenario originalScenario = ScenarioUtils.loadScenario(originalConfig);
		originalScenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DefaultEnrichedTransitRoute.class, new DefaultEnrichedTransitRouteFactory());
		Network originalNetwork = originalScenario.getNetwork();
		TransitSchedule originalTransitSchedule = originalScenario.getTransitSchedule();

		// Initialize a customLinkMap with all links from original network
		Map<Id<Link>, CustomLinkAttributes> allOriginalLinks = NetworkEvolutionImpl.createCustomLinkMap(originalNetwork, null);
		
		// Run event handler to count movements on each stop facility of original map and add traffic data to customLinkMap
		Map<Id<Link>, CustomLinkAttributes> trafficProcessedLinkMap = NetworkEvolutionImpl.runPTStopTrafficScanner(
				new PT_StopTrafficCounter(), allOriginalLinks, iterationToReadOriginalNetwork, originalNetwork, null);

		// Select all metro candidate links by setting bounds on their location (distance from city center)	
		Map<Id<Link>, CustomLinkAttributes> links_withinRadius = NetworkEvolutionImpl.findLinksWithinBounds(
				trafficProcessedLinkMap , originalNetwork, zurich_NetworkCenterCoord, minMetroRadiusFromCenter, maxMetroRadiusFromCenter,
				null); // FOR SAVING: replace (null) by (mNetworkPath+"/1_WithinRadius" + ((int) Math.round(metroCityRadius)) + ".xml")

		// Find most frequent links from input links
		Map<Id<Link>, CustomLinkAttributes> links_mostFrequentInRadius = 
				NetworkEvolutionImpl.findMostFrequentLinks(nMostFrequentLinks, links_withinRadius, originalNetwork, null);

		// Set dominant transit stop facility in given network (from custom link list)
		Map<Id<Link>, CustomLinkAttributes> links_mostFrequentInRadiusMainFacilitiesSet = NetworkEvolutionImpl.setMainFacilities(originalTransitSchedule, 
				originalNetwork, links_mostFrequentInRadius, null); // FOR SAVING: replace (null) by (mNetworkPath+"/2_MostFrequentInRadius.xml")

		// Create a metro network from candidate links/stopFaiclities
		Network metroNetwork = NetworkEvolutionImpl.createMetroNetworkFromCandidates(
				links_mostFrequentInRadiusMainFacilitiesSet, maxNewMetroLinkDistance, originalNetwork, 
				null); // FOR SAVING: replace (null) by (mNetworkPath+"/4_MetroNetwork.xml"))
		
		// STORE GLOBAL NETWORK WITH ALL METRO LINKS
		Metro_TransitScheduleImpl.mergeRoutesNetworkToOriginalNetwork(metroNetwork, originalNetwork,
				Sets.newHashSet("pt"), "zurich_1pm/Evolution/Population/GlobalMetroNetwork.xml");				
				
		// %%% Everything in old system up to here %%% //
		// CONVERSIONS:
		// 	Get [new map] node from [old map] refLink: Node newMapNode = newNetwork.getNodes.get(Id.createNodeId("MetroNodeLinkRef_"+oldMapRefLink.toString()))
		// 	---> Id<Node> metroNodeId = metroNodeFromOriginalLink(Id<Link> originalLinkRefID) 
		// 	Get [old map] refLink from [new map] node: Link oldMapLink = newMapNode.parse
		// 	---> Id<Link> originalLinkId = orginalLinkFromMetroNode(Id<Node> metroNodeId)

		boolean useOdPairsForInitialRoutes = false;
		if (initialRouteType.equals("OD")) {
			useOdPairsForInitialRoutes = true;
		}
		ArrayList<NetworkRoute> initialMetroRoutes = null;
		Network separateRoutesNetwork = null;
		if (useOdPairsForInitialRoutes==false) {								
			// Initial Routes random terminals within bounds and min dist apart:
			// Select all metro term. cand. by setting bounds on their loc. (dist. from center)
			Map<Id<Link>, CustomLinkAttributes> links_MetroTerminalCandidates = NetworkEvolutionImpl.findLinksWithinBounds(links_mostFrequentInRadiusMainFacilitiesSet, 
					originalNetwork, zurich_NetworkCenterCoord, minTerminalRadiusFromCenter, maxTerminalRadiusFromCenter, 
					null); // FOR SAVING: replace (null) by (mNetworkPath+"/3_MetroTerminalCandidate.xml"));
			initialMetroRoutes = NetworkEvolutionImpl.createInitialRoutesRandom(metroNetwork, links_MetroTerminalCandidates, routesPerNetwork, minTerminalDistance);			
			separateRoutesNetwork = NetworkEvolutionImpl.networkRoutesToNetwork(initialMetroRoutes, metroNetwork,
					Sets.newHashSet("pt"), (mNetworkPath+"/0_MetroInitialRoutes_Random.xml"));
		}
		if (useOdPairsForInitialRoutes==true) {	
			// Initial Routes OD_Pairs within bounds	
			initialMetroRoutes = OD_ProcessorImpl.createInitialRoutesOD(metroNetwork, routesPerNetwork, minTerminalRadiusFromCenter,
					maxTerminalRadiusFromCenter, odConsiderationThreshold, zurich_NetworkCenterCoord, "zurich_1pm/Evolution/Input/Data/OD_Input/Demand2013_PT.csv",
					"zurich_1pm/Evolution/Input/Data/OD_Input/OD_ZoneCodesLocations.csv", xOffset, yOffset);	
					// CAUTION: Make sure .csv is separated by semi-colon because location names also include commas sometimes and lead to failure!!			
			separateRoutesNetwork = NetworkEvolutionImpl.networkRoutesToNetwork(initialMetroRoutes, metroNetwork, Sets.newHashSet("pt"),
					(mNetworkPath+"/0_MetroInitialRoutes_OD.xml"));
		}
				
		// Load & Create Schedules and Factories
		Config newConfig = ConfigUtils.createConfig();						// this is totally default and may be modified as required
		Scenario newScenario = ScenarioUtils.createScenario(newConfig);
		TransitSchedule newSchedule = newScenario.getTransitSchedule();
		TransitScheduleFactory metroScheduleFactory = newSchedule.getFactory();
				
		// Create a New Metro Vehicle
		VehicleType metroVehicleType = Metro_TransitScheduleImpl.createNewVehicleType(vehicleTypeName, vehicleLength, maxVelocity, vehicleSeats, vehicleStandingRoom);
					newScenario.getTransitVehicles().addVehicleType(metroVehicleType);
				
		// Generate TransitLines and Schedules on NetworkRoutes --> Add to Transit Schedule
		int nTransitLines = initialMetroRoutes.size();
		for(int lineNr=1; lineNr<=nTransitLines; lineNr++) {
				
			// NetworkRoute
			NetworkRoute metroNetworkRoute = initialMetroRoutes.get(lineNr-1);
			MRoute mRoute = new MRoute(thisNewNetworkName+"_Route"+lineNr);
			mRoute.departureSpacing = depSpacing;
			mRoute.firstDeparture = tFirstDep;
			mRoute.nDepartures = nDepartures;
			mRoute.setNetworkRoute(metroNetworkRoute);
			mNetwork.addNetworkRoute(mRoute);
			
			// Create an array of stops along new networkRoute on the center of each of its individual links
			List<TransitRouteStop> stopArray = Metro_TransitScheduleImpl.createAndAddNetworkRouteStops(
							newSchedule, metroNetwork, metroNetworkRoute, defaultPtMode, stopTime, maxVehicleSpeed, blocksLane);
			
			
			// Build TransitRoute from stops and NetworkRoute --> and add departures
			String vehicleFileLocation = (mNetworkPath+"/Vehicles.xml");
			TransitRoute transitRoute = metroScheduleFactory.createTransitRoute(Id.create(thisNewNetworkName+"_Route"+lineNr, TransitRoute.class ), 
					metroNetworkRoute, stopArray, defaultPtMode);
			double totalRouteTravelTime = stopArray.get(stopArray.size()-1).getArrivalOffset();
			transitRoute = Metro_TransitScheduleImpl.addDeparturesAndVehiclesToTransitRoute(newScenario, newSchedule, transitRoute,
					mRoute.nDepartures, mRoute.firstDeparture, mRoute.departureSpacing, totalRouteTravelTime, metroVehicleType, vehicleFileLocation); // Add (nDepartures) departures to TransitRoute
								
			// Build TransitLine from TrasitRoute
			TransitLine transitLine = metroScheduleFactory.createTransitLine(Id.create("TransitLine_Nr"+lineNr, TransitLine.class));
			transitLine.addRoute(transitRoute);
			
			// Add new line to schedule
			newSchedule.addTransitLine(transitLine);			
		
			mRoute.setTransitLine(transitLine);
			mRoute.setLinkList(NetworkRoute2LinkIdList(metroNetworkRoute));
			mRoute.setNodeList(NetworkRoute2NodeIdList(metroNetworkRoute, metroNetwork));
			mRoute.setRouteLength(NetworkRoute2TotalLength(metroNetworkRoute, metroNetwork));
			mRoute.nDepartures = nDepartures;
			mRoute.setDrivenKM(mRoute.routeLength*mRoute.nDepartures);
			mRoute.constrCost = mRoute.routeLength*(metroConstructionCostPerKmOverground*0.01*(100-mRoute.undergroundPercentage)+metroConstructionCostPerKmUnderground*0.01*mRoute.undergroundPercentage);
			mRoute.opsCost = mRoute.routeLength*(metroOpsCostPerKM*0.01*(100-mRoute.undergroundPercentage)+2*metroOpsCostPerKM*0.01*mRoute.undergroundPercentage);
			mRoute.transitScheduleFile = mNetworkPath+"/MetroSchedule.xml";
			mRoute.setEventsFile( "zurich_1pm/Zurich_1pm_SimulationOutput/ITERS/it." + iterationToReadOriginalNetwork +
					"/" + iterationToReadOriginalNetwork + ".events.xml.gz");
			
		}	// end of TransitLine creator loop

		// Write TransitSchedule to corresponding file
		TransitScheduleWriter tsw = new TransitScheduleWriter(newSchedule);
		tsw.writeFile(mNetworkPath+"/MetroSchedule.xml");
				
		String mergedNetworkFileName = "";
		if (useOdPairsForInitialRoutes==true) {
			mergedNetworkFileName = (mNetworkPath+"/OriginalNetwork_with_ODInitialRoutes.xml");
		}
		else {
			mergedNetworkFileName = (mNetworkPath+"/OriginalNetwork_with_RandomInitialRoutes.xml");
		}
		//Network mergedNetwork = ...
				Metro_TransitScheduleImpl.mergeRoutesNetworkToOriginalNetwork(separateRoutesNetwork, originalNetwork, Sets.newHashSet("pt"), mergedNetworkFileName);
		//TransitSchedule mergedTransitSchedule = ...
				Metro_TransitScheduleImpl.mergeAndWriteTransitSchedules(newSchedule, originalTransitSchedule, (mNetworkPath+"/MergedSchedule.xml"));
		//Vehicles mergedVehicles = ...
				Metro_TransitScheduleImpl.mergeAndWriteVehicles(newScenario.getTransitVehicles(), originalScenario.getTransitVehicles(), (mNetworkPath+"/MergedVehicles.xml"));
		
				// FOR DIRECT DATA TRANSPORT W/O SAVING TO FILES - fill in MNetwork Objects for this Network
//		mNetwork.network = mergedNetwork;
//		mNetwork.transitSchedule = mergedTransitSchedule;
//		mNetwork.vehicles = mergedVehicles;
		return mNetwork;
	}		
		
	
// %%%%%%%%%%%%%%%%%%%%%% HELPER METHODS STATIC %%%%%%%%%%%%%%%%%%%%%%%%%

	// done
		public static Map<Id<Link>, CustomLinkAttributes> createCustomLinkMap(Network network, String fileName) {
			Map<Id<Link>, CustomLinkAttributes> customLinkMap = new HashMap<Id<Link>, CustomLinkAttributes>(
					network.getLinks().size());
			Iterator<Id<Link>> iterator = network.getLinks().keySet().iterator(); // take network and put all links in
																					// linkTrafficMap
			while (iterator.hasNext()) {
				Id<Link> thisLinkID = iterator.next();
				customLinkMap.put(thisLinkID, new CustomLinkAttributes()); // - initiate traffic with default attributes
			}

			if (fileName != null) {
				createNetworkFromCustomLinks(customLinkMap, network, fileName);
			}

			return customLinkMap;
		}
		

		public static Map<Id<Link>, CustomLinkAttributes> findLinksAboveThreshold(Network network, double threshold,
				Map<Id<Link>, CustomLinkAttributes> customLinkMapIn, String fileName) { // output is a map with all links
																						// above threshold and their traffic
																						// (number of link enter events)

			// make a custom linkMap and initialize with all network links
			Map<Id<Link>, CustomLinkAttributes> customLinkMap = copyCustomMap(customLinkMapIn);

			// remove all links below threshold
			Map<Id<Link>, CustomLinkAttributes> linksAboveThreshold = new HashMap<Id<Link>, CustomLinkAttributes>();
			Iterator<Entry<Id<Link>, CustomLinkAttributes>> thresholdIterator = customLinkMap.entrySet().iterator();
			while (thresholdIterator.hasNext()) {
				Entry<Id<Link>, CustomLinkAttributes> entry = thresholdIterator.next();
				if (threshold <= entry.getValue().getTotalTraffic()) {
					linksAboveThreshold.put(entry.getKey(), entry.getValue());
				}
			}
			// double average = getAverageTrafficOnLinks(linksAboveThreshold);
//			System.out.println("Average pt traffic on links (person arrivals + departures) is: " + average);
//			System.out.println("Number of links above threshold is: " + linksAboveThreshold.size());

			if (fileName != null) {
				createNetworkFromCustomLinks(linksAboveThreshold, network, fileName);
			}

			return linksAboveThreshold;
		}

		public static Map<Id<Link>, CustomLinkAttributes> findMostFrequentLinks(int nMostFrequentLinks,
				Map<Id<Link>, CustomLinkAttributes> customLinkMap, Network network, String fileName) {
			// output is a map with all links above threshold and their traffic (number of link enter events)

			// add links if they are within top nMostFrequentlinks
			Map<Id<Link>, CustomLinkAttributes> mostFrequentLinks = new HashMap<Id<Link>, CustomLinkAttributes>( nMostFrequentLinks);
			int i = 0;
			for (Id<Link> linkID : customLinkMap.keySet()) {
				mostFrequentLinks.put(linkID, customLinkMap.get(linkID));
				i++;
				if (i == nMostFrequentLinks) {
					break;
				}
			}

			// add other links from customLinkMap if they have more traffic than previous
			// minimum link
			for (Id<Link> linkID : customLinkMap.keySet()) {
				Id<Link> minTrafficLinkID = minimumTrafficLink(mostFrequentLinks);
				Double minTraffic = mostFrequentLinks.get(minimumTrafficLink(mostFrequentLinks)).getTotalTraffic();
				if (customLinkMap.get(linkID).getTotalTraffic() > minTraffic
						&& mostFrequentLinks.containsKey(linkID) == false) {
					mostFrequentLinks.put(linkID, customLinkMap.get(linkID));
					mostFrequentLinks.remove(minTrafficLinkID);
				}
			}
			// calculate and display average
			// double average = getAverageTrafficOnLinks(mostFrequentLinks);
//			System.out.println("Average pt traffic on most frequent n=" + nMostFrequentLinks
//					+ " links (person arrivals + departures) is: " + average);
//			System.out.println("Number of most frequent links is: " + mostFrequentLinks.size());

			if (fileName != null) {
				createNetworkFromCustomLinks(mostFrequentLinks, network, fileName);
			}

			return mostFrequentLinks;
		}

		public static Id<Link> minimumTrafficLink(Map<Id<Link>, CustomLinkAttributes> linkSet) {
			double minTraffic = Double.MAX_VALUE;
			Id<Link> minLinkID = null;
			for (Id<Link> linkID : linkSet.keySet()) {
				if (linkSet.get(linkID).getTotalTraffic() < minTraffic) {
					minTraffic = linkSet.get(linkID).getTotalTraffic();
					minLinkID = linkID;
				}
			}
			return minLinkID;
		}

		public static Map<Id<Link>, CustomLinkAttributes> runPTStopTrafficScanner(
				PT_StopTrafficCounter myPT_StopTrafficCounter, Map<Id<Link>, CustomLinkAttributes> emptyCustomLinkMap,
				int iterationToRead, Network network, String fileName) {

			myPT_StopTrafficCounter.CustomLinkMap = copyCustomMap(emptyCustomLinkMap);
			EventsManager myEventsManager = EventsUtils.createEventsManager();
			myEventsManager.addHandler(myPT_StopTrafficCounter);
			MatsimEventsReader reader = new MatsimEventsReader(myEventsManager);
			String eventsFile = "zurich_1pm/Zurich_1pm_SimulationOutput/ITERS/it." + iterationToRead + "/" + iterationToRead
					+ ".events.xml.gz";
			reader.readFile(eventsFile);

			if (fileName != null) {
				createNetworkFromCustomLinks(myPT_StopTrafficCounter.CustomLinkMap, network, fileName);
			}

			return myPT_StopTrafficCounter.CustomLinkMap;
		}

		public static Map<Id<Link>, CustomLinkAttributes> setMainFacilities(TransitSchedule transitSchedule,
				Network network, Map<Id<Link>, CustomLinkAttributes> selectedLinksIn, String fileName) {

			Map<Id<Link>, CustomLinkAttributes> selectedLinks = copyCustomMap(selectedLinksIn);

			// Go through all facilities and whenever a stop facility refers to a selected
			// link, associate that stop facility with that link
			// Check its transport mode and - if a mode exists already - associate only if
			// this transport mode has the bigger facility than the one before (rail > bus)
			// How to check transport mode of a facility:
			// - Go through all lines --> all routes --> all stops
			// - Check if stops contain the link of question (each selectedLink)
			// - if yes, assess that transportMode e.g. "rail" in transitRoute.transitMode
			// and return the mode for the selected link!

			LinkLoop: for (Id<Link> selectedLinkID : selectedLinks.keySet()) {
				boolean facilityFound = false;
				for (TransitLine transitLine : transitSchedule.getTransitLines().values()) {
					for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
						for (TransitRouteStop transitRouteStop : transitRoute.getStops()) {
							if (transitRouteStop.getStopFacility().getLinkId() == selectedLinkID) {
								facilityFound = true;
								String mode = transitRoute.getTransportMode();
								// System.out.println("Mode on detected transit stop facility is |"+mode+"|");
								if (mode == "rail") {
									CustomLinkAttributes updatedAttributes = selectedLinks.get(selectedLinkID);
									updatedAttributes.setDominantMode(mode);
									updatedAttributes.setDominantStopFacility(transitRouteStop.getStopFacility());
									selectedLinks.put(selectedLinkID, updatedAttributes);
									// System.out.println("Added mode: "+mode);
									continue LinkLoop; // if mode is rail, we set the default to rail bc it is most dominant
														// and move to next link (--> this link is completed)
								} else if (mode == "tram") {
									CustomLinkAttributes updatedAttributes = selectedLinks.get(selectedLinkID);
									updatedAttributes.setDominantMode(mode);
									updatedAttributes.setDominantStopFacility(transitRouteStop.getStopFacility());
									selectedLinks.put(selectedLinkID, updatedAttributes);
									// System.out.println("Added mode: "+mode);

								} else if (mode == "bus") {
									if (selectedLinks.get(selectedLinkID).getDominantMode() == null
											|| selectedLinks.get(selectedLinkID).getDominantMode() == "funicular") {
										CustomLinkAttributes updatedAttributes = selectedLinks.get(selectedLinkID);
										updatedAttributes.setDominantMode(mode);
										updatedAttributes.setDominantStopFacility(transitRouteStop.getStopFacility());
										selectedLinks.put(selectedLinkID, updatedAttributes);
										// System.out.println("Added mode: "+mode);
									}
								} else if (mode == "funicular") {
									if (selectedLinks.get(selectedLinkID).getDominantMode() == null) {
										CustomLinkAttributes updatedAttributes = selectedLinks.get(selectedLinkID);
										updatedAttributes.setDominantMode(mode);
										updatedAttributes.setDominantStopFacility(transitRouteStop.getStopFacility());
										selectedLinks.put(selectedLinkID, updatedAttributes);
										// System.out.println("Added mode: "+mode);
									}
								} else {
//									System.out.println("Did not recognize mode: " + mode + ", but adding it anyways...");
									CustomLinkAttributes updatedAttributes = selectedLinks.get(selectedLinkID);
									updatedAttributes.setDominantMode(mode);
									updatedAttributes.setDominantStopFacility(transitRouteStop.getStopFacility());
									selectedLinks.put(selectedLinkID, updatedAttributes);
								}

							}
						}
					}
				}
				if (facilityFound==false) {
//					System.out.println("Link "+selectedLinkID+" has no facility attached.");
				}
			}

			if (fileName != null) {
				createNetworkFromCustomLinks(selectedLinks, network, fileName);
			}
			return selectedLinks;
		}

		public static Map<Id<Link>, CustomLinkAttributes> findLinksWithinBounds(
				Map<Id<Link>, CustomLinkAttributes> customLinkMap, Network network, Coord networkCenterCoord,
				double minRadiusFromCenter, double maxRadiusFromCenter, String fileName) {

			Map<Id<Link>, CustomLinkAttributes> feasibleLinks = copyCustomMap(customLinkMap);
			double distanceFromCenter = 0.0;
			Iterator<Id<Link>> linkIterator = feasibleLinks.keySet().iterator();
			while (linkIterator.hasNext()) {
				Id<Link> thisLinkID = linkIterator.next();
				// calculate distance with FromNode;
				distanceFromCenter = GeomDistance.calculate(network.getLinks().get(thisLinkID).getFromNode().getCoord(),
						networkCenterCoord);
				if (distanceFromCenter < minRadiusFromCenter || distanceFromCenter > maxRadiusFromCenter) {
					linkIterator.remove();
				}
			}
//			System.out.println("Size is: " + feasibleLinks.size());
			if (fileName != null) {
				createNetworkFromCustomLinks(feasibleLinks, network, fileName);
			}

			return feasibleLinks;
		}

		public static Network createMetroNetworkFromCandidates(Map<Id<Link>, CustomLinkAttributes> customLinkMap,
				double maxNewMetroLinkDistance, Network mergerNetwork, String fileName) {
			Config config = ConfigUtils.createConfig();
			Scenario scenario = ScenarioUtils.createScenario(config);
			Network newNetwork = scenario.getNetwork();
			NetworkFactory networkFactory = newNetwork.getFactory();

			// Initiate all nodes of facility location (in customLinkMap) with their names
			// as from mergerNetwork
			Node newNode;
			Link newLink;
			Map<Id<Node>, Id<Link>> metroNodeLinkReferences = new HashMap<Id<Node>, Id<Link>>(customLinkMap.size());
			for (Id<Link> linkID : customLinkMap.keySet()) {
				//System.out.println("linkID is: "+linkID.toString() );
				//System.out.println("Custom attributes are: "+customLinkMap.get(linkID).toString());
				/*System.out.println("Custom attributes dominant mode: "+customLinkMap.get(linkID).getDominantMode());
				System.out.println("Custom attributes facility: "+customLinkMap.get(linkID).getDominantStopFacility().getName());
				System.out.println("Custom attributes coord: "+customLinkMap.get(linkID).getDominantStopFacility().getCoord());*/
				if (customLinkMap.get(linkID).getDominantStopFacility()==null) {
					newNode = networkFactory.createNode(Id.createNodeId("MetroNodeLinkRef_" + linkID.toString()), 
							mergerNetwork.getLinks().get(linkID).getFromNode().getCoord());
				}
				else {
					newNode = networkFactory.createNode(Id.createNodeId("MetroNodeLinkRef_" + linkID.toString()),
							customLinkMap.get(linkID).dominantStopFacility.getCoord());					
				}
				metroNodeLinkReferences.put(newNode.getId(), linkID);
				//System.out.println("New node is called: " + newNode.getId().toString());
				//System.out.println("Node counter l= " + l);
				newNetwork.addNode(newNode);
			}

			// Create links in network --> for every node:
			for (Node thisNode : newNetwork.getNodes().values()) {
				for (Node otherNode : newNetwork.getNodes().values()) {
					if (thisNode == otherNode) {
						continue;
					}
					// add NEW links (with appropriate naming method) to all nodes within a specific
					// radius
					else if (GeomDistance.betweenNodes(thisNode, otherNode) < maxNewMetroLinkDistance) {
						newLink = networkFactory.createLink(
								Id.createLinkId(thisNode.getId().toString() + "_" + otherNode.getId().toString()), thisNode,
								otherNode);
						newNetwork.addLink(newLink);
					}
					// add NEW links if refLink of other facility node was on a next link to the
					// link of this facility (an outLink of toNode of this node's refLink)
					else if (mergerNetwork.getLinks().get(metroNodeLinkReferences.get(thisNode.getId())).getToNode()
							.getOutLinks().containsKey(metroNodeLinkReferences.get(otherNode.getId()))) {
						newLink = networkFactory.createLink(
								Id.createLinkId(thisNode.getId().toString() + "_" + otherNode.getId().toString()), thisNode,
								otherNode);
						newNetwork.addLink(newLink);
					}
				}

			}

			if (fileName != null) {
				NetworkWriter networkWriter = new NetworkWriter(newNetwork);
				networkWriter.write(fileName);
			}

			return newNetwork;
		}

		public static Map<Id<Link>, CustomLinkAttributes> copyCustomMap(Map<Id<Link>, CustomLinkAttributes> customMap) {
			Map<Id<Link>, CustomLinkAttributes> customMapCopy = new HashMap<Id<Link>, CustomLinkAttributes>();
			for (Entry<Id<Link>, CustomLinkAttributes> entry : customMap.entrySet()) {
				customMapCopy.put(entry.getKey(), entry.getValue());
			}
			return customMapCopy;
		}

		// REMEMBER: New nodes are named "MetroNodeLinkRef_"+linkID.toString()
		public static ArrayList<NetworkRoute> createInitialRoutesRandom(Network newMetroNetwork,
				Map<Id<Link>, CustomLinkAttributes> links_MetroTerminalCandidates, int nRoutes, double minTerminalDistance) {

			ArrayList<NetworkRoute> networkRouteArray = new ArrayList<NetworkRoute>();

			// make nRoutes new routes
			Id<Node> terminalNode1 = null;
			Id<Node> terminalNode2 = null;
			
			OuterNetworkRouteLoop:
			while (networkRouteArray.size() < nRoutes) {

				// choose two random terminals
				Id<Link> randomTerminalLinkId1 = getRandomLink(links_MetroTerminalCandidates.keySet());
				terminalNode1 = Id.createNodeId("MetroNodeLinkRef_" + randomTerminalLinkId1.toString());
				if (newMetroNetwork.getNodes().keySet().contains(terminalNode1) == false) {
//					System.out.println("Terminal node 1 is not featured in new network: ");
				}
				int safetyCounter = 0;
				int iterLimit = 10000;
				do {
					Id<Link> randomTerminalLinkId2 = getRandomLink(links_MetroTerminalCandidates.keySet());
					terminalNode2 = Id.createNodeId("MetroNodeLinkRef_" + randomTerminalLinkId2.toString());
					safetyCounter++;
					if (safetyCounter == iterLimit) {
//						System.out.println("Oops no second terminal node found after " + iterLimit + " iterations. Trying to create next networkRoute. "
//								+ "Please lower minTerminalDistance!");
						continue OuterNetworkRouteLoop;
					}
				} while (GeomDistance.calculate(newMetroNetwork.getNodes().get(terminalNode1).getCoord(),
						newMetroNetwork.getNodes().get(terminalNode2).getCoord()) < minTerminalDistance
						&& safetyCounter < iterLimit);

				if (newMetroNetwork.getNodes().keySet().contains(terminalNode2) == false) {
//					System.out.println("Terminal node 2 is not featured in new network: ");
				}

				// Find Djikstra --> nodeList
				ArrayList<Node> nodeList = DijkstraOwn_I.findShortestPathVirtualNetwork(newMetroNetwork, terminalNode1,
						terminalNode2);
				if (nodeList == null) {
//						System.out.println("Oops, no shortest path available. Trying to create next networkRoute. Please lower minTerminalDistance"
//								+ " ,or increase maxNewMetroLinkDistance (and - last - increase nMostFrequentLinks if required)!");
//						System.out.println("Distance between terminals is "+GeomDistance.betweenNodes(newMetroNetwork.getNodes().get(terminalNode1), newMetroNetwork.getNodes().get(terminalNode2)));
//						System.out.println("Coord of terminal1 is "+newMetroNetwork.getNodes().get(terminalNode1).getCoord());
//						System.out.println("Coord of terminal2 is "+newMetroNetwork.getNodes().get(terminalNode2).getCoord());
						continue OuterNetworkRouteLoop;
				}
				List<Id<Link>> linkList = nodeListToNetworkLinkList(newMetroNetwork, nodeList);
				linkList.addAll(OppositeLinkListOf(linkList)); // extend linkList with its opposite direction for PT transportation!
				NetworkRoute networkRoute = RouteUtils.createNetworkRoute(linkList, newMetroNetwork);

//				System.out.println("The new networkRoute is: [Length="+(networkRoute.getLinkIds().size()+2)+"] - " + networkRoute.toString());
				networkRouteArray.add(networkRoute);
			}

			// Doing already in main file --> Not necessary to do here again:
			// Store all new networkRoutes in a separate network file for visualization
			// --> networkRoutesToNetwork(networkRouteArray, newMetroNetwork, fileName);
			return networkRouteArray;
		}

		public static Network networkRoutesToNetwork(ArrayList<NetworkRoute> networkRoutes, Network network, Set<String> networkRouteModes, String fileName) {
			// Store all new networkRoutes in a separate network file for visualization
				Network routesNetwork = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getNetwork();
				NetworkFactory networkFactory = routesNetwork.getFactory();
				for (NetworkRoute nR : networkRoutes) {
					List<Id<Link>> routeLinkList = new ArrayList<Id<Link>>();
					routeLinkList.add(nR.getStartLinkId());
					routeLinkList.addAll(nR.getLinkIds());
					routeLinkList.add(nR.getEndLinkId());
					for (Id<Link> linkID : routeLinkList) {
						Node tempToNode = networkFactory.createNode(network.getLinks().get(linkID).getToNode().getId(),
								network.getLinks().get(linkID).getToNode().getCoord());
						Node tempFromNode = networkFactory.createNode( network.getLinks().get(linkID).getFromNode().getId(),
								network.getLinks().get(linkID).getFromNode().getCoord());
						Link tempLink = networkFactory.createLink(network.getLinks().get(linkID).getId(), tempFromNode, tempToNode);
						tempLink.setAllowedModes(networkRouteModes);
						if (routesNetwork.getNodes().containsKey(tempToNode.getId()) == false) {
							routesNetwork.addNode(tempToNode);
						}
						if (routesNetwork.getNodes().containsKey(tempFromNode.getId()) == false) {
							routesNetwork.addNode(tempFromNode);
						}
						if (routesNetwork.getLinks().containsKey(tempLink.getId()) == false) {
							routesNetwork.addLink(tempLink);
						}
					}
				}
			NetworkWriter initialRoutesNetworkWriter = new NetworkWriter(routesNetwork);
			initialRoutesNetworkWriter.write(fileName);
			
			return routesNetwork;
		}
		
		public static Network NetworkRouteToNetwork(NetworkRoute networkRoute, Network metroNetwork, Set<String> networkRouteModes, String fileName) {
			// Store all new networkRoutes in a separate network file for visualization
				Network routesNetwork = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getNetwork();
				NetworkFactory networkFactory = routesNetwork.getFactory();
					List<Id<Link>> routeLinkList = new ArrayList<Id<Link>>();
					routeLinkList.add(networkRoute.getStartLinkId());
					routeLinkList.addAll(networkRoute.getLinkIds());
					routeLinkList.add(networkRoute.getEndLinkId());
					for (Id<Link> linkID : routeLinkList) {
						Node tempToNode = networkFactory.createNode(metroNetwork.getLinks().get(linkID).getToNode().getId(),
								metroNetwork.getLinks().get(linkID).getToNode().getCoord());
						Node tempFromNode = networkFactory.createNode( metroNetwork.getLinks().get(linkID).getFromNode().getId(),
								metroNetwork.getLinks().get(linkID).getFromNode().getCoord());
						Link tempLink = networkFactory.createLink(metroNetwork.getLinks().get(linkID).getId(), tempFromNode, tempToNode);
						tempLink.setAllowedModes(networkRouteModes);
						if (routesNetwork.getNodes().containsKey(tempToNode.getId()) == false) {
							routesNetwork.addNode(tempToNode);
						}
						if (routesNetwork.getNodes().containsKey(tempFromNode.getId()) == false) {
							routesNetwork.addNode(tempFromNode);
						}
						if (routesNetwork.getLinks().containsKey(tempLink.getId()) == false) {
							routesNetwork.addLink(tempLink);
						}
					}
			NetworkWriter initialRoutesNetworkWriter = new NetworkWriter(routesNetwork);
			initialRoutesNetworkWriter.write(fileName);
			return routesNetwork;
		}
		
		public static Id<Link> getRandomLink(Set<Id<Link>> linkSet) {
			Random rand = new Random();
			int rInt = rand.nextInt(linkSet.size());
			int linkCount = 0;
			for (Id<Link> linkID : linkSet) {
				if (linkCount == rInt) {
					// System.out.println("Returning random Id<Link> "+linkID);
					return linkID;
				}
				linkCount++;
			}
//			System.out.println("Something strange happened. Returning /null/ ...");
			return null;
		}

		public static Id<Node> getRandomNode(Set<Id<Node>> nodeSet) {
			Random rand = new Random();
			int nInt = rand.nextInt(nodeSet.size());
			int nodeCount = 0;
			for (Id<Node> nodeID : nodeSet) {
				if (nodeCount == nInt) {
//					System.out.println("Returning Id<Link> " + nodeID);
					return nodeID;
				}
				nodeCount++;
			}
//			System.out.println("Something strange happended. Returning /null/ ...");
			return null;
		}

		public static List<Id<Link>> nodeListToNetworkLinkList(Network network, ArrayList<Node> nodeList) {
			List<Id<Link>> linkList = new ArrayList<Id<Link>>(nodeList.size() - 1);
			for (int n = 0; n < (nodeList.size() - 1); n++) {
				for (Link l : nodeList.get(n).getOutLinks().values()) {
					if (l.getToNode() == nodeList.get(n + 1)) {
						linkList.add(l.getId());
					}
				}
			}
			return linkList;
		}

		public static Id<Node> metroNodeFromOriginalLink(Id<Link> originalLinkRefID) {
			Id<Node> metroNodeId = Id.createNodeId("MetroNodeLinkRef_"+originalLinkRefID.toString());
			return metroNodeId;
		}
		
		public static Id<Link> orginalLinkFromMetroNode(Id<Node> metroNodeId){
			String metroString = metroNodeId.toString();
			Id<Link> originalLinkId = Id.createLinkId(metroString.substring(metroString.indexOf("_")+1));
			return originalLinkId;
		}
		
		public static ArrayList<Id<Link>> networkRouteToLinkIdList(NetworkRoute networkRoute){
			ArrayList<Id<Link>> linkList = new ArrayList<Id<Link>>(networkRoute.getLinkIds().size()+2);
			linkList.add(networkRoute.getStartLinkId());
			linkList.addAll(networkRoute.getLinkIds());
			linkList.add(networkRoute.getEndLinkId());
			return linkList;
		}

		public static Network copyNetworkToNetwork(Network fromNetwork, Network toNetwork) {
			NetworkFactory networkFactory = toNetwork.getFactory();
			
			for (Link link : fromNetwork.getLinks().values()) {
				Node tempFromNode = networkFactory.createNode(Id.createNodeId(link.getFromNode().getId()), link.getFromNode().getCoord());
				Node tempToNode = networkFactory.createNode(Id.createNodeId(link.getToNode().getId()), link.getToNode().getCoord());
				Link tempLink = networkFactory.createLink(Id.createLinkId(link.getId()), tempFromNode, tempToNode);
				tempLink.setAllowedModes(link.getAllowedModes());
				if (toNetwork.getNodes().keySet().contains(tempFromNode.getId())==false) {
					toNetwork.addNode(tempFromNode);
				}
				if (toNetwork.getNodes().keySet().contains(tempToNode.getId())==false) {
					toNetwork.addNode(tempToNode);
				}
				if (toNetwork.getLinks().keySet().contains(tempLink.getId())==false) {
					toNetwork.addLink(tempLink);
				}
			}
			
			return toNetwork;
		}
		
		public static void createNetworkFromCustomLinks(Map<Id<Link>,CustomLinkAttributes> customLinkMap, Network oldNetwork, String linksString) {
			// public static void createNetworkFromCustomLinks(Map<Id<Link>,CustomLinkAttributes> customLinkMap, Network oldNetwork, String linksString, String facilityNodesString) {	
				
				Config config = ConfigUtils.createConfig();
				Scenario scenario = ScenarioUtils.createScenario(config);
				Network strongestLinksNetwork = scenario.getNetwork();
				NetworkFactory stongestLinksFactory = strongestLinksNetwork.getFactory();
						
				Network dominantFacilities = scenario.getNetwork();
				NetworkFactory dominantFacilitiesFactory = dominantFacilities.getFactory();
				Node facilityNode;
				
				for (Id<Link> linkIDiter : customLinkMap.keySet()) {
					Node fromNode = stongestLinksFactory.createNode(oldNetwork.getLinks().get(linkIDiter).getFromNode().getId(), oldNetwork.getLinks().get(linkIDiter).getFromNode().getCoord());
					Node toNode = stongestLinksFactory.createNode(oldNetwork.getLinks().get(linkIDiter).getToNode().getId(), oldNetwork.getLinks().get(linkIDiter).getToNode().getCoord());
					if (strongestLinksNetwork.getNodes().containsKey(fromNode.getId())==false) {				
						strongestLinksNetwork.addNode(fromNode);
					}
					if (strongestLinksNetwork.getNodes().containsKey(toNode.getId())==false) {				
						strongestLinksNetwork.addNode(toNode);
					}
					Link linkBetweenNodes = stongestLinksFactory.createLink(linkIDiter, fromNode, toNode);
					strongestLinksNetwork.addLink(linkBetweenNodes);
					if (customLinkMap.get(linkIDiter).dominantStopFacility==null) {
						facilityNode = dominantFacilitiesFactory.createNode(Id.createNodeId("FacilityCoordNodeOfLink"+linkIDiter.toString()), 
								oldNetwork.getLinks().get(linkIDiter).getFromNode().getCoord());
					}
					else {
						facilityNode = dominantFacilitiesFactory.createNode(Id.createNodeId("FacilityCoordNodeOfLink"+linkIDiter.toString()), 
																								customLinkMap.get(linkIDiter).dominantStopFacility.getCoord());
					}
					dominantFacilities.addNode(facilityNode);
				}
					
				NetworkWriter networkWriterLinks = new NetworkWriter(strongestLinksNetwork);
				networkWriterLinks.write(linksString);
				
				// NetworkWriter networkWriterNodes = new NetworkWriter(dominantFacilities);
				// networkWriterNodes.write(facilityNodesString);
				
			}
			
			public static double getAverageTrafficOnLinks(Map<Id<Link>,CustomLinkAttributes> customLinkMap) {
				double totalTraffic = 0.0;
				for (Id<Link> linkID : customLinkMap.keySet()) {
					totalTraffic += customLinkMap.get(linkID).getTotalTraffic();
				}
				return totalTraffic/customLinkMap.size();
			}
		
			
			
// %%%%%%%%%%%%% Route & Line Processors %%%%%%%%%%%%%%%%%%%%%
			public static double NetworkRoute2TotalLength(NetworkRoute networkRoute, Network thisNetwork) {
				double totalLength = 0.00;
				for (Id<Link> linkID : networkRoute.getLinkIds()) {
					totalLength += thisNetwork.getLinks().get(linkID).getLength();
				}
				return totalLength;
			}
			
			public static List<Id<Link>> NetworkRoute2LinkIdList(NetworkRoute networkRoute){
				List<Id<Link>> linkList = new ArrayList<Id<Link>>(networkRoute.getLinkIds().size()+2);
				linkList.add(networkRoute.getStartLinkId());
				linkList.addAll(networkRoute.getLinkIds());
				linkList.add(networkRoute.getEndLinkId());
				return linkList;
			}
			
			public static List<Id<Node>> NetworkRoute2NodeIdList(NetworkRoute networkRoute, Network thisNetwork){
				List<Id<Link>> linkList = NetworkRoute2LinkIdList(networkRoute);
				List<Id<Node>> nodeList = new ArrayList<Id<Node>>();
				for (Id<Link> linkID : linkList) {
					nodeList.add(thisNetwork.getLinks().get(linkID).getFromNode().getId());
				}
				nodeList.add(thisNetwork.getLinks().get(linkList.get(linkList.size()-1)).getToNode().getId());
				return nodeList;
			}
					
	
// %%%%%%%%%%%%% Plot Makers %%%%%%%%%%%%%%%%%%%%%

	@SuppressWarnings("unchecked")
	public static void writeChartAverageTravelTimes(int lastGeneration, String fileName) throws FileNotFoundException { 	// Average and Best Scores
		Map<Integer, Double> generationsAverageTravelTime = new HashMap<Integer, Double>();
		Map<Integer, Double> generationsAverageTravelTimeStdDev = new HashMap<Integer, Double>();
		String generationPath = "zurich_1pm/Evolution/Population/HistoryLog/Generation";
		Map<Integer, Double> generationsBestTravelTime = new HashMap<Integer, Double>();
		Map<String, NetworkScoreLog> networkScores = new HashMap<String, NetworkScoreLog>();
		for (int g = 1; g <= lastGeneration; g++) {
			double averageTravelTimeThisGeneration = 0.0;
			double averageTravelTimeStdDevThisGeneration = 0.0;
			double bestAverageTravelTimeThisGeneration = Double.MAX_VALUE;
			networkScores = (Map<String, NetworkScoreLog>) XMLOps.readFromFile(networkScores.getClass(),
					generationPath + g + "/networkScoreMap.xml");
			for (NetworkScoreLog nsl : networkScores.values()) {
				if (nsl.averageTravelTime < bestAverageTravelTimeThisGeneration) {
					bestAverageTravelTimeThisGeneration = nsl.averageTravelTime;
				}
				averageTravelTimeThisGeneration += nsl.averageTravelTime / networkScores.size();
				averageTravelTimeStdDevThisGeneration += nsl.stdDeviationTravelTime / networkScores.size();
			}
			System.out.println("bestAverageTravelTimeThisGeneration = " + bestAverageTravelTimeThisGeneration);
			generationsAverageTravelTime.put(g, averageTravelTimeThisGeneration);
			generationsAverageTravelTimeStdDev.put(g, averageTravelTimeStdDevThisGeneration);
			generationsBestTravelTime.put(g, bestAverageTravelTimeThisGeneration);
		}
		XYLineChart chart = new XYLineChart("Evolution of Network Performance", "Generation", "Score");
		chart.addSeries("Average Travel Time [min]", generationsAverageTravelTime);
		chart.addSeries("Average Travel Time - Std Deviation [min]", generationsAverageTravelTimeStdDev);
		chart.addSeries("Best Average Travel Time [min]", generationsBestTravelTime);
		chart.saveAsPng(fileName, 800, 600);
	}

	@SuppressWarnings("unchecked")
	public static void writeChartAverageGenerationNetworkAverageTravelTimes(int lastGeneration, String fileName) 	// only average scores
			throws FileNotFoundException {
		String generationPath = "zurich_1pm/Evolution/Population/HistoryLog/Generation";
		Map<Integer, Double> generationsAverageTravelTime = new HashMap<Integer, Double>();
		Map<Integer, Double> generationsAverageTravelTimeStdDev = new HashMap<Integer, Double>();
		Map<String, NetworkScoreLog> networkScores = new HashMap<String, NetworkScoreLog>();
		for (int g = 1; g <= lastGeneration; g++) {
			double averageTravelTimeThisGeneration = 0.0;
			double averageTravelTimeStdDevThisGeneration = 0.0;
			networkScores = (Map<String, NetworkScoreLog>) XMLOps.readFromFile(networkScores.getClass(),
					generationPath + g + "/networkScoreMap.xml");
			for (NetworkScoreLog nsl : networkScores.values()) {
				averageTravelTimeThisGeneration += nsl.averageTravelTime / networkScores.size();
				averageTravelTimeStdDevThisGeneration += nsl.stdDeviationTravelTime / networkScores.size();
			}
			generationsAverageTravelTime.put(g, averageTravelTimeThisGeneration);
			generationsAverageTravelTimeStdDev.put(g, averageTravelTimeStdDevThisGeneration);
		}
		XYLineChart chart = new XYLineChart("Evolution of Network Performance", "Generation", "Score");
		chart.addSeries("Average Travel Time [min]", generationsAverageTravelTime);
		chart.addSeries("Average Travel Time - Std Deviation [min]", generationsAverageTravelTimeStdDev);
		chart.saveAsPng(fileName, 800, 600);
	}

	@SuppressWarnings("unchecked")
	public static void writeChartBestGenerationNetworkAverageTravelTimes(int lastGeneration, String fileName) 	// only best scores
			throws FileNotFoundException {
		String generationPath = "zurich_1pm/Evolution/Population/HistoryLog/Generation";
		Map<Integer, Double> generationsBestTravelTime = new HashMap<Integer, Double>();
		Map<String, NetworkScoreLog> networkScores = new HashMap<String, NetworkScoreLog>();
		for (int g = 1; g <= lastGeneration; g++) {
			double bestAverageTravelTimeThisGeneration = Double.MAX_VALUE;
			networkScores = (Map<String, NetworkScoreLog>) XMLOps.readFromFile(networkScores.getClass(),
					generationPath + g + "/networkScoreMap.xml");
			for (NetworkScoreLog nsl : networkScores.values()) {
				if (nsl.averageTravelTime < bestAverageTravelTimeThisGeneration) {
					bestAverageTravelTimeThisGeneration = nsl.averageTravelTime;
					System.out.println("bestAverageTravelTimeThisGeneration = " + bestAverageTravelTimeThisGeneration);
				}
			}
			generationsBestTravelTime.put(g, bestAverageTravelTimeThisGeneration);
		}
		XYLineChart chart = new XYLineChart("Evolution of Network Performance", "Generation", "Score");
		chart.addSeries("Best Average Travel Time [min]", generationsBestTravelTime);
		chart.saveAsPng(fileName, 800, 600);
	}
	
	
	public static List<Id<Link>> OppositeLinkListOf(List<Id<Link>> linkList){
		List<Id<Link>> oppositeLinkList = new ArrayList<Id<Link>>(linkList.size());
		for (int c=0; c<linkList.size(); c++) {
			oppositeLinkList.add(ReverseLink(linkList.get(linkList.size()-1-c)));
		}
		return oppositeLinkList;
	}
	
	public static Id<Link> ReverseLink(Id<Link> linkId){
		String[] linkIdStrings = linkId.toString().split("_");
		Id<Link> reverseId = Id.createLinkId("MetroNodeLinkRef_"+linkIdStrings[3]+"_MetroNodeLinkRef_"+linkIdStrings[1]);
		return reverseId;
	}
	
	
	public static MNetworkPop developGeneration(Network globalNetwork, Map<String, NetworkScoreLog> networkScoreMap, MNetworkPop evoNetworksToProcessPlans, String populationName,
			Double alpha, Double pCrossOver, double metroConstructionCostPerKmOverground, double metroConstructionCostPerKmUnderground, double metroOpsCostPerKM,
			int iterationToReadOriginalNetwork, boolean useOdPairsForInitialRoutes, String vehicleTypeName, double vehicleLength, double maxVelocity, 
			int vehicleSeats, int vehicleStandingRoom, String defaultPtMode, double stopTime, boolean blocksLane) {
		
		// Copy oldPopulation (evoNetworksToProcessPlans) to new one to fill in gradually with new offspring afterwards
		MNetworkPop newPopulation = new MNetworkPop(evoNetworksToProcessPlans.populationId);
		newPopulation.networkMap = evoNetworksToProcessPlans.networkMap;
		int nOldPop = newPopulation.networkMap.size();
		
		// List<MRoute> offspringRoutes = new ArrayList<MRoute>();
		// find and store Elite network
		String eliteNetwork = "";
		if (networkScoreMap.size() == 0) {		System.out.println("CAUTION: NetworkScoreMapSize is zero!");	}
		double maxNetworkScore = -Double.MAX_VALUE;
		for (String networkName : networkScoreMap.keySet()) {
			if (networkScoreMap.get(networkName).overallScore > maxNetworkScore) {
				maxNetworkScore = networkScoreMap.get(networkName).overallScore;
				eliteNetwork = networkName;
			}
		}
		MNetwork eliteMNetwork = evoNetworksToProcessPlans.getNetworks().get(eliteNetwork);
		
		
		// CROSS-OVERS
		int nCrossOverCandidates = (int) Math.ceil(0.5*nOldPop);
		List<MNetwork> newOffspring = new ArrayList<MNetwork>();
		System.out.println("We will try nCrossOverCandidates="+nCrossOverCandidates);
		
		for (int n=0; n<nCrossOverCandidates; n++) {
			Random r = new Random();
			if (r.nextDouble()<pCrossOver) {
				String nameParent1;
				String nameParent2;
				nameParent1 = NetworkEvolutionImpl.selectMNetworkByRoulette(alpha, networkScoreMap);
				System.out.println("ParentName 1="+nameParent1);
				do{
					nameParent2 = NetworkEvolutionImpl.selectMNetworkByRoulette(alpha, networkScoreMap);
					System.out.println("ParentName 2="+nameParent2);
				}while(nameParent1.equals(nameParent2));
				MNetwork parentMNetwork1 = evoNetworksToProcessPlans.getNetworks().get(nameParent1);
				MNetwork parentMNetwork2 = evoNetworksToProcessPlans.getNetworks().get(nameParent2);
				MNetwork[] offspringMNetworks = NetworkEvolutionImpl.crossMNetworks(globalNetwork, parentMNetwork1, parentMNetwork2,
						vehicleTypeName, vehicleLength, maxVelocity, vehicleSeats, vehicleStandingRoom, defaultPtMode,
						stopTime, blocksLane, metroConstructionCostPerKmOverground,
						metroConstructionCostPerKmUnderground, metroOpsCostPerKM, iterationToReadOriginalNetwork,
						useOdPairsForInitialRoutes); // (make sure IDs are same as parent Networks to remove old network adding to newPopulation)
				newOffspring.add(offspringMNetworks[0]);
				System.out.println("Added new offspring="+newOffspring.get(0).routeMap.keySet().toString());
				newOffspring.add(offspringMNetworks[1]);
				System.out.println("Added new offspring="+newOffspring.get(1).routeMap.keySet().toString());

			}
		}
		int nNewOffspring = newOffspring.size();
		System.out.println("nNewOffspring="+nNewOffspring);
		if(nNewOffspring != 0) {
			List<String> deletedNetworkNames = RemoveWeakestNetworks(newPopulation, nNewOffspring);
			System.out.println("deletedNetworkNames="+deletedNetworkNames.toString());
			do {
				RenameOffspring(deletedNetworkNames.get(0), newOffspring.get(0));	// renaming offspring with its MNetworkId and the Id of all its MRoutes
				newPopulation.networkMap.put(newOffspring.get(0).networkID, newOffspring.get(0));
				deletedNetworkNames.remove(0);
				newOffspring.remove(0);
			}while(deletedNetworkNames.size()>0);
		}
		if (nNewOffspring == nOldPop) {										// check with this condition if all old networks have been deleted for new offspring
			newPopulation.networkMap.put(eliteNetwork, eliteMNetwork);		// if also elite network has been deleted, add manually again (it will replace the new one with the same name)
		}
		// Read out all final network routes
		for(MNetwork mnetwork : newPopulation.networkMap.values()) {
			for (String mr : mnetwork.routeMap.keySet()) {
				System.out.println("Network="+mnetwork.networkID+"   |   Route="+mnetwork.routeMap.get(mr).linkList.toString());
			}
		}
		
		// MUTATIONS
		
		// SAVE DATA
		return newPopulation;
	}


	public static MNetwork[] crossMNetworks(Network globalNetwork, MNetwork parentMNetwork1, MNetwork parentMNetwork2, String vehicleTypeName, double vehicleLength, double maxVelocity, 
			int vehicleSeats, int vehicleStandingRoom, String defaultPtMode, double stopTime, boolean blocksLane, double metroConstructionCostPerKmOverground,
			double metroConstructionCostPerKmUnderground, double metroOpsCostPerKM, int iterationToReadOriginalNetwork, boolean useOdPairsForInitialRoutes) {
		Map<String, MRoute> routesPool1 = parentMNetwork1.routeMap;
		Map<String, MRoute> routesPool2 = parentMNetwork2.routeMap;
		Map<String, MRoute> routesOut1 = new HashMap<String, MRoute>();
		Map<String, MRoute> routesOut2 = new HashMap<String, MRoute>();
		Loop1:
		for (String routeP1name : routesPool1.keySet()) {
			MRoute routeFromP1 = routesPool1.get(routeP1name);
			for (String routeP2name : routesPool2.keySet()) {
				MRoute routeFromP2 = routesPool2.get(routeP2name);
				// hand over IDs to new crossed routes
				// make route and transitSchedule and calculate stuff here (maybe store frequency in MRoute itself!)
				System.out.println("Now trying to cross route A: "+routeFromP1.linkList.toString());
				System.out.println("Now trying to cross route B: "+routeFromP2.linkList.toString());
				MRoute[] crossedRoutes = crossMRoutes(routeFromP1, routeFromP2, globalNetwork);
				if (crossedRoutes != null) {
					System.out.println("Success - new Route A = "+crossedRoutes[0].linkList.toString());
					System.out.println("Success - new Route B = "+crossedRoutes[1].linkList.toString());
					routesOut1.put(crossedRoutes[0].routeID, crossedRoutes[0]);
					routesOut2.put(crossedRoutes[1].routeID, crossedRoutes[1]);
					continue Loop1;
				}
			}
			// this will come in place if inner loop has not found a feasible crossing and has therefore not broken inner loop to jump to outer loop
			routesOut1.put(routeFromP1.routeID, routeFromP1);	
		}
		for (MRoute routeFromP2 : routesPool2.values()) { // add all routesFromP2 that could not be crossed with any routesFromP1
			if (routesOut2.containsKey(routeFromP2.routeID)==false) {
				routesOut2.put(routeFromP2.routeID, routeFromP2);
			}
		}
		
		MNetwork mnetworkOut1 = new MNetwork(parentMNetwork1.networkID);
		MNetwork mnetworkOut2 = new MNetwork(parentMNetwork2.networkID);
		System.out.println("Out routes1 are: "+routesOut1.keySet().toString());
		System.out.println("Out routes2 are: "+routesOut1.keySet().toString());
		mnetworkOut1.routeMap = routesOut1;
		mnetworkOut2.routeMap = routesOut2;
		MNetwork[] mNetworksOut = new MNetwork[] {mnetworkOut1, mnetworkOut2};

		
		Config originalConfig = ConfigUtils.loadConfig("zurich_1pm/zurich_config.xml");
		Scenario originalScenario = ScenarioUtils.loadScenario(originalConfig);
		originalScenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DefaultEnrichedTransitRoute.class, new DefaultEnrichedTransitRouteFactory());
		Network originalNetwork = originalScenario.getNetwork();
		TransitSchedule originalTransitSchedule = originalScenario.getTransitSchedule();
		
		for (MNetwork mNetwork : mNetworksOut) {
			System.out.println("This network is: "+mNetwork.networkID);
			// Transit Schedule Implementations
			Config newConfig = ConfigUtils.createConfig();
			Scenario newScenario = ScenarioUtils.loadScenario(newConfig);
			TransitSchedule newSchedule = newScenario.getTransitSchedule();
			TransitScheduleFactory newScheduleFactory = newSchedule.getFactory();
			// Create a New Metro Vehicle
			VehicleType newVehicleType = Metro_TransitScheduleImpl.createNewVehicleType(vehicleTypeName, vehicleLength, maxVelocity, vehicleSeats, vehicleStandingRoom);
			newScenario.getTransitVehicles().addVehicleType(newVehicleType);
			
			// Generate TransitLines and Schedules on NetworkRoutes --> Add to Transit Schedule
			int lineNr = 0;
			for (MRoute mRoute : mNetwork.routeMap.values()) {
				lineNr++;
				// Create an array of stops along new networkRoute on the center of each of its individual links
				List<TransitRouteStop> stopArray = Metro_TransitScheduleImpl.createAndAddNetworkRouteStops(newSchedule, globalNetwork, mRoute.networkRoute, defaultPtMode, stopTime, maxVelocity, blocksLane);
				// Build TransitRoute from stops and NetworkRoute --> and add departures
				TransitRoute transitRoute = newScheduleFactory.createTransitRoute(Id.create(mNetwork.networkID+"_Route"+lineNr, TransitRoute.class ), mRoute.networkRoute, stopArray, defaultPtMode);
				double totalRouteTravelTime = stopArray.get(stopArray.size()-1).getArrivalOffset();
				String vehicleFileLocation = ("zurich_1pm/Evolution/Population/"+mNetwork.networkID+"/Vehicles.xml"); // Add (nDepartures) departures to TransitRoute
				transitRoute = Metro_TransitScheduleImpl.addDeparturesAndVehiclesToTransitRoute(newScenario, newSchedule, transitRoute, 
						mRoute.nDepartures, mRoute.firstDeparture, mRoute.departureSpacing, totalRouteTravelTime, newVehicleType, vehicleFileLocation);
				// Build TransitLine from TrasitRoute
				TransitLine transitLine = newScheduleFactory.createTransitLine(Id.create("TransitLine_Nr" + lineNr, TransitLine.class));
				transitLine.addRoute(transitRoute);
				// Add new line to schedule
				System.out.println(newSchedule.getTransitLines().keySet().toString());
				newSchedule.addTransitLine(transitLine);
				mRoute.setTransitLine(transitLine);
				mRoute.setLinkList(NetworkRoute2LinkIdList(mRoute.networkRoute));
				mRoute.setNodeList(NetworkRoute2NodeIdList(mRoute.networkRoute, globalNetwork));
				mRoute.setRouteLength(NetworkRoute2TotalLength(mRoute.networkRoute, globalNetwork));
				mRoute.setDrivenKM(mRoute.routeLength * mRoute.nDepartures);
				mRoute.constrCost = mRoute.routeLength
						* (metroConstructionCostPerKmOverground * 0.01 * (100 - mRoute.undergroundPercentage)
								+ metroConstructionCostPerKmUnderground * 0.01 * mRoute.undergroundPercentage);
				mRoute.opsCost = mRoute.routeLength * (metroOpsCostPerKM * 0.01 * (100 - mRoute.undergroundPercentage)
						+ 2 * metroOpsCostPerKM * 0.01 * mRoute.undergroundPercentage);
				mRoute.transitScheduleFile = "zurich_1pm/Evolution/Population/" + mNetwork.networkID
						+ "/MetroSchedule.xml";
				mRoute.setEventsFile("zurich_1pm/Zurich_1pm_SimulationOutput/ITERS/it." + iterationToReadOriginalNetwork
						+ "/" + iterationToReadOriginalNetwork + ".events.xml.gz");
			} // end of TransitLine creator loop
		
			// Write TransitSchedule to corresponding file
			TransitScheduleWriter tsw = new TransitScheduleWriter(newSchedule);
			tsw.writeFile("zurich_1pm/Evolution/Population/"+mNetwork.networkID+"/MetroSchedule.xml");
						
			String mergedNetworkFileName = "";
			String separateRoutesNetworkFileName = "";
			if (useOdPairsForInitialRoutes==true) {
				mergedNetworkFileName = ("zurich_1pm/Evolution/Population/"+mNetwork.networkID+"/OriginalNetwork_with_ODInitialRoutes.xml");
				separateRoutesNetworkFileName = ("zurich_1pm/Evolution/Population/"+mNetwork.networkID+"/0_MetroInitialRoutes_OD.xml");
			}
			else {
				mergedNetworkFileName = ("zurich_1pm/Evolution/Population/"+mNetwork.networkID+"/OriginalNetwork_with_RandomInitialRoutes.xml");
				separateRoutesNetworkFileName = ("zurich_1pm/Evolution/Population/"+mNetwork.networkID+"/0_MetroInitialRoutes_Random.xml");
			}
			Network separateRoutesNetwork = NetworkEvolutionImpl.MRoutesToNetwork(mNetwork.routeMap, globalNetwork, Sets.newHashSet("pt"), separateRoutesNetworkFileName);
			// Network mergedNetwork = ...
					Metro_TransitScheduleImpl.mergeRoutesNetworkToOriginalNetwork(separateRoutesNetwork, originalNetwork, Sets.newHashSet("pt"), mergedNetworkFileName);
			//TransitSchedule mergedTransitSchedule = ...
					Metro_TransitScheduleImpl.mergeAndWriteTransitSchedules(newSchedule, originalTransitSchedule, ("zurich_1pm/Evolution/Population/"+mNetwork.networkID+"/MergedSchedule.xml"));
			//Vehicles mergedVehicles = ...
					Metro_TransitScheduleImpl.mergeAndWriteVehicles(newScenario.getTransitVehicles(), originalScenario.getTransitVehicles(), ("zurich_1pm/Evolution/Population/"+mNetwork.networkID+"/MergedVehicles.xml"));
			
			// FOR DIRECT DATA TRANSPORT W/O SAVING TO FILES - fill in MNetwork Objects for this Network
			//mNetwork.network = mergedNetwork;
			//mNetwork.transitSchedule = mergedTransitSchedule;
			//mNetwork.vehicles = mergedVehicles;
		}
		
		return mNetworksOut;
	}



	public static MRoute[] crossMRoutes(MRoute routeFromP1, MRoute routeFromP2, Network globalNetwork) {
		List<Id<Link>> route1LinkListOneway = routeFromP1.linkList.subList(0, routeFromP1.linkList.size()/2);
		List<Id<Link>> route2LinkListOneway = routeFromP2.linkList.subList(0, routeFromP2.linkList.size()/2);
		System.out.println("Entire route is: "+routeFromP1.linkList.toString());
		System.out.println("Half route is: "+route1LinkListOneway.toString());
		
		MRoute mRouteNew1 = new MRoute(routeFromP1.routeID);
		MRoute mRouteNew2 = new MRoute(routeFromP2.routeID);
		MRoute[] crossedRoutes = new MRoute[2];
		Id<Link> crossLink1 = null;
		Id<Link> crossLink2 = null;
		boolean crossingFound = false;
		for (Id<Link> linkFrom1 : route1LinkListOneway) {
			if (crossingFound) {
				break;
			}
			Node fromNode1 = globalNetwork.getLinks().get(linkFrom1).getFromNode();
			System.out.println("now trying FromNode in route1: "+fromNode1.getId().toString());
			for (Id<Link> linkFrom2 : route2LinkListOneway) {
				Node fromNode2 = globalNetwork.getLinks().get(linkFrom2).getFromNode();
				System.out.println("trying: "+fromNode1.getId().toString()+" against: "+fromNode2.getId().toString());
				if (fromNode1.getId().toString().equals(fromNode2.getId().toString())) {
					crossingFound = true;
					crossLink1 = linkFrom1;		// crossing takes place at from link!
					crossLink2 = linkFrom2;		// crossing takes place at from link!
					System.out.println("YES - FromNode found in route2: "+fromNode1.getId().toString());
					break;
				}
				System.out.println("NO - FromNode not found in route2: "+fromNode1.getId().toString());
			}
		}
		
		if(crossingFound) {
			System.out.println("Starting this loop: for successful crossing found");
			List<Id<Link>> crossLinkList1 = new ArrayList<Id<Link>>();
			List<Id<Link>> crossLinkList2 = new ArrayList<Id<Link>>();
			crossLinkList1.addAll(route1LinkListOneway.subList(0, route1LinkListOneway.indexOf(crossLink1)));								// route1 part before crossing ...
			crossLinkList1.addAll(route2LinkListOneway.subList(route2LinkListOneway.indexOf(crossLink2), route2LinkListOneway.size()));		// ... + route2 after crossing
			crossLinkList2.addAll(route2LinkListOneway.subList(0, route2LinkListOneway.indexOf(crossLink2)));								// route2 part before crossing ...
			crossLinkList2.addAll(route1LinkListOneway.subList(route1LinkListOneway.indexOf(crossLink1), route1LinkListOneway.size()));		// ... + route1 after crossing
			crossLinkList1.addAll(OppositeLinkListOf(crossLinkList1)); // extend again with its opposite direction for PT transportation!
			crossLinkList2.addAll(OppositeLinkListOf(crossLinkList2)); // extend again with its opposite direction for PT transportation!
			System.out.println("Crossed linked list 1 = "+crossLinkList1.toString());
			System.out.println("Crossed linked list 2 = "+crossLinkList2.toString());
			NetworkRoute networkRoute1 = RouteUtils.createNetworkRoute(crossLinkList1, globalNetwork);
			NetworkRoute networkRoute2 = RouteUtils.createNetworkRoute(crossLinkList2, globalNetwork);
			mRouteNew1.networkRoute = networkRoute1;
			mRouteNew1.linkList = crossLinkList1;
			mRouteNew2.linkList = crossLinkList2;
			mRouteNew2.networkRoute = networkRoute2;
			mRouteNew1.firstDeparture = 0.5*Math.round(routeFromP1.firstDeparture+routeFromP2.firstDeparture);
			mRouteNew2.firstDeparture = 0.5*Math.round(routeFromP1.firstDeparture+routeFromP2.firstDeparture);
			mRouteNew1.departureSpacing = 0.5*Math.round(routeFromP1.departureSpacing+routeFromP2.departureSpacing);
			mRouteNew2.departureSpacing = 0.5*Math.round(routeFromP1.departureSpacing+routeFromP2.departureSpacing);
			mRouteNew1.nDepartures = (int) 0.5*Math.round(routeFromP1.nDepartures+routeFromP2.nDepartures);
			mRouteNew2.nDepartures = (int) 0.5*Math.round(routeFromP1.nDepartures+routeFromP2.nDepartures);
			crossedRoutes[0] = mRouteNew1;
			crossedRoutes[1] = mRouteNew2;
			return crossedRoutes;
		}
		else {
			return null;
		}
	}

	
	
	public static String selectMNetworkByRoulette(double alpha, Map<String, NetworkScoreLog> networkScoreMap) {
		// Map<String, Double> rouletteMap = new HashMap<String, Double>(newPopulation.getNetworks().size());
		double totalRouletteScore = 0.0;
		for (String networkName : networkScoreMap.keySet()) {
			networkScoreMap.get(networkName).rouletteScore = Math.exp(alpha*networkScoreMap.get(networkName).overallScore);
			totalRouletteScore += Math.exp(alpha*networkScoreMap.get(networkName).overallScore);
		}
		Random r = new Random();
		double rD = r.nextDouble();
		double attemptedProb = 0.0;
		for (String networkName : networkScoreMap.keySet()) {
			if (attemptedProb/totalRouletteScore <= rD   &&   rD < (attemptedProb+networkScoreMap.get(networkName).rouletteScore)/totalRouletteScore) {
				return networkName;
			}
			attemptedProb += networkScoreMap.get(networkName).rouletteScore;
		}
		System.out.println("Roulette has not selected any network! Returning NULL...");
		return null;
	}
	
	
	
	public static void RenameOffspring(String newNetworkName, MNetwork mNetwork) {
		mNetwork.networkID = newNetworkName;
		//thisNewNetworkName+"_Route"+lineNr
		Map<String, MRoute> newRoutesMap = new HashMap<String, MRoute>();
		int counter = 1;
		for (MRoute mRoute : mNetwork.routeMap.values()) {
			mRoute.routeID = newNetworkName+"_Route"+counter;
			newRoutesMap.put(mRoute.routeID, mRoute);
			counter++;
		}
		mNetwork.routeMap = newRoutesMap;
	}
	
	


	public static List<String> RemoveWeakestNetworks(MNetworkPop newPopulation, int nDelete) {
		List<String> deletedNetworks = new ArrayList<String>();
		for (int n=0; n<nDelete; n++) {
			String weakestNetworkName = "";
			Double weakestScore = Double.MAX_VALUE;
			for (String networkName : newPopulation.networkMap.keySet()) {
				if (newPopulation.networkMap.get(networkName).overallScore < weakestScore) {
					weakestNetworkName = networkName;
					weakestScore = newPopulation.networkMap.get(networkName).overallScore;
				}
			}
			deletedNetworks.add(weakestNetworkName);
			newPopulation.networkMap.remove(weakestNetworkName);
		}		
		return deletedNetworks;
	}
	
	public static Network MRoutesToNetwork(Map<String, MRoute> mRoutes, Network network, Set<String> networkRouteModes, String fileName) {
		// Store all new networkRoutes in a separate network file for visualization
			Network routesNetwork = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getNetwork();
			NetworkFactory networkFactory = routesNetwork.getFactory();
			for (MRoute mRoute : mRoutes.values()) {
				NetworkRoute nR = mRoute.networkRoute;
				List<Id<Link>> routeLinkList = new ArrayList<Id<Link>>();
				routeLinkList.add(nR.getStartLinkId());
				routeLinkList.addAll(nR.getLinkIds());
				routeLinkList.add(nR.getEndLinkId());
				for (Id<Link> linkID : routeLinkList) {
					Node tempToNode = networkFactory.createNode(network.getLinks().get(linkID).getToNode().getId(),
							network.getLinks().get(linkID).getToNode().getCoord());
					Node tempFromNode = networkFactory.createNode( network.getLinks().get(linkID).getFromNode().getId(),
							network.getLinks().get(linkID).getFromNode().getCoord());
					Link tempLink = networkFactory.createLink(network.getLinks().get(linkID).getId(), tempFromNode, tempToNode);
					tempLink.setAllowedModes(networkRouteModes);
					if (routesNetwork.getNodes().containsKey(tempToNode.getId()) == false) {
						routesNetwork.addNode(tempToNode);
					}
					if (routesNetwork.getNodes().containsKey(tempFromNode.getId()) == false) {
						routesNetwork.addNode(tempFromNode);
					}
					if (routesNetwork.getLinks().containsKey(tempLink.getId()) == false) {
						routesNetwork.addLink(tempLink);
					}
				}
			}
		NetworkWriter initialRoutesNetworkWriter = new NetworkWriter(routesNetwork);
		initialRoutesNetworkWriter.write(fileName);
		
		return routesNetwork;
	}
	
	
}

