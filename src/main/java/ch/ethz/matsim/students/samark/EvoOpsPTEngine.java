package ch.ethz.matsim.students.samark;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
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

public class EvoOpsPTEngine {

	public EvoOpsPTEngine() {
	}

	public static MNetworkPop applyPT(MNetworkPop newPopulation, Network globalNetwork, Map<Id<Link>, CustomMetroLinkAttributes> metroLinkAttributes, String eliteNetwork,
			String vehicleTypeName, double vehicleLength, double maxVelocity, int vehicleSeats, int vehicleStandingRoom, String defaultPtMode,
			double stopTime, boolean blocksLane, boolean useOdPairsForInitialRoutes) throws IOException {
		
		Config originalConfig = ConfigUtils.loadConfig("zurich_1pm/zurich_config.xml");
		Scenario originalScenario = ScenarioUtils.loadScenario(originalConfig);
		originalScenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DefaultEnrichedTransitRoute.class, new DefaultEnrichedTransitRouteFactory());
		Network originalNetwork = originalScenario.getNetwork();
		TransitSchedule originalTransitSchedule = originalScenario.getTransitSchedule();
		
		for (MNetwork mNetwork : newPopulation.networkMap.values()) {
			if (newPopulation.modifiedNetworksInLastEvolution.contains(mNetwork.networkID)==false || mNetwork.networkID == eliteNetwork) {
				continue;
			}
//			Log.write("  > Adding PT to "+ mNetwork.networkID);
			// Transit Schedule Implementations
			Config newConfig = ConfigUtils.createConfig();
			Scenario newScenario = ScenarioUtils.loadScenario(newConfig);
			TransitSchedule metroSchedule = newScenario.getTransitSchedule();
			TransitScheduleFactory metroScheduleFactory = metroSchedule.getFactory();
			// Create a New Metro Vehicle
			VehicleType newVehicleType = Metro_TransitScheduleImpl.createNewVehicleType(vehicleTypeName, vehicleLength, maxVelocity, vehicleSeats, vehicleStandingRoom);
			newScenario.getTransitVehicles().addVehicleType(newVehicleType);
			
			// Generate TransitLines and Schedules on NetworkRoutes --> Add to Transit Schedule
			int lineNr = 0;
			Iterator<Entry<String, MRoute>> mRouteIter = mNetwork.routeMap.entrySet().iterator();
			while (mRouteIter.hasNext()) {
				Entry<String, MRoute> mRouteEntry = mRouteIter.next();
				String mRouteName = mRouteEntry.getKey();
				MRoute mRoute = mRouteEntry.getValue();
				lineNr++;
				// Create an array of stops along new networkRoute on the center of each of its individual links
				List<TransitRouteStop> stopArray = Metro_TransitScheduleImpl.createAndAddNetworkRouteStops(
						metroLinkAttributes, metroSchedule, globalNetwork, mRoute, defaultPtMode, stopTime, maxVelocity, blocksLane);
				if (stopArray == null) {
					Log.write("CAUTION: stopArray was too short (see code for size limits) --> Therefore deleting mRoute = " +mRouteName);
					mRouteIter.remove();
					continue;
				}
				mRoute.roundtripTravelTime = stopArray.get(stopArray.size()-1).getArrivalOffset();
				mRoute.departureSpacing = NetworkEvolutionImpl.depSpacingCalculator(mRoute.vehiclesNr, mRoute.roundtripTravelTime);
				
				// Build TransitRoute from stops and NetworkRoute --> and add departures
				TransitRoute transitRoute = metroScheduleFactory.createTransitRoute(Id.create(mNetwork.networkID+"_Route"+lineNr, TransitRoute.class ), 
						mRoute.networkRoute, stopArray, defaultPtMode);

				String vehicleFileLocation = ("zurich_1pm/Evolution/Population/"+mNetwork.networkID+"/Vehicles.xml");
				// make sure mRoute.nDepartures and mRoute.depSpacing have been updated correctly during modifications
				transitRoute = Metro_TransitScheduleImpl.addDeparturesAndVehiclesToTransitRoute(mRoute, newScenario, metroSchedule, transitRoute,
						newVehicleType, vehicleFileLocation);
				// Build TransitLine from TrasitRoute
				TransitLine transitLine = metroScheduleFactory.createTransitLine(Id.create("TransitLine_Nr" + lineNr, TransitLine.class));
				transitLine.addRoute(transitRoute);
				// Add new line to schedule
				metroSchedule.addTransitLine(transitLine);
				mRoute.setTransitLine(transitLine);
				mRoute.setLinkList(NetworkEvolutionImpl.NetworkRoute2LinkIdList(mRoute.networkRoute));
				mRoute.setNodeList(NetworkEvolutionImpl.NetworkRoute2NodeIdList(mRoute.networkRoute, globalNetwork));
				mRoute.setRouteLength(NetworkEvolutionImpl.NetworkRoute2TotalLength(mRoute.networkRoute, globalNetwork));
				mRoute.setTotalDrivenDist(mRoute.routeLength * mRoute.nDepartures);

			} // end of TransitLine creator loop
		
			// Write TransitSchedule to corresponding file
			TransitScheduleWriter tsw = new TransitScheduleWriter(metroSchedule);
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
			NetworkOperators.networkIntoNetwork(separateRoutesNetwork, Sets.newHashSet("pt"), originalNetwork, mergedNetworkFileName);
			//TransitSchedule mergedTransitSchedule = ...
					Metro_TransitScheduleImpl.mergeAndWriteTransitSchedules(metroSchedule, originalTransitSchedule, ("zurich_1pm/Evolution/Population/"+mNetwork.networkID+"/MergedSchedule.xml"));
			//Vehicles mergedVehicles = ...
					Metro_TransitScheduleImpl.mergeAndWriteVehicles(newScenario.getTransitVehicles(), originalScenario.getTransitVehicles(), ("zurich_1pm/Evolution/Population/"+mNetwork.networkID+"/MergedVehicles.xml"));
		}
		
		return newPopulation;
	}
	
}
