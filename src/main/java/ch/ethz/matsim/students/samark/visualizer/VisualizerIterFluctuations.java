package ch.ethz.matsim.students.samark.visualizer;

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

import ch.ethz.matsim.students.samark.*;

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

	// ---------- PARAMETERS ----------
		String networkName = args[0]; // Network1 1 1000 1 1000 false false global 
		Integer generationNr = Integer.parseInt(args[1]);
		Integer maxIterations = Integer.parseInt(args[2]);
		Integer iterationsToAverage = Integer.parseInt(args[3]);
		String censusSize = args[4];
		Integer maxConsideredTravelTimeInSec = 240 * 60;
		Boolean recalculateOriginalCBP = Boolean.parseBoolean(args[5]);
		Boolean recalculateNewCBP = Boolean.parseBoolean(args[6]);
		String originalValuesSelection = args[7];	// "global", "individual" if comparisons should use every iterations individual value or just global average				
		// String utilityFunctionSelection = args[8];
		Double lifeTime = 40.0;
		
		Range yRange = null;
		Integer populationFactor = 0;
		if (censusSize.equals("1pct")) { populationFactor = 100; yRange = null; }			// yRange = new Range(6400.0, 7000.0);
		else if (censusSize.equals("0.4pm")) {populationFactor = 2500;}
		else if (censusSize.equals("0.5pm")) {populationFactor = 2000;}
		else if (censusSize.equals("0.6pm")) {populationFactor = 1667;}
		else if (censusSize.equals("1pm")) {populationFactor = 1000; yRange = null; } 	// yRange = new Range(7400.0, 8100.0);
		else if (censusSize.equals("3pm")) {populationFactor = 333; yRange = null; }
		else if (censusSize.equals("6pm")) {populationFactor = 167; yRange = null; }
		else {System.out.println("Census Size invalid! Please check. Aborting..."); System.exit(0);};

		

	// ---------- INITIALIZATION ----------
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
		Map<Integer, Double> travelTimeBenefitOtherByIteration = new HashMap<Integer, Double>();
		Map<Integer, Double> travelTimeAverageCarByIteration = new HashMap<Integer, Double>();
		Map<Integer, Double> travelTimeAverageCarByIterationOriginal = new HashMap<Integer, Double>();
		Map<Integer, Double> travelTimeAveragePtByIteration = new HashMap<Integer, Double>();
		Map<Integer, Double> travelTimeAveragePtByIterationOriginal = new HashMap<Integer, Double>();
		Map<Integer, Double> travelTimeAverageOtherByIteration = new HashMap<Integer, Double>();
		Map<Integer, Double> travelTimeAverageOtherByIterationOriginal = new HashMap<Integer, Double>();
		Map<Integer, Double> carUsersByIteration = new HashMap<Integer, Double>();
		Map<Integer, Double> carUsersByIterationOriginal = new HashMap<Integer, Double>();
		Map<Integer, Double> deltaCarUsersByIteration = new HashMap<Integer, Double>();
		Map<Integer, Double> ptUsersByIteration = new HashMap<Integer, Double>();
		Map<Integer, Double> ptUsersByIterationOriginal = new HashMap<Integer, Double>();
		Map<Integer, Double> deltaPtUsersByIteration = new HashMap<Integer, Double>();
		Map<Integer, Double> otherUsersByIteration = new HashMap<Integer, Double>();
		Map<Integer, Double> otherUsersByIterationOriginal = new HashMap<Integer, Double>();
		Map<Integer, Double> deltaOtherUsersByIteration = new HashMap<Integer, Double>();
		Map<Integer, Double> totalBenefit = new HashMap<Integer, Double>();
		Map<Integer, Double> travelTimeGainsPt = new HashMap<Integer, Double>();
		Map<Integer, Double> travelTimeGainsCar = new HashMap<Integer, Double>();
		Map<Integer, Double> travelTimeGainsOther = new HashMap<Integer, Double>();
		Map<Integer, Double> travelTimeGains = new HashMap<Integer, Double>();
		Map<Integer, Double> otherBenefits = new HashMap<Integer, Double>();

		
	// ---------- INITIALIZATION ----------
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

	// ---------- SCAN ITERATIONS ----------
		// Backup last iter from sim for totalCosts before overwriting in following loop!
		CBPII cbpOriginal;
		CBPII cbpFinalBackupForCost;
		if (!(new File("zurich_1pm/Evolution/Population/" + networkName + "/cbpParametersAveraged"+maxIterations+"_Backup.xml")).exists()) {
			cbpFinalBackupForCost = XMLOps.readFromFile((new CBPII()).getClass(),
					"zurich_1pm/Evolution/Population/" + networkName + "/cbpParametersAveraged"+maxIterations+".xml");
			XMLOps.writeToFile(cbpFinalBackupForCost, "zurich_1pm/Evolution/Population/" + networkName + "/cbpParametersAveraged"+maxIterations+"_Backup.xml");
		}
		else {
			cbpFinalBackupForCost = XMLOps.readFromFile((new CBPII()).getClass(),
					"zurich_1pm/Evolution/Population/" + networkName + "/cbpParametersAveraged"+maxIterations+"_Backup.xml");
		}
		// go through ALL iterations
		for (Integer lastIteration = 1; lastIteration <= maxIterations; lastIteration++) {
			// ---------- GET ORIGINAL CBPs ----------
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
			
		// ---------- GET NEW CBPs ----------
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
			CBPII cbpNew = XMLOps.readFromFile((new CBPII()).getClass(),
					"zurich_1pm/Evolution/Population/" + networkName + "/cbpParametersAveraged"+lastIteration+".xml");
			cbpNew.calculateBenefits(cbpOriginal);
			XMLOps.writeToFile(cbpNew, "zurich_1pm/Evolution/Population/" + networkName + "/cbpParametersAveraged"+lastIteration+".xml");

		// ---------- FILL IN FOR SIMs ----------
			utilityByIteration.put(lastIteration, mNetwork.overallScore);
			totalCostByIteration.put(lastIteration, mNetwork.constructionCost+mNetwork.operationalCost);
			travelTimeBenefitCarByIteration.put(lastIteration, mNetwork.travelTimeGainsCar);
			travelTimeBenefitPtByIteration.put(lastIteration, mNetwork.travelTimeGainsPT);
			travelTimeAverageCarByIteration.put(lastIteration, cbpNew.averageCartime);
			travelTimeAverageCarByIterationOriginal.put(lastIteration, cbpOriginal.averageCartime);
			travelTimeAveragePtByIteration.put(lastIteration, cbpNew.averagePtTime);
			travelTimeAveragePtByIterationOriginal.put(lastIteration, cbpOriginal.averagePtTime);
			travelTimeAverageOtherByIteration.put(lastIteration, cbpNew.customVariable1/cbpNew.otherUsers);
			travelTimeAverageOtherByIterationOriginal.put(lastIteration, cbpOriginal.customVariable1/cbpOriginal.otherUsers);
			carUsersByIteration.put(lastIteration, cbpNew.carUsers);
			carUsersByIterationOriginal.put(lastIteration, cbpOriginal.carUsers);
			deltaCarUsersByIteration.put(lastIteration, cbpNew.carUsers - cbpOriginal.carUsers);
			ptUsersByIteration.put(lastIteration, cbpNew.ptUsers);
			ptUsersByIterationOriginal.put(lastIteration, cbpOriginal.ptUsers);
			deltaPtUsersByIteration.put(lastIteration, cbpNew.ptUsers - cbpOriginal.ptUsers);
			otherUsersByIteration.put(lastIteration, cbpNew.otherUsers);
			otherUsersByIterationOriginal.put(lastIteration, cbpOriginal.otherUsers);
			deltaOtherUsersByIteration.put(lastIteration, cbpNew.otherUsers - cbpOriginal.otherUsers);
			totalBenefit.put(lastIteration, cbpNew.totalAnnualBenefit);
			travelTimeGains.put(lastIteration, cbpNew.travelTimeGains);
			travelTimeGainsPt.put(lastIteration, cbpNew.travelTimeGainsPt);
			travelTimeGainsCar.put(lastIteration, cbpNew.travelTimeGainsCar);
			travelTimeGainsOther.put(lastIteration, cbpNew.customVariable2);
			otherBenefits.put(lastIteration, cbpNew.extCostSavings + cbpNew.customVariable4);
			
		}
		
	
		
		
	// ---------- AVERAGE LINES CBP - (for total result & graphs) ----------
		List<CBPII> CBPs = new ArrayList<CBPII>();
		int minIter = (int) Math.min(20.0, 1.0*maxIterations);
		for (Integer i = minIter; i<=maxIterations; i++) {
			CBPII cbpi = XMLOps.readFromFile(CBPII.class, "zurich_1pm/Evolution/Population/" + networkName + "/cbpParametersAveraged"+i+".xml");
			CBPs.add(cbpi);
		}
		CBPII cbpGlobal = CBPII.calculateAveragesX(CBPs);
		XMLOps.writeToFile(cbpGlobal, "zurich_1pm/Evolution/Population/" + networkName + "/cbpParametersAveragedGlobal.xml");
		
		
	// ---------- FOR GRAPHS ----------
		List<Double> deltaPtTimeAverage = new ArrayList<Double>();
		List<Double> deltaCarTimeAverage = new ArrayList<Double>();
		for (int iter=1; iter<=travelTimeAveragePtByIteration.size(); iter++) {
			deltaPtTimeAverage.add(100*(travelTimeAveragePtByIteration.get(iter)-travelTimeAveragePtByIterationOriginal.get(iter))/travelTimeAveragePtByIterationOriginal.get(iter));
			deltaCarTimeAverage.add(100*(travelTimeAverageCarByIteration.get(iter)-travelTimeAverageCarByIterationOriginal.get(iter))/travelTimeAverageCarByIterationOriginal.get(iter));
		}
