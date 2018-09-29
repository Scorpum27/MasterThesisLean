package ch.ethz.matsim.students.samark;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.VehicleType;

import com.google.common.collect.Sets;

import ch.ethz.matsim.baseline_scenario.transit.routing.DefaultEnrichedTransitRoute;
import ch.ethz.matsim.baseline_scenario.transit.routing.DefaultEnrichedTransitRouteFactory;

public class ThreadCreateNetwork extends Thread{

	String thisNewNetworkName;
	String initialRouteType;
	Coord zurich_NetworkCenterCoord;
	Double maxTerminalRadiusFromCenter;
	Double minTerminalRadiusFromCenter;
	Network metroNetwork;
	String shortestPathStrategy;
	Map<String,CustomStop> allMetroStops;
	Integer initialRoutesPerNetwork;
	Double minTerminalDistance;
	Double odConsiderationThreshold;
	Double xOffset;
	Double yOffset;
	Double initialDepSpacing;
	Double tFirstDep;
	Double tLastDep;
	Map<Id<Link>,CustomMetroLinkAttributes> metroLinkAttributes;
	TransitSchedule metroSchedule;
	String defaultPtMode;
	Double stopTime;
	Double maxVehicleSpeed;
	Boolean blocksLane;
	Scenario newScenario;
	VehicleType metroVehicleType;
	Integer iterationToReadOriginalNetwork;
	Double metroOpsCostPerKM;
	Double metroConstructionCostPerKmOverground;
	Double metroConstructionCostPerKmUnderground;
	Network originalNetwork;
	TransitSchedule originalTransitSchedule;
	Scenario originalScenario;
	MNetworkPop networkPopulation;
	
	public ThreadCreateNetwork(MNetworkPop networkPopulation, Map<Id<Link>,CustomMetroLinkAttributes> metroLinkAttributes, Map<String,CustomStop> allMetroStops,
			VehicleType metroVehicleType, Network metroNetwork, String thisNewNetworkName, int initialRoutesPerNetwork, String initialRouteType, 
			String shortestPathStrategy, int iterationToReadOriginalNetwork, Coord zurich_NetworkCenterCoord, double metroCityRadius,
			double minTerminalRadiusFromCenter, double maxTerminalRadiusFromCenter, double minTerminalDistance, double odConsiderationThreshold, 
			double xOffset, double yOffset, String vehicleTypeName, String defaultPtMode, 
			boolean blocksLane, double stopTime, double maxVehicleSpeed, double tFirstDep, double tLastDep, double initialDepSpacing,
			double metroOpsCostPerKM, double metroConstructionCostPerKmOverground, double metroConstructionCostPerKmUnderground) {
		
		this.thisNewNetworkName = thisNewNetworkName;
		this.initialRouteType = initialRouteType;
		this.zurich_NetworkCenterCoord = zurich_NetworkCenterCoord;
		this.maxTerminalRadiusFromCenter = maxTerminalRadiusFromCenter;		
		this.minTerminalRadiusFromCenter = minTerminalRadiusFromCenter;
		this.metroNetwork = metroNetwork;
		this.shortestPathStrategy = shortestPathStrategy;
		this.allMetroStops = allMetroStops;
		this.initialRoutesPerNetwork = initialRoutesPerNetwork;
		this.minTerminalDistance = minTerminalDistance;
		this.odConsiderationThreshold = odConsiderationThreshold;
		this.xOffset = xOffset;
		this.yOffset = yOffset;
		this.initialDepSpacing = initialDepSpacing;
		this.tFirstDep = tFirstDep;
		this.tLastDep = tLastDep;
		this.metroLinkAttributes = metroLinkAttributes;
		this.defaultPtMode = defaultPtMode;
		this.stopTime = stopTime;
		this.maxVehicleSpeed = maxVehicleSpeed;
		this.blocksLane = blocksLane;
		this.metroVehicleType = metroVehicleType;
		this.iterationToReadOriginalNetwork = iterationToReadOriginalNetwork;
		this.metroOpsCostPerKM = metroOpsCostPerKM;
		this.metroConstructionCostPerKmOverground = metroConstructionCostPerKmOverground;
		this.metroConstructionCostPerKmUnderground = metroConstructionCostPerKmUnderground;		
		this.originalNetwork= originalNetwork;
		this.originalTransitSchedule = originalTransitSchedule;
		this.originalScenario = originalScenario;
		this.networkPopulation = networkPopulation;
	}

