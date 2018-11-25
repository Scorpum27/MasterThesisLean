package ch.ethz.matsim.students.samark;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.FacilityWrapperActivity;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.FacilitiesFromPopulation;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.OpeningTime;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.VehicleType;

import com.google.common.collect.Sets;


public class VC_NetworkImpl {

	public static void main(String[] args) throws IOException {
		double popCutPercentage = 0.4;
		int xmax = 100;
		int ymax = 100;
		double unitLinkLength = 100.0;		
		double minStopDistance = 400.0; // original bus stop spacing
		int nBusRoutes = 13;
		double minTerminalDist = Math.sqrt(xmax*xmax+ymax*ymax)*unitLinkLength*0.7;
		
	// create network
		Config config = ConfigUtils.createConfig();
		Scenario sceanrio = ScenarioUtils.loadScenario(config);
		Network network = sceanrio.getNetwork();
		network = fillNetwork(xmax, ymax, network, unitLinkLength);
		Integer nrRectanglesToBeRemoved = 15;
		Integer maxRemovalSizeFraction = 4;
		List<RectX> rectanglesToBeRemoved = new ArrayList<RectX>();
		rectanglesToBeRemoved.add(new RectX(10, 20, 10, 20));
		rectanglesToBeRemoved.add(new RectX(36, 64, 36, 64));
		rectanglesToBeRemoved.add(new RectX(58, 68, 58, 68));
		rectanglesToBeRemoved.add(new RectX(70, 85, 20, 35));
		rectanglesToBeRemoved.add(new RectX(12, 24, 60, 80));

		network = removeRectangluarSection(rectanglesToBeRemoved, nrRectanglesToBeRemoved, maxRemovalSizeFraction, xmax, ymax, network, unitLinkLength);
		Integer nDiagonalsToBeBuilt = 50;
		network = buildDiagonals(network, nDiagonalsToBeBuilt, xmax, ymax);
//		NetworkWriter nw = new NetworkWriter(network); nw.write("zurich_1pm/VC_files/virtualCityNetwork.xml");
		NetworkWriter nw = new NetworkWriter(network); nw.write("zurich_1pm/VC_files/zurich_network.xml.gz");
		
	// create infrastructure + population
		Population popVC = cutPopulation(popCutPercentage); // do this only once to get a cut population! Load it after that
//		Config configZh1pm = ConfigUtils.createConfig();
//		configZh1pm.getModules().get("plans").addParam("inputPlansFile", "zurich_1pm/VC_files/zurich_population_0."+((int)(10*popCutPercentage))+"pm.xml.gz");		
//		Population popVC = ScenarioUtils.loadScenario(configZh1pm).getPopulation();
		ActivityFacilities rebuiltFacilities = rebuildFacilities(network, popVC, popCutPercentage, xmax, ymax, unitLinkLength);
		popVC = rebuildPlans(network, popVC, rebuiltFacilities, popCutPercentage);
		
	// create pt
//		// List<TransitStopFacility> busStopFacilities = createStopFacilities(network);
//		Node keyNode1a = network.getNodes().get(Id.createNodeId(Integer.toString(xmax/2)));
//		Node keyNode1b = network.getNodes().get(Id.createNodeId(Integer.toString((xmax+1)*(ymax/2)+6)));
//		Node keyNode1c = network.getNodes().get(Id.createNodeId(Integer.toString((xmax+1)*(ymax-4)-4)));
//		Node keyNode2a = network.getNodes().get(Id.createNodeId(Integer.toString((xmax+1)*(ymax-3)+2)));
//		Node keyNode2b = network.getNodes().get(Id.createNodeId(Integer.toString((xmax+1)*(3)-5)));
//		Node keyNode3a = network.getNodes().get(Id.createNodeId(Integer.toString((xmax+1)*(ymax-4)-9)));
//		Node keyNode3b = network.getNodes().get(Id.createNodeId(Integer.toString(xmax+2)));
//		Node keyNode3c = network.getNodes().get(Id.createNodeId(Integer.toString((xmax+1)*(ymax-4)-4)));
//		// TODO make coord2NodeMapper where one can give keyCoord instead of keyNodes (searches closest node to coord and does not select a removed node)
//		List<List<Node>> keyRouteNodeSequences = Arrays.asList(
//				Arrays.asList(keyNode1a, keyNode1b, keyNode1c), Arrays.asList(keyNode2a, keyNode2b), Arrays.asList(keyNode3a, keyNode3b, keyNode3c));
////		System.out.println(keyRouteNodeSequences);
		List<List<Coord>> manualKeyRoutingPoints = Arrays.asList(
				Arrays.asList(new Coord(0.0, 0.0), new Coord(unitLinkLength*xmax, unitLinkLength*ymax)),
				Arrays.asList(new Coord(unitLinkLength*xmax, 0.0), new Coord(0.0, unitLinkLength*ymax))
				);
		List<List<Node>> keyRouteNodeSequences = createKeyNodeSequences(nBusRoutes, manualKeyRoutingPoints, network, minTerminalDist, xmax, unitLinkLength);
		ArrayList<NetworkRoute> busRoutes = createInitialRoutes(network, keyRouteNodeSequences);
		applyPtSchedule(busRoutes, network, minStopDistance);
	}
	


	public static List<List<Node>> createKeyNodeSequences(int nRoutes, List<List<Coord>> manualKeyRoutingPoints,
			Network network, Double minTerminalDist, int xmax, double unitLinkLength) {
		List<List<Node>> keyRouteNodeSequences = new ArrayList<List<Node>>();
		for (List<Coord> coordList : manualKeyRoutingPoints) {
			List<Node> nodeList = new ArrayList<Node>();
			for (Coord coord : coordList) {
				nodeList.add(coord2NextNode(coord, network, xmax, unitLinkLength));
			}
			keyRouteNodeSequences.add(nodeList);
		}
		while (keyRouteNodeSequences.size() < nRoutes) {
			List<Node> terminalPair = getTerminalNodePair(network, minTerminalDist);
			if (terminalPair != null) {
				keyRouteNodeSequences.add(terminalPair);				
			}
		}
		return keyRouteNodeSequences;
	}



	public static List<Node> getTerminalNodePair(Network network, Double minTerminalDist) {
		double terminalDist = 0.0;
		int tries = 0;
		Node startTerminal = null;
		Node endTerminal = null;
		while(terminalDist < minTerminalDist && tries < 10000) {
			tries++;
			int rStart = (new Random()).nextInt(network.getNodes().size());
			int rEnd = (new Random()).nextInt(network.getNodes().size());
			int countStart = 0;
			int countEnd = 0;
			for (Node node : network.getNodes().values()) {
				if (countStart == rStart) {
					startTerminal = node;
					break;
				}
				countStart++;
			}
			for (Node node : network.getNodes().values()) {
				if (countEnd == rEnd) {
					endTerminal = node;
					break;
				}
				countEnd++;
			}
			terminalDist = GeomDistance.betweenNodes(startTerminal, endTerminal);
		}
		List<Node> terminalPair = Arrays.asList(startTerminal, endTerminal);
		return terminalPair;
	}



	public static Node coord2NextNode(Coord coord, Network network, int xmax, double unitLinkLength) {
		Node closestNode = null;
		int virtualNodeNr = xy2NodeNr((int) (Math.round(coord.getX()/unitLinkLength)), (int) (Math.round(coord.getY()/unitLinkLength)), xmax);
		if (network.getNodes().containsKey(Id.createNodeId(virtualNodeNr))) {
			return network.getNodes().get(Id.createNodeId(virtualNodeNr));
		}
		else {
			Double clostestDistance = Double.MAX_VALUE;
			for (Node node : network.getNodes().values()) {
				Double dist = GeomDistance.calculate(coord, node.getCoord());
				if (dist < clostestDistance) {
					closestNode = node;
					clostestDistance = dist;
				}
			}
			return closestNode;
		}		
	}



	public static Population rebuildPlans(Network network, Population popVC, ActivityFacilities rebuiltFacilities, double popCutPercentage) throws IOException {
		
//		Population updatedPopVC = ScenarioUtils.loadScenario(ConfigUtils.createConfig()).getPopulation();
		
		for (Person person : popVC.getPersons().values()) {
//			Person clonePerson = updatedPopVC.getFactory().createPerson(Id.createPersonId(person.getId()));
//			clonePerson.
			List<Plan> originalPersonPlans = new ArrayList<Plan>();
			List<Plan> rebuiltPersonPlans = new ArrayList<Plan>();
			for (Plan originalPlan : person.getPlans()) {
				originalPersonPlans.add(originalPlan);
			}
			for (Plan plan : originalPersonPlans) {
				rebuiltPersonPlans.add(rebuildPlan(plan, popVC, rebuiltFacilities));
			}
			for (Plan originalPlan : originalPersonPlans) {
				person.removePlan(originalPlan);
			}
			for (Plan rebuiltPlan : rebuiltPersonPlans) {
				person.addPlan(rebuiltPlan);
			}
			
		}
		PopulationWriter pw = new PopulationWriter(popVC);
		pw.write("zurich_1pm/VC_files/zurich_population.xml.gz");
		return popVC;
	}



