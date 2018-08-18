package ch.ethz.matsim.students.samark;

import java.util.List;

import org.matsim.api.core.v01.Coord;

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
 */
	
/* NETWORK ANALYSIS
 * double totalTravelTime
 * double personKM;
 * double personKMdirect;
 * int nPassengers
 */
	
	public static void main(String[] args) {

	// INITIALIZATION
	// !! Run RunScenario First to have simulation output that can be analyzed !!
	
	// - Initiate N=16 networks to make a population
		// % Parameters for Population: %
		int populationSize = 8;														// how many networks should be developed in parallel
		int routesPerNetwork = 10;													// how many initial routes should be placed in every network
		String initialRouteType = "OD";												// Options: {"OD","Random"}	-- Choose method to create initial routes [OD=StrongestOriginDestinationShortestPaths, Random=RandomTerminals in outer frame of specified network]
		int iterationToReadOriginalNetwork = 100;									// TODO simulate originalNetwork up to 1000(?) This is the iteration for the simulation output of the original network
																					// TODO maybe include additional strategy option here for how to make routes e.g. createNetworkRoutes(Strategy, initialRouteType, ...)
		// %% Parameters for NetworkRoutes %%
		Coord zurich_NetworkCenterCoord = new Coord(2683099.3305, 1247442.9076);	// default Coord(2683099.3305, 1247442.9076);
		double xOffset = 1733436; 													// add this to QGis to get MATSim		// Right upper corner of Zürisee -- X_QGis=950040; X_MATSim= 2683476;
		double yOffset = -4748525;													// add this to QGis to get MATSim		// Right upper corner of Zürisee -- Y_QGis=5995336; Y_MATSim= 1246811;
		double metroCityRadius = 3000.00;											// old 1600.00
		double minMetroRadiusFactor = 0.00;											// default 0.00
		double maxMetroRadiusFactor = 1.20;											// give some flexibility by increasing from default 1.00 to 1.20
		double minMetroRadiusFromCenter = metroCityRadius * minMetroRadiusFactor; 	// set default = 0.00 to not restrict metro network in city center
		double maxMetroRadiusFromCenter = metroCityRadius * maxMetroRadiusFactor;	// this is rather large for an inner city network but more realistic to pull inner city network into outer parts to better connect inner/outer city
		int nMostFrequentLinks = (int) maxMetroRadiusFromCenter*250;				// empirical formula - default 300
		double maxNewMetroLinkDistance = 2/3*metroCityRadius;						// default 0.80*metroCityRadius
		double minTerminalRadiusFromCenter = 2/3*metroCityRadius;				 	// default 0.67 // use this for both initial route generators 
		double maxTerminalRadiusFromCenter = maxMetroRadiusFromCenter;				// default = maxMetroRadiusFromCenter
		double minTerminalDistance = 2/3*maxMetroRadiusFromCenter;					// no default yet
		
		// %% Parameters for Vehicles, StopFacilities & Departures %%
		String vehicleTypeName = "metro";  double maxVehicleSpeed = 100/3.6 /*[m/s]*/;
		double vehicleLength = 50;  int vehicleSeats = 100; int vehicleStandingRoom = 100;
		double tFirstDep = 6.0*60*60;  double tLastDep = 20.5*60*60;  double depSpacing = 3.0*60;
		int nDepartures = (int) ((tLastDep-tFirstDep)/depSpacing);
		double stopTime = 30.0; /*stopDuration [s];*/  String defaultPtMode = "metro";  boolean blocksLane = false;  
		
		MNetworkPop networkPopulation = new MNetworkPop(populationSize);			// Initialize population of networks
		for (int N=1; N<=populationSize; N++) {										// Make individual networks one by one in loop
			String thisNewNetworkName = ("Network"+N);								// Name networks by their number [1;populationSize]
			MNetwork network = new MNetwork(thisNewNetworkName);					// This is a map carrying the MRoutes and their names
			List<MRoute> routesList = NetworkEvolutionImpl.createMNetworkRoutes(	// Make a list of routes that will be added to this network
					routesPerNetwork, initialRouteType, iterationToReadOriginalNetwork,
					minMetroRadiusFromCenter, maxMetroRadiusFromCenter, zurich_NetworkCenterCoord, metroCityRadius, nMostFrequentLinks,
					maxNewMetroLinkDistance, minTerminalRadiusFromCenter, maxTerminalRadiusFromCenter, minTerminalDistance,
					xOffset, yOffset, vehicleTypeName, vehicleLength, maxVehicleSpeed, vehicleSeats, vehicleStandingRoom,
					defaultPtMode, blocksLane, stopTime, maxVehicleSpeed, tFirstDep, tLastDep, depSpacing, nDepartures);
			network.addRoutes(routesList);
			networkPopulation.addNetwork(network);
		}
		
		
		
		
	
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
		
	
	
	}
}
