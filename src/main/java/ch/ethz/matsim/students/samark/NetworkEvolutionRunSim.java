package ch.ethz.matsim.students.samark;

import java.io.File;
import java.util.HashMap;
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
import org.matsim.core.population.io.PopulationReader;
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
			//System.out.println("Total Metro Boardings = "+nTotalBoardings);
			
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
		}		// END of NETWORK Loop

		// - Maybe hand over score to a separate score map for sorting scores
	}
	
	public static void peoplePlansProcessing(MNetworkPop networkPopulation) {
		//System.out.println("Population name = "+networkPopulation.populationId);
		//System.out.println("Population size = "+networkPopulation.networkMap.size());
		for (MNetwork mNetwork : networkPopulation.networkMap.values()) {
			// TEST			
			String networkName = mNetwork.networkID;
			//System.out.println("NetworkName = "+networkName);
			String finalPlansFile = "zurich_1pm/Evolution/Population/"+networkName+"/Simulation_Output/output_plans.xml.gz";			
			Config emptyConfig = ConfigUtils.createConfig();
			emptyConfig.getModules().get("plans").addParam("inputPlansFile", finalPlansFile);
			Scenario emptyScenario = ScenarioUtils.loadScenario(emptyConfig);
			Population finalPlansPopulation = emptyScenario.getPopulation();
			//PopulationReader p = new PopulationReader(emptyScenario);
			Double[] travelTimeBins = new Double[90+1];
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
			double standardDeviationInnerSum = 0.0;
			for (int i=0; i<travelTimeBins.length; i++) {
				for (int j=0; j<travelTimeBins[i]; j++) {
					standardDeviationInnerSum += Math.pow(i-mNetwork.averageTravelTime, 2);
				}
			}
			double standardDeviation = Math.sqrt(standardDeviationInnerSum/(travels-1));
			
			mNetwork.stdDeviationTravelTime = standardDeviation;
			//System.out.println("standardDeviation = " + mNetwork.stdDeviationTravelTime);
			//System.out.println("averageTravelTime = " + mNetwork.averageTravelTime);
			
		}
		for (MNetwork network : networkPopulation.networkMap.values()) {
			System.out.println(network.networkID+" AverageTavelTime [min] = "+network.averageTravelTime+"   (StandardDeviation="+network.stdDeviationTravelTime+")");
			System.out.println(network.networkID+" TotalTravelTime [min] = "+network.totalTravelTime);
		}
	}
	
	
}


