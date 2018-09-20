package ch.ethz.matsim.students.samark;

import java.io.FileNotFoundException;
import java.util.ArrayList;
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
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import com.google.common.collect.Sets;


public class VC_NetworkImpl {

	public static Network fill(int xmax, int ymax, Network networkIn, double unitLinkLength) {
		Network network = fillNodes(xmax, ymax, networkIn);
		network = fillNeighboringLinks(xmax, ymax, network);
		network = addGeometricLinkLengths(network, unitLinkLength);
		return network;
	}

	public static Network fillNodes(int xmax, int ymax, Network networkIn)	{
		NetworkFactory NF = networkIn.getFactory();
		for (int y=1; y<=ymax; y++) {
			for (int x=1; x<=xmax; x++) {
				Coord tempCoord = new Coord(x,y);
				int nodeNr = xyCoordToNr((int)tempCoord.getX(),(int)tempCoord.getY(), xmax);
				Node newNode = NF.createNode(Id.createNodeId(nodeNr), tempCoord);				// naming of nodes .. fill in from left to right and bottom to top
				networkIn.addNode(newNode);														// add all nodes to network
				// System.out.println("Added node "+newNode.getId().toString()+" to network.");
			}
		}
		return networkIn;
	}
	 
	public static Network fillNeighboringLinks(int xmax, int ymax, Network networkIn) {				// connects all nodes of perfect network with links and returns new network with all links
		 																								// only in perfect network
		 for (int y=1; y<=ymax; y++) {
				for (int x=1; x<=xmax; x++) {
					
					Node centerNode = networkIn.getNodes().get(Id.createNodeId(xyCoordToNr(x, y, xmax)));
					
					 int nodeX = (int) centerNode.getCoord().getX();
					 int nodeY = (int) centerNode.getCoord().getY();
				
					 int askIfNodeRight = xyCoordToNr(nodeX+1, nodeY, xmax);
					 if (0 < nodeX+1 && nodeX+1 <= xmax && 0 < nodeY && nodeY <= ymax) {
						 Link rightLink = networkIn.getFactory().createLink(Id.createLinkId(centerNode.getId().toString() + "_" + askIfNodeRight), centerNode, networkIn.getNodes().get(Id.createNodeId(askIfNodeRight)));
						 rightLink.setAllowedModes(Sets.newHashSet("pt", "car"));
						 networkIn.addLink(rightLink);
					 }	 
					 int askIfNodeTop = xyCoordToNr(nodeX, nodeY+1, xmax);
					 if (0 < nodeX && nodeX <= xmax && 0 < nodeY+1 && nodeY+1 <= ymax) {
						 Link topLink = networkIn.getFactory().createLink(Id.createLinkId(centerNode.getId().toString() + "_" + askIfNodeTop), centerNode, networkIn.getNodes().get(Id.createNodeId(askIfNodeTop)));
						 topLink.setAllowedModes(Sets.newHashSet("pt", "car"));
						 networkIn.addLink(topLink);
					 }	 
					 int askIfNodeLeft = xyCoordToNr(nodeX-1, nodeY, xmax);
					 if (0 < nodeX-1 && nodeX-1 <= xmax && 0 < nodeY && nodeY <= ymax) {
						 Link leftLink = networkIn.getFactory().createLink(Id.createLinkId(centerNode.getId().toString() + "_" + askIfNodeLeft), centerNode, networkIn.getNodes().get(Id.createNodeId(askIfNodeLeft)));
						 leftLink.setAllowedModes(Sets.newHashSet("pt", "car"));
						 networkIn.addLink(leftLink);
					 }	 
					 int askIfNodeBottom = xyCoordToNr(nodeX, nodeY-1, xmax);
					 if (0 < nodeX && nodeX <= xmax && 0 < nodeY-1 && nodeY-1 <= ymax) {
						 Link bottomLink = networkIn.getFactory().createLink(Id.createLinkId(centerNode.getId().toString() + "_" + askIfNodeBottom), centerNode, networkIn.getNodes().get(Id.createNodeId(askIfNodeBottom)));
						 bottomLink.setAllowedModes(Sets.newHashSet("pt", "car"));
						 networkIn.addLink(bottomLink);
					 }
				}
		 }
		 
		 Network networkOut = networkIn;
		 return networkOut;
	}

