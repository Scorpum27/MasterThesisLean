package ch.ethz.matsim.students.samark;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
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

public class Metro_NetworkCreatorCopy {

	public static void run() {
		// TODO METRO Network

		// Run RunScenario First to have simulation output that can be analyzed
		Config originalConfig = ConfigUtils.loadConfig("zurich_1pm/zurich_config.xml");
		Scenario originalScenario = ScenarioUtils.loadScenario(originalConfig);
		originalScenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DefaultEnrichedTransitRoute.class,
				new DefaultEnrichedTransitRouteFactory()); // why do we need this again?
		Network originalNetwork = originalScenario.getNetwork(); // NetworkFactory netFac = network.getFactory();
		TransitSchedule originalTransitSchedule = originalScenario.getTransitSchedule();
		
		Coord zurich_NetworkCenterCoord = new Coord(2683099.3305, 1247442.9076);
		double metroCityRadius = 1564.00;		// default 1564.00

		// Initialize a customLinkMap
		Map<Id<Link>, CustomLinkAttributes> customLinkMap = Metro_NetworkImpl.createCustomLinkMap(originalNetwork, null);

		// Run event handler to count movements on each stop facility and add traffic
		// data to customLinkMap
		int iterationToRead = 100; // set default = 400; Which iteration to read from output of inner loop
									// simulation
		Map<Id<Link>, CustomLinkAttributes> processedLinkMap = Metro_NetworkImpl
				.runPTStopTrafficScanner(new PT_StopTrafficCounter(), customLinkMap, iterationToRead, originalNetwork, null);

		// Select all metro candidate links by setting bounds on their location (distance from city center)
		double minMetroRadiusFromCenter = metroCityRadius * 0.00; // set default = 0.00 to not restrict metro network in city center
		double maxMetroRadiusFromCenter = metroCityRadius * 4.00; // set default = 2.50 for reasonable results
		Map<Id<Link>, CustomLinkAttributes> links_withinRadius = Metro_NetworkImpl.findLinksWithinBounds(
				processedLinkMap, originalNetwork, zurich_NetworkCenterCoord, minMetroRadiusFromCenter,
				maxMetroRadiusFromCenter, "zurich_1pm/Metro/Input/Generated_Networks/1_zurich_network_WithinRadius"
						+ ((int) Math.round(metroCityRadius)) + ".xml"); // find most frequent links from all network links

		// Find most frequent links from input links
		int nMostFrequentLinks = 250;	// default 100 for reasonable results
		Map<Id<Link>, CustomLinkAttributes> links_mostFrequentInRadius = 
				Metro_NetworkImpl.findMostFrequentLinks(nMostFrequentLinks, links_withinRadius, originalNetwork, null);

		// Set dominant transit stop facility in given network (from custom link list)
		Map<Id<Link>, CustomLinkAttributes> links_mostFrequentInRadiusMainFacilitiesSet = Metro_NetworkImpl.setMainFacilities(originalTransitSchedule, 
						originalNetwork, links_mostFrequentInRadius, "zurich_1pm/Metro/Input/Generated_Networks/2_zurich_network_MostFrequentInRadius.xml");

		// Select all metro terminal candidates by setting bounds on their location (distance from city center)
		double minTerminalRadiusFromCenter = metroCityRadius * 0.67; 	// default 0.67
		double maxTerminalRadiusFromCenter = metroCityRadius * 4.00;	// default 1.67
		Map<Id<Link>, CustomLinkAttributes> links_MetroTerminalCandidates = Metro_NetworkImpl.findLinksWithinBounds(links_mostFrequentInRadiusMainFacilitiesSet, 
				originalNetwork, zurich_NetworkCenterCoord, minTerminalRadiusFromCenter, maxTerminalRadiusFromCenter, "zurich_1pm/Metro/Input/Generated_Networks/3_zurich_network_MetroTerminalCandidate.xml"); // find most frequent links
		
		
		// Create a metro network from candidate links/stopFaiclities
		double maxNewMetroLinkDistance = 0.80 * metroCityRadius; 	
		Network metroNetwork = Metro_NetworkImpl.createMetroNetworkFromCandidates(
				links_mostFrequentInRadiusMainFacilitiesSet, maxNewMetroLinkDistance, originalNetwork,
				"zurich_1pm/Metro/Input/Generated_Networks/4_zurich_network_MetroNetwork.xml");
		
		
				// %%% Everything in old system up to here %%% //
				// CONVERSIONS:
				// 	Get [new map] node from [old map] refLink: Node newMapNode = newNetwork.getNodes.get(Id.createNodeId("MetroNodeLinkRef_"+oldMapRefLink.toString()))
				// 	---> Id<Node> metroNodeId = metroNodeFromOriginalLink(Id<Link> originalLinkRefID) 
				// 	Get [old map] refLink from [new map] node: Link oldMapLink = newMapNode.parse
				// 	---> Id<Link> originalLinkId = orginalLinkFromMetroNode(Id<Node> metroNodeId)

		
		int nRoutes = 20;
		double minTerminalDistance = 2.80 * metroCityRadius;
		//ArrayList<NetworkRoute> initialMetroRoutes = Metro_NetworkImpl.createInitialRoutes(metroNetwork,
		//		links_MetroTerminalCandidates, nRoutes, minTerminalDistance, "zurich_1pm/Metro/Input/Generated_Networks/5_zurich_network_MetroInitialRoutes.xml");
		
