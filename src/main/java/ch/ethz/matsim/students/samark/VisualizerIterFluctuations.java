package ch.ethz.matsim.students.samark;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jfree.data.Range;
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
		String networkName = args[0]; // Network1 1 100 1 1000 false false global 
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
		
		Map<Integer, Double> totalBenefit = new HashMap<Integer, Double>();
		Map<Integer, Double> travelTimeGainsPt = new HashMap<Integer, Double>();
		Map<Integer, Double> travelTimeGainsCar = new HashMap<Integer, Double>();
		Map<Integer, Double> travelTimeGains = new HashMap<Integer, Double>();
		
		CBPII cbpOriginal;
		
		MNetwork mNetwork = new MNetwork(networkName); // TODO choose which network
		MNetworkPop latestPopulation = new MNetworkPop();
		latestPopulation.addNetwork(mNetwork);
		latestPopulation.modifiedNetworksInLastEvolution.add(networkName);
		for (int r = 1; r <= 1000; r++) {
			File routesFile = new File("zurich_1pm/Evolution/Population/HistoryLog/Generation" + generationNr
					+ "/MRoutes/" + networkName + "_Route" + r + "_RoutesFile.xml");
			if (routesFile.exists()) {
				System.out.println("Loading route =  Route"+r);
				mNetwork.addNetworkRoute(XMLOps.readFromFile(MRoute.class, routesFile.toString()));
			}
		}
		
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
				cbpOriginal = XMLOps.readFromFile(CBPII.class, outputFile);
			} else if (originalValuesSelection.equals("global")){
				cbpOriginal = XMLOps.readFromFile(CBPII.class, "zurich_1pm/cbpParametersOriginal/cbpParametersOriginalGlobal.xml");
			} else {
				cbpOriginal = XMLOps.readFromFile(CBPII.class, "zurich_1pm/cbpParametersOriginal/cbpParametersOriginalGlobal.xml");
			}
			
			if (!(new File("zurich_1pm/Evolution/Population/" + networkName + "/cbpParametersAveraged" + lastIteration + ".xml")).exists()
					|| recalculateNewCBP.equals(true)) {
				// need to add routes to Network(Population) for events and plans processing - already added in outside loop
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
			
//			// CALCULATE NETWORK SCORES (UTILITIES) - for SIMs after 13-11-18 12:00h, this is not necessary as it is done in evaluation directly
//			// load MRoutes to a network - already done above
//			mNetwork.calculateRoutesAndNetworkScore(lastIteration, populationFactor, globalNetwork, metroLinkAttributes,
//					"zurich_1pm/cbpParametersOriginal/", "zurich_1pm/Evolution/Population/", "1");

			
			CBPII cbpNew = XMLOps.readFromFile((new CBPII()).getClass(),
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
			totalBenefit.put(lastIteration, cbpNew.totalAnnualBenefit);
			travelTimeGains.put(lastIteration, cbpNew.travelTimeGains);
			travelTimeGainsPt.put(lastIteration, cbpNew.travelTimeGainsPt);
			travelTimeGainsCar.put(lastIteration, cbpNew.travelTimeGainsCar);
			
			
			// store as map that can be retrieved! Show fluctuations
			// make new utility function with all MATSim utilities....
			
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
			CBPII cbpi = XMLOps.readFromFile(CBPII.class,
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
		CBPII cbpGlobal = new CBPII(ptUsers/iterGlobalAverage, carUsers/iterGlobalAverage, otherUsers/iterGlobalAverage,
				carTimeTotal/iterGlobalAverage, carPersonDist/iterGlobalAverage, ptTimeTotal/iterGlobalAverage, ptPersonDist/iterGlobalAverage,
				metroPersonDist/iterGlobalAverage);
		XMLOps.writeToFile(cbpGlobal, "zurich_1pm/Evolution/Population/" + networkName + "/cbpParametersAveragedGlobal.xml");
		
		String pop;
		if(populationFactor==100) {
			pop = "1pct";
		}
		else if(populationFactor==1000) {
			pop = "1pm";
		}
		else {
			pop = "";
		}
		
		// Visualize developments
//		Visualizer.plot2D(" Network Utility by MATSimIterationStage [#maxMATSimIter=" + maxIterations + "] \r\n ",
//				"MATSim Iteration", "Annual Utility [Mio CHF]", Arrays.asList(utilityByIteration, totalCostByIteration,
//						travelTimeBenefitCarByIteration, travelTimeBenefitPtByIteration),
//				Arrays.asList("Total Utility - "+networkName, "Total Cost", "TravelTimeGains-Car", "TravelTimeGains-Pt"), 0.0, 0.0, null,
//				"UtilityByIteration" + networkName + "_maxIter" + maxIterations + ".png"); // rangeAxis.setRange(-21.0E1, 1.5E1)
//
//		Visualizer.plot2D(" Travel Time Gains by MATSimIterationStage [#maxMATSimIter=" + maxIterations + "] \r\n ",
//				"MATSim Iteration", "Annual Utility of Travel Time Gains [Mio CHF]",
//				Arrays.asList( travelTimeBenefitCarByIteration, travelTimeBenefitPtByIteration),
//				Arrays.asList("TravelTimeGains-Car", "TravelTimeGains-Pt"), 0.0, 0.0, null,
//				"TravelTimeGainsByIteration" + networkName + "_maxIter" + maxIterations + ".png"); // rangeAxis.setRange(-21.0E1, 1.5E1)
		
		Visualizer.plot2D(" Modal Split (#usersAbsolute) by MATSimIterationStage [#maxMATSimIter=" + maxIterations + "] \r\n ",
				"MATSim Iteration", "Delta #TransportModeUsers (MetroCase-DefaultCase)",
				Arrays.asList(deltaCarUsersByIteration, deltaPtUsersByIteration),
				Arrays.asList("Car", "PT"), 0.0, 0.0, null,
				"DeltaModeUsersByIteration_" +pop + networkName + "_maxIter" + maxIterations + ".png"); // rangeAxis.setRange(-21.0E1, // 1.5E1)
		
		Visualizer.plot2D(" Modal Split (#users) by MATSimIterationStage [#maxMATSimIter=" + maxIterations + "] \r\n ",
				"MATSim Iteration", "#TransportModeUsers",
				Arrays.asList(carUsersByIteration, carUsersByIterationOriginal, ptUsersByIteration, ptUsersByIterationOriginal),
				Arrays.asList("Car - Metro Case", "Car - Default ZH Case", "PT - Metro Case", "PT - Default ZH Case"), 0.0, 0.0, null,
				"ModeShareByIteration_"+pop + "_maxIter" + maxIterations + ".png"); // rangeAxis.setRange(-21.0E1, // 1.5E1)

		Visualizer.plot2D(" Benefits [#maxMATSimIter=" + maxIterations + "] \r\n ",
				"MATSim Iteration", "Annual Benefit [CHF p.a.]",
				Arrays.asList(totalBenefit, travelTimeGains, travelTimeGainsPt, travelTimeGainsCar),
				Arrays.asList("totalBenefit", "travelTimeGains", "travelTimeGainsPt", "travelTimeGainsCar"), 0.0, 0.0, null,
				"BenefitsByIteration_"+pop + "_maxIter" + maxIterations + ".png"); // rangeAxis.setRange(-21.0E1, // 1.5E1)

		
		CBPII cbpOriginalGlobal = XMLOps.readFromFile(CBPII.class, "zurich_1pm/cbpParametersOriginal/cbpParametersOriginalGlobal.xml");
		String[] originals = Log.readFile("zurich_1pm/cbpParametersOriginal/Percentiles_90.txt", Charset.defaultCharset()).split(",");
		Double travelTimeAverageCarOrigConfInterval = Double.parseDouble(originals[0]);
		Double travelTimeAveragePtOrigConfInterval = Double.parseDouble(originals[1]);
		Double meanCarTime = VisualizerStdDev.meanMap(travelTimeAverageCarByIteration);
		Double stdDevCarTime = VisualizerStdDev.getPercentileIntervalMap(travelTimeAverageCarByIteration, 90);
		Double meanPtTime = VisualizerStdDev.meanMap(travelTimeAveragePtByIteration);
		Double stdDevPtTime = VisualizerStdDev.getPercentileIntervalMap(travelTimeAveragePtByIteration, 90);
//		System.out.println("meanCarTime="+meanCarTime);
//		System.out.println("stdDevCarTime="+stdDevCarTime);
//		System.out.println("meanPtTime="+meanPtTime);
//		System.out.println("stdDevPtTime="+stdDevPtTime);
		
		Visualizer.plot2DConfIntervals(" AverageTravelTime by MATSimIterationStage [#maxMATSimIter=" + maxIterations + "] \r\n "
				+ "[Metro scenario average = " +meanCarTime+ " ],  [StdDev from ref. value = " +stdDevCarTime+" ]",
				"MATSim Iteration", "AverageTravelTime [s]",
				Arrays.asList(travelTimeAverageCarByIteration, travelTimeAverageCarByIterationOriginal),
				Arrays.asList("Car - Metro Case", "Car - Default ZH Case"), 0.0, 0.0, new Range(2000.0, 3000.0),
				"AverageCarTravelTimeByIteration_" + pop + "_maxIter" + maxIterations + ".png",
				Arrays.asList(
					Arrays.asList(meanCarTime-stdDevCarTime, meanCarTime+stdDevCarTime),
					Arrays.asList(cbpOriginalGlobal.averageCartime - travelTimeAverageCarOrigConfInterval,
									cbpOriginalGlobal.averageCartime + travelTimeAverageCarOrigConfInterval))); // rangeAxis.setRange(-21.0E1, // 1.5E1)
		
		Visualizer.plot2DConfIntervals(" AverageTravelTime by MATSimIterationStage [#maxMATSimIter=" + maxIterations + "] \r\n "
				+ "[Metro scenario average = " +meanPtTime+ " ],  [StdDev from ref. value = " +stdDevPtTime+" ]",
				"MATSim Iteration", "AverageTravelTime [s]",
				Arrays.asList(travelTimeAveragePtByIteration, travelTimeAveragePtByIterationOriginal),
				Arrays.asList("PT - Metro Case", "PT - Default ZH Case"), 0.0, 0.0, new Range(6300.0, 7300.0),
				"AveragePtTravelTimeByIteration_" + pop + "_maxIter" + maxIterations + ".png",
				Arrays.asList(
					Arrays.asList(meanPtTime-stdDevPtTime, meanPtTime+stdDevPtTime),
					Arrays.asList(cbpOriginalGlobal.averagePtTime - travelTimeAveragePtOrigConfInterval,
									cbpOriginalGlobal.averagePtTime + travelTimeAveragePtOrigConfInterval))); // rangeAxis.setRange(-21.0E1, // 1.5E1)
		

	}

}
