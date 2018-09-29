package ch.ethz.matsim.students.samark;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import ch.ethz.matsim.baseline_scenario.config.CommandLine.ConfigurationException;

/* java -Xmx100G -cp samark-0.0.1-SNAPSHOT.jar ch.ethz.matsim.students.samark.NetworkEvolution --model-type tour --fallback-behaviour IGNORE_AGENT
 * 
 * 
 *
 * TODO Make network creation only once! ... and PARALLELIZE the network creation
 * TODO COST MODELS for metro (also wrt other pt modes)
 * TODO Tuning of EvoAlgo's
 * TODO Combining routes procedures when they come close etc...
 * TODO Different Scoring functions (--> Literature as well) (all roulette wheels, proportional choices, metro cost etc.)
 * TODO Run many times frequencyAlgo for finding errors !
 * TODO ABC Algo. as a reference !
 * TODO OD-Route improvements
 * TODO introduce test on Mendl's network!
 * TODO RAIL-Strategy: How to add rails to innercityNetwork:
 * 			- Make it manual
 * 			- Or take a Djikstra from internet
 * DIVERSE
 *  - TODO What happens if I delete very short links (are they really worth using as feasible links or are they just a product of a crossing bottleneck?)
 *  - GlobalNetwork: Does it have to be with zurichPT or can it be only Metro?! --> Try it!
 *  - Compare metro KM to SBahn KMs
 *  - Total beeline distance in NetworkEvolutionRunSim (mNetwork.mPersonKMdirect = beelinedistances)
 *  - Make proper IDs to get objects
 *  - Make a GeographyProcessor that calculates the OG/UG percentage from given regions
 *  - Check, where VC fails --> The population is zero from the start (also check event handlers for their naming and if they can be detected by algorithm!) --- VC - also store global network that one can refer to when merging together new routes!
 *  - Generics: Not all links of a route have a stop: Must mark links with stop and not make a stop for every link!
 * 
 * OD-OPTIONS
 *  - TODO Make outer loop to connect existing routes that suddenly touch!
 *  - Pick best N routes at the end (make more routes at the beginning instead)
 *  - Freeze long enough routes when it comes to making them longer or even increasing their score in order for the other ones to gain more length as well
 *  - Delete routes, which are not long enough
 *  - Decrease internal parameter for trying to add to new routes to existing ones gradually
 * 
 */


