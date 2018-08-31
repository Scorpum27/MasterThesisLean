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
import org.matsim.vehicles.Vehicles;

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
				(mNetworkPath+"/1_zurich_network_WithinRadius" + ((int) Math.round(metroCityRadius)) + ".xml"));

		// Find most frequent links from input links
		Map<Id<Link>, CustomLinkAttributes> links_mostFrequentInRadius = 
				NetworkEvolutionImpl.findMostFrequentLinks(nMostFrequentLinks, links_withinRadius, originalNetwork, null);

		// Set dominant transit stop facility in given network (from custom link list)
		Map<Id<Link>, CustomLinkAttributes> links_mostFrequentInRadiusMainFacilitiesSet = NetworkEvolutionImpl.setMainFacilities(originalTransitSchedule, 
				originalNetwork, links_mostFrequentInRadius, (mNetworkPath+"/2_zurich_network_MostFrequentInRadius.xml"));

		// Create a metro network from candidate links/stopFaiclities
		Network metroNetwork = NetworkEvolutionImpl.createMetroNetworkFromCandidates(
				links_mostFrequentInRadiusMainFacilitiesSet, maxNewMetroLinkDistance, originalNetwork,
				(mNetworkPath+"/4_zurich_network_MetroNetwork.xml"));
				
				
		// %%% Everything in old system up to here %%% //
		// CONVERSIONS:
		// 	Get [new map] node from [old map] refLink: Node newMapNode = newNetwork.getNodes.get(Id.createNodeId("MetroNodeLinkRef_"+oldMapRefLink.toString()))
		// 	---> Id<Node> metroNodeId = metroNodeFromOriginalLink(Id<Link> originalLinkRefID) 
		// 	Get [old map] refLink from [new map] node: Link oldMapLink = newMapNode.parse
		// 	---> Id<Link> originalLinkId = orginalLinkFromMetroNode(Id<Node> metroNodeId)

		boolean useOdPairsForInitialRoutes = false;
		if (initialRouteType.equals("OD")) {						// %%% TODO MIGHT BE CRITICAL !!
			useOdPairsForInitialRoutes = true;
		}
		ArrayList<NetworkRoute> initialMetroRoutes = null;
		Network separateRoutesNetwork = null;
		if (useOdPairsForInitialRoutes==false) {								
			// %%% initial Routes random terminals within bounds and min distance apart %%%
			// Select all metro terminal candidates by setting bounds on their location (distance from city center)
			Map<Id<Link>, CustomLinkAttributes> links_MetroTerminalCandidates = NetworkEvolutionImpl.findLinksWithinBounds(links_mostFrequentInRadiusMainFacilitiesSet, 
					originalNetwork, zurich_NetworkCenterCoord, minTerminalRadiusFromCenter, maxTerminalRadiusFromCenter, (mNetworkPath+"/3_zurich_network_MetroTerminalCandidate.xml")); // find most frequent links
			initialMetroRoutes = NetworkEvolutionImpl.createInitialRoutesRandom(metroNetwork, links_MetroTerminalCandidates, routesPerNetwork, minTerminalDistance);			
			separateRoutesNetwork = NetworkEvolutionImpl.networkRoutesToNetwork(initialMetroRoutes, metroNetwork, Sets.newHashSet("pt"), (mNetworkPath+"/5_zurich_network_MetroInitialRoutes_Random.xml"));
		}
		if (useOdPairsForInitialRoutes==true) {									
			// %%% initial Routes OD_Pairs within bounds %%%		
			initialMetroRoutes = OD_ProcessorImpl.createInitialRoutesOD(metroNetwork, routesPerNetwork, minTerminalRadiusFromCenter,
					maxTerminalRadiusFromCenter, odConsiderationThreshold, zurich_NetworkCenterCoord, "zurich_1pm/Evolution/Input/Data/OD_Input/Demand2013_PT.csv",
					"zurich_1pm/Evolution/Input/Data/OD_Input/OD_ZoneCodesLocations.csv", xOffset, yOffset);	
					// CAUTION: Make sure .csv is separated by semi-colon because location names also include commas sometimes and lead to failure!!			
			separateRoutesNetwork = NetworkEvolutionImpl.networkRoutesToNetwork(initialMetroRoutes, metroNetwork, Sets.newHashSet("pt"),
					(mNetworkPath+"/5_zurich_network_MetroInitialRoutes_OD.xml"));
		}
				
		// Load & Create Schedules and Factories
		Config metroConfig = ConfigUtils.createConfig();						// this is totally default and may be modified as required
		Scenario metroScenario = ScenarioUtils.createScenario(metroConfig);
		TransitSchedule metroSchedule = metroScenario.getTransitSchedule();
		TransitScheduleFactory metroScheduleFactory = metroSchedule.getFactory();
				
		// Create a New Metro Vehicle
		VehicleType metroVehicleType = Metro_TransitScheduleImpl.createNewVehicleType(vehicleTypeName, vehicleLength, maxVelocity, vehicleSeats, vehicleStandingRoom);
					metroScenario.getTransitVehicles().addVehicleType(metroVehicleType);
				
		// Generate TransitLines and Schedules on NetworkRoutes --> Add to Transit Schedule
		int nTransitLines = initialMetroRoutes.size();
		for(int lineNr=1; lineNr<=nTransitLines; lineNr++) {
				
			// networkRoute
			NetworkRoute metroNetworkRoute = initialMetroRoutes.get(lineNr-1);
			MRoute mRoute = new MRoute(thisNewNetworkName+"_Route"+lineNr);
			mRoute.setNetworkRoute(metroNetworkRoute);
			mNetwork.addNetworkRoute(mRoute);
			
			// Create an array of stops along new networkRoute on the center of each of its individual links
			List<TransitRouteStop> stopArray = Metro_TransitScheduleImpl.createAndAddNetworkRouteStops(
							metroSchedule, metroNetwork, metroNetworkRoute, defaultPtMode, stopTime, maxVehicleSpeed, blocksLane);
			
			
			// Build TransitRoute from stops and NetworkRoute --> and add departures
			String vehicleFileLocation = (mNetworkPath+"/Vehicles.xml");
			TransitRoute transitRoute = metroScheduleFactory.createTransitRoute(Id.create(thisNewNetworkName+"_Route"+lineNr, TransitRoute.class ), 
					metroNetworkRoute, stopArray, defaultPtMode);
			double totalRouteTravelTime = stopArray.get(stopArray.size()-1).getArrivalOffset();
			transitRoute = Metro_TransitScheduleImpl.addDeparturesAndVehiclesToTransitRoute(metroScenario, metroSchedule, transitRoute,
					nDepartures, tFirstDep, depSpacing, totalRouteTravelTime, metroVehicleType, vehicleFileLocation); // Add (nDepartures) departures to TransitRoute
								
			// Build TransitLine from TrasitRoute
			TransitLine transitLine = metroScheduleFactory.createTransitLine(Id.create("TransitLine_Nr"+lineNr, TransitLine.class));
			transitLine.addRoute(transitRoute);
			
			// Add new line to schedule
			metroSchedule.addTransitLine(transitLine);			
		
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
		TransitScheduleWriter tsw = new TransitScheduleWriter(metroSchedule);
		tsw.writeFile(mNetworkPath+"/MetroSchedule.xml");
				
		String mergedNetworkFileName = "";
		if (useOdPairsForInitialRoutes==true) {
			mergedNetworkFileName = (mNetworkPath+"/MergedNetworkODInitialRoutes.xml");
		}
		else {
			mergedNetworkFileName = (mNetworkPath+"/MergedNetworkRandomInitialRoutes.xml");
		}
		Network mergedNetwork = Metro_TransitScheduleImpl.mergeRoutesNetworkToOriginalNetwork(separateRoutesNetwork, originalNetwork, Sets.newHashSet("pt"), mergedNetworkFileName);
		TransitSchedule mergedTransitSchedule = Metro_TransitScheduleImpl.mergeAndWriteTransitSchedules(metroSchedule, originalTransitSchedule, (mNetworkPath+"/MergedSchedule.xml"));
		Vehicles mergedVehicles = Metro_TransitScheduleImpl.mergeAndWriteVehicles(metroScenario.getTransitVehicles(), originalScenario.getTransitVehicles(), (mNetworkPath+"/MergedVehicles.xml"));
		
		// fill in MNetwork Objects for this Network
		mNetwork.network = mergedNetwork;
		mNetwork.transitSchedule = mergedTransitSchedule;
		mNetwork.vehicles = mergedVehicles;
		return mNetwork; // TODO !! %% must change here to give out networkRoutesArray or similar!
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
			double average = getAverageTrafficOnLinks(linksAboveThreshold);
			System.out.println("Average pt traffic on links (person arrivals + departures) is: " + average);
			System.out.println("Number of links above threshold is: " + linksAboveThreshold.size());

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
			double average = getAverageTrafficOnLinks(mostFrequentLinks);
			System.out.println("Average pt traffic on most frequent n=" + nMostFrequentLinks
					+ " links (person arrivals + departures) is: " + average);
			System.out.println("Number of most frequent links is: " + mostFrequentLinks.size());

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
									System.out.println("Did not recognize mode: " + mode + ", but adding it anyways...");
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
					System.out.println("Link "+selectedLinkID+" has no facility attached.");
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
			System.out.println("Size is: " + feasibleLinks.size());
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
			int l = 1;
			for (Id<Link> linkID : customLinkMap.keySet()) {
				System.out.println("linkID is: "+linkID.toString() );
				System.out.println("Custom attributes are: "+customLinkMap.get(linkID).toString());
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
				System.out.println("New node is called: " + newNode.getId().toString());
				System.out.println("Node counter l= " + l);
				newNetwork.addNode(newNode);
				l++;
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
					System.out.println("Terminal node 1 is not featured in new network: ");
				}
				int safetyCounter = 0;
				int iterLimit = 10000;
				do {
					Id<Link> randomTerminalLinkId2 = getRandomLink(links_MetroTerminalCandidates.keySet());
					terminalNode2 = Id.createNodeId("MetroNodeLinkRef_" + randomTerminalLinkId2.toString());
					safetyCounter++;
					if (safetyCounter == iterLimit) {
						System.out.println("Oops no second terminal node found after " + iterLimit + " iterations. Trying to create next networkRoute. "
								+ "Please lower minTerminalDistance!");
						continue OuterNetworkRouteLoop;
					}
				} while (GeomDistance.calculate(newMetroNetwork.getNodes().get(terminalNode1).getCoord(),
						newMetroNetwork.getNodes().get(terminalNode2).getCoord()) < minTerminalDistance
						&& safetyCounter < iterLimit);

				if (newMetroNetwork.getNodes().keySet().contains(terminalNode2) == false) {
					System.out.println("Terminal node 2 is not featured in new network: ");
				}

				// Find Djikstra --> nodeList
				ArrayList<Node> nodeList = DijkstraOwn_I.findShortestPathVirtualNetwork(newMetroNetwork, terminalNode1,
						terminalNode2);
				if (nodeList == null) {
						System.out.println("Oops, no shortest path available. Trying to create next networkRoute. Please lower minTerminalDistance"
								+ " ,or increase maxNewMetroLinkDistance (and - last - increase nMostFrequentLinks if required)!");
						System.out.println("Distance between terminals is "+GeomDistance.betweenNodes(newMetroNetwork.getNodes().get(terminalNode1), newMetroNetwork.getNodes().get(terminalNode2)));
						System.out.println("Coord of terminal1 is "+newMetroNetwork.getNodes().get(terminalNode1).getCoord());
						System.out.println("Coord of terminal2 is "+newMetroNetwork.getNodes().get(terminalNode2).getCoord());
						continue OuterNetworkRouteLoop;
				}
				List<Id<Link>> linkList = nodeListToNetworkLinkList(newMetroNetwork, nodeList);
				linkList.addAll(OppositeLinkListOf(linkList)); // extend linkList with its opposite direction for PT transportation!
				NetworkRoute networkRoute = RouteUtils.createNetworkRoute(linkList, newMetroNetwork);

				System.out.println("The new networkRoute is: [Length="+(networkRoute.getLinkIds().size()+2)+"] - " + networkRoute.toString());
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
			System.out.println("Something strange happened. Returning /null/ ...");
			return null;
		}

		public static Id<Node> getRandomNode(Set<Id<Node>> nodeSet) {
			Random rand = new Random();
			int nInt = rand.nextInt(nodeSet.size());
			int nodeCount = 0;
			for (Id<Node> nodeID : nodeSet) {
				if (nodeCount == nInt) {
					System.out.println("Returning Id<Link> " + nodeID);
					return nodeID;
				}
				nodeCount++;
			}
			System.out.println("Something strange happended. Returning /null/ ...");
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
			
			// XXX !!! CAUTION !!! XXX this does not work due to casting failure from stop facility to activity facility
			/*public static void allFeasibleStopFacilitiesToFile(Map<Id<Link>,CustomLinkAttributes> mostFeasibleLinks) {
				List<TransitStopFacility> allTransitRouteStopFacilities = new ArrayList<TransitStopFacility>(mostFeasibleLinks.size());
				for (Id<Link> linkID : mostFeasibleLinks.keySet()) {
					allTransitRouteStopFacilities.add(mostFeasibleLinks.get(linkID).dominantStopFacility);
				}
				FacilitiesWriter facilitiesWriter = new FacilitiesWriter((ActivityFacilities) allTransitRouteStopFacilities); // !!! FAILS
				facilitiesWriter.write("zurich_1pm/Metro/Input/Generated_PT_Files/newFacilities.xml");
			}*/
		
			
			
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
					System.out.println("bestAverageTravelTimeThisGeneration = " + bestAverageTravelTimeThisGeneration);
				}
				averageTravelTimeThisGeneration += nsl.averageTravelTime / networkScores.size();
				averageTravelTimeStdDevThisGeneration += nsl.stdDeviationTravelTime / networkScores.size();
			}
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
	
}






// TODO Method to run event handler NetworkPerformanceHandler(String EventsFilePath, int iteration, String NetworkID)	
// %%%%%%%%%%%%%%%%%%%%%% END HELPER METHODS STATIC %%%%%%%%%%%%%%%%%%%%%%%%%%%%
// %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
// %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
