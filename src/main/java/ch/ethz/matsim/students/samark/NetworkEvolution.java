package ch.ethz.matsim.students.samark;

import java.io.File;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.algorithms.TripsToLegsAlgorithm;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.PtConstants;

import ch.ethz.matsim.baseline_scenario.BaselineModule;
import ch.ethz.matsim.baseline_scenario.config.CommandLine;
import ch.ethz.matsim.baseline_scenario.config.CommandLine.ConfigurationException;
import ch.ethz.matsim.baseline_scenario.traffic.BaselineTrafficModule;
import ch.ethz.matsim.baseline_scenario.transit.BaselineTransitModule;
import ch.ethz.matsim.baseline_scenario.transit.routing.DefaultEnrichedTransitRoute;
import ch.ethz.matsim.baseline_scenario.transit.routing.DefaultEnrichedTransitRouteFactory;
import ch.ethz.matsim.baseline_scenario.zurich.ZurichModule;
import ch.ethz.matsim.papers.mode_choice_paper.CustomModeChoiceModule;
import ch.ethz.matsim.papers.mode_choice_paper.utils.LongPlanFilter;

/* TODO
 * Small todo's in code
 * Make transitSchedule for both ways so that same vehicles are used (reverse)
 * OD initial routes: Make requirement that one terminal is certain distance from network center so that we get longer routes into city!
 * Make a GeographyProcessor that calculates the OG/UG percentage from given regions
 */

public class NetworkEvolution {

/* GENETIC STRUCTURE
 * Multiple networks = Population 	= Map<MNetwork.Id, MNetwork> = networkMap
 * Single Network = Chromosome 		= Map<MRoute.Id, MRoute> = routesMap
 * Single Route = Gene				= MRoute
 */
	
/* FRAMEWORK STRUCTURE
 * This NetworkEvolution manages the POPULATION.
 * - it compares score of each network and processes/develops them accordingly
 * - after modifying population it forwards each network individually to the inner MATSim loop
 * - it analyzes the MATSim events and writes the significant info to the individual networks and routes
 * - Make new class instead of Metro_TransitScheduleImpl for corresponding methods
 */
	
/* NETWORK ANALYSIS
 * double totalTravelTime
 * double personKM;
 * double personKMdirect;
 * int nPassengers
 */
	

