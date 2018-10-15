package ch.ethz.matsim.students.samark;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

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
 * TODO SIMULATION: ProportionalRoulette vs. ExponentialRoulette
 * TODO SIMULATION: ModificationProb with Mutation etc...
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
 *  - PlansProcessing: Make nicer with travelTime
 *  - What happens if I delete very short links (are they really worth using as feasible links or are they just a product of a crossing bottleneck?)
 *  - Compare metro KM to SBahn KMs
 *  - Total beeline distance in NetworkEvolutionRunSim (mNetwork.mPersonKMdirect = beelinedistances)
 *  - Make proper IDs to get objects
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
 * CAUTION: Put back in applyMutations jump over eliteNetwork
 * Run RunScenario First to have simulation output that can be analyzed [up to iterationToReadOriginalNetwork]
 * Parameters: Tune well, may use default
 * For OD: minTerminalRadiusFromCenter = 0.00*metroCityRadius
 * Do not use "noModificationInLastEvolution" if lastIteration changes over evolutions, because this would give another result from the simulations
 * minCrossingDistanceFactorFromRouteEnd must have a MINIMUM = 0.25
 * POM INCLUDE:
 * 		<dependency>
			<groupId>org.apache.directory.studio</groupId>
			<artifactId>org.apache.commons.io</artifactId>
			<version>2.4</version>
		</dependency>
 *
 */

	
	
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws ConfigurationException, IOException, InterruptedException {
		PrintWriter pwDefault = new PrintWriter("zurich_1pm/Evolution/Population/LogDefault.txt");	pwDefault.close();	// Prepare empty defaultLog file for run
		PrintWriter pwEvo = new PrintWriter("zurich_1pm/Evolution/Population/LogEvo.txt");	pwEvo.close();				// Prepare empty evoLog file for run
		Log.write("START TIME = "+(new SimpleDateFormat("HH:mm:ss")).format(Calendar.getInstance().getTime()));

	
	// INITIALIZATIon
	// - Initiate N networks to make a population
		// % Parameters for Network Population & Strategy: %
		int populationSize = 2;													// how many networks should be developed in parallel
		String populationName = "evoNetworks";
		int initialRoutesPerNetwork = 5;
		boolean mergeMetroWithRailway = true;
		String shortestPathStrategy = "Dijkstra2";									// Options: {"Dijkstra1","Dijkstra2"} -- Both work nicely.
		String initialRouteType = "Random";											// Options: {"OD","Random"}	-- Choose method to create initial routes 																						[OD=StrongestOriginDestinationShortestPaths, Random=RandomTerminals in outer frame of 																						specified network]
		boolean useOdPairsForInitialRoutes = false;									// For OD also modify as follows: minTerminalRadiusFromCenter = 0.00*metroCityRadius
		if (initialRouteType.equals("OD")) { useOdPairsForInitialRoutes = true; }
		int iterationToReadOriginalNetwork = 100;									// This is the iteration for the simulation output of the original network
		double lifeTime = 40.0;
		
		// %% Parameters for NetworkRoutes %%
		Coord zurich_NetworkCenterCoord = new Coord(2683360.00, 1248100.00);		// default Coord(2683360.00, 1248100.00);  old:(2683000.00, 1247700.00)
		double xOffset = 1733436; 													// add this to QGis to get MATSim		// Right upper corner of Zürisee -- X_QGis=950040; 																					  																						X_MATSim= 2683476;
		double yOffset = -4748525;													// add this to QGis to get MATSim		// Right upper corner of Zürisee -- Y_QGis=5995336; 																						Y_MATSim= 1246811;
		double metroCityRadius = 4000; 												// DEFAULT = 2500
		double minMetroRadiusFactor = 0.00;											// DEFAULT = 0.00
		double maxMetroRadiusFactor = 1.40;											// DEFAULT = 1.40: give some flexibility by increasing from 1.00 to 1.40
		double minMetroRadiusFromCenter = metroCityRadius * minMetroRadiusFactor; 	// DEFAULT = set 0.00 to not restrict metro network in city center
		double maxMetroRadiusFromCenter = metroCityRadius * maxMetroRadiusFactor;	// this is rather large for an inner city network but more realistic to pull inner city network 																						into outer parts to better connect inner/outer city
		double maxExtendedMetroRadiusFromCenter = 2.2*maxMetroRadiusFromCenter;		// DEFAULT = [1,3]*maxMetroRadiusFromCenter; (3 for mergeMetroWithRailway=true, 1 for =false) How 																						far a metro can travel on railwayNetwork
		int nMostFrequentLinks = (int) metroCityRadius/20;							// DEFAULT = 70 (will further be reduced during merging procedure for close facilities)
		double maxNewMetroLinkDistance = Math.max(0.33*metroCityRadius, 1400);		// DEFAULT = 0.40*metroCityRadius
		double minTerminalRadiusFromCenter = 0.20*metroCityRadius; 					// DEFAULT = 0.00*metroCityRadius for OD-Pairs  
																					// DEFAULT = 0.20*metroCityRadius for RandomRoutes
		double maxTerminalRadiusFromCenter = maxExtendedMetroRadiusFromCenter;		// DEFAULT = maxExtendedMetroRadiusFromCenter
		double minTerminalDistance = 0.80*maxMetroRadiusFromCenter;					// DEFAULT = 0.70*maxMetroRadiusFromCenter
		double railway2metroCatchmentArea = 150;									// DEFAULT = 150 or metroProximityRadius/3
		double metro2metroCatchmentArea = 400;										// DEFAULT = 400  (merge metro stops within 400 meters)
		double odConsiderationThreshold = 0.10;										// DEFAULT = 0.10 (from which threshold onwards odPairs can be considered for adding to developing 																						routes)
		
		// %% Parameters for Vehicles, StopFacilities & Departures %%
		String vehicleTypeName = "metro";  double maxVelocity = 80.0/3.6 /*[m/s]*/;
		double vehicleLength = 50;  int vehicleSeats = 100; int vehicleStandingRoom = 100;
		double initialDepSpacing = 10.0*60.0; double tFirstDep = 6.0*60*60;  double tLastDep = 20.5*60*60; 
		double stopTime = 40.0; /*stopDuration [s];*/  String defaultPtMode = "metro";  boolean blocksLane = false;
		
		// %% Parameters Simulation, Events & Plans Processing %%
		int firstGeneration = 1;
		int lastGeneration = 2;
		int lastIterationOriginal = 20;
		int lastIteration = lastIterationOriginal;
		int storeScheduleInterval = 1;	// every X generations the mergedSchedule/Vehicles are saved for continuation of simulation after undesired breakdown

		// %% Parameters Events & Plans Processing, Scores %%
		double averageTravelTimePerformanceGoal = 40.0;
		int maxConsideredTravelTimeInMin = 240;
		double populationFactor = 1000.0;
		
		// %% Parameters Evolution %%
		double alphaXover = 1.3;									// DEFAULT = 1.3; Sensitive param for RouletteWheel-XOverProb Interval=[1.0, 2.0].
																	// The higher, the more strong networks are favored!
		double pCrossOver = 0.45; 									// DEFAULT = 0.35
		double minCrossingDistanceFactorFromRouteEnd = 0.3; 		// DEFAULT = 0.30; MINIMUM = 0.25
		boolean logEntireRoutes = false;
		double maxCrossingAngle = 110; 								// DEFAULT = 110
		double pMutation = 0.35;									// DEFAULT = 0.35; <=0.5, because used rankMethod has meanProbability of 0.5 by nature
		double pBigChange = 0.25;									// DEFAULT = 0.25
		double pSmallChange = 1.0-pBigChange;
		String crossoverRouletteStrategy = "allPositiveProportional";	// Options: allPositiveProportional, rank, tournamentSelection3, logarithmic

		
		Log.write("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%    " + "NETWORK CREATION - START" + "    %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
		MNetworkPop networkPopulation = NetworkEvolutionImpl.createMNetworkRoutes(							// Make a list of routes that will be added to this network
					populationName, populationSize, initialRoutesPerNetwork, initialRouteType, shortestPathStrategy, iterationToReadOriginalNetwork, lastIterationOriginal,
					minMetroRadiusFromCenter, maxMetroRadiusFromCenter, maxExtendedMetroRadiusFromCenter, zurich_NetworkCenterCoord, metroCityRadius, nMostFrequentLinks,
					maxNewMetroLinkDistance, minTerminalRadiusFromCenter, maxTerminalRadiusFromCenter, minTerminalDistance, mergeMetroWithRailway, railway2metroCatchmentArea,
					metro2metroCatchmentArea, odConsiderationThreshold, useOdPairsForInitialRoutes, xOffset, yOffset, populationFactor, vehicleTypeName, vehicleLength, maxVelocity, 
					vehicleSeats, vehicleStandingRoom, defaultPtMode, blocksLane, stopTime, maxVelocity, tFirstDep, tLastDep, initialDepSpacing, lifeTime
					);
		
		MNetworkPop latestPopulation = networkPopulation;
		Map<Id<Link>, CustomMetroLinkAttributes> metroLinkAttributes = new HashMap<Id<Link>, CustomMetroLinkAttributes>();
		metroLinkAttributes.putAll(XMLOps.readFromFile(metroLinkAttributes.getClass(), "zurich_1pm/Evolution/Population/BaseInfrastructure/metroLinkAttributes.xml"));
		List<Map<String, NetworkScoreLog>> networkScoreMaps = new ArrayList<Map<String, NetworkScoreLog>>();
		// Uncomment until here for RECALL
		
		
		// RECALL MODULE
		// - Uncomment "LogCleaner" & "Network Creation"
		// - firstGeneration=generationToRecall
//				int generationToRecall = 3;	// it is recommended to use the Generation before the one that failed in order
//											// to make sure it's data is complete and ready for next clean generation
//				Map<Id<Link>, CustomMetroLinkAttributes> metroLinkAttributes = new HashMap<Id<Link>, CustomMetroLinkAttributes>();
//				List<Map<String, NetworkScoreLog>> networkScoreMaps = new ArrayList<Map<String, NetworkScoreLog>>();
//				MNetworkPop latestPopulation = new MNetworkPop(populationName);
//				NetworkEvolutionRunSim.recallSimulation(latestPopulation, metroLinkAttributes, generationToRecall, networkScoreMaps, 
//														 "evoNetworks", populationSize, initialRoutesPerNetwork);
				
		
	// EVOLUTIONARY PROCESS
		Config config = ConfigUtils.createConfig();
		config.getModules().get("network").addParam("inputNetworkFile", "zurich_1pm/Evolution/Population/BaseInfrastructure/GlobalNetwork.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Network globalNetwork = scenario.getNetwork();

		
		for (int generationNr=firstGeneration; generationNr<=lastGeneration; generationNr++) {
			Log.write("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%    " + "GENERATION - " + generationNr + " - START" + "    %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
			NetworkEvolutionImpl.saveCurrentMRoutes2HistoryLog(latestPopulation, generationNr, globalNetwork, storeScheduleInterval);			
			
			// - SIMULATION LOOP:
		
			Log.write("SIMULATION of GEN"+generationNr+": ("+lastIteration+" iterations)");
			Log.write("  >> A modification has occured for networks: "+latestPopulation.modifiedNetworksInLastEvolution.toString());

			// % Normal approach! (See before 11.10.2018 for alternative threading approaches incl. executorMethod)
			for (MNetwork mNetwork : latestPopulation.getNetworks().values()) {
				if (latestPopulation.modifiedNetworksInLastEvolution.contains(mNetwork.getNetworkID())==false) {
					// must not simulate this loop again, because it has not been changed in last evolution
					// Comment this if lastIteration changes over evolutions !!
					continue;
				}
				mNetwork.evolutionGeneration = generationNr;
				String initialConfig = "zurich_1pm/zurich_config.xml";
				NetworkEvolutionRunSim.run(args, mNetwork, initialRouteType, initialConfig, lastIteration);
			} // End Network Simulation Loop
			Log.write("Completed all MATSim runs.");
			
			
		// - EVENTS PROCESSING: 
			Log.write("EVENTS PROCESSING of GEN"+generationNr+"");
			int lastEventIteration = lastIteration; // CAUTION: make sure it is not higher than lastIteration above resp. the last simulated iteration!
			MNetworkPop evoNetworksToProcess = latestPopulation;
			evoNetworksToProcess = NetworkEvolutionRunSim.runEventsProcessing(evoNetworksToProcess, lastEventIteration, globalNetwork);

		// - PLANS PROCESSING:
			Log.write("PLANS PROCESSING of GEN"+generationNr+"");
			latestPopulation = NetworkEvolutionRunSim.peoplePlansProcessingM(latestPopulation, maxConsideredTravelTimeInMin, lastIterationOriginal, (int) populationFactor);
			
		// - TOTAL SCORE CALCULATOR & HISTORY LOGGER & SCORE CHECK: hand over score to a separate score map for sorting scores	and store most important data of each iteration	
			Log.write("LOGGING SCORES of GEN"+generationNr+":");
			String historyFileLocation = "zurich_1pm/Evolution/Population/HistoryLog/Generation"+generationNr;
			String networkScoreMapGeneralLocation = "zurich_1pm/Evolution/Population/networkScoreMaps.xml";
//			Map<String, NetworkScoreLog> networkScoreMap = new HashMap<String, NetworkScoreLog>();
			boolean performanceGoalAccomplished = NetworkEvolutionImpl.logResults(networkScoreMaps, historyFileLocation, networkScoreMapGeneralLocation, 
					latestPopulation, averageTravelTimePerformanceGoal, generationNr, lastIterationOriginal, populationFactor, globalNetwork, metroLinkAttributes, lifeTime);
			if(performanceGoalAccomplished == true) {		// 
				break;
			}
			
		// - EVOLUTION: If PerformanceGoal not yet achieved, change routes and network here according to their scores!
			Log.write("EVOLUTION at the end of GEN"+generationNr+":");
			Log.writeEvo("%%%    EVOLUTION OF GEN_"+generationNr+"    %%%");

			// Frequency Modification: Redistribute vehicle fleet by scores and stochastics before mutating actual routes
				// - first set all mRoute.nVehicle as a function of current fleet size and stochastics
				// - applyPT: make functions for depSpacing = f(nVehicles, total route length) while total route length = f(linkList or stopArray)
			
			if (generationNr != lastGeneration) {
				latestPopulation = NetworkEvolutionImpl.developGeneration(globalNetwork, metroLinkAttributes, networkScoreMaps.get(generationNr-1),
						latestPopulation, populationName, alphaXover, pCrossOver, crossoverRouletteStrategy,
						useOdPairsForInitialRoutes, vehicleTypeName, vehicleLength, maxVelocity, vehicleSeats, vehicleStandingRoom,
						defaultPtMode, stopTime, blocksLane, logEntireRoutes, minCrossingDistanceFactorFromRouteEnd, maxCrossingAngle,
						zurich_NetworkCenterCoord, lastIterationOriginal, pMutation, pBigChange, pSmallChange);
			}		
			
		}

	// PLOT RESULTS
		int generationsToPlot = lastGeneration;
		NetworkEvolutionImpl.writeChartAverageTravelTimes(generationsToPlot, populationSize, initialRoutesPerNetwork, lastIteration,
				"zurich_1pm/Evolution/Population/networkScoreMaps.xml", "zurich_1pm/Evolution/Population/networkTravelTimesEvo.png");
		NetworkEvolutionImpl.writeChartNetworkScore(generationsToPlot, populationSize, initialRoutesPerNetwork, lastIteration,
				"zurich_1pm/Evolution/Population/networkScoreMaps.xml", "zurich_1pm/Evolution/Population/networkScoreEvo.png");
	
	// LOG GLOBAL SIMULATION PARAMETERS
		PrintWriter pwParams = new PrintWriter("zurich_1pm/Evolution/Population/runParameters.txt");	pwParams.close();	// Prepare empty defaultLog file for run
		Log.write("zurich_1pm/Evolution/Population/runParameters.txt",
				"populationSize="+populationSize  + ";\r\n" + 
				"initialRoutesPerNetwork="+initialRoutesPerNetwork + ";\r\n" + 
				"populationName="+populationName  + ";\r\n" + 
				"mergeMetroWithRailway="+mergeMetroWithRailway  + ";\r\n" + 
				"shortestPathStrategy="+shortestPathStrategy  + ";\r\n" + 
				"initialRouteType="+initialRouteType  + ";\r\n" + 
				"useOdPairsForInitialRoutes="+useOdPairsForInitialRoutes  + ";\r\n" + 
				"iterationToReadOriginalNetwork="+iterationToReadOriginalNetwork  + ";\r\n" + 
				"lifeTime="+lifeTime  + ";\r\n" + 
				"zurich_NetworkCenterCoord="+zurich_NetworkCenterCoord.toString()  + ";\r\n" + 
				"xOffset="+xOffset  + ";\r\n" + 
				"yOffset="+yOffset  + ";\r\n" + 
				"metroCityRadius="+metroCityRadius  + ";\r\n" + 
				"minMetroRadiusFactor="+minMetroRadiusFactor  + ";\r\n" + 
				"maxMetroRadiusFactor="+maxMetroRadiusFactor  + ";\r\n" + 
				"minMetroRadiusFromCenter="+minMetroRadiusFromCenter  + ";\r\n" + 
				"maxMetroRadiusFromCenter="+maxMetroRadiusFromCenter  + ";\r\n" + 
				"maxExtendedMetroRadiusFromCenter="+maxExtendedMetroRadiusFromCenter  + ";\r\n" + 
				"nMostFrequentLinks="+nMostFrequentLinks  + ";\r\n" + 
				"maxNewMetroLinkDistance="+maxNewMetroLinkDistance  + ";\r\n" + 
				"minTerminalRadiusFromCenter="+minTerminalRadiusFromCenter  + ";\r\n" + 
				"maxTerminalRadiusFromCenter="+maxTerminalRadiusFromCenter  + ";\r\n" + 
				"minTerminalDistance="+minTerminalDistance  + ";\r\n" + 
				"railway2metroCatchmentArea="+railway2metroCatchmentArea  + ";\r\n" + 
				"metro2metroCatchmentArea="+metro2metroCatchmentArea  + ";\r\n" + 
				"odConsiderationThreshold="+odConsiderationThreshold  + ";\r\n" + 
				"vehicleTypeName="+vehicleTypeName  + ";\r\n" + 
				"maxVelocity="+maxVelocity  + ";\r\n" + 
				"vehicleLength="+vehicleLength  + ";\r\n" + 
				"vehicleSeats="+vehicleSeats  + ";\r\n" + 
				"vehicleStandingRoom="+vehicleStandingRoom  + ";\r\n" + 
				"initialDepSpacing="+initialDepSpacing  + ";\r\n" + 
				"tFirstDep="+tFirstDep  + ";\r\n" + 
				"tLastDep="+tLastDep  + ";\r\n" + 
				"stopTime="+stopTime  + ";\r\n" + 
				"defaultPtMode="+defaultPtMode  + ";\r\n" + 
				"blocksLane="+blocksLane  + ";\r\n" + 
				"firstGeneration="+firstGeneration  + ";\r\n" + 
				"lastGeneration="+lastGeneration  + ";\r\n" + 
				"lastIterationOriginal="+lastIterationOriginal  + ";\r\n" + 
				"lastIteration="+lastIteration  + ";\r\n" + 
				"storeScheduleInterval="+storeScheduleInterval  + ";\r\n" + 
				"averageTravelTimePerformanceGoal="+averageTravelTimePerformanceGoal  + ";\r\n" + 
				"maxConsideredTravelTimeInMin="+maxConsideredTravelTimeInMin  + ";\r\n" + 
				"populationFactor="+populationFactor  + ";\r\n" + 
				"alphaXover="+alphaXover  + ";\r\n" + 
				"pCrossOver="+pCrossOver  + ";\r\n" + 
				"minCrossingDistanceFactorFromRouteEnd="+minCrossingDistanceFactorFromRouteEnd  + ";\r\n" + 
				"logEntireRoutes="+logEntireRoutes  + ";\r\n" + 
				"maxCrossingAngle="+maxCrossingAngle  + ";\r\n" + 
				"pMutation="+pMutation  + ";\r\n" + 
				"pBigChange="+pBigChange  + ";\r\n" + 
				"pSmallChange="+pSmallChange  + ";\r\n" + 
				"crossoverRouletteStrategy="+crossoverRouletteStrategy);

		Log.write("END TIME = "+(new SimpleDateFormat("HH:mm:ss")).format(Calendar.getInstance().getTime()));
	} // end Main Method

} // end NetworkEvolution Class


// For overview of different modules refer to versions before 11.10.2018
