package ch.ethz.matsim.students.samark.virtualCity;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;

import ch.ethz.matsim.students.samark.GeomDistance;

public class VC_PublicTransportImpl {
	
	public static List<TransitRouteStop> networkRouteStopsAllLinks(TransitSchedule transitSchedule, Network network, NetworkRoute networkRoute, String defaultPtMode, double stopTime, double vehicleSpeed, boolean blocksLane){
		TransitScheduleFactory transitScheduleFactory = transitSchedule.getFactory();
		List<TransitRouteStop> stopArray = new ArrayList<TransitRouteStop>();				// prepare an array for stop facilities on new networkRoute
		
		int stopCount = 0;
		double accumulatedDrivingTime = 0;
		Link lastLink = null;
		
		for (Id<Link> linkID : networkRouteToLinkIdList(networkRoute)) {
			Link currentLink = network.getLinks().get(linkID);
			TransitStopFacility transitStopFacility = transitScheduleFactory.createTransitStopFacility(Id.create("linkStop_"+linkID.toString(), TransitStopFacility.class), GeomDistance.coordBetweenNodes(currentLink.getFromNode(), currentLink.getToNode()), blocksLane);
			transitStopFacility.setName("CenterLinkStop_"+linkID.toString());
			transitStopFacility.setLinkId(linkID);
			stopCount++;
			if(stopCount>1) {
				accumulatedDrivingTime += (lastLink.getLength()/2+currentLink.getLength()/2)/vehicleSpeed;
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
			Departure departure = transitSchedule.getFactory().createDeparture(Id.create(transitRoute.getId().toString()+"_Departure_"+d+"_"+(firstDepTime+depTimeOffset), Departure.class), firstDepTime+depTimeOffset);
			Vehicle vehicle = scenario.getVehicles().getFactory().createVehicle(Id.createVehicleId(transitRoute.getId().toString()+"_"+vehicleType.getId().toString()+"_"+d), vehicleType);
			// System.out.println(scenario.getTransitVehicles().getVehicles().containsKey(vehicle.getId()));
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
	
	public static VehicleType createNewVehicleType(Scenario scenario, String vehicleTypeName, double length, double maxVelocity, int seats, int standingRoom) {
		Vehicles transitVehicles = scenario.getTransitVehicles();
		VehiclesFactory vehiclesFactory = transitVehicles.getFactory();
		VehicleType vehicleType = vehiclesFactory.createVehicleType(Id.create(vehicleTypeName, VehicleType.class));
		vehicleType.setLength(length);
		vehicleType.setMaximumVelocity(maxVelocity);
		VehicleCapacity vehicleCapacity = vehiclesFactory.createVehicleCapacity();
		vehicleCapacity.setSeats(seats);
		vehicleCapacity.setStandingRoom(standingRoom);
		vehicleType.setCapacity(vehicleCapacity);
		System.out.println("New vehicle type is: "+vehicleType.getId().toString());
		return vehicleType;
	}
	
	public static ArrayList<Id<Link>> networkRouteToLinkIdList(NetworkRoute networkRoute){
		ArrayList<Id<Link>> linkList = new ArrayList<Id<Link>>(networkRoute.getLinkIds().size()+2);
		linkList.add(networkRoute.getStartLinkId());
		linkList.addAll(networkRoute.getLinkIds());
		linkList.add(networkRoute.getEndLinkId());
		return linkList;
	}
	
	/*
	public static ArrayList<Id<Vehicle>> generateNewVehicles(int nDepartures, VehicleType vehicleType){
		
		ArrayList<Id<Vehicle>> arrVehicleIDs = new ArrayList<Id<Vehicle>>(nDepartures);
		for (int n=0; n<nDepartures; n++) {
			arrVehicleIDs(n) = 
		}
		return arrVehicleIDs;
	}*/
	

}
