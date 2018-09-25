package ch.ethz.matsim.students.samark;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.population.algorithms.TripsToLegsAlgorithm;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.pt.PtConstants;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import ch.ethz.matsim.baseline_scenario.BaselineModule;
import ch.ethz.matsim.baseline_scenario.config.CommandLine;
import ch.ethz.matsim.baseline_scenario.config.CommandLine.ConfigurationException;
import ch.ethz.matsim.baseline_scenario.traffic.BaselineTrafficModule;
import ch.ethz.matsim.baseline_scenario.transit.BaselineTransitModule;
import ch.ethz.matsim.baseline_scenario.transit.routing.DefaultEnrichedTransitRoute;
import ch.ethz.matsim.baseline_scenario.transit.routing.DefaultEnrichedTransitRouteFactory;
import ch.ethz.matsim.baseline_scenario.zurich.ZurichModule;
import ch.ethz.matsim.papers.mode_choice_paper.CustomModeChoiceModule;
import ch.ethz.matsim.papers.mode_choice_paper.utils.LongPlanFilter;

public class SimulationProcessing {

	
	
	
	public static void main(String[] args) throws IOException, ConfigurationException {
		
	// %%% START - Plots From History Log %%%
		
		int populationSize = 16;
		int initialRoutesPerNetwork = 5;
		int generationsToPlot = 23; // = nEvolutions-1
		int lastIteration = 20;
		String folderName = "ForExport/05_nEvolutionGenerations/nEvolutions100/";
		String inputFileName = folderName + "zurich_1pm/Evolution/Population/HistoryLog/";
		String outputFileName1 = folderName + "zurich_1pm/Evolution/Population/networkTravelTimesEvo.png";
		SimulationProcessing.travelTimesEvolutionMap(generationsToPlot, populationSize, initialRoutesPerNetwork, lastIteration, inputFileName, outputFileName1);
		SimulationProcessing.travelTimesEvolutionMap(generationsToPlot, populationSize, initialRoutesPerNetwork, lastIteration, inputFileName, folderName + "networkTravelTimes32GEN.png");
		String outputFileName2 = folderName + "zurich_1pm/Evolution/Population/networkScoreEvo.png";
		SimulationProcessing.scoreEvolutionMap(generationsToPlot, populationSize, initialRoutesPerNetwork, lastIteration, inputFileName, outputFileName2);
		SimulationProcessing.scoreEvolutionMap(generationsToPlot, populationSize, initialRoutesPerNetwork, lastIteration, inputFileName, folderName + "networkScoreEvo23GEN.png");
		
	// %%% END - Plots From History Log %%%
	
	
	// %%% START - Network Score vs. MATSim Iterations %%%	
		
		/*PrintWriter pw = new PrintWriter("zurich_1pm/nIterTest/log.txt");	pw.close();		// Prepare empty log file for run
		
		Map<Integer, Double> averageScores = new HashMap<Integer, Double>();
		Map<Integer, Double> averageTravelTimesScores = new HashMap<Integer, Double>();
		
		String initialRouteType = "Random";
		String initialConfig = "zurich_1pm/zurich_config.xml";
		
		MRoute mroute;
		MNetwork mNetwork = new MNetwork("Network10");
		MNetworkPop evoNetworksToProcess = new MNetworkPop("evoNetworks");
		for (int r=1; r<=5; r++) {
			mroute = XMLOps.readFromFile(MRoute.class, "zurich_1pm/nIterTest/"+mNetwork.networkID+"_Route"+r+"_RoutesFile.xml"); 	// TODO load mNetwork file here
			mNetwork.addNetworkRoute(mroute);
		}
		evoNetworksToProcess.addNetwork(mNetwork);
		
		Config nwConf = ConfigUtils.createConfig();
		nwConf.getModules().get("network").addParam("inputNetworkFile", "zurich_1pm/nIterTest/GlobalNetwork.xml");
		Network globalNetwork = ScenarioUtils.loadScenario(nwConf).getNetwork();
		
		
		int nRuns = 20;
		for (int n=1; n<nRuns+1; n++) {
			int lastIteration = n * 5;
			Log.write("zurich_1pm/nIterTest/log.txt", "Starting Simulation with nIter = "+lastIteration);
			SimulationProcessing.runNIterTest(args, mNetwork, initialRouteType, initialConfig, lastIteration);
			evoNetworksToProcess = SimulationProcessing.runEventsProcessingNIterTest(evoNetworksToProcess, lastIteration, globalNetwork);
			int maxConsideredTravelTimeInMin = 240;
			evoNetworksToProcess = SimulationProcessing.peoplePlansProcessingNIterTest(evoNetworksToProcess, maxConsideredTravelTimeInMin, lastIteration);

			String historyFileLocation = "zurich_1pm/nIterTest/nIter" + Integer.toString(lastIteration);
			String networkScoreMapGeneralLocation = "zurich_1pm/nIterTest/nIter" + Integer.toString(lastIteration) + "/networkScoreMap.xml";
			Map<String, NetworkScoreLog> networkScoreMap = new HashMap<String, NetworkScoreLog>();
			SimulationProcessing.logResultsNIterTest(networkScoreMap, historyFileLocation, networkScoreMapGeneralLocation,
					evoNetworksToProcess, 40.0, 1);
			averageScores.put(lastIteration, mNetwork.overallScore);
			averageTravelTimesScores.put(lastIteration, mNetwork.averageTravelTime);
		}
		
		

		XYLineChart chart1 = new XYLineChart("NetworkScore by nIterations", "nIterations", "Score");
		chart1.addSeries("NetworkScore", averageScores);
		chart1.saveAsPng("zurich_1pm/nIterTest/ScoreByNIter.png", 800, 600);
		
		XYLineChart chart2 = new XYLineChart("Average TravelTime by nIterations", "nIterations", "Score");
		chart2.addSeries("TravelTime", averageTravelTimesScores);
		chart2.saveAsPng("zurich_1pm/nIterTest/TravelTimeByNIter.png", 800, 600);
		
		// after simulation:
		double[] xscore = new double[20];
		double[] yscore = new double[20];
		double[] xtime = new double[20];
		double[] ytime = new double[20];
		for (int n=1; n<=20; n++) {			
			Map<String, NetworkScoreLog> scores = new HashMap<String, NetworkScoreLog>();
			scores = XMLOps.readFromFile(scores.getClass(), "ForExport/06_nIterScoreConvergence/zurich_1pm/nIterTest/nIter"+Integer.toString(5*n)+"/networkScoreMap.xml");
			xscore[n-1] = n*5.0;
			yscore[n-1] = 40.0*scores.get("Network10").overallScore;
			xtime[n-1] = n*5.0;
			ytime[n-1] = scores.get("Network10").averageTravelTime;
		}
		
		
//		XYLineChart chart3 = new XYLineChart("NetworkScore by nIterations", "nIterations", "Score");
//		chart3.addSeries("NetworkScore []", xscore, yscore);
//		chart3.saveAsPng("ForExport/06_nIterScoreConvergence/ScoreByNIterPostprocessed.png", 800, 600);
//		
//		XYLineChart chart4 = new XYLineChart("Average TravelTime by nIterations", "nIterations", "Score");
//		chart4.addSeries("TravelTime [min]", xtime, ytime);
//		chart4.saveAsPng("ForExport/06_nIterScoreConvergence/TravelTimeByNIterPostprocessed.png", 800, 600);
		
		
		XYLineChart chart5 = new XYLineChart("Average TravelTime and NetworkScore by nIterations", "nIterations", "Score");
		chart5.addSeries("TravelTime [min]", xtime, ytime);
		chart5.addSeries("NetworkScore [-]", xscore, yscore);
		chart5.saveAsPng("ForExport/06_nIterScoreConvergence/TravelTimeAndNetworkScoreByNIterPostprocessed.png", 800, 600);*/

	// %%% END - Network Score vs. MATSim Iterations %%%	

	
	}