public class NetworkEvolution {
	

/* CAUTION & NOTES
 * Run RunScenario First to have simulation output that can be analyzed [up to iterationToReadOriginalNetwork]
 * Parameters: Tune well, may use default
 * For OD: minTerminalRadiusFromCenter = 0.00*metroCityRadius
 * Do not use "noModificationInLastEvolution" if lastIteration changes over evolutions, because this would give another result from the simulations
 * minCrossingDistanceFactorFromRouteEnd must have a MINIMUM = 0.25
 */

	
	public static void main(String[] args) throws ConfigurationException, IOException {
		PrintWriter pwDefault = new PrintWriter("zurich_1pm/Evolution/Population/LogDefault.txt");	pwDefault.close();	// Prepare empty defaultLog file for run
		PrintWriter pwEvo = new PrintWriter("zurich_1pm/Evolution/Population/LogEvo.txt");	pwEvo.close();				// Prepare empty evoLog file for run

		
	// INITIALIZATIon
		// if desired, process raw existing network for its performance for reference : NetworkScoreLog rawNetworkPerformance = NetworkEvolutionRunSim.peoplePlansProcessingStandard("zurich_1pm/Zurich_1pm_SimulationOutput/output_plans.xml.gz", 240);		
		// NetworkEvolutionRunSim.peoplePlansProcessingStandard("zurich_1pm/Zurich_1pm_SimulationOutput/output_plans.xml.gz", 240); or ("zurich_1pm/Zurich_1pm_SimulationOutput_BACKUP__10/output_plans.xml.gz", 240); or ("zurich_1pm/Evolution/Population/Network1/Simulation_Output/output_plans.xml.gz", 240);
	// - Initiate N networks to make a population
		// % Parameters for Population: %
		int populationSize = 2;													// how many networks should be developed in parallel
		String populationName = "evoNetworks";
		int initialRoutesPerNetwork = 5;
		boolean mergeMetroWithRailway = true;
		String shortestPathStrategy = "Dijkstra2";									// Options: {"Dijkstra1","Dijkstra2"} -- Both work nicely.
		String initialRouteType = "Random";											// Options: {"OD","Random"}	-- Choose method to create initial routes [OD=StrongestOriginDestinationShortestPaths, Random=RandomTerminals in outer frame of specified network]
		boolean useOdPairsForInitialRoutes = false;									// For OD also modify as follows: minTerminalRadiusFromCenter = 0.00*metroCityRadius
		if (initialRouteType.equals("OD")) { useOdPairsForInitialRoutes = true; }
		int iterationToReadOriginalNetwork = 100;									// This is the iteration for the simulation output of the original network
		String zeroLog = "zurich_1pm/Evolution/Population/HistoryLog/Generation0";	// Make string and directory to save to file first generation (Generation0)
		new File(zeroLog).mkdirs();
		// %% Parameters for NetworkRoutes %%
		Coord zurich_NetworkCenterCoord = new Coord(2683000.00, 1247700.00);		// default Coord(2683099.3305, 1247442.9076);
		double xOffset = 1733436; 													// add this to QGis to get MATSim		// Right upper corner of Zürisee -- X_QGis=950040; X_MATSim= 2683476;
		double yOffset = -4748525;													// add this to QGis to get MATSim		// Right upper corner of Zürisee -- Y_QGis=5995336; Y_MATSim= 1246811;
		double metroCityRadius = 4000; 												// DEFAULT = 2500
		double minMetroRadiusFactor = 0.00;											// DEFAULT = 0.00
		double maxMetroRadiusFactor = 1.40;											// DEFAULT = 1.40: give some flexibility by increasing from 1.00 to 1.40
		double minMetroRadiusFromCenter = metroCityRadius * minMetroRadiusFactor; 	// DEFAULT = set 0.00 to not restrict metro network in city center
		double maxMetroRadiusFromCenter = metroCityRadius * maxMetroRadiusFactor;	// this is rather large for an inner city network but more realistic to pull inner city network into outer parts to better connect inner/outer city
		double maxExtendedMetroRadiusFromCenter = 1.6*maxMetroRadiusFromCenter;		// DEFAULT = [1,3]*maxMetroRadiusFromCenter; (3 for mergeMetroWithRailway=true, 1 for =false) How far a metro can travel on railwayNetwork
		int nMostFrequentLinks = (int) metroCityRadius/20;							// DEFAULT = 70 (will further be reduced during merging procedure for close facilities)
		double maxNewMetroLinkDistance = Math.max(0.33*metroCityRadius, 1400);		// DEFAULT = 0.40*metroCityRadius
		double minTerminalRadiusFromCenter = 0.00*metroCityRadius; 					// DEFAULT = 0.00*metroCityRadius for OD-Pairs  
																					// DEFAULT = 0.20*metroCityRadius for RandomRoutes (1.50)
		double maxTerminalRadiusFromCenter = maxExtendedMetroRadiusFromCenter;		// DEFAULT = maxExtendedMetroRadiusFromCenter
		double minTerminalDistance = 0.80*maxMetroRadiusFromCenter;					// DEFAULT = 0.70*maxMetroRadiusFromCenter (4.00)
		double railway2metroCatchmentArea = 150;									// DEFAULT 150 or metroProximityRadius/3
		double metro2metroCatchmentArea = 400;										// DEFAULT = 400  (merge metro stops within 400 meters)
		double odConsiderationThreshold = 0.10;										// DEFAULT = 0.10 (from which threshold onwards odPairs can be considered for adding to developing routes)
		
		// %% Parameters for Vehicles, StopFacilities & Departures %%
		String vehicleTypeName = "metro";  double maxVelocity = 80/3.6 /*[m/s]*/;
		double vehicleLength = 50;  int vehicleSeats = 100; int vehicleStandingRoom = 100;
		double initialDepSpacing = 7.5*60.0; double tFirstDep = 6.0*60*60;  double tLastDep = 20.5*60*60; 
		double stopTime = 30.0; /*stopDuration [s];*/  String defaultPtMode = "metro";  boolean blocksLane = false;
		double metroOpsCostPerKM = 1000; double metroConstructionCostPerKmOverground = 1000000; double metroConstructionCostPerKmUnderground = 10000000;
		
		
		Log.write("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%" + "\r\n" + "NETWORK CREATION - START" + "\r\n" + "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
		// Log.write("NETWORK CREATION - START");
		MNetworkPop networkPopulation = new MNetworkPop(populationName, populationSize);			// Initialize population of networks
		Map<Id<Link>, CustomMetroLinkAttributes> metroLinkAttributes = new HashMap<Id<Link>, CustomMetroLinkAttributes>();
		for (int N=1; N<=populationSize; N++) {														// Make individual networks one by one in loop
			String thisNewNetworkName = ("Network"+N);												// Name networks by their number [1;populationSize]
			MNetwork mNetwork = NetworkEvolutionImpl.createMNetworkRoutes(							// Make a list of routes that will be added to this network
					metroLinkAttributes, thisNewNetworkName, initialRoutesPerNetwork, initialRouteType, shortestPathStrategy, iterationToReadOriginalNetwork,
					minMetroRadiusFromCenter, maxMetroRadiusFromCenter, maxExtendedMetroRadiusFromCenter, zurich_NetworkCenterCoord, metroCityRadius, nMostFrequentLinks,
					maxNewMetroLinkDistance, minTerminalRadiusFromCenter, maxTerminalRadiusFromCenter, minTerminalDistance, mergeMetroWithRailway, railway2metroCatchmentArea,
					metro2metroCatchmentArea, odConsiderationThreshold, xOffset, yOffset, vehicleTypeName, vehicleLength, maxVelocity, 
					vehicleSeats, vehicleStandingRoom, defaultPtMode, blocksLane, stopTime, maxVelocity, tFirstDep, tLastDep, initialDepSpacing,
					metroOpsCostPerKM, metroConstructionCostPerKmOverground, metroConstructionCostPerKmUnderground);
			networkPopulation.addNetwork(mNetwork);
			networkPopulation.modifiedNetworksInLastEvolution.add(thisNewNetworkName);	// do this so EVO/SIM loop knows to consider these networks for processing
			// For network Evolution log:
			// XMLOps.writeToFileMNetwork(mNetwork, zeroLog +"/"+mNetwork.networkID);
		}
		XMLOps.writeToFile(metroLinkAttributes, "zurich_1pm/Evolution/Population/MetroLinkAttributes.xml");
		// for isolated code running:
		// XMLOps.writeToFileMNetworkPop(networkPopulation, "zurich_1pm/Evolution/Population/"+networkPopulation.populationId+".xml");
		Log.write("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%" + "\r\n" + "NETWORK CREATION - END" + "\r\n" + "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
		MNetworkPop latestPopulation = networkPopulation;		// Uncomment until here for RECALL

		
		// RECALL MODULE
		// - Uncomment "LogCleaner" & "Network Creation"
		// - firstGeneration=generationToRecall
		//		Map<Id<Link>, CustomMetroLinkAttributes> metroLinkAttributes = new HashMap<Id<Link>, CustomMetroLinkAttributes>();
		//		int generationToRecall = 4;	// it is recommended to use the Generation before the one that failed in order
		//									// to make sure it's data is complete and ready for next clean generation
		//		MNetworkPop latestPopulation = new MNetworkPop(populationName);
		//		NetworkEvolutionRunSim.recallSimulation(latestPopulation, metroLinkAttributes, generationToRecall, "evoNetworks", populationSize, initialRoutesPerNetwork);
		
	// EVOLUTIONARY PROCESS
		Config config = ConfigUtils.createConfig();
		config.getModules().get("network").addParam("inputNetworkFile", "zurich_1pm/Evolution/Population/GlobalNetwork.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Network globalNetwork = scenario.getNetwork();
		
		
		int firstGeneration = 1;
		int lastGeneration = 4;
		
		int lastIteration = 1;
		double averageTravelTimePerformanceGoal = 40.0;
		int storeScheduleInterval = 1;	// every X generations the mergedSchedule/Vehicles are saved for continuation of simulation after undesired breakdown
		for (int generationNr=firstGeneration; generationNr<=lastGeneration; generationNr++) {
			Log.write("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%" + "\r\n" + "GENERATION - " + generationNr + " - START" + "\r\n" + "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
			NetworkEvolutionImpl.saveCurrentMRoutes2HistoryLog(latestPopulation, generationNr, globalNetwork, storeScheduleInterval);			
			
			// - SIMULATION LOOP:
		
			//MNetworkPop evoNetworksToSimulate = latestPopulation;
			Log.write("SIMULATION of GEN"+generationNr+": ("+lastIteration+" iterations)");
			Log.write("  >> A modification has occured for networks: "+latestPopulation.modifiedNetworksInLastEvolution.toString());
					// for isolated code running:
					// XMLOps.readFromFileMNetworkPop("zurich_1pm/Evolution/Population/"+populationName+".xml");
			for (MNetwork mNetwork : latestPopulation.getNetworks().values()) {
				if (latestPopulation.modifiedNetworksInLastEvolution.contains(mNetwork.getNetworkID())==false) {
					continue;		// must not simulate this loop again, because it has not been changed in last evolution
									// Comment this if lastIteration changes over evolutions !!
				}
				mNetwork.evolutionGeneration = generationNr;
				String initialConfig = "zurich_1pm/zurich_config.xml";
				NetworkEvolutionRunSim.run(args, mNetwork, initialRouteType, initialConfig, lastIteration);
			} // End Network Simulation Loop 
	
		// - EVENTS PROCESSING: 
			Log.write("EVENTS PROCESSING of GEN"+generationNr+"");
			int lastEventIteration = lastIteration; // CAUTION: make sure it is not higher than lastIteration above resp. the last simulated iteration!
			MNetworkPop evoNetworksToProcess = latestPopulation;  // for isolated code running: MNetworkPop evoNetworksToProcess = XMLOps.readFromFileMNetworkPop("zurich_1pm/Evolution/Population/"+populationName+".xml");
			evoNetworksToProcess = NetworkEvolutionRunSim.runEventsProcessing(evoNetworksToProcess, lastEventIteration, globalNetwork);
					// only for isolated code running to store processed performance parameters:
					// XMLOps.writeToFileMNetworkPop(evoNetworksToProcess, "zurich_1pm/Evolution/Population/"+evoNetworksToProcess.populationId+".xml");
			
		// - PLANS PROCESSING:
			Log.write("PLANS PROCESSING of GEN"+generationNr+"");
			//MNetworkPop evoNetworksToProcessPlans = evoNetworksToProcess; 	// for isolated code running: XMLOps.readFromFileMNetworkPop("zurich_1pm/Evolution/Population/"+populationName+".xml");
			int maxConsideredTravelTimeInMin = 240;
			latestPopulation = NetworkEvolutionRunSim.peoplePlansProcessingM(latestPopulation, maxConsideredTravelTimeInMin);
			
		// - TOTAL SCORE CALCULATOR & HISTORY LOGGER & SCORE CHECK: hand over score to a separate score map for sorting scores	and store most important data of each iteration	
			Log.write("LOGGING SCORES of GEN"+generationNr+":");
			String historyFileLocation = "zurich_1pm/Evolution/Population/HistoryLog/Generation"+generationNr;
			String networkScoreMapGeneralLocation = "zurich_1pm/Evolution/Population/networkScoreMap.xml";
			Map<String, NetworkScoreLog> networkScoreMap = new HashMap<String, NetworkScoreLog>();
			boolean performanceGoalAccomplished = NetworkEvolutionImpl.logResults(networkScoreMap, historyFileLocation, networkScoreMapGeneralLocation, 
					latestPopulation, averageTravelTimePerformanceGoal, generationNr);
			if(performanceGoalAccomplished == true) {		// 
				break;
			}
			
		// - EVOLUTION: If PerformanceGoal not yet achieved, change routes and network here according to their scores!
			Log.write("EVOLUTION at the end of GEN"+generationNr+":");
			Log.writeEvo("%%%    EVOLUTION OF GEN_"+generationNr+"    %%%");

			// Frequency Modification: Redistribute vehicle fleet by scores and stochastics before mutating actual routes
				// - first set all mRoute.nVehicle as a function of current fleet size and stochastics
				// - applyPT: make functions for depSpacing = f(nVehicles, total route length) while total route length = f(linkList or stopArray)
			
			double alpha = 10.0;											// tunes roulette wheel choice: high alpha (>5) enhances probability to choose a high-score network and decreases probability to choose a weak network more than linearly -> linearly would be p_i = Score_i/Score_tot)
			double pCrossOver = 0.35; 										// DEFAULT = 0.35
			double minCrossingDistanceFactorFromRouteEnd = 0.3; 			// DEFAULT = 0.30; MINIMUM = 0.25
			boolean logEntireRoutes = false;
			double maxCrossingAngle = 110; 									// DEFAULT = 110
			double pMutation = 0.35;										// DEFAULT = 0.35 
			double pBigChange = 0.2;										// DEFAULT = 0.20
			double pSmallChange = 1.0-pBigChange;
			if (generationNr != lastGeneration) {
				latestPopulation = NetworkEvolutionImpl.developGeneration(globalNetwork, metroLinkAttributes, networkScoreMap, latestPopulation, populationName, alpha, pCrossOver,
						metroConstructionCostPerKmOverground, metroConstructionCostPerKmUnderground, metroOpsCostPerKM, iterationToReadOriginalNetwork, 
						useOdPairsForInitialRoutes, vehicleTypeName, vehicleLength, maxVelocity, vehicleSeats, vehicleStandingRoom, defaultPtMode, stopTime, blocksLane, 
						logEntireRoutes, minCrossingDistanceFactorFromRouteEnd, maxCrossingAngle, pMutation, pBigChange, pSmallChange);
			}			
		}

	// PLOT RESULTS
		int generationsToPlot = lastGeneration;
		NetworkEvolutionImpl.writeChartAverageTravelTimes(generationsToPlot, populationSize, initialRoutesPerNetwork, lastIteration, "zurich_1pm/Evolution/Population/networkTravelTimesEvo.png");
		NetworkEvolutionImpl.writeChartNetworkScore(generationsToPlot, populationSize, initialRoutesPerNetwork, lastIteration, "zurich_1pm/Evolution/Population/networkScoreEvo.png");
	
	// LOG GLOBAL SIMULATION PARAMETERS
		Log.write("zurich_1pm/Evolution/Population/parameters.txt",
						"populationSize = "+populationSize  + "\r\n" + "initialRoutesPerNetwork = "+initialRoutesPerNetwork + "\r\n" +
						"mergeMetroWithRailway = "+mergeMetroWithRailway  + "\r\n" + "... etc ...");

	} // end Main Method

} // end NetworkEvolution Class


/* GENETIC STRUCTURE
 * Multiple networks = Population 	= Map<MNetwork.Id, MNetwork> = networkMap
 * Single Network = Chromosome 		= Map<MRoute.Id, MRoute> = routesMap
 * Single Route = Gene				= MRoute
 */

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
// - Frequency & Departure Spacing
//  - Initialization
//		- Set initial Departure Spacing, firstDep & lastDep.
//		- Calculate roundtripDuration
//		- Calculate nVehicles
//		- Fill in vehicles automatically according to addVehiclesAndDepartures
//	- Evolution
//		- Frequency opt: redistribute vehicles according to cost and previous vehicles 
//		--> keep roundtripDuration, set nVehicles by new, set nDep to zero, keep firstDep/lastDep, the rest will be calculated when applyPT!
//		- Crossovers: split vehicles according to average (floor lower nVeh, ceil higher nVeh)
//		- Mutations: make mutation, %%%calculate new total length%%%, keep nVehicles the same
//		- ApplyPT: take nVehicles, first/lastDep --> Calculate DepSpacing and fill in accordingly --> Calculate drivenKM thereafter.
// - Make evolutionary operations
// - Update population
// - Log files to save development
//		- MNetwork (with its MRoutes, but without NetworkFile!)
//		- Iteration
//		- ScoreMap for each network (and routes?)
// --> Simulation loop