		double xOffset = 1733436; 	// add this to QGis to get MATSim		// Right upper corner of Z�risee -- X_QGis=950040; X_MATSim= 2683476;
		double yOffset = -4748525;	// add this to QGis to get MATSim		// Right upper corner of Z�risee -- Y_QGis=5995336; Y_MATSim= 1246811;
		ArrayList<NetworkRoute> initialMetroRoutes = OD_ProcessorImpl.createInitialRoutes(originalNetwork,
					links_mostFrequentInRadiusMainFacilitiesSet, nRoutes, minTerminalRadiusFromCenter, maxTerminalRadiusFromCenter, zurich_NetworkCenterCoord, 
					"zurich_1pm/Metro/Input/Generated_Networks/5_zurich_network_MetroInitialRoutes.xml",
					"zurich_1pm/Metro/Input/Data/OD_Input/DemandPT2013.csv",										
					"zurich_1pm/Metro/Input/Data/OD_Input/OD_EssentialData_open.csv", xOffset, yOffset);	// CAUTION: Make sure .csv is separated by semi-colon because location names also include commas sometimes and lead to failure!!
		Network separateRoutesNetwork = Metro_NetworkImpl.networkRoutesToNetwork(initialMetroRoutes, metroNetwork, Sets.newHashSet("pt"), "zurich_1pm/Metro/Input/Generated_Networks/5_zurich_network_MetroInitialRoutes.xml");
		