	@SuppressWarnings("unchecked")
	public static void travelTimesEvolutionMap(int generationsToPlot, int populationSize, int initialRoutesPerNetwork, int lastIteration, 
			String inputFileName, String outputFileName) throws FileNotFoundException {
		
			Map<Integer, Double> generationsAverageTravelTime = new HashMap<Integer, Double>();
			Map<Integer, Double> generationsAverageTravelTimeStdDev = new HashMap<Integer, Double>();
			String generationPath = inputFileName + "Generation";
			Map<Integer, Double> generationsBestTravelTime = new HashMap<Integer, Double>();
			Map<String, NetworkScoreLog> networkScores = new HashMap<String, NetworkScoreLog>();
			for (int g = 1; g <= generationsToPlot; g++) {
				double averageTravelTimeThisGeneration = 0.0;
				double averageTravelTimeStdDevThisGeneration = 0.0;
				double bestAverageTravelTimeThisGeneration = Double.MAX_VALUE;
				networkScores = (Map<String, NetworkScoreLog>) XMLOps.readFromFile(networkScores.getClass(),
						generationPath + g + "/networkScoreMap.xml");
				for (NetworkScoreLog nsl : networkScores.values()) {
					if (nsl.averageTravelTime < bestAverageTravelTimeThisGeneration) {
						bestAverageTravelTimeThisGeneration = nsl.averageTravelTime;
					}
					averageTravelTimeThisGeneration += nsl.averageTravelTime / networkScores.size();
					averageTravelTimeStdDevThisGeneration += nsl.stdDeviationTravelTime / networkScores.size();
				}
				System.out.println("bestAverageTravelTimeThisGeneration = " + bestAverageTravelTimeThisGeneration);
				System.out.println("Average AverageTravelTime This Generation = " + averageTravelTimeThisGeneration);
				generationsAverageTravelTime.put(g, averageTravelTimeThisGeneration);
				generationsAverageTravelTimeStdDev.put(g, averageTravelTimeStdDevThisGeneration);
				generationsBestTravelTime.put(g, bestAverageTravelTimeThisGeneration);
			}
			XYLineChart chart = new XYLineChart("Perform. Evol. [nNetw="+populationSize+"], [nSimIter="+lastIteration+"],"
					+ "[nInitRoutes/Netw="+initialRoutesPerNetwork+"]", "Generation", "Score");
			chart.addSeries("Average Travel Time [min]", generationsAverageTravelTime);
			chart.addSeries("Average Travel Time - Std Deviation [min]", generationsAverageTravelTimeStdDev);
			chart.addSeries("Best Average Travel Time [min]", generationsBestTravelTime);
			chart.saveAsPng(outputFileName, 800, 600);
	}
	
	
	
