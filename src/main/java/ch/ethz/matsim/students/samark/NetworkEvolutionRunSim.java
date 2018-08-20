package ch.ethz.matsim.students.samark;

import java.io.File;
import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.population.algorithms.TripsToLegsAlgorithm;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.PtConstants;

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

public class NetworkEvolutionRunSim {

	public static void run(String[] args, MNetwork mNetwork, String initialRouteType, 
			String initialConfig, int lastIteration) throws ConfigurationException  {
		
		CommandLine cmd = new CommandLine.Builder(args)
				.allowOptions("model-type", "fallback-behaviour")
				.build();
		
		Config modConfig = ConfigUtils.loadConfig(initialConfig);
		String simulationPath = "zurich_1pm/Evolution/Population/"+mNetwork.networkID+"/Simulation_Output";
		new File(simulationPath).mkdirs();
		modConfig.getModules().get("controler").addParam("outputDirectory", simulationPath);
		modConfig.getModules().get("controler").addParam("overwriteFiles", "overwriteExistingFiles");
		modConfig.getModules().get("controler").addParam("lastIteration", Integer.toString(lastIteration));
		modConfig.getModules().get("controler").addParam("writeEventsInterval", "1");
		String inputNetworkFile = "";
		if (initialRouteType.equals("OD")) {
			inputNetworkFile = "Evolution/Population/"+mNetwork.networkID+"/MergedNetworkODInitialRoutes.xml";
		}
		else if (initialRouteType.equals("Random")) {
			inputNetworkFile = "Evolution/Population/"+mNetwork.networkID+"/MergedNetworkRandomInitialRoutes.xml";
		}
		else {
			System.out.println("ERROR: Do not know which inputNetwork to simulate. "
					+ "Please specify in initialNetworkType and make sure such a network file actually exists!");
		}
		modConfig.getModules().get("network").addParam("inputNetworkFile", inputNetworkFile);
		modConfig.getModules().get("transit").addParam("transitScheduleFile","Evolution/Population/"+mNetwork.networkID+"/MergedSchedule.xml");
		modConfig.getModules().get("transit").addParam("vehiclesFile","Evolution/Population/"+mNetwork.networkID+"/MergedVehicles.xml");
		ConfigWriter configWriter = new ConfigWriter(modConfig);
		configWriter.write("zurich_1pm/Evolution/Population/"+mNetwork.networkID+"/modifiedConfig.xml");
		
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

	
	public static void runEventsProcessing(MNetworkPop networkPopulation, int lastIteration) {
		for (MNetwork mNetwork : networkPopulation.networkMap.values()) {
			String networkName = mNetwork.networkID;
			
			// read and handle events
			String eventsFile = "zurich_1pm/Evolution/Population/"+networkName+"/Simulation_Output/ITERS/it."+lastIteration+"/"+lastIteration+".events.xml.gz";			
			MHandlerPassengers mPassengerHandler = new MHandlerPassengers();
			EventsManager eventsManager = EventsUtils.createEventsManager();
			eventsManager.addHandler(mPassengerHandler);
			MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager);
			eventsReader.readFile(eventsFile);
			
			// read out travel stats and display important indicators to console
			Map<String, Map<Double, String>> personStats = mPassengerHandler.personStats;				// Map<PersonID, Map<BoardingTime, RouteName>>
			Map<String, Double> transitPersonKM = mPassengerHandler.transitPersonKM;					// Map<RouteName, TotalPersonKM>
			Map<String, Integer> routeBoardingCounter = mPassengerHandler.routeBoardingCounter;			// Map<RouteName, nBoardingsOnThatRoute>
			int nPassengers = personStats.size(); 														// total number of persons who use the metro
			System.out.println("Number of Metro Users = "+nPassengers);
			int nTotalBoardings = 0;
			for (int i : routeBoardingCounter.values()) {
				nTotalBoardings += i;
			}
			System.out.println("Total Metro Boardings = "+nTotalBoardings);
			double totalPersonKM = 0.0;
			for (Double d : transitPersonKM.values()) {
				totalPersonKM += d;
			}
			System.out.println("Total Metro PersonKM = "+Double.toString(totalPersonKM));

			// fill in performance indicators and scores in MRoutes
			for (String routeId : mNetwork.routeMap.keySet()) {
				MRoute mRoute = mNetwork.routeMap.get(routeId);
				mRoute.personKM = transitPersonKM.get(routeId);
				mRoute.nBoardings = routeBoardingCounter.get(routeId);
				mNetwork.routeMap.put(routeId, mRoute);
			}
			
			// fill in performance indicators and scores in MNetworks
			// TODO [NOT PRIO] mNetwork.mPersonKMdirect = beelinedistances;
			mNetwork.personKM = totalPersonKM;
			mNetwork.nPassengers = nPassengers;
			
			String finalPlansFile = "zurich_1pm/Evolution/Population/"+networkName+"/Simulation_Output/output_plans.xml.gz";			
			Config emptyConfig = ConfigUtils.createConfig();
			emptyConfig.getModules().get("network").addParam("inputPlansFile", finalPlansFile);
			Scenario emptyScenario = ScenarioUtils.createScenario(emptyConfig);
			Population finalPlansPopulation = emptyScenario.getPopulation();
			Double[] travelTimeBins = new Double[90+1];
			for (Person person : finalPlansPopulation.getPersons().values()) {
				double personTravelTime = 0.0;
				Plan plan = person.getSelectedPlan();
				for (PlanElement element : plan.getPlanElements()) {
						if (element instanceof Leg) {
							System.out.println(element.getAttributes().getAttribute("travTime").getClass().getName());
							String[] HourMinSec = element.getAttributes().getAttribute("travTime").toString().split(":");
							personTravelTime += (1/60)*(Double.parseDouble(HourMinSec[0])*3600+Double.parseDouble(HourMinSec[1])*60+Double.parseDouble(HourMinSec[2]));
						}
				}
				if (personTravelTime>=90) {
					travelTimeBins[90]++;
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
			double standardDeviation = 0.0;
			for (int i=0; i<travelTimeBins.length; i++) {
				for (int j=0; j<travelTimeBins[i]; j++) {
					standardDeviation += Math.pow(i-mNetwork.averageTravelTime, 2);
				}
			}
			mNetwork.stdDeviationTravelTime = standardDeviation;
			
		}		// END of NETWORK Loop
		
		// TEST
		for (MNetwork mNetwork : networkPopulation.networkMap.values()) {
			System.out.println(mNetwork.networkID+" AverageTavelTime = "+mNetwork.averageTravelTime+"   (StandardDeviation="+mNetwork.stdDeviationTravelTime+")");
			System.out.println(mNetwork.networkID+" TotalTravelTime = "+mNetwork.totalTravelTime);
		}
		// - Maybe hand over score to a separate score map for sorting scores
	}
	
}


