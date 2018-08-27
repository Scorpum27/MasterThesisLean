package ch.ethz.matsim.students.samark;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import ch.ethz.matsim.baseline_scenario.config.CommandLine.ConfigurationException;

/* TODO
 * Make actual evolutionary loop
 * OD initial routes: Make requirement that one terminal is certain distance from network center so that we get longer routes into city!
 * Make transitSchedule for both ways so that same vehicles are used (reverse)
 * Increase performance by not saving entire population, but storing location of separate networks, which can be loaded into population!
 * 
 * CAUTION: Change: [int lastIteration = generationNr*2];
 * Total beeline distance in NetworkEvolutionRunSim (mNetwork.mPersonKMdirect = beelinedistances)
 * Make proper IDs to get objects
 * Small todo's in code
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
	

	public static void main(String[] args) throws ConfigurationException, FileNotFoundException {

	// INITIALIZATION
	// !! Run RunScenario First to have simulation output that can be analyzed !!
	
	// - Initiate N=16 networks to make a population
		// % Parameters for Population: %
		int populationSize = 3;														// how many networks should be developed in parallel
		String populationName = "evoNetworks";
		int routesPerNetwork = 30;													// how many initial routes should be placed in every network
		String initialRouteType = "Random";												// Options: {"OD","Random"}	-- Choose method to create initial routes [OD=StrongestOriginDestinationShortestPaths, Random=RandomTerminals in outer frame of specified network]
																					// For OD also modify as follows: minTerminalRadiusFromCenter = 0.00*metroCityRadius
		int iterationToReadOriginalNetwork = 100;									// TODO simulate originalNetwork up to 1000(?) This is the iteration for the simulation output of the original network
																					// TODO maybe include additional strategy option here for how to make routes e.g. createNetworkRoutes(Strategy, initialRouteType, ...)
		String zeroLog = "zurich_1pm/Evolution/Population/HistoryLog/Generation0";	// Make string and directory to save to file first generation (Generation0)
		new File(zeroLog).mkdirs();
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
		double minTerminalRadiusFromCenter = 0.20*metroCityRadius; 					// CAUTION: 0.00*metroCityRadius; // For OD-Pairs  [0.30*metroCityRadius for RandomRoutes]
		double maxTerminalRadiusFromCenter = maxMetroRadiusFromCenter;				// default = maxMetroRadiusFromCenter
		double minTerminalDistance = 0.70*maxMetroRadiusFromCenter;					// no default yet
		
		// %% Parameters for Vehicles, StopFacilities & Departures %%
		String vehicleTypeName = "metro";  double maxVehicleSpeed = 120/3.6 /*[m/s]*/;
		double vehicleLength = 50;  int vehicleSeats = 100; int vehicleStandingRoom = 100;
		double tFirstDep = 6.0*60*60;  double tLastDep = 20.5*60*60;  double depSpacing = 5.0*60;
		int nDepartures = (int) ((tLastDep-tFirstDep)/depSpacing);
		double stopTime = 30.0; /*stopDuration [s];*/  String defaultPtMode = "metro";  boolean blocksLane = false;
		double metroOpsCostPerKM = 1000; double metroConstructionCostPerKmOverground = 1000000; double metroConstructionCostPerKmUnderground = 10000000;
		
		MNetworkPop networkPopulation = new MNetworkPop(populationName, populationSize);			// Initialize population of networks
		for (int N=1; N<=populationSize; N++) {													// Make individual networks one by one in loop
			String thisNewNetworkName = ("Network"+N);											// Name networks by their number [1;populationSize]
			MNetwork mNetwork = NetworkEvolutionImpl.createMNetworkRoutes(						// Make a list of routes that will be added to this network
					thisNewNetworkName, routesPerNetwork, initialRouteType, iterationToReadOriginalNetwork,
					minMetroRadiusFromCenter, maxMetroRadiusFromCenter, zurich_NetworkCenterCoord, metroCityRadius, nMostFrequentLinks,
					maxNewMetroLinkDistance, minTerminalRadiusFromCenter, maxTerminalRadiusFromCenter, minTerminalDistance,
					xOffset, yOffset, vehicleTypeName, vehicleLength, maxVehicleSpeed, vehicleSeats, vehicleStandingRoom,
					defaultPtMode, blocksLane, stopTime, maxVehicleSpeed, tFirstDep, tLastDep, depSpacing, nDepartures,
					metroOpsCostPerKM, metroConstructionCostPerKmOverground, metroConstructionCostPerKmUnderground);
			XMLOps.writeToFile(mNetwork, "zurich_1pm/Evolution/Population/"+thisNewNetworkName+"/Objects/"+mNetwork.networkID+".xml");
			networkPopulation.addNetwork(mNetwork);
			XMLOps.writeToFile(networkPopulation, "zurich_1pm/Evolution/Population/"+networkPopulation.populationId+".xml");
			// initial data logging for generationNr=0;
			mNetwork.network = null;		// set to null before storing to file bc would use up too much storage and is not needed (network can be created from other data, but make sure to save the complete population above so that simulation loop can retrieve from there)
			XMLOps.writeToFile(mNetwork, zeroLog +"/"+mNetwork.networkID+".xml");
		}
		
		
		
	// EVOLUTIONARY PROCESS
	int nEvolutions = 3;
	double averageTravelTimePerformanceGoal = 73.0;
	MNetwork successfulNetwork = null;
	double successfulAverageTravelTime = 0.0;
	for (int generationNr = 1; generationNr<=nEvolutions; generationNr++) {
		int finalGeneration = generationNr;
		
		// SIMULATION LOOP:
		int lastIteration = generationNr*2;					// CHANGE THIS!
		MNetworkPop evoNetworksToSimulate = XMLOps.readFromFile(new MNetworkPop().getClass(), "zurich_1pm/Evolution/Population/"+populationName+".xml");
		for (MNetwork mNetwork : evoNetworksToSimulate.getNetworks().values()) {
			String initialConfig = "zurich_1pm/zurich_config.xml";
			NetworkEvolutionRunSim.run(args, mNetwork, initialRouteType, initialConfig, lastIteration);
		} // End Network Simulation Loop 

		// - EVENTS PROCESSING:
		int lastEventIteration = lastIteration; // CAUTION: make sure it is not higher than lastIteration above resp. the last simulated iteration!
		MNetworkPop evoNetworksToProcess = XMLOps.readFromFile(new MNetworkPop().getClass(), "zurich_1pm/Evolution/Population/"+populationName+".xml");
		NetworkEvolutionRunSim.runEventsProcessing(evoNetworksToProcess, lastEventIteration);
		XMLOps.writeToFile(evoNetworksToProcess, "zurich_1pm/Evolution/Population/"+evoNetworksToProcess.populationId+".xml");
		// - PLANS PROCESSING:
		MNetworkPop evoNetworksToProcessPlans = XMLOps.readFromFile(new MNetworkPop().getClass(), "zurich_1pm/Evolution/Population/"+populationName+".xml");
		NetworkEvolutionRunSim.peoplePlansProcessing(evoNetworksToProcessPlans);
		// - HISTORY LOGGER: hand over score to a separate score map for sorting scores	and store most important data of each iteration	
		String historyFileLocation = "zurich_1pm/Evolution/Population/HistoryLog/Generation"+generationNr;
		new File(historyFileLocation).mkdirs();
		Map<String, NetworkScoreLog> networkScoreMap = new HashMap<String, NetworkScoreLog>();
		boolean performanceGoalAccomplished = false;
		for (String networkName : evoNetworksToProcessPlans.getNetworks().keySet()) {
			MNetwork mnetwork = evoNetworksToProcessPlans.getNetworks().get(networkName);
			NetworkScoreLog nsl = new NetworkScoreLog();
			nsl.averageTravelTime = mnetwork.averageTravelTime;
			if (performanceGoalAccomplished == false) {
				if (nsl.averageTravelTime < averageTravelTimePerformanceGoal) {
					performanceGoalAccomplished = true;
					successfulNetwork = mnetwork;
					successfulAverageTravelTime = nsl.averageTravelTime;
				}				
			}
			if (performanceGoalAccomplished == true) {
				if (nsl.averageTravelTime < successfulAverageTravelTime) {
					successfulAverageTravelTime = nsl.averageTravelTime;
					successfulNetwork = mnetwork;
				}				
			}
			nsl.stdDeviationTravelTime = mnetwork.stdDeviationTravelTime;
			networkScoreMap.put(networkName, nsl);
			System.out.println("Network Stats: " + mnetwork.networkID);			// display most important analyzed data here
			System.out.println("Average Travel Time = " + mnetwork.averageTravelTime);
			System.out.println("Number of Metro Users = " + mnetwork.nMetroUsers);
			System.out.println("Total Metro Passengers KM = " + mnetwork.totalMetroPersonKM);
			mnetwork.network = null;		// set to null before storing to file bc would use up too much storage and is not needed (network can be created from other data)
			XMLOps.writeToFile(mnetwork, historyFileLocation+"/"+mnetwork.networkID+".xml");
		}
		XMLOps.writeToFile(networkScoreMap, "zurich_1pm/Evolution/Population/networkScoreMap.xml");
		XMLOps.writeToFile(networkScoreMap, historyFileLocation+"/networkScoreMap.xml");
		
		// If scores are good enough, stop evolution and give out a well-performing network
		if(performanceGoalAccomplished == true) {		// 
			System.out.println("Performance Goal has been achieved in Generation " +finalGeneration+ " by Network "+successfulNetwork.networkID+" at averageTravelTime "+successfulAverageTravelTime);			// display most important analyzed data here			
			break;
		}
		
		// If PerformanceGoal not yet achieved, change routes and network here according to their scores!
		
		
	}

	// Plot Score Evolution
	int generationsToPlot = 3;
	//NetworkEvolutionImpl.writeChartAverageGenerationNetworkAverageTravelTimes(generationsToPlot, "zurich_1pm/Evolution/Population/scoreEvolutionAverageOfGeneration.png");
	//NetworkEvolutionImpl.writeChartBestGenerationNetworkAverageTravelTimes(generationsToPlot, "zurich_1pm/Evolution/Population/scoreEvolutionBestScoreOfGeneration.png");
	NetworkEvolutionImpl.writeChartAverageTravelTimes(generationsToPlot, "zurich_1pm/Evolution/Population/scoreEvolution.png");

	
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
		//		- MNetwork (with its MRoutes, but without NetworkFile!)
		//		- Iteration
		//		- ScoreMap for each network (and routes?)
		// --> Simulation loop
		
	
	
	} // end Main Method
} // end NetworkEvolution Class
