package ch.ethz.matsim.students.samark;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
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
				// transitStopFacility.setLinkId(currentLinkID);

		
		public static List<TransitRouteStop> createAndAddNetworkRouteStops(Map<Id<Link>, CustomMetroLinkAttributes> metroLinkAttributes, 
				TransitSchedule transitSchedule, Network network, MRoute mRoute, String defaultPtMode, double stopTime, double maxVehicleSpeed, boolean blocksLane) throws IOException{

			TransitScheduleFactory transitScheduleFactory = transitSchedule.getFactory();
			List<TransitRouteStop> stopArray = new ArrayList<TransitRouteStop>();				// prepare an array for stop facilities on new networkRoute
			List<TransitStopFacility> newlyAddedTSF = new ArrayList<TransitStopFacility>();				// prepare an array for stop facilities on new networkRoute
			
			int stopCount = 0;
			double accumulatedDrivingTime = 0;
			double lastStopDistance = 0.0; // new
			
			List<Id<Link>> routeLinkList = new ArrayList<Id<Link>>();
			routeLinkList.addAll(Metro_NetworkImpl.networkRouteToLinkIdList(mRoute.networkRoute));
			double acceleration = 0.1*9.81;
			double vMaxAccDistance = maxVehicleSpeed*maxVehicleSpeed/(2*acceleration);
			double tAccVMax = maxVehicleSpeed/acceleration;

			Id<Link> lastStopLinkId = routeLinkList.get(0); // Have secured by terminal choice that first link definitely has a stopFacility
			TransitStopFacility lastStopFacility = selectStopFacilityOnLink(metroLinkAttributes, network.getLinks().get(lastStopLinkId), null);
			for (Id<Link> currentLinkID : routeLinkList) {
				if (mRoute.facilityBlockedLinks.contains(currentLinkID)) {
//					Log.write("Jumping over link with a blocked StopFacility, i.e. a stop chosen not to be serviced on this route!");
					continue;
				}
				Link currentLink = network.getLinks().get(currentLinkID);
				if (currentLink.equals(null)) {
					Log.writeAndDisplay("linkID cannot be found in network! Next line will give a NullPointer Exception.");
					currentLink.getId().toString();
					// If the above line gives an error, then it is probable that currentLink=null because line above linkID cannot be found in network. 
					// Please check network and make sure network choice is correct, link name is correct, no new link has been added to network!
				}
//				Log.write("Moving along link " + currentLinkID.toString());
				TransitStopFacility transitStopFacility = Metro_TransitScheduleImpl.selectStopFacilityOnLink(metroLinkAttributes, currentLink, lastStopFacility);
				if (transitStopFacility != null) {
//					Log.write("Found new stop = " + transitStopFacility.getId().toString()     );// + "  [refLink = " + transitStopFacility.getLinkId().toString() + " ]");
//					Log.write("Found new stop = " + transitStopFacility.getName()       );//+ "  [refLink = " + transitStopFacility.getLinkId().toString() + " ]");
					stopCount++;
					if(stopCount>1) {
						lastStopDistance = Metro_TransitScheduleImpl.calculateDistanceBetweenStops( routeLinkList, lastStopLinkId, currentLinkID, lastStopFacility, transitStopFacility, network);
						if (lastStopDistance >= vMaxAccDistance) {
							accumulatedDrivingTime += (2*tAccVMax + (lastStopDistance-vMaxAccDistance)/(maxVehicleSpeed));	// 2*AccTime for accelerating and braking and then the cruise time in between
						}
						else {
							accumulatedDrivingTime += 2*Math.sqrt(2*lastStopDistance/acceleration); // 2*xxx for accelerating and then symmetric braking with const acceleration
						}
					}
					double arrivalDelay = (stopCount-1)*stopTime + accumulatedDrivingTime;
					double departureDelay = (stopCount)*stopTime + accumulatedDrivingTime;		// same as arrivalDelay + 1*stopTime
					lastStopFacility = transitStopFacility;
					lastStopLinkId = currentLinkID;
					TransitRouteStop transitRouteStop = transitScheduleFactory.createTransitRouteStop(transitStopFacility, arrivalDelay, departureDelay);
					if (transitSchedule.getFacilities().containsKey(transitStopFacility.getId())==false) {
						transitStopFacility.setLinkId(currentLinkID); // IMPORTANT - Is necessary for MATSim!
						transitSchedule.addStopFacility(transitStopFacility);
						newlyAddedTSF.add(transitStopFacility);
					}
					stopArray.add(transitRouteStop);
				}
				else {
					continue;
//					Log.write("No stop found on this link");
				}
			}
			if (stopArray.size() < 3) {
				// length two would mean same stop is services one time there and one time back, while length three means an additional link 
				// with a stop must be involved and the vehicle must actually drive off from first stop to get to the second one.
				Log.write("CAUTION: too small stopArray = "+stopArray.toString() + " --> Returning NULL and will be removing mRoute from network.");
				// IMPORTANT: remove again all newly added transitStopFacilities in order to avoid SwissRaptor routing issues
				for (TransitStopFacility tsfToRemoveAgain : newlyAddedTSF) {
					transitSchedule.removeStopFacility(tsfToRemoveAgain);
				}
				return null;
			}
			if (stopArray.get(0).getStopFacility().getId().equals(stopArray.get(stopArray.size()-1).getStopFacility().getId()) == false) {
				double terminalArrivalOffset = stopArray.get(stopArray.size()-1).getDepartureOffset()+stopArray.get(1).getArrivalOffset();
				double terminalDepartureOffset = terminalArrivalOffset+stopTime;
				TransitRouteStop terminalTransitRouteStop = transitScheduleFactory.createTransitRouteStop(stopArray.get(0).getStopFacility(),
						terminalArrivalOffset, terminalDepartureOffset);
				stopArray.add(terminalTransitRouteStop);
			}
			return stopArray;
		}
	
	
	public static double calculateDistanceBetweenStops(List<Id<Link>> routeLinkList, Id<Link> lastStopLinkId, Id<Link> currentLinkID, TransitStopFacility lastStopFacility,
			TransitStopFacility transitStopFacility, Network network) {
		List<Id<Link>> subrouteLinkList = null;
		int index1 = routeLinkList.indexOf(lastStopLinkId);
		int index2 = routeLinkList.indexOf(currentLinkID);
		if ( index1 == -1 || index2 == -1 ) {
			return 1000.0;
		}
		if (index1 < index2) {
			subrouteLinkList = routeLinkList.subList(index1, index2);
		}
		else {
			subrouteLinkList = routeLinkList.subList(index2, index1);
		}
		double distance = 0.0;
		distance += GeomDistance.calculate(lastStopFacility.getCoord(), network.getLinks().get(subrouteLinkList.get(0)).getToNode().getCoord());
		distance += GeomDistance.calculate(transitStopFacility.getCoord(), network.getLinks().get(subrouteLinkList.get(subrouteLinkList.size()-1)).getFromNode().getCoord());
		if (subrouteLinkList.size()>2) {
			for (Id<Link> subLinkId : subrouteLinkList.subList(1, subrouteLinkList.size())) {
				distance += network.getLinks().get(subLinkId).getLength();
			}
		}
		return distance;
	}

	public static TransitStopFacility selectStopFacilityOnLink(Map<Id<Link>,CustomMetroLinkAttributes> metroLinkAttributes, 
			Link currentLink, TransitStopFacility lastStopFacility) throws IOException {
		CustomMetroLinkAttributes customMetroLinkAttributes = metroLinkAttributes.get(currentLink.getId());
		if (customMetroLinkAttributes == null) {
			Log.write("No metro link attributes found for link = "+currentLink.getId().toString());
			return null;
		}
		// Second condition makes sure that a TSF is not used twice given the network nature where the same facility can be used on the ToNode of one link
		// and on the FromNode of the next link. Making a stop twice on the same TSF would be unreasonable.
		else if (customMetroLinkAttributes.singleRefStopFacility != null && customMetroLinkAttributes.singleRefStopFacility != lastStopFacility) {
//			Log.write("Found SINGLE REF FACILITY " +  customMetroLinkAttributes.singleRefStopFacility.getName() + " on link="+currentLink.getId().toString());
			return customMetroLinkAttributes.singleRefStopFacility;
		}
		else if(customMetroLinkAttributes.fromNodeStopFacility != null && customMetroLinkAttributes.fromNodeStopFacility != lastStopFacility) {
//			Log.write("Found FROM NODE FACILITY " +  customMetroLinkAttributes.fromNodeStopFacility.getName() + " on link="+currentLink.getId().toString());
			return customMetroLinkAttributes.fromNodeStopFacility;
		}
		else if(customMetroLinkAttributes.toNodeStopFacility != null && customMetroLinkAttributes.toNodeStopFacility != lastStopFacility) {
//			Log.write("Found TO NODE FACILITY " +  customMetroLinkAttributes.toNodeStopFacility.getName() + " on link="+currentLink.getId().toString());
			return customMetroLinkAttributes.toNodeStopFacility;
		}
		else {
//			Log.write("Found no new facility on link= "+currentLink.getId().toString() + "... Try next link...");
			return null;
		}
	}

	
	
	public static TransitStopFacility coord2Facility(TransitSchedule ts, Coord coord) throws IOException {
		for (TransitStopFacility tsf : ts.getFacilities().values()) {
			if (coord.equals(tsf.getCoord())) {
				return tsf;
			}
		}
		Log.write("ERROR: No TransitStopFacility found at such coordinate location! Returning null ...");
		return null;
	}
	
	public static TransitStopFacility node2Facility(Node node, Map<String, CustomStop> customStopsMap) throws IOException {
		for (CustomStop stop : customStopsMap.values()) {
			if (node.getId().equals(stop.newNetworkNode)) { // CAUTION: If you get an error here it may be because stop.newNetworkNode has not been set and is null;
				return stop.transitStopFacility;
			}
		}
		Log.write("ERROR: No TransitStopFacility found for such node! Returning null ...");
		return null;
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
		Id<Link> reverseId = Id.createLinkId(linkIdStrings[1]+"_"+linkIdStrings[0]);
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
	
	

	public static TransitSchedule mergeAndWriteTransitSchedules(TransitSchedule schedule1, TransitSchedule schedule2, String fileName) {
		
		Config defaultConfig = ConfigUtils.createConfig();
		Scenario defaultScenario = ScenarioUtils.createScenario(defaultConfig);
		TransitSchedule mergedSchedule = defaultScenario.getTransitSchedule();

		
		// Add all TransitStopFacilities from both TransitSchedules
		for (TransitStopFacility stopFacility : schedule1.getFacilities().values()) {
			if (mergedSchedule.getFacilities().containsKey(stopFacility.getId())==false) {
				mergedSchedule.addStopFacility(stopFacility);
			}
		}
		for (TransitStopFacility stopFacility : schedule2.getFacilities().values()) {
			if (mergedSchedule.getFacilities().containsKey(stopFacility.getId())==false) {
				mergedSchedule.addStopFacility(stopFacility);
			}
		}
		

		// Add all TransitLines from both TransitSchedules
		for (TransitLine transitLine : schedule1.getTransitLines().values()) {
			if (mergedSchedule.getTransitLines().containsKey(transitLine.getId())==false) {
				mergedSchedule.addTransitLine(transitLine);
			}
		}			
		for (TransitLine transitLine : schedule2.getTransitLines().values()) {
			if (mergedSchedule.getTransitLines().containsKey(transitLine.getId())==false) {
				mergedSchedule.addTransitLine(transitLine);
			}
		}
		
		
		TransitScheduleWriter tsw = new TransitScheduleWriter(mergedSchedule);
		tsw.writeFile(fileName);

		return null;
	}
	
	public static Vehicles mergeAndWriteVehicles(Vehicles transitVehicles1, Vehicles transitVehicles2, String fileName) {
		
		Config defaultConfig = ConfigUtils.createConfig();
		Scenario defaultScenario = ScenarioUtils.createScenario(defaultConfig);
		Vehicles mergedTransitVehicles = defaultScenario.getTransitVehicles();
		
		// CAUTION: May have to construct conditional loops as in schedule merger above if can't add new vehicles
		// because they are already featured in vehicles bin
		
		// Add all VehicleTypes
		for (VehicleType transitVehicleType : transitVehicles1.getVehicleTypes().values()) {
			mergedTransitVehicles.addVehicleType(transitVehicleType);
		}
		for (VehicleType transitVehicleType : transitVehicles2.getVehicleTypes().values()) {
			mergedTransitVehicles.addVehicleType(transitVehicleType);
		}
		
		// CAUTION: May have to construct conditional loops as in schedule merger above if can't add new vehicles
		// because they are already featured in vehicles bin

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