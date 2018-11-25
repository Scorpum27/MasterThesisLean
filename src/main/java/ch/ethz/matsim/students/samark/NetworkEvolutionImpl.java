package ch.ethz.matsim.students.samark;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.SeriesDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.VehicleType;

import com.google.common.collect.Sets;

import ch.ethz.matsim.baseline_scenario.transit.routing.DefaultEnrichedTransitRoute;
import ch.ethz.matsim.baseline_scenario.transit.routing.DefaultEnrichedTransitRouteFactory;

public class NetworkEvolutionImpl {

	public static MNetworkPop createMNetworks(String populationName, int populationSize, int initialRoutesPerNetwork, String initialRouteType, 
			String shortestPathStrategy, int iterationToReadOriginalNetwork, int lastIterationOriginal, Integer iterationsToAverage, double minMetroRadiusFromCenter,
			double maxMetroRadiusFromCenter, double maxExtendedMetroRadiusFromCenter, Coord zurich_NetworkCenterCoord, double metroCityRadius, 
			int nMostFrequentLinks, double maxNewMetroLinkDistance, double minTerminalRadiusFromCenter, double maxTerminalRadiusFromCenter, double minInitialTerminalDistance, 
			double minInitialTerminalRadiusFromCenter, double maxInitialTerminalRadiusFromCenter, boolean varyInitRouteSize,
			boolean mergeMetroWithRailway, double railway2metroCatchmentArea, double metro2metroCatchmentArea,
			double odConsiderationThreshold, boolean useOdPairsForInitialRoutes, double xOffset, double yOffset, double populationFactor,
			String vehicleTypeName, double vehicleLength, double maxVelocity, int vehicleSeats, int vehicleStandingRoom,String defaultPtMode, 
			boolean blocksLane, double stopTime, double maxVehicleSpeed, double tFirstDep, double tLastDep, double initialDepSpacing, double lifeTime
			) throws IOException {

		String networkPath = "zurich_1pm/Evolution/Population/BaseInfrastructure";
		new File(networkPath).mkdirs();
		
		Config originalConfig = ConfigUtils.loadConfig("zurich_1pm/zurich_config.xml");
		Scenario originalScenario = ScenarioUtils.loadScenario(originalConfig);
		originalScenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DefaultEnrichedTransitRoute.class, new DefaultEnrichedTransitRouteFactory());
		Network originalNetwork = originalScenario.getNetwork();
		TransitSchedule originalTransitSchedule = originalScenario.getTransitSchedule();
		
		// calculate average speed in Original Zurich TransitSchedule: Do this only once and put in commentaries afterwards
		 NetworkEvolutionImpl.CalculateAverageNetworkSpeed(originalTransitSchedule, originalNetwork, "zurich_1pm/zurich_transit_schedule_meanSpeed.xml");
		
		// CostBenefitAnalysis: calculate travel stats for original network at lastIterationOriginal,
		// 						which should be the same iteration number as the one that will be simulated
			// -nPersons PT;  		-nPersons MPT;
			// -totalLegKM PT;		-totalLegKM MPT;
			// -aveTravTime PT;		-aveTravTime MPT;
		
		String plansFolder = "zurich_1pm/Zurich_1pm_SimulationOutputEnriched/ITERS";
		if ( ! (new File("zurich_1pm/cbpParametersOriginal/cbpParametersOriginalGlobal.xml")).exists()) {
			(new File("zurich_1pm/cbpParametersOriginal")).mkdirs();
			String outputFile = "zurich_1pm/cbpParametersOriginal/cbpParametersOriginalGlobal.xml";
			NetworkEvolutionImpl.calculateCBAStats(plansFolder, outputFile, (int) populationFactor, 100, 80);
		}
		
		
		// Initialize a customLinkMap with all links from original network
		Map<Id<Link>, CustomLinkAttributes> allOriginalLinks = NetworkEvolutionImpl.createCustomLinkMap(originalNetwork, null);
		
		// Run event handler to count movements on each stop facility of original map and add traffic data to customLinkMap
		Map<Id<Link>, CustomLinkAttributes> trafficProcessedLinkMap = NetworkEvolutionImpl.runPTStopTrafficScanner(
				new PT_StopTrafficCounter(), allOriginalLinks, iterationToReadOriginalNetwork, originalNetwork, null);
		
		// Select all metro candidate links by setting bounds on their location (distance from city center)	
		Map<Id<Link>, CustomLinkAttributes> links_withinRadius = NetworkEvolutionImpl.findLinksWithinBounds(
				trafficProcessedLinkMap , originalNetwork, zurich_NetworkCenterCoord, minMetroRadiusFromCenter, maxMetroRadiusFromCenter,
				(networkPath+"/1a_WithinRadius" + ((int) Math.round(metroCityRadius)) + ".xml"));
				// null); // FOR SAVING: replace (null) by (networkPath+"/1a_WithinRadius" + ((int) Math.round(metroCityRadius)) + ".xml")

		// Find most frequent links from input links
		Map<Id<Link>, CustomLinkAttributes> links_mostFrequentInRadius = 
				NetworkEvolutionImpl.findMostFrequentLinks(nMostFrequentLinks, links_withinRadius, originalNetwork, null);
	
		// Set dominant transit stop facility in given network (from custom link list)
		// On these highly frequented links find for each the most dominant stop facility (by mode e.g. rail) attached to them
		Map<Id<Link>, CustomLinkAttributes> links_mostFrequentInRadiusMainFacilitiesSet = NetworkEvolutionImpl.setMainFacilities(originalTransitSchedule, 
				originalNetwork, links_mostFrequentInRadius, 
				(networkPath+"/2a_MostFrequentInRadius.xml"));
				//null); // FOR SAVING: replace (null) by (networkPath+"/2_MostFrequentInRadius.xml")
		
		// Extract current rails network [stations, originalLinks] - Do this now in order to hand over stations so that linksMerging can take place around railStations
				// Maybe delete <<metroLinkAttributesOriginalScenario>> because it is not (yet) required.
				// This is for extracting all original railwayStations with
					// - String=SuperName (only first part of TransitStopFacilityId, which is identical for all stopFacilities around one station with different refLinks)
					// - CustomRailStop=Facility|Name|LinkRefIds|Node
				Map<String, CustomStop> railStops = new HashMap<String, CustomStop>();
				railStops = NetworkEvolutionImpl.getOriginalRailwayStations(maxExtendedMetroRadiusFromCenter, zurich_NetworkCenterCoord, railStops);
				
				// 1. Frequent link stop facilities within 150m range of a railway stop facility are replaced be the railway stop facility for economic reasons
				//    Take care, may add several custom links to one railway station --> have to merge them and remove "duplicates"
				// 2. Merge close frequent links + add their total traffic to one dominant stopFacility -> this way metro links are not squeezed unreasonably next to each other
				// 3. Finally, for all, set a reference railway facility if it is within bounds!
				// In the next steps only the remaining domStopFacilities will be taken and used as starting point for the new metro network nodes
		Map<Id<Link>, CustomLinkAttributes> mergedLinks_mostFrequentInRadiusMainFacilitiesSet = NetworkEvolutionImpl.mergeLinksWithinBounds(
				links_mostFrequentInRadiusMainFacilitiesSet, railway2metroCatchmentArea, metro2metroCatchmentArea, railStops, originalNetwork,
				(networkPath+"/2b_MostFrequentInRadiusMERGED.xml"));
				// null); // FOR SAVING: replace (null) by (networkPath+"/2b_MostFrequentInRadiusMERGED.xml"));
		

		// %%% EVERYTHING IN OLD SYSTEM UP TO HERE %%% CONVERSIONS %%%
		// 	Get [new map] node from [old map] refLink: Node newMapNode = newNetwork.getNodes.get(Id.createNodeId("MetroNodeLinkRef_"+oldMapRefLink.toString()))
		// 	---> Id<Node> metroNodeId = metroNodeFromOriginalLink(Id<Link> originalLinkRefID) 
		// 	Get [old map] refLink from [new map] node: Link oldMapLink = newMapNode.parse
		// 	---> Id<Link> originalLinkId = orginalLinkFromMetroNode(Id<Node> metroNodeId)

		
		// Make a New innerCity network:
					// - Create and add to newInnerCityNetwork a new node for each domFacility at its coordinates --> Interconnect nodes by new metroLinks
					// - Build a new clone as metroStopFacility
					// - Build map with newMetroStopAttributes and build a map of newMetroLinkAttributes such as neighboring stop facilities
					// - Update by superName if facility is featured in railwayFacilities
		
		
		Map<Id<Link>, CustomMetroLinkAttributes> metroLinkAttributes = new HashMap<Id<Link>, CustomMetroLinkAttributes>();
		Map<String, CustomStop> innerCityMetroStops = new HashMap<String, CustomStop>();	// to save all details of new cloned stops
		Network innerCityMetroNetwork = NetworkEvolutionImpl.createMetroNetworkFromCandidates( mergedLinks_mostFrequentInRadiusMainFacilitiesSet,
				railStops, innerCityMetroStops, metroLinkAttributes, maxNewMetroLinkDistance, originalNetwork, networkPath+"/MetroStopFacilities.xml",
				networkPath+"/3a_MetroNetworkInnerCity.xml");
				//null); // FOR SAVING: replace (null) by (networkPath+"/4_MetroNetwork.xml"))
		
		
		// Extract copy of railway network from Zürich Scenario and convert (rename and connect to facilities) it to a metroNetwork:
		// Go through customRailStops (=all Zurich railway stopFacilities):
			// - For all Facilities not added to transitSchedule yet (marked in customRailStops), create & add node & facility to new transitSchedule, mark in customRailStops
			// - For all Facilities already added to innerCitySchedule: Create and add IDENTICAL node to isolated railwayNetwork --> CONSISTENCY WHEN MERGING later on!!
		// Take existing innerCityFacilities
		// Run along TransitRoutes and create new metro links and nodes (also make correctly named new facility node connectors) 
		// CAUTION: may have to update metro link attributes along the way with the new stopFacilities!
		
		Network metroNetwork = null;
		Map<String, CustomStop> allMetroStops = new HashMap<String, CustomStop>();
		if (mergeMetroWithRailway == true) { // merge with outerCity existing metroNetwork
			Map<String, CustomStop> outerCityMetroStops = new HashMap<String, CustomStop>();		// to save all details of new stops
			Network outerCityMetroNetwork = null;
			outerCityMetroNetwork = NetworkEvolutionImpl.createMetroNetworkFromRailwayNetwork(
					railStops, outerCityMetroStops, metroLinkAttributes, originalNetwork, innerCityMetroNetwork, networkPath+"/MetroStopFacilities.xml",
					networkPath+"/3b_MetroNetworkOuterCity.xml");
					//null); // FOR SAVING: replace (null) by (networkPath+"/4_MetroNetwork.xml"))
			// Merge innerCity newMetro network with outerCity (railway2newMetro) network if desired
			// The schedule has already been merged on the way (contains only stopFacilities by this point)
			metroNetwork = NetworkOperators.superimpose2separateNetwork(
					innerCityMetroNetwork, Sets.newHashSet("pt"), outerCityMetroNetwork, Sets.newHashSet("pt"), null);
			connectRailStopsInMetroNetworkToSurroundingNodes(metroNetwork, railStops, maxNewMetroLinkDistance, Sets.newHashSet("pt"),
					metroLinkAttributes,  networkPath + "/MetroStopFacilities.xml");
			allMetroStops.putAll(innerCityMetroStops);
			allMetroStops.putAll(outerCityMetroStops);			
			
			// Manual inputs here
			addManualInputs(metroNetwork, metroLinkAttributes, allMetroStops, networkPath+"/MetroStopFacilities.xml");
		}
		else {
			metroNetwork = innerCityMetroNetwork;
			allMetroStops.putAll(innerCityMetroStops);
		}
		
		
		List<TransitStopFacility> terminalFacilityCandidates = new ArrayList<TransitStopFacility>();
		if (useOdPairsForInitialRoutes==false) {								
			terminalFacilityCandidates = NetworkEvolutionImpl.findFacilitiesWithinBounds( networkPath + "/MetroStopFacilities.xml",
					zurich_NetworkCenterCoord, minTerminalRadiusFromCenter, maxTerminalRadiusFromCenter,
					(networkPath + "/4_MetroTerminalCandidateNodeLocations.xml"));
					// null); // FOR SAVING: replace (null) by (networkPath+"/4_MetroTerminalCandidate.xml"));
		}
		
		// STORE GLOBAL -NEW- NETWORK WITH ALL METRO LINKS + TOTALMETRONETWORK MERGED TO ZH_NETWORK
		NetworkWriter metroNetworkWriter = new NetworkWriter(metroNetwork);
		metroNetworkWriter.write("zurich_1pm/Evolution/Population/BaseInfrastructure/TotalMetroNetwork.xml");
//		NetworkOperators.superimposeNetworks(originalNetwork, null, metroNetwork, 
//				Sets.newHashSet("pt"), "zurich_1pm/Evolution/Population/BaseInfrastructure/GlobalNetwork.xml");
		NetworkOperators.superimpose2separateNetwork(originalNetwork, null, metroNetwork,  Sets.newHashSet("pt"),
				"zurich_1pm/Evolution/Population/BaseInfrastructure/GlobalNetwork.xml");
		XMLOps.writeToFile(metroLinkAttributes, "zurich_1pm/Evolution/Population/BaseInfrastructure/metroLinkAttributes.xml");
		XMLOps.writeToFile(allMetroStops, "zurich_1pm/Evolution/Population/BaseInfrastructure/metroStopAttributes.xml");
		
		MNetworkPop networkPopulation = new MNetworkPop(populationName, populationSize);			// Initialize population of networks
		
