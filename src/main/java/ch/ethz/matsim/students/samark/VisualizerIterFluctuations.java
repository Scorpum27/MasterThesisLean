package ch.ethz.matsim.students.samark;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

// run iter test after evaluating CBP_Originals
// in first run set recalculateNewCBP to true to calculate them
// choose if want to compare all iterations to global original or individual original cbp values


public class VisualizerIterFluctuations {

	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub

		// %%% --- %% --- %% --- %% --- %% --- %% --- %% --- %% --- NETWORK Utility FLUCTUATIONS by MATSIM --- %% --- %% --- %% --- %% --- %% --- %% --- %% ---

		// select good utility and bad utility network from a simulation
		// --> download Networks' MRoute files, simulation iterations (ITERS)
		// --> download BaseInfrastructure's globalNetwork, metroLinkAttributes
		// --> make sure all iters of original default sim are stored in ENRICHED-folder
		// --> Set networkPath to TestingFolder! (see below)
		// for (iterations) runEvents, runPeoplePlansProcessing, calculate scores

		// args: Network1 1 100 1 1000 false true 1
		String networkName = args[0]; // 4,8,9
		Integer generationNr = Integer.parseInt(args[1]);
		Integer maxIterations = Integer.parseInt(args[2]);
		Integer iterationsToAverage = Integer.parseInt(args[3]);
		Integer maxConsideredTravelTimeInSec = 240 * 60;
		Integer populationFactor = Integer.parseInt(args[4]);
		Boolean recalculateOriginalCBP = Boolean.parseBoolean(args[5]);
		Boolean recalculateNewCBP = Boolean.parseBoolean(args[6]);
		String originalValuesSelection = args[7];	// "global", "individual" if comparisons should use every iterations individual value or just global average				
		// String utilityFunctionSelection = args[8];
		Double lifeTime = 40.0;