	public static Network addGeometricLinkLengths(Network network, double unitLinkLength) {
		for (Id<Link> linkID : network.getLinks().keySet()) {
			double linkLength = unitLinkLength*GeomDistance.calculate(network.getLinks().get(linkID).getFromNode().getCoord(), network.getLinks().get(linkID).getToNode().getCoord());
			network.getLinks().get(linkID).setLength(linkLength);
			// System.out.println("Length of link "+linkID.toString()+" is "+linkLength+".");
		}
		return network;
	}


	public static Network thin(Network network, int XMax, int YMax, int removalPercentage, boolean saveAsFile, String fileName) {
		Network nwThin = network;
		int networkNodesNumber = nwThin.getNodes().size();
		int nodeAmountRemove = (int) (networkNodesNumber*removalPercentage/100);
		System.out.println("Removing a total of "+nodeAmountRemove+" nodes");
		for (int n=1; n<=nodeAmountRemove; n++) {
			
			Id<Node> nodeToRemoveID = null;
			do  {
				Random r = new Random();
				int nodeNrToRemove = r.nextInt(networkNodesNumber)+1;
				nodeToRemoveID = Id.createNodeId(nodeNrToRemove);
			} while (nwThin.getNodes().containsKey(nodeToRemoveID) == false);

			// System.out.println("Chose to thin around node "+nodeToRemoveID.toString());
			for (Id<Link> inLink : nwThin.getNodes().get(nodeToRemoveID).getInLinks().keySet()) {
				nwThin.removeLink(inLink);
				// System.out.println("Removing inLink "+inLink.toString());
			}
			for (Id<Link> outLink : nwThin.getNodes().get(nodeToRemoveID).getOutLinks().keySet()) {
				nwThin.removeLink(outLink);
				// System.out.println("Removing outLink "+outLink.toString());
			}
			nwThin.removeNode(nodeToRemoveID);
			// System.out.println("Removed node "+nodeToRemoveID.toString()+" and its links");

		}
		if (saveAsFile) {
			NetworkWriter nwT = new NetworkWriter(nwThin);
			nwT.write(fileName);
			System.out.println("Saved new (thinned) network as "+fileName);
		}
		
		// Display all (remaining) nodes of current desired network
			/* n = 1;
			 * for (Id<Node> nID : nwThin.getNodes().keySet()) {
			 * System.out.println("Remaining node "+n+" is: "+nID.toString());
			 * n++;}
			 */
		
		return nwThin;
	}

	public static void writeToFile(int XMax, int YMax, Network network, String fileName) {
		NetworkWriter nw = new NetworkWriter(network);
		nw.write(fileName);
	}


	public static boolean exist(boolean containsKey) {
		if (containsKey == true) {
			return true;
		}
		else {
			return false;
		}
	}

	public static int xyCoordToNr(int x, int y, int xmax) {
		return (int) ((y-1)*xmax + x);
	}
		
	public static Coord nrToCoord(int nr, int xmax, int ymax) {
		double x = nr % ymax;
		double y = (nr-x) / xmax;
		Coord xyCoord = new Coord(x,y);
		return xyCoord;
	}