	public static Plan rebuildPlan(Plan planIn, Population popVC, ActivityFacilities rebuiltFacilities) throws IOException {
//			Log.write("XXXXX - Plan before modification = "+planIn.toString());
//			for (PlanElement e : planIn.getPlanElements()) {
//				Log.write(e.toString());
//			}
		Plan planOut = popVC.getFactory().createPlan();

		int count = 0;
		Coord lastActivityCoord = null;
		Id<Link> lastActivityLinkId = null;
			
		for (PlanElement e : planIn.getPlanElements()) {
//			Log.write("Plan Element before modification = "+e.toString());
			if (e instanceof Activity) {
				Activity act = (Activity) e;
//				Log.write(act.toString());
				if (act.getType().equals("pt_interaction") || act.getType().equals("pt interaction")) {
					Activity actCopy = popVC.getFactory().createActivityFromCoord(act.getType(),lastActivityCoord);
					actCopy.setLinkId(lastActivityLinkId);
					actCopy.setMaximumDuration(act.getMaximumDuration());
					planOut.addActivity(actCopy);
					lastActivityCoord = actCopy.getCoord();
					lastActivityLinkId = actCopy.getLinkId();
				}
				else {
					Id<ActivityFacility> actFacilityId = act.getFacilityId();
					ActivityFacility actFacility = rebuiltFacilities.getFacilities().get(actFacilityId);
					if (actFacility == null) {
						Log.write("X Facility is not found in new facilities = "+actFacilityId.toString());
						count++;
						continue;
					}
					Activity actCopy = popVC.getFactory().createActivityFromCoord(act.getType(), actFacility.getCoord());
					actCopy.setLinkId(actFacility.getLinkId());
//					Log.write("Link ID = " + actFacility.getLinkId());
					actCopy.setFacilityId(act.getFacilityId());
					actCopy.setStartTime(act.getStartTime());
					actCopy.setEndTime(act.getEndTime());
					planOut.addActivity(actCopy);
					lastActivityCoord = actCopy.getCoord();
					lastActivityLinkId = actCopy.getLinkId();
				}
				
			}
			else if (e instanceof Leg){
				planOut.addLeg(popVC.getFactory().createLeg(((Leg) e).getMode()));
			}
//			Log.write("Plan Element after modification = "+e.toString());
		}
//		Log.write("YYYYY - Plan after modification = "+planIn.toString());
//		for (PlanElement e : planIn.getPlanElements()) {
//			Log.write(e.toString());
//		}
		return planOut;
	}



// %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%      SCENARIO      %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%	
	
	public static ActivityFacilities rebuildFacilities(Network network, Population popVC, double popPercentage, int xmax, int ymax, double unitLinkLength) throws IOException {
		Config configZh = ConfigUtils.loadConfig("zurich_1pm/zurich_config.xml");
		// load existing facilities in ZH
		Collection<? extends ActivityFacility> activityFacilitiesZH = ScenarioUtils.loadScenario(configZh).getActivityFacilities().getFacilities().values();
		// list facilities, which were actually used by agents
		List<Id<ActivityFacility>> activityFacilitiesInInitialPlans = new ArrayList<Id<ActivityFacility>>();
		for (Person person : popVC.getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				for (PlanElement e : plan.getPlanElements()) {
					if (e instanceof Activity) {
						Activity act = (Activity) e;
						if (!activityFacilitiesInInitialPlans.contains(act.getFacilityId()))
						activityFacilitiesInInitialPlans.add(act.getFacilityId());
					}
				}
			}
		}
		// make a new facilities file, where all facilities are featured that were used by initial plans in ZH scenario
		//   + some other random facilities for location choice
		System.out.println("Number of activity facilities in ZH = "+activityFacilitiesZH.size());
		ActivityFacilities activityFacilitiesCut = ScenarioUtils.loadScenario(ConfigUtils.createConfig()).getActivityFacilities();
		for (ActivityFacility origFacility : activityFacilitiesZH) {
			if (activityFacilitiesInInitialPlans.contains(origFacility.getId()) || (new Random()).nextDouble() < popPercentage) {
				ActivityFacility activityFacility = rebuildFacilityIntoVC(origFacility, activityFacilitiesCut.getFactory(), network, xmax, ymax, unitLinkLength);
				if (activityFacility != null) {
					activityFacilitiesCut.addActivityFacility(activityFacility);
				}
				else {
					Log.write("ActivityFacility could not be added: "+origFacility.getActivityOptions().toString());
				}
			}
		}
		
		new FacilitiesWriter(activityFacilitiesCut).write("zurich_1pm/VC_files/zurich_facilities.xml.gz");
		return activityFacilitiesCut;
	}
	
	public static ActivityFacility rebuildFacilityIntoVC(ActivityFacility origFacility, ActivityFacilitiesFactory factory, Network network,
			int xmax, int ymax, double unitLinkLength) {
		// make new coord and link... then set these for rebuild facility
		Link linkVC = activityFacilityPlacerVC(origFacility, network, xmax, ymax, unitLinkLength);
		if (linkVC == null) {
			return null;
		}
		Coord coordVC = linkVC.getFromNode().getCoord();	// center, or toNode, or fromNode
		
		ActivityFacility facilityRebuild = factory.createActivityFacility(Id.create(origFacility.getId().toString(), ActivityFacility.class), linkVC.getId());
		facilityRebuild.setCoord(coordVC);
		
		for (ActivityOption option : origFacility.getActivityOptions().values()) {
			ActivityOption optionCopy = factory.createActivityOption(option.getType());
			optionCopy.setCapacity(option.getCapacity());
			for (OpeningTime openingTime : option.getOpeningTimes()) {
				optionCopy.addOpeningTime(openingTime);
			}
			facilityRebuild.addActivityOption(optionCopy);			
		}
		return facilityRebuild;
	}



	public static Link activityFacilityPlacerVC(ActivityFacility origFacility, Network network, int xmax, int ymax, double unitLinkLength) {
		// Random facility placement (on a link) - but enhance certain areas for certain facilities!

		Link facilityLink;
		
		String facilityType = "";
		for (String actType : origFacility.getActivityOptions().keySet()) {
			if (actType.contains("work")) {
				facilityType = "work";
				break;
			}
			if (actType.contains("home")) {
				facilityType = "home";
				break;
			}
		}
		if (facilityType.equals("work")) {
			Double rD = (new Random()).nextDouble();
			if (rD < 0.27) {
				facilityLink = getRandomLink(Math.floorDiv(xmax, 4), Math.floorDiv(xmax, 2), (int) (Math.round(ymax*0.67)), ymax, network, xmax);
			}
			else if (rD < 0.54) {
				facilityLink = getRandomLink(Math.floorDiv(xmax, 2), xmax, (int) (Math.round(ymax*0.67)), ymax, network, xmax);
			}
			else if (rD < 0.81) {
				facilityLink = getRandomLink((int) (Math.round(xmax*0.33)), (int) (Math.round(xmax*0.67)), 0, (int) (Math.round(ymax*0.67)), network, xmax);
			}
			else {
				facilityLink = getRandomLink(0, xmax, 0, ymax, network, xmax);
			}
		}
		else if(facilityType.equals("home")) {
			Double rD = (new Random()).nextDouble();
			if (rD < 0.33) {
				facilityLink = getRandomLink(0, (int) (Math.round(xmax*0.33)), (int) (Math.round(ymax*0.33)), (int) (Math.round(ymax*0.67)), network, xmax);
			}
			else if (rD < 0.67) {
				facilityLink = getRandomLink((int) (Math.round(xmax*0.67)), xmax, 0, (int) (Math.round(ymax*0.67)), network, xmax);
			}
			else {
				facilityLink = getRandomLink(0, xmax, 0, ymax, network, xmax);
			}
		}
		else {
			facilityLink = getRandomLink(0, xmax, 0, ymax, network, xmax);
		}
		
		return facilityLink;
	}



	public static Link getRandomLink(int x0, int x1, int y0, int y1, Network network, int xmax) {
		int iter = 0;
		while(iter<10000) {
			iter++;
			int randomX = x0 + (new Random()).nextInt(x1-x0+1);
			int randomY = y0 + (new Random()).nextInt(y1-y0+1);
			int nodeNr = xy2NodeNr(randomX, randomY, xmax);
			if (network.getNodes().containsKey(Id.createNodeId(nodeNr))) {
				Node node = network.getNodes().get(Id.createNodeId(nodeNr));
				if (node.getOutLinks().size() > 0) {
					return node.getOutLinks().values().iterator().next();
				}
			}
		}
		return null;
	}



	public static Population cutPopulation(double popCutPercentage) {
		Config configZh1pm = ConfigUtils.createConfig();
		configZh1pm.getModules().get("plans").addParam("inputPlansFile", "zurich_1pm/zurich_population.xml.gz");		
		Population pop = ScenarioUtils.loadScenario(configZh1pm).getPopulation();
		Population popCut = ScenarioUtils.loadScenario(ConfigUtils.createConfig()).getPopulation();
		int popSize = 0;
		for (Person person : pop.getPersons().values()) {
			// from 10 agents pick pmSize random agents to be featured in cut population: 
			if ((new Random()).nextDouble() < popCutPercentage) {
				popCut.addPerson(person);
				popSize++;
			}
			if (popSize > popCutPercentage*pop.getPersons().size()) {
				break;
			}
		}
		PopulationWriter pw = new PopulationWriter(popCut);
//		int popFullNr = 
		pw.write("zurich_1pm/VC_files/zurich_population_0."+((int) (10*popCutPercentage))+"pm.xml.gz");
		System.out.println("PopSize = "+popSize);
		System.out.println("PopSizeMax = "+popCutPercentage*pop.getPersons().size());
		return popCut;
	}

	

	
