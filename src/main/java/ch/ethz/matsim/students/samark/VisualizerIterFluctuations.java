package ch.ethz.matsim.students.samark;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

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

		// args: Network1 1 30 1 1000 false true 1
		String networkName = args[0]; // 4,8,9
		Integer generationNr = Integer.parseInt(args[1]);
		Integer maxIterations = Integer.parseInt(args[2]);
		Integer iterationsToAverage = Integer.parseInt(args[3]);
		Integer maxConsideredTravelTimeInSec = 240 * 60;
		Integer populationFactor = Integer.parseInt(args[4]);
		Boolean recalculateOriginalCBP = Boolean.parseBoolean(args[5]);
		Boolean recalculateNewCBP = Boolean.parseBoolean(args[6]);
		String utilityFunctionSelection = args[7];
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
		
		Map<Integer, Double> carUsersByIteration = new HashMap<Integer, Double>();
		Map<Integer, Double> carUsersByIterationOriginal = new HashMap<Integer, Double>();
		Map<Integer, Double> deltaCarUsersByIteration = new HashMap<Integer, Double>();
		Map<Integer, Double> ptUsersByIteration = new HashMap<Integer, Double>();
		Map<Integer, Double> ptUsersByIterationOriginal = new HashMap<Integer, Double>();
		Map<Integer, Double> deltaPtUsersByIteration = new HashMap<Integer, Double>();

		for (Integer lastIteration = 1; lastIteration < maxIterations; lastIteration++) {

			String plansFolder = "zurich_1pm/Zurich_1pm_SimulationOutputEnriched/ITERS";
			String outputFile = "zurich_1pm/cbpParametersOriginal/cbpParametersOriginal" + lastIteration + ".xml";
			CostBenefitParameters cbpOriginal;
			if (!(new File(outputFile)).exists() || recalculateOriginalCBP.equals(true)) {
				if (lastIteration < iterationsToAverage) { // then use all available (=lastIteration) for averaging
					cbpOriginal = NetworkEvolutionImpl.calculateCBAStats(plansFolder, outputFile,
							(int) populationFactor, lastIteration, lastIteration);									
				}
				else {
					cbpOriginal = NetworkEvolutionImpl.calculateCBAStats(plansFolder, outputFile,
							(int) populationFactor, lastIteration, iterationsToAverage);				
				}
			}
			else {
				cbpOriginal = XMLOps.readFromFile(CostBenefitParameters.class, outputFile);
			}

			MNetwork mNetwork = new MNetwork(networkName); // TODO choose which network
			for (int r = 1; r < 20; r++) {
				File routesFile = new File("zurich_1pm/Evolution/Population/HistoryLog/Generation" + generationNr
						+ "/MRoutes/" + networkName + "_Route" + r + "_RoutesFile.xml");
				if (routesFile.exists()) {
					mNetwork.addNetworkRoute(XMLOps.readFromFile(MRoute.class, routesFile.toString()));
				}
			}
			MNetworkPop latestPopulation = new MNetworkPop();
			latestPopulation.addNetwork(mNetwork);
			latestPopulation.modifiedNetworksInLastEvolution.add(networkName);

			
			if (!(new File("zurich_1pm/Evolution/Population/" + networkName + "/cbpParameters" + lastIteration + ".xml")).exists() || recalculateNewCBP.equals(true)) {
				if (lastIteration < iterationsToAverage) { // then use all available (=lastIteration) for averaging
					NetworkEvolutionRunSim.runEventsProcessing(latestPopulation, lastIteration, lastIteration,
							globalNetwork, "zurich_1pm/Evolution/Population/");
					NetworkEvolutionRunSim.peoplePlansProcessingM(latestPopulation, maxConsideredTravelTimeInSec,
							lastIteration, lastIteration, populationFactor, "zurich_1pm/Evolution/Population/");
				} else { // enough iterations to average over all iterationsToAverage
					NetworkEvolutionRunSim.runEventsProcessing(latestPopulation, lastIteration, iterationsToAverage,
							globalNetwork, "zurich_1pm/Evolution/Population/");
					NetworkEvolutionRunSim.peoplePlansProcessingM(latestPopulation, maxConsideredTravelTimeInSec,
							lastIteration, iterationsToAverage, populationFactor, "zurich_1pm/Evolution/Population/");
					// calculates cbp stats here already!
				}
			}
			
			Log.write("LOGGING SCORES of GEN" + generationNr + ":");
			mNetwork.lifeTime = lifeTime;
			mNetwork.calculateRoutesAndNetworkScore(lastIteration, populationFactor, globalNetwork, metroLinkAttributes,
					"zurich_1pm/cbpParametersOriginal/", "zurich_1pm/Evolution/Population/", utilityFunctionSelection);

			// CostBenefitParameters cbpOriginal = // calculated already at the top!!
			// XMLOps.readFromFile((new CostBenefitParameters()).getClass(),
			// "zurich_1pm/cbpParametersOriginal"+lastIteration+".xml");
			CostBenefitParameters cbpNew = XMLOps.readFromFile((new CostBenefitParameters()).getClass(),
					"zurich_1pm/Evolution/Population/" + networkName + "/cbpParameters" + lastIteration + ".xml");

			utilityByIteration.put(lastIteration, mNetwork.overallScore);
			totalCostByIteration.put(lastIteration, mNetwork.constructionCost+mNetwork.operationalCost);
			travelTimeBenefitCarByIteration.put(lastIteration, mNetwork.travelTimeGainsCar);
			travelTimeBenefitPtByIteration.put(lastIteration, mNetwork.travelTimeGainsPT);
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
				"MATSim Iteration", "Delta #TransportModeUsers (MetroCase-Default1pmCase)",
				Arrays.asList(deltaCarUsersByIteration, deltaPtUsersByIteration),
				Arrays.asList("TransportMode = Car", "TransportMode = PT"), 0.0, 0.0, null,
				"DeltaModeUsersByIteration" + networkName + "_maxIter" + maxIterations + ".png"); // rangeAxis.setRange(-21.0E1, // 1.5E1)

	}

}
