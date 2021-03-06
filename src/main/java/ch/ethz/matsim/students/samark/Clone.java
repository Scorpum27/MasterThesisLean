package ch.ethz.matsim.students.samark;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class Clone {

	public static <T> List<T> list(List<T> originalList){
		List<T> copy = new ArrayList<T>();
		for (T t : originalList) {
			copy.add(t);
		}
		return copy;
	}
	
	public static <T> Set<T> set(Set<T> originalSet){
		Set<T> copy = new HashSet<T>();
		for (T t : originalSet) {
			copy.add(t);
		}
		return copy;
	}

	public static <T, S> Map<T, S> map(Map<T, S> originalMap){
		Map<T,S> copy = new HashMap<T,S>();
		for (T t : originalMap.keySet()) {
			copy.put(t,originalMap.get(t));
		}
		return copy;
	}
	
	
	public static Map<String, MRoute> mRouteMap(Map<String, MRoute> original){
		Map<String, MRoute> copy = new HashMap<String, MRoute>();
		for (String s : original.keySet()) {
			copy.put(s, Clone.mRoute(original.get(s)));
		}
		return copy;
	}
	
	public static Map<Id<Link>, CustomLinkAttributes> customLinkMap(Map<Id<Link>, CustomLinkAttributes> original){
		 Map<Id<Link>, CustomLinkAttributes> copy = new HashMap<Id<Link>, CustomLinkAttributes>();
		for (Id<Link> id: original.keySet()) {
			copy.put(id, Clone.customLinkAttributes(original.get(id)));
		}
		return copy;
	}
	
	public static CustomLinkAttributes customLinkAttributes(CustomLinkAttributes original) {
		CustomLinkAttributes copy = new CustomLinkAttributes();
		copy.dominantMode = original.dominantMode;
		copy.totalTraffic = original.totalTraffic;
		copy.dominantStopFacility = original.dominantStopFacility;
		copy.nextRailwayStopFacility = original.nextRailwayStopFacility;
		copy.distance2nextRailwayStopFacility = original.distance2nextRailwayStopFacility;
		return copy;
	}

	public static Map<String, MNetwork> mNetworkMap(Map<String, MNetwork> original){
		Map<String, MNetwork> copy = new HashMap<String, MNetwork>();
		for (String s : original.keySet()) {
			copy.put(s, Clone.mNetwork(original.get(s)));
		}
		return copy;
	}

	public static MRoute mRoute(MRoute o) {
		MRoute copy = new MRoute();
		copy.routeID = o.routeID;
		copy.networkRoute = o.networkRoute.clone();
		if (o.nodeList != null) {
			copy.nodeList = Clone.nodeList(o.nodeList);
		}
		if(o.linkList != null) {
			copy.linkList = Clone.linkList(o.linkList);
		}
		if(o.facilityBlockedLinks != null) {
			copy.facilityBlockedLinks = Clone.linkList(o.facilityBlockedLinks);
		}
		if(o.transitLine != null) {
			copy.transitLine = Clone.transitLine(o.transitLine, ScenarioUtils.loadScenario(ConfigUtils.createConfig()).getTransitSchedule().getFactory());
		}
		copy.lastUtilityBalance = o.lastUtilityBalance;
		copy.attemptedFrequencyModifications = o.attemptedFrequencyModifications;
		copy.blockedFreqModGenerations = o.blockedFreqModGenerations;
		copy.freqModOccured = o.freqModOccured;
		copy.significantRouteModOccured = o.significantRouteModOccured;
		copy.lastFreqMod = o.lastFreqMod;
		copy.probNextFreqModPositive = o.probNextFreqModPositive;
		
		copy.routeLength = o.routeLength;
		copy.vehiclesNr = o.vehiclesNr;
		copy.lifeTime = o.lifeTime;
		
		copy.eventsFile = o.eventsFile;
		copy.nBoardings = o.nBoardings;
		copy.personMetroDist = o.personMetroDist;
		
		copy.nDepartures = o.nDepartures;
		copy.departureSpacing = o.departureSpacing;
		copy.firstDeparture = o.firstDeparture;
		copy.lastDeparture = o.lastDeparture;
		copy.roundtripTravelTime = o.roundtripTravelTime;
		copy.transitScheduleFile = o.transitScheduleFile;
		copy.totalDrivenDist = o.totalDrivenDist;
		copy.nStationsExtend = o.nStationsExtend;
		copy.nStationsNew = o.nStationsNew;
		copy.opsCost = o.opsCost;
		copy.constrCost = o.constrCost;
		copy.utilityBalance = o.utilityBalance;
		copy.undergroundPercentage = o.undergroundPercentage;
		copy.NewUGpercentage = o.NewUGpercentage;
		copy.DevelopUGPercentage = o.DevelopUGPercentage;
		copy.NewOGpercentage = o.NewOGpercentage;
		copy.EquipOGPercentage = o.EquipOGPercentage;
		copy.DevelopOGPercentage = o.DevelopOGPercentage;
		return copy;
	}
	
	public static TransitSchedule transitSchedule(TransitSchedule o) {
		TransitSchedule copy = ScenarioUtils.loadScenario(ConfigUtils.createConfig()).getTransitSchedule();
		for (TransitStopFacility tsf : o.getFacilities().values()) {
			copy.addStopFacility(Clone.transitStopFacility(tsf, o.getFactory()));
		}
		for (TransitLine tl : o.getTransitLines().values()) {
			copy.addTransitLine(Clone.transitLine(tl, o.getFactory()));
		}
		return copy;
	}
	
	public static TransitStopFacility transitStopFacility(TransitStopFacility o, TransitScheduleFactory tsf) {
		TransitStopFacility copy = tsf.createTransitStopFacility(o.getId(), o.getCoord(), o.getIsBlockingLane());
		copy.setLinkId(o.getLinkId());
		copy.setName(o.getName());
		copy.setStopAreaId(o.getStopAreaId());
		return copy;
	}

	public static TransitLine transitLine(TransitLine o, TransitScheduleFactory tsf) {
		TransitLine copy = tsf.createTransitLine(o.getId());
		copy.setName(o.getName());
		for (Id<TransitRoute> tr : o.getRoutes().keySet()) {
			TransitRoute TR = o.getRoutes().get(tr);
			TransitRoute TRR = tsf.createTransitRoute(tr, TR.getRoute().clone(), Clone.list(TR.getStops()), TR.getTransportMode());
			for (Departure d : TR.getDepartures().values()){ 			// DEFAULT MODULE
				TRR.addDeparture(d);
			}
			copy.addRoute(TRR);
		}
		return copy;
	}
	
	public static Network network(Network o) {
		if(o==null) {
			return null;
		}
		Network copy = ScenarioUtils.loadScenario(ConfigUtils.createConfig()).getNetwork();
		NetworkFactory networkFactory = copy.getFactory();
		// connectingLinkToToNode.setAllowedModes(transportModes);
		for (Link link : o.getLinks().values()) {
			Node tempFromNode = networkFactory.createNode(Id.createNodeId(link.getFromNode().getId()), link.getFromNode().getCoord());
			Node tempToNode = networkFactory.createNode(Id.createNodeId(link.getToNode().getId()), link.getToNode().getCoord());
			Link tempLink = networkFactory.createLink(Id.createLinkId(link.getId()), tempFromNode, tempToNode);
			Set<String> transportModes = o.getLinks().get(link.getId()).getAllowedModes();
			if (transportModes != null) {
				tempLink.setAllowedModes(transportModes);				
			}
			else {
				tempLink.setAllowedModes(transportModes);
			}
			if (copy.getNodes().keySet().contains(tempFromNode.getId())==false) {
				copy.addNode(tempFromNode);
			}
			if (copy.getNodes().keySet().contains(tempToNode.getId())==false) {
				copy.addNode(tempToNode);
			}
			if (copy.getLinks().keySet().contains(tempLink.getId())==false) {
				copy.addLink(tempLink);
			}
		}
		return copy;
	}
	
	
	
	public static MNetwork mNetwork(MNetwork o){
		MNetwork copy = new MNetwork();
		copy.network = Clone.network(o.network);
		copy.networkFileLocation = o.networkFileLocation;
		copy.networkID = o.networkID;
		copy.routeMap = Clone.mRouteMap(o.routeMap);
		copy.totalRouteLength = o.totalRouteLength;			// calculate from individual route lengths (one-way only) 
		copy.lifeTime = o.lifeTime;
		//copy.transitSchedule = o.transitSchedule;
		//copy.vehicles = o.getVehicles();

		copy.personMetroDist = o.personMetroDist;		// NetworkEvolutionRunSim.runEventsProcessing
		copy.personKMdirect = o.personKMdirect;			// to be implemented in: NetworkEvolutionRunSim.runEventsProcessing
		copy.nMetroUsers = o.nMetroUsers;				// NetworkEvolutionRunSim.runEventsProcessing
		copy.totalPtPersonDist = o.totalPtPersonDist;

		copy.totalDrivenDist = o.totalDrivenDist;				// TODO: to be implemented in NetworkEvolution (may make separate scoring function!) --> Take lengths from route lengths and km from nDepartures*routeLengths
		copy.totalVehiclesNr = o.totalVehiclesNr;
		copy.annualCost = o.annualCost;					// to be implemented in NetworkEvolution
		copy.annualBenefit = o.annualBenefit;				// to be implemented in NetworkEvolution
		copy.constructionCost = o.constructionCost;
		copy.operationalCost = o.operationalCost;
		
		copy.evolutionGeneration = o.evolutionGeneration;		// NetworkEvolution --> Evolutionary loop
		copy.averageTravelTime = o.averageTravelTime;		// NetworkEvolutionRunSim.peoplePlansProcessingM
		copy.stdDeviationTravelTime = o.stdDeviationTravelTime;	// NetworkEvolutionRunSim.peoplePlansProcessingM
		copy.totalTravelTime = o.totalTravelTime; 		// NetworkEvolutionRunSim.peoplePlansProcessingM
		copy.evoLog = o.evoLog;
		copy.parents = o.parents;
		copy.dominantParent = o.dominantParent;
		
		copy.travelTimeGainsPT = o.travelTimeGainsPT;
		copy.travelTimeGainsCar = o.travelTimeGainsCar;
		copy.otherGains = o.otherGains;
		
		copy.overallScore = o.overallScore;			// NetworkEvolution main separate line
		return copy;
	}
	
	public static MNetworkPop mNetworkPop(MNetworkPop o) {
		MNetworkPop copy = new MNetworkPop();
		copy.mNetworkFileLocationMap = o.mNetworkFileLocationMap;
		copy.modifiedNetworksInLastEvolution = Clone.list(o.modifiedNetworksInLastEvolution);
		copy.networkMap = Clone.mNetworkMap(o.networkMap);
		copy.populationId = o.populationId;
		return copy;
	}

	private static List<Id<Node>> nodeList(List<Id<Node>> o) {
		List<Id<Node>> copy = new ArrayList<Id<Node>>();
		for (Id<Node> node : o) {
			copy.add(node);
		}
		return copy;
	}
	
	private static List<Id<Link>> linkList(List<Id<Link>> o) {
		List<Id<Link>> copy = new ArrayList<Id<Link>>();
		for (Id<Link> link : o) {
			copy.add(link);
		}
		return copy;
	}
	
	
}