// %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%      PT      %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

	// %%% Create Routes %%%
	public static ArrayList<NetworkRoute> createInitialRoutes(Network network, List<List<Node>> keyRouteNodeSequences) throws IOException {

		ArrayList<NetworkRoute> networkRouteArray = new ArrayList<NetworkRoute>();

		OuterLoop:
		for (List<Node> keyRouteNodeSequence : keyRouteNodeSequences) {
			Node thisKeyNode = keyRouteNodeSequence.get(0);
			List<Node> nodeList = new ArrayList<Node>();
			nodeList.add(thisKeyNode);
			for (Node lastKeyNode : keyRouteNodeSequence.subList(1, keyRouteNodeSequence.size())) {
				List<Node> thisShortestLeg = DemoDijkstra.calculateShortestPath(network, thisKeyNode.getId(), lastKeyNode.getId());
				if (thisShortestLeg == null) {
					continue;
				}
//				for (Node node : thisShortestLeg.subList(1, thisShortestLeg.size())) {
//					nodeList.add(node);
//				}
				nodeList.addAll(thisShortestLeg.subList(1, thisShortestLeg.size()));
				thisKeyNode = lastKeyNode;
				// from 2nd entry bc first one is already featured as last node of previous leg
			}
			if (nodeList.size() < 2) {
				Log.write("Oops, no shortest path available. Check placemnet of key nodes! Continuing with next transit route.");
				continue OuterLoop;
			}
			List<Id<Link>> linkList = NetworkEvolutionImpl.nodeListToNetworkLinkList(network, nodeList);
			linkList.addAll(NetworkEvolutionImpl.OppositeLinkListOf(linkList)); // extend linkList with its opposite direction for PT transportation!
			NetworkRoute networkRoute = RouteUtils.createNetworkRoute(linkList, network);
			System.out.println("The new networkRoute is: [Length="+(networkRoute.getLinkIds().size()+2)+"] - " + NetworkEvolutionImpl.networkRouteToLinkIdList(networkRoute).toString());
			networkRouteArray.add(networkRoute);
		}

		// Store all new networkRoutes in a separate network file for visualization
		NetworkOperators.networkRoutesToNetwork(networkRouteArray, network, Sets.newHashSet("pt"),
				"zurich_1pm/VC_files/busRoutes.xml");
		return networkRouteArray;
	}
	
	// %%% Add actual Transit: Create stops & make schedule along lines %%%
	public static void applyPtSchedule(List<NetworkRoute> busRoutes, Network network, double minStopDistance) throws IOException {
		
		Config busConfig = ConfigUtils.createConfig();						// this is totally default and may be modified as required
		Scenario busScenario = ScenarioUtils.createScenario(busConfig);
		TransitSchedule busSchedule = busScenario.getTransitSchedule();
		TransitScheduleFactory busScheduleFactory = busSchedule.getFactory();
			
		// Create a New Metro Vehicle
		VehicleType metroVehicleType = Metro_TransitScheduleImpl.createNewVehicleType("myBus", 30.0, 50.0/3.6, 40, 80);
		busScenario.getTransitVehicles().addVehicleType(metroVehicleType);
			
		// Generate TransitLines and Schedules on NetworkRoutes --> Add to Transit Schedule
		int nTransitLines = busRoutes.size();
		for(int lineNr=1; lineNr<=nTransitLines; lineNr++) {
			
			// NetworkRoute
			NetworkRoute busRoute = busRoutes.get(lineNr-1);
			MRoute mRoute = new MRoute("myBus_Route"+lineNr);
			mRoute.lifeTime = 40;
			mRoute.departureSpacing = 300.0;
			mRoute.isInitialDepartureSpacing = false;
			mRoute.firstDeparture = 6.0*3600;
			mRoute.lastDeparture = 22.0*3600;
			mRoute.setNetworkRoute(busRoute);
			//	mNetwork.addNetworkRoute(mRoute);
			
			// Create an array of stops along new networkRoute on the FromNode of each of its individual links (and ToNode for final terminal)
			// The new network was constructed so that every node had a corresponding stop facility from the original zurich network on it.
			
			List<TransitRouteStop> stopArray = new ArrayList<TransitRouteStop>();				// prepare an array for stop facilities on new networkRoute
			List<TransitStopFacility> newlyAddedTSF = new ArrayList<TransitStopFacility>();				// prepare an array for stop facilities on new networkRoute
			
			int stopCount = 0;
			double stopTime = 30.0;
			double accumulatedDrivingTime = 0;
			double lastStopDistance = 0.0;
			
			List<Id<Link>> routeLinkList = new ArrayList<Id<Link>>();
			routeLinkList.addAll(Metro_NetworkImpl.networkRouteToLinkIdList(mRoute.networkRoute));
			double maxVehicleSpeed = 30.0/3.6;
			double acceleration = 0.1*9.81;
			double vMaxAccDistance = maxVehicleSpeed*maxVehicleSpeed/(2*acceleration);
			double tAccVMax = maxVehicleSpeed/acceleration;
			
			Id<Link> lastStopLinkId = routeLinkList.get(0); // Have secured by terminal choice that first link definitely has a stopFacility
			Link lastStopLink = network.getLinks().get(lastStopLinkId);
			TransitStopFacility foundLastTransitStopFacility = busSchedule.getFacilities().get(Id.create(lastStopLink.getFromNode().getId().toString(), TransitStopFacility.class));
			TransitStopFacility lastStopFacility;
			if (foundLastTransitStopFacility == null) {
				lastStopFacility = busScheduleFactory.createTransitStopFacility(
						Id.create(lastStopLink.getFromNode().getId().toString(), TransitStopFacility.class), lastStopLink.getFromNode().getCoord(), false);
				lastStopFacility.setLinkId(lastStopLinkId);
				lastStopFacility.setName("stopNode"+lastStopLink.getFromNode().getId().toString());
				busSchedule.addStopFacility(lastStopFacility);
				newlyAddedTSF.add(lastStopFacility);
			}
			else {
				lastStopFacility = foundLastTransitStopFacility;
			}
			double distanceSinceLastStop = 0.0;
			for (Id<Link> currentLinkID : routeLinkList.subList(1, routeLinkList.size())) {
				Link currentLink = network.getLinks().get(currentLinkID);
				distanceSinceLastStop += lastStopLink.getLength();
				if (distanceSinceLastStop < minStopDistance) {
					continue;
				}
				else {
					distanceSinceLastStop = 0.0;	// if minStopDist has been surpassed, the stop can be built and the distSinceLastStop is reset
				}
				
				TransitStopFacility currentStopFacility;
				Id<TransitStopFacility> transitStopFacilityId = Id.create(currentLink.getFromNode().getId().toString(), TransitStopFacility.class);
				TransitStopFacility foundTransitStopFacility = busSchedule.getFacilities().get(transitStopFacilityId);
				if (foundTransitStopFacility == null) {
					currentStopFacility = busScheduleFactory.createTransitStopFacility(
							transitStopFacilityId, currentLink.getFromNode().getCoord(), false);
					currentStopFacility.setLinkId(currentLinkID);
					currentStopFacility.setName("stopNode"+currentLink.getFromNode().getId().toString());
					busSchedule.addStopFacility(currentStopFacility);
					newlyAddedTSF.add(currentStopFacility);
				}
				else {
					currentStopFacility = foundTransitStopFacility;
				}
				lastStopDistance = Metro_TransitScheduleImpl.calculateDistanceBetweenStops(
						routeLinkList, lastStopLinkId, currentLinkID, lastStopFacility, currentStopFacility, network);
				if (lastStopDistance >= vMaxAccDistance) {
					accumulatedDrivingTime += (2*tAccVMax + (lastStopDistance-vMaxAccDistance)/(maxVehicleSpeed));	// 2*AccTime for accelerating and braking and then the cruise time in between
				}
				else {
					accumulatedDrivingTime += 2*Math.sqrt(2*lastStopDistance/acceleration); // 2*xxx for accelerating and then symmetric braking with const acceleration
				}
				double arrivalDelay = (stopCount-1)*stopTime + accumulatedDrivingTime;
				double departureDelay = (stopCount)*stopTime + accumulatedDrivingTime;		// same as arrivalDelay + 1*stopTime
				lastStopFacility = currentStopFacility;
				lastStopLinkId = currentLinkID;
				lastStopLink = currentLink;
				TransitRouteStop transitRouteStop = busScheduleFactory.createTransitRouteStop(currentStopFacility, arrivalDelay, departureDelay);

				stopArray.add(transitRouteStop);
			}
			
			if (stopArray.get(0).getStopFacility().getId().equals(stopArray.get(stopArray.size()-1).getStopFacility().getId()) == false) {
				double terminalArrivalOffset = stopArray.get(stopArray.size()-1).getDepartureOffset()+stopArray.get(1).getArrivalOffset();
				double terminalDepartureOffset = terminalArrivalOffset+stopTime;
				TransitRouteStop terminalTransitRouteStop = busScheduleFactory.createTransitRouteStop(stopArray.get(0).getStopFacility(),
						terminalArrivalOffset, terminalDepartureOffset);
				stopArray.add(terminalTransitRouteStop);
			}
	
			if (stopArray.size() < 3) {
				Log.write("CAUTION: too small stopArray = "+stopArray.toString() + " --> Returning NULL and will be removing mRoute from network.");
				// IMPORTANT: remove again all newly added transitStopFacilities in order to avoid SwissRaptor routing issues
				for (TransitStopFacility tsfToRemoveAgain : newlyAddedTSF) {
					busSchedule.removeStopFacility(tsfToRemoveAgain);
				}
				continue;
			}
			
			mRoute.roundtripTravelTime = stopArray.get(stopArray.size()-1).getArrivalOffset();
			mRoute.vehiclesNr = (int) Math.ceil(mRoute.roundtripTravelTime/mRoute.departureSpacing);		// set vehicles initially so they are not zero for evo loops
			
			// Build TransitRoute from stops and NetworkRoute --> and add departures
			String vehicleFileLocation = ("zurich_1pm/VC_files/zurich_transit_vehicles.xml.gz");
			TransitRoute transitRoute = busScheduleFactory.createTransitRoute(Id.create("VC_Route"+lineNr, TransitRoute.class ), 
					busRoute, stopArray, "bus");
			transitRoute = Metro_TransitScheduleImpl.addDeparturesAndVehiclesToTransitRoute(mRoute, busScenario, busSchedule, 
					transitRoute, metroVehicleType, vehicleFileLocation); // Add departures to TransitRoute as a function of f=(DepSpacing, First/LastDeparture)
			// Build TransitLine from TrasitRoute
			TransitLine transitLine = busScheduleFactory.createTransitLine(Id.create("VC_TransitLine_Nr"+lineNr, TransitLine.class));
			transitLine.addRoute(transitRoute);
			// Add new line to schedule
			busSchedule.addTransitLine(transitLine);
			mRoute.setTransitLine(transitLine);
			mRoute.setLinkList(NetworkEvolutionImpl.NetworkRoute2LinkIdList(busRoute));
			mRoute.setNodeList(NetworkEvolutionImpl.NetworkRoute2NodeIdList(busRoute, network));
			mRoute.setRouteLength(network);
			mRoute.setTotalDrivenDist(mRoute.routeLength * mRoute.nDepartures);		
		}	// end of TransitLine creator loop

		// Write TransitSchedule to corresponding file
		TransitScheduleWriter tsw = new TransitScheduleWriter(busSchedule);
		tsw.writeFile("zurich_1pm/VC_files/zurich_transit_schedule.xml.gz");	
	}
	
