package ch.ethz.matsim.students.samark;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
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
		
		List<Id<Link>> routeLinkList = new ArrayList<Id<Link>>();
		routeLinkList.addAll(Metro_NetworkImpl.networkRouteToLinkIdList(networkRoute));
		double acceleration = 0.1*9.81;
		double vMaxAccDistance = maxVehicleSpeed*maxVehicleSpeed/(2*acceleration);
		double tAccVMax = maxVehicleSpeed/acceleration;

		for (Id<Link> linkID : routeLinkList) {
			// place the stop facilities always on the FromNode of the RefLink; this way, the new facilities will have the same coords as the original network's facilities!
			Link currentLink = network.getLinks().get(linkID);
			// CAUTION XXXXXXXXXXXXXX If the following line gives an error, then it is prob. that currentLink=null because line above
			//                        linkID cannot be found in network. Please check network and make sure network choice is corrcet,
			//						  link name is correct, no new link has been added to network!
			TransitStopFacility transitStopFacility = transitScheduleFactory.createTransitStopFacility(Id.create("MetroStopRefLink_"+linkID.toString(), TransitStopFacility.class), currentLink.getFromNode().getCoord(), blocksLane);
			transitStopFacility.setName("MetroStopRefLink_"+linkID.toString());
			transitStopFacility.setLinkId(linkID);
			stopCount++;
			if(stopCount>1) {
				if (lastLink.getLength() >= vMaxAccDistance) {
					accumulatedDrivingTime += (2*tAccVMax + (lastLink.getLength()-vMaxAccDistance)/(maxVehicleSpeed));	// 2*AccTime for accelerating and braking and then the cruise time in between
				}
				else {
					accumulatedDrivingTime += 2*Math.sqrt(2*lastLink.getLength()/acceleration); // 2*xxx for accelerating and then symmetric braking with const acceleration
				}
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
		// do this to add last terminal link on way back, because the stops are always added at the fromNode location and the last link needs a stop at the final toNode!
		Id<Link> terminalLink = stopArray.get(stopArray.size()-1).getStopFacility().getLinkId();
		TransitStopFacility terminalTransitStopFacility = transitScheduleFactory.createTransitStopFacility(Id.create("MetroStopRefLink_"+terminalLink.toString()+"_TerminalStop", TransitStopFacility.class), 
				network.getLinks().get(terminalLink).getToNode().getCoord(), blocksLane);
		terminalTransitStopFacility.setName("MetroStopRefLink_"+terminalLink.toString()+"_TerminalStop");
		terminalTransitStopFacility.setLinkId(terminalLink);
		double terminalArrivalOffset = stopArray.get(stopArray.size()-1).getDepartureOffset()+stopArray.get(1).getArrivalOffset();
		double terminalDepartureOffset = terminalArrivalOffset+stopTime;
		TransitRouteStop terminalTransitRouteStop = transitScheduleFactory.createTransitRouteStop(
				stopArray.get(0).getStopFacility(), terminalArrivalOffset, terminalDepartureOffset);
		stopArray.add(terminalTransitRouteStop);
		
		return stopArray;
	}
	
	public static List<Id<Link>> OppositeLinkListOf(List<Id<Link>> linkList){
		List<Id<Link>> oppositeLinkList = new ArrayList<Id<Link>>(linkList.size());
		for (int c=0; c<linkList.size(); c++) {
			oppositeLinkList.add(ReverseLink(linkList.get(linkList.size()-1-c)));
		}
		return oppositeLinkList;
	}
	
	public static Id<Link> ReverseLink(Id<Link> linkId){
		String[] linkIdStrings = linkId.toString().split("_");
		Id<Link> reverseId = Id.createLinkId("MetroNodeLinkRef_"+linkIdStrings[3]+"_MetroNodeLinkRef_"+linkIdStrings[1]);
		return reverseId;
	}
	
	public static TransitRoute addDeparturesAndVehiclesToTransitRoute(MRoute mRoute, Scenario scenario, TransitSchedule transitSchedule, TransitRoute transitRoute, 
			VehicleType vehicleType, String vehicleFileLocation) throws IOException {
		
		mRoute.nDepartures = (int) Math.floor((mRoute.lastDeparture-mRoute.firstDeparture)/mRoute.departureSpacing);
		double depTimeOffset = 0;
		LinkedHashMap<Double, Id<Vehicle>> freeVehicles = new LinkedHashMap<>();
		int nVehicles = 0;
		for (int d=0; d<mRoute.nDepartures; d++) {
			depTimeOffset = d*mRoute.departureSpacing;
			Departure departure = transitSchedule.getFactory().createDeparture(Id.create(transitRoute.getId().toString()+"_Departure_"+d+"_"+(mRoute.firstDeparture+depTimeOffset), 
					Departure.class), mRoute.firstDeparture+depTimeOffset); // TODO specify departureX with better name
			Vehicle vehicle;
			if (freeVehicles.isEmpty()==false) {
				Iterator<Double> depOffsetIter = freeVehicles.keySet().iterator();
				Double earliestFreeDeparture = depOffsetIter.next();
				if(earliestFreeDeparture < depTimeOffset) {
					vehicle = scenario.getTransitVehicles().getVehicles().get(freeVehicles.get(earliestFreeDeparture));
					freeVehicles.remove(earliestFreeDeparture);
				}
				else {
					vehicle = scenario.getTransitVehicles().getFactory().createVehicle(Id.createVehicleId(transitRoute.getId().toString()+"_"+vehicleType.getId().toString()+"_"+d), vehicleType);
				}
			}
			else {
				nVehicles++;
				vehicle = scenario.getTransitVehicles().getFactory().createVehicle(Id.createVehicleId(transitRoute.getId().toString()+"_"+vehicleType.getId().toString()+"_"+d), vehicleType);
			}
			freeVehicles.put(depTimeOffset+mRoute.roundtripTravelTime, vehicle.getId());
			if (scenario.getTransitVehicles().getVehicles().containsKey(vehicle.getId())) {
				scenario.getTransitVehicles().removeVehicle(vehicle.getId());
			}
			scenario.getTransitVehicles().addVehicle(vehicle);
			departure.setVehicleId(vehicle.getId());
			transitRoute.addDeparture(departure);
		}
		if(nVehicles > mRoute.vehiclesNr) {
			Log.writeAndDisplay("CAUTION: Vehicles foreseen|added = "+mRoute.vehiclesNr+"|"+nVehicles);
		}
		VehicleWriterV1 vehicleWriter = new VehicleWriterV1(scenario.getTransitVehicles());
		vehicleWriter.writeFile(vehicleFileLocation);
		
		return transitRoute;
	}
	
	public static Network mergeRoutesNetworkToOriginalNetwork(Network routesNetwork, Network originalNetwork, Set<String> transportModes, String fileName) {
		Network mergedNetwork = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getNetwork();
		
		Metro_NetworkImpl.copyNetworkToNetwork(routesNetwork, mergedNetwork, transportModes);
		Metro_NetworkImpl.copyNetworkToNetwork(originalNetwork, mergedNetwork, null);
		
		// BEFORE 06.09.2018: Add small connectors between metro links and original links
		// Add this part to connect new network to old network. this is NOT NECESSARY as the agents will "telewalk" to the metro nodes of the new network.
		// This would have to be added for new car links given the fact, that only walking is teleported and cars could not reach the new links.
		
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
