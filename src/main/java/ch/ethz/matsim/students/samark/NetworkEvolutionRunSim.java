package ch.ethz.matsim.students.samark;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
		modConfig.getModules().get("controler").addParam("writePlansInterval", "1");
		String inputNetworkFile = "Evolution/Population/BaseInfrastructure/GlobalNetwork.xml"; 
		// See old versions BEFORE 06.09.2018 for how to load specific mergedNetworks OD/Random instead of Global Network with all links
		modConfig.getModules().get("network").addParam("inputNetworkFile", inputNetworkFile);
		modConfig.getModules().get("transit").addParam("transitScheduleFile","Evolution/Population/"+mNetwork.networkID+"/MergedSchedule.xml");
		modConfig.getModules().get("transit").addParam("vehiclesFile","Evolution/Population/"+mNetwork.networkID+"/MergedVehicles.xml");
//		modConfig.getModules().get("global").addParam("numberOfThreads","1");
//		modConfig.getModules().get("parallelEventHandling").addParam("numberOfThreads","1");
//		modConfig.getModules().get("qsim").addParam("numberOfThreads","1");
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

	
	public static MNetworkPop runEventsProcessing(MNetworkPop networkPopulation, Integer lastIteration, Integer iterationsToAverage, 
			Network globalNetwork) throws IOException {
		
		for (MNetwork mNetwork : networkPopulation.networkMap.values()) {
			if(networkPopulation.modifiedNetworksInLastEvolution.contains(mNetwork.networkID)==false) {
				continue;
			}
			Log.write("  >> Running Events Processing on:  "+mNetwork.networkID);
			String networkName = mNetwork.networkID;
			
			Integer nMetroUsersTotal = 0;
			Double personMetroDistTotal = 0.0;
			
			// Average the events output over several iteration (generationsToAverage). For every generation add its performance divided by its single weight
			for (Integer thisIteration=lastIteration-iterationsToAverage+1; thisIteration<=lastIteration; thisIteration++) {
			
				// read and handle events
				String eventsFile = "zurich_1pm/Evolution/Population/"+networkName+"/Simulation_Output/ITERS/it."+thisIteration+"/"+thisIteration+".events.xml.gz";			
				MHandlerPassengers mPassengerHandler = new MHandlerPassengers();
				EventsManager eventsManager = EventsUtils.createEventsManager();
				eventsManager.addHandler(mPassengerHandler);
				MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager);
				eventsReader.readFile(eventsFile);
				
				nMetroUsersTotal += mPassengerHandler.metroPassengers.size();
				
				for (Entry<String,Double> routeEntry : mPassengerHandler.routeDistances.entrySet()) {
					System.out.println(routeEntry.toString());
					personMetroDistTotal += routeEntry.getValue();
					if (mNetwork.routeMap.containsKey(routeEntry.getKey())) {
						mNetwork.routeMap.get(routeEntry.getKey()).personMetroDist += routeEntry.getValue()/iterationsToAverage;
//						System.out.println("Added distance to route "+routeEntry.getKey().toString());
					}
				}
			} // end of averaging loop for performances
			mNetwork.nMetroUsers = nMetroUsersTotal/iterationsToAverage;
			mNetwork.personMetroDist = personMetroDistTotal/iterationsToAverage;
			
			
			
			Log.write(mNetwork.networkID+" - totalMetroPersonKM = "+mNetwork.personMetroDist/1000);
			Log.write(mNetwork.networkID+" - nMetroUsers = "+mNetwork.nMetroUsers);
		}	// END of NETWORK Loop

		return networkPopulation;
	}
	
	public static MNetworkPop peoplePlansProcessingM(MNetworkPop networkPopulation, int maxTravelTimeInSec,
			int lastIterationOriginal, int iterationsToAverage, int populationFactor) throws IOException {
		
		// PROCESSING
		// - TravelTimes (exclude unrealistic (transit_)walk legs)
		// - mRoute.personMetroDist = (exclude walking distance here)
		// - mNetwork.personMetroDist = totalMetroPersonKM;
		// - mNetwork.nMetroUsers = nMetroUsers;
		// - mNetwork.totalPtPersonDist = mPassengerHandler.totalPtTransitPersonKM;
		
		for (MNetwork mNetwork : networkPopulation.networkMap.values()) {
			String networkName = mNetwork.networkID;
			
			// CBP stats instantiation
			Double ptUsers = 0.0;
			Double carUsers = 0.0;
			Double otherUsers = 0.0;
			Double carTimeTotal = 0.0;
			Double carPersonDist = 0.0;
			Double ptTimeTotal = 0.0;
			Double ptPersonDist = 0.0;
			Double totalTravelTime = 0.0;
			Double averageTravelTime = 0.0;
			Double standardDeviation = 0.0;
			
			// Average the events output over several iteration (generationsToAverage). For every generation add its performance divided by its single weight
			for (Integer thisIteration=lastIterationOriginal-iterationsToAverage+1; thisIteration<=lastIterationOriginal; thisIteration++) {

				String finalPlansFile = "zurich_1pm/Evolution/Population/"+networkName+"/Simulation_Output/ITERS/it."+thisIteration+"/"+thisIteration+".plans.xml.gz";
				Config newConfig = ConfigUtils.createConfig();
				newConfig.getModules().get("plans").addParam("inputPlansFile", finalPlansFile);
				Scenario newScenario = ScenarioUtils.loadScenario(newConfig);
				Population finalPlansPopulation = newScenario.getPopulation();
				
				// Travel times instantiation
				Double[] travelTimeBins = new Double[maxTravelTimeInSec*60+1];
				for (int d=0; d<travelTimeBins.length; d++) {
					travelTimeBins[d] = 0.0;
				}
				// Metro stats --> see MHandler functionality in RunEventsProcessing method above
	
				for (Person person : finalPlansPopulation.getPersons().values()) {
					boolean isPtTraveler = false;
					boolean isCarTraveler = false;
					double personTravelTime = 0.0;
					Plan plan = person.getSelectedPlan();
					for (PlanElement element : plan.getPlanElements()) {
						if (element instanceof Leg) {
							Leg leg = (Leg) element;
							// do following two conditions to avoid unreasonably high (transit_)walk times!
							if (leg.getMode().equals("transit_walk") && leg.getTravelTime()>7*60.0) {
								leg.setTravelTime(7*60.0);
							}
							if (leg.getMode().equals("walk") && leg.getTravelTime()>12*60.0) {
								leg.setTravelTime(12*60.0);
							}
							if (leg.getMode().contains("car")) {
								carTimeTotal += leg.getTravelTime();
								carPersonDist += leg.getRoute().getDistance();
								isCarTraveler = true;
							}
							if (leg.getMode().contains("pt") || leg.getMode().contains("access_walk") ||
									leg.getMode().contains("transit_walk") || leg.getMode().contains("egress_walk")) {
								ptTimeTotal += leg.getTravelTime();
								ptPersonDist += leg.getRoute().getDistance();
								isPtTraveler = true;
							}
							personTravelTime += leg.getTravelTime();	// totalPersonTravelTime
						}
					}
					// travel time bins
					if (personTravelTime>=maxTravelTimeInSec) {
						travelTimeBins[maxTravelTimeInSec]++;
					}
					else {
						travelTimeBins[(int) Math.ceil(personTravelTime)]++;
					}
					// travel user type bins
					if (isCarTraveler && isPtTraveler) {
						ptUsers ++;
						carUsers ++;
					}
					else if (isCarTraveler) {
						carUsers ++;
					}
					else if (isPtTraveler) {
						ptUsers ++;
					}
					else {
						otherUsers ++;
					}
				}
				
				// time calculations and saving
				int travels = 0;
				Double thisTotalTravelTime = 0.0;
				for (int i=0; i<travelTimeBins.length; i++) {
					thisTotalTravelTime += i*travelTimeBins[i];
					travels += travelTimeBins[i];
				}
				totalTravelTime += thisTotalTravelTime;
				Double thisAverageTravelTime = thisTotalTravelTime/travels;
				averageTravelTime += thisAverageTravelTime;
				double standardDeviationInnerSum = 0.0;
				for (int i=0; i<travelTimeBins.length; i++) {
					for (int j=0; j<travelTimeBins[i]; j++) {
						standardDeviationInnerSum += Math.pow(i-thisAverageTravelTime, 2);
					}
				}
				standardDeviation += Math.sqrt(standardDeviationInnerSum/(travels-1));

			} // end of averaging processing loop
			
			// parameters have been summed up over entire loop --> have to be averaged now! 
			mNetwork.totalTravelTime = totalTravelTime/iterationsToAverage;
			mNetwork.averageTravelTime = averageTravelTime/iterationsToAverage;
			mNetwork.stdDeviationTravelTime = standardDeviation/iterationsToAverage;
			mNetwork.totalPtPersonDist = ptPersonDist/iterationsToAverage;
			ptUsers /= iterationsToAverage;
			carUsers /= iterationsToAverage;
			otherUsers /= iterationsToAverage;
			carTimeTotal /= iterationsToAverage;
			carPersonDist /= iterationsToAverage;
			ptTimeTotal /= iterationsToAverage;
			ptPersonDist /= iterationsToAverage;
			
			// calculate & save CBP stats
			CostBenefitParameters cbp = new CostBenefitParameters( populationFactor*ptUsers, populationFactor*carUsers, populationFactor*otherUsers,
					populationFactor*carTimeTotal,  populationFactor*carPersonDist,  populationFactor*ptTimeTotal,  populationFactor*ptPersonDist);
			cbp.calculateAverages();
			XMLOps.writeToFile(cbp, "zurich_1pm/Evolution/Population/"+networkName+"/cbaParameters"+lastIterationOriginal+".xml");
		} // end of networkLoop
		
		for (MNetwork network : networkPopulation.networkMap.values()) {
			System.out.println(network.networkID+" AverageTavelTime [min] = "+network.averageTravelTime/60+"   (StandardDeviation="+network.stdDeviationTravelTime/60+")");
			System.out.println(network.networkID+" TotalTravelTime [min] = "+network.totalTravelTime/60);
		}
		return networkPopulation;
	}
	
	// depreceated
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
			int generationToRecall, List<Map<String, NetworkScoreLog>> networkScoreMaps, String populationName, int populationSize, int initialRoutesPerNetwork) throws IOException {
		Log.write("%%%%%%%%%%%%%%%%%%%            %%%%%%%%%%%%%% RECALLING END STATE OF GEN=\"+ generationToRecall %%%%%%%%%%%%%%%            %%%%%%%%%%%%%%%%%%");
		Log.write(" "); Log.write(" "); Log.write(" ");
		Log.write("%%%%%%%%%%%%%%%%%%%            %%%%%%%%%%%%%% ------------------------------------------------- %%%%%%%%%%%%%%%            %%%%%%%%%%%%%%%%%%");
		metroLinkAttributes.putAll(XMLOps.readFromFile(metroLinkAttributes.getClass(), "zurich_1pm/Evolution/Population/BaseInfrastructure/metroLinkAttributes.xml"));
		File networkScoreMapsFile = new File("zurich_1pm/Evolution/Population/networkScoreMaps.xml");
		if (networkScoreMapsFile.exists()) {
			networkScoreMaps.addAll(XMLOps.readFromFile(networkScoreMaps.getClass(),"zurich_1pm/Evolution/Population/networkScoreMaps.xml"));
		}
		else {
			XMLOps.writeToFile(networkScoreMaps,"zurich_1pm/Evolution/Population/networkScoreMaps.xml");
		}
		if (networkScoreMaps.size() >= generationToRecall) {
			networkScoreMaps.removeAll(networkScoreMaps.subList(generationToRecall-1, networkScoreMaps.size()));
			// delete logs, which may have been added in a last simulation after storing the networks and could therefore be faulty
		}
		// load old pedigree tree and trim to recallGeneration (older sims do not have pedigree tree and will give an FileNotFoundException here)
		List<Map<String, String>> pedigreeTree = new ArrayList<Map<String, String>>();
		File pedigreeTreeFile = new File("zurich_1pm/Evolution/Population/HistoryLog/pedigreeTree.xml");
		if (pedigreeTreeFile.exists()) {
			pedigreeTree.addAll(XMLOps.readFromFile(pedigreeTree.getClass(),"zurich_1pm/Evolution/Population/HistoryLog/pedigreeTree.xml"));
			XMLOps.writeToFile(pedigreeTree, "zurich_1pm/Evolution/Population/HistoryLog/pedigreeTreeOldBeforeRecall.xml");
		}
		else {
			XMLOps.writeToFile(pedigreeTree,"zurich_1pm/Evolution/Population/HistoryLog/pedigreeTree.xml");
		}
		if (pedigreeTree.size() > generationToRecall-1) {
			XMLOps.writeToFile(pedigreeTree.subList(0, generationToRecall-1), "zurich_1pm/Evolution/Population/HistoryLog/pedigreeTree.xml");			
		}
		else {
			XMLOps.writeToFile(pedigreeTree, "zurich_1pm/Evolution/Population/HistoryLog/pedigreeTree.xml");			
		}
		
		
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

