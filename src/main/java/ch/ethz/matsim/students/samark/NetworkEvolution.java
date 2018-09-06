package ch.ethz.matsim.students.samark;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import ch.ethz.matsim.baseline_scenario.config.CommandLine.ConfigurationException;

/* 
 * TODO Most frequent links: Merge close ones from beginning (small walking distance is justifiable for significant increase in networkEfficiency)
 * TODO MUTATIONS in evo-loop
 * TODO Check Theory and Questions for Network Approach Optimization -> IVT
 * TODO Make frequency optimization !
 * TODO Make separate score evolution with actual OverallScore instead of bestAverageTravelTimeThisGeneration
 * TODO !! If a network has not undergone modification (evolution), we can use the same simulationOutput as in the Generation before (as we would just simulate the same network again)
 * TODO Check, where VC fails --> The population is zero from the start (also check event handlers for their naming and if they can be detected by algorithm!) --- VC - also store global network that one can refer to when merging together new routes!
 * TODO Introduce more randomness in MRoutesMerger
 * 
 * OD-OPTIONS: For optimization
 *  - TODO Make outer loop to connect existing routes that suddenly touch!
 *  - Pick best N routes at the end (make more routes at the beginning instead)
 *  - Freeze long enough routes when it comes to making them longer or even increasing their score in order for the other ones to gain more length as well
 *  - Delete routes, which are not long enough
 *  - Decrease internal parameter for trying to add to new routes to existing ones gradually
 * 
 * Compare metro KM to SBahn KMs
 * Total beeline distance in NetworkEvolutionRunSim (mNetwork.mPersonKMdirect = beelinedistances)
 * Make proper IDs to get objects
 * Small todo's in code
 * Make a GeographyProcessor that calculates the OG/UG percentage from given regions
 */