		for (int N=1; N<=populationSize; N++) {														// Make individual networks one by one in loop

			String thisNewNetworkName = ("Network"+N);												// Name networks by their number [1;populationSize]
			Log.write("Creating Network = "+thisNewNetworkName);
			
			MNetwork mNetwork = new MNetwork(thisNewNetworkName);
			mNetwork.lifeTime = lifeTime;
			String mNetworkPath = "zurich_1pm/Evolution/Population/"+thisNewNetworkName;
			new File(mNetworkPath).mkdirs();
			
			ArrayList<NetworkRoute> initialMetroRoutes = null;
			Network separateRoutesNetwork = null;

			if (useOdPairsForInitialRoutes==false) {
				initialMetroRoutes = NetworkEvolutionImpl.createInitialRoutesRandom(metroNetwork, shortestPathStrategy,
						terminalFacilityCandidates, allMetroStops, initialRoutesPerNetwork, zurich_NetworkCenterCoord, metroCityRadius, varyInitRouteSize,
						minInitialTerminalDistance, minInitialTerminalRadiusFromCenter, maxInitialTerminalRadiusFromCenter);
				// CAUTION: If NullPointerException, probably maxTerminalRadius >  metroNetworkRadius
				separateRoutesNetwork = NetworkOperators.networkRoutesToNetwork(initialMetroRoutes, metroNetwork,
						Sets.newHashSet("pt"), (mNetworkPath + "/0_MetroInitialRoutes_Random.xml"));
			}
			else if (useOdPairsForInitialRoutes==true) {	
				// Initial Routes OD_Pairs within bounds
				initialMetroRoutes = OD_ProcessorImpl.createInitialRoutesOD(metroNetwork, initialRoutesPerNetwork,
						minTerminalRadiusFromCenter, maxTerminalRadiusFromCenter, odConsiderationThreshold,
						zurich_NetworkCenterCoord, "zurich_1pm/Evolution/Input/Data/OD_Input/Demand2013_PT.csv",
						"zurich_1pm/Evolution/Input/Data/OD_Input/OD_ZoneCodesLocations.csv", xOffset, yOffset);
				// CAUTION: Make sure .csv is separated by semi-colon because location names also include commas sometimes and lead to failure!!
				separateRoutesNetwork = NetworkOperators.networkRoutesToNetwork(initialMetroRoutes, metroNetwork,
						Sets.newHashSet("pt"), (mNetworkPath + "/0_MetroInitialRoutes_OD.xml"));
			}
			
			
			// Load & Create Schedules and Factories
			Config newConfig = ConfigUtils.createConfig();						// this is totally default and may be modified as required
			Scenario newScenario = ScenarioUtils.createScenario(newConfig);
			TransitSchedule metroSchedule = newScenario.getTransitSchedule();
			TransitScheduleFactory metroScheduleFactory = metroSchedule.getFactory();
					
			// Create a New Metro Vehicle
			VehicleType metroVehicleType = Metro_TransitScheduleImpl.createNewVehicleType(vehicleTypeName, vehicleLength,
					maxVelocity, vehicleSeats, vehicleStandingRoom);
			newScenario.getTransitVehicles().addVehicleType(metroVehicleType);
					
			// Generate TransitLines and Schedules on NetworkRoutes --> Add to Transit Schedule
			int nTransitLines = initialMetroRoutes.size();
			for(int lineNr=1; lineNr<=nTransitLines; lineNr++) {
					
				// NetworkRoute
				NetworkRoute metroNetworkRoute = initialMetroRoutes.get(lineNr-1);
				MRoute mRoute = new MRoute(thisNewNetworkName+"_Route"+lineNr);
				mRoute.lifeTime = lifeTime;
				mRoute.departureSpacing = initialDepSpacing;
				mRoute.isInitialDepartureSpacing = false;
				mRoute.firstDeparture = tFirstDep;
				mRoute.lastDeparture = tLastDep;
				mRoute.setNetworkRoute(metroNetworkRoute);
				mNetwork.addNetworkRoute(mRoute);
				
				// Create an array of stops along new networkRoute on the FromNode of each of its individual links (and ToNode for final terminal)
				// The new network was constructed so that every node had a corresponding stop facility from the original zurich network on it.
				List<TransitRouteStop> stopArray = Metro_TransitScheduleImpl.createAndAddNetworkRouteStops(
						metroLinkAttributes, metroSchedule, metroNetwork, mRoute, defaultPtMode, stopTime, maxVehicleSpeed, blocksLane);
				if (stopArray == null) {
					Log.write("CAUTION: stopArray was too short (see code for size limits) --> Therefore deleting mRoute = " +mRoute.routeID + "and moving to next initial line");
					mNetwork.routeMap.remove(mRoute.routeID);
					continue;
				}
				mRoute.roundtripTravelTime = stopArray.get(stopArray.size()-1).getArrivalOffset();
				mRoute.vehiclesNr = (int) Math.ceil(mRoute.roundtripTravelTime/mRoute.departureSpacing);		// set vehicles initially so they are not zero for evo loops
				// Log.writeAndDisplay("stopArray.size()="+stopArray.size());
				
				// Build TransitRoute from stops and NetworkRoute --> and add departures
				String vehicleFileLocation = (mNetworkPath+"/Vehicles.xml");
				TransitRoute transitRoute = metroScheduleFactory.createTransitRoute(Id.create(thisNewNetworkName+"_Route"+lineNr, TransitRoute.class ), 
						metroNetworkRoute, stopArray, defaultPtMode);
				
				transitRoute = Metro_TransitScheduleImpl.addDeparturesAndVehiclesToTransitRoute(mRoute, newScenario, metroSchedule, 
						transitRoute, metroVehicleType, vehicleFileLocation); // Add departures to TransitRoute as a function of f=(DepSpacing, First/LastDeparture)
									
				// Build TransitLine from TrasitRoute
				TransitLine transitLine = metroScheduleFactory.createTransitLine(Id.create("TransitLine_Nr"+lineNr, TransitLine.class));
				transitLine.addRoute(transitRoute);
				
				// Add new line to schedule
				metroSchedule.addTransitLine(transitLine);
	
				mRoute.setTransitLine(transitLine);
				mRoute.setLinkList(NetworkRoute2LinkIdList(metroNetworkRoute));
				mRoute.setNodeList(NetworkRoute2NodeIdList(metroNetworkRoute, metroNetwork));
				mRoute.setRouteLength(metroNetwork);
				mRoute.setTotalDrivenDist(mRoute.routeLength * mRoute.nDepartures);		
			}	// end of TransitLine creator loop
	
			// Write TransitSchedule to corresponding file
			TransitScheduleWriter tsw = new TransitScheduleWriter(metroSchedule);
			tsw.writeFile(mNetworkPath+"/MetroSchedule.xml");
					
			String mergedNetworkFileName = "";
			if (useOdPairsForInitialRoutes==true) {
				mergedNetworkFileName = (mNetworkPath+"/OriginalNetwork_with_ODInitialRoutes.xml");
			}
			else {
				mergedNetworkFileName = (mNetworkPath+"/OriginalNetwork_with_RandomInitialRoutes.xml");
			}
			
			NetworkOperators.superimpose2separateNetwork(originalNetwork, null, separateRoutesNetwork, Sets.newHashSet("pt"), mergedNetworkFileName);
			Metro_TransitScheduleImpl.mergeAndWriteTransitSchedules(metroSchedule, originalTransitSchedule, (mNetworkPath+"/MergedSchedule.xml"));
			Metro_TransitScheduleImpl.mergeAndWriteVehicles(newScenario.getTransitVehicles(), originalScenario.getTransitVehicles(), (mNetworkPath+"/MergedVehicles.xml"));
			
			networkPopulation.modifiedNetworksInLastEvolution.add(thisNewNetworkName);	// do this so EVO/SIM loop knows to consider these networks for processing
			networkPopulation.addNetwork(mNetwork);
		}
		return networkPopulation;
	}


	public static void connectRailStopsInMetroNetworkToSurroundingNodes(Network metroNetwork,
			Map<String, CustomStop> railStops, double maxNewMetroLinkDistance, Set<String> transportModes,
			Map<Id<Link>, CustomMetroLinkAttributes> metroLinkAttributes, String transitScheduleFileName) throws IOException {
		Config conf = ConfigUtils.createConfig();
		conf.getModules().get("transit").addParam("transitScheduleFile",transitScheduleFileName);
		Scenario sc = ScenarioUtils.loadScenario(conf);
		TransitSchedule metroStopFacilities = sc.getTransitSchedule();
		Network newConnectionsNetwork = sc.getNetwork();
		
		for (CustomStop railStop : railStops.values()) {
			List<Id<Node>> alreadyConnectedNodes = new ArrayList<Id<Node>>();
			Node facilityNode = NetworkOperators.getNodeOfFacility(railStop.transitStopFacility, metroNetwork);
			if (facilityNode == null) {
				Log.write("CAUTION: facilityNode is null of facility="+railStop.transitStopFacility.getName());
				continue;
			}
			else {
				for (Link outLink : facilityNode.getOutLinks().values()) {
					alreadyConnectedNodes.add(outLink.getToNode().getId());
				}				
			}
			for (Node node : metroNetwork.getNodes().values()) {
				if (node.getId().equals(facilityNode.getId()) || alreadyConnectedNodes.contains(node.getId())) {
					continue;
				}
				if (GeomDistance.betweenNodes(facilityNode, node) < 500.0) {
					Boolean otherNodeHasFacility = false;
					TransitStopFacility otherNodeTSF = null;
					for (TransitStopFacility tsf : metroStopFacilities.getFacilities().values()) {
						if (node.getCoord().equals(tsf.getCoord())) {
							otherNodeTSF = tsf;
							otherNodeHasFacility = true;
						}
					}
					
					Link newLink = metroNetwork.getFactory().
							createLink(Id.createLinkId(facilityNode.getId().toString() + "_" + node.getId().toString()), facilityNode, node);
					if ( ! metroNetwork.getLinks().keySet().contains(newLink.getId())) {
						newLink.setAllowedModes(transportModes);
						metroNetwork.addLink(newLink);
						Node fromNode = newLink.getFromNode();
						Node toNode = newLink.getToNode();
						Node newFromNode = newConnectionsNetwork.getFactory().createNode(Id.createNodeId(fromNode.getId().toString()), fromNode.getCoord());
						if ( !newConnectionsNetwork.getNodes().containsKey(newFromNode.getId())) {
							newConnectionsNetwork.addNode(newFromNode);
						}
						Node newToNode = newConnectionsNetwork.getFactory().createNode(Id.createNodeId(toNode.getId().toString()), toNode.getCoord());
						if ( !newConnectionsNetwork.getNodes().containsKey(newToNode.getId())) {
							newConnectionsNetwork.addNode(newToNode);
						}
						newConnectionsNetwork.addLink(newConnectionsNetwork.getFactory().createLink(Id.createLinkId(newLink.getId().toString()), newFromNode, newToNode));

						CustomMetroLinkAttributes cmla = new CustomMetroLinkAttributes("newMetro");
						cmla.fromNodeStopFacility = railStop.transitStopFacility;
						if (otherNodeHasFacility) {
							cmla.toNodeStopFacility = otherNodeTSF;
						}
						metroLinkAttributes.put(newLink.getId(), cmla);
					}
					else {
						Log.write("CAUTION: Rail stop connecting link already exists: "+newLink.getId());
					}
					Link newLinkReverse = metroNetwork.getFactory().
							createLink(Id.createLinkId(node.getId().toString() + "_" + facilityNode.getId().toString()), node, facilityNode);
					if ( ! metroNetwork.getLinks().keySet().contains(newLinkReverse.getId())) {
						newLinkReverse.setAllowedModes(transportModes);
						metroNetwork.addLink(newLinkReverse);
						CustomMetroLinkAttributes cmla = new CustomMetroLinkAttributes("newMetro");
						cmla.toNodeStopFacility = railStop.transitStopFacility;
						if (otherNodeHasFacility) {
							cmla.fromNodeStopFacility = otherNodeTSF;
						}
						metroLinkAttributes.put(newLinkReverse.getId(), cmla);
					}
					else {
						Log.write("CAUTION: Rail stop connecting link already exists: "+newLinkReverse.getId());
					}
				}
			}
		}
		
		NetworkWriter nwConnect = new NetworkWriter(newConnectionsNetwork);
		nwConnect.write("zurich_1pm/Evolution/Population/BaseInfrastructure/rail2metroConnections.xml");
	}


	public static CBPII calculateCBAStats(String plansFolder, String outputFile, int populationFactor,
			Integer lastIteration, Integer iterationsToAverage) throws IOException {
		
		// CBP stats instantiation
		Double ptUsers = 0.0;
		Double carUsers = 0.0;
		Double otherUsers = 0.0;
		Double carTimeTotal = 0.0;
		Double carPersonDist = 0.0;
		Double ptTimeTotal = 0.0;
		Double ptPersonDist = 0.0;
		Double walkBikeTimeTotal = 0.0;
		Double ptDisutilityEquivalentTimeTotal = 0.0;

		
		// Average the events output over several iteration (generationsToAverage). For every generation add its performance divided by its single weight
		for (Integer thisIteration=lastIteration-iterationsToAverage+1; thisIteration<=lastIteration; thisIteration++) {

			String finalPlansFile = plansFolder+"/it."+thisIteration+"/"+thisIteration+".plans.xml.gz";
			Config newConfig = ConfigUtils.createConfig();
			newConfig.getModules().get("plans").addParam("inputPlansFile", finalPlansFile);
			Scenario newScenario = ScenarioUtils.loadScenario(newConfig);
			Population finalPlansPopulation = newScenario.getPopulation();

			for (Person person : finalPlansPopulation.getPersons().values()) {
				boolean isPtTraveler = false;
				boolean isCarTraveler = false;
				double personTravelTime = 0.0;
				Plan selectedPlan = person.getSelectedPlan();
				for (PlanElement e : selectedPlan.getPlanElements()) {
					if (e instanceof Leg) {
						Leg leg = (Leg) e;
						// do following two conditions to avoid unreasonably high (transit_)walk times!
						if (leg.getMode().equals("transit_walk") && leg.getTravelTime()>35*60.0) {
							leg.setTravelTime(35*60.0);
						}
						if ((leg.getMode().equals("walk") || leg.getMode().equals("bike")) && leg.getTravelTime()>60*60.0) {
							leg.setTravelTime(60*60.0);
						}
						//
						if (leg.getMode().contains("car")) {
							carTimeTotal += leg.getTravelTime();
							carPersonDist += leg.getRoute().getDistance();
							isCarTraveler = true;
						}
						else if (leg.getMode().contains("pt") || leg.getMode().contains("access_walk") ||
								leg.getMode().contains("transit_walk") || leg.getMode().contains("egress_walk")) {
							ptTimeTotal += leg.getTravelTime();
							ptPersonDist += leg.getRoute().getDistance();
							isPtTraveler = true;
							if (leg.getMode().equals("pt")) {
								ptDisutilityEquivalentTimeTotal += (14.43/14.43)*leg.getTravelTime();
							}
							else if (leg.getMode().equals("transit_walk")) {
								ptDisutilityEquivalentTimeTotal += (6.45/14.43)*leg.getTravelTime();
								ptDisutilityEquivalentTimeTotal += 3600* (2.45/14.43);	// for transfer add unit share of an hour according to cost ratio!
							}
							else if (leg.getMode().equals("access_walk") || leg.getMode().equals("egress_walk")) {
								ptDisutilityEquivalentTimeTotal += (24.13/14.43)*leg.getTravelTime();
							}
						}
						else if (leg.getMode().equals("walk") || leg.getMode().equals("bike")){
							walkBikeTimeTotal += leg.getTravelTime();
						}
						personTravelTime += leg.getTravelTime();	// totalPersonTravelTime
					}
				}
				// travel user type bins
				if (isCarTraveler && isPtTraveler) {
					ptUsers ++;
					carUsers ++;
				}
				else if (isCarTraveler) {
					carUsers ++;
				}
				else if (isPtTraveler) {
					ptUsers ++;
				}
				else {
					otherUsers ++;
				}
			}

		} // end of averaging processing loop
		
		// parameters have been summed up over entire loop --> have to be averaged now! 
		ptUsers /= iterationsToAverage;
		carUsers /= iterationsToAverage;
		otherUsers /= iterationsToAverage;
		carTimeTotal /= iterationsToAverage;
		carPersonDist /= iterationsToAverage;
		ptTimeTotal /= iterationsToAverage;
		ptPersonDist /= iterationsToAverage;
		walkBikeTimeTotal /= iterationsToAverage;
		ptDisutilityEquivalentTimeTotal /= iterationsToAverage;
		
		// calculate & save CBP stats
		CBPII cbp = new CBPII( populationFactor*ptUsers, populationFactor*carUsers, populationFactor*otherUsers,
				populationFactor*carTimeTotal,  populationFactor*carPersonDist,  populationFactor*ptTimeTotal,  populationFactor*ptPersonDist);
		cbp.customVariable1 = populationFactor*walkBikeTimeTotal;
		cbp.customVariable3 = populationFactor*ptDisutilityEquivalentTimeTotal;
		cbp.calculateAverages();
		XMLOps.writeToFile(cbp, outputFile);
		return cbp;
	}
	

	public static void CalculateAverageNetworkSpeed(TransitSchedule originalTransitSchedule, Network originalNetwork,
			String fileName) throws FileNotFoundException {
		double totalLength = 0.0;
		double totalTime = 0.0;
		for (TransitLine tl : originalTransitSchedule.getTransitLines().values()) {
			for (TransitRoute tr : tl.getRoutes().values()) {
				// may want to check from NetworkRoute>>LinkList which links are within linkRadius (or Zone110)
				// in order to calculate average speed in city only
				int nRouteDepartures = tr.getDepartures().size();
				double routeTravelTime = tr.getStops().get(tr.getStops().size()-1).getArrivalOffset(); 
				// if city: find last/first stop in city and go from there
				double cumulatedRouteLength = transitRoute2TotalLength(tr, originalNetwork);
				// if city: find last/first link in city and go from there
				totalLength += cumulatedRouteLength*nRouteDepartures;
				totalTime += routeTravelTime*nRouteDepartures;
			}
		}
		double averageSpeedOriginalPtZH = totalLength/totalTime;
		XMLOps.writeToFile(averageSpeedOriginalPtZH, fileName);
	}


	public static double transitRoute2TotalLength(TransitRoute tr, Network network) {
		double length = 0.0;
		List<Id<Link>> linkList = NetworkRoute2LinkIdList(tr.getRoute());
		for (Id<Link> linkId : linkList) {
			length += network.getLinks().get(linkId).getLength();
		}		
		return length;
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
		
// %%%%%%%%%%%%%%%%%%%%%%%%%%%  STABLE UP TO HERE  %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

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
				double railway2metroCatchmentArea,
				double metro2metroCatchmentArea, Map<String, CustomStop> railwayStations, Network originalNetwork, String fileName) throws IOException{
			
			
			// SECTION 1: Merge links with railwayStopFacility if their dominant facilities is in vicinity of existing railwayStop & set reference railwayStation (= closest railwayStation)
			Map<Id<Link>, CustomLinkAttributes> metroRailwayMergedLinks = Clone.customLinkMap(links_withinRadius); // just a copy so that we can play around and remove entries
			
			Iterator<Entry<Id<Link>, CustomLinkAttributes>> linkIter = metroRailwayMergedLinks.entrySet().iterator();
			while(linkIter.hasNext()) {
				Entry<Id<Link>, CustomLinkAttributes> linkEntry = linkIter.next();
				CustomLinkAttributes att = linkEntry.getValue();
				for (String railwayStation : railwayStations.keySet()) {
					double rail2linkDistance = GeomDistance.calculate(att.dominantStopFacility.getCoord(),
							railwayStations.get(railwayStation).originalMainTransitStopFacility.getCoord());
					if(rail2linkDistance < att.distance2nextRailwayStopFacility){
						// initial value = Double.MAX_VALUE; check if this value is less than max value and check if it is closer than previous railStop
						att.nextRailwayStopFacility = railwayStations.get(railwayStation).originalMainTransitStopFacility;
						att.distance2nextRailwayStopFacility = rail2linkDistance;
						// if it is even within close proximity of railwayStop (railway2metroCatchmentArea), replace domStopFacility by rail stop facility
						if (rail2linkDistance < railway2metroCatchmentArea) {
							att.dominantStopFacility = railwayStations.get(railwayStation).originalMainTransitStopFacility;	// Ref. stop facility is exactly this one
							railwayStations.get(railwayStation).used = true;
						}
					}
				}
			}
			
			// SECTION 2: Merge close links into one (which one does not matter)
			//Use this line if SECTION 1 is not activated:
			//Map<Id<Link>, CustomLinkAttributes> metroRailwayMergedLinks = Clone.customLinkMap(links_withinRadius); // just a copy so that we can play around and remove entries
			Map<Id<Link>, CustomLinkAttributes> metroRailwayMetroMergedLinks = new HashMap<Id<Link>, CustomLinkAttributes>();
			
			do {
				List<Id<Link>> toBeDeletedLinks = new ArrayList<Id<Link>>();
				Iterator<Id<Link>> intIter = metroRailwayMergedLinks.keySet().iterator();
				Id<Link> thisLink = intIter.next();
				toBeDeletedLinks.add(thisLink);

				metroRailwayMetroMergedLinks.put(thisLink, metroRailwayMergedLinks.get(thisLink));
				for (Id<Link> otherLink : metroRailwayMergedLinks.keySet()) {
					if(thisLink.equals(otherLink)) {
						continue;
					}
					// XXX this if loop is new!
					if (metroRailwayMergedLinks.get(thisLink).getDominantStopFacility() == null
							|| metroRailwayMergedLinks.get(otherLink).getDominantStopFacility() == null) {
						// will receive NullPointer if CoordThis/CoordOther cannot be found by .getDominantStopFacility() in next line
						Log.write("We have a dom. stop facility null pointer issue - ;-))");
						continue;
					}
					else {
//						Log.write("This one is ok...");
					}
					Coord CoordThis = metroRailwayMergedLinks.get(thisLink).getDominantStopFacility().getCoord();    //originalNetwork.getLinks().get(thisLink).getFromNode().getCoord();
					Coord CoordOther = metroRailwayMergedLinks.get(otherLink).getDominantStopFacility().getCoord();  //originalNetwork.getLinks().get(otherLink).getFromNode().getCoord();
					if(Math.abs(GeomDistance.calculate(CoordThis, CoordOther)) < metro2metroCatchmentArea) {
						CustomLinkAttributes thisLinkAtt = metroRailwayMergedLinks.get(thisLink);
						thisLinkAtt.totalTraffic += metroRailwayMergedLinks.get(otherLink).totalTraffic;
						metroRailwayMetroMergedLinks.put(thisLink, thisLinkAtt);
						toBeDeletedLinks.add(otherLink);
					}				
				}
				for (Id<Link> l : toBeDeletedLinks) {
					metroRailwayMergedLinks.remove(l);
				}
			}while(metroRailwayMergedLinks.size()>0);
			

			if (fileName != null) {
				createNetworkFromCustomLinks(metroRailwayMetroMergedLinks, originalNetwork, fileName);
			}
			
			return metroRailwayMetroMergedLinks;
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
			String eventsFile = "zurich_1pm/Zurich_1pm_SimulationOutputEnriched/ITERS/it." + iterationToRead + "/" + iterationToRead
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

			LinkLoop:
			for (Id<Link> selectedLinkID : selectedLinks.keySet()) {
			boolean facilityFound = false;
				for (TransitLine transitLine : transitSchedule.getTransitLines().values()) {
					for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
						for (TransitRouteStop transitRouteStop : transitRoute.getStops()) {
							if (transitRouteStop.getStopFacility().getLinkId().equals(selectedLinkID)) {
								facilityFound = true;
								String mode = transitRoute.getTransportMode();
								// System.out.println("Mode on detected transit stop facility is |"+mode+"|");
								if (mode.equals("rail")) {
									CustomLinkAttributes updatedAttributes = selectedLinks.get(selectedLinkID);
									updatedAttributes.setDominantMode(mode);
									updatedAttributes.setDominantStopFacility(transitRouteStop.getStopFacility());
									selectedLinks.put(selectedLinkID, updatedAttributes);
									// System.out.println("Added mode: "+mode);
									continue LinkLoop; // if mode is rail, we set the default to rail bc it is most dominant
														// and move to next link (--> this link is completed)
								} else if (mode.equals("tram")) {
									CustomLinkAttributes updatedAttributes = selectedLinks.get(selectedLinkID);
									updatedAttributes.setDominantMode(mode);
									updatedAttributes.setDominantStopFacility(transitRouteStop.getStopFacility());
									selectedLinks.put(selectedLinkID, updatedAttributes);
									// System.out.println("Added mode: "+mode);

								} else if (mode.equals("bus")) {
									if (selectedLinks.get(selectedLinkID).getDominantMode() == null
											|| selectedLinks.get(selectedLinkID).getDominantMode() == "funicular") {
										CustomLinkAttributes updatedAttributes = selectedLinks.get(selectedLinkID);
										updatedAttributes.setDominantMode(mode);
										updatedAttributes.setDominantStopFacility(transitRouteStop.getStopFacility());
										selectedLinks.put(selectedLinkID, updatedAttributes);
										// System.out.println("Added mode: "+mode);
									}
								} else if (mode.equals("funicular")) {
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
			Map<String, CustomStop> railStops, Map<String, CustomStop> innerCityMetroStops, Map<Id<Link>,CustomMetroLinkAttributes> metroLinkAttributes,
			double maxNewMetroLinkDistance,  Network mergerNetwork, String transitScheduleFileName, String fileName)  	throws IOException {
		
			Config config = ConfigUtils.createConfig();
			Scenario scenario = ScenarioUtils.createScenario(config);
			Network newNetwork = scenario.getNetwork();
			NetworkFactory networkFactory = newNetwork.getFactory();

			// - Create and add to network a new node for each domFacility at exactly the Coordinates of domFacility --> Naming: "zhLinkRef"+LinkId"
						// - Add facility to facilityStopAttributesMap (make a new object so that rail remains unchanged)
						// - Check by superName if facility is featured in railwayStops -> Yes: Process facility (superName) in railStopAttributes - Set mode/NODE/addedToSchedule
						// - Make new clone facility with Id (removeBlanks! add "_metro") and add to metroTransitSchedule (add _metro also in facilityName)
						// - Interconnect nodes by links (all possible connections with max length constraint).
						// - in metroLinkAttributes add new links and store which link has which facilities etc.
						// TODO Is it a problem that no refLink is assigned to new stopFacility? StopAdding has trouble without refLinks (SwissRaptor error!)
						
						TransitSchedule newTransitSchedule = scenario.getTransitSchedule();
						TransitScheduleFactory newTSF = newTransitSchedule.getFactory();
						
						Node newNode = null;
						Link newLink = null;
						Map<Id<Node>, Id<Link>> metroNode2originalLinkRefMap = new HashMap<Id<Node>, Id<Link>>(customLinkMap.size());
						for (Entry<Id<Link>, CustomLinkAttributes> customLink : customLinkMap.entrySet()) {
							Id<Link> linkID = customLink.getKey();
							CustomLinkAttributes cla = customLink.getValue();
							TransitStopFacility dominantFacility = cla.dominantStopFacility;
							if (dominantFacility == null) {
								Log.write("CAUTION: NoDomFacility for this selected Link!!");
								continue;	// XXX this line is new
							}
							// - Create and add to network a new node for each domFacility at exactly the Coordinates of domFacility --> Naming: "zhLinkRef"+LinkId"
								// THIS AND THE FOLLOWING ELSE LOOP HAVE BEEN DELETED: if (cla.getDominantStopFacility() != null) {
							newNode = networkFactory.createNode(
									Id.createNodeId("zhStopLinkRef" + NetworkEvolutionImpl.removeString(linkID.toString(),"_")), dominantFacility.getCoord());
							metroNode2originalLinkRefMap.put(newNode.getId(), linkID);
							newNetwork.addNode(newNode);
							// - Make new facility with Id (removeBlanks! add "_metro") and add to transitSchedule (add _metro also in facilityName)
							String originalStopFacilityId = dominantFacility.getId().toString();
							String originalStopFacilityName = dominantFacility.getName().toString();
							String newStopSuperName = cutString(originalStopFacilityId, ".");
							String newMetroStopFacilityId = newStopSuperName+"_metro"; // stopFacilityId = superName.refLinkId --> Want only superName
							String newMetroStopFacilityName = removeSpecialChar(originalStopFacilityName)+"_metro"; // don't want blanks in stop name
							TransitStopFacility metroCloneFacility = newTSF.createTransitStopFacility(Id.create(newMetroStopFacilityId, TransitStopFacility.class), 
									dominantFacility.getCoord(), dominantFacility.getIsBlockingLane());
							metroCloneFacility.setName(newMetroStopFacilityName);
							// Add new MetroFacility to schedule and this current (innerCity) customStopMap
							newTransitSchedule.addStopFacility(metroCloneFacility);
							innerCityMetroStops.put(newStopSuperName, new CustomStop(metroCloneFacility, newNode.getId(), "newMetro", true));
							// - If facility is featured in railwayFacilities (check by superName): Process facility (superName) in railCustomStopMap - Set mode/NODE/addedToSchedule
							for (String railStopSuperName : railStops.keySet()) {
								if (newStopSuperName.equals(railStopSuperName)) {
									railStops.get(railStopSuperName).mode = "railway2metro";
									railStops.get(railStopSuperName).addedToNewSchedule = true;
									railStops.get(railStopSuperName).newNetworkNode = newNode.getId();
									railStops.get(railStopSuperName).transitStopFacility = metroCloneFacility;
								}
							}
						}
						TransitScheduleWriter tsw = new TransitScheduleWriter(newTransitSchedule);
						tsw.writeFile(transitScheduleFileName);	
			
						// At this point:
							// - A node has been created for every selectedMetroLink
							// - A new metro facility has been created on top of that node --> New stopFacility has been marked in innerCityMetroStops
							// - railStops is marked that has been assigned and is linked with railStops.transitStopFacility = newMetroStopFacility
			
			// Create links in network --> for every node:
			for (Node thisNode : newNetwork.getNodes().values()) {
				for (Node otherNode : newNetwork.getNodes().values()) {
					double thisMaxMetroLinkDistance = maxNewMetroLinkDistance;
					// increase maxLinkDistance if two hotspot nodes are connected i.e. nodes with traffic above a threshold, here 15.0
					if (Math.min(customLinkMap.get(metroNode2originalLinkRefMap.get(thisNode.getId())).getTotalTraffic(),
							customLinkMap.get(metroNode2originalLinkRefMap.get(otherNode.getId())).getTotalTraffic()) > 15.0) {
						thisMaxMetroLinkDistance = 1.3*maxNewMetroLinkDistance;
						//Log.write("traffic.txt", "Found a very frequent link between coords: "+thisNode.getCoord()+" / "+otherNode.getCoord());
					}
					if (thisNode.equals(otherNode)) {
						continue;
					}
					// add NEW links (with appropriate naming method) to all nodes within a specific radius
					else if (GeomDistance.betweenNodes(thisNode, otherNode) < thisMaxMetroLinkDistance) {
						newLink = networkFactory.createLink(
								Id.createLinkId(thisNode.getId().toString() + "_" + otherNode.getId().toString()), thisNode,
								otherNode);
						newNetwork.addLink(newLink);
					}
					// add NEW links if refLink of other facility node was on a next link to the link of this facility (an outLink of toNode of this node's refLink)
					else if (mergerNetwork.getLinks().get(metroNode2originalLinkRefMap.get(thisNode.getId())).getToNode()
							.getOutLinks().containsKey(metroNode2originalLinkRefMap.get(otherNode.getId()))) {
						newLink = networkFactory.createLink(
								Id.createLinkId(thisNode.getId().toString() + "_" + otherNode.getId().toString()), thisNode,
								otherNode);
						newNetwork.addLink(newLink);
					}
				}

			}
			
			for (Entry<Id<Link>, ? extends Link> newLinkEntry : newNetwork.getLinks().entrySet()) {
				TransitStopFacility fromNodeFacility = Metro_TransitScheduleImpl.node2Facility(newLinkEntry.getValue().getFromNode(), innerCityMetroStops);
				TransitStopFacility toNodeFacility = Metro_TransitScheduleImpl.node2Facility(newLinkEntry.getValue().getToNode(), innerCityMetroStops);
				CustomMetroLinkAttributes cmla = new CustomMetroLinkAttributes("newMetro");
				cmla.fromNodeStopFacility = fromNodeFacility;
				cmla.toNodeStopFacility = toNodeFacility;
				metroLinkAttributes.put(newLinkEntry.getKey(), cmla);
			}
			
			
			if (fileName != null) {
				NetworkWriter networkWriter = new NetworkWriter(newNetwork);
				networkWriter.write(fileName);
			}
			
			return newNetwork;
		}
		
		public static void addManualInputs(Network metroNetwork, Map<Id<Link>, CustomMetroLinkAttributes> metroLinkAttributes, 
				Map<String, CustomStop> allMetroStops, String transitScheduleFileName) throws IOException {

			
		// NEW NODES: Add to metroNetwork
			Map<String,Coord> manualNodes = new HashMap<String,Coord>();
			// manual inputs here:
			manualNodes.put("xWaldgarten", new Coord(2684357.0,1250895.0));				// *newStop		:has a new stop to it
			manualNodes.put("xSchwamendingerplatz", new Coord(2685683.0,1251026.0));	// *newStop
			manualNodes.put("xProbstei", new Coord(2686511.0,1250623.0));				// *newStop
			manualNodes.put("xMeiershofstrasse", new Coord(2688893.0,1250258.0));		// *newStop
			
			for (Entry<String,Coord> manualNodeEntry : manualNodes.entrySet()) {
				Id<Node> manualNodeId = Id.createNodeId(manualNodeEntry.getKey());
				Node manualNode = metroNetwork.getFactory().createNode(manualNodeId, manualNodeEntry.getValue());
				metroNetwork.addNode(manualNode);
//				Log.write("Node added: "+manualNode.getId().toString());
			}

		// NEW LINKS: Add to metroNetwork & metroLinkAttributes
			List<Link> newLinks = new ArrayList<Link>();
			List<List<Coord>> linkNodesLocationPairs = new ArrayList<List<Coord>>();
			// manual inputs here:
			linkNodesLocationPairs.add(Arrays.asList(new Coord(2689950.0, 1253588.0), new Coord(2690423.0, 1254697.0) ));	// Dietlikon - Baltenswil
			linkNodesLocationPairs.add(Arrays.asList(new Coord(2677698.0, 1244821.0), new Coord(2681679.0, 1244230.0) ));	// Uetliberg Metro Tunnel
			linkNodesLocationPairs.add(Arrays.asList(new Coord(2683189.0, 1251648.0), new Coord(2684357.0, 1250895.0) ));	// Oerlikon  - Waldgarten
			linkNodesLocationPairs.add(Arrays.asList(new Coord(2683301.0, 1250225.0), new Coord(2684357.0, 1250895.0) ));	// Milchbuck - Waldgarten
			linkNodesLocationPairs.add(Arrays.asList(new Coord(2684357.0, 1250895.0), new Coord(2685683.0, 1251026.0) ));	// Waldgarten - Schwamendingerplatz
			linkNodesLocationPairs.add(Arrays.asList(new Coord(2685683.0, 1251026.0), new Coord(2687270.0, 1251891.0) ));	// Schwamendingerplatz - Wallisellen
			linkNodesLocationPairs.add(Arrays.asList(new Coord(2687376.0, 1250232.0), new Coord(2687270.0, 1251891.0) ));	// Stettbach - Wallisellen
			linkNodesLocationPairs.add(Arrays.asList(new Coord(2685683.0, 1251026.0), new Coord(2686511.0, 1250623.0) ));	// Schwamendingerplatz - Probstei
			linkNodesLocationPairs.add(Arrays.asList(new Coord(2687376.0, 1250232.0), new Coord(2686511.0, 1250623.0) ));	// Stettbach - Probstei
			linkNodesLocationPairs.add(Arrays.asList(new Coord(2687376.0, 1250232.0), new Coord(2688893.0, 1250258.0) ));	// Stettbach - Meiershofstrasse
			linkNodesLocationPairs.add(Arrays.asList(new Coord(2688893.0, 1250258.0), new Coord(2689437.0, 1250588.0) ));	// Meiershofstrasse - Dübendorf

			for (List<Coord> locPair : linkNodesLocationPairs) {
				Link manualLink = createLinkFromLocationPair(locPair, metroNetwork);
				Link manualLinkReverse = (ReverseLinkEntire(manualLink, metroNetwork));
				manualLink.setAllowedModes(Sets.newHashSet("pt"));
				manualLinkReverse.setAllowedModes(Sets.newHashSet("pt"));
				if ( ! metroNetwork.getLinks().containsKey(manualLink.getId()) && ! metroNetwork.getLinks().containsKey(manualLinkReverse.getId())) {
					metroNetwork.addLink(manualLink);
					metroNetwork.addLink(manualLinkReverse);
					newLinks.add(manualLink);
					newLinks.add(manualLinkReverse);
//					Log.write("Link added: "+manualLink.getId().toString());
//					Log.write("LinkR added: "+manualLinkReverse.getId().toString());
				}
			}
			
		// NEW STOPFACILITIES: Add to schedule & allMetroStops
			Map<String,Coord> manualStops = new HashMap<String,Coord>();
			// manual inputs here:
			manualStops.put("xWaldgartenMetro", new Coord(2684357.0,1250895.0));
			manualStops.put("xSchwamendingerplatzMetro", new Coord(2685683.0,1251026.0));	// *newStop
			manualStops.put("xProbsteiMetro", new Coord(2686511.0,1250623.0));				// *newStop
			manualStops.put("xMeiershofstrasseMetro", new Coord(2688893.0,1250258.0));		// *newStop
						
			Config conf = ConfigUtils.createConfig();
			conf.getModules().get("transit").addParam("transitScheduleFile",transitScheduleFileName);
			Scenario sc = ScenarioUtils.loadScenario(conf);
			TransitSchedule metroStopFacilities = sc.getTransitSchedule();
			TransitScheduleFactory tsf = metroStopFacilities.getFactory();
			for (Entry<String, Coord> stopEntry : manualStops.entrySet()) {
				TransitStopFacility stopFacility = tsf.createTransitStopFacility(Id.create(stopEntry.getKey(), TransitStopFacility.class), stopEntry.getValue(), false);
				metroStopFacilities.addStopFacility(stopFacility);
//				Log.write("StopFacility added: "+stopFacility.getId().toString());
				// make sure to have created a metroNode above with the same name but without the "metro" suffix
				allMetroStops.put(stopEntry.getKey(), new CustomStop(stopFacility, Id.createNodeId(removeString(stopEntry.getKey(), "Metro")), "newMetro", true));
			}
			TransitScheduleWriter tsw = new TransitScheduleWriter(metroStopFacilities);
			tsw.writeFile(transitScheduleFileName);

			for (Link newLink : newLinks) {
				CustomMetroLinkAttributes cmla = new CustomMetroLinkAttributes("newMetro", null);
				Node toNode = newLink.getToNode();
				Node fromNode = newLink.getFromNode();
				for (TransitStopFacility thisTSF : metroStopFacilities.getFacilities().values()) {
					if (toNode.getCoord().equals(thisTSF.getCoord())) {
						cmla.toNodeStopFacility = thisTSF;
//						Log.write("ToNodeStopFacility found and marked: "+thisTSF.getId().toString()+ "  (Link="+newLink.getId().toString()+")");
					}
					if (fromNode.getCoord().equals(thisTSF.getCoord())) {
						cmla.fromNodeStopFacility = thisTSF;
//						Log.write("FromNodeStopFacility found and marked: "+thisTSF.getId().toString()+ "  (Link="+newLink.getId().toString()+")");
					}
				}
				metroLinkAttributes.put(newLink.getId(), cmla);
			}
			
			
		}
		

		public static Link createLinkFromLocationPair(List<Coord> locPair, Network metroNetwork) throws IOException {
			Node closestNode1 = findClosestNode(locPair.get(0), metroNetwork);
			Node closestNode2 = findClosestNode(locPair.get(1), metroNetwork);
			if (closestNode1 == null || closestNode2 == null) {
				Log.write("CAUTION: no closest node found for manual linkNodeLocation. Aborting");
				System.exit(0);
			}
			if (closestNode1.equals(closestNode2)) {
				Log.write("CAUTION: closest nodes are identical. Check input coord for locationPairs for one link! ... Aborting ...");
				System.exit(0);	
			}
			Id<Link> linkId = Id.createLinkId(removeString(closestNode1.getId().toString(), "_") + "_" + removeString(closestNode2.getId().toString(), "_"));
			return metroNetwork.getFactory().createLink(linkId, closestNode1, closestNode2);
		}

		public static Node findClosestNode(Coord coord, Network metroNetwork) {
			Node closestNode = null;
			Double closestNodeDistance = Double.MAX_VALUE;
			Double thisNodeDistance = 0.0;
			for (Node node : metroNetwork.getNodes().values()) {
				thisNodeDistance = GeomDistance.calculate(coord, node.getCoord());
				if (thisNodeDistance < closestNodeDistance) {
					closestNode = node;
					closestNodeDistance = thisNodeDistance;
				}
			}
			return closestNode;
		}


		public static Network createMetroNetworkFromRailwayNetwork( Map<String, CustomStop> railStops,
				Map<String, CustomStop> outerCityMetroStops, Map<Id<Link>, CustomMetroLinkAttributes> metroLinkAttributes, Network originalNetwork, Network innerCityMetroNetwork,
				String transitScheduleFileName, String fileName) throws IOException {
			
			// Extract copy of railway network from Zürich Scenario and convert (rename and connect to facilities) it to a metroNetwork:
				// - make new facilities (add to transitSchedule)
				// - make new nodes (add to network) and connecting links
				// - convert existing nodes/links to a metroVersion (with naming)
			
				// Go through customRailStops (=all Zurich railway stopFacilities):
					// - For all Facilities already added to innerCitySchedule: Create and add IDENTICAL node to isolated railwayNetwork --> CONSISTENCY WHEN MERGING later on!!
					// - For all Facilities not added to transitSchedule yet (marked in customRailStops), create & add node & facility to new transitSchedule, mark in customRailStops
			// Run along TransitRoutes and create new metro links and nodes (also make correctly named new facility node connectors) 
			// Update metroLinkAttributes along the way with the originalLinkIds and the new stopFacilities!
			
			Config conf = ConfigUtils.createConfig();
			conf.getModules().get("transit").addParam("transitScheduleFile",transitScheduleFileName);
			Scenario sc = ScenarioUtils.loadScenario(conf);
			TransitSchedule metroStopFacilities = sc.getTransitSchedule();
			TransitScheduleFactory tsf = metroStopFacilities.getFactory();
			Network outerCityMetroNetwork = sc.getNetwork();
			NetworkFactory networkFactory = outerCityMetroNetwork.getFactory();
			
			
			Node newNode;
			for (CustomStop railStop : railStops.values()) {
				if (railStop.addedToNewSchedule == true) {
					newNode = innerCityMetroNetwork.getNodes().get(railStop.newNetworkNode);		// build identical node as in innerCityMetroNetwork (Consistency)
					outerCityMetroNetwork.addNode(newNode);
				}
				else {
					railStop.addedToNewSchedule = true;
					Id<Node> newNodeId = Id.createNodeId("zhStopLinkRef" + removeString(railStop.originalMainTransitStopFacility.getLinkId().toString(),"_"));
					newNode = networkFactory.createNode(newNodeId, railStop.originalMainTransitStopFacility.getCoord());
					if (outerCityMetroNetwork.getNodes().containsKey(newNodeId) == false) {
						outerCityMetroNetwork.addNode(newNode);
					}
					String originalStopFacilityId = railStop.originalMainTransitStopFacility.getId().toString();
					String originalStopFacilityName = railStop.originalMainTransitStopFacility.getName().toString();
					String newStopSuperName = cutString(originalStopFacilityId, ".");
					String newMetroStopFacilityId = newStopSuperName+"_metro"; // stopFacilityId = superName.refLinkId --> Want only superName
					String newMetroStopFacilityName = removeSpecialChar(originalStopFacilityName)+"_metro"; // don't want blanks in stop name
					TransitStopFacility metroCloneFacility = tsf.createTransitStopFacility(Id.create(newMetroStopFacilityId, TransitStopFacility.class), 
							railStop.originalMainTransitStopFacility.getCoord(), railStop.originalMainTransitStopFacility.getIsBlockingLane());
					metroCloneFacility.setName(newMetroStopFacilityName);
					metroStopFacilities.addStopFacility(metroCloneFacility);
					outerCityMetroStops.put(newStopSuperName, new CustomStop(metroCloneFacility, newNode.getId(), "newMetro", true));
					railStop.mode = "railway2metro";
					railStop.newNetworkNode = newNode.getId();
					railStop.addedToNewSchedule = true;
					railStop.transitStopFacility = metroCloneFacility;
				}
			}
			
			// Manual inputs, Nodes|Links|StopFacilities
			
			
			TransitScheduleWriter tsw = new TransitScheduleWriter(metroStopFacilities);
			tsw.writeFile(transitScheduleFileName);
			
			
			Config confOriginal = ConfigUtils.createConfig();
			confOriginal.getModules().get("transit").addParam("transitScheduleFile","zurich_1pm/zurich_transit_schedule.xml.gz");
			Scenario scenOriginal = ScenarioUtils.loadScenario(confOriginal);
			TransitSchedule tsOriginal = scenOriginal.getTransitSchedule();
			
			for (TransitLine tl : tsOriginal.getTransitLines().values()) {
				for (TransitRoute tr : tl.getRoutes().values()) {
					if (tr.getTransportMode().toString().equals("rail")) {
						List<Id<Link>> routeLinks = NetworkRoute2LinkIdList(tr.getRoute());
						TransitStopFacility currentStop = null;
						TransitStopFacility lastStop = tr.getStops().get(0).getStopFacility();
						for (TransitRouteStop RouteStop : tr.getStops().subList(1, tr.getStops().size())) {
							currentStop = RouteStop.getStopFacility();
							// call it superName bc several stops may exist with same superName and are then further specified by refLink
							// we want superName here to not repeat same facility location (for metro we build only one main facility)
							String currentStopSuperName = cutString(currentStop.getId().toString(), ".");
							String lastStopSuperName = cutString(lastStop.getId().toString(), ".");
							if (railStops.containsKey(lastStopSuperName) == false || railStops.containsKey(currentStopSuperName) == false) {
								// this might be the case if a Zurich stop is beyond specified radius bounds (note, these are ALL railwayStops in the whole of Zurich!)
								lastStop = currentStop;
								continue;
							}
							else {
								// CAUTION: If link can't be added to outerCityNetwork then set .addLink expressions in if condition that is not part of network already
								// if the stop stops are not yet connected by a metro route build a new metro route between them
								if (railStops.get(lastStopSuperName).nextOriginalTransitStopNames.contains(currentStopSuperName) == false) {
									railStops.get(lastStopSuperName).nextOriginalTransitStopNames.add(currentStopSuperName);
									railStops.get(currentStopSuperName).nextOriginalTransitStopNames.add(lastStopSuperName);
									// now find linkList of route between those two stops excluding the direct original links the two facility were on
									// CAUTION: If you get an error here it may be that either routeLinks is not long enough or that both stopFacilities have same refLink!
									List<Id<Link>> routeBetweenStops = routeLinks.subList(routeLinks.indexOf(lastStop.getLinkId())+1, routeLinks.indexOf(currentStop.getLinkId()));
									
									// start making connecting link from facilityNode to last node of routeBetween (= toNode of facilityRefLink as used here for flexibility in case routeBetween.size()=0)
									Node firstFromNode = originalNetwork.getLinks().get(lastStop.getLinkId()).getToNode();
									Node firstFromNodeMetro = networkFactory.createNode(
											Id.createNodeId("zhNodeRef"+removeString(firstFromNode.getId().toString(),"_")), firstFromNode.getCoord());
									if(outerCityMetroNetwork.getNodes().containsKey(firstFromNodeMetro.getId())==false) {
										outerCityMetroNetwork.addNode(firstFromNodeMetro);
									}
									// when we have connecting link to new node at lastStop, make connection links!
									Node lastStopNetworkNode = outerCityMetroNetwork.getNodes().get(railStops.get(lastStopSuperName).newNetworkNode);
									Id<Link> newLinkIdLastStop = Id.createLinkId(lastStopNetworkNode.getId().toString()+"RC_"+ firstFromNodeMetro.getId().toString());							
									Id<Link> newLinkIdLastStopReverse = NetworkEvolutionImpl.ReverseLink(newLinkIdLastStop);
									if (outerCityMetroNetwork.getLinks().containsKey(newLinkIdLastStop)==false) {
										outerCityMetroNetwork.addLink(networkFactory.createLink(newLinkIdLastStop, lastStopNetworkNode, firstFromNodeMetro));
									}
									if (outerCityMetroNetwork.getLinks().containsKey(newLinkIdLastStopReverse)==false) {
										outerCityMetroNetwork.addLink(networkFactory.createLink(newLinkIdLastStopReverse,firstFromNodeMetro, lastStopNetworkNode));
									}
									CustomMetroLinkAttributes cmlaLastStopFromNodeFacility = new CustomMetroLinkAttributes("rail2newMetro", null);
									cmlaLastStopFromNodeFacility.fromNodeStopFacility = railStops.get(lastStopSuperName).transitStopFacility;
									metroLinkAttributes.put(newLinkIdLastStop, cmlaLastStopFromNodeFacility);
									CustomMetroLinkAttributes cmlaLastStopToNodeFacility = new CustomMetroLinkAttributes("rail2newMetro", null);		
									cmlaLastStopToNodeFacility.toNodeStopFacility = railStops.get(lastStopSuperName).transitStopFacility;
									metroLinkAttributes.put(newLinkIdLastStopReverse, cmlaLastStopToNodeFacility);
									Node thisFromNodeMetro = firstFromNodeMetro;
									Node lastToNodeMetro;
									if (routeBetweenStops.size() > 0) {
										Node thisToNode = null;
										Node thisToNodeMetro = null;
										// fill in links and nodes between stops
										for (Id<Link> linkId : routeBetweenStops) {
											Link thisLink = originalNetwork.getLinks().get(linkId);
											thisToNode = thisLink.getToNode();
											thisToNodeMetro =  networkFactory.createNode(
													Id.createNodeId("zhNodeRef"+removeString(thisToNode.getId().toString(),"_")), thisToNode.getCoord());
											
											if(outerCityMetroNetwork.getNodes().containsKey(thisToNodeMetro.getId())==false) {
												outerCityMetroNetwork.addNode(thisToNodeMetro);
											}
											Id<Link> thisLinkIdMetro = Id.createLinkId(thisFromNodeMetro.getId().toString()+"_"+thisToNodeMetro.getId().toString());
											Id<Link> thisLinkReverseIdMetro = Id.createLinkId(thisToNodeMetro.getId().toString()+"_"+thisFromNodeMetro.getId().toString());
											Link thisLinkMetro = networkFactory.createLink(thisLinkIdMetro, thisFromNodeMetro, thisToNodeMetro);
											Link thisLinkMetroReverse = networkFactory.createLink(thisLinkReverseIdMetro, thisToNodeMetro, thisFromNodeMetro);
											if (outerCityMetroNetwork.getLinks().containsKey(thisLinkIdMetro)==false) {
												outerCityMetroNetwork.addLink(thisLinkMetro);										
											}
											if (outerCityMetroNetwork.getLinks().containsKey(thisLinkReverseIdMetro)==false) {
												outerCityMetroNetwork.addLink(thisLinkMetroReverse);										
											}									
											metroLinkAttributes.put(thisLinkIdMetro, new CustomMetroLinkAttributes("rail2newMetro", linkId));
											metroLinkAttributes.put(thisLinkReverseIdMetro, new CustomMetroLinkAttributes("rail2newMetro", linkId));
											thisFromNodeMetro = thisToNodeMetro;
										}
										lastToNodeMetro = thisToNodeMetro;
									}
									else {
										lastToNodeMetro = originalNetwork.getLinks().get(currentStop.getLinkId()).getFromNode();
										if(outerCityMetroNetwork.getNodes().containsKey(lastToNodeMetro.getId())==false) {
											outerCityMetroNetwork.addNode(lastToNodeMetro);
										}
									}
									// now we have reached currentStop and can make connecting links there and add to network
									Node currentStopNetworkNode = outerCityMetroNetwork.getNodes().get(railStops.get(currentStopSuperName).newNetworkNode);
									Id<Link> newLinkIdCurrentStop = Id.createLinkId(currentStopNetworkNode.getId().toString()+"RC_"+ lastToNodeMetro.getId().toString());
									Id<Link> newLinkIdCurrentStopReverse = NetworkEvolutionImpl.ReverseLink(newLinkIdCurrentStop);
									if (outerCityMetroNetwork.getLinks().containsKey(newLinkIdCurrentStop)==false) {
										outerCityMetroNetwork.addLink(networkFactory.createLink(newLinkIdCurrentStop, currentStopNetworkNode, lastToNodeMetro));										
									}
									if (outerCityMetroNetwork.getLinks().containsKey(newLinkIdCurrentStopReverse)==false) {
										outerCityMetroNetwork.addLink(networkFactory.createLink(newLinkIdCurrentStopReverse, lastToNodeMetro, currentStopNetworkNode));										
									}								
									CustomMetroLinkAttributes cmlaCurrentStopFromNodeFacility = new CustomMetroLinkAttributes("rail2newMetro", null);
									cmlaCurrentStopFromNodeFacility.fromNodeStopFacility = railStops.get(currentStopSuperName).transitStopFacility;
									metroLinkAttributes.put(newLinkIdCurrentStop, cmlaCurrentStopFromNodeFacility);
									CustomMetroLinkAttributes cmlaCurrentStopToNodeFacility = new CustomMetroLinkAttributes("rail2newMetro", null);		
									cmlaCurrentStopToNodeFacility.toNodeStopFacility = railStops.get(currentStopSuperName).transitStopFacility;
									metroLinkAttributes.put(newLinkIdCurrentStopReverse, cmlaCurrentStopToNodeFacility);
								}
							}
							lastStop = currentStop;		// IMPORTANT :)
						}
					}
				}
			}
			
			// store railStops with its newly assigned tsf's here.
			XMLOps.writeToFile(railStops, "zurich_1pm/Evolution/Population/BaseInfrastructure/railStopAttributes.xml");
			
			NetworkWriter nw = new NetworkWriter(outerCityMetroNetwork);
			nw.write(fileName);
			return outerCityMetroNetwork;
		}
		
		
		public static List<TransitStopFacility> findFacilitiesWithinBounds(String transitScheduleFileName, 
				Coord networkCenterCoord, double minRadiusFromCenter, double maxRadiusFromCenter, String terminalCandidateNetworkFileName) {
 			Config conf = ConfigUtils.createConfig();
			conf.getModules().get("transit").addParam("transitScheduleFile",transitScheduleFileName);
			Scenario sc = ScenarioUtils.loadScenario(conf);
			TransitSchedule ts = sc.getTransitSchedule();
			Network nw = sc.getNetwork();
					
			List<TransitStopFacility> terminalCandidateFacilities = new ArrayList<TransitStopFacility>();
			double distanceFromCenter = 0.0;
			for (TransitStopFacility tsf : ts.getFacilities().values()) {
				distanceFromCenter = GeomDistance.calculate(tsf.getCoord(),	networkCenterCoord);
				if (distanceFromCenter > minRadiusFromCenter && distanceFromCenter < maxRadiusFromCenter) {
					terminalCandidateFacilities.add(tsf);
					nw.addNode(nw.getFactory().createNode(Id.createNodeId(tsf.getId().toString()), tsf.getCoord()));
				}
			}
			
			if (terminalCandidateNetworkFileName != null) {
				NetworkWriter networkWriter = new NetworkWriter(nw);
				networkWriter.write(terminalCandidateNetworkFileName);
			}
 			return terminalCandidateFacilities;
		}
		
		
		

		public static Map<Id<Link>, CustomLinkAttributes> copyCustomMap(Map<Id<Link>, CustomLinkAttributes> customMap) {
			Map<Id<Link>, CustomLinkAttributes> customMapCopy = new HashMap<Id<Link>, CustomLinkAttributes>();
			for (Entry<Id<Link>, CustomLinkAttributes> entry : customMap.entrySet()) {
				customMapCopy.put(entry.getKey(), entry.getValue());
			}
			return customMapCopy;
		}

		// REMEMBER: New nodes are named "MetroNodeLinkRef_"+linkID.toString()
		public static ArrayList<NetworkRoute> createInitialRoutesRandom(Network newMetroNetwork, String shortestPathStrategy,
				List<TransitStopFacility> terminalFacilities, Map<String, CustomStop> metroStops, int nRoutes, Coord zhCenterCoord,
				double metroCityRadius, boolean varyInitRouteSize, 
				double minTerminalDistance0, double minInitialTerminalRadiusFromCenter0, double maxInitialTerminalRadiusFromCenter0) throws IOException {

			double minInitialTerminalRadiusFromCenter;
			double maxInitialTerminalRadiusFromCenter;
			double minTerminalDistance;
			ArrayList<NetworkRoute> networkRouteArray = new ArrayList<NetworkRoute>();

			// make nRoutes new routes
			Id<Node> terminalNode1 = null;
			Id<Node> terminalNode2 = null;
			TransitStopFacility terminalFacility1 = null;
			TransitStopFacility terminalFacility2 = null;
			
			OuterNetworkRouteLoop:
			while (networkRouteArray.size() < nRoutes) {
				if ((new Random()).nextDouble()<0.5 || !varyInitRouteSize) {
					minInitialTerminalRadiusFromCenter = minInitialTerminalRadiusFromCenter0;
					maxInitialTerminalRadiusFromCenter = maxInitialTerminalRadiusFromCenter0;
					minTerminalDistance = minTerminalDistance0;
				}
				else {
					minInitialTerminalRadiusFromCenter = 0.5*minInitialTerminalRadiusFromCenter0;
					maxInitialTerminalRadiusFromCenter = 0.5*maxInitialTerminalRadiusFromCenter0;
					minTerminalDistance = 0.5*metroCityRadius;
				}
				// choose two random terminals
				do {
					Random r1 = new Random();
					terminalFacility1 = terminalFacilities.get(r1.nextInt(terminalFacilities.size()));
					terminalNode1 = NetworkEvolutionImpl.facility2nodeId(newMetroNetwork, terminalFacility1);	// maybe do this with nodeLocation instead of name
				} while (terminalNode1 == null || GeomDistance.calculate(newMetroNetwork.getNodes().get(terminalNode1).getCoord(), zhCenterCoord) < minInitialTerminalRadiusFromCenter 
						|| GeomDistance.calculate(newMetroNetwork.getNodes().get(terminalNode1).getCoord(), zhCenterCoord) > maxInitialTerminalRadiusFromCenter);
				
				int safetyCounter = 0;
				int iterLimit = 10000;
				do {
					do {
						Random r2 = new Random();
						terminalFacility2 = terminalFacilities.get(r2.nextInt(terminalFacilities.size()));
						terminalNode2 = NetworkEvolutionImpl.facility2nodeId(newMetroNetwork, terminalFacility2);	// maybe do this with nodeLocation instead of name
					} while (terminalNode2 == null|| GeomDistance.calculate(newMetroNetwork.getNodes().get(terminalNode2).getCoord(), zhCenterCoord) < minInitialTerminalRadiusFromCenter 
							|| GeomDistance.calculate(newMetroNetwork.getNodes().get(terminalNode2).getCoord(), zhCenterCoord) > maxInitialTerminalRadiusFromCenter);
					safetyCounter++;
					if (safetyCounter == iterLimit) {
						continue OuterNetworkRouteLoop;
					}
				} while (GeomDistance.calculate(newMetroNetwork.getNodes().get(terminalNode1).getCoord(),
						newMetroNetwork.getNodes().get(terminalNode2).getCoord()) < minTerminalDistance
						&& safetyCounter < iterLimit);

//				Log.write("Terminal 1 = "+terminalFacility1.getName() + "   Node="+terminalNode1.toString()+" --> Coord="+newMetroNetwork.getNodes().get(terminalNode1).getCoord());
//				Log.write("Terminal 2 = "+terminalFacility2.getName() + "   Node="+terminalNode2.toString()+" --> Coord="+newMetroNetwork.getNodes().get(terminalNode2).getCoord());
				
				
				// Find Dijkstra --> nodeList
				List<Node> nodeList = null;
				if (shortestPathStrategy.equals("Dijkstra1")) {
					nodeList = DijkstraOwn_I.findShortestPathVirtualNetwork(newMetroNetwork, terminalNode1, terminalNode2);
				}
				if (shortestPathStrategy.equals("Dijkstra2")) {
					nodeList = DemoDijkstra.calculateShortestPath(newMetroNetwork, terminalNode1, terminalNode2);
				}
				if (nodeList == null || nodeList.size()<3) {
					Log.write("Oops, no shortest path available. Trying to create next networkRoute. Please lower minTerminalDistance"
							+ " ,or increase maxNewMetroLinkDistance (and - last - increase nMostFrequentLinks if required)!");
						continue OuterNetworkRouteLoop;
				}
//				Log.write("Dijkstra Nodelist:");
//				for (Node node: nodeList) {
//					Log.write(node.getId().toString() + "  --> Coord = "+node.getCoord());
//				}
				List<Id<Link>> linkList = nodeListToNetworkLinkList(newMetroNetwork, nodeList);
//				for (Id<Link> link: linkList) {
//					Log.write(link.toString());
//				}
				linkList.addAll(OppositeLinkListOf(linkList)); // extend linkList with its opposite direction for PT transportation!
				NetworkRoute networkRoute = RouteUtils.createNetworkRoute(linkList, newMetroNetwork);

				System.out.println("The new networkRoute is: [Length="+(networkRoute.getLinkIds().size()+2)+"] - " + networkRouteToLinkIdList(networkRoute).toString());
				networkRouteArray.add(networkRoute);
			}

			// Doing already in main file --> Not necessary to do here again:
			// Store all new networkRoutes in a separate network file for visualization
			// --> networkRoutesToNetwork(networkRouteArray, newMetroNetwork, fileName);
			return networkRouteArray;
		}

		public static Id<Node> facility2nodeId(Network newMetroNetwork, TransitStopFacility terminalFacility1) {
			for (Node node : newMetroNetwork.getNodes().values()) {
				if (node.getCoord().equals(terminalFacility1.getCoord())) {
					return node.getId();
				}
			}
			return null;
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

		public static List<Id<Link>> nodeListToNetworkLinkList(Network network, List<Node> nodeList) {
			List<Id<Link>> linkList = new ArrayList<Id<Link>>(nodeList.size() - 1);
			for (int n = 0; n < (nodeList.size()-1); n++) {
				for (Link l : nodeList.get(n).getOutLinks().values()) {
					if (l.getToNode().getId().equals(nodeList.get(n + 1).getId())) {
						linkList.add(l.getId());
					}
				}
			}
			return linkList;
		}

		public static List<Link> nodeListToNetworkLinkList2(Network network, List<Node> nodeList) {
			List<Link> linkList = new ArrayList<Link>(nodeList.size() - 1);
			for (int n = 0; n < (nodeList.size()-1); n++) {
				for (Link l : nodeList.get(n).getOutLinks().values()) {
					if (l.getToNode().getId().equals(nodeList.get(n + 1).getId())) {
						linkList.add(l);
					}
				}
			}
			return linkList;
		}
		
//		public static Id<Node> metroNodeFromOriginalLink(Id<Link> originalLinkRefID) {
//			Id<Node> metroNodeId = Id.createNodeId("MetroNodeLinkRef_"+originalLinkRefID.toString());
//			return metroNodeId;
//		}
		
//		public static Id<Link> orginalLinkFromMetroNode(Id<Node> metroNodeId){
//			String metroString = metroNodeId.toString();
//			Id<Link> originalLinkId = Id.createLinkId(metroString.substring(metroString.indexOf("_")+1));
//			return originalLinkId;
//		}
		
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
		
		public static Network copyNetworkToNetworkX(Network fromNetwork, Network toNetwork) {
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
		
		public static Network mergeNetworksX(Network n1, Network n2) {
			Network nOut = ScenarioUtils.loadScenario(ConfigUtils.createConfig()).getNetwork();
			nOut = copyNetworkToNetworkX(n1, nOut);
			nOut = copyNetworkToNetworkX(n2, nOut);
			return nOut;
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
				for (Id<Link> linkID : NetworkRoute2LinkIdList(networkRoute)) {
					totalLength += thisNetwork.getLinks().get(linkID).getLength();
				}
				return totalLength;
			}
			
			public static double MRoute2TotalLength(MRoute mRoute, Network thisNetwork) {
				double totalLength = 0.00;
				for (Id<Link> linkID : mRoute.linkList) {
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

	

//	@SuppressWarnings("unchecked")
//	public static void writeChartAverageGenerationNetworkAverageTravelTimes(int lastGeneration, String fileName) 	// only average scores
//			throws FileNotFoundException {
//		String generationPath = "zurich_1pm/Evolution/Population/HistoryLog/Generation";
//		Map<Integer, Double> generationsAverageTravelTime = new HashMap<Integer, Double>();
//		Map<Integer, Double> generationsAverageTravelTimeStdDev = new HashMap<Integer, Double>();
//		Map<String, NetworkScoreLog> networkScores = new HashMap<String, NetworkScoreLog>();
//		for (int g = 1; g <= lastGeneration; g++) {
//			double averageTravelTimeThisGeneration = 0.0;
//			double averageTravelTimeStdDevThisGeneration = 0.0;
//			networkScores = (Map<String, NetworkScoreLog>) XMLOps.readFromFile(networkScores.getClass(),
//					generationPath + g + "/networkScoreMap.xml");
//			for (NetworkScoreLog nsl : networkScores.values()) {
//				averageTravelTimeThisGeneration += nsl.averageTravelTime / networkScores.size();
//				averageTravelTimeStdDevThisGeneration += nsl.stdDeviationTravelTime / networkScores.size();
//			}
//			generationsAverageTravelTime.put(g, averageTravelTimeThisGeneration);
//			generationsAverageTravelTimeStdDev.put(g, averageTravelTimeStdDevThisGeneration);
//		}
//		XYLineChart chart = new XYLineChart("Evolution of Network Performance", "Generation", "Score");
//		chart.addSeries("Average Travel Time [min]", generationsAverageTravelTime);
//		chart.addSeries("Average Travel Time - Std Deviation [min]", generationsAverageTravelTimeStdDev);
//		chart.saveAsPng(fileName, 800, 600);
//	}
//
//	@SuppressWarnings("unchecked")
//	public static void writeChartBestGenerationNetworkAverageTravelTimes(int lastGeneration, String fileName) 	// only best scores
//			throws FileNotFoundException {
//		String generationPath = "zurich_1pm/Evolution/Population/HistoryLog/Generation";
//		Map<Integer, Double> generationsBestTravelTime = new HashMap<Integer, Double>();
//		Map<String, NetworkScoreLog> networkScores = new HashMap<String, NetworkScoreLog>();
//		for (int g = 1; g <= lastGeneration; g++) {
//			double bestAverageTravelTimeThisGeneration = Double.MAX_VALUE;
//			networkScores = (Map<String, NetworkScoreLog>) XMLOps.readFromFile(networkScores.getClass(),
//					generationPath + g + "/networkScoreMap.xml");
//			for (NetworkScoreLog nsl : networkScores.values()) {
//				if (nsl.averageTravelTime < bestAverageTravelTimeThisGeneration) {
//					bestAverageTravelTimeThisGeneration = nsl.averageTravelTime;
//					System.out.println("bestAverageTravelTimeThisGeneration = " + bestAverageTravelTimeThisGeneration);
//				}
//			}
//			generationsBestTravelTime.put(g, bestAverageTravelTimeThisGeneration);
//		}
//		XYLineChart chart = new XYLineChart("Evolution of Network Performance", "Generation", "Score");
//		chart.addSeries("Best Average Travel Time [min]", generationsBestTravelTime);
//		chart.saveAsPng(fileName, 800, 600);
//	}
	
	
	public static List<Id<Link>> OppositeLinkListOf(List<Id<Link>> linkList){
		List<Id<Link>> oppositeLinkList = new ArrayList<Id<Link>>(linkList.size());
		for (int c=0; c<linkList.size(); c++) {
			oppositeLinkList.add(ReverseLink(linkList.get(linkList.size()-1-c)));
		}
		return oppositeLinkList;
	}
	
	public static Id<Link> ReverseLink(Id<Link> linkId){
		String[] linkIdStrings = linkId.toString().split("_");
		Id<Link> reverseId = Id.createLinkId(linkIdStrings[1]+"_"+linkIdStrings[0]);
		return reverseId;
	}

	public static Link ReverseLinkEntire(Link link, Network network){
		return network.getFactory().createLink(Id.createLinkId(ReverseLink(link.getId())), link.getToNode(), link.getFromNode());
	}
	
	public static MNetworkPop developGeneration(Network globalNetwork, Map<Id<Link>, CustomMetroLinkAttributes> metroLinkAttributes,
			Map<String, NetworkScoreLog> networkScoreMap, MNetworkPop evoNetworksToProcessPlans, String populationName,
			Double alpha, Double pCrossOver, String crossoverRouletteStrategy, Double initialDepSpacing,
			boolean useOdPairsForInitialRoutes, int initialRoutesPerNetwork, String vehicleTypeName, double vehicleLength, double maxVelocity, 
			int vehicleSeats, int vehicleStandingRoom, String defaultPtMode, double stopTime, boolean blocksLane, boolean logEntireRoutes,
			double minCrossingDistanceFactorFromRouteEnd, double maxCrossingAngle, Coord zurich_NetworkCenterCoord, int lastIterationOriginal,
			double pMutation, double pBigChange, double pSmallChange, double routeDisutilityLimit,
			String shortestPathStrategy, Double minTerminalDistance, Double minTerminalRadiusFromCenter, Double maxTerminalRadiusFromCenter,
			Double minInitialTerminalRadiusFromCenter, Double maxInitialTerminalRadiusFromCenter, Double metroCityRadius, Boolean varyInitRouteSize, 
			Double tFirstDep, Double tLastDep, Double odConsiderationThreshold, Double xOffset, Double yOffset,
			Integer stopUnprofitableRoutesReplacementGEN, Integer blockFreqModGENs, Integer currentGEN, Integer lastGeneration,
			Double maxConnectingDistance) throws IOException {
		
		
		MNetworkPop newPopulation = Clone.mNetworkPop(evoNetworksToProcessPlans);
		
		// ELITE NETWORK
		MNetwork eliteMNetwork = NetworkEvolutionImpl.getEliteNetwork(networkScoreMap, evoNetworksToProcessPlans);

		// FREQUENCY MODIFICATIONS (set nDepartures=0, keep first/lastDep, change nVehicles --> DepSpacing will be changed accordingly in applyPT)
		if (currentGEN > blockFreqModGENs) { // block before xGEN
			newPopulation = EvoOpsFreqModifier.applyFrequencyModification2(newPopulation, eliteMNetwork.networkID, routeDisutilityLimit);			
		}
		// TODO might have vehicle pool: removed vehicle comes into pool first and is then redistributed. If route hits freq. < 4min, add vehicle to next strongest route
		
		// CROSS-OVERS (set nDepartures=0, average first/lastDep & nVehicles during mRouteCrossovers --> DepSpacing, nDep etc. will be changed accordingly in applyPT)
//		if (currentGEN > 25) {
//			pCrossOver = 0.20;
//		}
		newPopulation = EvoOpsCrossover.applyCrossovers(currentGEN, globalNetwork,  networkScoreMap,  newPopulation,  populationName,
				 eliteMNetwork, alpha, pCrossOver, crossoverRouletteStrategy, useOdPairsForInitialRoutes, 
				 vehicleTypeName, vehicleLength, maxVelocity, vehicleSeats, vehicleStandingRoom, defaultPtMode, stopTime, blocksLane,
				 logEntireRoutes, minCrossingDistanceFactorFromRouteEnd, maxCrossingAngle);
		
		// MUTATIONS (set nDepartures=0, keep nVehicles, keep first/lastDep, --> RouteLength, RoundTripTravelTime, DepSpacing will be changed accordingly in applyPT)
		newPopulation = EvoOpsMutator.applyMutations(currentGEN, newPopulation, globalNetwork, zurich_NetworkCenterCoord, lastIterationOriginal,
				pMutation, pBigChange, pSmallChange, routeDisutilityLimit,
				maxCrossingAngle, eliteMNetwork.networkID, metroLinkAttributes);
		
//		newPopulation.getNetworks().get("Network1").getRouteMap().remove("Network1_Route3");
//		newPopulation.getNetworks().get("Network1").getRouteMap().remove("Network1_Route4");
		
		// TOP UP NETWORK with routes if individuals have died out 
		EvoOpsRoutesAdder.topUpNetworkRouteMaps(initialRoutesPerNetwork, currentGEN, stopUnprofitableRoutesReplacementGEN, newPopulation, useOdPairsForInitialRoutes, shortestPathStrategy,
				minTerminalDistance, minTerminalRadiusFromCenter, maxTerminalRadiusFromCenter, minInitialTerminalRadiusFromCenter, maxInitialTerminalRadiusFromCenter,
				metroCityRadius, varyInitRouteSize, tFirstDep, tLastDep, eliteMNetwork,
				odConsiderationThreshold, zurich_NetworkCenterCoord, xOffset, yOffset);
		
		// MERGE SINGLE ROUTES TO A NETWORK
		// last merger is performed at the end of the second last GEN before last GEN is simulated, bc after last GEN sim no modifications are made to network
//		if (currentGEN % 4 == 0 || currentGEN == lastGeneration-1) {
//		}
		EvoOpsMerger.mergeRoutes(newPopulation, globalNetwork, maxConnectingDistance, metroLinkAttributes, eliteMNetwork.networkID,
				maxCrossingAngle);
		
		// APPLY TRANSIT + STORE POPULATION & TRANSITSCHEDULE (calculates & updates: routeLength, roundTripTravelTimes, nDepartures, depSpacing=d(nVehicles))
		EvoOpsPTEngine.applyPT(newPopulation, globalNetwork, metroLinkAttributes, eliteMNetwork.networkID,
				vehicleTypeName, vehicleLength, maxVelocity, vehicleSeats, vehicleStandingRoom, defaultPtMode, stopTime, blocksLane,
				useOdPairsForInitialRoutes, initialDepSpacing);
		
		// Remove empty networks (when all routes have died out)
		Iterator<Entry<String, MNetwork>> networkIter = newPopulation.networkMap.entrySet().iterator();
		while (networkIter.hasNext()) {
			Entry<String, MNetwork> networkEntry = networkIter.next();
			if (networkEntry.getValue().routeMap.size() == 0) {
				networkIter.remove();
			}
		}
		
		// calculate and Log total Nr. of vehicles
		for (MNetwork mn : newPopulation.networkMap.values()) {
			int nVehicles = 0;
			for (MRoute mr : mn.routeMap.values()) {
				nVehicles += mr.vehiclesNr;
			}
			mn.totalVehiclesNr = nVehicles;
//			Log.write(mn.networkID+" carries nMetroVehicles="+nVehicles);
		}
		// (Save data to files if do not use routesLogger before next mobsim)
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


	public static boolean mergerHasBeenExecutedPreviously(Map<Integer, List<String>> executedMergers, String nameParent1, String nameParent2) {
		for (List<String> mergedNetworks : executedMergers.values()) {
			if (mergedNetworks.contains(nameParent1) && mergedNetworks.contains(nameParent2)) {
				return true;
			}
		}
		return false;
	}


	public static MNetwork[] crossMNetworks(Network globalNetwork, MNetwork parentMNetwork1, MNetwork parentMNetwork2, String vehicleTypeName, double vehicleLength, double maxVelocity, 
			int vehicleSeats, int vehicleStandingRoom, String defaultPtMode, double stopTime, boolean blocksLane, boolean useOdPairsForInitialRoutes,
			double minCrossingDistanceFactorFromRouteEnd, double maxCrossingAngle, ParentNetworksWeight parentNetworksWeight) throws IOException {
			
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
				MRoute[] crossedRoutes = crossMRoutes(routeFromP1, routeFromP2, globalNetwork, minCrossingDistanceFactorFromRouteEnd, maxCrossingAngle,
						parentNetworksWeight);
				if (crossedRoutes != null) {
					Log.writeAndDisplay("   >>> MRoute Cross Success:  " + routeFromP1.routeID + " X " + routeFromP2.routeID);
					routesOut1.put(crossedRoutes[0].routeID, crossedRoutes[0]);
					routesOut2.put(crossedRoutes[1].routeID, crossedRoutes[1]);
					iter2.remove();
					iter1.remove();
					continue Loop1;
				}
				//else {Log.writeAndDisplay("   >>> MRoute Cross FAIL:  " + routeP1name + " X " + routeP2name);}
			}
		}
		// The following section for uncrossed routes shall enhance further crossing activity by trading entire routes between Parent1 and Parent2
		// add all routesFromP1 that could not be crossed with any routesFromP2 with 50:50 prob to Out1 or Out2
		for (MRoute routeFromP1 : routesParent1.values()) {
			if ((new Random()).nextDouble() < 0.5) {
				routesOut1.put(routeFromP1.routeID+"x", routeFromP1);
				// naming does not matter as the routes are renamed anyways,
				// but the "x" makes sure we do not accidentally replace a crossed route with the same name
			}
			else {
				routesOut2.put(routeFromP1.routeID+"x", routeFromP1);
			}
		}
		// add all routesFromP2 that could not be crossed with any routesFromP1 with 50:50 prob to Out1 or Out2
		for (MRoute routeFromP2 : routesParent2.values()) {
			if ((new Random()).nextDouble() < 0.5) {
				routesOut1.put(routeFromP2.routeID+"x", routeFromP2);
			}
			else {
				routesOut2.put(routeFromP2.routeID+"x", routeFromP2);
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
			double minCrossingDistanceFactorFromRouteEnd, double maxCrossingAngle, ParentNetworksWeight parentNetworksWeight) throws IOException {
		
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
			// this (min routeLength) and minCrossingDistanceFactorFromRouteEnd ensure that routes always keep a min link length also after crossover
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
			parentNetworksWeight.child1.put(1, parentNetworksWeight.child1.get(1) + NetworkOperators.linkList2length(route1LinkListOneway.subList(0, route1LinkListOneway.indexOf(crossLink1)), globalNetwork));
			crossLinkList1.addAll(route2LinkListOneway.subList(route2LinkListOneway.indexOf(crossLink2), route2LinkListOneway.size()));		// ... + route2 after crossing
			parentNetworksWeight.child1.put(2, parentNetworksWeight.child1.get(2) + NetworkOperators.linkList2length(route2LinkListOneway.subList(route2LinkListOneway.indexOf(crossLink2), route2LinkListOneway.size()), globalNetwork));
			crossLinkList2.addAll(route2LinkListOneway.subList(0, route2LinkListOneway.indexOf(crossLink2)));								// route2 part before crossing ...
			parentNetworksWeight.child2.put(2, parentNetworksWeight.child2.get(2) + NetworkOperators.linkList2length(route2LinkListOneway.subList(0, route2LinkListOneway.indexOf(crossLink2)), globalNetwork));
			crossLinkList2.addAll(route1LinkListOneway.subList(route1LinkListOneway.indexOf(crossLink1), route1LinkListOneway.size()));		// ... + route1 after crossing
			parentNetworksWeight.child2.put(1, parentNetworksWeight.child2.get(1) + NetworkOperators.linkList2length(route1LinkListOneway.subList(route1LinkListOneway.indexOf(crossLink1), route1LinkListOneway.size()), globalNetwork));

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
			mRouteNew1.lifeTime = 0.5*(routeFromP1.lifeTime + routeFromP2.lifeTime);
			mRouteNew2.lifeTime = 0.5*(routeFromP1.lifeTime + routeFromP2.lifeTime);
			mRouteNew1.firstDeparture = (int) Math.round(0.5*(routeFromP1.firstDeparture+routeFromP2.firstDeparture));
			mRouteNew2.firstDeparture = (int) Math.round(0.5*(routeFromP1.firstDeparture+routeFromP2.firstDeparture));
			mRouteNew1.lastDeparture = (int) Math.round(0.5*(routeFromP1.lastDeparture+routeFromP2.lastDeparture));
			mRouteNew2.lastDeparture = (int) Math.round(0.5*(routeFromP1.lastDeparture+routeFromP2.lastDeparture));
			// make sure a new route has a minimum of 3 vehicles so it doesn't die out straight away
			if(routeFromP1.vehiclesNr > routeFromP2.vehiclesNr) {
				mRouteNew1.vehiclesNr = (int) Math.max(3.0,Math.ceil(0.5*(routeFromP1.vehiclesNr+routeFromP2.vehiclesNr)));
				mRouteNew2.vehiclesNr = (int) Math.max(3.0,Math.floor(0.5*(routeFromP1.vehiclesNr+routeFromP2.vehiclesNr)));				
			}
			else {
				mRouteNew1.vehiclesNr = (int) Math.max(3.0,Math.floor(0.5*(routeFromP1.vehiclesNr+routeFromP2.vehiclesNr)));
				mRouteNew2.vehiclesNr = (int) Math.max(3.0,Math.ceil(0.5*(routeFromP1.vehiclesNr+routeFromP2.vehiclesNr)));				
			}
			mRouteNew1.significantRouteModOccured = true;
			mRouteNew2.significantRouteModOccured = true;
			crossedRoutes[0] = mRouteNew1;
			crossedRoutes[1] = mRouteNew2;
			return crossedRoutes;
		}
		else {
			return null;
		}
	}

	
	
	public static String selectMNetworkByRoulette(MNetworkPop networkPop, double alpha, Map<String, NetworkScoreLog> networkScoreMap,
			String strategy) throws IOException {
		
		// for plausibility use add only those networks that have a positive benefit value (this is only the benefits and not the costs)
		// make separate score map to select from for this purpose
		Map<String, NetworkScoreLog> networkScoreMapPositiveBenefits = new HashMap<String, NetworkScoreLog>();
		for (String networkId : networkScoreMap.keySet()) {
			Log.write(networkId + " has annual benefit = "+networkPop.networkMap.get(networkId).annualBenefit);
			if (networkPop.networkMap.get(networkId).annualBenefit > 0.0) {
				networkScoreMapPositiveBenefits.put(networkId, networkScoreMap.get(networkId));
			}
		}
//		Log.write("networkScoreMap = "+networkScoreMap.toString());
//		Log.write("networkScoreMapPositiveBenefits = "+networkScoreMapPositiveBenefits.toString());
		if (networkScoreMapPositiveBenefits.size() == 0) {
			Log.write("No network features any positive benefits. Using any network for cross-over.");
			networkScoreMapPositiveBenefits = networkScoreMap;
		}
		
		// Option 1 : All positive overallScoreProportional
		if (strategy.equals("allPositiveProportional")) {
			double totalRouletteScore = 0.0;
			for (String networkName : networkScoreMapPositiveBenefits.keySet()) {
				networkScoreMapPositiveBenefits.get(networkName).rouletteScore = networkScoreMapPositiveBenefits.get(networkName).overallScore/(1.0E8) + 1.5;
				totalRouletteScore += networkScoreMapPositiveBenefits.get(networkName).rouletteScore;		
			}
			Random r = new Random();
			double rD = r.nextDouble();
			double attemptedProb = 0.0;
			for (String networkName : networkScoreMapPositiveBenefits.keySet()) {
				if (attemptedProb/totalRouletteScore <= rD   &&   rD < (attemptedProb+networkScoreMapPositiveBenefits.get(networkName).rouletteScore)/totalRouletteScore) {
					return networkName;
				}
				attemptedProb += networkScoreMapPositiveBenefits.get(networkName).rouletteScore;
			}
			System.out.println("Roulette has not selected any network! Returning NULL...");
			return null;
		}
		// Option 2 : overallScoreRank
		else if (strategy.equals("rank")) {
			List<String> rankedNetworks = sortNetworksByScore(networkScoreMapPositiveBenefits);
			int N = networkScoreMapPositiveBenefits.size();
			Random r = new Random();
			double rD = r.nextDouble();
			double attemptedProb = 0.0;
			for (int n=1; n<=N; n++) {
				if (attemptedProb <= rD   &&   rD < (attemptedProb+(N-n+1)/(N*(0.5*N+0.5)))) {
					return rankedNetworks.get(n-1);
				}
				attemptedProb += (N-n+1)/(N*(0.5*N+0.5));
			}
			System.out.println("Roulette has not selected any network! Returning NULL...");
			return null;
		}
		// Option 3 : Tournament selection
		else if (strategy.equals("tournamentSelection3")) {
			int n=3;
			if (n > networkScoreMapPositiveBenefits.size()) {
				Log.write("Tournament has less than n="+n+" networks. Therefore, n is set to tournament size -> n="+networkScoreMapPositiveBenefits.size());
				n = networkScoreMapPositiveBenefits.size();
			}
			List<String> tournamentNetworks = nRandomNetworks(networkScoreMapPositiveBenefits.keySet(), n);
			String winnerNetwork = getTournamentWinner(tournamentNetworks, networkScoreMapPositiveBenefits);
			return winnerNetwork;
		}
		// Option 4 : logarithmic
		else if (strategy.equals("logarithmic")) {
			int N = networkScoreMapPositiveBenefits.size();
			double totalRouletteScore = 0.0;
			for (String networkName : networkScoreMapPositiveBenefits.keySet()) {
				double rouletteScore = Math.exp(alpha*(networkScoreMapPositiveBenefits.get(networkName).overallScore/(1.0E8)+1.5));
				networkScoreMapPositiveBenefits.get(networkName).rouletteScore = rouletteScore;
				totalRouletteScore += rouletteScore;
			}
			Random r = new Random();
			double rD = r.nextDouble();
			double attemptedProb = 0.0;
			for (String networkName : networkScoreMapPositiveBenefits.keySet()) {
				if (attemptedProb/totalRouletteScore <= rD   &&   rD < (attemptedProb+networkScoreMapPositiveBenefits.get(networkName).rouletteScore)/totalRouletteScore) {
					return networkName;
				}
				attemptedProb += networkScoreMapPositiveBenefits.get(networkName).rouletteScore;
			}
			System.out.println("Roulette has not selected any network! Returning NULL...");
			return null;
		}
		else {
			Log.write("Roulette strategy="+strategy+" does not confrom to any of the stored strategies. Please check spelling and existence. Returning NULL ...");
			return null;
		}
		
	}

	
	public static List<String> sortNetworksByScore(Map<String, NetworkScoreLog> networkScoreMap) {
		List<String> sortedNetworks = new ArrayList<String>();
		sortedNetworks.add("");	// do this just as a helper to start off with so that valueArray we compare to is not empty
		List<Double> sortedValues = new ArrayList<Double>();
		sortedValues.add(-Double.MAX_VALUE);
		for (Entry<String,NetworkScoreLog> entry : networkScoreMap.entrySet()) {
			for (int index=0; index < sortedNetworks.size(); index++) {
				if (entry.getValue().overallScore > sortedValues.get(index)) {
					sortedNetworks.add(index, entry.getKey());
					sortedValues.add(index, entry.getValue().overallScore);
					break;
				}
			}
		}
		sortedNetworks.remove(sortedNetworks.size()-1); // removing the "" entry at the end again.
		// sortedValues.remove(sortedValues.size()-1);
		return sortedNetworks;
	}


	public static String getTournamentWinner(List<String> tournamentNetworks, Map<String, NetworkScoreLog> networkScoreMap) {
		String winnerNetwork = tournamentNetworks.get(0);
		double max = networkScoreMap.get(tournamentNetworks.get(0)).overallScore;
		for (String network : tournamentNetworks) {
			if (networkScoreMap.get(network).overallScore > max) {
				winnerNetwork = network;
				max = networkScoreMap.get(network).overallScore;
			}
		}
		return winnerNetwork;
	}


	public static List<String> nRandomNetworks(Set<String> allNetworks, int n) {
		List<String> chosenNetworks = new ArrayList<String>();
		outerLoop:
		while(chosenNetworks.size()<n) {
			Random r = new Random();
			int index = r.nextInt(allNetworks.size());
			int i = 0;
			for (String network : allNetworks) {
				if (i == index) {
					if ( ! chosenNetworks.contains(network)) {
						chosenNetworks.add(network);
					}
					continue outerLoop;
				}
				i++;
			}
		}
		
		return chosenNetworks;
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

	public static void saveCurrentMRoutes2HistoryLog(MNetworkPop latestPopulation, int generationNr, Network globalNetwork, int storeScheduleInterval) throws FileNotFoundException {
		String historyFileLocation = "zurich_1pm/Evolution/Population/HistoryLog/Generation"+(generationNr)+"/MRoutes";
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
			if ((generationNr % storeScheduleInterval) == 0) {
				File sourceSchedule = new File("zurich_1pm/Evolution/Population/"+mn.networkID+"/MergedSchedule.xml"); 
				File destSchedule = new File("zurich_1pm/Evolution/Population/HistoryLog/Generation"+(generationNr)+"/"+mn.networkID+"/MergedSchedule.xml");
				File sourceVehicles = new File("zurich_1pm/Evolution/Population/"+mn.networkID+"/MergedVehicles.xml"); 
				File destVehicles = new File("zurich_1pm/Evolution/Population/HistoryLog/Generation"+(generationNr)+"/"+mn.networkID+"/MergedVehicles.xml");		
				try {
					FileUtils.copyFile(sourceSchedule, destSchedule);
					FileUtils.copyFile(sourceVehicles, destVehicles);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
	}
	
	public static Network updateGlobalNetwork(String globalNetworkFile) {
		Config config = ConfigUtils.createConfig();
		config.getModules().get("network").addParam("inputNetworkFile", "zurich_1pm/Evolution/Population/BaseInfrastructure/GlobalNetwork.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Network globalNetwork = scenario.getNetwork();
		return globalNetwork;
	}
	
	public static double depSpacingCalculator(int nVehicles, double roundtripTravelTime) {
		return Math.ceil(roundtripTravelTime/nVehicles);
	}


	public static boolean logResults(List<Map<String, NetworkScoreLog>> networkScoreMaps, String historyFileLocation,
			String networkScoreMapGeneralLocation, MNetworkPop latestPopulation, double averageTravelTimePerformanceGoal,
			int finalGeneration, int lastIteration, double populationFactor,
			Network globalNetwork, Map<Id<Link>, CustomMetroLinkAttributes> metroLinkAttributes, Double lifeTime) throws IOException {
		
		Map<String, NetworkScoreLog> networkScoreMap = new HashMap<String, NetworkScoreLog>();
		boolean performanceGoalAccomplished = false;
		new File(historyFileLocation).mkdirs();
		
		MNetwork successfulNetwork = null;
		double successfulAverageTravelTime = 0.0;
		
		for (String networkName : latestPopulation.getNetworks().keySet()) {
			MNetwork mnetwork = latestPopulation.getNetworks().get(networkName);
			if(latestPopulation.modifiedNetworksInLastEvolution.contains(mnetwork.getNetworkID())) {
				
				mnetwork.lifeTime = lifeTime;
				mnetwork.calculateRoutesAndNetworkScore(lastIteration, populationFactor, globalNetwork, metroLinkAttributes,
						"zurich_1pm/cbpParametersOriginal/", "zurich_1pm/Evolution/Population/", "1"); // include here also part of routesHandling
				XMLOps.writeToFile(mnetwork, "zurich_1pm/Evolution/Population/"+mnetwork.networkID+"/M"+mnetwork.networkID+".xml");
				if (performanceGoalAccomplished == false) {		// checking whether performance goal achieved
					if (mnetwork.averageTravelTime < averageTravelTimePerformanceGoal) {
						performanceGoalAccomplished = true;
						successfulNetwork = mnetwork;
						successfulAverageTravelTime = mnetwork.getAverageTravelTime();
					}					
				}
				if (performanceGoalAccomplished == true) {		// this loop is for the case that performance goal is achieved by one network, but in same iteration another network has an even better score
					if (mnetwork.averageTravelTime < successfulAverageTravelTime) {
						successfulAverageTravelTime = mnetwork.getAverageTravelTime();
						successfulNetwork = mnetwork;
					}				
				}
			}	// do from here for all networks, also those who have not been modified!
			NetworkScoreLog nsl = new NetworkScoreLog();
			nsl.NetworkScore2LogMap(mnetwork);			// copy network parameters to network score log for storing evolution
			networkScoreMap.put(networkName, nsl);		// network score map is finally stored
			Log.writeAndDisplay("   >>> "+mnetwork.networkID+": OVERALL SCORE = " + mnetwork.overallScore);
			Log.writeAndDisplay("   >>> "+mnetwork.networkID+": Total Metro Passengers KM = " + mnetwork.personMetroDist);
			//Log.writeAndDisplay("   >>> "+mnetwork.networkID+": Average Travel Time = " + mnetwork.averageTravelTime);
			
			// mnetwork.network = null;		// set to null before storing to file bc would use up too much storage and is not needed (network can be created from other data)
		}
		networkScoreMaps.add(networkScoreMap);
		for (MNetwork mn : latestPopulation.getNetworks().values()) {
			Log.write(mn.networkID + " - Overall Score = " + mn.overallScore);
		}
		
		XMLOps.writeToFile(networkScoreMaps, networkScoreMapGeneralLocation);
		XMLOps.writeToFile(networkScoreMap, historyFileLocation+"/networkScoreMap.xml");
		
		
		if (performanceGoalAccomplished == true) {
			System.out.println("Performance Goal has been achieved in Generation " +finalGeneration+ " by Network "+successfulNetwork.networkID+" at averageTravelTime "+successfulAverageTravelTime);			// display most important analyzed data here			
			Log.write("PERFORMANCE GOAL ACHIEVED: in Generation " +finalGeneration+ " by Network "+successfulNetwork.networkID+" at averageTravelTime "+successfulAverageTravelTime);
			return true;
		}
		else {
			return false;
		}
	}
	
	public static List<String> sortMapByValueScore(Map<String, Double> routePerformances) {
		List<String> order = new ArrayList<String>();
		String os = "";
		int addingIndex = 0;
		for (String s : routePerformances.keySet()) {
			for (int i=0; i<order.size(); i++) {
				os = order.get(i);
				addingIndex = order.size();
				if(routePerformances.get(s) > routePerformances.get(os)) {
					addingIndex = i;
					break;
				}
			}
			order.add(addingIndex, s);
		}
		return order;
	}
	
	public static Map<String,CustomStop> getOriginalRailwayStations(double radius, Coord zurich_NetworkCenterCoord,
			Map<String, CustomStop> railStopsZH) throws IOException {
		Config conf = ConfigUtils.createConfig();
		conf.getModules().get("transit").addParam("transitScheduleFile","zurich_1pm/zurich_transit_schedule.xml.gz");
		conf.getModules().get("network").addParam("inputNetworkFile","zurich_1pm/zurich_network.xml.gz");
		Scenario scen = ScenarioUtils.loadScenario(conf);
		TransitSchedule ts = scen.getTransitSchedule();
		Network originalNetwork = scen.getNetwork();
		Network isolNetwork = ScenarioUtils.loadScenario(ConfigUtils.createConfig()).getNetwork();

		Id<Node> tempFromNodeId;
		Node tempFromNode;
		Id<Node> tempToNodeId;
		Node tempToNode;
		Id<Link> tempLinkId;
		Link tempLink;
		Link link;
		
		for (TransitLine tl : ts.getTransitLines().values()) {
			for (TransitRoute tr : tl.getRoutes().values()) {
				if (tr.getTransportMode().toString().equals("rail")) {
					// PART A: Isolate rails network for visualization
					List<Id<Link>> linkList = NetworkRoute2LinkIdList(tr.getRoute());
					for (Id<Link> linkId : linkList) {
						link = originalNetwork.getLinks().get(linkId);
						tempFromNodeId = Id.createNodeId(link.getFromNode().getId().toString()+"R");
						if (isolNetwork.getNodes().keySet().contains(tempFromNodeId)==false) {
							tempFromNode = isolNetwork.getFactory().createNode(tempFromNodeId, link.getFromNode().getCoord());
							isolNetwork.addNode(tempFromNode);
						}
						tempToNodeId = Id.createNodeId(link.getToNode().getId().toString()+"R");
						if (isolNetwork.getNodes().keySet().contains(tempToNodeId)==false) {
							tempToNode = isolNetwork.getFactory().createNode(tempToNodeId, link.getToNode().getCoord());
							isolNetwork.addNode(tempToNode);
						}
						tempLinkId = Id.createLinkId(tempFromNodeId+"_"+tempToNodeId);
						if (isolNetwork.getLinks().keySet().contains(tempLinkId)==false) {
							tempLink = isolNetwork.getFactory().createLink(tempLinkId, isolNetwork.getNodes().get(tempFromNodeId), isolNetwork.getNodes().get(tempToNodeId));
							isolNetwork.addLink(tempLink);
						}
					}
					// PART B: Isolate railsStopFacilities for archiving and later facilities constructing
					for (TransitRouteStop trs : tr.getStops()) {
						TransitStopFacility tsf = trs.getStopFacility();
						String stopName = tsf.getId().toString().substring(0, tsf.getId().toString().indexOf("."));
						// stopFacility has id="8500562.link:920757". First part is unique to the stop, but it can have several refLinks (second part)
						if (railStopsZH.keySet().contains(stopName) == false) {
							railStopsZH.put(stopName, new CustomStop(tsf, tsf.getLinkId(), "rail"));							
						}
						else if (railStopsZH.get(stopName).originalTransitStopFacilities.contains(tsf)==false) {
							railStopsZH.get(stopName).originalTransitStopFacilities.add(tsf);
							railStopsZH.get(stopName).linkRefIds.add(tsf.getLinkId());
						}
					}
				}
			}
		}
		
		// check here if same superName facilities always have same coordinates. This is very important for mapping coords to facilities.
		/*for (CustomStop cs : railStopsZH.values()) {
			for (TransitStopFacility thisT : cs.originalTransitStopFacilities) {
				for (TransitStopFacility otherT : cs.originalTransitStopFacilities) {
					if (thisT.getCoord().equals(otherT.getCoord()) == false) {
						Log.write("ERROR: Facilities with same superName not at same coordinates!!");
						Log.write(thisT.getId().toString() + "   " + thisT.getName() + "   " + thisT.getCoord());
						Log.write(otherT.getId().toString() + "   " + otherT.getName() + "   " + otherT.getCoord());
						System.exit(0);
					}
				}
			}
		}*/
		
		// PART A: writeIsolatedRailwayNetwork
		NetworkWriter nwA = new NetworkWriter(isolNetwork);
		nwA.write("zurich_1pm/Evolution/Population/BaseInfrastructure/zurich_networkRailOriginal_Rad"+((int) radius)+".xml");
		
		// PART B: writeRailwayStopFacilities by means of building representing nodes at their respective locations
		Iterator<Entry<String, CustomStop>> stopEntryIter = railStopsZH.entrySet().iterator();
		while(stopEntryIter.hasNext()) {
			Entry<String, CustomStop> stopEntry = stopEntryIter.next();
			if(GeomDistance.calculate(zurich_NetworkCenterCoord, stopEntry.getValue().originalMainTransitStopFacility.getCoord()) > radius) {
				stopEntryIter.remove();
			}
		}
		Network railNetwork = ScenarioUtils.loadScenario(ConfigUtils.createConfig()).getNetwork();
		NetworkFactory nf = railNetwork.getFactory();
		for(String stop : railStopsZH.keySet()) {
			railNetwork.addNode(nf.createNode(Id.createNodeId(stop), railStopsZH.get(stop).originalMainTransitStopFacility.getCoord()));
		}
		NetworkWriter nwB = new NetworkWriter(railNetwork);
		nwB.write("zurich_1pm/Evolution/Population/BaseInfrastructure/zurich_networkRailStationsRadius"+((int) radius)+".xml");
		
		return railStopsZH;
	}
	
	
	
	public static String removeString(String stringToProcess, String substringToDelete){
		if (substringToDelete.equals(".")) {
			substringToDelete = "\\.";
		}
		if (substringToDelete.equals("'")) {
			substringToDelete = "\\'";
		}
		if (substringToDelete.equals("\\")) {
			substringToDelete = "\\\\";
		}
		String[] substrings = stringToProcess.split(substringToDelete);
		String filteredString = "";
		for (String s : substrings) {
			filteredString += s;
		}
		return filteredString;
	}
	
	public static String removeStrings(String stringIn, List<String> removeStrings) {
		String sout = stringIn;
		for (String s : removeStrings) {
			sout = removeString(sout, s);
		}
		return sout;
	}

	public static String removeSpecialChar(String stringIn) {
		List<String> removeStrings = Arrays.asList("_","-",","," ",".","'","/","\\");
		return removeStrings(stringIn, removeStrings);
	}
	
 	public static String cutString(String stringToProcess, String subtringToCutOffFrom){
		if (stringToProcess.contains(subtringToCutOffFrom)) {
			return stringToProcess.substring(0, stringToProcess.indexOf(subtringToCutOffFrom)); 			
		}
		else {
			return stringToProcess;
		}
 	}
 	
 	
 	public static Network createMetroNetworkFromRailwayNetwork2( Map<String, CustomStop> railStops,
			Map<String, CustomStop> outerCityMetroStops, Map<Id<Link>, CustomMetroLinkAttributes> metroLinkAttributes, Network originalNetwork, Network innerCityMetroNetwork,
			String transitScheduleFileName, String fileName) throws IOException {
		
		// Extract copy of railway network from Zürich Scenario and convert (rename and connect to facilities) it to a metroNetwork:
			// - make new facilities (add to transitSchedule)
			// - make new nodes (add to network) and connecting links
			// - convert existing nodes/links to a metroVersion (with naming)
		
			// Go through customRailStops (=all Zurich railway stopFacilities):
				// - For all Facilities already added to innerCitySchedule: Create and add IDENTICAL node to isolated railwayNetwork --> CONSISTENCY WHEN MERGING later on!!
				// - For all Facilities not added to transitSchedule yet (marked in customRailStops), create & add node & facility to new transitSchedule, mark in customRailStops
		// Run along TransitRoutes and create new metro links and nodes (also make correctly named new facility node connectors) 
		// Update metroLinkAttributes along the way with the originalLinkIds and the new stopFacilities!
		Config conf = ConfigUtils.createConfig();
		conf.getModules().get("transit").addParam("transitScheduleFile",transitScheduleFileName);
		Scenario sc = ScenarioUtils.loadScenario(conf);
		TransitSchedule metroStopFacilities = sc.getTransitSchedule();
		TransitScheduleFactory tsf = metroStopFacilities.getFactory();
		Network outerCityMetroNetwork = sc.getNetwork();
		NetworkFactory networkFactory = outerCityMetroNetwork.getFactory();
		
//		List<Id<Node>[]> nodeStrings = new ArrayList<Id<Node>[]>();
		List<List<Id<Node>>> nodeStrings = new ArrayList<List<Id<Node>>>();
		nodeStrings.add(Arrays.asList(Id.createNodeId("2282196295"), Id.createNodeId("1189293948")));
		// fill in nodeStrings now!  2282196295
		
		for (int s=0; s<nodeStrings.size(); s++) {
			List<Id<Node>> nodes = nodeStrings.get(s);
			Id<Node> lastNodeId = nodes.get(0);
			Node lastNode = originalNetwork.getNodes().get(lastNodeId);
			Node lastNodeMetro = networkFactory.createNode(Id.createNodeId("zhRailNode"+lastNodeId), lastNode.getCoord());
			outerCityMetroNetwork.addNode(lastNodeMetro);
			for (Id<Node> thisNodeId : nodes.subList(1, nodes.size())) {
				Node thisNode = originalNetwork.getNodes().get(thisNodeId);
				Node thisNodeMetro = networkFactory.createNode(Id.createNodeId("zhRailNode"+thisNodeId), thisNode.getCoord());
				outerCityMetroNetwork.addNode(thisNodeMetro);
				Id<Link> linkThereId = Id.createLinkId(lastNodeMetro.getId().toString()+"_"+thisNodeMetro.getId().toString());
				Link linkThere = networkFactory.createLink(linkThereId, lastNodeMetro, thisNodeMetro);
				Id<Link> linkBackId = ReverseLink(linkThereId);
				Link linkBack = networkFactory.createLink(linkBackId, thisNodeMetro, lastNodeMetro);
				outerCityMetroNetwork.addLink(linkThere);
				outerCityMetroNetwork.addLink(linkBack);
				metroLinkAttributes.put(linkThereId, new CustomMetroLinkAttributes("rail2newMetro", null));
				metroLinkAttributes.put(linkBackId, new CustomMetroLinkAttributes("rail2newMetro", null));	
			}
		}
		
		Node newNode;
		TransitStopFacility metroCloneFacility;
		for (CustomStop railStop : railStops.values()) {
			Log.write("Considering facility = "+railStop.originalMainTransitStopFacility.getName());
			if (railStop.addedToNewSchedule == true) {
				Log.write("Already added to network.");
				Node addedNodeInnerCity = innerCityMetroNetwork.getNodes().get(railStop.newNetworkNode);	
				newNode = networkFactory.createNode(addedNodeInnerCity.getId(), addedNodeInnerCity.getCoord());	// build identical node as in innerCityMetroNetwork (Consistency)
				connectNodeToRailwayStrings(newNode, outerCityMetroNetwork, railStop, metroLinkAttributes);
			}
			else {
				Id<Node> newNodeId = Id.createNodeId("zhRailStopNode" + removeSpecialChar(railStop.originalMainTransitStopFacility.getName()));
				newNode = networkFactory.createNode(newNodeId, railStop.originalMainTransitStopFacility.getCoord());
				String originalStopFacilityId = railStop.originalMainTransitStopFacility.getId().toString();
				String originalStopFacilityName = railStop.originalMainTransitStopFacility.getName().toString();
				String newStopSuperName = cutString(originalStopFacilityId, ".");
				String newMetroStopFacilityId = newStopSuperName+"_metro"; // stopFacilityId = superName.refLinkId --> Want only superName
				String newMetroStopFacilityName = removeSpecialChar(originalStopFacilityName)+"_metro"; // don't want blanks in stop name
				metroCloneFacility = tsf.createTransitStopFacility(Id.create(newMetroStopFacilityId, TransitStopFacility.class), 
						railStop.originalMainTransitStopFacility.getCoord(), railStop.originalMainTransitStopFacility.getIsBlockingLane());
				metroCloneFacility.setName(newMetroStopFacilityName);
				boolean newNodeHasBeenConnected = connectNodeToRailwayStrings(newNode, outerCityMetroNetwork, railStop, metroLinkAttributes);
				if (newNodeHasBeenConnected) {		// if newNode and therefore metroCloneFacility could be connected. If not, then stop can be disregarded anyways.
					metroCloneFacility.setLinkId(newNode.getOutLinks().keySet().iterator().next());
					metroStopFacilities.addStopFacility(metroCloneFacility);
					outerCityMetroStops.put(newStopSuperName, new CustomStop(metroCloneFacility, newNode.getId(), "newMetro", true));
					railStop.addedToNewSchedule = true;
					railStop.transitStopFacility = metroCloneFacility;	// due to this being in the if loop, we can compare all
																		// railStop.transitStopFacility to railStop.originalTSF
																		// and see which ones have not been added and therefore not connected
					railStop.newNetworkNode = newNode.getId();
				}
			}
		}
		
//		XMLOps.readFromFile(railStop.getClass(), "zurich_1pm/Evolution/Population/BaseInfrastructure/metroStopAttributes.xml"));
		
		TransitScheduleWriter tsw = new TransitScheduleWriter(metroStopFacilities);
		tsw.writeFile(transitScheduleFileName);
	
		NetworkWriter nw = new NetworkWriter(outerCityMetroNetwork);
		nw.write(fileName);
		return outerCityMetroNetwork;
	}


	public static boolean connectNodeToRailwayStrings(Node thisNode, Network outerCityMetroNetwork, CustomStop railStop, 
			Map<Id<Link>, CustomMetroLinkAttributes> metroLinkAttributes) {
		List<Node> connectableNodes = new ArrayList<Node>();
		for (Node otherNode : outerCityMetroNetwork.getNodes().values()) {
			if (thisNode.getId().equals(otherNode.getId())) {
				continue;
			}
			else if (GeomDistance.betweenNodes(thisNode, otherNode) < 200) {
				connectableNodes.add(otherNode);
			}
		}
		if (connectableNodes.size() > 0) {
			outerCityMetroNetwork.addNode(thisNode);
			for (Node otherNode : connectableNodes) {
				Id<Link> linkThereId = Id.createLinkId(thisNode.getId().toString() + "_" + otherNode.getId().toString());
				Link linkThere = outerCityMetroNetwork.getFactory().createLink(linkThereId, thisNode, otherNode);
				outerCityMetroNetwork.addLink(linkThere);
				CustomMetroLinkAttributes cmlaThere = new CustomMetroLinkAttributes("rail2newMetro", null);
				cmlaThere.fromNodeStopFacility = railStop.transitStopFacility;
				metroLinkAttributes.put(linkThereId, cmlaThere);

				Id<Link> linkBackId = ReverseLink(linkThereId);
				Link linkBack = outerCityMetroNetwork.getFactory().createLink(linkBackId, otherNode, thisNode);
				outerCityMetroNetwork.addLink(linkBack);
				CustomMetroLinkAttributes cmlaBack = new CustomMetroLinkAttributes("rail2newMetro", null);
				cmlaBack.toNodeStopFacility = railStop.transitStopFacility;
				metroLinkAttributes.put(linkThereId, cmlaBack);
			}
			return true;
		}
		else {
			return false;
		}
		

	}


	public static void freeSpace(Integer lastGeneration, Integer keepGenerationInterval, Integer populationSize) throws IOException {
		for (Integer gen=1; gen<=lastGeneration-1; gen++) {
			if (gen % keepGenerationInterval == 0) {	// keep every 10th generation for potential recall issue
				continue;
			}
			for (Integer nw=1; nw<=populationSize; nw++) {
				File f = new File("zurich_1pm/Evolution/Population/HistoryLog/Generation"+gen+"/Network"+nw);
		        if (f.exists()) {
		       	  FileUtils.cleanDirectory(f); //clean out directory (this is optional -- but good know)
		    	  FileUtils.forceDelete(f); //delete directory
		        }
		        else {
		        	Log.write("Free Space Processing: "+f.toString()+" does not exist and can therefore not be deleted.");
		        }
			}
		}
		
	}
 	
	
	
	
	
}