	public static NetworkRoute createNetworkRoute(Network networkThin, int XMax, int YMax, int outerFramePercentage, int minSpacingPercentage) {
		
		ArrayList<Node> routeNodeList = new ArrayList<Node>();
		do{
			routeNodeList = createRandomRoute(networkThin, XMax, YMax, outerFramePercentage, minSpacingPercentage); 		// makes random starting points in network in outer network regions
		} while(routeNodeList==null);
		
		NetworkRoute networkRoute = NodeListToNetworkRoute(networkThin, routeNodeList);			// convert from node list format to network route by connecting the corresponding links
		return networkRoute;	
	}

	
	public static Network createAndWriteNetworkRouteToNetwork(Config config, Network network, NetworkRoute networkRoute, Set<String> transportModes, int lineNr, int XMax, int YMax, int removalPercentage) {
		Network shortestPathNetwork = ScenarioUtils.createScenario(config).getNetwork();
		NetworkFactory shortestPathNetworkFactory = shortestPathNetwork.getFactory();
		// Link tempLink = null;
		// Node tempToNode = null;
		// Node tempFromNode = null;
		for (Id<Link> linkID : VC_PublicTransportImpl.networkRouteToLinkIdList(networkRoute)) {
			Node tempToNode = shortestPathNetworkFactory.createNode(network.getLinks().get(linkID).getToNode().getId(), network.getLinks().get(linkID).getToNode().getCoord());
			Node tempFromNode = shortestPathNetworkFactory.createNode(network.getLinks().get(linkID).getFromNode().getId(), network.getLinks().get(linkID).getFromNode().getCoord());
			Link tempLink = shortestPathNetworkFactory.createLink(network.getLinks().get(linkID).getId(), tempFromNode, tempToNode);
			tempLink.setAllowedModes(transportModes);
			if (shortestPathNetwork.getNodes().containsKey(tempToNode.getId())==false) {
				shortestPathNetwork.addNode(tempToNode);
			}
			if (shortestPathNetwork.getNodes().containsKey(tempFromNode.getId())==false) {
				shortestPathNetwork.addNode(tempFromNode);
			}
			if (shortestPathNetwork.getLinks().containsKey(tempLink.getId())==false) {
				shortestPathNetwork.addLink(tempLink);
			}
		}
		
		NetworkWriter nwShortestPath = new NetworkWriter(shortestPathNetwork);
		String filepathShortestPath = "zurich_1pm/VirtualCity/Input/Generated_Networks/ShortestPath_"+XMax+"x"+YMax+"_"+removalPercentage+"PercentLean_TransitLineNr"+lineNr+".xml";
		nwShortestPath.write(filepathShortestPath);
		
		return shortestPathNetwork;
	}

	public static NetworkRoute NodeListToNetworkRoute(Network network, ArrayList<Node> nodeList) {
		// ArrayList<Link> linkListArray = new ArrayList<Link>(nodeList.size());
		List<Id<Link>> linkList = new ArrayList<Id<Link>>(nodeList.size()-1);
		for (int n=0; n<(nodeList.size()-1); n++) {
			// System.out.println("n= "+n);
			// System.out.println("nodeListSize= "+nodeList.size());
			for (Link l : nodeList.get(n).getOutLinks().values()) {
				if (l.getToNode() == nodeList.get(n+1)) {
					linkList.add(l.getId());
					System.out.println("Adding link "+l.getId().toString());
				}
			}
		}
		
		NetworkRoute networkRoute = RouteUtils.createNetworkRoute(linkList, network);
		return networkRoute;
	}
	

	public static ArrayList<Node> createRandomRoute(Network network, int XMax, int YMax, int outerFramePercentage, int minSpacingPercentage) {
		Node startNode = createStartOrEndNode(network, XMax, YMax, outerFramePercentage);
		System.out.println("Start Node: "+startNode.getId().toString());
		Node endNode = startNode; // just for initializing
		do {
			endNode = createStartOrEndNode(network, XMax, YMax, outerFramePercentage);
			// System.out.println("Distance between node is: "+GeomDistance.calculate(startNode.getCoord(), endNode.getCoord()));
		} while(endNode.equals(startNode) || GeomDistance.calculate(startNode.getCoord(), endNode.getCoord())<=Math.sqrt(1.0*XMax*XMax+YMax*YMax)*outerFramePercentage/100);
		System.out.println("End Node: "+endNode.getId().toString());
		return createRouteBetweenNodes(network, startNode, endNode);
	}	
	
	public static ArrayList<Node> createRouteBetweenNodes(Network network, Node startNode, Node endNode) {
		// System.out.println("started route generator");
		ArrayList<Node> dijkstraNodePath = DijkstraOwn_I.findShortestPath(network, startNode, endNode);
		if(dijkstraNodePath == null) {
			System.out.println("Error: No path found between start and end node! Trying a new pair of start/end nodes ... ");
			return null;
		}
		// System.out.println("To my surprise it worked ...");
		return dijkstraNodePath;
	}
	