		Network newMetroNetwork = 
		Map<Id<Link>, CustomLinkAttributes> metroCandidateLinks, int nRoutes, double minRadius, double maxRadius, Coord cityCenterCoord,
		String fileName, String csvFileODValues, String csvFileODLocations, double xOffset, double yOffset) {
	// TODO Auto-generated method stub
	
	ArrayList<NetworkRoute> networkRouteArray = new ArrayList<NetworkRoute>();
			
	Node[][] OD_Terminals = findODPairs(newMetroNetwork, csvFileODValues, csvFileODLocations, minRadius, maxRadius, cityCenterCoord, 
			metroCandidateLinks, nRoutes, xOffset, yOffset);
	
	int n = 0;
	OuterNetworkRouteLoop:
	while (networkRouteArray.size() < nRoutes) {
		Id<Node> terminalNode1 = OD_Terminals[n][0].getId();
		Id<Node> terminalNode2 = OD_Terminals[n][1].getId();			
		
		// Find Djikstra --> nodeList
		ArrayList<Node> nodeList = DijkstraOwn_I.findShortestPathVirtualNetwork(newMetroNetwork, terminalNode1, terminalNode2);
		if (nodeList == null) {
				System.out.println("Oops, no shortest path available. Trying to create next networkRoute. Please lower minTerminalDistance"
						+ " ,or increase maxNewMetroLinkDistance (and - last - increase nMostFrequentLinks if required)!");
				continue OuterNetworkRouteLoop;
		}
		List<Id<Link>> linkList = Metro_NetworkImpl.nodeListToNetworkLinkList(newMetroNetwork, nodeList);
		NetworkRoute networkRoute = RouteUtils.createNetworkRoute(linkList, newMetroNetwork);
		networkRouteArray.add(networkRoute);
		System.out.println("The new networkRoute is: [Length="+(networkRoute.getLinkIds().size()+2)+"] - " + networkRoute.toString());
		n++;
	}
	
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		// %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
		
		// Load & Create Schedules and Factories
		Config metroConfig = ConfigUtils.createConfig();								// this is totally default and may be modified as required
		Scenario metroScenario = ScenarioUtils.createScenario(metroConfig);
		TransitSchedule metroSchedule = metroScenario.getTransitSchedule();
		TransitScheduleFactory metroScheduleFactory = metroSchedule.getFactory();
		
		// Create a New Metro Vehicle
		String vehicleTypeName = "metro";  double vehicleLength = 50;  double maxVelocity = 80/3.6;	 int vehicleSeats = 100; int vehicleStandingRoom = 100;
		VehicleType metroVehicleType = Metro_TransitScheduleImpl.createNewVehicleType(vehicleTypeName, vehicleLength, maxVelocity, vehicleSeats, vehicleStandingRoom);
		metroScenario.getTransitVehicles().addVehicleType(metroVehicleType);
		
		// Generate TransitLines and Schedules on NetworkRoutes --> Add to Transit Schedule
		int nTransitLines = initialMetroRoutes.size();
		for(int lineNr=1; lineNr<=nTransitLines; lineNr++) {		
		
			// networkRoute
			NetworkRoute metroNetworkRoute = initialMetroRoutes.get(lineNr-1);
			
			// Create an array of stops along new networkRoute on the center of each of its individual links
				String defaultPtMode = "metro";  boolean blocksLane = false;  double stopTime = 30.0; /*stopDuration [s];*/  double maxVehicleSpeed = 200/3.6;  /*[m/s]*/
				List<TransitRouteStop> stopArray = Metro_TransitScheduleImpl.createAndAddNetworkRouteStops(
					metroSchedule, metroNetwork, metroNetworkRoute, defaultPtMode, stopTime, maxVehicleSpeed, blocksLane);
			
			// Build TransitRoute from stops and NetworkRoute --> and add departures
				double tFirstDep = 6.0*60*60;  double tLastDep = 20.5*60*60;  double depSpacing = 15*60;  int nDepartures = (int) ((tLastDep-tFirstDep)/depSpacing);
				String vehicleFileLocation = "zurich_1pm/Metro/Input/Generated_PT_Files/Vehicles.xml";
			TransitRoute transitRoute = metroScheduleFactory.createTransitRoute(Id.create("TransitRoute_LineNr"+lineNr, TransitRoute.class ), metroNetworkRoute, stopArray, defaultPtMode);
			transitRoute = Metro_TransitScheduleImpl.addDeparturesAndVehiclesToTransitRoute(metroScenario, metroSchedule, transitRoute, nDepartures, tFirstDep, depSpacing, metroVehicleType, vehicleFileLocation); // Add (nDepartures) departures to TransitRoute
						
			// Build TransitLine from TrasitRoute
			TransitLine transitLine = metroScheduleFactory.createTransitLine(Id.create("TransitLine_Nr"+lineNr, TransitLine.class));
			transitLine.addRoute(transitRoute);
			
			// Add new line to schedule
			metroSchedule.addTransitLine(transitLine);			

		}	// end of TransitLine creator loop

		// Write TransitSchedule to corresponding file
		TransitScheduleWriter tsw = new TransitScheduleWriter(metroSchedule);
		tsw.writeFile("zurich_1pm/Metro/Input/Generated_PT_Files/MetroSchedule.xml");
				
		Network mergedNework = Metro_TransitScheduleImpl.mergeRoutesNetworkToOriginalNetwork(separateRoutesNetwork, originalNetwork, Sets.newHashSet("pt"), "zurich_1pm/Metro/Input/Generated_Networks/MergedNetwork.xml");
		TransitSchedule mergedTransitSchedule = Metro_TransitScheduleImpl.mergeAndWriteTransitSchedules(metroSchedule, originalTransitSchedule, "zurich_1pm/Metro/Input/Generated_PT_Files/MergedSchedule.xml");
		Vehicles mergedVehicles = Metro_TransitScheduleImpl.mergeAndWriteVehicles(metroScenario.getTransitVehicles(), originalScenario.getTransitVehicles(), "zurich_1pm/Metro/Input/Generated_PT_Files/MergedVehicles.xml");
		
		// --------------------------------------------------------------------------------------------------------------
		
	}

}