/*
 * NetworkEvolutionImpl-Line 201/1101: Not necessary > test by running without 
 * 		mNetwork.network = mergedNetwork;
		mNetwork.transitSchedule = mergedTransitSchedule;
		mNetwork.vehicles = mergedVehicles;
	NetworkEvolutionImpl-Line 1021: Not necessary > test by running without	
		mnetworkOut1.network = globalNetwork;
		mnetworkOut2.network = globalNetwork;
	
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
		// if desired, process raw existing network for its performance for reference
		// NetworkScoreLog rawNetworkPerformance = NetworkEvolutionRunSim.peoplePlansProcessingStandard("zurich_1pm/Zurich_1pm_SimulationOutput/output_plans.xml.gz", 240);		
		// NetworkEvolutionRunSim.peoplePlansProcessingStandard("zurich_1pm/Zurich_1pm_SimulationOutput/output_plans.xml.gz", 240);
		// NetworkEvolutionRunSim.peoplePlansProcessingStandard("zurich_1pm/Zurich_1pm_SimulationOutput_BACKUP__10/output_plans.xml.gz", 240);
		// NetworkEvolutionRunSim.peoplePlansProcessingStandard("zurich_1pm/Evolution/Population/Network1/Simulation_Output/output_plans.xml.gz", 240);
		
		
	// - Initiate N=16 networks to make a population
		// % Parameters for Population: %
		int populationSize = 4;														// how many networks should be developed in parallel
		String populationName = "evoNetworks";
		int routesPerNetwork = 5;													// how many initial routes should be placed in every network
		String initialRouteType = "Random";											// Options: {"OD","Random"}	-- Choose method to create initial routes [OD=StrongestOriginDestinationShortestPaths, Random=RandomTerminals in outer frame of specified network]
		boolean useOdPairsForInitialRoutes = false;									// For OD also modify as follows: minTerminalRadiusFromCenter = 0.00*metroCityRadius
		if (initialRouteType.equals("OD")) { useOdPairsForInitialRoutes = true; }
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
		double maxMetroRadiusFactor = 1.40;											// give some flexibility by increasing from default 1.00 to 1.20
		double minMetroRadiusFromCenter = metroCityRadius * minMetroRadiusFactor; 	// set default = 0.00 to not restrict metro network in city center
		double maxMetroRadiusFromCenter = metroCityRadius * maxMetroRadiusFactor;	// this is rather large for an inner city network but more realistic to pull inner city network into outer parts to better connect inner/outer city
		int nMostFrequentLinks = 80;												// empirical formula - default 300
		double maxNewMetroLinkDistance = 0.40*metroCityRadius;						// default 0.80*metroCityRadius
		double minTerminalRadiusFromCenter = 0.20*metroCityRadius; 					// CAUTION: 0.00*metroCityRadius; // For OD-Pairs  [0.30*metroCityRadius for RandomRoutes]
		double maxTerminalRadiusFromCenter = maxMetroRadiusFromCenter;				// default = maxMetroRadiusFromCenter
		double minTerminalDistance = 0.70*maxMetroRadiusFromCenter;					// no default yet
		double odConsiderationThreshold = 0.10;										// from which threshold onwards odPairs can be considered for adding to developing routes
		
		// %% Parameters for Vehicles, StopFacilities & Departures %%
		String vehicleTypeName = "metro";  double maxVelocity = 70/3.6 /*[m/s]*/;
		double vehicleLength = 50;  int vehicleSeats = 100; int vehicleStandingRoom = 100;
		double tFirstDep = 6.0*60*60;  double tLastDep = 20.5*60*60;  double depSpacing = 7.5*60;
		int nDepartures = (int) ((tLastDep-tFirstDep)/depSpacing);
		double stopTime = 30.0; /*stopDuration [s];*/  String defaultPtMode = "metro";  boolean blocksLane = false;
		double metroOpsCostPerKM = 1000; double metroConstructionCostPerKmOverground = 1000000; double metroConstructionCostPerKmUnderground = 10000000;
		
		MNetworkPop networkPopulation = new MNetworkPop(populationName, populationSize);			// Initialize population of networks
		for (int N=1; N<=populationSize; N++) {														// Make individual networks one by one in loop
			String thisNewNetworkName = ("Network"+N);												// Name networks by their number [1;populationSize]
			MNetwork mNetwork = NetworkEvolutionImpl.createMNetworkRoutes(							// Make a list of routes that will be added to this network
					thisNewNetworkName, routesPerNetwork, initialRouteType, iterationToReadOriginalNetwork,
					minMetroRadiusFromCenter, maxMetroRadiusFromCenter, zurich_NetworkCenterCoord, metroCityRadius, nMostFrequentLinks,
					maxNewMetroLinkDistance, minTerminalRadiusFromCenter, maxTerminalRadiusFromCenter, minTerminalDistance, odConsiderationThreshold,
					xOffset, yOffset, vehicleTypeName, vehicleLength, maxVelocity, vehicleSeats, vehicleStandingRoom,
					defaultPtMode, blocksLane, stopTime, maxVelocity, tFirstDep, tLastDep, depSpacing, nDepartures,
					metroOpsCostPerKM, metroConstructionCostPerKmOverground, metroConstructionCostPerKmUnderground);
			networkPopulation.addNetwork(mNetwork);
			// For network Evolution log:
			// XMLOps.writeToFileMNetwork(mNetwork, zeroLog +"/"+mNetwork.networkID);
		}
		MNetworkPop latestPopulation = networkPopulation;
			// for isolated code running:
			// XMLOps.writeToFileMNetworkPop(networkPopulation, "zurich_1pm/Evolution/Population/"+networkPopulation.populationId+".xml");
		
		
	
	// EVOLUTIONARY PROCESS
		Config config = ConfigUtils.createConfig();
		config.getModules().get("network").addParam("inputNetworkFile", "zurich_1pm/Evolution/Population/GlobalMetroNetwork.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Network globalNetwork = scenario.getNetwork();
		int nEvolutions = 2;
		double averageTravelTimePerformanceGoal = 40.0;
		MNetwork successfulNetwork = null;
		double successfulAverageTravelTime = 0.0;
		for (int generationNr = 1; generationNr<=nEvolutions; generationNr++) {
			int finalGeneration = generationNr;
			
		// - SIMULATION LOOP:
			int lastIteration = 1; // 1+(generationNr-1)*5; // 1*generationNr;
			MNetworkPop evoNetworksToSimulate = latestPopulation; 
					// for isolated code running:
					// XMLOps.readFromFileMNetworkPop("zurich_1pm/Evolution/Population/"+populationName+".xml");
			for (MNetwork mNetwork : evoNetworksToSimulate.getNetworks().values()) {
				mNetwork.evolutionGeneration = generationNr;
				String initialConfig = "zurich_1pm/zurich_config.xml";
				NetworkEvolutionRunSim.run(args, mNetwork, initialRouteType, initialConfig, lastIteration);
			} // End Network Simulation Loop 
	
		// - EVENTS PROCESSING: 
			int lastEventIteration = lastIteration; // CAUTION: make sure it is not higher than lastIteration above resp. the last simulated iteration!
			MNetworkPop evoNetworksToProcess = evoNetworksToSimulate;  // for isolated code running: MNetworkPop evoNetworksToProcess = XMLOps.readFromFileMNetworkPop("zurich_1pm/Evolution/Population/"+populationName+".xml");
			evoNetworksToProcess = NetworkEvolutionRunSim.runEventsProcessing(evoNetworksToProcess, lastEventIteration);
					// only for isolated code running to store processed performance parameters:
					// XMLOps.writeToFileMNetworkPop(evoNetworksToProcess, "zurich_1pm/Evolution/Population/"+evoNetworksToProcess.populationId+".xml");
			
		// - PLANS PROCESSING:
			MNetworkPop evoNetworksToProcessPlans = evoNetworksToProcess; 	// for isolated code running: XMLOps.readFromFileMNetworkPop("zurich_1pm/Evolution/Population/"+populationName+".xml");
			int maxConsideredTravelTimeInMin = 240;
			evoNetworksToProcessPlans = NetworkEvolutionRunSim.peoplePlansProcessingM(evoNetworksToProcessPlans, maxConsideredTravelTimeInMin);
			
		// - TOTAL SCORE CALCULATOR & HISTORY LOGGER: hand over score to a separate score map for sorting scores	and store most important data of each iteration	
			String historyFileLocation = "zurich_1pm/Evolution/Population/HistoryLog/Generation"+generationNr;
			new File(historyFileLocation).mkdirs();
			Map<String, NetworkScoreLog> networkScoreMap = new HashMap<String, NetworkScoreLog>();
			boolean performanceGoalAccomplished = false;
			for (String networkName : evoNetworksToProcessPlans.getNetworks().keySet()) {
				MNetwork mnetwork = evoNetworksToProcessPlans.getNetworks().get(networkName);
				mnetwork.calculateTotalRouteLength();
				mnetwork.drivenKM = mnetwork.totalRouteLength*(2*nDepartures);
				mnetwork.calculateNetworkScore();		// from internal scoring parameters calculate overall score according to internal function
				if (performanceGoalAccomplished == false) {		// checking whether performance goal achieved
					if (mnetwork.averageTravelTime < averageTravelTimePerformanceGoal) {
						performanceGoalAccomplished = true;
						successfulNetwork = mnetwork;
						successfulAverageTravelTime = mnetwork.averageTravelTime;
					}					
				}
				if (performanceGoalAccomplished == true) {		// this loop is for the case that performance goal is achieved by one network, but in same iteration another network has an even better score
					if (mnetwork.averageTravelTime < successfulAverageTravelTime) {
						successfulAverageTravelTime = mnetwork.averageTravelTime;
						successfulNetwork = mnetwork;
					}				
				}				
				NetworkScoreLog nsl = new NetworkScoreLog();
				nsl.NetworkScore2LogMap(mnetwork);			// copy network parameters to network score log for storing evolution
				networkScoreMap.put(networkName, nsl);		// network score map is finally stored
				System.out.println("Network Stats for Network = " + mnetwork.networkID);			// display most important analyzed data here
				System.out.println("Total Travel Time = " + mnetwork.totalTravelTime);
				System.out.println("Number of Metro Users = " + mnetwork.nMetroUsers);
				System.out.println("Average Travel Time = " + mnetwork.averageTravelTime);
				System.out.println("Total Metro Passengers KM = " + mnetwork.totalMetroPersonKM);
				System.out.println("Total Driven KM = " + mnetwork.drivenKM);
				System.out.println("10/(this.averageTravelTime-60)+ 25*this.totalMetroPersonKM/this.drivenKM = " + 10/(mnetwork.averageTravelTime-60) +" + "+ 25*mnetwork.totalMetroPersonKM/mnetwork.drivenKM);
				System.out.println("Math.exp((this.averageTravelTime-60)/(-100))+Math.exp((this.drivenKM/this.totalMetroPersonKM)/(-100)) = " + Math.exp((mnetwork.averageTravelTime-60)/(-100))+" + "+Math.exp((mnetwork.drivenKM/mnetwork.totalMetroPersonKM)/(-100)));     
				// mnetwork.network = null;		// set to null before storing to file bc would use up too much storage and is not needed (network can be created from other data)
				// CAUTION: Do this for continuous loops! // XMLOps.writeToFileMNetwork(mnetwork, historyFileLocation+"/"+mnetwork.networkID+".xml");
			}
			XMLOps.writeToFile(networkScoreMap, "zurich_1pm/Evolution/Population/networkScoreMap.xml");
			XMLOps.writeToFile(networkScoreMap, historyFileLocation+"/networkScoreMap.xml");
			
		// - SCORE CHECK: If scores are good enough, stop evolution and give out a well-performing network
			if(performanceGoalAccomplished == true) {		// 
				System.out.println("Performance Goal has been achieved in Generation " +finalGeneration+ " by Network "+successfulNetwork.networkID+" at averageTravelTime "+successfulAverageTravelTime);			// display most important analyzed data here			
				break;
			}
			
			// If PerformanceGoal not yet achieved, change routes and network here according to their scores!
			double alpha = 2.0;
			Double pCrossOver = 0.5; // 0.25;
			latestPopulation = NetworkEvolutionImpl.developGeneration(globalNetwork, networkScoreMap, evoNetworksToProcessPlans, populationName, alpha, pCrossOver, metroConstructionCostPerKmOverground, metroConstructionCostPerKmUnderground, metroOpsCostPerKM,
					iterationToReadOriginalNetwork, useOdPairsForInitialRoutes, vehicleTypeName, vehicleLength, maxVelocity, vehicleSeats, vehicleStandingRoom, defaultPtMode, stopTime, blocksLane);
		
			
			// choose by roulette wheel (overall network score) four times two parents to yield four offspring
				// offspring by merging the two networks in all identical node cross-over --> CONSTRAINTS
				// make new schedule for each route with start parameters (take existing and make new for new routes)
			// for every new offspring choose if it shall replace old worst network (p=1/nOffspring) and replace worst one
			// mutation: p=0.15
				// with p=1/3 kill node
				// with p=1/3 
			
			
		}

	// Plot Score Evolution
		int generationsToPlot = nEvolutions;
		NetworkEvolutionImpl.writeChartAverageTravelTimes(generationsToPlot, "zurich_1pm/Evolution/Population/scoreEvolution.png");
			//NetworkEvolutionImpl.writeChartAverageGenerationNetworkAverageTravelTimes(generationsToPlot, "zurich_1pm/Evolution/Population/scoreEvolutionAverageOfGeneration.png");
			//NetworkEvolutionImpl.writeChartBestGenerationNetworkAverageTravelTimes(generationsToPlot, "zurich_1pm/Evolution/Population/scoreEvolutionBestScoreOfGeneration.png");

	
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
