package ch.ethz.matsim.students.samark;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
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

public class NetworkEvolutionRunSim {

	public static void run(String[] args, MNetwork mNetwork, String initialRouteType, 
			String initialConfig, int lastIteration) throws ConfigurationException, IOException  {
		
		Log.write("  >> Running MATSim simulation on:  "+mNetwork.networkID);
		
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
		String inputNetworkFile = "Evolution/Population/GlobalNetwork.xml"; 
		// See old versions BEFORE 06.09.2018 for how to load specific mergedNetworks OD/Random instead of Global Network with all links
		modConfig.getModules().get("network").addParam("inputNetworkFile", inputNetworkFile);
		modConfig.getModules().get("transit").addParam("transitScheduleFile","Evolution/Population/"+mNetwork.networkID+"/MergedSchedule.xml");
		modConfig.getModules().get("transit").addParam("vehiclesFile","Evolution/Population/"+mNetwork.networkID+"/MergedVehicles.xml");
		// See old versions BEFORE 06.09.2018 for how to write config to file (not necessary)
		
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

	
	public static MNetworkPop runEventsProcessing(MNetworkPop networkPopulation, int lastIteration, 
			Network globalNetwork) throws IOException {
		for (MNetwork mNetwork : networkPopulation.networkMap.values()) {
			if(networkPopulation.modifiedNetworksInLastEvolution.contains(mNetwork.networkID)==false) {
				continue;
			}
			Log.write("  >> Running Events Processing on:  "+mNetwork.networkID);
			String networkName = mNetwork.networkID;

			
			// read and handle events
			String eventsFile = "zurich_1pm/Evolution/Population/"+networkName+"/Simulation_Output/ITERS/it."+lastIteration+"/"+lastIteration+".events.xml.gz";			
			
			Config config = ConfigUtils.createConfig();
			config.getModules().get("transit").addParam("transitScheduleFile", "zurich_1pm/Evolution/Population/"+networkName+"/MergedSchedule.xml");
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
			Log.write(mNetwork.networkID+" - totalPersonKM = "+totalMetroPersonKM);
			Log.write(mNetwork.networkID+" - nMetroUsers = "+nMetroUsers);
			Log.write(mNetwork.networkID+" - totalPtTransitPersonKM = "+mNetwork.totalPtTransitPersonKM);
		}	// END of NETWORK Loop

		// - Maybe hand over score to a separate score map for sorting scores
		return networkPopulation;
	}
	
	public static MNetworkPop peoplePlansProcessingM(MNetworkPop networkPopulation, int maxTravelTimeInMin) {
		for (MNetwork mNetwork : networkPopulation.networkMap.values()) {
			String networkName = mNetwork.networkID;
			String finalPlansFile = "zurich_1pm/Evolution/Population/"+networkName+"/Simulation_Output/output_plans.xml.gz";			
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
	
	public static NetworkScoreLog peoplePlansProcessingStandard(String finalOutputPlansFile, int maxTravelTimeInMin) {
			Config emptyConfig = ConfigUtils.createConfig();
			emptyConfig.getModules().get("plans").addParam("inputPlansFile", finalOutputPlansFile);
			Scenario emptyScenario = ScenarioUtils.loadScenario(emptyConfig);
			Population finalPlansPopulation = emptyScenario.getPopulation();	// the population is contained within the outputPlans and retrieved by the inputPlansFile config parameter
			Double[] travelTimeBins = new Double[maxTravelTimeInMin+1];
			for (int d=0; d<travelTimeBins.length; d++) {
				travelTimeBins[d] = 0.0;
			}
			for (Person person : finalPlansPopulation.getPersons().values()) {
				double personTravelTime = 0.0;
				Plan plan = person.getSelectedPlan();
				for (PlanElement element : plan.getPlanElements()) {
						if (element instanceof Leg) {
							String findString = "[travTime=";
							int i1 = element.toString().indexOf(findString);
							String travTime = element.toString().substring(i1+findString.length(), i1+findString.length()+8);
							String[] HourMinSec = travTime.split(":");
							personTravelTime += (Double.parseDouble(HourMinSec[0])*3600+Double.parseDouble(HourMinSec[1])*60+Double.parseDouble(HourMinSec[2]))/60;
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
			
			NetworkScoreLog nsl = new NetworkScoreLog();
			nsl.totalTravelTime = totalTravelTime;
			nsl.averageTravelTime = totalTravelTime/travels;
			double standardDeviationInnerSum = 0.0;
			for (int i=0; i<travelTimeBins.length; i++) {
				for (int j=0; j<travelTimeBins[i]; j++) {
					standardDeviationInnerSum += Math.pow(i-nsl.averageTravelTime, 2);
				}
			}
			double standardDeviation = Math.sqrt(standardDeviationInnerSum/(travels-1));
			
			nsl.stdDeviationTravelTime = standardDeviation;
			//System.out.println("standardDeviation = " + mNetwork.stdDeviationTravelTime);
			//System.out.println("averageTravelTime = " + mNetwork.averageTravelTime);
			
			System.out.println(" AverageTavelTime [min] = "+nsl.averageTravelTime+"   (StandardDeviation="+nsl.stdDeviationTravelTime+")");
			System.out.println(" TotalTravelTime [min] = "+nsl.totalTravelTime);
		return nsl;
	}


	@SuppressWarnings("unchecked")
	public static void recallSimulation(MNetworkPop latestPopulation, Map<Id<Link>, CustomMetroLinkAttributes> metroLinkAttributes,
			int generationToRecall, String populationName, int populationSize, int initialRoutesPerNetwork) throws IOException {
		Log.write("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%" + "\r\n" + "RECALLING END STATE OF GEN="+ generationToRecall + "\r\n" + "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
		metroLinkAttributes = XMLOps.readFromFile(metroLinkAttributes.getClass(), "zurich_1pm/Evolution/Population/MetroLinkAttributes.xml");
		for (int n=1; n<=populationSize; n++) {
			MNetwork loadedNetwork = new MNetwork("Network"+n);
			latestPopulation.modifiedNetworksInLastEvolution.add(loadedNetwork.networkID);
			Log.write("Added Network to ModifiedInLastGeneration = "+ loadedNetwork.networkID);
			for (int r=1; r<=initialRoutesPerNetwork; r++) {
				String routeFilePath =
						"zurich_1pm/Evolution/Population/HistoryLog/Generation"+generationToRecall+"/MRoutes/"+loadedNetwork.networkID+"_Route"+r+"_RoutesFile.xml";
				File f = new File(routeFilePath);
				if (f.exists()) {
					MRoute loadedRoute = XMLOps.readFromFile(MRoute.class, routeFilePath);
					loadedNetwork.addNetworkRoute(loadedRoute);
					Log.write("Adding network route "+loadedRoute.routeID);
				}
			}
			// copy MergedSchedule/Vehicles from HistoryLog (archived) to the active working directory of the network as per the paths below
			File sourceSchedule = new File("zurich_1pm/Evolution/Population/HistoryLog/Generation"+(generationToRecall)+"/"+loadedNetwork.networkID+"/MergedSchedule.xml");
			File destSchedule = new File("zurich_1pm/Evolution/Population/"+loadedNetwork.networkID+"/MergedSchedule.xml"); 
			File sourceVehicles = new File("zurich_1pm/Evolution/Population/HistoryLog/Generation"+(generationToRecall)+"/"+loadedNetwork.networkID+"/MergedVehicles.xml");		
			File destVehicles = new File("zurich_1pm/Evolution/Population/"+loadedNetwork.networkID+"/MergedVehicles.xml");
			if (sourceSchedule.exists() && sourceVehicles.exists()) {
				try {
					FileUtils.copyFile(sourceSchedule, destSchedule);
					FileUtils.copyFile(sourceVehicles, destVehicles);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			else {
				Log.write("ERROR: Either MergedSchedule or MergedVehicles does not exist in HistoryLog. Please check folder or choose another generationToRecall.");
				Log.write("Cannot proceed. Terminating ...");
				System.exit(0);
			}
			latestPopulation.addNetwork(loadedNetwork);
		}
		
	}
	
}

