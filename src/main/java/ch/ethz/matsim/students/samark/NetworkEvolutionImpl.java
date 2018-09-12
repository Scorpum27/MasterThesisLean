package ch.ethz.matsim.students.samark;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
			double minTerminalDistance, double proximityRadius, double odConsiderationThreshold, double xOffset, double yOffset,String vehicleTypeName, double vehicleLength, double maxVelocity,
			int vehicleSeats, int vehicleStandingRoom,String defaultPtMode, boolean blocksLane, double stopTime, double maxVehicleSpeed,
			double tFirstDep, double tLastDep, double depSpacing, int nDepartures,
			double metroOpsCostPerKM, double metroConstructionCostPerKmOverground, double metroConstructionCostPerKmUnderground ) throws IOException {

		Log.write("Creating Network = "+thisNewNetworkName);

		
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
				(mNetworkPath+"/1a_WithinRadius" + ((int) Math.round(metroCityRadius)) + ".xml"));
				// null); // FOR SAVING: replace (null) by (mNetworkPath+"/1a_WithinRadius" + ((int) Math.round(metroCityRadius)) + ".xml")
		
		// Find most frequent links from input links
		Map<Id<Link>, CustomLinkAttributes> links_mostFrequentInRadius = 
				NetworkEvolutionImpl.findMostFrequentLinks(nMostFrequentLinks, links_withinRadius, originalNetwork, null);

		// Set dominant transit stop facility in given network (from custom link list)
		Map<Id<Link>, CustomLinkAttributes> links_mostFrequentInRadiusMainFacilitiesSet = NetworkEvolutionImpl.setMainFacilities(originalTransitSchedule, 
				originalNetwork, links_mostFrequentInRadius, 
				(mNetworkPath+"/2a_MostFrequentInRadius.xml"));
				//null); // FOR SAVING: replace (null) by (mNetworkPath+"/2_MostFrequentInRadius.xml")

		// Merge close links and add their total traffic - this way different metro links are not squeezed next to each other
		Map<Id<Link>, CustomLinkAttributes> mergedLinks_mostFrequentInRadiusMainFacilitiesSet = NetworkEvolutionImpl.mergeLinksWithinBounds(
				links_mostFrequentInRadiusMainFacilitiesSet, proximityRadius, originalNetwork,
				(mNetworkPath+"/2b_MostFrequentInRadiusMERGED.xml"));
				// null); // FOR SAVING: replace (null) by (mNetworkPath+"/1b_mergedWithinRadius" + ((int) Math.round(metroCityRadius)) + ".xml")
		
		// Create a metro network from candidate links/stopFaiclities
		Network metroNetwork = NetworkEvolutionImpl.createMetroNetworkFromCandidates(
				mergedLinks_mostFrequentInRadiusMainFacilitiesSet, maxNewMetroLinkDistance, originalNetwork, 
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
			Map<Id<Link>, CustomLinkAttributes> links_MetroTerminalCandidates = NetworkEvolutionImpl.findLinksWithinBounds(mergedLinks_mostFrequentInRadiusMainFacilitiesSet, 
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
		
		
		public static Map<Id<Link>, CustomLinkAttributes> mergeLinksWithinBounds( Map<Id<Link>, CustomLinkAttributes> links_withinRadius, 
				double proximityRadius, Network originalNetwork, String fileName){
			
			Map<Id<Link>, CustomLinkAttributes> mergedLinks = new HashMap<Id<Link>, CustomLinkAttributes>();
			do {	
				List<Id<Link>> toBeDeletedLinks = new ArrayList<Id<Link>>();
				Iterator<Id<Link>> intIter = links_withinRadius.keySet().iterator();
				Id<Link> thisLink = intIter.next();
				toBeDeletedLinks.add(thisLink);

				mergedLinks.put(thisLink, links_withinRadius.get(thisLink));
				for (Id<Link> otherLink : links_withinRadius.keySet()) {
					if(thisLink.equals(otherLink)) {
						continue;
					}
//					Coord centerCoordThis = GeomDistance.coordBetweenNodes(originalNetwork.getLinks().get(thisLink).getFromNode(), originalNetwork.getLinks().get(thisLink).getToNode());
//					Coord centerCoordOther = GeomDistance.coordBetweenNodes(originalNetwork.getLinks().get(otherLink).getFromNode(), originalNetwork.getLinks().get(otherLink).getToNode());
					Coord CoordThis = links_withinRadius.get(thisLink).getDominantStopFacility().getCoord();    //originalNetwork.getLinks().get(thisLink).getFromNode().getCoord();
					Coord CoordOther = links_withinRadius.get(otherLink).getDominantStopFacility().getCoord();  //originalNetwork.getLinks().get(otherLink).getFromNode().getCoord();
					if(Math.abs(GeomDistance.calculate(CoordThis, CoordOther))<proximityRadius) {
						CustomLinkAttributes thisLinkAtt = links_withinRadius.get(thisLink);
						thisLinkAtt.totalTraffic += links_withinRadius.get(otherLink).totalTraffic;
						mergedLinks.put(thisLink, thisLinkAtt);
						toBeDeletedLinks.add(otherLink);
					}				
				}
				for (Id<Link> l : toBeDeletedLinks) {
					links_withinRadius.remove(l);
				}
			}while(links_withinRadius.size()>0);
			
			if (fileName != null) {
				createNetworkFromCustomLinks(mergedLinks, originalNetwork, fileName);
			}
			
			return mergedLinks;
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
				Map<Id<Link>, CustomLinkAttributes> links_MetroTerminalCandidates, int nRoutes, double minTerminalDistance) throws IOException {

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
				Log.write("Created Random-Links NetworkRoute with Links: "+"\r\n"+linkList.toString());
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
	public static void writeChartNetworkScore(int lastGeneration, String fileName) throws FileNotFoundException {
		Map<Integer, Double> generationsAverageNetworkScore = new HashMap<Integer, Double>();
		String generationPath = "zurich_1pm/Evolution/Population/HistoryLog/Generation";
		Map<Integer, Double> generationsBestNetworkScore = new HashMap<Integer, Double>();
		Map<String, NetworkScoreLog> networkScores = new HashMap<String, NetworkScoreLog>();
		for (int g = 1; g <= lastGeneration; g++) {
			double averageNetworkScoreThisGeneration = 0.0;
			double bestNetworkScoreThisGeneration = 0.0;
			networkScores = (Map<String, NetworkScoreLog>) XMLOps.readFromFile(networkScores.getClass(),
					generationPath + g + "/networkScoreMap.xml");
			for (NetworkScoreLog nsl : networkScores.values()) {
				if (nsl.overallScore > bestNetworkScoreThisGeneration) {
					bestNetworkScoreThisGeneration = nsl.overallScore;
				}
				averageNetworkScoreThisGeneration += nsl.overallScore / networkScores.size();
			}
			System.out.println("Best Network Score This Generation = " + bestNetworkScoreThisGeneration);
			generationsAverageNetworkScore.put(g, averageNetworkScoreThisGeneration);
			generationsBestNetworkScore.put(g, bestNetworkScoreThisGeneration);
		}
		XYLineChart chart = new XYLineChart("Evolution of Network Performance", "Generation", "Score");
		chart.addSeries("Average Network Score", generationsAverageNetworkScore);
		chart.addSeries("Best Network Score in Generation", generationsBestNetworkScore);
		chart.saveAsPng(fileName, 800, 600);
	}
			
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
			int vehicleSeats, int vehicleStandingRoom, String defaultPtMode, double stopTime, boolean blocksLane, boolean logEntireRoutes,
			double minCrossingDistanceFactorFromRouteEnd, double maxCrossingAngle, double pMutation, double pBigChange, double pSmallChange) throws IOException {
		
		MNetworkPop newPopulation = Clone.mNetworkPop(evoNetworksToProcessPlans);
		
		// ELITE NETWORK
		MNetwork eliteMNetwork = NetworkEvolutionImpl.getEliteNetwork(networkScoreMap, evoNetworksToProcessPlans);

		// CROSS-OVERS
		newPopulation = NetworkEvolutionImpl.applyCrossovers(globalNetwork,  networkScoreMap,  newPopulation,  populationName,
				 eliteMNetwork, alpha, pCrossOver, metroConstructionCostPerKmOverground,  metroConstructionCostPerKmUnderground, 
				 metroOpsCostPerKM, iterationToReadOriginalNetwork, useOdPairsForInitialRoutes, 
				 vehicleTypeName, vehicleLength, maxVelocity, vehicleSeats, vehicleStandingRoom, defaultPtMode, stopTime, blocksLane,
				 logEntireRoutes, minCrossingDistanceFactorFromRouteEnd, maxCrossingAngle);
		
		// MUTATIONS
		newPopulation = NetworkEvolutionImpl.applyMutations(newPopulation, globalNetwork, pMutation, pBigChange, pSmallChange,
				maxCrossingAngle, eliteMNetwork.networkID);

		// APPLY TRANSIT + STORE POPULATION & TRANSITSCHEDULE
		NetworkEvolutionImpl.applyPT(newPopulation, globalNetwork, vehicleTypeName, vehicleLength, maxVelocity, vehicleSeats, vehicleStandingRoom, defaultPtMode,
				stopTime, blocksLane, metroConstructionCostPerKmOverground, metroConstructionCostPerKmUnderground, metroOpsCostPerKM, iterationToReadOriginalNetwork,
				useOdPairsForInitialRoutes);
		
		// SAVE DATA TO FILES
		return newPopulation;
	}

	

	public static MNetwork getEliteNetwork(Map<String, NetworkScoreLog> networkScoreMap, MNetworkPop evoNetworksToProcessPlans) throws IOException {
		String eliteNetwork = "IfYouReadThisIn_LOG_FileThanNoEliteNetworkHasBeenFound";
		if (networkScoreMap.size() == 0) {		System.out.println("CAUTION: NetworkScoreMapSize is zero!");	}
		double maxNetworkScore = -Double.MAX_VALUE;
		for (String networkName : networkScoreMap.keySet()) {
			if (networkScoreMap.get(networkName).overallScore > maxNetworkScore) {
				maxNetworkScore = networkScoreMap.get(networkName).overallScore;
				eliteNetwork = networkName;
			}
		}
		MNetwork eliteMNetwork = evoNetworksToProcessPlans.getNetworks().get(eliteNetwork);
		Log.writeAndDisplay("  >> EliteNetwork = "+eliteNetwork+"  [ Score = " + maxNetworkScore + " ]");
		return eliteMNetwork;
	}


	public static MNetworkPop applyCrossovers(Network globalNetwork, Map<String, NetworkScoreLog> networkScoreMap, MNetworkPop newPopulation, String populationName, 
			MNetwork eliteMNetwork, double alpha, double pCrossOver, double metroConstructionCostPerKmOverground, double metroConstructionCostPerKmUnderground, 
			double metroOpsCostPerKM, int iterationToReadOriginalNetwork, boolean useOdPairsForInitialRoutes, 
			String vehicleTypeName, double vehicleLength, double maxVelocity, int vehicleSeats,
			int vehicleStandingRoom, String defaultPtMode, double stopTime, boolean blocksLane, boolean logEntireRoutes,
			double minCrossingDistanceFactorFromRouteEnd, double maxCrossingAngle) throws IOException {
		int nOldPop = newPopulation.networkMap.size();
		int nCrossOverCandidates = (int) Math.ceil(0.5*nOldPop);
		List<MNetwork> newOffspring = new ArrayList<MNetwork>();
		System.out.println("We will try nCrossOverCandidates="+nCrossOverCandidates);
		Log.writeAndDisplay("  >> Attempting crossover of nCrossOverCandidates="+nCrossOverCandidates);
		
		List<String> processedNetworks = new ArrayList<String>();
		Map<Integer, List<String>> executedMergers = new HashMap<Integer, List<String>>();
		for (int n=0; n<nCrossOverCandidates; n++) {
			Random r = new Random();
			if (r.nextDouble()<pCrossOver) {
				String nameParent1;
				String nameParent2;
				do {
					nameParent1 = NetworkEvolutionImpl.selectMNetworkByRoulette(alpha, networkScoreMap);
					System.out.println("ParentName 1="+nameParent1);
					do{
						nameParent2 = NetworkEvolutionImpl.selectMNetworkByRoulette(alpha, networkScoreMap);
						System.out.println("ParentName 2="+nameParent2);
					}while(nameParent1.equals(nameParent2));
				}while(NetworkEvolutionImpl.mergerHasBeenExecutedPreviously(executedMergers, nameParent1, nameParent2));
				executedMergers.put(n, Arrays.asList(nameParent1, nameParent2));
				Log.writeAndDisplay("  >> Crossing:  " + nameParent1 + " X " + nameParent2);
				MNetwork parentMNetwork1 = Clone.mNetwork(newPopulation.getNetworks().get(nameParent1));
				MNetwork parentMNetwork2 = Clone.mNetwork(newPopulation.getNetworks().get(nameParent2));
				MNetwork[] childrenMNetworks = NetworkEvolutionImpl.crossMNetworks(globalNetwork, parentMNetwork1, parentMNetwork2,
						vehicleTypeName, vehicleLength, maxVelocity, vehicleSeats, vehicleStandingRoom, defaultPtMode,
						stopTime, blocksLane, metroConstructionCostPerKmOverground,
						metroConstructionCostPerKmUnderground, metroOpsCostPerKM, iterationToReadOriginalNetwork,
						useOdPairsForInitialRoutes, minCrossingDistanceFactorFromRouteEnd, maxCrossingAngle);
				newOffspring.add(childrenMNetworks[0]);
				newOffspring.add(childrenMNetworks[1]);
			}
		}
		int nNewOffspring = newOffspring.size();
		System.out.println("nNewOffspring="+nNewOffspring);
		if(nNewOffspring != 0) {
			List<String> deletedNetworkNames = RemoveWeakestNetworks(newPopulation, nNewOffspring);
			processedNetworks.addAll(deletedNetworkNames);
			Log.write("  >> nNewOffspring=" + nNewOffspring + " --> Remove weakest networks: " + deletedNetworkNames.toString());
			for (int i=0; i<newOffspring.size(); i++) {
				RenameOffspring(deletedNetworkNames.get(i), newOffspring.get(i));	// renaming offspring with its MNetworkId and the Id of all its MRoutes
				newPopulation.addNetwork(newOffspring.get(i));
				Log.write("   >>> Putting New Offspring Network = " + newOffspring.get(i).networkID);
			}
		}
		if (nNewOffspring == nOldPop) {										// check with this condition if all old networks have been deleted for new offspring
			newPopulation.addNetwork(eliteMNetwork);						// if also elite network has been deleted, add manually again (it will replace the new one with the same name)
			processedNetworks.remove(eliteMNetwork.networkID);							// because this network remains unchanged for this generation as if it were not processed
			Log.write("   >>> Putting back removed ELITE NETWORK = " + eliteMNetwork.networkID);
		}
		Log.write("   >>> Processed Networks [CROSS-OVER] = " + processedNetworks.toString());
		if (logEntireRoutes) {
			for (MNetwork mn : newPopulation.networkMap.values()) {
				for (String mString : mn.routeMap.keySet()) {
					MRoute mr = mn.routeMap.get(mString);
					Log.writeAndDisplay(
							"   >>> " + mString + " = " + mr.linkList.subList(0, mr.linkList.size() / 2).toString());
				}
			}
		}
		newPopulation.modifiedNetworksInLastEvolution = processedNetworks;		// store which networks have not been changed and must therefore not be simulated again in next simulation loop!!
		return newPopulation;
	}


	public static MNetworkPop applyMutations(MNetworkPop newPopulation, Network globalNetwork, double pMutation, double pBigChange, double pSmallChange,
			double maxCrossingAngle, String eliteNetworkName) throws IOException {
		List<String> mutatedNetworks = new ArrayList<String>();
		List<Id<Link>> linkListMutate;
		for (String mNetworkName : newPopulation.networkMap.keySet()) {
			Log.write("Starting Mutation On: "+mNetworkName);
			boolean hasHadMutation = false;
			if (mNetworkName.equals(eliteNetworkName)) {
				continue;
			}
			MNetwork mNetwork = newPopulation.networkMap.get(mNetworkName);
			double averageRouletteScore = 0.0;
			for (MRoute mRoute : mNetwork.routeMap.values()) {
				averageRouletteScore +=  (mRoute.personMetroKM/mRoute.drivenKM)/mNetwork.routeMap.size();
			}
			for (MRoute mRoute : mNetwork.routeMap.values()) {
				Log.write("Checking for Mutation - "+mRoute.routeID);				
				Random rMutation = new Random();
				if (rMutation.nextDouble() < pMutation) {
					linkListMutate = mRoute.linkList.subList(0, mRoute.linkList.size()/2);
					Log.write("Mutating route: "+linkListMutate.toString());
					// make mutation of this route
					Random rBig = new Random();
					if (rBig.nextDouble() < pBigChange) { // make big change
						if(linkListMutate.size()>2) {
							hasHadMutation = true;
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
												connectingLinkReverse = globalNetwork.getFactory().createLink(ReverseLink(connectingLinkId), nextAfterOpenLink.getFromNode(), outLinkFrom.getToNode());  
											}
											else {
												connectingLink = globalNetwork.getLinks().get(connectingLinkId);
											}
											if(GeomDistance.angleBetweenLinks(connectingLink, nextAfterOpenLink) < maxCrossingAngle) {	// can make this condition harder!!
												feasibleCutLinkFound = true;
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
							linkListMutateComplete.addAll(OppositeLinkListOf(linkListMutateComplete));
							mRoute.linkList = Clone.list(linkListMutateComplete);
							mRoute.networkRoute = RouteUtils.createNetworkRoute(linkListMutateComplete, globalNetwork);
						}
					}
					else{ // make small change
						hasHadMutation = true;
						double weakeningFactor = 0.8; // factor to weaked dominant routes, which would just extend and extend (maybe make here like network score with exp function)
						double thisRouletteScore = mRoute.personMetroKM/mRoute.drivenKM;
						double pExtend = weakeningFactor*thisRouletteScore / (weakeningFactor*thisRouletteScore + averageRouletteScore);
						Random rExt = new Random();
						double rExtDouble = rExt.nextDouble();
						if (rExtDouble < pExtend) { // extend route // TODO this should be done with better condition e.g. abs. profitability instead of rel. performance!
							Random rEnd = new Random();
							if(rEnd.nextDouble() < 0.5) { // add on start link
								Link startLink = globalNetwork.getLinks().get(linkListMutate.get(0));
								for (Id<Link> previousLink : globalNetwork.getNodes().get(startLink.getFromNode().getId()).getInLinks().keySet()) {
									if(GeomDistance.angleBetweenLinks(globalNetwork.getLinks().get(previousLink), startLink)<maxCrossingAngle) {
										linkListMutate.add(0, previousLink);
										break;
									}
								}
							}
							else {  //	add on end link
								Link endLink = globalNetwork.getLinks().get(linkListMutate.get(linkListMutate.size()-1));
								for (Id<Link> nextLink : globalNetwork.getNodes().get(endLink.getToNode().getId()).getOutLinks().keySet()) {
									if(GeomDistance.angleBetweenLinks(endLink, globalNetwork.getLinks().get(nextLink))<maxCrossingAngle) {
										linkListMutate.add(nextLink);
										break;
									}
								}
							}		
						}
						else { // shorten route
							Random rEnd = new Random();
							if(rEnd.nextDouble() < 0.5) { // shorten on start link
								linkListMutate = linkListMutate.subList(1, linkListMutate.size());
							}
							else {  //	shorten on end link
								linkListMutate = linkListMutate.subList(0, linkListMutate.size()-1);
							}
						}
						linkListMutate.addAll(OppositeLinkListOf(linkListMutate));
						mRoute.linkList = Clone.list(linkListMutate);
						mRoute.networkRoute = RouteUtils.createNetworkRoute(linkListMutate, globalNetwork);
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
		newPopulation.modifiedNetworksInLastEvolution.addAll(mutatedNetworks);
		Log.write("   >>> Processed Networks [MUTATION] = " + mutatedNetworks.toString()); 
		
		NetworkWriter nw = new NetworkWriter(globalNetwork);
		nw.write("zurich_1pm/Evolution/Population/GlobalMetroNetwork.xml");
		return newPopulation;
	}


	public static MNetworkPop applyPT(MNetworkPop newPopulation, Network globalNetwork, String vehicleTypeName,
			double vehicleLength, double maxVelocity, int vehicleSeats, int vehicleStandingRoom, String defaultPtMode,
			double stopTime, boolean blocksLane, double metroConstructionCostPerKmOverground,
			double metroConstructionCostPerKmUnderground, double metroOpsCostPerKM, int iterationToReadOriginalNetwork,
			boolean useOdPairsForInitialRoutes) throws IOException {
		
		Config originalConfig = ConfigUtils.loadConfig("zurich_1pm/zurich_config.xml");
		Scenario originalScenario = ScenarioUtils.loadScenario(originalConfig);
		originalScenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DefaultEnrichedTransitRoute.class, new DefaultEnrichedTransitRouteFactory());
		Network originalNetwork = originalScenario.getNetwork();
		TransitSchedule originalTransitSchedule = originalScenario.getTransitSchedule();
		
		for (MNetwork mNetwork : newPopulation.networkMap.values()) {
			if (newPopulation.modifiedNetworksInLastEvolution.contains(mNetwork.networkID)==false) {
				continue;
			}
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
			for (String mRouteName : mNetwork.routeMap.keySet()) {
				MRoute mRoute = mNetwork.routeMap.get(mRouteName);
				Log.write("Adding PT to route: "+ mRouteName);
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
		
		return newPopulation;
	}


	public static boolean mergerHasBeenExecutedPreviously(Map<Integer, List<String>> executedMergers, String nameParent1, String nameParent2) {
		for (List<String> mergedNetworks : executedMergers.values()) {
			if (mergedNetworks.contains(nameParent1) && mergedNetworks.contains(nameParent2)) {
				return true;
			}
		}
		return false;
	}


	public static MNetwork[] crossMNetworks(Network globalNetwork, MNetwork parentMNetwork1, MNetwork parentMNetwork2, String vehicleTypeName, double vehicleLength, double maxVelocity, 
			int vehicleSeats, int vehicleStandingRoom, String defaultPtMode, double stopTime, boolean blocksLane, double metroConstructionCostPerKmOverground,
			double metroConstructionCostPerKmUnderground, double metroOpsCostPerKM, int iterationToReadOriginalNetwork, boolean useOdPairsForInitialRoutes,
			double minCrossingDistanceFactorFromRouteEnd, double maxCrossingAngle) throws IOException {
			
		Map<String, MRoute> routesParent1 = Clone.mRouteMap(parentMNetwork1.getRouteMap());
		Map<String, MRoute> routesParent2 = Clone.mRouteMap(parentMNetwork2.getRouteMap());
		Map<String, MRoute> routesOut1 = new HashMap<String, MRoute>();
		Map<String, MRoute> routesOut2 = new HashMap<String, MRoute>();
		
		Iterator<Entry<String, MRoute>> iter1 = routesParent1.entrySet().iterator();
		
		Loop1:
		while (iter1.hasNext()) {
			Entry<String, MRoute> entry1 = iter1.next();
			MRoute routeFromP1 = entry1.getValue();
			Iterator<Entry<String, MRoute>> iter2 = routesParent2.entrySet().iterator();
			while (iter2.hasNext()) {
				Entry<String, MRoute> entry2 = iter2.next();
				MRoute routeFromP2 = entry2.getValue();
				MRoute[] crossedRoutes = crossMRoutes(routeFromP1, routeFromP2, globalNetwork, minCrossingDistanceFactorFromRouteEnd, maxCrossingAngle);
				if (crossedRoutes != null) {
					//Log.writeAndDisplay("   >>> MRoute Cross Success:  " + routeP1name + " X " + routeP2name);
					routesOut1.put(crossedRoutes[0].routeID, crossedRoutes[0]);
					routesOut2.put(crossedRoutes[1].routeID, crossedRoutes[1]);
					iter2.remove();
					iter1.remove();
					continue Loop1;
				}
				//else {Log.writeAndDisplay("   >>> MRoute Cross FAIL:  " + routeP1name + " X " + routeP2name);}
			}
			// this will come in place if inner loop has not found a feasible crossing and has therefore not broken inner loop to jump to outer loop
			routesOut1.put(routeFromP1.routeID, routeFromP1);	
			iter1.remove();
		}
		for (MRoute routeFromP2 : routesParent2.values()) { // add all routesFromP2 that could not be crossed with any routesFromP1
			if (routesOut2.containsKey(routeFromP2.routeID)==false) {
				routesOut2.put(routeFromP2.routeID, routeFromP2);
			}
		}
		
		MNetwork mnetworkOut1 = new MNetwork(parentMNetwork1.networkID);
		MNetwork mnetworkOut2 = new MNetwork(parentMNetwork2.networkID);
		mnetworkOut1.routeMap = routesOut1;
		mnetworkOut2.routeMap = routesOut2;
		MNetwork[] mNetworksOut = new MNetwork[] {mnetworkOut1, mnetworkOut2};
		
		return mNetworksOut;
	}



	public static MRoute[] crossMRoutes(MRoute routeFromP1, MRoute routeFromP2, Network globalNetwork,
			double minCrossingDistanceFactorFromRouteEnd, double maxCrossingAngle) throws IOException {
		
		List<Id<Link>> route1LinkListOneway = new ArrayList<Id<Link>>();
		List<Id<Link>> route2LinkListOneway = new ArrayList<Id<Link>>();
		Map<Integer, List<Id<Link>>> routesCandidates1 = new HashMap<Integer, List<Id<Link>>>();
		Map<Integer, List<Id<Link>>> routesCandidates2 = new HashMap<Integer, List<Id<Link>>>();
		routesCandidates1.put(0, routeFromP1.linkList.subList(0, routeFromP1.linkList.size()/2));
		routesCandidates2.put(0, routeFromP2.linkList.subList(0, routeFromP2.linkList.size()/2));		
		routesCandidates1.put(1, routeFromP1.linkList.subList(routeFromP1.linkList.size()/2, routeFromP1.linkList.size()));
		routesCandidates2.put(1, routeFromP2.linkList.subList(0, routeFromP2.linkList.size()/2));
		routesCandidates1.put(2, routeFromP1.linkList.subList(0, routeFromP1.linkList.size()/2));
		routesCandidates2.put(2, routeFromP2.linkList.subList(routeFromP2.linkList.size()/2, routeFromP2.linkList.size()));
		routesCandidates1.put(3, routeFromP1.linkList.subList(routeFromP1.linkList.size()/2, routeFromP1.linkList.size()));
		routesCandidates2.put(3, routeFromP2.linkList.subList(routeFromP2.linkList.size()/2, routeFromP2.linkList.size()));
		
		MRoute mRouteNew1 = new MRoute(routeFromP1.routeID);
		MRoute mRouteNew2 = new MRoute(routeFromP2.routeID);
		MRoute[] crossedRoutes = new MRoute[2];
		Id<Link> crossLink1 = null;
		Id<Link> crossLink2 = null;
		boolean crossingFound = false;
		
		Link link1BeforeNode;
		Link link1AfterNode;
		Link link2BeforeNode;
		Link link2AfterNode;
		
		OUTERLOOP:
		while(routesCandidates1.size() > 0 && crossingFound == false) {
			int ri;
			do {
			Random r = new Random();
			ri = r.nextInt(4);
			}while(routesCandidates1.keySet().contains(ri)==false);
			route1LinkListOneway = routesCandidates1.remove(ri); 
			route2LinkListOneway = routesCandidates2.remove(ri);
			int routeLength1 = route1LinkListOneway.size();
			int routeLength2 = route2LinkListOneway.size();
			if( routeLength1 < 2 || routeLength2 < 2 ) {
				System.out.println("One of the MRoute parents has length<2xlinks. Crossing is prohibited.");
				continue;
			}
			for (Id<Link> linkFrom1 : route1LinkListOneway.subList((int) Math.floor((minCrossingDistanceFactorFromRouteEnd)*routeLength1), (int) Math.ceil((1-minCrossingDistanceFactorFromRouteEnd)*routeLength1))) {
				Node fromNode1 = globalNetwork.getLinks().get(linkFrom1).getFromNode();
				for (Id<Link> linkFrom2 : route2LinkListOneway.subList((int) Math.floor((minCrossingDistanceFactorFromRouteEnd)*routeLength2), (int) Math.ceil((1-minCrossingDistanceFactorFromRouteEnd)*routeLength2))) {
					Node fromNode2 = globalNetwork.getLinks().get(linkFrom2).getFromNode();
					if (fromNode1.getId().toString().equals(fromNode2.getId().toString())) {
						link1AfterNode = globalNetwork.getLinks().get(linkFrom1);
						link2AfterNode = globalNetwork.getLinks().get(linkFrom2);
						if (route1LinkListOneway.indexOf(linkFrom1)==0) {
							link1BeforeNode = link2AfterNode; // this is not per se true, but this is only for taking the direction of the links (and we want direction of link2AfterNode)
						}
						else {
							link1BeforeNode = globalNetwork.getLinks().get(route1LinkListOneway.get(route1LinkListOneway.indexOf(linkFrom1)-1));
						}
						if (route2LinkListOneway.indexOf(linkFrom2)==0) {
							link2BeforeNode = link1AfterNode; // this is not per se true, but this is only for taking the direction of the links (and we want direction of link1AfterNode)
						}
						else {
							link2BeforeNode = globalNetwork.getLinks().get(route2LinkListOneway.get(route2LinkListOneway.indexOf(linkFrom2)-1));
						}
						//Log.write("Angles were: "+GeomDistance.angleBetweenLinks(link1BeforeNode,link2AfterNode)+"  ||   "+GeomDistance.angleBetweenLinks(link2BeforeNode,link1AfterNode));
						if (GeomDistance.angleBetweenLinks(link1BeforeNode,link2AfterNode) > maxCrossingAngle || GeomDistance.angleBetweenLinks(link2BeforeNode,link1AfterNode) > maxCrossingAngle) {
							continue;
						}
						else {
							crossingFound = true;
							crossLink1 = linkFrom1;		// crossing takes place at from link!
							crossLink2 = linkFrom2;		// crossing takes place at from link!
							//System.out.println("YES - FromNode found in route2: "+fromNode1.getId().toString());
							continue OUTERLOOP;
						}
					}
					//System.out.println("NO - FromNode not found in route2: "+fromNode1.getId().toString());
				}
			}
		}
		// OLD Version: Check BEFORE 11.09.2018 - 12:00
		
		if(crossingFound) {
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
			mRouteNew1.drivenKM = (int) Math.round(0.5*(routeFromP1.drivenKM+routeFromP2.drivenKM));
			mRouteNew2.drivenKM = (int) Math.round(0.5*(routeFromP1.drivenKM+routeFromP2.drivenKM));
			mRouteNew1.personMetroKM = (int) Math.round(0.5*(routeFromP1.personMetroKM+routeFromP2.personMetroKM));
			mRouteNew2.personMetroKM = (int) Math.round(0.5*(routeFromP1.personMetroKM+routeFromP2.personMetroKM));
			mRouteNew1.firstDeparture = (int) Math.round(0.5*(routeFromP1.firstDeparture+routeFromP2.firstDeparture));
			mRouteNew2.firstDeparture = (int) Math.round(0.5*(routeFromP1.firstDeparture+routeFromP2.firstDeparture));
			mRouteNew1.departureSpacing = (int) Math.round(0.5*(routeFromP1.departureSpacing+routeFromP2.departureSpacing));
			mRouteNew2.departureSpacing = (int) Math.round(0.5*(routeFromP1.departureSpacing+routeFromP2.departureSpacing));
			mRouteNew1.nDepartures = (int) Math.round(0.5*(routeFromP1.nDepartures+routeFromP2.nDepartures));
			mRouteNew2.nDepartures = (int) Math.round(0.5*(routeFromP1.nDepartures+routeFromP2.nDepartures));
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
			MRoute mrTemp = Clone.mRoute(mRoute);
			mrTemp.routeID = newNetworkName+"_Route"+counter;
			newRoutesMap.put(mrTemp.routeID, mrTemp);
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
		// Usually Set<String> networkRouteModes = Sets.newHashSet("pt")
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

	public static Network MRouteToNetwork(MRoute mRoute, Network network, Set<String> networkRouteModes, String fileName) {
		// Store all new networkRoutes in a separate network file for visualization
		// Usually Set<String> networkRouteModes = Sets.newHashSet("pt")
		Network routesNetwork = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getNetwork();
		NetworkFactory networkFactory = routesNetwork.getFactory();
		NetworkRoute nR = mRoute.networkRoute;
		List<Id<Link>> routeLinkList = new ArrayList<Id<Link>>();
		routeLinkList.add(nR.getStartLinkId());
		routeLinkList.addAll(nR.getLinkIds());
		routeLinkList.add(nR.getEndLinkId());
		for (Id<Link> linkID : routeLinkList) {
			Node tempToNode = networkFactory.createNode(network.getLinks().get(linkID).getToNode().getId(),
					network.getLinks().get(linkID).getToNode().getCoord());
			Node tempFromNode = networkFactory.createNode(network.getLinks().get(linkID).getFromNode().getId(),
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
		NetworkWriter initialRoutesNetworkWriter = new NetworkWriter(routesNetwork);
		initialRoutesNetworkWriter.write(fileName);
		
		return routesNetwork;
	}

	public static void saveCurrentMRoutes2HistoryLog(MNetworkPop latestPopulation, int generationNr, Network globalNetwork) throws FileNotFoundException {
		String historyFileLocation = "zurich_1pm/Evolution/Population/HistoryLog/Generation"+(generationNr-1)+"/MRoutes";
		new File(historyFileLocation).mkdirs();
		for (MNetwork mn : latestPopulation.getNetworks().values()) {
			for (MRoute mr : mn.getRouteMap().values()) {
				XMLOps.writeToFile(mr, historyFileLocation+"/"+mr.routeID+"_RoutesFile.xml");
				// make a separate network of all these individual mRoutes for visualization purposes (individual routes)
				NetworkEvolutionImpl.MRouteToNetwork(mr, globalNetwork,  Sets.newHashSet("pt"), historyFileLocation+"/"+mr.routeID+"_NetworkFile.xml");
			}
			// make a separate network of all mRoutes of one network for visualization purposes (all routes of a network)
			NetworkEvolutionImpl.MRoutesToNetwork(mn.getRouteMap(), globalNetwork, 
					Sets.newHashSet("pt"), historyFileLocation+"/MRoutes"+mn.networkID+".xml");
		}
	}
	
	public static Network updateGlobalNetwork(String globalNetworkFile) {
		Config config = ConfigUtils.createConfig();
		config.getModules().get("network").addParam("inputNetworkFile", "zurich_1pm/Evolution/Population/GlobalMetroNetwork.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Network globalNetwork = scenario.getNetwork();
		return globalNetwork;
	}
	
}

