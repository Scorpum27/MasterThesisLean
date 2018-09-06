
package ch.ethz.matsim.students.samark;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.algorithms.TripsToLegsAlgorithm;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.PtConstants;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.vehicles.VehicleType;
import com.google.common.collect.Sets;

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


/* %%% COMMENTS %%%
 * - network is so small that walking was always preferred to pt because pt was set to very slow speed
 * 		--> two solutions: (1) convert to another coordinate system where every link has length 100.0 instead of 1.0 or (2) change walk speed to a 100th of before i.e. 0.008333
 * - Strange population file person name output comes from VC_PublicTransportImpl.java: Vehicle vehicle = scenario.getVehicles().getFactory().createVehicle(Id.createVehicleId(transitRoute.getId().toString()+"_"+vehicleType.getId().toString()+"_"+d), vehicleType);
 */

public class Run_VirtualCity {

	static public void main(String[] args) throws FileNotFoundException, ConfigurationException {
		int populationSize = 2;														// how many networks should be developed in parallel
		String populationName = "evoNetworks";

		final int XMax = 30;													// set network size in West-to-East
		final int YMax = 30;													// set network size in South-to-North	
		int removalPercentage = 30;
		int outerFramePercentage = 40;											// take only nodes on outer 50% of network (specify frame as outer 50% of network nodes)
		int minSpacingPercentage = 60;											// minimum spacing requirement between start and end node of a route so that sample routes are not too short!
		int routesPerNetwork = 10;	
		
		String zeroLog = "zurich_1pm/VirtualCity/Population/HistoryLog/Generation0";	// Make string and directory to save to file first generation (Generation0)
		new File(zeroLog).mkdirs();

		String defaultPtMode = "bus";
		boolean blocksLane = false;
		double stopTime = 1.0; 					// stop duration for vehicle in [seconds]
		String vehicleTypeName = "magicalBusType";
		double vehicleLength = 15;
		double maxVelocity = 50/3.6;
		int vehicleSeats = 15;
		int vehicleStandingRoom = 15;
		int nDepartures = 120;
		double firstDepTime = 6.0*60*60;
		double departureSpacing = 7.5*60;
		double lastDepTime = firstDepTime+nDepartures*departureSpacing;
		
		int nNewPeople = 200;
		
		MNetworkPop networkPopulation = new MNetworkPop(populationName, populationSize);		// Initialize population of networks
		for (int N=1; N<=populationSize; N++) {													// Make individual networks one by one in loop
			String thisNewNetworkName = ("Network"+N);											// Name networks by their number [1;populationSize]
			MNetwork mNetwork = createMNetworkRoutes(											// Make a list of routes that will be added to this network
					thisNewNetworkName, routesPerNetwork, XMax, YMax, removalPercentage, outerFramePercentage,
					minSpacingPercentage, vehicleTypeName, vehicleLength, maxVelocity, vehicleSeats, vehicleStandingRoom,
					defaultPtMode, blocksLane, stopTime, maxVelocity, firstDepTime, lastDepTime, departureSpacing, nDepartures);	// metroOpsCostPerKM, metroConstructionCostPerKmOverground, metroConstructionCostPerKmUnderground
			XMLOps.writeToFile(mNetwork, "zurich_1pm/VirtualCity/Population/"+thisNewNetworkName+"/Objects/"+mNetwork.networkID+".xml");
			networkPopulation.addNetwork(mNetwork);
			XMLOps.writeToFile(networkPopulation, "zurich_1pm/VirtualCity/Population/"+networkPopulation.populationId+".xml");
			// initial data logging for generationNr=0;
			XMLOps.writeToFile(mNetwork, zeroLog +"/"+mNetwork.networkID+".xml");
		}
		
		// create population by means of population factory
		double networkSize = Math.sqrt(XMax*XMax+YMax*YMax);
		String populationPrefix = "Pop";
		VC_ScenarioImpl demandCreator = new VC_ScenarioImpl();
		System.out.println("networkSize is: "+networkSize);
		Population population = demandCreator.createNewDemand(ScenarioUtils.createScenario(ConfigUtils.createConfig()), networkPopulation.networkMap.values().iterator().next().network, networkSize, nNewPeople, populationPrefix);

		PopulationWriter populationWriter = new PopulationWriter(population);		// Write new population to new file >> change config after that to new network name!
		populationWriter.write("zurich_1pm/VirtualCity/Input/Generated_Population/Plans"+nNewPeople+".xml");
		
		// EVOLUTIONARY PROCESS
		int nEvolutions = 2;
		double averageTravelTimePerformanceGoal = 0.0;
		MNetwork successfulNetwork = null;
		double successfulAverageTravelTime = 0.0;
		for (int generationNr = 1; generationNr<=nEvolutions; generationNr++) {
			int finalGeneration = generationNr;
			
			// SIMULATION LOOP:
			int lastIterationSim = generationNr;
			MNetworkPop vcNetworksToSimulate = XMLOps.readFromFile(new MNetworkPop().getClass(), "zurich_1pm/VirtualCity/Population/"+populationName+".xml");
			for (MNetwork mNetwork : vcNetworksToSimulate.getNetworks().values()) {
				String initialConfig = "zurich_1pm/zurich_config.xml";
				Run_VirtualCity.run(args, mNetwork, initialConfig, lastIterationSim, XMax, YMax, removalPercentage, nNewPeople);
			} // End Network Simulation Loop 

			// - EVENTS PROCESSING:
			int lastEventIteration = lastIterationSim; // CAUTION: make sure it is not higher than lastIteration above resp. the last simulated iteration!
			MNetworkPop evoNetworksToProcess = XMLOps.readFromFile(new MNetworkPop().getClass(), "zurich_1pm/VirtualCity/Population/"+populationName+".xml");
			VC_NetworkImpl.runEventsProcessing(evoNetworksToProcess, lastEventIteration);
			XMLOps.writeToFile(evoNetworksToProcess, "zurich_1pm/VirtualCity/Population/"+evoNetworksToProcess.populationId+".xml");
			// - PLANS PROCESSING:
			MNetworkPop vcNetworksToProcessPlans = XMLOps.readFromFile(new MNetworkPop().getClass(), "zurich_1pm/VirtualCity/Population/"+populationName+".xml");
			int maxConsideredTravelTimeInMin = 240;
			VC_NetworkImpl.peoplePlansProcessingM(vcNetworksToProcessPlans, maxConsideredTravelTimeInMin);
			// - HISTORY LOGGER: hand over score to a separate score map for sorting scores	and store most important data of each iteration	
			String historyFileLocation = "zurich_1pm/VirtualCity/Population/HistoryLog/Generation"+generationNr;
			new File(historyFileLocation).mkdirs();
			Map<String, NetworkScoreLog> networkScoreMap = new HashMap<String, NetworkScoreLog>();
			boolean performanceGoalAccomplished = false;
			for (String networkNameString : vcNetworksToProcessPlans.getNetworks().keySet()) {
				MNetwork mnetwork = vcNetworksToProcessPlans.getNetworks().get(networkNameString);
				NetworkScoreLog nsl = new NetworkScoreLog();
				nsl.averageTravelTime = mnetwork.averageTravelTime;
				if (performanceGoalAccomplished == false) {
					if (nsl.averageTravelTime < averageTravelTimePerformanceGoal) {
						performanceGoalAccomplished = true;
						successfulNetwork = mnetwork;
						successfulAverageTravelTime = nsl.averageTravelTime;
					}				
				}
				if (performanceGoalAccomplished == true) {
					if (nsl.averageTravelTime < successfulAverageTravelTime) {
						successfulAverageTravelTime = nsl.averageTravelTime;
						successfulNetwork = mnetwork;
					}				
				}
				nsl.stdDeviationTravelTime = mnetwork.stdDeviationTravelTime;
				networkScoreMap.put(networkNameString, nsl);
				System.out.println("Network Stats: " + mnetwork.networkID);			// display most important analyzed data here
				System.out.println("Average Travel Time = " + mnetwork.averageTravelTime);
				System.out.println("Number of Metro Users = " + mnetwork.nMetroUsers);
				System.out.println("Total Metro Passengers KM = " + mnetwork.totalMetroPersonKM);
				mnetwork.network = null;		// set to null before storing to file bc would use up too much storage and is not needed (network can be created from other data)
				XMLOps.writeToFile(mnetwork, historyFileLocation+"/"+mnetwork.networkID+".xml");
			}
			XMLOps.writeToFile(networkScoreMap, "zurich_1pm/VirtualCity/Population/networkScoreMap.xml");
			XMLOps.writeToFile(networkScoreMap, historyFileLocation+"/networkScoreMap.xml");
			
			// If scores are good enough, stop evolution and give out a well-performing network
			if(performanceGoalAccomplished == true) {		// 
				System.out.println("Performance Goal has been achieved in Generation " +finalGeneration+ " by Network "+successfulNetwork.networkID+" at averageTravelTime "+successfulAverageTravelTime);			// display most important analyzed data here			
				break;
			}
			
			// If PerformanceGoal not yet achieved, change routes and network here according to their scores!
			// TODO: ...
		}

		// Plot Score Evolution
		int generationsToPlot = nEvolutions;
		//NetworkEvolutionImpl.writeChartAverageGenerationNetworkAverageTravelTimes(generationsToPlot, "zurich_1pm/Evolution/Population/scoreEvolutionAverageOfGeneration.png");
		//NetworkEvolutionImpl.writeChartBestGenerationNetworkAverageTravelTimes(generationsToPlot, "zurich_1pm/Evolution/Population/scoreEvolutionBestScoreOfGeneration.png");
		VC_NetworkImpl.writeChartAverageTravelTimes(generationsToPlot, "zurich_1pm/VirtualCity/Population/scoreEvolution.png");
	}

	
	
	
	public static MNetwork createMNetworkRoutes(String thisNewNetworkName, int routesPerNetwork, 
			int XMax, int YMax, int removalPercentage, int outerFramePercentage,
			int minSpacingPercentage, String vehicleTypeName, double vehicleLength, double maxVelocity,
			int vehicleSeats, int vehicleStandingRoom,String defaultPtMode, boolean blocksLane, double stopTime, double maxVehicleSpeed,
			double tFirstDep, double tLastDep, double depSpacing, int nDepartures) {

		MNetwork mNetwork = new MNetwork(thisNewNetworkName);
		String mNetworkPath = "zurich_1pm/VirtualCity/Population/"+thisNewNetworkName;
		new File(mNetworkPath).mkdirs();
		
		Config originalConfig = ConfigUtils.loadConfig("zurich_1pm/zurich_config.xml");
		Scenario originalScenario = ScenarioUtils.loadScenario(originalConfig);
		originalScenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DefaultEnrichedTransitRoute.class, new DefaultEnrichedTransitRouteFactory());
		//Network originalNetwork = originalScenario.getNetwork();
		//TransitSchedule originalTransitSchedule = originalScenario.getTransitSchedule();