		Config config = ConfigUtils.createConfig();
		config.getModules().get("network").addParam("inputNetworkFile",
				"zurich_1pm/Evolution/Population/BaseInfrastructure/GlobalNetwork.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Network globalNetwork = scenario.getNetwork();
		Map<Id<Link>, CustomMetroLinkAttributes> metroLinkAttributes = new HashMap<Id<Link>, CustomMetroLinkAttributes>();
		metroLinkAttributes.putAll(XMLOps.readFromFile(metroLinkAttributes.getClass(),
				"zurich_1pm/Evolution/Population/BaseInfrastructure/metroLinkAttributes.xml"));

		Map<Integer, Double> utilityByIteration = new HashMap<Integer, Double>();
		Map<Integer, Double> totalCostByIteration = new HashMap<Integer, Double>();
		Map<Integer, Double> travelTimeBenefitCarByIteration = new HashMap<Integer, Double>();
		Map<Integer, Double> travelTimeBenefitPtByIteration = new HashMap<Integer, Double>();
		Map<Integer, Double> travelTimeAverageCarByIteration = new HashMap<Integer, Double>();
		Map<Integer, Double> travelTimeAveragePtByIteration = new HashMap<Integer, Double>();
		Map<Integer, Double> travelTimeAverageCarByIterationOriginal = new HashMap<Integer, Double>();
		Map<Integer, Double> travelTimeAveragePtByIterationOriginal = new HashMap<Integer, Double>();
		
		Map<Integer, Double> carUsersByIteration = new HashMap<Integer, Double>();
		Map<Integer, Double> carUsersByIterationOriginal = new HashMap<Integer, Double>();
		Map<Integer, Double> deltaCarUsersByIteration = new HashMap<Integer, Double>();
		Map<Integer, Double> ptUsersByIteration = new HashMap<Integer, Double>();
		Map<Integer, Double> ptUsersByIterationOriginal = new HashMap<Integer, Double>();
		Map<Integer, Double> deltaPtUsersByIteration = new HashMap<Integer, Double>();
		
		
		CostBenefitParameters cbpOriginal;
		
		for (Integer lastIteration = 1; lastIteration <= maxIterations; lastIteration++) {

			// this section is for simulating again cbp of every single iteration and its comparisons
			String plansFolder = "zurich_1pm/Zurich_1pm_SimulationOutputEnriched/ITERS";
			String outputFile = "zurich_1pm/cbpParametersOriginal/cbpParametersOriginal" + lastIteration + ".xml";
			if (!(new File(outputFile)).exists() || recalculateOriginalCBP.equals(true)) {
				if (lastIteration < iterationsToAverage) { // then use all available (=lastIteration) for averaging
					cbpOriginal = NetworkEvolutionImpl.calculateCBAStats(plansFolder, outputFile,
							(int) populationFactor, lastIteration, lastIteration);
				} else {
					cbpOriginal = NetworkEvolutionImpl.calculateCBAStats(plansFolder, outputFile,
							(int) populationFactor, lastIteration, iterationsToAverage);
				}
			} else if (originalValuesSelection.equals("individual")){
				cbpOriginal = XMLOps.readFromFile(CostBenefitParameters.class, outputFile);
			} else if (originalValuesSelection.equals("global")){
				cbpOriginal = XMLOps.readFromFile(CostBenefitParameters.class, "zurich_1pm/cbpParametersOriginal/cbpParametersOriginalGlobal.xml");
			} else {
				cbpOriginal = XMLOps.readFromFile(CostBenefitParameters.class, "zurich_1pm/cbpParametersOriginal/cbpParametersOriginalGlobal.xml");
			}

			MNetwork mNetwork = new MNetwork(networkName); // TODO choose which network
			MNetworkPop latestPopulation = new MNetworkPop();
			latestPopulation.addNetwork(mNetwork);
			latestPopulation.modifiedNetworksInLastEvolution.add(networkName);

			
			if (!(new File("zurich_1pm/Evolution/Population/" + networkName + "/cbpParametersAveraged" + lastIteration + ".xml")).exists()
					|| recalculateNewCBP.equals(true)) {
				// need to add routes for events and plans processing
				for (int r = 1; r <= 1000; r++) {
					File routesFile = new File("zurich_1pm/Evolution/Population/HistoryLog/Generation" + generationNr
							+ "/MRoutes/" + networkName + "_Route" + r + "_RoutesFile.xml");
					if (routesFile.exists()) {
//						System.out.println("Trying to load route  =  Route"+r);
//						mNetwork.addNetworkRoute(XMLOps.readFromFile(MRoute.class, routesFile.toString()));
						System.out.println("Creating new pseudo route  =  Route"+r);
						mNetwork.addNetworkRoute(new MRoute(networkName+"_Route"+r));
					}
				}
				// do actual processing here (calculates cbp stats here already)
				if (lastIteration < iterationsToAverage) { // then use all available (=lastIteration) for averaging
					NetworkEvolutionRunSim.runEventsProcessing(latestPopulation, lastIteration, lastIteration,
							globalNetwork, "zurich_1pm/Evolution/Population/", populationFactor);
					NetworkEvolutionRunSim.peoplePlansProcessingM(latestPopulation, maxConsideredTravelTimeInSec,
							lastIteration, lastIteration, populationFactor, "zurich_1pm/Evolution/Population/");
				} else { // enough iterations to average over all iterationsToAverage
					NetworkEvolutionRunSim.runEventsProcessing(latestPopulation, lastIteration, iterationsToAverage,
							globalNetwork, "zurich_1pm/Evolution/Population/", populationFactor);
					NetworkEvolutionRunSim.peoplePlansProcessingM(latestPopulation, maxConsideredTravelTimeInSec,
							lastIteration, iterationsToAverage, populationFactor, "zurich_1pm/Evolution/Population/");
					// calculates cbp stats here already!
				}
			}
			
//			Log.write("LOGGING SCORES of GEN" + generationNr + ":");
//			mNetwork.lifeTime = lifeTime;
//			mNetwork.calculateRoutesAndNetworkScore(lastIteration, populationFactor, globalNetwork, metroLinkAttributes,
//					"zurich_1pm/cbpParametersOriginal/", "zurich_1pm/Evolution/Population/", utilityFunctionSelection);

			// CostBenefitParameters cbpOriginal = // calculated already at the top!!
			// XMLOps.readFromFile((new CostBenefitParameters()).getClass(),
			// "zurich_1pm/cbpParametersOriginal"+lastIteration+".xml");
			CostBenefitParameters cbpNew = XMLOps.readFromFile((new CostBenefitParameters()).getClass(),
					"zurich_1pm/Evolution/Population/" + networkName + "/cbpParametersAveraged"+lastIteration+".xml");

			utilityByIteration.put(lastIteration, mNetwork.overallScore);
			totalCostByIteration.put(lastIteration, mNetwork.constructionCost+mNetwork.operationalCost);
			travelTimeBenefitCarByIteration.put(lastIteration, mNetwork.travelTimeGainsCar);
			travelTimeBenefitPtByIteration.put(lastIteration, mNetwork.travelTimeGainsPT);
			travelTimeAverageCarByIteration.put(lastIteration, cbpNew.averageCartime);
			travelTimeAveragePtByIteration.put(lastIteration, cbpNew.averagePtTime);
			travelTimeAverageCarByIterationOriginal.put(lastIteration, cbpOriginal.averageCartime);
			travelTimeAveragePtByIterationOriginal.put(lastIteration, cbpOriginal.averagePtTime);
			carUsersByIteration.put(lastIteration, cbpNew.carUsers);
			carUsersByIterationOriginal.put(lastIteration, cbpOriginal.carUsers);
			deltaCarUsersByIteration.put(lastIteration, cbpNew.carUsers - cbpOriginal.carUsers);
			ptUsersByIteration.put(lastIteration, cbpNew.ptUsers);
			ptUsersByIterationOriginal.put(lastIteration, cbpOriginal.ptUsers);
			deltaPtUsersByIteration.put(lastIteration, cbpNew.ptUsers - cbpOriginal.ptUsers);
			
			
			
//			Log.write("lastIteration = "+lastIteration);
//			Log.write("cbpNew.carUsers = "+cbpNew.carUsers);
//			Log.write("cbpOld.carUsers = "+cbpOriginal.carUsers);
//			Log.write("cbpNew.ptUsers = "+cbpNew.ptUsers);
//			Log.write("cbpOld.ptUsers = "+cbpOriginal.ptUsers);
//			Log.write("Map entry delta car users = " + deltaCarUsersByIteration.get(lastIteration) );
//			Log.write("Map entry delta pt users = " + deltaPtUsersByIteration.get(lastIteration) );
			
			// | Times | carTimeSavings, ptTimeSavingsUtility |
			// NetworkScore
		}
		
		List<Double> deltaPtTimeAverage = new ArrayList<Double>();
		List<Double> deltaCarTimeAverage = new ArrayList<Double>();
		for (int iter=1; iter<=travelTimeAveragePtByIteration.size(); iter++) {
			deltaPtTimeAverage.add(100*(travelTimeAveragePtByIteration.get(iter)-travelTimeAveragePtByIterationOriginal.get(iter))/travelTimeAveragePtByIterationOriginal.get(iter));
			deltaCarTimeAverage.add(100*(travelTimeAverageCarByIteration.get(iter)-travelTimeAverageCarByIterationOriginal.get(iter))/travelTimeAverageCarByIterationOriginal.get(iter));
		}
		Double deltaPtTimeAverageStdDev = VisualizerStdDev.sampleStandardDeviation(deltaPtTimeAverage);
		Double deltaCarTimeAverageStdDev = VisualizerStdDev.sampleStandardDeviation(deltaCarTimeAverage);
		Double deltaPtTimeAverageMean = VisualizerStdDev.mean(deltaPtTimeAverage);
		Double deltaCarTimeAverageMean = VisualizerStdDev.mean(deltaCarTimeAverage);		
		
		
		// calculate average of all new cbp parameters here:
		double ptUsers = 0.0;
		double carUsers = 0.0;
		double otherUsers = 0.0;
		double carTimeTotal = 0.0;
		double carPersonDist = 0.0;
		double ptTimeTotal = 0.0;
		double ptPersonDist = 0.0;
		double metroPersonDist = 0.0;
		int minIter = (int) Math.min(20.0, 1.0*maxIterations);
		int iterGlobalAverage = maxIterations-minIter+1;
		for (Integer i = minIter; i<=maxIterations; i++) {
			CostBenefitParameters cbpi = XMLOps.readFromFile(CostBenefitParameters.class,
					"zurich_1pm/Evolution/Population/" + networkName + "/cbpParametersAveraged"+i+".xml");
			ptUsers += cbpi.ptUsers;
			carUsers += cbpi.carUsers;
			otherUsers += cbpi.otherUsers;
			carTimeTotal += cbpi.carTimeTotal;
			carPersonDist += cbpi.carPersonDist;
			ptTimeTotal += cbpi.ptTimeTotal;
			ptPersonDist += cbpi.ptPersonDist;
			metroPersonDist += cbpi.metroPersonDist;
		}
		CostBenefitParameters cbpGlobal = new CostBenefitParameters(ptUsers/iterGlobalAverage, carUsers/iterGlobalAverage, otherUsers/iterGlobalAverage,
				carTimeTotal/iterGlobalAverage, carPersonDist/iterGlobalAverage, ptTimeTotal/iterGlobalAverage, ptPersonDist/iterGlobalAverage,
				metroPersonDist/iterGlobalAverage);
		XMLOps.writeToFile(cbpGlobal, "zurich_1pm/Evolution/Population/" + networkName + "/cbpParametersAveragedGlobal.xml");
		
		
		
		// Visualize developments
		Visualizer.plot2D(" Network Utility by MATSimIterationStage [#maxMATSimIter=" + maxIterations + "] \r\n ",
				"MATSim Iteration", "Annual Utility [Mio CHF]", Arrays.asList(utilityByIteration, totalCostByIteration,
						travelTimeBenefitCarByIteration, travelTimeBenefitPtByIteration),
				Arrays.asList("Total Utility - "+networkName, "Total Cost", "TravelTimeGains-Car", "TravelTimeGains-Pt"), 0.0, 0.0, null,
				"UtilityByIteration" + networkName + "_maxIter" + maxIterations + ".png"); // rangeAxis.setRange(-21.0E1, 1.5E1)

		Visualizer.plot2D(" Travel Time Gains by MATSimIterationStage [#maxMATSimIter=" + maxIterations + "] \r\n ",
				"MATSim Iteration", "Annual Utility of Travel Time Gains [Mio CHF]",
				Arrays.asList( travelTimeBenefitCarByIteration, travelTimeBenefitPtByIteration),
				Arrays.asList("TravelTimeGains-Car", "TravelTimeGains-Pt"), 0.0, 0.0, null,
				"TravelTimeGainsByIteration" + networkName + "_maxIter" + maxIterations + ".png"); // rangeAxis.setRange(-21.0E1, 1.5E1)
		
		Visualizer.plot2D(" Modal Split (#usersAbsolute) by MATSimIterationStage [#maxMATSimIter=" + maxIterations + "] \r\n ",
				"MATSim Iteration", "Delta #TransportModeUsers (MetroCase-DefaultCase)",
				Arrays.asList(deltaCarUsersByIteration, deltaPtUsersByIteration),
				Arrays.asList("Car", "PT"), 0.0, 0.0, null,
				"DeltaModeUsersByIteration" + networkName + "_maxIter" + maxIterations + ".png"); // rangeAxis.setRange(-21.0E1, // 1.5E1)

		Visualizer.plot2D(" Modal Split (#users) by MATSimIterationStage [#maxMATSimIter=" + maxIterations + "] \r\n ",
				"MATSim Iteration", "#TransportModeUsers",
				Arrays.asList(carUsersByIteration, carUsersByIterationOriginal, ptUsersByIteration, ptUsersByIterationOriginal),
				Arrays.asList("Car - Metro Case", "Car - Default ZH Case", "PT - Metro Case", "PT - Default ZH Case"), 0.0, 0.0, null,
				"ModeShareByIteration_1pm" + "_maxIter" + maxIterations + ".png"); // rangeAxis.setRange(-21.0E1, // 1.5E1)
		
		Visualizer.plot2D(" AverageTravelTime by MATSimIterationStage [#maxMATSimIter=" + maxIterations + "] \r\n "
				+ "[Metro case average = " +deltaCarTimeAverageMean+ " ],  [Standard deviation from reference value = " +deltaCarTimeAverageStdDev+" ]",
				"MATSim Iteration", "AverageTravelTime [s]",
				Arrays.asList(travelTimeAverageCarByIteration, travelTimeAverageCarByIterationOriginal),
				Arrays.asList("Car - Metro Case", "Car - Default ZH Case"), 0.0, 0.0, null,
				"AverageCarTravelTimeByIteration_1pm" + "_maxIter" + maxIterations + ".png"); // rangeAxis.setRange(-21.0E1, // 1.5E1)
		
		Visualizer.plot2D(" AverageTravelTime by MATSimIterationStage [#maxMATSimIter=" + maxIterations + "] \r\n "
				+ "[Metro case average = " +deltaPtTimeAverageMean+ " ],  [Standard deviation from reference value = " +deltaPtTimeAverageStdDev+" ]",
				"MATSim Iteration", "AverageTravelTime [s]",
				Arrays.asList(travelTimeAveragePtByIteration, travelTimeAveragePtByIterationOriginal),
				Arrays.asList("PT - Metro Case", "PT - Default ZH Case"), 0.0, 0.0, null,
				"AveragePtTravelTimeByIteration_1pm" + "_maxIter" + maxIterations + ".png"); // rangeAxis.setRange(-21.0E1, // 1.5E1)
	}

}