	@SuppressWarnings("unchecked")
	public static void scoreEvolutionMap(int lastGeneration, int populationSize, int routesPerNetwork, int lastIteration, String inputFileName, String outputFileName)
			throws FileNotFoundException {
		Map<Integer, Double> generationsAverageNetworkScore = new HashMap<Integer, Double>();
		String generationPath = inputFileName + "Generation";
		Map<Integer, Double> generationsBestNetworkScore = new HashMap<Integer, Double>();
		Map<String, NetworkScoreLog> networkScores = new HashMap<String, NetworkScoreLog>();
		for (int g = 1; g <= lastGeneration; g++) {
			double averageNetworkScoreThisGeneration = 0.0;
			double bestNetworkScoreThisGeneration = 0.0;
			networkScores = (Map<String, NetworkScoreLog>) XMLOps.readFromFile(networkScores.getClass(),
					generationPath + g + "/networkScoreMap.xml");
			for (NetworkScoreLog nsl : networkScores.values()) {
				if (nsl.overallScore > bestNetworkScoreThisGeneration) {
					bestNetworkScoreThisGeneration = nsl.overallScore;
				}
				averageNetworkScoreThisGeneration += nsl.overallScore / networkScores.size();
			}
			System.out.println("Best    Network Score This Generation = " + bestNetworkScoreThisGeneration);
			System.out.println("Average Network Score This Generation = " + averageNetworkScoreThisGeneration);
			generationsAverageNetworkScore.put(g, averageNetworkScoreThisGeneration);
			generationsBestNetworkScore.put(g, bestNetworkScoreThisGeneration);
		}
		XYLineChart chart = new XYLineChart("Perform. Evol. [nNetw="+populationSize+"], [nSimIter="+lastIteration+"], [nInitRoutes/Netw="+routesPerNetwork+"]", "Generation", "Score");
		chart.addSeries("Average Network Score", generationsAverageNetworkScore);
		chart.addSeries("Best Network Score in Generation", generationsBestNetworkScore);
		chart.saveAsPng(outputFileName, 800, 600);
	}
	
	// %%%
	