		// load & create configuration and scenario
		Config config = ConfigUtils.createConfig();								// in this case it is empty files and structures
		Scenario scenario = ScenarioUtils.loadScenario(config);
		scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DefaultEnrichedTransitRoute.class,
				new DefaultEnrichedTransitRouteFactory());						// why do we need this again?
		Network network = scenario.getNetwork();								// NetworkFactory netFac = network.getFactory();

	// load, create & process network
		double unitLinkLength = 500.0;															// [meters]
		network = VC_NetworkImpl.fill(XMax, YMax, network, unitLinkLength);						// Fill up network with nodes between XMax and YMax to make a perfect node grid - These nodes can be used as potential stop locations in a perfect and uniform network.
		VC_NetworkImpl.writeToFile(XMax, YMax, network, mNetworkPath+"/Network_"+XMax+"x"+YMax+"_RAW.xml");		
	// make a thinner and more realistic network by removing a percentage of nodes and its connecting links
		boolean writeToFile = true;																														// if we want to keep 
		Network finalNetwork = VC_NetworkImpl.thin(network, XMax, YMax, removalPercentage, 																// make new static method in networkFiller		
				writeToFile, mNetworkPath+"/Network_"+XMax+"x"+YMax+"_"+removalPercentage+"PercentLean.xml");		
		// TODO create loading function here to load a specific network that can be compared over several runs
		ArrayList<NetworkRoute> initialVCRoutes = new ArrayList<NetworkRoute>();
		for (int nr=0; nr<routesPerNetwork; nr++) {
			NetworkRoute networkRoute = VC_NetworkImpl.createNetworkRoute(finalNetwork, XMax, YMax, outerFramePercentage, minSpacingPercentage);		// make a shortest path networkRoute between two random nodes in the outer regions of the network				
			initialVCRoutes.add(networkRoute);
		}
		//Network separateRoutesNetwork = NetworkEvolutionImpl.networkRoutesToNetwork(initialVCRoutes, finalNetwork, Sets.newHashSet("pt"), (mNetworkPath+"/InitialNetworkRoutes.xml"));
		NetworkEvolutionImpl.networkRoutesToNetwork(initialVCRoutes, finalNetwork, Sets.newHashSet("pt"), (mNetworkPath+"/InitialNetworkRoutes.xml"));

		
		// Load & Create Schedules and Factories
		Config vcConfig = ConfigUtils.createConfig();						// this is totally default and may be modified as required
		Scenario vcScenario = ScenarioUtils.createScenario(vcConfig);
		TransitSchedule vcSchedule = vcScenario.getTransitSchedule();
		TransitScheduleFactory vcScheduleFactory = vcSchedule.getFactory();
		// Create a New Metro Vehicle
		VehicleType vcVehicleType = Metro_TransitScheduleImpl.createNewVehicleType(vehicleTypeName, vehicleLength, maxVelocity, vehicleSeats, vehicleStandingRoom);
					vcScenario.getTransitVehicles().addVehicleType(vcVehicleType);
		// Generate TransitLines and Schedules on NetworkRoutes --> Add to Transit Schedule
		int nTransitLines = initialVCRoutes.size();
		for(int lineNr=1; lineNr<=nTransitLines; lineNr++) {
				
			// networkRoute
			NetworkRoute vcNetworkRoute = initialVCRoutes.get(lineNr-1);
			MRoute mRoute = new MRoute(thisNewNetworkName+"_Route"+lineNr);
			mRoute.setNetworkRoute(vcNetworkRoute);
			mNetwork.addNetworkRoute(mRoute);
			
			// Create an array of stops along new networkRoute on the center of each of its individual links
			List<TransitRouteStop> stopArray = VC_NetworkImpl.createAndAddNetworkRouteStops(
							vcSchedule, finalNetwork, vcNetworkRoute, defaultPtMode, stopTime, maxVehicleSpeed, blocksLane);
			
			
			// Build TransitRoute from stops and NetworkRoute --> and add departures
			String vehicleFileLocation = (mNetworkPath+"/vcVehicles.xml");
			TransitRoute transitRoute = vcScheduleFactory.createTransitRoute(Id.create(thisNewNetworkName+"_Route"+lineNr, TransitRoute.class ), 
					vcNetworkRoute, stopArray, defaultPtMode);
			double totalRouteTravelTime = stopArray.get(stopArray.size()-1).getArrivalOffset();
			transitRoute = Metro_TransitScheduleImpl.addDeparturesAndVehiclesToTransitRoute(vcScenario, vcSchedule, transitRoute,
					nDepartures, tFirstDep, depSpacing, totalRouteTravelTime, vcVehicleType, vehicleFileLocation); // Add (nDepartures) departures to TransitRoute
								
			// Build TransitLine from TrasitRoute
			TransitLine transitLine = vcScheduleFactory.createTransitLine(Id.create("TransitLine_Nr"+lineNr, TransitLine.class));
			transitLine.addRoute(transitRoute);
			
			// Add new line to schedule
			vcSchedule.addTransitLine(transitLine);			
		
			mRoute.setTransitLine(transitLine);
			mRoute.setLinkList(NetworkEvolutionImpl.NetworkRoute2LinkIdList(vcNetworkRoute));
			mRoute.setNodeList(NetworkEvolutionImpl.NetworkRoute2NodeIdList(vcNetworkRoute, finalNetwork));
			mRoute.setRouteLength(NetworkEvolutionImpl.NetworkRoute2TotalLength(vcNetworkRoute, finalNetwork));
			mRoute.nDepartures = nDepartures;
			mRoute.setDrivenKM(mRoute.routeLength*mRoute.nDepartures);
			//mRoute.constrCost = mRoute.routeLength*(metroConstructionCostPerKmOverground*0.01*(100-mRoute.undergroundPercentage)+metroConstructionCostPerKmUnderground*0.01*mRoute.undergroundPercentage);
			//mRoute.opsCost = mRoute.routeLength*(metroOpsCostPerKM*0.01*(100-mRoute.undergroundPercentage)+2*metroOpsCostPerKM*0.01*mRoute.undergroundPercentage);
			mRoute.transitScheduleFile = mNetworkPath+"/vcSchedule.xml";
			
		}	// end of TransitLine creator loop

		// Write TransitSchedule to corresponding file
		TransitScheduleWriter tsw = new TransitScheduleWriter(vcSchedule);
		tsw.writeFile(mNetworkPath+"/vcSchedule.xml");
		
		/*String mergedNetworkFileName = (mNetworkPath+"/vcNetworkWithTransitRoutes.xml");
		Network mergedNetwork = Metro_TransitScheduleImpl.mergeRoutesNetworkToOriginalNetwork(separateRoutesNetwork, originalNetwork, Sets.newHashSet("pt"), mergedNetworkFileName);
		TransitSchedule mergedTransitSchedule = Metro_TransitScheduleImpl.mergeAndWriteTransitSchedules(vcSchedule, originalTransitSchedule, (mNetworkPath+"/vcSchedule.xml"));
		Vehicles mergedVehicles = Metro_TransitScheduleImpl.mergeAndWriteVehicles(vcScenario.getTransitVehicles(), originalScenario.getTransitVehicles(), (mNetworkPath+"/vcVehicles.xml"));*/
		
		// fill in MNetwork Objects for this Network
		mNetwork.network = finalNetwork;
		mNetwork.transitSchedule = vcSchedule;
		mNetwork.vehicles = vcScenario.getTransitVehicles();
		return mNetwork;
	}
	
	
	
	
	public static void run(String[] args, MNetwork mNetwork,
			String initialConfig, int lastIteration, int XMax, int YMax, int removalPercentage, int nNewPeople) throws ConfigurationException  {
		

//	
//		Scenario scenario = ScenarioUtils.createScenario(modConfig);
//		ScenarioUtils.loadScenario(scenario);															// do I have to load scenario here due to having set the new route factory or would I have to load anyways
//		Controler controler = new Controler(scenario);
//		controler.run();
		
		CommandLine cmd = new CommandLine.Builder(args)
				.allowOptions("model-type", "fallback-behaviour")
				.build();
		
		Config modConfig = ConfigUtils.loadConfig(initialConfig);
		String simulationPath = "zurich_1pm/VirtualCity/Population/"+mNetwork.networkID+"/Simulation_Output";
		new File(simulationPath).mkdirs();
		
		modConfig.getModules().get("controler").addParam("outputDirectory", simulationPath);
		modConfig.getModules().get("controler").addParam("overwriteFiles", "overwriteExistingFiles");
		modConfig.getModules().get("controler").addParam("lastIteration", Integer.toString(lastIteration));
		modConfig.getModules().get("controler").addParam("writeEventsInterval", "1");
		String inputNetworkFile = "VirtualCity/Population/"+mNetwork.networkID+"/Network_"+XMax+"x"+YMax+"_"+removalPercentage+"PercentLean.xml";
		modConfig.getModules().get("network").addParam("inputNetworkFile", inputNetworkFile);
		String inputPopulationFile = "VirtualCity/Input/Generated_Population/Plans"+nNewPeople+".xml";
		modConfig.getModules().get("plans").addParam("inputPlansFile", inputPopulationFile);
		modConfig.getModules().get("plans").addParam("inputPersonAttributesFile", "null"); // may have to add attributes in population creation !!
		modConfig.getModules().get("transit").addParam("transitScheduleFile","VirtualCity/Population/"+mNetwork.networkID+"/vcSchedule.xml");
		modConfig.getModules().get("transit").addParam("vehiclesFile","VirtualCity/Population/"+mNetwork.networkID+"/vcVehicles.xml");
		modConfig.getModules().get("ptCounts").addParam("outputformat", "txt");		
		modConfig.getModules().remove("facilities");
		modConfig.getModules().remove("households");
		ConfigWriter configWriter = new ConfigWriter(modConfig);
		configWriter.write("zurich_1pm/VirtualCity/Population/"+mNetwork.networkID+"/modifiedConfig.xml");
		
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
	
}