	public void run(){
		try {

		// load original scenario
		Config originalConfig = ConfigUtils.loadConfig("zurich_1pm/zurich_config.xml");
		Scenario originalScenario = ScenarioUtils.loadScenario(originalConfig);
		originalScenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DefaultEnrichedTransitRoute.class, new DefaultEnrichedTransitRouteFactory());
		Network originalNetwork = originalScenario.getNetwork();
		TransitSchedule originalTransitSchedule = originalScenario.getTransitSchedule();
			
		// Load & Create Schedules and Factories
		Config newConfig = ConfigUtils.createConfig();						// this is totally default and may be modified as required
		Scenario newScenario = ScenarioUtils.createScenario(newConfig);
		TransitSchedule metroSchedule = newScenario.getTransitSchedule();
			
		// Add a new Metro Vehicle Type
		newScenario.getTransitVehicles().addVehicleType(metroVehicleType);
			
		MNetwork mNetwork = new MNetwork(thisNewNetworkName);
		String mNetworkPath = "zurich_1pm/Evolution/Population/" + thisNewNetworkName;
		new File(mNetworkPath).mkdirs();		
		
		ArrayList<NetworkRoute> initialMetroRoutes = null;
		Network separateRoutesNetwork = null;
		boolean useOdPairsForInitialRoutes = false;
		if (initialRouteType.equals("OD")) { useOdPairsForInitialRoutes = true; }
		if (useOdPairsForInitialRoutes==false) {								
			List<TransitStopFacility> terminalFacilityCandidates = NetworkEvolutionImpl.findFacilitiesWithinBounds(
					mNetworkPath + "/MetroStopFacilities.xml", zurich_NetworkCenterCoord, minTerminalRadiusFromCenter,
					maxTerminalRadiusFromCenter, (mNetworkPath + "/4_MetroTerminalCandidateNodeLocations.xml"));
					// null); // FOR SAVING: replace (null) by (mNetworkPath+"/4_MetroTerminalCandidate.xml"));
				initialMetroRoutes = NetworkEvolutionImpl.createInitialRoutesRandom(metroNetwork, shortestPathStrategy,
						terminalFacilityCandidates, allMetroStops, initialRoutesPerNetwork, minTerminalDistance);
			// CAUTION: If NullPointerException, probably maxTerminalRadius >  metroNetworkRadius
			separateRoutesNetwork = NetworkEvolutionImpl.networkRoutesToNetwork(initialMetroRoutes, metroNetwork,
					Sets.newHashSet("pt"), (mNetworkPath + "/0_MetroInitialRoutes_Random.xml"));
		}
		else if (useOdPairsForInitialRoutes==true) {	
			// Initial Routes OD_Pairs within bounds
			initialMetroRoutes = OD_ProcessorImpl.createInitialRoutesOD(metroNetwork, initialRoutesPerNetwork,
					minTerminalRadiusFromCenter, maxTerminalRadiusFromCenter, odConsiderationThreshold,
					zurich_NetworkCenterCoord, "zurich_1pm/Evolution/Input/Data/OD_Input/Demand2013_PT.csv",
					"zurich_1pm/Evolution/Input/Data/OD_Input/OD_ZoneCodesLocations.csv", xOffset, yOffset);
			// CAUTION: Make sure .csv is separated by semi-colon because location names also include commas sometimes and lead to failure!!
			separateRoutesNetwork = NetworkEvolutionImpl.networkRoutesToNetwork(initialMetroRoutes, metroNetwork,
					Sets.newHashSet("pt"), (mNetworkPath + "/0_MetroInitialRoutes_OD.xml"));
		}
		
		// Generate TransitLines and Schedules on NetworkRoutes --> Add to Transit Schedule
				int nTransitLines = initialMetroRoutes.size();
				for(int lineNr=1; lineNr<=nTransitLines; lineNr++) {
						
					// NetworkRoute
					NetworkRoute metroNetworkRoute = initialMetroRoutes.get(lineNr-1);
					MRoute mRoute = new MRoute(thisNewNetworkName+"_Route"+lineNr);
					mRoute.departureSpacing = initialDepSpacing;
					mRoute.firstDeparture = tFirstDep;
					mRoute.lastDeparture = tLastDep;
					mRoute.setNetworkRoute(metroNetworkRoute);
					mNetwork.addNetworkRoute(mRoute);
					
					// Create an array of stops along new networkRoute on the FromNode of each of its individual links (and ToNode for final terminal)
					// The new network was constructed so that every node had a corresponding stop facility from the original zurich network on it.
					List<TransitRouteStop> stopArray = Metro_TransitScheduleImpl.createAndAddNetworkRouteStops(
							metroLinkAttributes, metroSchedule, metroNetwork, metroNetworkRoute, defaultPtMode, stopTime, maxVehicleSpeed, blocksLane);
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
					TransitRoute transitRoute = metroSchedule.getFactory().createTransitRoute(Id.create(thisNewNetworkName+"_Route"+lineNr, TransitRoute.class ), 
							metroNetworkRoute, stopArray, defaultPtMode);
					
					transitRoute = Metro_TransitScheduleImpl.addDeparturesAndVehiclesToTransitRoute(mRoute, newScenario, metroSchedule, 
							transitRoute, metroVehicleType, vehicleFileLocation); // Add departures to TransitRoute as a function of f=(DepSpacing, First/LastDeparture)
										
					// Build TransitLine from TrasitRoute
					TransitLine transitLine = metroSchedule.getFactory().createTransitLine(Id.create("TransitLine_Nr"+lineNr, TransitLine.class));
					transitLine.addRoute(transitRoute);
					
					// Add new line to schedule
					metroSchedule.addTransitLine(transitLine);

					mRoute.setTransitLine(transitLine);
					mRoute.setLinkList(NetworkEvolutionImpl.NetworkRoute2LinkIdList(metroNetworkRoute));
					mRoute.setNodeList(NetworkEvolutionImpl.NetworkRoute2NodeIdList(metroNetworkRoute, metroNetwork));
					mRoute.setRouteLength(metroNetwork);
					mRoute.setDrivenKM(mRoute.routeLength * mRoute.nDepartures);
					mRoute.constrCost = mRoute.routeLength
							* (metroConstructionCostPerKmOverground * 0.01 * (100 - mRoute.undergroundPercentage)
									+ metroConstructionCostPerKmUnderground * 0.01 * mRoute.undergroundPercentage);
					mRoute.opsCost = mRoute.routeLength * (metroOpsCostPerKM * 0.01 * (100 - mRoute.undergroundPercentage)
							+ 2 * metroOpsCostPerKM * 0.01 * mRoute.undergroundPercentage);
					mRoute.transitScheduleFile = mNetworkPath + "/MetroSchedule.xml";
					mRoute.setEventsFile("zurich_1pm/Zurich_1pm_SimulationOutput/ITERS/it." + iterationToReadOriginalNetwork
							+ "/" + iterationToReadOriginalNetwork + ".events.xml.gz");
					// Log.write(mRoute.routeID + " - Created route: " + "\r\n" + mRoute.linkList.toString());			
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
				//Network mergedNetwork = ...
						Metro_TransitScheduleImpl.mergeRoutesNetworkToOriginalNetwork(separateRoutesNetwork, originalNetwork, Sets.newHashSet("pt"), mergedNetworkFileName);
				//TransitSchedule mergedTransitSchedule = ...
						Metro_TransitScheduleImpl.mergeAndWriteTransitSchedules(metroSchedule, originalTransitSchedule, (mNetworkPath+"/MergedSchedule.xml"));
				//Vehicles mergedVehicles = ...
						Metro_TransitScheduleImpl.mergeAndWriteVehicles(newScenario.getTransitVehicles(), originalScenario.getTransitVehicles(), (mNetworkPath+"/MergedVehicles.xml"));
				
				networkPopulation.addNetwork(mNetwork);
				
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
}
