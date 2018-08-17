package ch.ethz.matsim.students.samark;

import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.population.routes.NetworkRoute;

public class MRoute {

	String routeID;
	NetworkRoute networkRoute;
	List<Node> nodeList;
	List<Link> linkList;
	
	String eventsFile;
	String transitScheduleFile;
	double drivenKM;
	double routeLength;
	double undergroundPercentage;
	int nPassengers;
	double personKM;
	double opsCost;
	double constrCost;
	
	public MRoute(String name) {	
		this.routeID = name;
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
	
	public List<Node> getNodeList() {
		return this.nodeList;
	}
	public void setNodeList(List<Node> nodeList) {
		this.nodeList = nodeList;
	}
	
	public List<Link> getLinkList() {
		return this.getLinkList();
	}
	public void setLinkList(List<Link> linkList) {
		this.linkList = linkList;
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
	
	public double getPersonKM() {
		return this.personKM;
	}
	public void setPersonKM(double personKM) {
		this.personKM = personKM;
	}
	
	public Double getUndergroundPercentage() {
		return this.undergroundPercentage;
	}
	public void setUGPercentage(double UGPercentage) {
		this.undergroundPercentage = UGPercentage;
	}
	
	public Integer getPassengerNr() {
		return this.nPassengers;
	}
	public void setPassengerNr(int nPassengers) {
		this.nPassengers = nPassengers;
	}
	
	public Double getRouteLength() {
		return this.routeLength;
	}
	public void setRouteLength(double routeLength) {
		this.routeLength = routeLength;
	}

	
}