//%%%%%%%%%%%%%%%%%%%%% ARCHIVE %%%%%%%%%%%%%%%%%%%%%%%%

/*static public void main2(String[] args) {
	
	final int XMax = 30;													// set network size in West-to-East
	final int YMax = 30;													// set network size in South-to-North	
	int removalPercentage = 30;
	int nTransitLines = 10;
	int nNewPeople = 20;

	// Create an entirely new scenario here [ = Network + Population/Demand + TransitSchedule/Infrastructure]
	createCompleteScenario(XMax, YMax, removalPercentage, nTransitLines, nNewPeople);

	// Configure and run MATSim
	String networkName = "Network_"+Integer.toString(XMax)+"x"+Integer.toString(YMax)+"_"+Integer.toString(removalPercentage)+"PercentLean.xml";
	int lastIteration = 10;
	String populationName = "Plans"+Integer.toString(nNewPeople)+".xml";
	String scheduleName = "Schedule.xml";
	String vehiclesName = "Vehicles.xml";
	String initialConfig = "zurich_1pm/zurich_config_modScores.xml";
	Config modConfig = VC_ConfigModifier.modifyConfig(ConfigUtils.loadConfig(initialConfig), lastIteration, networkName, populationName, 
			scheduleName, vehiclesName);  
	Scenario scenario = ScenarioUtils.createScenario(modConfig);
	ScenarioUtils.loadScenario(scenario);															// do I have to load scenario here due to having set the new route factory or would I have to load anyways
	Controler controler = new Controler(scenario);
	controler.run();
}*/