	public static List<TransitRouteStop> createAndAddNetworkRouteStops(TransitSchedule transitSchedule, Network network, NetworkRoute networkRoute, String defaultPtMode, double stopTime, double maxVehicleSpeed, boolean blocksLane){
		TransitScheduleFactory transitScheduleFactory = transitSchedule.getFactory();
		
		List<TransitRouteStop> stopArray = new ArrayList<TransitRouteStop>();				// prepare an array for stop facilities on new networkRoute
		
		int stopCount = 0;
		double accumulatedDrivingTime = 0;
		Link lastLink = null;
		
		List<Id<Link>> routeLinkList = new ArrayList<Id<Link>>();
		routeLinkList.addAll(Metro_NetworkImpl.networkRouteToLinkIdList(networkRoute));
		routeLinkList.addAll(OppositeLinkListOf(Metro_NetworkImpl.networkRouteToLinkIdList(networkRoute), network));	// TODO: Reverse Routes for this network
		for (Id<Link> linkID : routeLinkList) {
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
		/*for (int s=0; s<stopArray.size(); s++) {
			System.out.println(stopArray.get(s).toString());
		}*/
		return stopArray;
	}
	
	
	public static List<Id<Link>> OppositeLinkListOf(List<Id<Link>> linkList, Network network){
		List<Id<Link>> oppositeLinkList = new ArrayList<Id<Link>>(linkList.size());
		for (int c=0; c<linkList.size(); c++) {
			Id<Link> linkToBeReversed = linkList.get(linkList.size()-1-c);
			oppositeLinkList.add(ReverseLink(linkToBeReversed, network));
		}
		return oppositeLinkList;
	}
	
	public static Id<Link> ReverseLink(Id<Link> linkId, Network network){
		Id<Link> reverseId = null;
		Id<Node> fromNode = network.getLinks().get(linkId).getFromNode().getId();
		Id<Node> toNode = network.getLinks().get(linkId).getToNode().getId();
		for (Id<Link> thisLinkId : network.getLinks().keySet()) {
			Link thisLink = network.getLinks().get(thisLinkId);
			if (thisLink.getFromNode().getId() == toNode && thisLink.getToNode().getId() == fromNode) {
				reverseId = thisLink.getId();
				break;
			}
		}
		return reverseId;
	}
	
	
	public static Node createStartOrEndNode(Network network, int XMax, int YMax, int outerFramePercentage) {
		int xFrameWidth = (int) (XMax*outerFramePercentage/100) + 1 ;
		int yFrameWidth = (int) (YMax*outerFramePercentage/100) + 1 ;
		Id<Node> frameNodeID = null;
		int rXint = 0;
		int rYint = 0;
		do {
			boolean inFrameX = false;
			while(inFrameX == false) {
				Random rX = new Random();
				rXint = rX.nextInt(XMax)+1;
				if (rXint < xFrameWidth || XMax-xFrameWidth < rXint) {
					inFrameX = true;
				}
			}
			boolean inFrameY = false;
			while(inFrameY == false) {
				Random rY = new Random();
				rYint = rY.nextInt(YMax)+1;
				if (rYint < yFrameWidth || YMax-yFrameWidth < rYint) {
					inFrameY = true;
				}
			}
			int frameNodeNr = VC_NetworkImpl.xyCoordToNr(rXint, rYint, XMax);
			frameNodeID = Id.createNodeId(frameNodeNr);
		} while (network.getNodes().containsKey(frameNodeID)==false);
		Node frameNode = network.getNodes().get(frameNodeID);
		// System.out.println("Chosen node is "+frameNode.getId().toString());
		return frameNode;
	}
	
	public static void runEventsProcessing(MNetworkPop networkPopulation, int lastIteration) {
		for (MNetwork mNetwork : networkPopulation.networkMap.values()) {
			String networkName = mNetwork.networkID;
			
			// read and handle events
			String eventsFile = "zurich_1pm/VirtualCity/Population/"+networkName+"/Simulation_Output/ITERS/it."+lastIteration+"/"+lastIteration+".events.xml.gz";			
			MHandlerPassengers mPassengerHandler = new MHandlerPassengers(null, null); // TODO Caution; Insert proper network & transitSchedule
			EventsManager eventsManager = EventsUtils.createEventsManager();
			eventsManager.addHandler(mPassengerHandler);
			MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager);
			eventsReader.readFile(eventsFile);
			
			// read out travel stats and display important indicators to console
			Map<String, Map<String, Double>> travelStats = mPassengerHandler.travelStats;				// Map< PersonID, Map<RouteName,TravelDistance>>
			Map<String, Integer> routeBoardingCounter = mPassengerHandler.routeBoardingCounter;			// Map<RouteName, nBoardingsOnThatRoute>
			// double totalBeelineDistance = mPassengerHandler.totalBeelineKM;
			Map<String, Double> personKMonRoutes = new HashMap<String, Double>();						// Map<RouteName, TotalPersonKM>
			double totalMetroPersonKM = 0.0;
			int nMetroUsers = travelStats.size(); 														// total number of persons who use the metro
			//System.out.println("Number of Metro Users = " + nMetroUsers);
			int nTotalBoardings = 0;
			for (int i : routeBoardingCounter.values()) {
				nTotalBoardings += i;
			}
			System.out.println("Total Metro Boardings = "+nTotalBoardings);
			
			for (Map<String, Double> routesStats : travelStats.values()) {
				for (String route : routesStats.keySet()) {
					if (personKMonRoutes.containsKey(route)) {
						personKMonRoutes.put(route, personKMonRoutes.get(route)+routesStats.get(route));
						//System.out.println("Putting on Route " +route+ " an additional " + routesStats.get(route) + " to a total of " + personKMonRoutes.get(route));  
					}
					else {
						personKMonRoutes.put(route, routesStats.get(route));
						//System.out.println("Putting on Route " +route+ " an initial " + personKMonRoutes.get(route)); 
					}
				}
			}
			
			for (String route : personKMonRoutes.keySet()) {
				totalMetroPersonKM += personKMonRoutes.get(route);
			}
			//System.out.println("Total Metro TransitKM = " + totalMetroPersonKM);

			
			// fill in performance indicators and scores in MRoutes
			for (String routeId : mNetwork.routeMap.keySet()) {
				if (personKMonRoutes.containsKey(routeId)) {					
					MRoute mRoute = mNetwork.routeMap.get(routeId);
					mRoute.personMetroKM = personKMonRoutes.get(routeId);
					mRoute.nBoardings = routeBoardingCounter.get(routeId);
					mNetwork.routeMap.put(routeId, mRoute);
				}
			}
	
			// fill in performance indicators and scores in MNetworks
			// TODO [NOT PRIO] mNetwork.mPersonKMdirect = beelinedistances;
			mNetwork.totalMetroPersonKM = totalMetroPersonKM;
			mNetwork.nMetroUsers = nMetroUsers;
		}		// END of NETWORK Loop

		// - Maybe hand over score to a separate score map for sorting scores
	}
	
