package ch.ethz.matsim.students.samark;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;

public class Metro_TransitScheduleImpl {

	
	public static VehicleType createNewVehicleType(String vehicleTypeName, double length, double maxVelocity, int seats, int standingRoom) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Vehicles transitVehicles = scenario.getTransitVehicles();
		VehiclesFactory vehiclesFactory = transitVehicles.getFactory();
		VehicleType vehicleType = vehiclesFactory.createVehicleType(Id.create(vehicleTypeName, VehicleType.class));
		vehicleType.setLength(length);
		vehicleType.setMaximumVelocity(maxVelocity);
		VehicleCapacity vehicleCapacity = vehiclesFactory.createVehicleCapacity();
		vehicleCapacity.setSeats(seats);
		vehicleCapacity.setStandingRoom(standingRoom);
		vehicleType.setCapacity(vehicleCapacity);
		transitVehicles.addVehicleType(vehicleType);
		System.out.println("New vehicle type is: "+vehicleType.getId().toString());
		return vehicleType;
	}
	
	public static List<TransitRouteStop> createAndAddNetworkRouteStops(TransitSchedule transitSchedule, Network network, NetworkRoute networkRoute, String defaultPtMode, double stopTime, double maxVehicleSpeed, boolean blocksLane){
		TransitScheduleFactory transitScheduleFactory = transitSchedule.getFactory();
		
		List<TransitRouteStop> stopArray = new ArrayList<TransitRouteStop>();				// prepare an array for stop facilities on new networkRoute
		
		int stopCount = 0;
		double accumulatedDrivingTime = 0;
		Link lastLink = null;
		
		for (Id<Link> linkID : Metro_NetworkImpl.networkRouteToLinkIdList(networkRoute)) {
			// place the stop facilities always on the FromNode of the RefLink; this way, the new facilities will have the same coords as the original network's facilities!
			Link currentLink = network.getLinks().get(linkID);
			TransitStopFacility transitStopFacility = transitScheduleFactory.createTransitStopFacility(Id.create("MetroStopRefLink_"+linkID.toString(), TransitStopFacility.class), currentLink.getFromNode().getCoord(), blocksLane);
			transitStopFacility.setName("MetroStopRefLink_"+linkID.toString());
			transitStopFacility.setLinkId(linkID);
			stopCount++;
			if(stopCount>1) {
				accumulatedDrivingTime += lastLink.getLength()/(maxVehicleSpeed);
			}
			double arrivalDelay = (stopCount-1)*stopTime + accumulatedDrivingTime;
			double departureDelay = (stopCount)*stopTime + accumulatedDrivingTime;		// same as arrivalDelay + 1*stopTime
			TransitRouteStop transitRouteStop = transitScheduleFactory.createTransitRouteStop(transitStopFacility, arrivalDelay, departureDelay);
			if (transitSchedule.getFacilities().containsKey(transitStopFacility.getId())==false) {
				transitSchedule.addStopFacility(transitStopFacility);
			}
			stopArray.add(transitRouteStop);
			lastLink = currentLink;
		}
		
		return stopArray;
	}
	
	public static TransitRoute addDeparturesAndVehiclesToTransitRoute(Scenario scenario, TransitSchedule transitSchedule, TransitRoute transitRoute, int nDepartures, double firstDepTime, double departureSpacing, VehicleType vehicleType, String vehicleFileLocation) {
		double depTimeOffset = 0;
		for (int d=0; d<nDepartures; d++) {
			depTimeOffset = d*15*60;
			Departure departure = transitSchedule.getFactory().createDeparture(Id.create(transitRoute.getId().toString()+"_Departure_"+d+"_"+(firstDepTime+depTimeOffset), Departure.class), firstDepTime+depTimeOffset); // TODO specify departureX with better name
			Vehicle vehicle = scenario.getTransitVehicles().getFactory().createVehicle(Id.createVehicleId(transitRoute.getId().toString()+"_"+vehicleType.getId().toString()+"_"+d), vehicleType);
			// System.out.println(scenario.getVehicles().getVehicles().containsKey(vehicle.getId()));
			if (scenario.getTransitVehicles().getVehicles().containsKey(vehicle.getId())) {
				scenario.getTransitVehicles().removeVehicle(vehicle.getId());
			}
			scenario.getTransitVehicles().addVehicle(vehicle);
			departure.setVehicleId(vehicle.getId());
			transitRoute.addDeparture(departure);
		}
		VehicleWriterV1 vehicleWriter = new VehicleWriterV1(scenario.getTransitVehicles());
		vehicleWriter.writeFile(vehicleFileLocation);
		
		return transitRoute;
	}
	
	public static Network mergeRoutesNetworkToOriginalNetwork(Network routesNetwork, Network originalNetwork, Set<String> transportModes, String fileName) {
		Network mergedNetwork = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getNetwork();
		
		Metro_NetworkImpl.copyNetworkToNetwork(routesNetwork, mergedNetwork);
		Metro_NetworkImpl.copyNetworkToNetwork(originalNetwork, mergedNetwork);
		
		// Add this part to connect new network to old network. this is NOT NECESSARY as the agents will "telewalk" to the metro nodes of the new network.
		// This would have to be added for new car links given the fact, that only walking is teleported and cars could not reach the new links.
		/*for (Node node : routesNetwork.getNodes().values()) {
			Id<Link> originalRefLinkId = Metro_NetworkImpl.orginalLinkFromMetroNode(node.getId());
			Link originalRefLink = originalNetwork.getLinks().get(originalRefLinkId);
			Link connectingLinkToToNode = originalNetwork.getFactory().createLink(Id.createLinkId("Connector_newNode"+node.getId().toString()+"_originalNode"+originalRefLink.getToNode().getId().toString()), mergedNetwork.getNodes().get(node.getId()), mergedNetwork.getNodes().get(originalRefLink.getToNode().getId())); 
			Link connectingLinkToFromNode = originalNetwork.getFactory().createLink(Id.createLinkId("Connector_newNode"+node.getId().toString()+"_originalNode"+originalRefLink.getFromNode().getId().toString()), mergedNetwork.getNodes().get(node.getId()), mergedNetwork.getNodes().get(originalRefLink.getFromNode().getId())); 
			Link connectingLinkFromToNode = originalNetwork.getFactory().createLink(Id.createLinkId("Connector_originalNode"+originalRefLink.getToNode().getId().toString()+"_newNode"+node.getId().toString()), mergedNetwork.getNodes().get(originalRefLink.getToNode().getId()), mergedNetwork.getNodes().get(node.getId())); 
			Link connectingLinkFromFromNode = originalNetwork.getFactory().createLink(Id.createLinkId("Connector_originalNode"+originalRefLink.getFromNode().getId().toString()+"_newNode"+node.getId().toString()), mergedNetwork.getNodes().get(originalRefLink.getFromNode().getId()), mergedNetwork.getNodes().get(node.getId())); 
			connectingLinkToToNode.setAllowedModes(transportModes);
			connectingLinkToFromNode.setAllowedModes(transportModes);
			connectingLinkFromToNode.setAllowedModes(transportModes);
			connectingLinkFromFromNode.setAllowedModes(transportModes);
			mergedNetwork.addLink(connectingLinkToToNode);
			mergedNetwork.addLink(connectingLinkToFromNode);
			mergedNetwork.addLink(connectingLinkFromToNode);
			mergedNetwork.addLink(connectingLinkFromFromNode);
		}*/
		
		NetworkWriter initialRoutesNetworkWriter = new NetworkWriter(mergedNetwork);
		initialRoutesNetworkWriter.write(fileName);
		
		return mergedNetwork;
	}

	public static TransitSchedule mergeAndWriteTransitSchedules(TransitSchedule schedule1, TransitSchedule schedule2, String fileName) {
		Config defaultConfig = ConfigUtils.createConfig();
		Scenario defaultScenario = ScenarioUtils.createScenario(defaultConfig);
		TransitSchedule mergedSchedule = defaultScenario.getTransitSchedule();
		
		// Add all TransitStopFacilities from both TransitSchedules
		for (TransitStopFacility stopFacility : schedule1.getFacilities().values()) {
			mergedSchedule.addStopFacility(stopFacility);
		}
		for (TransitStopFacility stopFacility : schedule2.getFacilities().values()) {
			mergedSchedule.addStopFacility(stopFacility);
		}		

		// Add all TransitLines from both TransitSchedules
		for (TransitLine transitLine : schedule1.getTransitLines().values()) {
			mergedSchedule.addTransitLine(transitLine);
		}			
		for (TransitLine transitLine : schedule2.getTransitLines().values()) {
			mergedSchedule.addTransitLine(transitLine);
		}
		
		TransitScheduleWriter tsw = new TransitScheduleWriter(mergedSchedule);
		tsw.writeFile(fileName);

		return null;
	}
	
	public static Vehicles mergeAndWriteVehicles(Vehicles transitVehicles1, Vehicles transitVehicles2, String fileName) {
		Config defaultConfig = ConfigUtils.createConfig();
		Scenario defaultScenario = ScenarioUtils.createScenario(defaultConfig);
		Vehicles mergedTransitVehicles = defaultScenario.getTransitVehicles();
		
		// Add all VehicleTypes
		for (VehicleType transitVehicleType : transitVehicles1.getVehicleTypes().values()) {
			mergedTransitVehicles.addVehicleType(transitVehicleType);
		}
		for (VehicleType transitVehicleType : transitVehicles2.getVehicleTypes().values()) {
			mergedTransitVehicles.addVehicleType(transitVehicleType);
		}
		
		// Add all Vehicles
		for (Vehicle transitVehicle : transitVehicles1.getVehicles().values()) {
			mergedTransitVehicles.addVehicle(transitVehicle);
		}
		for (Vehicle transitVehicle : transitVehicles2.getVehicles().values()) {
			mergedTransitVehicles.addVehicle(transitVehicle);
		}

		VehicleWriterV1 vehicleWriterV1 = new VehicleWriterV1(mergedTransitVehicles);
		vehicleWriterV1.writeFile(fileName);

		return mergedTransitVehicles;
	}
	
}