//		Double deltaPtTimeAverageStdDev = VisualizerStdDev.sampleStandardDeviation(deltaPtTimeAverage);
//		Double deltaCarTimeAverageStdDev = VisualizerStdDev.sampleStandardDeviation(deltaCarTimeAverage);
//		Double deltaPtTimeAverageMean = VisualizerStdDev.mean(deltaPtTimeAverage);
//		Double deltaCarTimeAverageMean = VisualizerStdDev.mean(deltaCarTimeAverage);	
	
		CBPII cbpOriginalGlobal = XMLOps.readFromFile(CBPII.class, "zurich_1pm/cbpParametersOriginal/cbpParametersOriginalGlobal.xml");
		String[] originals = Log.readFile("zurich_1pm/cbpParametersOriginal/Percentiles_90.txt", Charset.defaultCharset()).split(",");
		Double travelTimeAverageCarOrigConfInterval = Double.parseDouble(originals[0]);
		Double travelTimeAveragePtOrigConfInterval = Double.parseDouble(originals[1]);
//		Double travelTimeAverageOtherOrigConfInterval = Double.parseDouble(originals[2]);
		Double meanCarTime = VisualizerStdDev.meanMap(travelTimeAverageCarByIteration);
		Double stdDevCarTime = VisualizerStdDev.getPercentileIntervalMap(travelTimeAverageCarByIteration, 90);
		Double meanPtTime = VisualizerStdDev.meanMap(travelTimeAveragePtByIteration);
		Double stdDevPtTime = VisualizerStdDev.getPercentileIntervalMap(travelTimeAveragePtByIteration, 90);
		Double meanOtherTime = VisualizerStdDev.meanMap(travelTimeAverageOtherByIteration);
		Double stdDevOtherTime = VisualizerStdDev.getPercentileIntervalMap(travelTimeAverageOtherByIteration, 90);
		
	// ---------- GRAPHS ----------		
		Visualizer.plot2D(" Change in Modal Split \r\n ",		// [#maxMATSimIter=" + maxIterations + "] 
				"MATSim Iteration", "Delta Users (Metro - Ref. Case)",
				Arrays.asList(deltaCarUsersByIteration, deltaPtUsersByIteration, deltaOtherUsersByIteration),
				Arrays.asList("Car", "PT", "Walk/Bike"), 0.0, 0.0, null,
				"DeltaModeUsersByIteration_" +censusSize + networkName + "_maxIter" + maxIterations + ".png"); // rangeAxis.setRange(-21.0E1, // 1.5E1)
		
		Visualizer.plot2D(" Change in Modal Split \r\n ",
				"MATSim Iteration", "#ModeUsers",
				Arrays.asList(carUsersByIteration, carUsersByIterationOriginal, ptUsersByIteration, ptUsersByIterationOriginal,
						otherUsersByIteration, otherUsersByIterationOriginal),
				Arrays.asList("Car - Metro Case", "Car - Ref Case", "PT - Metro Case", "PT - Ref Case", "Other - Metro Case", "Other - Ref Case"),
				0.0, 0.0, null, "ModeShareByIteration_"+censusSize + "_maxIter" + maxIterations + ".png"); // rangeAxis.setRange(-21.0E1, // 1.5E1)

