package ch.ethz.matsim.students.samark;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.sound.midi.Synthesizer;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import ch.ethz.matsim.baseline_scenario.config.CommandLine.ConfigurationException;

public class NetworkEvolutionXSimOnly {

	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws ConfigurationException, IOException {
//		Here, we don't want to empty logFile before simulating because we want the whole history
//		PrintWriter pwDefault = new PrintWriter("zurich_1pm/Evolution/Population/LogDefault.txt");	pwDefault.close();	// Prepare empty defaultLog file for run
//		PrintWriter pwEvo = new PrintWriter("zurich_1pm/Evolution/Population/LogEvo.txt");	pwEvo.close();				// Prepare empty evoLog file for run

		
	// INITIALIZATIon
		// if desired, process raw existing network for its performance for reference : NetworkScoreLog rawNetworkPerformance = NetworkEvolutionRunSim.peoplePlansProcessingStandard("zurich_1pm/Zurich_1pm_SimulationOutput/output_plans.xml.gz", 240);		
		// NetworkEvolutionRunSim.peoplePlansProcessingStandard("zurich_1pm/Zurich_1pm_SimulationOutput/output_plans.xml.gz", 240); or ("zurich_1pm/Zurich_1pm_SimulationOutput_BACKUP__10/output_plans.xml.gz", 240); or ("zurich_1pm/Evolution/Population/Network1/Simulation_Output/output_plans.xml.gz", 240);
	// - Initiate N networks to make a population
		// % Parameters for Population: %
	    int populationSize = 2;
	    String populationName = "evoNetworks";
	    int initialRoutesPerNetwork = 5;
	    boolean mergeMetroWithRailway = false;
	    String shortestPathStrategy = "Dijkstra2";
	    String initialRouteType = "Random";
	    boolean useOdPairsForInitialRoutes = false;
	    if (initialRouteType.equals("OD")) {
	      useOdPairsForInitialRoutes = true;
	    }
	    int iterationToReadOriginalNetwork = 100;
	    String zeroLog = "zurich_1pm/Evolution/Population/HistoryLog/Generation0";
	    new File(zeroLog).mkdirs();
	    
	    Coord zurich_NetworkCenterCoord = new Coord(2683000.0D, 1247700.0D);
	    double xOffset = 1733436.0D;
	    double yOffset = -4748525.0D;
	    double metroCityRadius = 4000.0D;
	    double minMetroRadiusFactor = 0.0D;
	    double maxMetroRadiusFactor = 1.4D;
	    double minMetroRadiusFromCenter = metroCityRadius * minMetroRadiusFactor;
	    double maxMetroRadiusFromCenter = metroCityRadius * maxMetroRadiusFactor;
	    double maxExtendedMetroRadiusFromCenter = 1.0D * maxMetroRadiusFromCenter;
	    int nMostFrequentLinks = (int)metroCityRadius / 25;
	    double maxNewMetroLinkDistance = Math.max(0.2D * metroCityRadius, 1400.0D);
	    double minTerminalRadiusFromCenter = 0.3D * metroCityRadius;
	    
	    double maxTerminalRadiusFromCenter = maxExtendedMetroRadiusFromCenter;
	    double minTerminalDistance = 0.8D * maxMetroRadiusFromCenter;
	    double railway2metroCatchmentArea = 150.0D;
	    double metro2metroCatchmentArea = 400.0D;
	    double odConsiderationThreshold = 0.1D;
	    
	    String vehicleTypeName = "metro";double maxVelocity = 22.22222222222222D;
	    double vehicleLength = 50.0D;int vehicleSeats = 100;int vehicleStandingRoom = 100;
	    double initialDepSpacing = 450.0D;double tFirstDep = 21600.0D;double tLastDep = 73800.0D;
	    double stopTime = 30.0D;String defaultPtMode = "metro";boolean blocksLane = false;
	    double metroOpsCostPerKM = 1000.0D;double metroConstructionCostPerKmOverground = 1000000.0D;double metroConstructionCostPerKmUnderground = 1.0E7D;	
		
		/*
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

		MNetworkPop latestPopulation = networkPopulation;
		*/
		
		
		// RECALL OLD SIMULATION STATE
		Map<Id<Link>, CustomMetroLinkAttributes> metroLinkAttributes = new HashMap<Id<Link>, CustomMetroLinkAttributes>();
		int generationToRecall = 4;	// it is recommended to use the Generation before the one that failed in order
									// to make sure it's data is complete and ready for next clean generation
	    MNetworkPop latestPopulation = new MNetworkPop(populationName);
		NetworkEvolutionRunSim.recallSimulation(latestPopulation, metroLinkAttributes, generationToRecall, "evoNetworks", populationSize, initialRoutesPerNetwork);
		
		
	// EVOLUTIONARY PROCESS
		Config config = ConfigUtils.createConfig();
		config.getModules().get("network").addParam("inputNetworkFile", "zurich_1pm/Evolution/Population/GlobalNetwork.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Network globalNetwork = scenario.getNetwork();
		
		int firstGeneration = generationToRecall;
		int lastGeneration = 7;
		
		double averageTravelTimePerformanceGoal = 40.0;
		int lastIteration = 1;
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
//			latestPopulation = NetworkEvolutionRunSim.peoplePlansProcessingM(latestPopulation, maxConsideredTravelTimeInMin);
			
		// - TOTAL SCORE CALCULATOR & HISTORY LOGGER & SCORE CHECK: hand over score to a separate score map for sorting scores	and store most important data of each iteration	
			Log.write("LOGGING SCORES of GEN"+generationNr+":");
			String historyFileLocation = "zurich_1pm/Evolution/Population/HistoryLog/Generation"+generationNr;
			String networkScoreMapGeneralLocation = "zurich_1pm/Evolution/Population/networkScoreMap.xml";
			Map<String, NetworkScoreLog> networkScoreMap = new HashMap<String, NetworkScoreLog>();
//			boolean performanceGoalAccomplished = NetworkEvolutionImpl.logResults(networkScoreMap, historyFileLocation, networkScoreMapGeneralLocation, 
//					latestPopulation, averageTravelTimePerformanceGoal, generationNr);
//			if(performanceGoalAccomplished == true) {		// 
//				break;
//			}
			
		// - EVOLUTION: If PerformanceGoal not yet achieved, change routes and network here according to their scores!
			Log.write("EVOLUTION at the end of GEN"+generationNr+":");
			Log.writeEvo("%%%    EVOLUTION OF GEN_"+generationNr+"    %%%");

			// Frequency Modification: Redistribute vehicle fleet by scores and stochastics before mutating actual routes
				// - first set all mRoute.nVehicle as a function of current fleet size and stochastics
				// - applyPT: make functions for depSpacing = f(nVehicles, total route length) while total route length = f(linkList or stopArray)
			
			double alpha = 10.0;											// tunes roulette wheel choice: high alpha (>5) enhances probability to choose a high-score network and decreases probability to choose a weak network more than linearly -> linearly would be p_i = Score_i/Score_tot)
			double pCrossOver = 0.30; 										// DEFAULT = 0.35;
			double minCrossingDistanceFactorFromRouteEnd = 0.3; 			// DEFAULT = 0.30; MINIMUM = 0.25
			boolean logEntireRoutes = false;
			double maxCrossingAngle = 110; 									// DEFAULT = 110;
			double pMutation = 0.35;										// DEFAULT = 0.35 
			double pBigChange = 0.2;										// DEFAULT = 0.20
			double pSmallChange = 1.0-pBigChange;
//			if (generationNr != lastGeneration) {
//				latestPopulation = NetworkEvolutionImpl.developGeneration(globalNetwork, metroLinkAttributes, networkScoreMap, latestPopulation, populationName, alpha, pCrossOver,
//						metroConstructionCostPerKmOverground, metroConstructionCostPerKmUnderground, metroOpsCostPerKM, iterationToReadOriginalNetwork, 
//						useOdPairsForInitialRoutes, vehicleTypeName, vehicleLength, maxVelocity, vehicleSeats, vehicleStandingRoom, defaultPtMode, stopTime, blocksLane, 
//						logEntireRoutes, minCrossingDistanceFactorFromRouteEnd, maxCrossingAngle, pMutation, pBigChange, pSmallChange);
//			}			
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

		
}


