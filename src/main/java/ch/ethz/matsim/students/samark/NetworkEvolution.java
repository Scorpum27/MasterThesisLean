package ch.ethz.matsim.students.samark;

import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;

import ch.ethz.matsim.baseline_scenario.config.CommandLine.ConfigurationException;

/* TODO
 * Make event handler for overall network performance: Overall travel time and travel distance!
 * Make proper IDs to get objects
 * Make transitSchedule for both ways so that same vehicles are used (reverse)
 * Small todo's in code
 * OD initial routes: Make requirement that one terminal is certain distance from network center so that we get longer routes into city!
 * Make a GeographyProcessor that calculates the OG/UG percentage from given regions
 * mNetwork.mPersonKMdirect = beelinedistances
 * include disutility of a transfer... or is that already included?
 */

public class NetworkEvolution {

/* GENETIC STRUCTURE
 * Multiple networks = Population 	= Map<MNetwork.Id, MNetwork> = networkMap
 * Single Network = Chromosome 		= Map<MRoute.Id, MRoute> = routesMap
 * Single Route = Gene				= MRoute
 */
	
/* FRAMEWORK STRUCTURE
 * This NetworkEvolution manages the POPULATION.
 * - it compares score of each network and processes/develops them accordingly
 * - after modifying population it forwards each network individually to the inner MATSim loop
 * - it analyzes the MATSim events and writes the significant info to the individual networks and routes
 * - Make new class instead of Metro_TransitScheduleImpl for corresponding methods
 */
	
/* NETWORK ANALYSIS
 * double totalTravelTime
 * double personKM;
 * double personKMdirect;
 * int nPassengers
 */
	

