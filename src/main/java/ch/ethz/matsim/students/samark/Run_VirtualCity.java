
package ch.ethz.matsim.students.samark;

import java.util.List;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.vehicles.VehicleType;

import com.google.common.collect.Sets;

import ch.ethz.matsim.baseline_scenario.transit.routing.DefaultEnrichedTransitRoute;
import ch.ethz.matsim.baseline_scenario.transit.routing.DefaultEnrichedTransitRouteFactory;


/* %%% COMMENTS %%%
 * - network is so small that walking was always preferred to pt because pt was set to very slow speed
 * 		--> two solutions: (1) convert to another coordinate system where every link has length 100.0 instead of 1.0 or (2) change walk speed to a 100th of before i.e. 0.008333
 * - Strange population file person name output comes from VC_PublicTransportImpl.java: Vehicle vehicle = scenario.getVehicles().getFactory().createVehicle(Id.createVehicleId(transitRoute.getId().toString()+"_"+vehicleType.getId().toString()+"_"+d), vehicleType);
 */

public class Run_VirtualCity {


	static public void main(String[] args) {
		
		final int XMax = 50;													// set network size in West-to-East
		final int YMax = 50;													// set network size in South-to-North	
		int removalPercentage = 20;
		int nTransitLines = 50;
		int nNewPeople = 30;

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
	}

	
	public static void createCompleteScenario(int XMax, int YMax, int removalPercentage, int nTransitLines,	int nNewPeople){
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
	}
	
}
