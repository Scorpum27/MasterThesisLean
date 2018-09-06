package ch.ethz.matsim.students.samark;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import java.io.Serializable;

public class MRoute implements Serializable{

	private static final long serialVersionUID = 1L;

	String routeID;
	NetworkRoute networkRoute;
	List<Id<Node>> nodeList;
	List<Id<Link>> linkList;
	TransitLine transitLine;
	double routeLength;
	
	// from eventsFile
	String eventsFile;
	int nBoardings;
	double personMetroKM;
	
	// from transitScheduleFile
	int nDepartures;
	double departureSpacing;
	double firstDeparture;
	String transitScheduleFile;
	double drivenKM;
	double opsCost;
	double constrCost;
	double undergroundPercentage;	
	
	public MRoute() {
	}
	
	public MRoute(String name) {	
		this.routeID = name;
		this.undergroundPercentage = 0.0;
		this.personMetroKM = 0.0;
		this.nBoardings = 0;
	}
	
	
	public String getId() {
		return this.routeID;
	}
	public void setId(String stringID) {
		this.routeID = stringID;
	}
	
	
	public NetworkRoute getNetworkRoute() {
		return this.networkRoute;
	}
	public void setNetworkRoute(NetworkRoute networkRoute) {
		this.networkRoute = networkRoute;
	}
	
	public List<Id<Node>> getNodeList() {
		return this.nodeList;
	}
	public void setNodeList(List<Id<Node>> list) {
		this.nodeList = list;
	}
	
	public List<Id<Link>> getLinkList() {
		return this.getLinkList();
	}
	public void setLinkList(List<Id<Link>> list) {
		this.linkList = list;
	}
	
	public String getTransitScheduleFile() {
		return this.transitScheduleFile;
	}
	public void setTransitScheduleFile(String transitScheduleFile) {
		this.transitScheduleFile = transitScheduleFile;
	}
	
	public String getEventsFile() {
		return this.eventsFile;
	}
	public void setEventsFile(String eventsFile) {
		this.eventsFile = eventsFile;
	}
	
	public double getDrivenKM() {
		return this.drivenKM;
	}
	public void setDrivenKM(double drivenKM) {
		this.drivenKM = drivenKM;
	}
	
	public double getPersonMetroKM() {
		return this.personMetroKM;
	}
	public void setMetroPersonKM(double personKM) {
		this.personMetroKM = personKM;
	}
	
	public Double getUndergroundPercentage() {
		return this.undergroundPercentage;
	}
	public void setUGPercentage(double UGPercentage) {
		this.undergroundPercentage = UGPercentage;
	}
	
	public Integer getBoardingNr() {
		return this.nBoardings;
	}
	public void setBoardingNr(int nPassengers) {
		this.nBoardings = nPassengers;
	}
	
	public Double getRouteLength() {
		return this.routeLength;
	}
	public void setRouteLength(double routeLength) {
		this.routeLength = routeLength;
	}

	public TransitLine getTransitLine() {
		return this.transitLine;
	}
	
	public void setTransitLine(TransitLine transitLine) {
		this.transitLine = transitLine;
	}
	
}