	public static void main(String[] args) throws ConfigurationException {

	// INITIALIZATION
	// !! Run RunScenario First to have simulation output that can be analyzed !!
	
	// - Initiate N=16 networks to make a population
		// % Parameters for Population: %
		int populationSize = 2;														// how many networks should be developed in parallel
		int routesPerNetwork = 30;													// how many initial routes should be placed in every network
		String initialRouteType = "OD";												// Options: {"OD","Random"}	-- Choose method to create initial routes [OD=StrongestOriginDestinationShortestPaths, Random=RandomTerminals in outer frame of specified network]
																					// For OD also modify as follows: minTerminalRadiusFromCenter = 0.00*metroCityRadius
		int iterationToReadOriginalNetwork = 100;									// TODO simulate originalNetwork up to 1000(?) This is the iteration for the simulation output of the original network
																					// TODO maybe include additional strategy option here for how to make routes e.g. createNetworkRoutes(Strategy, initialRouteType, ...)
		// %% Parameters for NetworkRoutes %%
		Coord zurich_NetworkCenterCoord = new Coord(2683000.00, 1247700.00);		// default Coord(2683099.3305, 1247442.9076);
		double xOffset = 1733436; 													// add this to QGis to get MATSim		// Right upper corner of Zürisee -- X_QGis=950040; X_MATSim= 2683476;
		double yOffset = -4748525;													// add this to QGis to get MATSim		// Right upper corner of Zürisee -- Y_QGis=5995336; Y_MATSim= 1246811;
		double metroCityRadius = 2500; 												// old 1600.00
		double minMetroRadiusFactor = 0.00;											// default 0.00
		double maxMetroRadiusFactor = 1.20;											// give some flexibility by increasing from default 1.00 to 1.20
		double minMetroRadiusFromCenter = metroCityRadius * minMetroRadiusFactor; 	// set default = 0.00 to not restrict metro network in city center
		double maxMetroRadiusFromCenter = metroCityRadius * maxMetroRadiusFactor;	// this is rather large for an inner city network but more realistic to pull inner city network into outer parts to better connect inner/outer city
		int nMostFrequentLinks = 150;												// empirical formula - default 300
		double maxNewMetroLinkDistance = 0.40*metroCityRadius;						// default 0.80*metroCityRadius
		double minTerminalRadiusFromCenter = 0.00*metroCityRadius; 					// CAUTION: 0.00*metroCityRadius; // For OD-Pairs  [0.30*metroCityRadius for RandomRoutes]
		double maxTerminalRadiusFromCenter = maxMetroRadiusFromCenter;				// default = maxMetroRadiusFromCenter
		double minTerminalDistance = 0.70*maxMetroRadiusFromCenter;					// no default yet
		
		// %% Parameters for Vehicles, StopFacilities & Departures %%
		String vehicleTypeName = "metro";  double maxVehicleSpeed = 100/3.6 /*[m/s]*/;
		double vehicleLength = 50;  int vehicleSeats = 100; int vehicleStandingRoom = 100;
		double tFirstDep = 6.0*60*60;  double tLastDep = 20.5*60*60;  double depSpacing = 3.0*60;
		int nDepartures = (int) ((tLastDep-tFirstDep)/depSpacing);
		double stopTime = 30.0; /*stopDuration [s];*/  String defaultPtMode = "metro";  boolean blocksLane = false;
		double metroOpsCostPerKM = 1000; double metroConstructionCostPerKmOverground = 1000000; double metroConstructionCostPerKmUnderground = 10000000;
		
		MNetworkPop networkPopulation = new MNetworkPop(populationSize);			// Initialize population of networks
		for (int N=1; N<=populationSize; N++) {										// Make individual networks one by one in loop
			String thisNewNetworkName = ("Network"+N);								// Name networks by their number [1;populationSize]
			MNetwork mNetwork = NetworkEvolutionImpl.createMNetworkRoutes(			// Make a list of routes that will be added to this network
					thisNewNetworkName, routesPerNetwork, initialRouteType, iterationToReadOriginalNetwork,
					minMetroRadiusFromCenter, maxMetroRadiusFromCenter, zurich_NetworkCenterCoord, metroCityRadius, nMostFrequentLinks,
					maxNewMetroLinkDistance, minTerminalRadiusFromCenter, maxTerminalRadiusFromCenter, minTerminalDistance,
					xOffset, yOffset, vehicleTypeName, vehicleLength, maxVehicleSpeed, vehicleSeats, vehicleStandingRoom,
					defaultPtMode, blocksLane, stopTime, maxVehicleSpeed, tFirstDep, tLastDep, depSpacing, nDepartures,
					metroOpsCostPerKM, metroConstructionCostPerKmOverground, metroConstructionCostPerKmUnderground);
			networkPopulation.addNetwork(mNetwork);
		}
		
		
	// EVOLUTIONARY PROCESS

		// SIMULATION LOOP:
		
		int lastIteration = 3;
		for (MNetwork mNetwork : networkPopulation.getNetworks().values()) {
			String initialConfig = "zurich_1pm/zurich_config.xml";
			NetworkEvolutionRunSim.run(args, mNetwork, initialRouteType, initialConfig, lastIteration);
		} // End Network Simulation Loop 
		

		// - EVENTS PROCESSING:
		
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

		
		
	
	// INITIALIZATION
		// - Initiate N=16 networks to make a population
		// - Fill in N=10 fixed Random/OD initial routes into every network
		// - Initialize all MRoutes/MNetworks with the corresponding info (linkList etc.)
		// - TODO Process all routes for their		
			/* - Apply to all routes the calculator
			 * - length
			 * - nVehicles
			 * - nDepartures = nRides
			 * - drivenKM = length * nRides
			 * - undergroundPercentage (at a later stage)
			 */
			
	// EVOLUTIONARY PROCESS
			
		// SIMULATION LOOP:
		// - For each network
		// - Simulate network in MATSim with plans
		// - Process events in NetworkPerformanceHandler and save to corresponding network of population
		// - Maybe hand over score to a separate score map for sorting scores
	
		// EVOLUTION
		// - Make evolutionary operations
		// - Update population
		// - Log files to save development
		// --> Simulation loop
		
	
	
	} // end Main Method
} // end NetworkEvolution Class
