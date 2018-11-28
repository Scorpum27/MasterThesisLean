package ch.ethz.matsim.students.samark.evo;

import ch.ethz.matsim.students.samark.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;


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
			else if (percentageOfProfitableRoute(mNetwork) >= 2.0/3.0
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
	
}