	public static void main(String[] args) throws ConfigurationException {

	// INITIALIZATION
	// !! Run RunScenario First to have simulation output that can be analyzed !!
	
	// - Initiate N=16 networks to make a population
		// % Parameters for Population: %
		int populationSize = 3;														// how many networks should be developed in parallel
		int routesPerNetwork = 6;													// how many initial routes should be placed in every network
		String initialRouteType = "Random";										// Options: {"OD","Random"}	-- Choose method to create initial routes [OD=StrongestOriginDestinationShortestPaths, Random=RandomTerminals in outer frame of specified network]
																					// For OD also modify as follows: minTerminalRadiusFromCenter = 0.00*metroCityRadius
		int iterationToReadOriginalNetwork = 100;									// TODO simulate originalNetwork up to 1000(?) This is the iteration for the simulation output of the original network
																					// TODO maybe include additional strategy option here for how to make routes e.g. createNetworkRoutes(Strategy, initialRouteType, ...)
		// %% Parameters for NetworkRoutes %%
		Coord zurich_NetworkCenterCoord = new Coord(2683000.00, 1247700.00);		// default Coord(2683099.3305, 1247442.9076);
		double xOffset = 1733436; 													// add this to QGis to get MATSim		// Right upper corner of Zürisee -- X_QGis=950040; X_MATSim= 2683476;
		double yOffset = -4748525;													// add this to QGis to get MATSim		// Right upper corner of Zürisee -- Y_QGis=5995336; Y_MATSim= 1246811;
		double metroCityRadius = 2500; 												// old 1600.00
		double minMetroRadiusFactor = 0.00;											// default 0.00
		double maxMetroRadiusFactor = 1.20;											// give some flexibility by increasing from default 1.00 to 1.20
		double minMetroRadiusFromCenter = metroCityRadius * minMetroRadiusFactor; 	// set default = 0.00 to not restrict metro network in city center
		double maxMetroRadiusFromCenter = metroCityRadius * maxMetroRadiusFactor;	// this is rather large for an inner city network but more realistic to pull inner city network into outer parts to better connect inner/outer city
		int nMostFrequentLinks = 150;												// empirical formula - default 300
		double maxNewMetroLinkDistance = 0.40*metroCityRadius;						// default 0.80*metroCityRadius
		double minTerminalRadiusFromCenter = 0.30*metroCityRadius; 					// CAUTION: 0.00*metroCityRadius; // For OD-Pairs  [0.30*metroCityRadius for RandomRoutes]
		double maxTerminalRadiusFromCenter = maxMetroRadiusFromCenter;				// default = maxMetroRadiusFromCenter
		double minTerminalDistance = 0.70*maxMetroRadiusFromCenter;					// no default yet
		
		// %% Parameters for Vehicles, StopFacilities & Departures %%
		String vehicleTypeName = "metro";  double maxVehicleSpeed = 100/3.6 /*[m/s]*/;
		double vehicleLength = 50;  int vehicleSeats = 100; int vehicleStandingRoom = 100;
		double tFirstDep = 6.0*60*60;  double tLastDep = 20.5*60*60;  double depSpacing = 3.0*60;
		int nDepartures = (int) ((tLastDep-tFirstDep)/depSpacing);
		double stopTime = 30.0; /*stopDuration [s];*/  String defaultPtMode = "metro";  boolean blocksLane = false;
		double metroOpsCostPerKM = 1000; double metroConstructionCostPerKmOverground = 1000000; double metroConstructionCostPerKmUnderground = 10000000;
		
		MNetworkPop networkPopulation = new MNetworkPop(populationSize);			// Initialize population of networks
		for (int N=1; N<=populationSize; N++) {										// Make individual networks one by one in loop
			String thisNewNetworkName = ("Network"+N);								// Name networks by their number [1;populationSize]
			MNetwork mNetwork = NetworkEvolutionImpl.createMNetworkRoutes(			// Make a list of routes that will be added to this network
					thisNewNetworkName, routesPerNetwork, initialRouteType, iterationToReadOriginalNetwork,
					minMetroRadiusFromCenter, maxMetroRadiusFromCenter, zurich_NetworkCenterCoord, metroCityRadius, nMostFrequentLinks,
					maxNewMetroLinkDistance, minTerminalRadiusFromCenter, maxTerminalRadiusFromCenter, minTerminalDistance,
					xOffset, yOffset, vehicleTypeName, vehicleLength, maxVehicleSpeed, vehicleSeats, vehicleStandingRoom,
					defaultPtMode, blocksLane, stopTime, maxVehicleSpeed, tFirstDep, tLastDep, depSpacing, nDepartures,
					metroOpsCostPerKM, metroConstructionCostPerKmOverground, metroConstructionCostPerKmUnderground);
			networkPopulation.addNetwork(mNetwork);
		}
		
	// EVOLUTIONARY PROCESS
		// SIMULATION LOOP:
		
		for (MNetwork mNetwork : networkPopulation.getNetworks().values()) {
			
			String initialConfig = "zurich_1pm/zurich_config.xml";
			int lastIteration = 3;
			NetworkEvolutionRunSim.run(args, mNetwork, initialRouteType, initialConfig, lastIteration);
			
		} // End Network Simulation Loop 
		
		// - For each network
		// - Simulate network in MATSim with plans
		// - Process events in NetworkPerformanceHandler and save to corresponding network of population
		// - Maybe hand over score to a separate score map for sorting scores		

		
		
	
	// INITIALIZATION
		// - Initiate N=16 networks to make a population
		// - Fill in N=10 fixed Random/OD initial routes into every network
		// - Initialize all MRoutes/MNetworks with the corresponding info (linkList etc.)
		// - TODO Process all routes for their		
			/* - Apply to all routes the calculator
			 * - length
			 * - nVehicles
			 * - nDepartures = nRides
			 * - drivenKM = length * nRides
			 * - undergroundPercentage (at a later stage)
			 */
			
	// EVOLUTIONARY PROCESS
			
		// SIMULATION LOOP:
		// - For each network
		// - Simulate network in MATSim with plans
		// - Process events in NetworkPerformanceHandler and save to corresponding network of population
		// - Maybe hand over score to a separate score map for sorting scores
	
		// EVOLUTION
		// - Make evolutionary operations
		// - Update population
		// - Log files to save development
		// --> Simulation loop
		
	
	
	} // end Main Method
} // end NetworkEvolution Class
