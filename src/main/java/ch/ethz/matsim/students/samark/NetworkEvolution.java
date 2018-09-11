package ch.ethz.matsim.students.samark;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
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
 * PRIO 
 * TODO MUTATIONS in EvoLoop
 * TODO Make frequency optimization !
 * TODO Extend current SBahn network with constraints
 * TODO Check Theory and Questions for Network Approach Optimization -> IVT
 * TODO Check, where VC fails --> The population is zero from the start (also check event handlers for their naming and if they can be detected by algorithm!) --- VC - also store global network that one can refer to when merging together new routes!
 * 
 * DIVERSE
 *  - TODO What happens if I delete very short links (are they really worth using as feasible links or are they just a product of a crossing bottleneck?)
 *  - Compare metro KM to SBahn KMs
 *  - Total beeline distance in NetworkEvolutionRunSim (mNetwork.mPersonKMdirect = beelinedistances)
 *  - Make proper IDs to get objects
 *  - Make a GeographyProcessor that calculates the OG/UG percentage from given regions
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
	

/* CAUTION & NOTES
 * Run RunScenario First to have simulation output that can be analyzed [up to iterationToReadOriginalNetwork]
 * Parameters: Tune well, may use default
 * For OD: minTerminalRadiusFromCenter = 0.00*metroCityRadius
 * Do not use "noModificationInLastEvolution" if lastIteration changes over evolutions, because this would give another result from the simulations
 *  
 */

	
	public static void main(String[] args) throws ConfigurationException, IOException {
		PrintWriter pw = new PrintWriter("zurich_1pm/Evolution/Population/PopulationEvolutionLog.txt");		pw.close();		// Prepare empty log file for simulation
		
	// INITIALIZATION
	// !! Run RunScenario First to have simulation output that can be analyzed !!
		// if desired, process raw existing network for its performance for reference
		// NetworkScoreLog rawNetworkPerformance = NetworkEvolutionRunSim.peoplePlansProcessingStandard("zurich_1pm/Zurich_1pm_SimulationOutput/output_plans.xml.gz", 240);		
		// NetworkEvolutionRunSim.peoplePlansProcessingStandard("zurich_1pm/Zurich_1pm_SimulationOutput/output_plans.xml.gz", 240);
		// NetworkEvolutionRunSim.peoplePlansProcessingStandard("zurich_1pm/Zurich_1pm_SimulationOutput_BACKUP__10/output_plans.xml.gz", 240);
		// NetworkEvolutionRunSim.peoplePlansProcessingStandard("zurich_1pm/Evolution/Population/Network1/Simulation_Output/output_plans.xml.gz", 240);
		
		
	// - Initiate N=16 networks to make a population
		// % Parameters for Population: %
		int populationSize = 6;														// how many networks should be developed in parallel
		String populationName = "evoNetworks";
		int routesPerNetwork = 5;													// how many initial routes should be placed in every network
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
		double metroCityRadius = 2500; 												// DEFAULT = 2500
		double minMetroRadiusFactor = 0.00;											// DEFAULT = 0.00
		double maxMetroRadiusFactor = 1.40;											// DEFAULT = 1.40: give some flexibility by increasing from 1.00 to 1.40
		double minMetroRadiusFromCenter = metroCityRadius * minMetroRadiusFactor; 	// DEFAULT = set 0.00 to not restrict metro network in city center
		double maxMetroRadiusFromCenter = metroCityRadius * maxMetroRadiusFactor;	// this is rather large for an inner city network but more realistic to pull inner city network into outer parts to better connect inner/outer city
		int nMostFrequentLinks = 80;												// DEFAULT = 70 (will further be reduced during merging procedure for close facilities)
		double maxNewMetroLinkDistance = 0.40*metroCityRadius;						// DEFAULT = 0.40*metroCityRadius
		double minTerminalRadiusFromCenter = 0.20*metroCityRadius; 					// DEFAULT = 0.00*metroCityRadius for OD-Pairs  
																					// DEFAULT = 0.20*metroCityRadius for RandomRoutes
		double maxTerminalRadiusFromCenter = maxMetroRadiusFromCenter;				// DEFAULT = maxMetroRadiusFromCenter
		double minTerminalDistance = 0.70*maxMetroRadiusFromCenter;					// DEFAULT = 0.70*maxMetroRadiusFromCenter
		double proximityRadius = 400;												// DEFAULT = 400  (merge metro stops within 400 meters)
		double odConsiderationThreshold = 0.10;										// DEFAULT = 0.10 (from which threshold onwards odPairs can be considered for adding to developing routes)
		
		// %% Parameters for Vehicles, StopFacilities & Departures %%
		String vehicleTypeName = "metro";  double maxVelocity = 70/3.6 /*[m/s]*/;
		double vehicleLength = 50;  int vehicleSeats = 100; int vehicleStandingRoom = 100;
		double tFirstDep = 6.0*60*60;  double tLastDep = 20.5*60*60;  double depSpacing = 7.5*60;
		int nDepartures = (int) ((tLastDep-tFirstDep)/depSpacing);
		double stopTime = 30.0; /*stopDuration [s];*/  String defaultPtMode = "metro";  boolean blocksLane = false;
		double metroOpsCostPerKM = 1000; double metroConstructionCostPerKmOverground = 1000000; double metroConstructionCostPerKmUnderground = 10000000;
		
		
		Log.write("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%" + "\r\n" + "NETWORK CREATION - START" + "\r\n" + "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
		// Log.write("NETWORK CREATION - START");
		MNetworkPop networkPopulation = new MNetworkPop(populationName, populationSize);			// Initialize population of networks
		for (int N=1; N<=populationSize; N++) {														// Make individual networks one by one in loop
			String thisNewNetworkName = ("Network"+N);												// Name networks by their number [1;populationSize]
			MNetwork mNetwork = NetworkEvolutionImpl.createMNetworkRoutes(							// Make a list of routes that will be added to this network
					thisNewNetworkName, routesPerNetwork, initialRouteType, iterationToReadOriginalNetwork,
					minMetroRadiusFromCenter, maxMetroRadiusFromCenter, zurich_NetworkCenterCoord, metroCityRadius, nMostFrequentLinks,
					maxNewMetroLinkDistance, minTerminalRadiusFromCenter, maxTerminalRadiusFromCenter, minTerminalDistance, proximityRadius, odConsiderationThreshold,
					xOffset, yOffset, vehicleTypeName, vehicleLength, maxVelocity, vehicleSeats, vehicleStandingRoom,
					defaultPtMode, blocksLane, stopTime, maxVelocity, tFirstDep, tLastDep, depSpacing, nDepartures,
					metroOpsCostPerKM, metroConstructionCostPerKmOverground, metroConstructionCostPerKmUnderground);
			networkPopulation.addNetwork(mNetwork);
			networkPopulation.modifiedNetworksInLastEvolution.add(thisNewNetworkName);	// do this so EVO/SIM loop knows to consider these networks for processing
			// For network Evolution log:
			// XMLOps.writeToFileMNetwork(mNetwork, zeroLog +"/"+mNetwork.networkID);
		}
		Log.write("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%" + "\r\n" + "NETWORK CREATION - END" + "\r\n" + "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
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
			Log.write("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%" + "\r\n" + "GENERATION - " + generationNr + " - START" + "\r\n" + "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
			NetworkEvolutionImpl.saveCurrentMRoutes2HistoryLog(latestPopulation, generationNr, globalNetwork);
			int finalGeneration = generationNr;
			
		// - SIMULATION LOOP:
			int lastIteration = 0; // 1+(generationNr-1)*5; // 1*generationNr;
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
			Log.write("EVENTS PROCESSING of GEN"+generationNr+":");
			int lastEventIteration = lastIteration; // CAUTION: make sure it is not higher than lastIteration above resp. the last simulated iteration!
			MNetworkPop evoNetworksToProcess = latestPopulation;  // for isolated code running: MNetworkPop evoNetworksToProcess = XMLOps.readFromFileMNetworkPop("zurich_1pm/Evolution/Population/"+populationName+".xml");
			evoNetworksToProcess = NetworkEvolutionRunSim.runEventsProcessing(evoNetworksToProcess, lastEventIteration);
					// only for isolated code running to store processed performance parameters:
					// XMLOps.writeToFileMNetworkPop(evoNetworksToProcess, "zurich_1pm/Evolution/Population/"+evoNetworksToProcess.populationId+".xml");
			
		// - PLANS PROCESSING:
			Log.write("PLANS PROCESSING of GEN"+generationNr+": >DONE!");
			//MNetworkPop evoNetworksToProcessPlans = evoNetworksToProcess; 	// for isolated code running: XMLOps.readFromFileMNetworkPop("zurich_1pm/Evolution/Population/"+populationName+".xml");
			int maxConsideredTravelTimeInMin = 240;
			latestPopulation = NetworkEvolutionRunSim.peoplePlansProcessingM(latestPopulation, maxConsideredTravelTimeInMin);
			
		// - TOTAL SCORE CALCULATOR & HISTORY LOGGER: hand over score to a separate score map for sorting scores	and store most important data of each iteration	
			Log.write("LOGGING SCORES of GEN"+generationNr+":");
			String historyFileLocation = "zurich_1pm/Evolution/Population/HistoryLog/Generation"+generationNr;
			new File(historyFileLocation).mkdirs();
			Map<String, NetworkScoreLog> networkScoreMap = new HashMap<String, NetworkScoreLog>();
			boolean performanceGoalAccomplished = false;
			for (String networkName : latestPopulation.getNetworks().keySet()) {
				MNetwork mnetwork = latestPopulation.getNetworks().get(networkName);
				if(latestPopulation.modifiedNetworksInLastEvolution.contains(mnetwork.getNetworkID())) {
					mnetwork.calculateTotalRouteLength();
					mnetwork.drivenKM = mnetwork.totalRouteLength*(2*nDepartures);
					mnetwork.calculateNetworkScore();		// from internal scoring parameters calculate overall score according to internal function
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
				Log.writeAndDisplay("   >>> "+mnetwork.networkID+": Total Metro Passengers KM = " + mnetwork.totalMetroPersonKM);
				//Log.writeAndDisplay("   >>> "+mnetwork.networkID+": Average Travel Time = " + mnetwork.averageTravelTime);
				
				// mnetwork.network = null;		// set to null before storing to file bc would use up too much storage and is not needed (network can be created from other data)
				// CAUTION: Do this for continuous loops! // XMLOps.writeToFileMNetwork(mnetwork, historyFileLocation+"/"+mnetwork.networkID+".xml");
			}
			XMLOps.writeToFile(networkScoreMap, "zurich_1pm/Evolution/Population/networkScoreMap.xml");
			XMLOps.writeToFile(networkScoreMap, historyFileLocation+"/networkScoreMap.xml");
			
		// - SCORE CHECK: If scores are good enough, stop evolution and give out a well-performing network
			if(performanceGoalAccomplished == true) {		// 
				System.out.println("Performance Goal has been achieved in Generation " +finalGeneration+ " by Network "+successfulNetwork.networkID+" at averageTravelTime "+successfulAverageTravelTime);			// display most important analyzed data here			
				Log.write("PERFORMANCE GOAL ACHIEVED: in Generation " +finalGeneration+ " by Network "+successfulNetwork.networkID+" at averageTravelTime "+successfulAverageTravelTime);
				break;
			}
			
			Log.write("NEW POPULATION at the end of GEN"+generationNr+":");
			// If PerformanceGoal not yet achieved, change routes and network here according to their scores!
			double alpha = 10.0;		// tunes roulette wheel choice: high alpha (>5) enhances probability to choose a high-score network and decreases probability
										// to choose a weak netwok more than linearly -> linearly would be p_i = Score_i/Score_tot)
			double pCrossOver = 0.5; 	// 0.25;
			double minCrossingDistanceFactorFromRouteEnd = 0.3;
			boolean logEntireRoutes = false;
			double maxCrossingAngle = 110;
			
			latestPopulation = NetworkEvolutionImpl.developGeneration(globalNetwork, networkScoreMap, latestPopulation, populationName, alpha, pCrossOver,
					metroConstructionCostPerKmOverground, metroConstructionCostPerKmUnderground, metroOpsCostPerKM, iterationToReadOriginalNetwork, 
					useOdPairsForInitialRoutes, vehicleTypeName, vehicleLength, maxVelocity, vehicleSeats, vehicleStandingRoom, defaultPtMode, stopTime, blocksLane, 
					logEntireRoutes, minCrossingDistanceFactorFromRouteEnd, maxCrossingAngle);
			
			// choose by roulette wheel (overall network score) four times two parents to yield four offspring
				// offspring by merging the two networks in all identical node cross-over --> CONSTRAINTS
				// make new schedule for each route with start parameters (take existing and make new for new routes)
			// for every new offspring choose if it shall replace old worst network (p=1/nOffspring) and replace worst one
			// mutation: p=0.15
				// with p=1/3 kill node
				// with p=1/3 
		}

	// Plot Score Evolution
		int generationsToPlot = nEvolutions-1;
		NetworkEvolutionImpl.writeChartAverageTravelTimes(generationsToPlot, "zurich_1pm/Evolution/Population/networkTravelTimesEvolution.png");
		NetworkEvolutionImpl.writeChartNetworkScore(generationsToPlot, "zurich_1pm/Evolution/Population/networkScoreEvolution.png");
		
	
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