/*public static void createCompleteScenario(int XMax, int YMax, int removalPercentage, int nTransitLines,	int nNewPeople){
	// load & create configuration and scenario
			Config config = ConfigUtils.createConfig();								// in this case it is empty files and structures
			Scenario scenario = ScenarioUtils.loadScenario(config);
			scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DefaultEnrichedTransitRoute.class,
					new DefaultEnrichedTransitRouteFactory());						// why do we need this again?
			Network network = scenario.getNetwork();								// NetworkFactory netFac = network.getFactory();

		// load, create & process network		
			network = VC_NetworkImpl.fill(XMax, YMax, network);						// Fill up network with nodes between XMax and YMax to make a perfect node grid - These nodes can be used as potential stop locations in a perfect and uniform network.
			VC_NetworkImpl.writeToFile(XMax, YMax, network);
			
		// make a thinner and more realistic network by removing a percentage of nodes and its connecting links
			boolean writeToFile = true;																			// if we want to keep 
			Network networkThin = VC_NetworkImpl.thin(network, XMax, YMax, removalPercentage, writeToFile);		// make new static method in networkFiller		
			// TODO create loading function here to load a specific network that can be compared over several runs	
					
		// load, create & process pt network
			TransitSchedule transitSchedule = scenario.getTransitSchedule();						// Create TransitSchedule placeholder and a factory
			TransitScheduleFactory transitScheduleFactory = transitSchedule.getFactory();

		// make a new vehicleType
			String vehicleTypeName = "magicalBusType";
			double vehicleLength = 15;
			double maxVelocity = 100/3.6;
			int vehicleSeats = 15;
			int vehicleStandingRoom = 15;
			VehicleType magicalBus = VC_PublicTransportImpl.createNewVehicleType(scenario, vehicleTypeName, vehicleLength, maxVelocity, vehicleSeats, vehicleStandingRoom);
			scenario.getTransitVehicles().addVehicleType(magicalBus);
			
		// Make (nTransitLines) new TransitLines from random networkRoutes in given network 
		// TODO make a method for entire loop: 
		// TODO newNetworkWithTransitSchedule(Scenario scenario, int nTransitLines, int outerFramePercentage, int minSpacingPercentage,
		//										String defaultPtMode, boolean blocksLane, double stopTime, double vehicleSpeed, int nDepartures, double firstDepTime, double departureSpacing)	
			

			for(int lineNr=0; lineNr<nTransitLines; lineNr++) {		
			
				// RandomNetworkRouteGenerator
				// - Chooses random start and end node on frame with a minimum Euclidean spacing between them
				// - Creates shortest path route between them with Dijkstra's			
				// - TODO OPTIONAL: make iterator dependent (r) names for networkRoute and transitRoute
					int outerFramePercentage = 40;				// take only nodes on outer 50% of network (specify frame as outer 50% of network nodes)
					int minSpacingPercentage = 60;				// minimum spacing requirement between start and end node of a route so that sample routes are not too short!
				NetworkRoute networkRoute = VC_NetworkImpl.createNetworkRoute(networkThin, XMax, YMax, outerFramePercentage, minSpacingPercentage);			// make a shortest path networkRoute between two random nodes in the outer regions of the network				
				VC_NetworkImpl.createAndWriteNetworkRouteToNetwork(config, networkThin, networkRoute, Sets.newHashSet("pt", "car", "walk"), lineNr, XMax, YMax, removalPercentage); 									// Store new network here consisting solely of shortest path route in order to display in VIA/ (or can I store route as such?) // or: Network shortestPathNetwork = ShortestPath.createAndWriteNetwork(...);
				
				// Create an array of stops along new networkRoute on the center of each of its individual links
					String defaultPtMode = "bus";
					boolean blocksLane = false;
					double stopTime = 1.0; 					// stop duration for vehicle in [seconds]
					double vehicleSpeed = 100/3.6; 				// 120 unit_link_lengths/hour = 2 unit_link_lengths/minute = 2/60 unit_link_lengths/second
				List<TransitRouteStop> stopArray = VC_PublicTransportImpl.networkRouteStopsAllLinks(
						transitSchedule, networkThin, networkRoute, defaultPtMode, stopTime, vehicleSpeed, blocksLane);
				
				// Build TransitRoute from stops and NetworkRoute --> and add departures
					int nDepartures = 10;
					double firstDepTime = 6.0*60*60;
					double departureSpacing = 15*60;
					String vehicleFileLocation = "zurich_1pm/VirtualCity/Input/Generated_PT_Files/Vehicles.xml";
				TransitRoute transitRoute = transitScheduleFactory.createTransitRoute(Id.create("transitRoute_"+lineNr, TransitRoute.class ), networkRoute, stopArray, defaultPtMode);
				transitRoute = VC_PublicTransportImpl.addDeparturesAndVehiclesToTransitRoute(scenario, transitSchedule, transitRoute, nDepartures, firstDepTime, departureSpacing, magicalBus, vehicleFileLocation); // Add (nDepartures) departures to TransitRoute
							
				// Build TransitLine from TrasitRoute
				TransitLine transitLine = transitScheduleFactory.createTransitLine(Id.create("transitLine_"+lineNr, TransitLine.class));
				transitLine.addRoute(transitRoute);
				
				// Add new line to schedule
				transitSchedule.addTransitLine(transitLine);			

			}	// end of TransitLine creator loop

			// Write TransitSchedule to corresponding file
			TransitScheduleWriter tsw = new TransitScheduleWriter(transitSchedule);
			tsw.writeFile("zurich_1pm/VirtualCity/Input/Generated_PT_Files/Schedule.xml");
			
			
		// create population by means of population factory
			Population population = scenario.getPopulation();
			
			double networkSize = Math.sqrt(XMax*XMax+YMax*YMax);
			String populationPrefix = "Pop";
			
			VC_ScenarioImpl demandCreator = new VC_ScenarioImpl();
			System.out.println("networkSize is: "+networkSize);
			demandCreator.createNewDemand(scenario, networkThin, networkSize, nNewPeople, populationPrefix);

			PopulationWriter populationWriter = new PopulationWriter(population);		// Write new population to new file >> change config after that to new network name!
			populationWriter.write("zurich_1pm/VirtualCity/Input/Generated_Population/Plans"+nNewPeople+".xml");
}*/