//		Visualizer.plot2D(" Induced Benefits \r\n [Total Cost = "+cbpFinalBackupForCost.totalAnnualCost+"] ",
//				"MATSim Iteration", "Annual Benefit [CHF p.a.]",
//				Arrays.asList(totalBenefit, travelTimeGains, travelTimeGainsPt, travelTimeGainsCar, travelTimeGainsOther, otherBenefits),
//				Arrays.asList("Total Benefit", "Travel Gains (Time & Comfort)", "travelTimeGainsPt", "travelTimeGainsCar",
//						"travelTimeGainsWalk/Bike", "External/VehicleCostSavings"), 0.0, 0.0, null, // new Range(-2.0E8, 7.0E8), // new Range(-1.0E8, 2.5E8)
//				"BenefitsByIteration_"+censusSize + "_maxIter" + maxIterations + ".png"); // rangeAxis.setRange(-21.0E1, // 1.5E1)
		
		Visualizer.plot2D(" Induced Benefits \r\n [Total Cost = "+cbpFinalBackupForCost.totalAnnualCost+"] ",
				"MATSim Iteration", "Annual Benefit [CHF p.a.]",
				Arrays.asList(totalBenefit, travelTimeGains, otherBenefits),
				Arrays.asList("Total Benefit", "Travel Gains (Time & Comfort)", "External Cost & Vehicle Savings"), 0.0, 0.0, null, // new Range(0.0E8, 4.5E8), // new Range(-1.0E8, 2.5E8)
				"BenefitsByIteration_"+censusSize + "_maxIter" + maxIterations + ".png"); // rangeAxis.setRange(-21.0E1, // 1.5E1)
		
		Visualizer.plot2DConfIntervals(" Car Average Person Travel Time \r\n "
				+ "[Metro scenario average = " +meanCarTime+ " ],  [StdDev from ref. value = " +stdDevCarTime+" ]",
				"MATSim Iteration", "Average Travel Time Car [s]",
				Arrays.asList(travelTimeAverageCarByIteration, travelTimeAverageCarByIterationOriginal),
				Arrays.asList("Metro Case", "Reference Case"), 0.0, 0.0, new Range(2000.0, 3000.0),
				"AverageCarTravelTimeByIteration_" + censusSize + "_maxIter" + maxIterations + ".png",
				Arrays.asList(
					Arrays.asList(meanCarTime-stdDevCarTime, meanCarTime+stdDevCarTime),
					Arrays.asList(cbpOriginalGlobal.averageCartime - travelTimeAverageCarOrigConfInterval,
									cbpOriginalGlobal.averageCartime + travelTimeAverageCarOrigConfInterval))); // rangeAxis.setRange(-21.0E1, // 1.5E1)
		
		Visualizer.plot2DConfIntervals(" PT Average Travel Time \r\n "
				+ "[Metro scenario average = " +meanPtTime+ " ],  [Metro scenario StdDev from ref. value = " +stdDevPtTime+" ]",
				"MATSim Iteration", "Average Travel Time PT [s]",
				Arrays.asList(travelTimeAveragePtByIteration, travelTimeAveragePtByIterationOriginal),
				Arrays.asList("Metro Case", "Ref Case"), 0.0, 0.0, yRange,
				"AveragePtTravelTimeByIteration_" + censusSize + "_maxIter" + maxIterations + ".png",
				Arrays.asList(
					Arrays.asList(meanPtTime-stdDevPtTime, meanPtTime+stdDevPtTime),
					Arrays.asList(cbpOriginalGlobal.averagePtTime - travelTimeAveragePtOrigConfInterval,
									cbpOriginalGlobal.averagePtTime + travelTimeAveragePtOrigConfInterval))); // rangeAxis.setRange(-21.0E1, // 1.5E1)
		
