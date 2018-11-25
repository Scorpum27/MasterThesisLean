package ch.ethz.matsim.students.samark;

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

// java -Xmx100G -cp samark-0.0.1-SNAPSHOT.jar ch.ethz.matsim.students.samark.NetworkEvolution --model-type tour --fallback-behaviour IGNORE_AGENT 10 9 4000 300 50 25 50 1pm 0.35
// java -Xmx100G -cp samark-0.0.1-SNAPSHOT.jar ch.ethz.matsim.students.samark.NetworkEvolution --model-type tour --fallback-behaviour IGNORE_AGENT 10 12 4000 300 50 25 50 1pct 0.2 

// CHANGE SIMULATION_OUTPUT_FOLDER
// CHANGE ZH_FILES

public class VCEvolution {


	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws ConfigurationException, IOException, InterruptedException {
		PrintWriter pwDefault = new PrintWriter("zurich_1pm/Evolution/Population/LogDefault.txt");	// Prepare empty defaultLog file for run
		PrintWriter pwEvo = new PrintWriter("zurich_1pm/Evolution/Population/LogEvo.txt");			// Prepare empty evoLog file for run
		Log.write("START TIME = "+(new SimpleDateFormat("HH:mm:ss")).format(Calendar.getInstance().getTime()));
		
	
	// INITIALIZATION
	// - Initiate N networks to make a population
		// % Parameters for Network Population & Strategy: %
		Integer populationSize = Integer.parseInt(args[4]);						// how many networks should be developed in parallel
		String populationName = "evoNetworks";
		Integer initialRoutesPerNetwork = Integer.parseInt(args[5]);			// DEFAULT = 5;
		Boolean mergeMetroWithRailway = false;
		String ptRemoveScenario = args[12];										// "tram", "bus", "rail", "subway", "funicular"
		Boolean useFastSBahnModule = false;
		Boolean varyInitRouteSize = false;
		Boolean enableThreading = false;
		Integer nThreads = 3;
		Boolean recallSimulation = false;
		int generationToRecall = 8;											// it is recommended to use the Generation before the one that failed in order
																				// to make sure it's data is complete and ready for next clean generation
		String shortestPathStrategy = "Dijkstra2";									// Options: {"Dijkstra1","Dijkstra2"} -- Both work nicely.
		String initialRouteType = "Random";											// Options: {"OD","Random"}	-- Choose method to create initial routes 																						[OD=StrongestOriginDestinationShortestPaths, Random=RandomTerminals in outer frame of 																						specified network]
		Boolean useOdPairsForInitialRoutes = false;									// For OD also modify as follows: minTerminalRadiusFromCenter = 0.00*metroCityRadius
		if (initialRouteType.equals("OD")) { useOdPairsForInitialRoutes = true; }
		Integer iterationToReadOriginalNetwork = 35;								// This is the iteration for the simulation output of the original network
		Double lifeTime = 40.0;
		
		// %% Parameters for NetworkRoutes %%
		Coord zurich_NetworkCenterCoord = new Coord(Double.parseDouble(args[6])/2, Double.parseDouble(args[6])/2);
		Double xOffset = 0.0;
		Double yOffset = 0.0;
		Double metroCityRadius = Double.parseDouble(args[6])/Math.sqrt(2.0);		// DEFAULT = 3000 (OLD=3600) ... (800 for no rail tests)
		Double minMetroRadiusFactor = 0.00;											// DEFAULT = 0.00
		Double maxMetroRadiusFactor = 1.00;											// DEFAULT = 1.70; (OLD=1.40: give some flexibility by increasing from 1.00 to 1.40)
		Double minMetroRadiusFromCenter = metroCityRadius * minMetroRadiusFactor; 	// DEFAULT = set 0.00 to not restrict metro network in city center
		Double maxMetroRadiusFromCenter = metroCityRadius * maxMetroRadiusFactor;	// this is rather large for an inner city network but more realistic to pull inner city network 																						into outer parts to better connect inner/outer city
		Double maxExtendedMetroRadiusFromCenter = 1.0*maxMetroRadiusFromCenter;		// DEFAULT = [1, 2.1]*maxMetroRadiusFromCenter; (2.1 for mergeMetroWithRailway=true, 1 for =false) How 																						far a metro can travel on railwayNetwork
		Integer nMostFrequentLinks = (int) (metroCityRadius/50.0);					// DEFAULT = (int) (metroCityRadius/20.0) (or 70; will further be reduced during merging procedure for close facilities)
//		Double maxNewMetroLinkDistance = 1000.0;									// DEFAULT = Math.max(0.33*metroCityRadius, 1400)
		Double maxNewMetroLinkDistance = 4000.0;									// DEFAULT = Math.max(0.33*metroCityRadius, 1400)

		Double minTerminalRadiusFromCenter = 0.00*metroCityRadius; 					// DEFAULT = 0.00/0.20*metroCityRadius for OD-Pairs/RandomRoutes
		Double maxTerminalRadiusFromCenter = maxExtendedMetroRadiusFromCenter;		// DEFAULT = maxExtendedMetroRadiusFromCenter
		Double minInitialTerminalRadiusFromCenter = 0.40*metroCityRadius; 			// DEFAULT = 0.30*metroCityRadius | put in parameter file and in routes creation file! alt: 0.20*maxExtendedMetroRadiusFromCenter
		Double maxInitialTerminalRadiusFromCenter = 1.00*metroCityRadius;			// DEFAULT = 1.20*metroCityRadius | put in parameter file and in routes creation file! alt: 0.80*maxExtendedMetroRadiusFromCenter
		Double minInitialTerminalDistance = 
		   (minInitialTerminalRadiusFromCenter+maxInitialTerminalRadiusFromCenter); // DEFAULT = minInitialTerminalRadiusFromCenter+maxInitialTerminalRadiusFromCenter (OLD=0.80*maxMetroRadiusFromCenter)
		Double railway2metroCatchmentArea = 150.0;									// DEFAULT = 150 or metroProximityRadius/3
		Double metro2metroCatchmentArea = 400.0;									// DEFAULT = 400  (merge metro stops within 400 meters)
		Double odConsiderationThreshold = 0.10;										// DEFAULT = 0.10 (from which threshold onwards odPairs can be considered for adding to developing 																						routes)
		
		// %% Parameters for Vehicles, StopFacilities & Departures %%
		String vehicleTypeName = "metro";  Double maxVelocity = 75.0/3.6 /*[m/s]*/;
		Double vehicleLength = 200.0;  int vehicleSeats = 100; Integer vehicleStandingRoom = 600;
		Double initialDepSpacing = Double.parseDouble(args[7]);	 Double tFirstDep = 6.0*60*60;  Double tLastDep = 21.5*60*60; 	// DEFAULT: initialDepSpacing = 5.0*60.0;
		Double stopTime = 30.0; /*stopDuration [s];*/  String defaultPtMode = "metro";  boolean blocksLane = false;
		
		// %% Parameters Simulation, Events & Plans Processing %%
		Integer firstGeneration = 1;
		Integer lastGeneration = Integer.parseInt(args[8]);
		Integer lastIterationOriginal = Integer.parseInt(args[10]);	// 45/50
		Integer lastIteration = lastIterationOriginal;
		Integer iterationsToAverage = Integer.parseInt(args[9]);	// 25
		if (lastIterationOriginal < iterationsToAverage || lastIteration < iterationsToAverage)
			{Log.writeAndDisplay(" iterationsToAverage > lastIterationSimulated. Aborting"); System.exit(0);}
		Integer storeScheduleInterval = 1;	// every X generations the mergedSchedule/Vehicles are saved for continuation of simulation after undesired breakdown

		// %% Parameters Events & Plans Processing, Scores %%
		Double averageTravelTimePerformanceGoal = 40.0;
		Integer maxConsideredTravelTimeInSec = 240*60;
		String censusSize = args[11]; // "1pct","1pm"
		Integer populationFactor;	// default 1000 for 1pm scenario 
		if (censusSize.equals("1pct")) { populationFactor = 100; }
		else if (censusSize.equals("1pm")) {populationFactor = 1000;}
		else if (censusSize.equals("3pm")) {populationFactor = 333;}
		else if (censusSize.equals("6pm")) {populationFactor = 167;}
		else if (censusSize.equals("0.6pm")) {populationFactor = 1667;}
		else {populationFactor = 100; Log.writeAndDisplay(" CensusSize invalid. Aborting!"); System.exit(0);}
		// TODO hand over to methods censusSize for to pick correct files folder for initial config, network, people's plans

		// %% Parameters Evolution %%
		Double alphaXover = 1.3;									// DEFAULT = 1.3; Sensitive param for RouletteWheel-XOverProb Interval=[1.0, 2.0].
																	// The higher, the more strong networks are favored!
		Double pCrossOver = 0.14; 									// DEFAULT = 0.14
		Double minCrossingDistanceFactorFromRouteEnd = 0.25; 		// DEFAULT = 0.30; MINIMUM = 0.25
		Double maxConnectingDistance = 2000.0;
		Boolean logEntireRoutes = false;
		Double maxCrossingAngle = 110.0; 							// DEFAULT = 110
		Double pMutation = 0.4;										// pMutation <= (N+1)/(2*N) !!!
		if (pMutation>1.0*(initialRoutesPerNetwork+1)/(2*initialRoutesPerNetwork)) {System.out.println("pMutation too high. Choose lower. Aborting."); System.exit(0);}
		Double pBigChange = 0.30;									// DEFAULT = 0.25
		Double pSmallChange = 1.0-pBigChange;
		String crossoverRouletteStrategy = "tournamentSelection3";	// Options: allPositiveProportional, rank, tournamentSelection3, logarithmic
		Double routeDisutilityLimit = -0.0E7;						// DEFAULT = -1.5E7;
		Integer blockFreqModGENs = 5;
		Integer stopUnprofitableRoutesReplacementGEN = 20;			// DEAFULT TBD; After this generation, a route that dies is not replaced by a newborn!
		
		// %% Infrastructure Parameters %%
		Double globalCostFactor = Double.parseDouble(args[13]);
		final double ConstrCostUGnew = globalCostFactor*1.5E5;								// within UG radius, new rails
		final double ConstrCostUGdevelop = globalCostFactor*2.25E4;							// DEFAULT: 0.25E5 = within UG radius, but existing train rails
		final double ConstrCostOGnew = globalCostFactor*4.0E4;
		final double ConstrCostOGdevelop = globalCostFactor*6.0E3;							// DEFAULT: 1.0E4
		final double ConstrCostOGequip = globalCostFactor*6.0E3;
		final double ConstrCostPerStationNew = globalCostFactor*1.6E5;
		final double ConstrCostPerStationExtend = globalCostFactor*0.1E5;
		final double costVehicle = globalCostFactor*13.0E6;									// x2 because assumed to be replaced once for 40y total lifetime (=2x20y)
		final double OpsCostPerVehDistUG = globalCostFactor*20.5/1000;
		final double OpsCostPerVehDistOG = globalCostFactor*20.5/1000;
		final double occupancyRate = 1.40; 									// personsPerVehicle
		final double ptPassengerCostPerDist = 0.1407/1000; 					// average price/km to buy a ticket for a trip with a certain distance
		final double taxPerVehicleDist = 0.06/1000;
		final double carCostPerVehDist = 0.7/1000; 							//(0.1403 + 0.11 + 0.13 + 0.32)/1000; 		// CHF/vehicleKM generalCost(repair etc.) + fuel + write-off
		final double externalCarCosts = 0.077/1000;  						// CHF/personKM  [noise, pollution, climate, accidents, energy]    OLD:(0.0111 + 0.0179 + 0.008 + 0.03)/1000
		final double externalPtCosts = 0.032/1000;							// CHF/personKM [noise, pollution, climate, accidents] + [energyForInfrastructure]   || OLD: 0.023/1000 + EnergyCost*energyPerPtPersDist;
		final double ptTrafficIncreasePercentage = 0.28; 					// by 2040 --> because we build infra anyways, this is just higher ticket revenue!!
		final double VATPercentage = 0.08;
		final double utilityOfTimePT = 14.43/3600;							// CHF/s
		final double utilityOfTimeCar = 23.29/3600;							// CHF/s
		// store infrastructure parameters here
		XMLOps.writeToFile(new InfrastructureParameters(ConstrCostUGnew, ConstrCostUGdevelop, ConstrCostOGnew, ConstrCostOGdevelop, ConstrCostOGequip,
				ConstrCostPerStationNew, ConstrCostPerStationExtend, costVehicle, OpsCostPerVehDistUG, OpsCostPerVehDistOG, occupancyRate,
				ptPassengerCostPerDist, taxPerVehicleDist, carCostPerVehDist, externalCarCosts, externalPtCosts, ptTrafficIncreasePercentage,
				VATPercentage, utilityOfTimePT, utilityOfTimeCar), "zurich_1pm/Evolution/Population/BaseInfrastructure/infrastructureCost.xml");
		
		MNetworkPop latestPopulation;
		Map<Id<Link>, CustomMetroLinkAttributes> metroLinkAttributes = new HashMap<Id<Link>, CustomMetroLinkAttributes>();
		List<Map<String, NetworkScoreLog>> networkScoreMaps = new ArrayList<Map<String, NetworkScoreLog>>();

		if (!recallSimulation) {
			pwDefault.close();
			pwEvo.close();
			Log.write("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%    " + "NETWORK CREATION - START" + "    %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
			latestPopulation = NetworkEvolutionImpl.createMNetworks(			// XXX				// Make a list of routes that will be added to this network
				populationName, populationSize, initialRoutesPerNetwork, initialRouteType, shortestPathStrategy, iterationToReadOriginalNetwork, lastIterationOriginal,
				iterationsToAverage, 
				minMetroRadiusFromCenter, maxMetroRadiusFromCenter, maxExtendedMetroRadiusFromCenter, zurich_NetworkCenterCoord, metroCityRadius, nMostFrequentLinks,
				maxNewMetroLinkDistance, minTerminalRadiusFromCenter, maxTerminalRadiusFromCenter, minInitialTerminalDistance, 
				minInitialTerminalRadiusFromCenter, maxInitialTerminalRadiusFromCenter, varyInitRouteSize, mergeMetroWithRailway, railway2metroCatchmentArea,
				metro2metroCatchmentArea, odConsiderationThreshold, useOdPairsForInitialRoutes, xOffset, yOffset, 1.0*populationFactor, vehicleTypeName, vehicleLength, maxVelocity, 
				vehicleSeats, vehicleStandingRoom, defaultPtMode, blocksLane, stopTime, maxVelocity, tFirstDep, tLastDep, initialDepSpacing, lifeTime
			);
			metroLinkAttributes.putAll(XMLOps.readFromFile(metroLinkAttributes.getClass(), "zurich_1pm/Evolution/Population/BaseInfrastructure/metroLinkAttributes.xml"));
			List<Map<String, String>> pedigreeTree = new ArrayList<Map<String, String>>();
			XMLOps.writeToFile(pedigreeTree, "zurich_1pm/Evolution/Population/HistoryLog/pedigreeTree.xml");
		}
		else {
			// RECALL MODULE - Uncomment "LogCleaner" & "Network Creation"
			Log.write("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%    " + "RECALL - START" + "    %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
			firstGeneration = generationToRecall;
			latestPopulation = new MNetworkPop(populationName);
			NetworkEvolutionRunSim.recallSimulation(latestPopulation, metroLinkAttributes, generationToRecall, networkScoreMaps, 
					"evoNetworks", populationSize, initialRoutesPerNetwork);			
		}
		
				
		
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
			String initialConfig = "zurich_1pm/zurich_config.xml";
			
			if (enableThreading) {
				ExecutorService executorService = Executors.newFixedThreadPool(nThreads);
				for (MNetwork mNetwork : latestPopulation.getNetworks().values()) {
					if (latestPopulation.modifiedNetworksInLastEvolution.contains(mNetwork.getNetworkID())==false) {continue;}
					mNetwork.evolutionGeneration = generationNr;
					RunnableRunSim MATSimRunnable = new RunnableRunSim(args, mNetwork, initialRouteType, initialConfig, lastIteration, useFastSBahnModule, ptRemoveScenario);
					executorService.execute(MATSimRunnable);
				} // End Network Simulation Loop
				executorService.shutdown();
				try { executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS); } catch (InterruptedException e) {}
			}
			else { // % Normal approach! (See before 11.10.2018 for alternative threading approaches incl. executorMethod)
				for (MNetwork mNetwork : latestPopulation.getNetworks().values()) {
					if (latestPopulation.modifiedNetworksInLastEvolution.contains(mNetwork.getNetworkID())==false) {
						// must not simulate this loop again, because it has not been changed in last evolution
						// Comment this if lastIteration changes over evolutions !!
						continue;
					}
					mNetwork.evolutionGeneration = generationNr;
					NetworkEvolutionRunSim.run(args, mNetwork, initialRouteType, initialConfig, lastIteration, useFastSBahnModule, ptRemoveScenario);
				} // End Network Simulation Loop
			}
			Log.write("Completed all MATSim runs.");
			
		// - EVENTS PROCESSING: 
			Log.write("EVENTS PROCESSING of GEN"+generationNr+"");
			int lastEventIteration = lastIteration; // CAUTION: make sure it is not higher than lastIteration above resp. the last simulated iteration!
			MNetworkPop evoNetworksToProcess = latestPopulation;
			evoNetworksToProcess = NetworkEvolutionRunSim.runEventsProcessing(evoNetworksToProcess, lastEventIteration, iterationsToAverage,
					globalNetwork, "zurich_1pm/Evolution/Population/", populationFactor);

		// - PLANS PROCESSING:
			Log.write("PLANS PROCESSING of GEN"+generationNr+"");
			latestPopulation = NetworkEvolutionRunSim.peoplePlansProcessingM(latestPopulation, maxConsideredTravelTimeInSec,
					lastIterationOriginal, iterationsToAverage, populationFactor, "zurich_1pm/Evolution/Population/");
			
		// - TOTAL SCORE CALCULATOR & HISTORY LOGGER & SCORE CHECK: hand over score to a separate score map for sorting scores	and store most important data of each iteration	
			Log.write("LOGGING SCORES of GEN"+generationNr+":");
			String historyFileLocation = "zurich_1pm/Evolution/Population/HistoryLog/Generation"+generationNr;
			String networkScoreMapGeneralLocation = "zurich_1pm/Evolution/Population/networkScoreMaps.xml";
//			Map<String, NetworkScoreLog> networkScoreMap = new HashMap<String, NetworkScoreLog>();
			boolean performanceGoalAccomplished = NetworkEvolutionImpl.logResults(networkScoreMaps, historyFileLocation, networkScoreMapGeneralLocation, 
					latestPopulation, averageTravelTimePerformanceGoal, generationNr, lastIterationOriginal, 1.0*populationFactor, 
					globalNetwork, metroLinkAttributes, lifeTime);
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
						latestPopulation, populationName, alphaXover, pCrossOver, crossoverRouletteStrategy, initialDepSpacing,
						useOdPairsForInitialRoutes, initialRoutesPerNetwork, vehicleTypeName, vehicleLength, maxVelocity, vehicleSeats, vehicleStandingRoom,
						defaultPtMode, stopTime, blocksLane, logEntireRoutes, minCrossingDistanceFactorFromRouteEnd, maxCrossingAngle,
						zurich_NetworkCenterCoord, lastIterationOriginal, pMutation, pBigChange, pSmallChange, routeDisutilityLimit,
						shortestPathStrategy, minInitialTerminalRadiusFromCenter, minTerminalRadiusFromCenter, maxTerminalRadiusFromCenter,
						minInitialTerminalRadiusFromCenter, maxInitialTerminalRadiusFromCenter, metroCityRadius, varyInitRouteSize, 
						tFirstDep, tLastDep, odConsiderationThreshold,
						xOffset, yOffset, stopUnprofitableRoutesReplacementGEN, blockFreqModGENs, generationNr, lastGeneration, maxConnectingDistance);
			}		
			
		}

	// PLOT RESULTS
		int generationsToPlot = lastGeneration;
		Visualizer.writeChartAverageTravelTimes(generationsToPlot, populationSize, initialRoutesPerNetwork, lastIteration,
				"zurich_1pm/Evolution/Population/networkScoreMaps.xml", "zurich_1pm/Evolution/Population/networkTravelTimesEvo.png");
		Visualizer.writeChartNetworkScore(generationsToPlot, populationSize, initialRoutesPerNetwork, lastIteration,
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
				"minTerminalDistance="+minInitialTerminalRadiusFromCenter  + ";\r\n" + 
				"minInitialTerminalRadiusFromCenter="+minInitialTerminalRadiusFromCenter  + ";\r\n" + 
				"maxInitialTerminalRadiusFromCenter="+maxInitialTerminalRadiusFromCenter  + ";\r\n" + 
				"minInitialTerminalDistance="+minInitialTerminalDistance  + ";\r\n" + 				
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
				"maxConsideredTravelTimeInSec="+maxConsideredTravelTimeInSec  + ";\r\n" + 
				"populationFactor="+populationFactor  + ";\r\n" + 
				"censusSize="+censusSize + ";\r\n" +
				"alphaXover="+alphaXover  + ";\r\n" + 
				"pCrossOver="+pCrossOver  + ";\r\n" + 
				"minCrossingDistanceFactorFromRouteEnd="+minCrossingDistanceFactorFromRouteEnd  + ";\r\n" + 
				"maxConnectingDistance="+maxConnectingDistance  + ";\r\n" + 
				"logEntireRoutes="+logEntireRoutes  + ";\r\n" + 
				"maxCrossingAngle="+maxCrossingAngle  + ";\r\n" + 
				"pMutation="+pMutation  + ";\r\n" + 
				"pBigChange="+pBigChange  + ";\r\n" + 
				"pSmallChange="+pSmallChange  + ";\r\n" + 
				"crossoverRouletteStrategy="+crossoverRouletteStrategy  + ";\r\n" +
				"routeDisutilityLimit="+routeDisutilityLimit  + ";\r\n" +
				"blockFreqModGENs="+blockFreqModGENs + ";\r\n" +
				"stopUnprofitableRoutesReplacementGEN="+stopUnprofitableRoutesReplacementGEN + ";\r\n" +
				"ConstrCostUGnew="+ConstrCostUGnew + ";\r\n" +
				"ConstrCostUGdevelop="+ ConstrCostUGdevelop+ ";\r\n" +
				"ConstrCostOGnew="+ ConstrCostOGnew+ ";\r\n" +
				"ConstrCostOGdevelop="+ ConstrCostOGdevelop+ ";\r\n" + 
				"ConstrCostOGequip="+ConstrCostOGequip+ ";\r\n" + 
				"ConstrCostPerStationNew="+ConstrCostPerStationNew+ ";\r\n" + 
				"ConstrCostPerStationExtend="+ConstrCostPerStationExtend+ ";\r\n" +
				"costVehicle="+costVehicle+ ";\r\n" +
				"OpsCostPerVehDistUG="+OpsCostPerVehDistUG+ ";\r\n" +
				"OpsCostPerVehDistOG="+ OpsCostPerVehDistOG+ ";\r\n" +
				"occupancyRate="+occupancyRate+ ";\r\n" + 
				"ptPassengerCostPerDist="+ptPassengerCostPerDist+";\r\n" +
				"taxPerVehicleDist="+taxPerVehicleDist+ ";\r\n" +
				"carCostPerVehDist="+carCostPerVehDist+ ";\r\n" + 
				"externalCarCosts="+externalCarCosts+ ";\r\n" +
				"externalPtCosts="+externalPtCosts+ ";\r\n" +
				"ptTrafficIncreasePercentage="+ptTrafficIncreasePercentage+ ";\r\n" + 
				"VATPercentage="+VATPercentage+ ";\r\n" +
				"utilityOfTimePT="+utilityOfTimePT+ ";\r\n" +
				"utilityOfTimeCar="+utilityOfTimeCar+ ";\r\n" +
				"globalCostFactor="+globalCostFactor+ ";\r\n" +
				"mergeMetroWithRailway="+mergeMetroWithRailway+ ";\r\n" +
				"useFastSBahnModule="+useFastSBahnModule+ ";\r\n" +
				"varyInitRouteSize="+varyInitRouteSize+ ";\r\n" +
				"enableThreading="+enableThreading+ ";\r\n" +
				"nThreads="+nThreads
				);

		// Free space after successful run:
//		Integer keepGenerationInterval = 10;
//		NetworkEvolutionImpl.freeSpace(lastGeneration, keepGenerationInterval, populationSize);

		
		Log.write("END TIME = "+(new SimpleDateFormat("HH:mm:ss")).format(Calendar.getInstance().getTime()));
	} // end Main Method

} // end NetworkEvolution Class


// For overview of different modules refer to versions before 11.10.2018
