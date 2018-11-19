package ch.ethz.matsim.students.samark;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
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

import ch.ethz.matsim.baseline_scenario.zurich.cutter.population.PopulationCutter;

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

	
	
	
	public static void TS_ModificationModule(String NetworkId) throws IOException {
		// do this to modify original zurichSchedule e.g. compromizedZurichSchedule with only have of the tram routes
		// CAUTION: make sure to use that new schedule in the MATSim run config!!
//		Log.write("  >> Removing half of the tram lines on VBZ schedule for  "+NetworkId);
//		Log.write("  >> Removing half of the rail departures for "+NetworkId);
		Config config = ConfigUtils.createConfig();
		config.getModules().get("transit").addParam("transitScheduleFile","zurich_1pm/Evolution/Population/"+NetworkId+"/MergedSchedule.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		TransitSchedule tsZH = scenario.getTransitSchedule();
	
		// now make sure to insert conditions accordingly in Clone.transitSchedule to modify the new schedule
		// change in the RUNSim config the transitSchedule to MergedScheduleModified.xml
		TransitSchedule tsZHCompromized = Clone.transitSchedule(tsZH);
		TransitScheduleWriter tsw = new TransitScheduleWriter(tsZHCompromized);
		tsw.writeFile("zurich_1pm/Evolution/Population/"+NetworkId+"/MergedScheduleModified.xml");
	}
	
	
	
	public static TransitSchedule SpeedSBahnModule(MNetwork mNetwork, String transitScheduleFileNameOld, String transitScheduleFileNameNew) throws IOException {
		
		Log.write("  >> Modifying ZH SBahn network for "+mNetwork.networkID+". Optimize stop sequences with new metro capacities --> Speed S-Bahn");
		Config configMerged = ConfigUtils.createConfig();
		configMerged.getModules().get("transit").addParam("transitScheduleFile","zurich_1pm/Evolution/Population/"+mNetwork.networkID+"/"+transitScheduleFileNameOld);
//		configOrig.getModules().get("transit").addParam("transitScheduleFile","zurich_1pm/zurich_transit_schedule.xml.gz");
		Scenario scenarioMerged = ScenarioUtils.loadScenario(configMerged);
		TransitSchedule tsMerged = scenarioMerged.getTransitSchedule();
		
		// GO THROUGH MERGED SCHEDULE and make OD pairs (travel times are not necessary)
		List<ODRoutePairX> odPairsX = new ArrayList<ODRoutePairX>();
		for (MRoute mr : mNetwork.routeMap.values()) {
			for (TransitRoute tr : mr.transitLine.getRoutes().values()) {
				for (TransitRouteStop trsO : tr.getStops().subList(0, tr.getStops().size()/2)) {
					for (TransitRouteStop trsD : tr.getStops().subList(0, tr.getStops().size()/2)) {
						if (trsO.getStopFacility().getId().equals(trsD.getStopFacility().getId())) {
							continue;
						}
						Boolean odXAlreadyFeatured = false;
						for (ODRoutePairX odPairX : odPairsX) {
							if (GeomDistance.calculate(trsO.getStopFacility().getCoord(), odPairX.O)<300.0 
									&& GeomDistance.calculate(trsD.getStopFacility().getCoord(), odPairX.D)<300.0) {
								odXAlreadyFeatured = true;
								break;
							}
						}
						if (odXAlreadyFeatured.equals(false)) {
							odPairsX.add(new ODRoutePairX(trsO.getStopFacility().getCoord(), trsD.getStopFacility().getCoord(),
									trsO.getStopFacility().getName()+ "_" +trsD.getStopFacility().getName())); // one way
							odPairsX.add(new ODRoutePairX(trsD.getStopFacility().getCoord(), trsO.getStopFacility().getCoord(),
									trsD.getStopFacility().getName()+ "_" +trsO.getStopFacility().getName())); // and reverse (always added together)
						}
					}
				}
			}
		}
		
		// OLD Version: half automatic (failed bc not all stops may be featured in metroSchedule)
		//	List<Id<TransitStopFacility>> keyStops = Arrays.asList(
		//			Id.create("8503000_metro", TransitStopFacility.class),	//  TSF-ID  Hauptbahnhof = 8503000_metro
		//			Id.create("8503003_metro", TransitStopFacility.class),	//  TSF-ID  Stadelhofen = 8503003_metro 
		//			Id.create("8503059_metro", TransitStopFacility.class),	//  TSF-ID  StadelhofenFB = 8503059_metro 
		//			Id.create("8503006_metro", TransitStopFacility.class),  //  TSF-ID  Oerlikon = 8503006_metro
		//			Id.create("8503001_metro", TransitStopFacility.class),  //  TSF-ID  Altstetten = 8503001_metro
		//			Id.create("8503010_metro", TransitStopFacility.class)); //  TSF-ID  Enge = 8503010_metro
		//	List<Coord> keyStopsX = new ArrayList<Coord>();
		//	for (Id<TransitStopFacility> keyStop : keyStops) {
		//		keyStopsX.add(tsMerged.getFacilities().get(keyStop).getCoord());
		//	}
		
		// NEW Version: fully manual
		List<Coord> keyStopsX = Arrays.asList(
				new Coord(2683188.0, 1248066.0),  //  TSF-ID  Hauptbahnhof = 8503000_metro
				new Coord(2683804.0, 1246754.0),  //  TSF-ID  Stadelhofen = 8503003_metro | StadelhofenFB = 8503059_metro 
				new Coord(2683424.0, 1251770.0),  //  TSF-ID  Oerlikon = 8503006_metro
				new Coord(2679313.0, 1249496.0),  //  TSF-ID  Altstetten = 8503001_metro
				new Coord(2682503.0, 1246493.0)); //  TSF-ID  Enge = 8503010_metro

		
		List<Id<TransitStopFacility>> unservicedTSFs = new ArrayList<Id<TransitStopFacility>>();
		TransitSchedule tsMod = ScenarioUtils.loadScenario(ConfigUtils.createConfig()).getTransitSchedule();
		
		for (TransitLine tl : tsMerged.getTransitLines().values()) {
			tsMod.addTransitLine(Metro_TransitScheduleImpl.cloneTransitLine(tl, tsMerged, odPairsX, keyStopsX, unservicedTSFs));
		}
		
		for (TransitStopFacility tsf : tsMerged.getFacilities().values()) {
//			if (unservicedTSFs.contains(tsf.getId())) {
			if (false) {
				Log.write("Considering to clone TSF "+tsf.getId()+" ("+tsf.getName()+"). ");
				Boolean tsfFeaturedInTransitRoutes = false;
				tsSearchLoop:
				for (TransitLine tl : tsMerged.getTransitLines().values()) {
					for (TransitRoute tr : tl.getRoutes().values()) {
						for (TransitRouteStop trs : tr.getStops()) {
							if (trs.getStopFacility().getId().equals(tsf.getId())) {
								tsfFeaturedInTransitRoutes = true;
								break tsSearchLoop;
							}
						}
					}
				}
				if (tsfFeaturedInTransitRoutes) {
					tsMod.addStopFacility(Metro_TransitScheduleImpl.cloneTransitStopFacility(tsf, tsMerged.getFactory()));
					Log.write("Adding TSF anyways as it is featured in a new metro route "+tsf.getId()+" ("+tsf.getName()+"). ");
				}
				else {
					Log.write("StopFacility is not featured in any route since SBahn has jumped over it: "+tsf.getId()+" ("+tsf.getName()+"). "
							+ "Therefore not adding it to the new schedule.");
					// this is a dangerous operation for the case that the not added stop is called by any of the evaluation/evolution operations as it does not exist.
					// However, those operations will only call MergedSchedule before deleting certain TSFs and therefore find any TSF they look for.
					continue;
				}
			}
			else {
				tsMod.addStopFacility(Metro_TransitScheduleImpl.cloneTransitStopFacility(tsf, tsMerged.getFactory()));
			}
		}
		
		TransitScheduleWriter tswMod = new TransitScheduleWriter(tsMod);
		tswMod.writeFile("zurich_1pm/Evolution/Population/"+mNetwork.networkID+"/"+transitScheduleFileNameNew);
		return tsMod;
	}
	
	
	
	public static TransitStopFacility cloneTransitStopFacility(TransitStopFacility o, TransitScheduleFactory tsf) {
		TransitStopFacility copy = tsf.createTransitStopFacility(o.getId(), o.getCoord(), o.getIsBlockingLane());
		copy.setLinkId(o.getLinkId());
		copy.setName(o.getName());
		copy.setStopAreaId(o.getStopAreaId());
		return copy;
	}

	public static TransitLine cloneTransitLine(TransitLine tlOrig, TransitSchedule tsMerged, List<ODRoutePairX> odPairsX, List<Coord> keyStopsX, 
			List<Id<TransitStopFacility>> unservicedTSFs) throws IOException {
		TransitLine tlNew = tsMerged.getFactory().createTransitLine(tlOrig.getId());
		tlNew.setName(tlOrig.getName());
		String lineNr = null;
		if (tlOrig.getId().toString().contains("SBB_S")) {
			lineNr = tlOrig.getId().toString().split("_")[1];
		}
		
		for (TransitRoute trOrig : tlOrig.getRoutes().values()) {
			TransitRoute trNew = null;
			if (tlOrig.getId().toString().contains("SBB_S")) { // SBB
//			if (false) {
				trNew = speedUpSBahnRoute(trOrig, tsMerged, odPairsX, keyStopsX, unservicedTSFs);
				for (Departure d : trOrig.getDepartures().values()){				
					trNew.addDeparture(d);
				}
			}
			else { // DEFAULT
				trNew = tsMerged.getFactory().createTransitRoute(trOrig.getId(), trOrig.getRoute().clone(), Clone.list(trOrig.getStops()), trOrig.getTransportMode());
				for (Departure d : trOrig.getDepartures().values()){				
					trNew.addDeparture(d);
				}
			}
			tlNew.addRoute(trNew);
		}
		return tlNew;
	}
	
	
	public static TransitRoute speedUpSBahnRoute(TransitRoute trOrig, TransitSchedule tsMerged, List<ODRoutePairX> odPairsX, 
			List<Coord> keyStopsX, List<Id<TransitStopFacility>> unservicedTSFs) throws IOException {
		Config config = ConfigUtils.createConfig();
		config.getModules().get("network").addParam("inputNetworkFile", "zurich_1pm/Evolution/Population/BaseInfrastructure/GlobalNetwork.xml");
		Network globalNetwork = ScenarioUtils.loadScenario(config).getNetwork();
		// NetworkRoute trNewRoute = null;
		
		List<TransitRouteStop> trNewRouteStops = new ArrayList<TransitRouteStop>();
		// List<Id<Link>> newRouteLinkIds = new ArrayList<Id<Link>>();
		
//		Log.write("Trying stop sequence of transitRoute "+trOrig.getId().toString()+" :");
		for (TransitRouteStop trs : trNewRouteStops) {
//			Log.write("Stop "+trs.getStopFacility()+"  ("+trs.getStopFacility().getName()+")");
		}
		if (trOrig.getStops().size()<3) { // size 2 or smaller means no intermediate stops to clear --> this route can be seen as cut already.
			trNewRouteStops.addAll(trOrig.getStops());
			// trNewRoute = trOrig.getRoute();
		}
		else {
			trNewRouteStops.addAll(trOrig.getStops());
			Boolean stopsRemovedThisIter = true;
			while (stopsRemovedThisIter) {
//				Log.write("stopsRemovedThisIter = "+stopsRemovedThisIter);
				stopsRemovedThisIter = false;
				ArrayList<TransitRouteStop> routeStopsCutTemp = new ArrayList<TransitRouteStop>();
				routeStopsCutTemp.addAll(trNewRouteStops);
				
				OuterLoop:
				for (TransitRouteStop trsO : trNewRouteStops.subList(0, trNewRouteStops.size()-2)) {	// -2 bc last intermediate stop that can be cleared is size-1
//					Log.write("Origin Route Stop = "+trsO.getStopFacility()+"  ("+trsO.getStopFacility().getName()+")");
					for (Coord keyCoord : keyStopsX) {
						if (GeomDistance.calculate(keyCoord, trNewRouteStops.get(trNewRouteStops.indexOf(trsO)+1).getStopFacility().getCoord()) < 400.0) {
							continue OuterLoop; // do not look for intermediate stops down the route if next stop down the route is a key stop -> Set O-stop to the keyStop.
						}
					}	
					for (TransitRouteStop trsD : trNewRouteStops.subList(trNewRouteStops.indexOf(trsO)+2, trNewRouteStops.size())) {
//						Log.write("Destination Route Stop = "+trsD.getStopFacility()+"  ("+trsD.getStopFacility().getName()+")");
						for (ODRoutePairX odPairX : odPairsX) {
//							Log.write("OD ");
							if (GeomDistance.calculate(odPairX.O, trsO.getStopFacility().getCoord()) < 400.0
									&& GeomDistance.calculate(odPairX.D, trsD.getStopFacility().getCoord()) < 400.0) {
//								Log.write("Cutting intermediate stops between OD-pair "+odPairX.odPairNames);
								// Log.write("Found metro OD-pair: "+odPairX.odStopPairNames); // make additional field with names for this here!
								// clear trNewRouteStops and only add again the stops before and after the od-pair (=clear intermediate stops)
								// XXX update the times after the intermediate stops (arrival/departure sooner)
								Integer indexO = trNewRouteStops.indexOf(trsO);
								Integer indexD = trNewRouteStops.indexOf(trsD);
								routeStopsCutTemp.clear();
								routeStopsCutTemp.addAll(trNewRouteStops.subList(0, indexO+1));
								// note all intermediate stops which are not serviced
//								for (TransitRouteStop unservicedTSF : trNewRouteStops.subList(indexO+1, indexD)) {
//									if ( ! unservicedTSFs.contains(unservicedTSF.getStopFacility().getId())) {
//										unservicedTSFs.add(unservicedTSF.getStopFacility().getId());
//										Log.write("Adding unserviced StopFacility: "+unservicedTSF.getStopFacility().getId()+" ("+
//										unservicedTSF.getStopFacility().getName()+")");
//									}
//								}
								routeStopsCutTemp.addAll(trNewRouteStops.subList(indexD, trNewRouteStops.size()));
								//update times (make ANOTHER temp route copy first, where you can have updated times and then go over temp route to update w. new times)
								ArrayList<TransitRouteStop> routeStopsTimeUpdatedTemp = new ArrayList<TransitRouteStop>();
								routeStopsTimeUpdatedTemp.addAll(routeStopsCutTemp.subList(0, indexO+1));	// add those stops without time gains
								Integer nClearedStops = indexD-indexO-1;
								Double timeGainsPerClearedStop = 92.0; // seconds
									// (comp. S15 to S9 between Uster->Stadelhofen | 4 clearedStops = 7*60s timeGains)
									// (comp. S25 to S8 between HB->Wädenswil      | 9 clearedStops = 12*60s timeGains)
								for (TransitRouteStop stopToUpdateTime : routeStopsCutTemp.subList(indexO+1, routeStopsCutTemp.size())) {
									TransitRouteStop timeUpdatedStop = tsMerged.getFactory().createTransitRouteStop(
											Metro_TransitScheduleImpl.cloneTransitStopFacility(stopToUpdateTime.getStopFacility(), tsMerged.getFactory()),
											stopToUpdateTime.getArrivalOffset()-nClearedStops*timeGainsPerClearedStop,
											stopToUpdateTime.getDepartureOffset()-nClearedStops*timeGainsPerClearedStop);
									timeUpdatedStop.setAwaitDepartureTime(true);
									routeStopsTimeUpdatedTemp.add(timeUpdatedStop);
								}
								stopsRemovedThisIter = true;
								routeStopsCutTemp.clear();
								routeStopsCutTemp.addAll(routeStopsTimeUpdatedTemp);
								break OuterLoop;
							}
						}
						for (Coord keyCoord : keyStopsX) {
							if (GeomDistance.calculate(keyCoord, trNewRouteStops.get(trNewRouteStops.indexOf(trsD)).getStopFacility().getCoord()) < 400.0) {
								// if od-pair was not detected (so we don't jump out of the loops), but D-stop is a key stop,
								// we must stop roaming along inner loop and proceed with outer loop. We do this until outer loop stop is the one before key stop
								// when the outer loop moves onto the key stop and opens up the search along inner loop again down the route until the next key stop
								continue OuterLoop;
							}
						}
					}
				}
				trNewRouteStops.clear();
				trNewRouteStops.addAll(routeStopsCutTemp);
			}
			// trNewRoute = RouteUtils.createNetworkRoute(newRouteLinkIds, globalNetwork);
		}	// else loop
		
//			// Display cut routes
//			for (Entry<String, ArrayList<Id<TransitStopFacility>>> routeStopsEntry : routeStopsMapX.entrySet()) {
//				Log.write("Cut SBahn Line "+routeStopsEntry.getKey()+":");
//				for (Id<TransitStopFacility> tsf : routeStopsEntry.getValue()) {
//					if (tsMetro.getFacilities().containsKey(tsf)) {
//						Log.write(tsf.toString() + " = " + tsMetro.getFacilities().get(tsf).getName());					
//					}
//					else if(tsOrig.getFacilities().containsKey(tsf)) {
//						Log.write("(not featured in metro schedule) "+tsf.toString() + " = " + tsOrig.getFacilities().get(tsf).getName());
//					}
//					else {
//						Log.write("(not featured in any schedule) "+tsf.toString());
//					}
//				}
//			}	
		
		TransitRoute trNew = tsMerged.getFactory().createTransitRoute(trOrig.getId(), trOrig.getRoute().clone(), trNewRouteStops, trOrig.getTransportMode());
		return trNew;
	}

	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	@Deprecated
	public static void FastSBahnModule(MNetwork mNetwork) throws IOException {
		
		// every sBahnStopSequence has several subRoutes as it is in the zurich_1pm transitSchedule file
		Log.write("  >> Modifying ZH SBahn network for "+mNetwork.networkID+". Optimize stop sequences with new metro capacities.");
		Config config = ConfigUtils.createConfig();
		config.getModules().get("transit").addParam("transitScheduleFile","zurich_1pm/Evolution/Population/"+mNetwork.networkID+"/MergedSchedule.xml");
//		configOrig.getModules().get("transit").addParam("transitScheduleFile","zurich_1pm/zurich_transit_schedule.xml.gz");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		TransitSchedule tsMetro = scenario.getTransitSchedule();

		
		
		// go through metro schedule and make OD pairs with travel times (update shortest travel times if found a faster one)
		// make converter from original stopFacilityId <> newFacilityId
		// convert original dominant lines to new system TransitRoute with arr/dep times
		// run along these lines and remove intermediate stops if found OD pair (update immediately travel times arr/dep)
		
		// GO THROUGH METRO SCHEDULE and make OD pairs (travel times are not necessary)
//		List<ODRoutePair> odPairs = new ArrayList<ODRoutePair>();
		List<ODRoutePairX> odPairsX = new ArrayList<ODRoutePairX>();
		for (MRoute mr : mNetwork.routeMap.values()) {
			for (TransitRoute tr : mr.transitLine.getRoutes().values()) {
				for (TransitRouteStop trsO : tr.getStops().subList(0, tr.getStops().size()/2)) {
					for (TransitRouteStop trsD : tr.getStops().subList(0, tr.getStops().size()/2)) {
						if (trsO.getStopFacility().getId().equals(trsD.getStopFacility().getId())) {
							continue;
						}
//						Boolean odAlreadyFeatured = false;
						Boolean odXAlreadyFeatured = false;
//						for (ODRoutePair odPair : odPairs) {
//							if (trsO.getStopFacility().getId().equals(odPair.O)
//									&& trsD.getStopFacility().getId().equals(odPair.D)) {
//								odAlreadyFeatured = true;
//								break;
//							}
//						}
						for (ODRoutePairX odPairX : odPairsX) {
							if (GeomDistance.calculate(trsO.getStopFacility().getCoord(), odPairX.O)<300.0 
									&& GeomDistance.calculate(trsD.getStopFacility().getCoord(), odPairX.D)<300.0) {
								odXAlreadyFeatured = true;
								break;
							}
						}
//						if (odAlreadyFeatured.equals(false)) {
//							odPairs.add(new ODRoutePair(trsO.getStopFacility().getId(), trsD.getStopFacility().getId()));		// one way
//							odPairs.add(new ODRoutePair(trsD.getStopFacility().getId(), trsO.getStopFacility().getId()));		// and reverse (always added together)
//						}
						if (odXAlreadyFeatured.equals(false)) {
							odPairsX.add(new ODRoutePairX(trsO.getStopFacility().getCoord(), trsD.getStopFacility().getCoord()));		// one way
							odPairsX.add(new ODRoutePairX(trsD.getStopFacility().getCoord(), trsO.getStopFacility().getCoord()));		// and reverse (always added together)
						}
					}
				}
			}
		}

		
		// EXTRACT all rail lines and routes (dirty form):
		
		Map<String, ArrayList<ArrayList<TransitRouteStop>>> sBahnStopSequences = new HashMap<String, ArrayList<ArrayList<TransitRouteStop>>>();
		for (TransitLine tl : tsMetro.getTransitLines().values()) {
			if (tl.getId().toString().contains("SBB_S")) {
				String lineNr = tl.getId().toString().split("_")[1];
//				Log.write("Scanning SBahn line "+lineNr);
				for (TransitRoute tr : tl.getRoutes().values()) {
					ArrayList<TransitRouteStop> routeStops = new ArrayList<TransitRouteStop>();
					routeStops.addAll(tr.getStops());
					if (sBahnStopSequences.containsKey(lineNr)) {
						sBahnStopSequences.get(lineNr).add(routeStops);
					}
					else {
						sBahnStopSequences.put(lineNr, new ArrayList<ArrayList<TransitRouteStop>>());
						sBahnStopSequences.get(lineNr).add(routeStops);
					}
				}
			}
		}
		
		List<Id<TransitStopFacility>> keyStops = Arrays.asList(
				Id.create("8503000_metro", TransitStopFacility.class),	//  TSF-ID  Hauptbahnhof = 8503000_metro
				Id.create("8503003_metro", TransitStopFacility.class),	//  TSF-ID  Stadelhofen = 8503003_metro 
				Id.create("8503059_metro", TransitStopFacility.class),	//  TSF-ID  StadelhofenFB = 8503059_metro 
				Id.create("8503006_metro", TransitStopFacility.class),  //  TSF-ID  Oerlikon = 8503006_metro
				Id.create("8503001_metro", TransitStopFacility.class),  //  TSF-ID  Altstetten = 8503001_metro
				Id.create("8503010_metro", TransitStopFacility.class)); //  TSF-ID  Enge = 8503010_metro

		List<Coord> keyStopsX = new ArrayList<Coord>();
		for (Id<TransitStopFacility> keyStop : keyStops) {
			keyStopsX.add(tsMetro.getFacilities().get(keyStop).getCoord());
		}
		
		
// ----------------------------------
		
		// EXTRACT in handy form the dominant rail lines:
		// transitStopsMap: Main stop sequence of this line (TransitRouteStops with departures etc.)
		// routeStopsMap: Main stop sequence of this line (TransitStopFacilities)
		Map<String, ArrayList<TransitRouteStop>> transitStopsMap = new HashMap<String, ArrayList<TransitRouteStop>>();
		Map<String, ArrayList<TransitStopFacility>> routeStopsMap = new HashMap<String, ArrayList<TransitStopFacility>>();

		for (Entry<String, ArrayList<ArrayList<TransitRouteStop>>> sBahnStopSequence : sBahnStopSequences.entrySet()) {
			String thisLine = sBahnStopSequence.getKey();
			ArrayList<ArrayList<TransitRouteStop>> thisLineStopSequences = sBahnStopSequence.getValue();
			Log.write("This line = "+thisLine + " has #StopSequences="+thisLineStopSequences.size());
			ArrayList<TransitRouteStop> thisLineLongestStopSequence = new ArrayList<TransitRouteStop>();
			Integer maxStopSequenceLength = 0;
			for (ArrayList<TransitRouteStop> stopSequence : thisLineStopSequences) {
				if (stopSequence.size() > maxStopSequenceLength) {
					maxStopSequenceLength = stopSequence.size();
					thisLineLongestStopSequence = stopSequence;
				}
			}
			transitStopsMap.put(thisLine, thisLineLongestStopSequence);
			ArrayList<TransitStopFacility> thisLineLongestStopSequenceFacilitiesOnly = new ArrayList<TransitStopFacility>();
			// Log.write("Longest stop sequence:");
			for (TransitRouteStop trs : thisLineLongestStopSequence) {
				thisLineLongestStopSequenceFacilitiesOnly.add(trs.getStopFacility());
				// Log.write(trs.getStopFacility().getId().toString() + "  ("+trs.getStopFacility().getName()+")");
			}
			routeStopsMap.put(thisLine, thisLineLongestStopSequenceFacilitiesOnly);
		}
		
		

		// CONVERTER from original stopFacilityId <> newFacilityId (see separate method)
		// Map<String=superName, CustomStop=originalMainFacility,newStopFacility> railStops
			// NetworkEvolutionImpl row 2360
			// String superName = OriginalStopId.substring(0, OriginalStopId.indexOf("."));
			// stopFacility has id="8500562.link:920757". First part is unique to the stop, but it can have several refLinks (second part)
			// String newMetroStopFacilityId = oldStopSuperName+"_metro";

		// convert original dominant lines to new system TransitRoute with arr/dep times
//		Map<String, ArrayList<TransitRouteStop>> transitStopsMapX = new HashMap<String, ArrayList<TransitRouteStop>>();
		Map<String, ArrayList<Id<TransitStopFacility>>> routeStopsMapX = new HashMap<String, ArrayList<Id<TransitStopFacility>>>();
		Map<Id<TransitStopFacility>, Id<TransitStopFacility>> idsConversionTable = new HashMap<Id<TransitStopFacility>, Id<TransitStopFacility>>();
		
		for (Entry<String, ArrayList<TransitRouteStop>> transitRouteStops : transitStopsMap.entrySet()) {
			ArrayList<Id<TransitStopFacility>> newSystemStopFacilityList = new ArrayList<Id<TransitStopFacility>>();
			for (TransitRouteStop trs : transitRouteStops.getValue()) {
				Id<TransitStopFacility> newSystemStopFacilityId = TransitStopFacilityOld2NewId(trs.getStopFacility().getId());
				newSystemStopFacilityList.add(newSystemStopFacilityId);
				idsConversionTable.put(newSystemStopFacilityId, trs.getStopFacility().getId());
//				transitStopsMapX.put(transitRouteStops.getKey(), tsMetro.getFactory().createTransitRouteStop(stop, arrivalDelay, departureDelay));
			}
			routeStopsMapX.put(transitRouteStops.getKey(), newSystemStopFacilityList);
		}
//		List<ODRoutePair> odPairsOrig = new ArrayList<ODRoutePair>();
			
		
		
		Map<String, ArrayList<Id<TransitStopFacility>>> routeStopsMapXCut = new HashMap<String, ArrayList<Id<TransitStopFacility>>>();
		for (Entry<String, ArrayList<Id<TransitStopFacility>>> routeStopsEntry : routeStopsMapX.entrySet()) {
			ArrayList<Id<TransitStopFacility>> routeStops = routeStopsEntry.getValue();
			if (routeStops.size()<3) {	// size 2 or smaller means no intermediate stops to clear --> this route can be seen as cut already.
				routeStopsMapXCut.put(routeStopsEntry.getKey(), routeStops);
				continue;
			}
			ArrayList<Id<TransitStopFacility>> routeStopsCut = new ArrayList<Id<TransitStopFacility>>();
			routeStopsCut.addAll(routeStops);
			Boolean stopsRemovedThisIter = true;
			while (stopsRemovedThisIter) {
				stopsRemovedThisIter = false;
				ArrayList<Id<TransitStopFacility>> routeStopsCutTemp = new ArrayList<Id<TransitStopFacility>>();
				routeStopsCutTemp.addAll(routeStopsCut);
				
				OuterLoop:
				for (Id<TransitStopFacility> tsfO : routeStopsCut.subList(0, routeStopsCut.size()-2)) {	// -2 bc last intermediate stop that can be cleared is size-1
					if (keyStops.contains(routeStopsCut.get(routeStopsCut.indexOf(tsfO)+1))) {
						continue OuterLoop; // do not look for intermediate stops down the route if next stop down the route is a key stop -> Set O-stop to the keyStop.
					}
					for (Id<TransitStopFacility> tsfD : routeStopsCut.subList(routeStopsCut.indexOf(tsfO)+2, routeStopsCut.size())) {
//						for (ODRoutePair odPair : odPairs) {
//							if (odPair.O.equals(tsfO) && odPair.D.equals(tsfD)) {
//								Log.write("Found metro OD-pair: "+tsMetro.getFacilities().get(odPair.O).getName()+" / "+tsMetro.getFacilities().get(odPair.D).getName());
//								// third condition makes sure the od-pair is not just adjacent, but it runs along several stops
//								// clear routeStopsCutTemp and only add again the stops before and after the od-pair (=clear intermediate stops)
//								routeStopsCutTemp.clear();
//								routeStopsCutTemp.addAll(routeStopsCut.subList(0, routeStopsCut.indexOf(tsfO)));
//								routeStopsCutTemp.addAll(routeStopsCut.subList(routeStopsCut.indexOf(tsfD), routeStopsCut.size()));
//								stopsRemovedThisIter = true;
//								break OuterLoop;
//							}
//						}
						if (keyStops.contains(tsfD)) {
							// if od-pair was not detected (so we don't jump out of the loops), but D-stop is a key stop,
							// we must stop roaming along inner loop and proceed with outer loop. We do this until outer loop stop is the one before key stop
							// when the outer loop moves onto the key stop and opens up the search along inner loop again down the route until the next key stop
							continue OuterLoop; 
						}
					}
				}
				routeStopsCut.clear();
				routeStopsCut.addAll(routeStopsCutTemp);
			}
			routeStopsMapXCut.put(routeStopsEntry.getKey(), routeStopsCut);
		}
		// Display cut routes
		for (Entry<String, ArrayList<Id<TransitStopFacility>>> routeStopsEntry : routeStopsMapX.entrySet()) {
			Log.write("Cut SBahn Line "+routeStopsEntry.getKey()+":");
			for (Id<TransitStopFacility> tsf : routeStopsEntry.getValue()) {
				if (tsMetro.getFacilities().containsKey(tsf)) {
					Log.write(tsf.toString() + " = " + tsMetro.getFacilities().get(tsf).getName());					
				}
//				else if(tsOrig.getFacilities().containsKey(tsf)) {
//					Log.write("(not featured in metro schedule) "+tsf.toString() + " = " + tsOrig.getFacilities().get(tsf).getName());
//				}
//				else {
//					Log.write("(not featured in any schedule) "+tsf.toString());
//				}
			}
		}
	}


	public static Id<TransitStopFacility> TransitStopFacilityOld2NewId(Id<TransitStopFacility> OriginalStopId) {
		String newIdString = OriginalStopId.toString().substring(0, OriginalStopId.toString().indexOf("."));
		return Id.create(newIdString+"_metro", TransitStopFacility.class);
	}
	
	
	
}