//%%%%%%%%%%%%%%%%%%%%%%%%%%%% HELPER METHODS ALL IN THIS MAIN FILE %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

/*public static ArrayList<NetworkRoute> createInitialRoutesRandom(Network newMetroNetwork, int nRoutes, double minTerminalDistance) {

	ArrayList<NetworkRoute> networkRouteArray = new ArrayList<NetworkRoute>();

	// make nRoutes new routes
	Id<Node> terminalNode1 = null;
	Id<Node> terminalNode2 = null;
	
	OuterNetworkRouteLoop:
	while (networkRouteArray.size() < nRoutes) {

		// choose two random terminals
		Id<Link> randomTerminalLinkId1 = getRandomLink(links_MetroTerminalCandidates.keySet());
		terminalNode1 = Id.createNodeId("MetroNodeLinkRef_" + randomTerminalLinkId1.toString());
		if (newMetroNetwork.getNodes().keySet().contains(terminalNode1) == false) {
			System.out.println("Terminal node 1 is not featured in new network: ");
		}
		int safetyCounter = 0;
		int iterLimit = 10000;
		do {
			Id<Link> randomTerminalLinkId2 = getRandomLink(links_MetroTerminalCandidates.keySet());
			terminalNode2 = Id.createNodeId("MetroNodeLinkRef_" + randomTerminalLinkId2.toString());
			safetyCounter++;
			if (safetyCounter == iterLimit) {
				System.out.println("Oops no second terminal node found after " + iterLimit + " iterations. Trying to create next networkRoute. "
						+ "Please lower minTerminalDistance!");
				continue OuterNetworkRouteLoop;
			}
		} while (GeomDistance.calculate(newMetroNetwork.getNodes().get(terminalNode1).getCoord(),
				newMetroNetwork.getNodes().get(terminalNode2).getCoord()) < minTerminalDistance
				&& safetyCounter < iterLimit);

		if (newMetroNetwork.getNodes().keySet().contains(terminalNode2) == false) {
			System.out.println("Terminal node 2 is not featured in new network: ");
		}

		// Find Djikstra --> nodeList
		ArrayList<Node> nodeList = DijkstraOwn_I.findShortestPathVirtualNetwork(newMetroNetwork, terminalNode1,
				terminalNode2);
		if (nodeList == null) {
				System.out.println("Oops, no shortest path available. Trying to create next networkRoute. Please lower minTerminalDistance"
						+ " ,or increase maxNewMetroLinkDistance (and - last - increase nMostFrequentLinks if required)!");
				System.out.println("Distance between terminals is "+GeomDistance.betweenNodes(newMetroNetwork.getNodes().get(terminalNode1), newMetroNetwork.getNodes().get(terminalNode2)));
				System.out.println("Coord of terminal1 is "+newMetroNetwork.getNodes().get(terminalNode1).getCoord());
				System.out.println("Coord of terminal2 is "+newMetroNetwork.getNodes().get(terminalNode2).getCoord());
				continue OuterNetworkRouteLoop;
		}
		List<Id<Link>> linkList = NetworkEvolutionImpl.nodeListToNetworkLinkList(newMetroNetwork, nodeList);
		linkList.addAll(NetworkEvolutionImpl.OppositeLinkListOf(linkList)); // extend linkList with its opposite direction for PT transportation!
		NetworkRoute networkRoute = RouteUtils.createNetworkRoute(linkList, newMetroNetwork);

		System.out.println("The new networkRoute is: [Length="+(networkRoute.getLinkIds().size()+2)+"] - " + networkRoute.toString());
		networkRouteArray.add(networkRoute);
	}

	// Doing already in main file --> Not necessary to do here again:
	// Store all new networkRoutes in a separate network file for visualization
	// --> networkRoutesToNetwork(networkRouteArray, newMetroNetwork, fileName);
	return networkRouteArray;
}*/