// %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%      NETWORK      %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
	
	public static int xy2NodeNr(int x, int y, int xmax) {
		return y*(xmax+1) + x;
	}

	public static List<Integer> nodeNr2xy(int nr, int xmax) {
		int x = nr % (xmax+1);
		int y = Math.floorDiv(nr, xmax+1);
		return Arrays.asList(x, y);
	}
	
	public static Network fillNetwork(int xmax, int ymax, Network network, double unitLinkLength) {
		fillNodes(xmax, ymax, network, unitLinkLength);
		fillNeighboringLinks(xmax, ymax, network, unitLinkLength);
		return network;
	}

	public static Network fillNodes(int xmax, int ymax, Network network, double unitLinkLength)	{
		NetworkFactory NF = network.getFactory();
		for (int y=0; y<=ymax; y++) {
			for (int x=0; x<=xmax; x++) {
				Coord coord = new Coord(x*unitLinkLength, y*unitLinkLength);
				int nodeNr = xy2NodeNr(x,y,xmax);
				Node newNode = NF.createNode(Id.createNodeId(nodeNr), coord);				// naming of nodes .. fill in from left to right and bottom to top
				network.addNode(newNode);														// add all nodes to network
				// System.out.println("Added node "+newNode.getId().toString()+" to network.");
			}
		}
		return network;
	}
	
	public static Network fillNeighboringLinks(int xmax, int ymax, Network network, double unitLinkLength) {				// connects all nodes of perfect network with links and returns new network with all links
		 																								// only in perfect network
		 for (int y=0; y<=ymax; y++) {
				for (int x=0; x<=xmax; x++) {
					
					Node centerNode = network.getNodes().get(Id.createNodeId(xy2NodeNr(x,y,xmax)));
					// right
					if (x+1<=xmax) {
						Node rightNode = network.getNodes().get(Id.createNodeId(xy2NodeNr(x+1,y,xmax)));
						Link rightLink = network.getFactory().createLink(Id.createLinkId(centerNode.getId()+"_"+rightNode.getId()), centerNode, rightNode);
						if ( ! network.getLinks().containsKey(rightLink.getId())) {
							 rightLink.setAllowedModes(Sets.newHashSet("pt", "car"));
							 network.addLink(rightLink);
							 // not necessary to add reverse link as this is done inherently in the course of the grid roaming in x and y
						}
					}
					// left
					if (x-1>=0) {
						Node leftNode = network.getNodes().get(Id.createNodeId(xy2NodeNr(x-1,y,xmax)));
						Link leftLink = network.getFactory().createLink(Id.createLinkId(centerNode.getId()+"_"+leftNode.getId()), centerNode, leftNode);
						if ( ! network.getLinks().containsKey(leftLink.getId())) {
							 leftLink.setAllowedModes(Sets.newHashSet("pt", "car"));
							 network.addLink(leftLink);
							 // not necessary to add reverse link as this is done inherently in the course of the grid roaming in x and y
						}
					}
					// top
					if (y+1<=ymax) {
						Node topNode = network.getNodes().get(Id.createNodeId(xy2NodeNr(x,y+1,xmax)));
						Link topLink = network.getFactory().createLink(Id.createLinkId(centerNode.getId()+"_"+topNode.getId()), centerNode, topNode);
						if ( ! network.getLinks().containsKey(topLink.getId())) {
							topLink.setAllowedModes(Sets.newHashSet("pt", "car"));
							 network.addLink(topLink);
							 // not necessary to add reverse link as this is done inherently in the course of the grid roaming in x and y
						}
					}
					// bottom
					if (y-1>=0) {
						Node bottomNode = network.getNodes().get(Id.createNodeId(xy2NodeNr(x,y-1,xmax)));
						Link bottomLink = network.getFactory().createLink(Id.createLinkId(centerNode.getId()+"_"+bottomNode.getId()), centerNode, bottomNode);
						if ( ! network.getLinks().containsKey(bottomLink.getId())) {
							bottomLink.setAllowedModes(Sets.newHashSet("pt", "car"));
							 network.addLink(bottomLink);
							 // not necessary to add reverse link as this is done inherently in the course of the grid roaming in x and y
						}
					}
					
				}
		 }
		 return network;
	}
	
	public static Network removeRectangluarSection(List<RectX> rectanglesToBeRemoved, int nrRectanglesToBeRemoved, int maxRemovalSizeFraction,
			int xmax, int ymax, Network network, double unitLinkLength) {

		Network positiveNetwork = ScenarioUtils.loadScenario(ConfigUtils.createConfig()).getNetwork();
		
		List<Id<Node>> nodesToDelete = new ArrayList<Id<Node>>();
		List<Id<Link>> linksToDelete = new ArrayList<Id<Link>>();
		while(rectanglesToBeRemoved.size() < nrRectanglesToBeRemoved) {
			int xSize = (new Random()).nextInt(Math.floorDiv(xmax, maxRemovalSizeFraction))+1;
			int ySize = (new Random()).nextInt(Math.floorDiv(ymax, maxRemovalSizeFraction))+1;
			int x0R = (new Random()).nextInt(xmax-xSize);
			int x1R = x0R + xSize;
			int y0R = (new Random()).nextInt(ymax-ySize);
			int y1R = y0R + ySize;
//			RectX rect = new RectX(x0R, x1R, y0R, y1R);
			rectanglesToBeRemoved.add(new RectX(x0R, x1R, y0R, y1R));
		}
		List<List<Integer>> potentialNewDiagonals = new ArrayList<List<Integer>>();
		for (RectX rectangleToBeRemoved : rectanglesToBeRemoved) {
			int x0 = rectangleToBeRemoved.x0;
			int x1 = rectangleToBeRemoved.x1;
			int y0 = rectangleToBeRemoved.y0;
			int y1 = rectangleToBeRemoved.y1;
			// each a third chance if one or other or both diagonals from rectangle
			if ((new Random()).nextDouble() < 0.33) {
				potentialNewDiagonals.add(Arrays.asList(xy2NodeNr(x0,y0,xmax), xy2NodeNr(x1,y1,xmax)));
			}
			else if ((new Random()).nextDouble() < 0.67) {
				potentialNewDiagonals.add(Arrays.asList(xy2NodeNr(x0,y1,xmax), xy2NodeNr(x1,y0,xmax)));
			}
			else {
				potentialNewDiagonals.add(Arrays.asList(xy2NodeNr(x0,y0,xmax), xy2NodeNr(x1,y1,xmax)));
				potentialNewDiagonals.add(Arrays.asList(xy2NodeNr(x0,y1,xmax), xy2NodeNr(x1,y0,xmax)));
			}
			if (x1-x0>1 && y1-y0>1) {
				for (int y=y0+1; y<=y1-1; y++) {
					for (int x=x0+1; x<=x1-1; x++) {
						nodesToDelete.add(Id.createNodeId(xy2NodeNr(x,y,xmax)));
					}
				}
			}
			for (int y=y0; y<=y1; y++) {
				for (int x=x0; x<=x1; x++) {
					Node centerNode = network.getNodes().get(Id.createNodeId(xy2NodeNr(x,y,xmax)));
					if (x+1<=x1 && y!=y0 && y!=y1) {
						Node rightNode = network.getNodes().get(Id.createNodeId(xy2NodeNr(x+1,y,xmax)));
						linksToDelete.add(Id.createLinkId(centerNode.getId()+"_"+rightNode.getId()));
					}
					if (x-1>=x0 && y!=y0 && y!=y1) {
						Node leftNode = network.getNodes().get(Id.createNodeId(xy2NodeNr(x-1,y,xmax)));
						linksToDelete.add(Id.createLinkId(centerNode.getId()+"_"+leftNode.getId()));
					}
					if (y+1<=y1 && x!=x0 && x!=x1) {
						Node topNode = network.getNodes().get(Id.createNodeId(xy2NodeNr(x,y+1,xmax)));
						linksToDelete.add(Id.createLinkId(centerNode.getId()+"_"+topNode.getId()));
					}
					if (y-1>=y0 && x!=x0 && x!=x1) {
						Node bottomNode = network.getNodes().get(Id.createNodeId(xy2NodeNr(x,y-1,xmax)));
						linksToDelete.add(Id.createLinkId(centerNode.getId()+"_"+bottomNode.getId()));
					}
				}			
			}
		}
		// build a copy of the original network and do not add links/nodes if they are featured in the negative network
		for (Node node : network.getNodes().values()) {
			if ( ! nodesToDelete.contains(node.getId())) {
				Node copyNode = network.getFactory().createNode(node.getId(), node.getCoord());
				positiveNetwork.addNode(copyNode);
			}
		}
		for (Link link : network.getLinks().values()) {
			if ( ! linksToDelete.contains(link.getId())) {
				Link copyLink = network.getFactory().createLink(link.getId(), link.getFromNode(), link.getToNode());
				copyLink.setAllowedModes(link.getAllowedModes());
				copyLink.setFreespeed(link.getFreespeed());
				copyLink.setCapacity(link.getCapacity());
				positiveNetwork.addLink(copyLink);
			}
		}
		// build some of the potential diagonals
		for (List<Integer> diagonalPair : potentialNewDiagonals) {
			if (new Random().nextDouble() < 0.33) {
				continue;
			}
			addDiagonalToNetwork(diagonalPair, positiveNetwork);
		}
		return positiveNetwork;
	}



	public static Boolean addDiagonalToNetwork(List<Integer> diagonalPair, Network network) {
		if (diagonalPair.get(0) == diagonalPair.get(1)) {
			return false;
		}
		if (network.getNodes().containsKey(Id.createNodeId(diagonalPair.get(0)))
				&& network.getNodes().containsKey(Id.createNodeId(diagonalPair.get(1)))) {
			Id<Link> diaLinkIdThere = Id.createLinkId(diagonalPair.get(0)+"_"+diagonalPair.get(1));
			Id<Link> diaLinkIdBack = Id.createLinkId(diagonalPair.get(1)+"_"+diagonalPair.get(0));
			if ( ! network.getLinks().containsKey(diaLinkIdThere) && ! network.getLinks().containsKey(diaLinkIdBack)) {
				Link dialLinkThere = network.getFactory().createLink(diaLinkIdThere,
						network.getNodes().get(Id.createNodeId(diagonalPair.get(0))),
						network.getNodes().get(Id.createNodeId(diagonalPair.get(1))));
				Link dialLinkBack = network.getFactory().createLink(diaLinkIdBack,
						network.getNodes().get(Id.createNodeId(diagonalPair.get(1))),
						network.getNodes().get(Id.createNodeId(diagonalPair.get(0))));
				Link randomLinkForDefaultAttributes = network.getLinks().values().iterator().next();
				dialLinkThere.setAllowedModes(randomLinkForDefaultAttributes.getAllowedModes());
				dialLinkThere.setFreespeed(randomLinkForDefaultAttributes.getFreespeed());
				dialLinkThere.setCapacity(randomLinkForDefaultAttributes.getCapacity());
				network.addLink(dialLinkThere);
				dialLinkBack.setAllowedModes(randomLinkForDefaultAttributes.getAllowedModes());
				dialLinkBack.setFreespeed(randomLinkForDefaultAttributes.getFreespeed());
				dialLinkBack.setCapacity(randomLinkForDefaultAttributes.getCapacity());
				network.addLink(dialLinkBack);
				return true;
			}
			else {
				return false;
			}
		}
		else {
			return false;
		}
		
	}
	
	public static Network buildDiagonals(Network network, Integer nDiagonalsToBeBuilt, Integer xmax, Integer ymax) {
		List<List<Integer>> diagonalsToBeBuilt = new ArrayList<List<Integer>>();
		while (diagonalsToBeBuilt.size() < nDiagonalsToBeBuilt) {
			int span = new Random().nextInt(Math.floorDiv(xmax, 5))+1;
			if (new Random().nextDouble() < 0.5) {	// bottom left to top right
				int x0 = new Random().nextInt(xmax-span);
				int y0 = new Random().nextInt(ymax-span);
				int x1 = x0 + span;
				int y1 = y0 + span;
				diagonalsToBeBuilt.add(Arrays.asList(xy2NodeNr(x0,y0,xmax), xy2NodeNr(x1,y1,xmax)));
			}
			else {	// top left to bottom right
				int x0 = new Random().nextInt(xmax-span);
				int y0 = new Random().nextInt(ymax-span)+span;
				int x1 = x0 + span;
				int y1 = y0 - span;
				diagonalsToBeBuilt.add(Arrays.asList(xy2NodeNr(x0,y0,xmax), xy2NodeNr(x1,y1,xmax)));
			}
			
		}
		for (List<Integer> diagonalPair : diagonalsToBeBuilt) {
			addDiagonalToNetwork(diagonalPair, network);		
		}
		
		return network;
	}

	
	
	
	
	
	