	public static void runNIterTest(String[] args, MNetwork mNetwork, String initialRouteType, 
			String initialConfig, int lastIteration) throws ConfigurationException, IOException  {
				
		CommandLine cmd = new CommandLine.Builder(args)
				.allowOptions("model-type", "fallback-behaviour")
				.build();
		
		Config modConfig = ConfigUtils.loadConfig(initialConfig);
		String simulationPath = "zurich_1pm/nIterTest/nIter"+ Integer.toString(lastIteration) +"/Simulation_Output";
		new File(simulationPath).mkdirs();
		modConfig.getModules().get("controler").addParam("outputDirectory", simulationPath);
		modConfig.getModules().get("controler").addParam("overwriteFiles", "overwriteExistingFiles");
		modConfig.getModules().get("controler").addParam("lastIteration", Integer.toString(lastIteration));
		modConfig.getModules().get("controler").addParam("writeEventsInterval", "5");
		String inputNetworkFile = "nIterTest/GlobalNetwork.xml"; 
		modConfig.getModules().get("network").addParam("inputNetworkFile", inputNetworkFile);
		modConfig.getModules().get("transit").addParam("transitScheduleFile","nIterTest/MergedSchedule.xml");
		modConfig.getModules().get("transit").addParam("vehiclesFile","nIterTest/MergedVehicles.xml");
		
		Scenario modScenario = ScenarioUtils.createScenario(modConfig);
		modScenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DefaultEnrichedTransitRoute.class,
					new DefaultEnrichedTransitRouteFactory());
		ScenarioUtils.loadScenario(modScenario);	
			