//		Visualizer.plot2DConfIntervals(" AverageTravelTime \r\n "
//				+ "[Metro scenario average = " +meanOtherTime+ " ],  [StdDev from ref. value = " +stdDevOtherTime+" ]",
//				"MATSim Iteration", "AverageTravelTime [s]",
//				Arrays.asList(travelTimeAverageOtherByIteration, travelTimeAverageOtherByIterationOriginal),
//				Arrays.asList("Walk/Bike - Metro Case", "Walk/Bike - Ref Case"), 0.0, 0.0, yRange,
//				"AverageOtherTravelTimeByIteration_" + censusSize + "_maxIter" + maxIterations + ".png",
//				Arrays.asList(
//					Arrays.asList(meanOtherTime-stdDevOtherTime, meanOtherTime+stdDevOtherTime),
//					Arrays.asList(cbpOriginalGlobal.customVariable1/cbpOriginalGlobal.otherUsers - travelTimeAverageOtherOrigConfInterval,
//									cbpOriginalGlobal.customVariable1/cbpOriginalGlobal.otherUsers + travelTimeAverageOtherOrigConfInterval))); // rangeAxis.setRange(-21.0E1, // 1.5E1)
		
		Visualizer.plot2D(" Walk/Bike Average Travel Time \r\n "
				+ "[Metro scenario average = " +meanOtherTime+ " ],  [StdDev from ref. value = " +stdDevOtherTime+" ]",
				"MATSim Iteration", "Average Travel Time Walk/Bike [s]",
				Arrays.asList(travelTimeAverageOtherByIteration, travelTimeAverageOtherByIterationOriginal),
				Arrays.asList("Metro Case", "Walk/Bike - Ref Case"), 0.0, 0.0, yRange,
				"AverageOtherTravelTimeByIteration_" + censusSize + "_maxIter" + maxIterations + ".png"); // rangeAxis.setRange(-21.0E1, // 1.5E1)
		
	// Visualize developments
	//		Visualizer.plot2D(" Network Utility by MATSimIterationStage [#maxMATSimIter=" + maxIterations + "] \r\n ",
	//				"MATSim Iteration", "Annual Utility [Mio CHF]", Arrays.asList(utilityByIteration, totalCostByIteration,
	//						travelTimeBenefitCarByIteration, travelTimeBenefitPtByIteration),
	//				Arrays.asList("Total Utility - "+networkName, "Total Cost", "TravelTimeGains-Car", "TravelTimeGains-Pt"), 0.0, 0.0, null,
	//				"UtilityByIteration" + networkName + "_maxIter" + maxIterations + ".png"); // rangeAxis.setRange(-21.0E1, 1.5E1)
	//		Visualizer.plot2D(" Travel Time Gains by MATSimIterationStage [#maxMATSimIter=" + maxIterations + "] \r\n ",
	//				"MATSim Iteration", "Annual Utility of Travel Time Gains [Mio CHF]",
	//				Arrays.asList( travelTimeBenefitCarByIteration, travelTimeBenefitPtByIteration),
	//				Arrays.asList("TravelTimeGains-Car", "TravelTimeGains-Pt"), 0.0, 0.0, null,
	//				"TravelTimeGainsByIteration" + networkName + "_maxIter" + maxIterations + ".png"); // rangeAxis.setRange(-21.0E1, 1.5E1)
	}

}