	public static void peoplePlansProcessingM(MNetworkPop networkPopulation, int maxTravelTimeInMin) {
		//System.out.println("Population name = "+networkPopulation.populationId);
		//System.out.println("Population size = "+networkPopulation.networkMap.size());
		for (MNetwork mNetwork : networkPopulation.networkMap.values()) {
			// TEST			
			String networkName = mNetwork.networkID;
			//System.out.println("NetworkName = "+networkName);
			String finalPlansFile = "zurich_1pm/VirtualCity/Population/"+networkName+"/Simulation_Output/output_plans.xml.gz";			
			Config emptyConfig = ConfigUtils.createConfig();
			emptyConfig.getModules().get("plans").addParam("inputPlansFile", finalPlansFile);
			Scenario emptyScenario = ScenarioUtils.loadScenario(emptyConfig);
			Population finalPlansPopulation = emptyScenario.getPopulation();
			//PopulationReader p = new PopulationReader(emptyScenario);
			Double[] travelTimeBins = new Double[maxTravelTimeInMin+1];
			for (int d=0; d<travelTimeBins.length; d++) {
				travelTimeBins[d] = 0.0;
			}
			for (Person person : finalPlansPopulation.getPersons().values()) {
				//System.out.println("Person = "+person.getId().toString());
				double personTravelTime = 0.0;
				Plan plan = person.getSelectedPlan();
				for (PlanElement element : plan.getPlanElements()) {
						if (element instanceof Leg) {
							/*System.out.println("Plan Elements is: "+element.toString());
							System.out.println("Plan Elements Attributes are: "+element.getAttributes().toString());
							System.out.println("Plan Elements Attribute travTime is: "+element.getAttributes().getAttribute("mode"));
							System.out.println("Plan Elements Attribute travTime is: "+element.getAttributes().getAttribute("travTime"));*/
							String findString = "[travTime=";
							int i1 = element.toString().indexOf(findString);
							//System.out.println("i1 is: "+i1);
							String travTime = element.toString().substring(i1+findString.length(), i1+findString.length()+8);
							//System.out.println("Plan Elements Attribute travTime is: "+travTime);

							//System.out.println("Plan Elements Attribute travTime is: "+element.getAttributes().getAttribute("trav_time"));
							//System.out.println(element.getAttributes().getAttribute("travTime").getClass().getName());
							String[] HourMinSec = travTime.split(":");
							//System.out.println("Person Travel Time of this leg in [s] = "+travTime);
							personTravelTime += (Double.parseDouble(HourMinSec[0])*3600+Double.parseDouble(HourMinSec[1])*60+Double.parseDouble(HourMinSec[2]))/60;
							//System.out.println("Total Person Travel Time of this leg in [m] = "+personTravelTime);
						}
				}
				if (personTravelTime>=maxTravelTimeInMin) {
					travelTimeBins[maxTravelTimeInMin]++;
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
			double standardDeviationInnerSum = 0.0;
			for (int i=0; i<travelTimeBins.length; i++) {
				for (int j=0; j<travelTimeBins[i]; j++) {
					standardDeviationInnerSum += Math.pow(i-mNetwork.averageTravelTime, 2);
				}
			}
			double standardDeviation = Math.sqrt(standardDeviationInnerSum/(travels-1));
			
			mNetwork.stdDeviationTravelTime = standardDeviation;
			//System.out.println("standardDeviation = " + mNetwork.stdDeviationTravelTime);
			//System.out.println("averageTravelTime = " + mNetwork.averageTravelTime);
			
		}
		for (MNetwork network : networkPopulation.networkMap.values()) {
			System.out.println(network.networkID+" AverageTavelTime [min] = "+network.averageTravelTime+"   (StandardDeviation="+network.stdDeviationTravelTime+")");
			System.out.println(network.networkID+" TotalTravelTime [min] = "+network.totalTravelTime);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static void writeChartAverageTravelTimes(int lastGeneration, String fileName) throws FileNotFoundException { 	// Average and Best Scores
		Map<Integer, Double> generationsAverageTravelTime = new HashMap<Integer, Double>();
		Map<Integer, Double> generationsAverageTravelTimeStdDev = new HashMap<Integer, Double>();
		String generationPath = "zurich_1pm/VirtualCity/Population/HistoryLog/Generation";
		Map<Integer, Double> generationsBestTravelTime = new HashMap<Integer, Double>();
		Map<String, NetworkScoreLog> networkScores = new HashMap<String, NetworkScoreLog>();
		for (int g = 1; g <= lastGeneration; g++) {
			double averageTravelTimeThisGeneration = 0.0;
			double averageTravelTimeStdDevThisGeneration = 0.0;
			double bestAverageTravelTimeThisGeneration = Double.MAX_VALUE;
			networkScores = (Map<String, NetworkScoreLog>) XMLOps.readFromFile(networkScores.getClass(),
					generationPath + g + "/networkScoreMap.xml");
			for (NetworkScoreLog nsl : networkScores.values()) {
				if (nsl.averageTravelTime < bestAverageTravelTimeThisGeneration) {
					bestAverageTravelTimeThisGeneration = nsl.averageTravelTime;
					System.out.println("bestAverageTravelTimeThisGeneration = " + bestAverageTravelTimeThisGeneration);
				}
				averageTravelTimeThisGeneration += nsl.averageTravelTime / networkScores.size();
				averageTravelTimeStdDevThisGeneration += nsl.stdDeviationTravelTime / networkScores.size();
			}
			generationsAverageTravelTime.put(g, averageTravelTimeThisGeneration);
			generationsAverageTravelTimeStdDev.put(g, averageTravelTimeStdDevThisGeneration);
			generationsBestTravelTime.put(g, bestAverageTravelTimeThisGeneration);
		}
		XYLineChart chart = new XYLineChart("Evolution of Network Performance", "Generation", "Score");
		chart.addSeries("Average Travel Time [min]", generationsAverageTravelTime);
		chart.addSeries("Average Travel Time - Std Deviation [min]", generationsAverageTravelTimeStdDev);
		chart.addSeries("Best Average Travel Time [min]", generationsBestTravelTime);
		chart.saveAsPng(fileName, 800, 600);
	}
	
}

