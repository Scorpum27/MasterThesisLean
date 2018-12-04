package ch.ethz.matsim.students.samark.evo;

import ch.ethz.matsim.baseline_scenario.transit.routing.DefaultEnrichedTransitRoute;
import ch.ethz.matsim.baseline_scenario.transit.routing.DefaultEnrichedTransitRouteFactory;
import ch.ethz.matsim.students.samark.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.VehicleType;

import com.google.common.collect.Sets;


public class EvoOpsRoutesAdder {

	public EvoOpsRoutesAdder() {
	}
	
	@SuppressWarnings("unchecked")
	public static void topUpNetworkRouteMaps(Integer initialRoutesPerNetwork, Integer maxRouteNumber, Integer currentGEN, Integer stopUnprofitableRoutesReplacementGEN, MNetworkPop newPopulation,
			Boolean useOdPairsForInitialRoutes, String shortestPathStrategy,
			Double minInitialTerminalDistance, Double minTerminalRadiusFromCenter, Double maxTerminalRadiusFromCenter, 
			Double minInitialTerminalRadiusFromCenter, Double maxInitialTerminalRadiusFromCenter, Double metroCityRadius, Boolean varyInitRouteSize, 
			Double tFirstDep, Double tLastDep,
			MNetwork eliteMNetwork, Double odConsiderationThreshold, Coord zurich_NetworkCenterCoord, Double xOffset, Double yOffset) throws IOException {
		
		if (currentGEN > 100) {
			maxRouteNumber = 6;
		}
		
		Config config = ConfigUtils.createConfig();
		config.getModules().get("network").addParam("inputNetworkFile", "zurich_1pm/Evolution/Population/BaseInfrastructure/TotalMetroNetwork.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Network metroNetwork = scenario.getNetwork();
		
		Map<String, CustomStop> allMetroStops = new HashMap<String, CustomStop>();
		allMetroStops.putAll(XMLOps.readFromFile(allMetroStops.getClass(),"zurich_1pm/Evolution/Population/BaseInfrastructure/metroStopAttributes.xml"));
		
		for (MNetwork mNetwork : newPopulation.networkMap.values()) {
			
			if (mNetwork.networkID.equals(eliteMNetwork.networkID) || newPopulation.modifiedNetworksInLastEvolution.contains(mNetwork.networkID)==false) {
				// make DAMN SURE that all condition in applyPT are placed here as well.
				// Code will fail if routes are topped up, but no PT is applied to them afterwards!
				// make here eliteNetwork exclusion so that it is not topped up if elite, because will not run through applyPT loop then and give empty routes!!!
				continue;
			}
			
			int nRoutesToBeToppedUp; // = nRoutesPerNetwork-mNetwork.routeMap.size();
			// || mNetwork.routeMap.size() >= nRoutesPerNetwork... continue...			
			if (currentGEN < stopUnprofitableRoutesReplacementGEN) {		// no more new routes introductions allowed due to advanced development
				if (mNetwork.routeMap.size() >= initialRoutesPerNetwork) {
					continue;
				}
				else {
					nRoutesToBeToppedUp = initialRoutesPerNetwork - mNetwork.routeMap.size();					
				}
			}
			else if (percentageOfProfitableRoute(mNetwork) >= 0.80
					&& mNetwork.routeMap.size() < maxRouteNumber ) {	// if past replacement generation but enough profitable routes, then add one more route!
				nRoutesToBeToppedUp = 1;
			}
			else {	// if past replacement generation and not enough profitable routes, then do not top up
				continue;
			}
			
			if( ! newPopulation.modifiedNetworksInLastEvolution.contains(mNetwork.networkID) ) {
				newPopulation.modifiedNetworksInLastEvolution.add(mNetwork.networkID);
			}
			
			Log.write("Introducing " +nRoutesToBeToppedUp+ " new routes on " + mNetwork.networkID + ":");
			
			ArrayList<NetworkRoute> newMetroRoutes = new ArrayList<NetworkRoute>();
			if (useOdPairsForInitialRoutes==false) {
				List<TransitStopFacility> terminalFacilityCandidates = new ArrayList<TransitStopFacility>();
				terminalFacilityCandidates = NetworkEvolutionImpl.findFacilitiesWithinBounds("zurich_1pm/Evolution/Population/BaseInfrastructure/MetroStopFacilities.xml",
						zurich_NetworkCenterCoord, minTerminalRadiusFromCenter, maxTerminalRadiusFromCenter, null);
				newMetroRoutes = NetworkEvolutionImpl.createInitialRoutesRandom(metroNetwork, shortestPathStrategy,
						terminalFacilityCandidates, allMetroStops, nRoutesToBeToppedUp, zurich_NetworkCenterCoord, 
						metroCityRadius, varyInitRouteSize, minInitialTerminalDistance,
						minInitialTerminalRadiusFromCenter, maxInitialTerminalRadiusFromCenter);
			}
			else if (useOdPairsForInitialRoutes==true) {
				// Initial Routes OD_Pairs within bounds
				newMetroRoutes = OD_ProcessorImpl.createInitialRoutesOD(metroNetwork, nRoutesToBeToppedUp,
						minTerminalRadiusFromCenter, maxTerminalRadiusFromCenter, odConsiderationThreshold,
						zurich_NetworkCenterCoord, "zurich_1pm/Evolution/Input/Data/OD_Input/Demand2013_PT.csv",
						"zurich_1pm/Evolution/Input/Data/OD_Input/OD_ZoneCodesLocations.csv", xOffset, yOffset);
			}
			
			for(NetworkRoute newMetroRoute : newMetroRoutes) {
				MRoute mRoute = new MRoute();
				for (Integer i=1; i<50; i++) {
					if( ! mNetwork.routeMap.keySet().contains(mNetwork.networkID+"_Route"+i)) {
						mRoute.routeID = mNetwork.networkID+"_Route"+i;
						Log.write("Reintroduced " + mNetwork.networkID+"_Route"+i);
						break;
					}
				}
				mRoute.significantRouteModOccured = true;
				mRoute.isInitialDepartureSpacing = false;
				mRoute.setNetworkRoute(newMetroRoute);
				mRoute.setLinkList(NetworkEvolutionImpl.NetworkRoute2LinkIdList(newMetroRoute));
				mRoute.vehiclesNr = 3;
				mRoute.firstDeparture = tFirstDep;
				mRoute.lastDeparture = tLastDep;
				mNetwork.addNetworkRoute(mRoute);
			}
		}
	}

	public static double percentageOfProfitableRoute(MNetwork mNetwork) {
		double profRoutesPercentage = 0.0;
		for (MRoute mroute : mNetwork.routeMap.values()) {
			if (mroute.utilityBalance > 0.0) {
				profRoutesPercentage += 1.0/mNetwork.routeMap.size();
			}
		}
		return profRoutesPercentage;
	}

	@SuppressWarnings("unchecked")
	public static void topUpNetworkPopulation(int initialRoutesPerNetwork, Integer maxRouteNumber, Integer currentGEN,
			Integer stopUnprofitableRoutesReplacementGEN, MNetworkPop newPopulation, boolean useOdPairsForInitialRoutes,
			String shortestPathStrategy, Double minTerminalDistance, Double minTerminalRadiusFromCenter,
			Double maxTerminalRadiusFromCenter, Double minInitialTerminalRadiusFromCenter,
			Double maxInitialTerminalRadiusFromCenter, Double minInitialTerminalDistance, Double metroCityRadius, Boolean varyInitRouteSize,
			Double tFirstDep, Double tLastDep, MNetwork eliteMNetwork, Double odConsiderationThreshold,
			Coord zurich_NetworkCenterCoord, Double xOffset, Double yOffset, String vehicleTypeName, Double vehicleLength,
			Double maxVelocity, Integer vehicleSeats, Integer vehicleStandingRoom, Double initialDepSpacing, String defaultPtMode,
			Double stopTime, Boolean blocksLane) throws IOException {
		
		Config originalConfig = ConfigUtils.loadConfig("zurich_1pm/zurich_config.xml");
		Scenario originalScenario = ScenarioUtils.loadScenario(originalConfig);
		originalScenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DefaultEnrichedTransitRoute.class, new DefaultEnrichedTransitRouteFactory());
		Network originalNetwork = originalScenario.getNetwork();
		TransitSchedule originalTransitSchedule = originalScenario.getTransitSchedule();
		
		Config tempConfig1 = ConfigUtils.createConfig();
		tempConfig1.getModules().get("network").addParam("inputNetworkFile", "zurich_1pm/Evolution/Population/BaseInfrastructure/TotalMetroNetwork.xml");
		Network metroNetwork = ScenarioUtils.loadScenario(tempConfig1).getNetwork();

		if ( ! new File("zurich_1pm/Evolution/Population/BaseInfrastructure/4_MetroTerminalCandidateFacilities.xml").exists()) {
			NetworkEvolutionImpl.findFacilitiesWithinBounds("zurich_1pm/Evolution/Population/BaseInfrastructure/MetroStopFacilities.xml",
					zurich_NetworkCenterCoord, minTerminalRadiusFromCenter, maxTerminalRadiusFromCenter,
					("zurich_1pm/Evolution/Population/BaseInfrastructure/4_MetroTerminalCandidateFacilities.xml"));
		} // do this when recalling older populations because 4_MetroTerminalCandidateFacilities was renamed (was something like 4_MetroTerminalNodeLocations)
		List<TransitStopFacility> terminalFacilityCandidates = new ArrayList<TransitStopFacility>();
		Config tempConfig2 = ConfigUtils.createConfig();
		tempConfig2.getModules().get("transit").addParam("transitScheduleFile","zurich_1pm/Evolution/Population/BaseInfrastructure/4_MetroTerminalCandidateFacilities.xml");
		terminalFacilityCandidates.addAll(ScenarioUtils.loadScenario(tempConfig2).getTransitSchedule().getFacilities().values());
		
		Map<String, CustomStop> allMetroStops = new HashMap<String, CustomStop>();
		allMetroStops.putAll(XMLOps.readFromFile(allMetroStops.getClass(),  "zurich_1pm/Evolution/Population/BaseInfrastructure/metroStopAttributes.xml"));

		Map<Id<Link>, CustomMetroLinkAttributes> metroLinkAttributes = new HashMap<Id<Link>, CustomMetroLinkAttributes>();
		metroLinkAttributes.putAll(XMLOps.readFromFile(metroLinkAttributes.getClass(), "zurich_1pm/Evolution/Population/BaseInfrastructure/metroLinkAttributes.xml"));

		int populationSize = newPopulation.networkMap.size();
		
		for (int N=1; N<=populationSize; N++) {														// Make individual networks one by one in loop
			
			String thisNetworkName = ("Network"+N);												// Name networks by their number [1;populationSize]
			Double lifeTime = newPopulation.networkMap.get(thisNetworkName).lifeTime;
			if (newPopulation.networkMap.get(thisNetworkName).routeMap.size() > 2 || thisNetworkName.equals(eliteMNetwork.networkID)) {
				continue;
			}
			else {
				Log.write("Replacing  Network = "+thisNetworkName);
				newPopulation.networkMap.remove(thisNetworkName);
				if ( ! newPopulation.modifiedNetworksInLastEvolution.contains(thisNetworkName)) {
					newPopulation.modifiedNetworksInLastEvolution.add(thisNetworkName);
				}
			}
			
			MNetwork mNetwork = new MNetwork(thisNetworkName);
			mNetwork.dominantParent = mNetwork.networkID;
			mNetwork.lifeTime = lifeTime;
			String mNetworkPath = "zurich_1pm/Evolution/Population/"+thisNetworkName;
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
				MRoute mRoute = new MRoute(thisNetworkName+"_Route"+lineNr);
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
						metroLinkAttributes, metroSchedule, metroNetwork, mRoute, defaultPtMode, stopTime, maxVelocity, blocksLane);
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
				TransitRoute transitRoute = metroScheduleFactory.createTransitRoute(Id.create(thisNetworkName+"_Route"+lineNr, TransitRoute.class ), 
						metroNetworkRoute, stopArray, defaultPtMode);
				
				transitRoute = Metro_TransitScheduleImpl.addDeparturesAndVehiclesToTransitRoute(mRoute, newScenario, metroSchedule, 
						transitRoute, metroVehicleType, vehicleFileLocation); // Add departures to TransitRoute as a function of f=(DepSpacing, First/LastDeparture)
									
				// Build TransitLine from TrasitRoute
				TransitLine transitLine = metroScheduleFactory.createTransitLine(Id.create("TransitLine_Nr"+lineNr, TransitLine.class));
				transitLine.addRoute(transitRoute);
				
				// Add new line to schedule
				metroSchedule.addTransitLine(transitLine);
	
				mRoute.setTransitLine(transitLine);
				mRoute.setLinkList(NetworkEvolutionImpl.NetworkRoute2LinkIdList(metroNetworkRoute));
				mRoute.setNodeList(NetworkEvolutionImpl.NetworkRoute2NodeIdList(metroNetworkRoute, metroNetwork));
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
			
			newPopulation.addNetwork(mNetwork);
		}
//		return networkPopulation;
		
	}
	
}