//	public static Network addGeometricLinkLengths(Network network, double unitLinkLength) {
//		for (Id<Link> linkID : network.getLinks().keySet()) {
//			double linkLength = unitLinkLength*GeomDistance.calculate(network.getLinks().get(linkID).getFromNode().getCoord(), network.getLinks().get(linkID).getToNode().getCoord());
//			network.getLinks().get(linkID).setLength(linkLength);
//			// System.out.println("Length of link "+linkID.toString()+" is "+linkLength+".");
//		}
//		return network;
//	}
//
//
//	public static Network thin(Network network, int XMax, int YMax, int removalPercentage, boolean saveAsFile, String fileName) {
//		Network nwThin = network;
//		int networkNodesNumber = nwThin.getNodes().size();
//		int nodeAmountRemove = (int) (networkNodesNumber*removalPercentage/100);
//		System.out.println("Removing a total of "+nodeAmountRemove+" nodes");
//		for (int n=1; n<=nodeAmountRemove; n++) {
//			
//			Id<Node> nodeToRemoveID = null;
//			do  {
//				Random r = new Random();
//				int nodeNrToRemove = r.nextInt(networkNodesNumber)+1;
//				nodeToRemoveID = Id.createNodeId(nodeNrToRemove);
//			} while (nwThin.getNodes().containsKey(nodeToRemoveID) == false);
//
//			// System.out.println("Chose to thin around node "+nodeToRemoveID.toString());
//			for (Id<Link> inLink : nwThin.getNodes().get(nodeToRemoveID).getInLinks().keySet()) {
//				nwThin.removeLink(inLink);
//				// System.out.println("Removing inLink "+inLink.toString());
//			}
//			for (Id<Link> outLink : nwThin.getNodes().get(nodeToRemoveID).getOutLinks().keySet()) {
//				nwThin.removeLink(outLink);
//				// System.out.println("Removing outLink "+outLink.toString());
//			}
//			nwThin.removeNode(nodeToRemoveID);
//			// System.out.println("Removed node "+nodeToRemoveID.toString()+" and its links");
//
//		}
//		if (saveAsFile) {
//			NetworkWriter nwT = new NetworkWriter(nwThin);
//			nwT.write(fileName);
//			System.out.println("Saved new (thinned) network as "+fileName);
//		}
//		
//		// Display all (remaining) nodes of current desired network
//			/* n = 1;
//			 * for (Id<Node> nID : nwThin.getNodes().keySet()) {
//			 * System.out.println("Remaining node "+n+" is: "+nID.toString());
//			 * n++;}
//			 */
//		
//		return nwThin;
//	}
//
//	public static void writeToFile(int XMax, int YMax, Network network, String fileName) {
//		NetworkWriter nw = new NetworkWriter(network);
//		nw.write(fileName);
//	}
//
//
//	public static boolean exist(boolean containsKey) {
//		if (containsKey == true) {
//			return true;
//		}
//		else {
//			return false;
//		}
//	}
//
//	public static int xyCoordToNr(int x, int y, int xmax) {
//		return (int) ((y-1)*xmax + x);
//	}
//		
//	public static Coord nrToCoord(int nr, int xmax, int ymax) {
//		double x = nr % ymax;
//		double y = (nr-x) / xmax;
//		Coord xyCoord = new Coord(x,y);
//		return xyCoord;
//	}
//
//	public static NetworkRoute createNetworkRoute(Network networkThin, int XMax, int YMax, int outerFramePercentage, int minSpacingPercentage) {
//		
//		ArrayList<Node> routeNodeList = new ArrayList<Node>();
//		do{
//			routeNodeList = createRandomRoute(networkThin, XMax, YMax, outerFramePercentage, minSpacingPercentage); 		// makes random starting points in network in outer network regions
//		} while(routeNodeList==null);
//		
//		NetworkRoute networkRoute = NodeListToNetworkRoute(networkThin, routeNodeList);			// convert from node list format to network route by connecting the corresponding links
//		return networkRoute;	
//	}
//
//	
//	public static Network createAndWriteNetworkRouteToNetwork(Config config, Network network, NetworkRoute networkRoute, Set<String> transportModes, int lineNr, int XMax, int YMax, int removalPercentage) {
//		Network shortestPathNetwork = ScenarioUtils.createScenario(config).getNetwork();
//		NetworkFactory shortestPathNetworkFactory = shortestPathNetwork.getFactory();
//		// Link tempLink = null;
//		// Node tempToNode = null;
//		// Node tempFromNode = null;
//		for (Id<Link> linkID : VC_PublicTransportImpl.networkRouteToLinkIdList(networkRoute)) {
//			Node tempToNode = shortestPathNetworkFactory.createNode(network.getLinks().get(linkID).getToNode().getId(), network.getLinks().get(linkID).getToNode().getCoord());
//			Node tempFromNode = shortestPathNetworkFactory.createNode(network.getLinks().get(linkID).getFromNode().getId(), network.getLinks().get(linkID).getFromNode().getCoord());
//			Link tempLink = shortestPathNetworkFactory.createLink(network.getLinks().get(linkID).getId(), tempFromNode, tempToNode);
//			tempLink.setAllowedModes(transportModes);
//			if (shortestPathNetwork.getNodes().containsKey(tempToNode.getId())==false) {
//				shortestPathNetwork.addNode(tempToNode);
//			}
//			if (shortestPathNetwork.getNodes().containsKey(tempFromNode.getId())==false) {
//				shortestPathNetwork.addNode(tempFromNode);
//			}
//			if (shortestPathNetwork.getLinks().containsKey(tempLink.getId())==false) {
//				shortestPathNetwork.addLink(tempLink);
//			}
//		}
//		
//		NetworkWriter nwShortestPath = new NetworkWriter(shortestPathNetwork);
//		String filepathShortestPath = "zurich_1pm/VirtualCity/Input/Generated_Networks/ShortestPath_"+XMax+"x"+YMax+"_"+removalPercentage+"PercentLean_TransitLineNr"+lineNr+".xml";
//		nwShortestPath.write(filepathShortestPath);
//		
//		return shortestPathNetwork;
//	}
//
//	public static NetworkRoute NodeListToNetworkRoute(Network network, ArrayList<Node> nodeList) {
//		// ArrayList<Link> linkListArray = new ArrayList<Link>(nodeList.size());
//		List<Id<Link>> linkList = new ArrayList<Id<Link>>(nodeList.size()-1);
//		for (int n=0; n<(nodeList.size()-1); n++) {
//			// System.out.println("n= "+n);
//			// System.out.println("nodeListSize= "+nodeList.size());
//			for (Link l : nodeList.get(n).getOutLinks().values()) {
//				if (l.getToNode() == nodeList.get(n+1)) {
//					linkList.add(l.getId());
//					System.out.println("Adding link "+l.getId().toString());
//				}
//			}
//		}
//		
//		NetworkRoute networkRoute = RouteUtils.createNetworkRoute(linkList, network);
//		return networkRoute;
//	}
//	
//
//	public static ArrayList<Node> createRandomRoute(Network network, int XMax, int YMax, int outerFramePercentage, int minSpacingPercentage) {
//		Node startNode = createStartOrEndNode(network, XMax, YMax, outerFramePercentage);
//		System.out.println("Start Node: "+startNode.getId().toString());
//		Node endNode = startNode; // just for initializing
//		do {
//			endNode = createStartOrEndNode(network, XMax, YMax, outerFramePercentage);
//			// System.out.println("Distance between node is: "+GeomDistance.calculate(startNode.getCoord(), endNode.getCoord()));
//		} while(endNode.equals(startNode) || GeomDistance.calculate(startNode.getCoord(), endNode.getCoord())<=Math.sqrt(1.0*XMax*XMax+YMax*YMax)*outerFramePercentage/100);
//		System.out.println("End Node: "+endNode.getId().toString());
//		return createRouteBetweenNodes(network, startNode, endNode);
//	}	
//	
//	public static ArrayList<Node> createRouteBetweenNodes(Network network, Node startNode, Node endNode) {
//		// System.out.println("started route generator");
//		ArrayList<Node> dijkstraNodePath = DijkstraOwn_I.findShortestPath(network, startNode, endNode);
//		if(dijkstraNodePath == null) {
//			System.out.println("Error: No path found between start and end node! Trying a new pair of start/end nodes ... ");
//			return null;
//		}
//		// System.out.println("To my surprise it worked ...");
//		return dijkstraNodePath;
//	}
//	
//	public static List<TransitRouteStop> createAndAddNetworkRouteStops(TransitSchedule transitSchedule, Network network, NetworkRoute networkRoute, String defaultPtMode, double stopTime, double maxVehicleSpeed, boolean blocksLane){
//		TransitScheduleFactory transitScheduleFactory = transitSchedule.getFactory();
//		
//		List<TransitRouteStop> stopArray = new ArrayList<TransitRouteStop>();				// prepare an array for stop facilities on new networkRoute
//		
//		int stopCount = 0;
//		double accumulatedDrivingTime = 0;
//		Link lastLink = null;
//		
//		List<Id<Link>> routeLinkList = new ArrayList<Id<Link>>();
//		routeLinkList.addAll(Metro_NetworkImpl.networkRouteToLinkIdList(networkRoute));
//		routeLinkList.addAll(OppositeLinkListOf(Metro_NetworkImpl.networkRouteToLinkIdList(networkRoute), network));	// TODO: Reverse Routes for this network
//		for (Id<Link> linkID : routeLinkList) {
//			// place the stop facilities always on the FromNode of the RefLink; this way, the new facilities will have the same coords as the original network's facilities!
//			Link currentLink = network.getLinks().get(linkID);
//			TransitStopFacility transitStopFacility = transitScheduleFactory.createTransitStopFacility(Id.create("MetroStopRefLink_"+linkID.toString(), TransitStopFacility.class), currentLink.getFromNode().getCoord(), blocksLane);
//			transitStopFacility.setName("MetroStopRefLink_"+linkID.toString());
//			transitStopFacility.setLinkId(linkID);
//			stopCount++;
//			if(stopCount>1) {
//				accumulatedDrivingTime += lastLink.getLength()/(maxVehicleSpeed);
//			}
//			double arrivalDelay = (stopCount-1)*stopTime + accumulatedDrivingTime;
//			double departureDelay = (stopCount)*stopTime + accumulatedDrivingTime;		// same as arrivalDelay + 1*stopTime
//			TransitRouteStop transitRouteStop = transitScheduleFactory.createTransitRouteStop(transitStopFacility, arrivalDelay, departureDelay);
//			if (transitSchedule.getFacilities().containsKey(transitStopFacility.getId())==false) {
//				transitSchedule.addStopFacility(transitStopFacility);
//			}
//			stopArray.add(transitRouteStop);
//			lastLink = currentLink;
//		}
//		// do this to add last terminal link on way back, because the stops are always added at the fromNode location and the last link needs a stop at the final toNode!
//		Id<Link> terminalLink = stopArray.get(stopArray.size()-1).getStopFacility().getLinkId();
//		TransitStopFacility terminalTransitStopFacility = transitScheduleFactory.createTransitStopFacility(Id.create("MetroStopRefLink_"+terminalLink.toString()+"_TerminalStop", TransitStopFacility.class), 
//				network.getLinks().get(terminalLink).getToNode().getCoord(), blocksLane);
//		terminalTransitStopFacility.setName("MetroStopRefLink_"+terminalLink.toString()+"_TerminalStop");
//		terminalTransitStopFacility.setLinkId(terminalLink);
//		double terminalArrivalOffset = stopArray.get(stopArray.size()-1).getDepartureOffset()+stopArray.get(1).getArrivalOffset();
//		double terminalDepartureOffset = terminalArrivalOffset+stopTime;
//		TransitRouteStop terminalTransitRouteStop = transitScheduleFactory.createTransitRouteStop(
//				stopArray.get(0).getStopFacility(), terminalArrivalOffset, terminalDepartureOffset);
//		stopArray.add(terminalTransitRouteStop);
//		/*for (int s=0; s<stopArray.size(); s++) {
//			System.out.println(stopArray.get(s).toString());
//		}*/
//		return stopArray;
//	}
//	
//	
//	public static List<Id<Link>> OppositeLinkListOf(List<Id<Link>> linkList, Network network){
//		List<Id<Link>> oppositeLinkList = new ArrayList<Id<Link>>(linkList.size());
//		for (int c=0; c<linkList.size(); c++) {
//			Id<Link> linkToBeReversed = linkList.get(linkList.size()-1-c);
//			oppositeLinkList.add(ReverseLink(linkToBeReversed, network));
//		}
//		return oppositeLinkList;
//	}
//	
//	public static Id<Link> ReverseLink(Id<Link> linkId, Network network){
//		Id<Link> reverseId = null;
//		Id<Node> fromNode = network.getLinks().get(linkId).getFromNode().getId();
//		Id<Node> toNode = network.getLinks().get(linkId).getToNode().getId();
//		for (Id<Link> thisLinkId : network.getLinks().keySet()) {
//			Link thisLink = network.getLinks().get(thisLinkId);
//			if (thisLink.getFromNode().getId() == toNode && thisLink.getToNode().getId() == fromNode) {
//				reverseId = thisLink.getId();
//				break;
//			}
//		}
//		return reverseId;
//	}
//	
//	
//	public static Node createStartOrEndNode(Network network, int XMax, int YMax, int outerFramePercentage) {
//		int xFrameWidth = (int) (XMax*outerFramePercentage/100) + 1 ;
//		int yFrameWidth = (int) (YMax*outerFramePercentage/100) + 1 ;
//		Id<Node> frameNodeID = null;
//		int rXint = 0;
//		int rYint = 0;
//		do {
//			boolean inFrameX = false;
//			while(inFrameX == false) {
//				Random rX = new Random();
//				rXint = rX.nextInt(XMax)+1;
//				if (rXint < xFrameWidth || XMax-xFrameWidth < rXint) {
//					inFrameX = true;
//				}
//			}
//			boolean inFrameY = false;
//			while(inFrameY == false) {
//				Random rY = new Random();
//				rYint = rY.nextInt(YMax)+1;
//				if (rYint < yFrameWidth || YMax-yFrameWidth < rYint) {
//					inFrameY = true;
//				}
//			}
//			int frameNodeNr = VC_NetworkImpl.xyCoordToNr(rXint, rYint, XMax);
//			frameNodeID = Id.createNodeId(frameNodeNr);
//		} while (network.getNodes().containsKey(frameNodeID)==false);
//		Node frameNode = network.getNodes().get(frameNodeID);
//		// System.out.println("Chosen node is "+frameNode.getId().toString());
//		return frameNode;
//	}
//	
//	public static void runEventsProcessing(MNetworkPop networkPopulation, int lastIteration) {
////		for (MNetwork mNetwork : networkPopulation.networkMap.values()) {
////			String networkName = mNetwork.networkID;
////			
////			// read and handle events
////			String eventsFile = "zurich_1pm/VirtualCity/Population/"+networkName+"/Simulation_Output/ITERS/it."+lastIteration+"/"+lastIteration+".events.xml.gz";			
////			MHandlerPassengers mPassengerHandler = new MHandlerPassengers(); // TODO Caution; Insert proper network & transitSchedule
////			EventsManager eventsManager = EventsUtils.createEventsManager();
////			eventsManager.addHandler(mPassengerHandler);
////			MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager);
////			eventsReader.readFile(eventsFile);
////			
////			// read out travel stats and display important indicators to console
////			Map<String, Map<String, Double>> travelStats = mPassengerHandler.travelStats;				// Map< PersonID, Map<RouteName,TravelDistance>>
////			Map<String, Integer> routeBoardingCounter = mPassengerHandler.routeBoardingCounter;			// Map<RouteName, nBoardingsOnThatRoute>
////			// double totalBeelineDistance = mPassengerHandler.totalBeelineKM;
////			Map<String, Double> personKMonRoutes = new HashMap<String, Double>();						// Map<RouteName, TotalPersonKM>
////			double totalMetroPersonKM = 0.0;
////			int nMetroUsers = travelStats.size(); 														// total number of persons who use the metro
////			//System.out.println("Number of Metro Users = " + nMetroUsers);
////			int nTotalBoardings = 0;
////			for (int i : routeBoardingCounter.values()) {
////				nTotalBoardings += i;
////			}
////			System.out.println("Total Metro Boardings = "+nTotalBoardings);
////			
////			for (Map<String, Double> routesStats : travelStats.values()) {
////				for (String route : routesStats.keySet()) {
////					if (personKMonRoutes.containsKey(route)) {
////						personKMonRoutes.put(route, personKMonRoutes.get(route)+routesStats.get(route));
////						//System.out.println("Putting on Route " +route+ " an additional " + routesStats.get(route) + " to a total of " + personKMonRoutes.get(route));  
////					}
////					else {
////						personKMonRoutes.put(route, routesStats.get(route));
////						//System.out.println("Putting on Route " +route+ " an initial " + personKMonRoutes.get(route)); 
////					}
////				}
////			}
////			
////			for (String route : personKMonRoutes.keySet()) {
////				totalMetroPersonKM += personKMonRoutes.get(route);
////			}
////			//System.out.println("Total Metro TransitKM = " + totalMetroPersonKM);
////
////			
////			// fill in performance indicators and scores in MRoutes
////			for (String routeId : mNetwork.routeMap.keySet()) {
////				if (personKMonRoutes.containsKey(routeId)) {					
////					MRoute mRoute = mNetwork.routeMap.get(routeId);
////					mRoute.personMetroDist = personKMonRoutes.get(routeId);
////					mRoute.nBoardings = routeBoardingCounter.get(routeId);
////					mNetwork.routeMap.put(routeId, mRoute);
////				}
////			}
////	
////			// fill in performance indicators and scores in MNetworks
////			// TODO [NOT PRIO] mNetwork.mPersonKMdirect = beelinedistances;
////			mNetwork.personMetroDist = totalMetroPersonKM;
////			mNetwork.nMetroUsers = nMetroUsers;
////		}		// END of NETWORK Loop
////
////		// - Maybe hand over score to a separate score map for sorting scores
//	}
//	
//	public static void peoplePlansProcessingM(MNetworkPop networkPopulation, int maxTravelTimeInMin) {
//		//System.out.println("Population name = "+networkPopulation.populationId);
//		//System.out.println("Population size = "+networkPopulation.networkMap.size());
//		for (MNetwork mNetwork : networkPopulation.networkMap.values()) {
//			// TEST			
//			String networkName = mNetwork.networkID;
//			//System.out.println("NetworkName = "+networkName);
//			String finalPlansFile = "zurich_1pm/VirtualCity/Population/"+networkName+"/Simulation_Output/output_plans.xml.gz";			
//			Config emptyConfig = ConfigUtils.createConfig();
//			emptyConfig.getModules().get("plans").addParam("inputPlansFile", finalPlansFile);
//			Scenario emptyScenario = ScenarioUtils.loadScenario(emptyConfig);
//			Population finalPlansPopulation = emptyScenario.getPopulation();
//			//PopulationReader p = new PopulationReader(emptyScenario);
//			Double[] travelTimeBins = new Double[maxTravelTimeInMin+1];
//			for (int d=0; d<travelTimeBins.length; d++) {
//				travelTimeBins[d] = 0.0;
//			}
//			for (Person person : finalPlansPopulation.getPersons().values()) {
//				//System.out.println("Person = "+person.getId().toString());
//				double personTravelTime = 0.0;
//				Plan plan = person.getSelectedPlan();
//				for (PlanElement element : plan.getPlanElements()) {
//						if (element instanceof Leg) {
//							/*System.out.println("Plan Elements is: "+element.toString());
//							System.out.println("Plan Elements Attributes are: "+element.getAttributes().toString());
//							System.out.println("Plan Elements Attribute travTime is: "+element.getAttributes().getAttribute("mode"));
//							System.out.println("Plan Elements Attribute travTime is: "+element.getAttributes().getAttribute("travTime"));*/
//							String findString = "[travTime=";
//							int i1 = element.toString().indexOf(findString);
//							//System.out.println("i1 is: "+i1);
//							String travTime = element.toString().substring(i1+findString.length(), i1+findString.length()+8);
//							//System.out.println("Plan Elements Attribute travTime is: "+travTime);
//
//							//System.out.println("Plan Elements Attribute travTime is: "+element.getAttributes().getAttribute("trav_time"));
//							//System.out.println(element.getAttributes().getAttribute("travTime").getClass().getName());
//							String[] HourMinSec = travTime.split(":");
//							//System.out.println("Person Travel Time of this leg in [s] = "+travTime);
//							personTravelTime += (Double.parseDouble(HourMinSec[0])*3600+Double.parseDouble(HourMinSec[1])*60+Double.parseDouble(HourMinSec[2]))/60;
//							//System.out.println("Total Person Travel Time of this leg in [m] = "+personTravelTime);
//						}
//				}
//				if (personTravelTime>=maxTravelTimeInMin) {
//					travelTimeBins[maxTravelTimeInMin]++;
//				}
//				else {
//					travelTimeBins[(int) Math.ceil(personTravelTime)]++;
//				}
//			}
//			double totalTravelTime = 0.0;
//			int travels = 0;
//			for (int i=0; i<travelTimeBins.length; i++) {
//				totalTravelTime += i*travelTimeBins[i];
//				travels += travelTimeBins[i];
//			}
//			mNetwork.totalTravelTime = totalTravelTime;
//			mNetwork.averageTravelTime = totalTravelTime/travels;
//			double standardDeviationInnerSum = 0.0;
//			for (int i=0; i<travelTimeBins.length; i++) {
//				for (int j=0; j<travelTimeBins[i]; j++) {
//					standardDeviationInnerSum += Math.pow(i-mNetwork.averageTravelTime, 2);
//				}
//			}
//			double standardDeviation = Math.sqrt(standardDeviationInnerSum/(travels-1));
//			
//			mNetwork.stdDeviationTravelTime = standardDeviation;
//			//System.out.println("standardDeviation = " + mNetwork.stdDeviationTravelTime);
//			//System.out.println("averageTravelTime = " + mNetwork.averageTravelTime);
//			
//		}
//		for (MNetwork network : networkPopulation.networkMap.values()) {
//			System.out.println(network.networkID+" AverageTavelTime [min] = "+network.averageTravelTime+"   (StandardDeviation="+network.stdDeviationTravelTime+")");
//			System.out.println(network.networkID+" TotalTravelTime [min] = "+network.totalTravelTime);
//		}
//	}
//	
//	@SuppressWarnings("unchecked")
//	public static void writeChartAverageTravelTimes(int lastGeneration, String fileName) throws FileNotFoundException { 	// Average and Best Scores
//		Map<Integer, Double> generationsAverageTravelTime = new HashMap<Integer, Double>();
//		Map<Integer, Double> generationsAverageTravelTimeStdDev = new HashMap<Integer, Double>();
//		String generationPath = "zurich_1pm/VirtualCity/Population/HistoryLog/Generation";
//		Map<Integer, Double> generationsBestTravelTime = new HashMap<Integer, Double>();
//		Map<String, NetworkScoreLog> networkScores = new HashMap<String, NetworkScoreLog>();
//		for (int g = 1; g <= lastGeneration; g++) {
//			double averageTravelTimeThisGeneration = 0.0;
//			double averageTravelTimeStdDevThisGeneration = 0.0;
//			double bestAverageTravelTimeThisGeneration = Double.MAX_VALUE;
//			networkScores = (Map<String, NetworkScoreLog>) XMLOps.readFromFile(networkScores.getClass(),
//					generationPath + g + "/networkScoreMap.xml");
//			for (NetworkScoreLog nsl : networkScores.values()) {
//				if (nsl.averageTravelTime < bestAverageTravelTimeThisGeneration) {
//					bestAverageTravelTimeThisGeneration = nsl.averageTravelTime;
//					System.out.println("bestAverageTravelTimeThisGeneration = " + bestAverageTravelTimeThisGeneration);
//				}
//				averageTravelTimeThisGeneration += nsl.averageTravelTime / networkScores.size();
//				averageTravelTimeStdDevThisGeneration += nsl.stdDeviationTravelTime / networkScores.size();
//			}
//			generationsAverageTravelTime.put(g, averageTravelTimeThisGeneration);
//			generationsAverageTravelTimeStdDev.put(g, averageTravelTimeStdDevThisGeneration);
//			generationsBestTravelTime.put(g, bestAverageTravelTimeThisGeneration);
//		}
//		XYLineChart chart = new XYLineChart("Evolution of Network Performance", "Generation", "Score");
//		chart.addSeries("Average Travel Time [min]", generationsAverageTravelTime);
//		chart.addSeries("Average Travel Time - Std Deviation [min]", generationsAverageTravelTimeStdDev);
//		chart.addSeries("Best Average Travel Time [min]", generationsBestTravelTime);
//		chart.saveAsPng(fileName, 800, 600);
//	}
	
}