		// Do this to delete initial plans in order to have same chances of success for metro as other traffic!
		TripsToLegsAlgorithm t2l = new TripsToLegsAlgorithm(new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE), new MainModeIdentifierImpl());
		for (Person person : modScenario.getPopulation().getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				for (PlanElement element : plan.getPlanElements()) {
					if (element instanceof Leg) {
						Leg leg = (Leg) element;
						leg.setRoute(null);
					}
				}
				
				t2l.run(plan);
			}
		}
			
		// filters too long plans, which bloat simulation time
		new LongPlanFilter(10, new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE)).run(modScenario.getPopulation());

		// new mode choice strategies in order to consider metro as other pt
		modConfig.strategy().clearStrategySettings();
		modConfig.strategy().setMaxAgentPlanMemorySize(1);
	    StrategySettings strategy;
	    // See MATSIM-766 (https://matsim.atlassian.net/browse/MATSIM-766)
	    strategy = new StrategySettings();
	    strategy.setStrategyName("SubtourModeChoice");
	    strategy.setDisableAfter(0);
	    strategy.setWeight(0.0);
	    modConfig.strategy().addStrategySettings(strategy);

	    strategy = new StrategySettings();
	    strategy.setStrategyName("custom");
	    strategy.setWeight(0.15);
	    modConfig.strategy().addStrategySettings(strategy);

	    strategy = new StrategySettings();
	    strategy.setStrategyName("ReRoute");
	    strategy.setWeight(0.05);
	    modConfig.strategy().addStrategySettings(strategy);

	    strategy = new StrategySettings();
	    strategy.setStrategyName("KeepLastSelected");
	    strategy.setWeight(0.85);
	    modConfig.strategy().addStrategySettings(strategy);
		
	    Controler controler = new Controler(modScenario);
		controler.addOverridingModule(new BaselineModule());
		controler.addOverridingModule(new BaselineTransitModule());
		controler.addOverridingModule(new ZurichModule());
		controler.addOverridingModule(new BaselineTrafficModule(3.0));
		controler.addOverridingModule(new CustomModeChoiceModule(cmd));
		controler.run();
		
	}
	
	public static MNetworkPop peoplePlansProcessingNIterTest(MNetworkPop networkPopulation, int maxTravelTimeInMin, int lastIteration) throws IOException {
		for (MNetwork mNetwork : networkPopulation.networkMap.values()) {
			String finalPlansFile = "zurich_1pm/nIterTest/nIter"+ Integer.toString(lastIteration) +"/Simulation_Output/output_plans.xml.gz";
			Config newConfig = ConfigUtils.createConfig();
			newConfig.getModules().get("plans").addParam("inputPlansFile", finalPlansFile);
			Scenario newScenario = ScenarioUtils.loadScenario(newConfig);
			Population finalPlansPopulation = newScenario.getPopulation();
			//PopulationReader p = new PopulationReader(emptyScenario);
			Double[] travelTimeBins = new Double[maxTravelTimeInMin+1];
			for (int d=0; d<travelTimeBins.length; d++) {
				travelTimeBins[d] = 0.0;
			}
			for (Person person : finalPlansPopulation.getPersons().values()) {
				//System.out.println("Person = "+person.getId().toString());
				double personTravelTime = 0.0;
				Plan plan = person.getSelectedPlan();
				for (PlanElement element : plan.getPlanElements()) {
						if (element instanceof Leg) {
							/*System.out.println("Plan Elements is: "+element.toString());
							System.out.println("Plan Elements Attributes are: "+element.getAttributes().toString());
							System.out.println("Plan Elements Attribute travTime is: "+element.getAttributes().getAttribute("mode"));
							System.out.println("Plan Elements Attribute travTime is: "+element.getAttributes().getAttribute("travTime"));*/
							String findString = "[travTime=";
							int i1 = element.toString().indexOf(findString);
							//System.out.println("i1 is: "+i1);
							String travTime = element.toString().substring(i1+findString.length(), i1+findString.length()+8);
							//System.out.println("Plan Elements Attribute travTime is: "+travTime);
							//System.out.println("Plan Elements Attribute travTime is: "+element.getAttributes().getAttribute("trav_time"));
							//System.out.println(element.getAttributes().getAttribute("travTime").getClass().getName());
							String[] HourMinSec = travTime.split(":");
							//System.out.println("Person Travel Time of this leg in [s] = "+travTime);
							personTravelTime += (Double.parseDouble(HourMinSec[0])*3600+Double.parseDouble(HourMinSec[1])*60+Double.parseDouble(HourMinSec[2]))/60;
							//System.out.println("Total Person Travel Time of this leg in [m] = "+personTravelTime);
						}
				}
				if (personTravelTime>=maxTravelTimeInMin) {
					travelTimeBins[maxTravelTimeInMin]++;
				}
				else {
					travelTimeBins[(int) Math.ceil(personTravelTime)]++;
				}
			}
			double totalTravelTime = 0.0;
			int travels = 0;
			for (int i=0; i<travelTimeBins.length; i++) {
				totalTravelTime += i*travelTimeBins[i];
				travels += travelTimeBins[i];
			}
			mNetwork.totalTravelTime = totalTravelTime;
			mNetwork.averageTravelTime = totalTravelTime/travels;
			Log.write("zurich_1pm/nIterTest/log.txt", "Average Travel Time = " + mNetwork.averageTravelTime);
			Log.write("zurich_1pm/nIterTest/log.txt", "Overall Network Score = " + mNetwork.overallScore);

			double standardDeviationInnerSum = 0.0;
			for (int i=0; i<travelTimeBins.length; i++) {
				for (int j=0; j<travelTimeBins[i]; j++) {
					standardDeviationInnerSum += Math.pow(i-mNetwork.averageTravelTime, 2);
				}
			}
			double standardDeviation = Math.sqrt(standardDeviationInnerSum/(travels-1));
			mNetwork.stdDeviationTravelTime = standardDeviation;

		}
		// Display Travel Time Stats
		for (MNetwork network : networkPopulation.networkMap.values()) {
			System.out.println(network.networkID+" AverageTavelTime [min] = "+network.averageTravelTime+"   (StandardDeviation="+network.stdDeviationTravelTime+")");
			System.out.println(network.networkID+" TotalTravelTime [min] = "+network.totalTravelTime);
		}
		return networkPopulation;
	}
	
	
	public static MNetworkPop runEventsProcessingNIterTest(MNetworkPop networkPopulation, int lastIteration, 
			Network globalNetwork) throws IOException {
		for (MNetwork mNetwork : networkPopulation.networkMap.values()) {
			
			// read and handle events
			String eventsFile = "zurich_1pm/nIterTest/nIter"+ Integer.toString(lastIteration) +"/Simulation_Output/ITERS/it."+lastIteration+"/"+lastIteration+".events.xml.gz";			
			
			Config config = ConfigUtils.createConfig();
			config.getModules().get("transit").addParam("transitScheduleFile","zurich_1pm/nIterTest/MergedSchedule.xml");
			TransitSchedule mergedTransitSchedule = ScenarioUtils.loadScenario(config).getTransitSchedule();
			
			MHandlerPassengers mPassengerHandler = new MHandlerPassengers(globalNetwork, mergedTransitSchedule);
			EventsManager eventsManager = EventsUtils.createEventsManager();
			eventsManager.addHandler(mPassengerHandler);
			MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager);
			eventsReader.readFile(eventsFile);
			
			// read out travel stats and display important indicators to console
			Map<String, Map<String, Double>> travelStats = mPassengerHandler.travelStats;				// Map< PersonID, Map<RouteName,TravelDistance>>
			Map<String, Integer> routeBoardingCounter = mPassengerHandler.routeBoardingCounter;			// Map<RouteName, nBoardingsOnThatRoute>
			// double totalBeelineDistance = mPassengerHandler.totalBeelineKM;
			Map<String, Double> personKMonRoutes = new HashMap<String, Double>();						// Map<RouteName, TotalPersonKM>
			double totalMetroPersonKM = 0.0;
			int nMetroUsers = travelStats.size(); 														// total number of persons who use the metro
			//System.out.println("Number of Metro Users = " + nMetroUsers);
			int nTotalBoardings = 0;
			for (int i : routeBoardingCounter.values()) {
				nTotalBoardings += i;
			}
			System.out.println("Total Metro Boardings = "+nTotalBoardings);
			
			for (Map<String, Double> routesStats : travelStats.values()) {
				for (String route : routesStats.keySet()) {
					if (personKMonRoutes.containsKey(route)) {
						personKMonRoutes.put(route, personKMonRoutes.get(route)+routesStats.get(route));
						//System.out.println("Putting on Route " +route+ " an additional " + routesStats.get(route) + " to a total of " + personKMonRoutes.get(route));  
					}
					else {
						personKMonRoutes.put(route, routesStats.get(route));
						//System.out.println("Putting on Route " +route+ " an initial " + personKMonRoutes.get(route)); 
					}
				}
			}
			
			for (String route : personKMonRoutes.keySet()) {
				totalMetroPersonKM += personKMonRoutes.get(route);
			}
			//System.out.println("Total Metro TransitKM = " + totalMetroPersonKM);
			
			// fill in performance indicators and scores in MRoutes
			for (String routeId : mNetwork.routeMap.keySet()) {
				if (personKMonRoutes.containsKey(routeId)) {					
					MRoute mRoute = mNetwork.routeMap.get(routeId);
					mRoute.personMetroKM = personKMonRoutes.get(routeId);
					mRoute.nBoardings = routeBoardingCounter.get(routeId);
					mNetwork.routeMap.put(routeId, mRoute);
				}
			}

			// fill in performance indicators and scores in MNetworks
			// TODO [NOT PRIO] mNetwork.mPersonKMdirect = beelinedistances;
			mNetwork.totalMetroPersonKM = totalMetroPersonKM;
			mNetwork.nMetroUsers = nMetroUsers;
			mNetwork.totalPtTransitPersonKM = mPassengerHandler.totalPtTransitPersonKM;
		}	// END of NETWORK Loop

		// - Maybe hand over score to a separate score map for sorting scores
		return networkPopulation;
	}
	
	public static boolean logResultsNIterTest(Map<String, NetworkScoreLog> networkScoreMap, String historyFileLocation,
			String networkScoreMapGeneralLocation, MNetworkPop latestPopulation, double averageTravelTimePerformanceGoal, int finalGeneration) throws IOException {
		
		boolean performanceGoalAccomplished = false;
		new File(historyFileLocation).mkdirs();
		
		MNetwork successfulNetwork = null;
		double successfulAverageTravelTime = 0.0;
		
		for (String networkName : latestPopulation.getNetworks().keySet()) {
			MNetwork mnetwork = latestPopulation.getNetworks().get(networkName);
			mnetwork.calculateTotalRouteLengthAndDrivenKM();
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
			NetworkScoreLog nsl = new NetworkScoreLog();
			nsl.NetworkScore2LogMap(mnetwork);			// copy network parameters to network score log for storing evolution
			networkScoreMap.put(networkName, nsl);		// network score map is finally stored
			//Log.writeAndDisplay("   >>> "+mnetwork.networkID+": Average Travel Time = " + mnetwork.averageTravelTime);
			
			// mnetwork.network = null;		// set to null before storing to file bc would use up too much storage and is not needed (network can be created from other data)
		}
		XMLOps.writeToFile(networkScoreMap, historyFileLocation+"/networkScoreMap.xml");
		
		
		if (performanceGoalAccomplished == true) {
			System.out.println("Performance Goal has been achieved in Generation " +finalGeneration+ " by Network "+successfulNetwork.networkID+" at averageTravelTime "+successfulAverageTravelTime);			// display most important analyzed data here			
			Log.write("PERFORMANCE GOAL ACHIEVED: in Generation " +finalGeneration+ " by Network "+successfulNetwork.networkID+" at averageTravelTime "+successfulAverageTravelTime);
			return true;
		}
		else {
			return false;
		}
	}
	
}
