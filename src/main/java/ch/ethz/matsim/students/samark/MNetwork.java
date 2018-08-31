package ch.ethz.matsim.students.samark;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.network.Network;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicles;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("MNetwork")
public class MNetwork implements Serializable{

	private static final long serialVersionUID = 1L;

	public MNetwork() {
	}
	
	public MNetwork(String name) {
		this.networkID = name;
		this.routeMap = new HashMap<String, MRoute>();
	}
	
	Network network;
	String networkID;
	Map<String, MRoute> routeMap;
	TransitSchedule transitSchedule;
	Vehicles vehicles;
	
	// from events
	double totalMetroPersonKM;
	double personKMdirect;
	int nMetroUsers;
	// from transitSchedule
	double drivenKM;
	double opsCost;
	double constrCost;
	// from evolution loop
	int evolutionGeneration;
	double averageTravelTime;
	double stdDeviationTravelTime;
	double totalTravelTime;
	
	public double getTotalTravelTime() {
		return this.totalTravelTime;
	}
	public void setTotalTravelTime(double totalTravelTime) {
		this.totalTravelTime = totalTravelTime;
	}
	
	public double getStdDeviationTravelTime() {
		return this.stdDeviationTravelTime;
	}
	public void setStdDeviationTravelTime(double stdDeviationTravelTime) {
		this.stdDeviationTravelTime = stdDeviationTravelTime;
	}	
	
	public double getAverageTravelTime() {
		return this.averageTravelTime;
	}
	public void setAverageTravelTime(double averageTravelTime) {
		this.averageTravelTime = averageTravelTime;
	}	
	
	public int getEvolutionGeneration() {
		return this.evolutionGeneration;
	}
	public void setEvolutionGeneration(int evolutionGeneration) {
		this.evolutionGeneration = evolutionGeneration;
	}
	
	public double getConstrCost() {
		return this.constrCost;
	}
	public void setConstrCost(double constrCost) {
		this.constrCost = constrCost;
	}		
	
	public double getOpsCost() {
		return this.opsCost;
	}
	public void setOpsCost(double opsCost) {
		this.opsCost = opsCost;
	}	
	
	public double getDrivenKM() {
		return this.drivenKM;
	}
	public void setDrivenKM(double drivenKM) {
		this.drivenKM = drivenKM;
	}
	
	public double getPersonKMdirect() {
		return this.personKMdirect;
	}
	public void setPersonKMdirect(double personKMdirect) {
		this.personKMdirect = personKMdirect;
	}		
	
	public double getTotalMetroPersonKM() {
		return this.totalMetroPersonKM;
	}
	public void setTotalMetroPersonKM(double personKM) {
		this.totalMetroPersonKM = personKM;
	}	
	
	public int getNMetroUsers() {
		return this.nMetroUsers;
	}
	public void setnMetroUsers(int n) {
		this.nMetroUsers = n;
	}
	
	public Vehicles getVehicles() {
		return this.vehicles;
	}
	
	public void setVehicles(Vehicles vehicles) {
		this.vehicles = vehicles;
	}
	
	public void setTransitSchedule(TransitSchedule ts) {
		this.transitSchedule = ts;
	}
	
	public TransitSchedule getTransitSchedule() {
		return this.transitSchedule;
	}
	
	public void setNetwork(Network network) {
		this.network = network;
	}
	
	public Network getNetwork() {
		return this.network;
	}
	
	public void setNetworkID(String networkID) {
		this.networkID = networkID;
	}
	
	public String getNetworkID() {
		return this.networkID;
	}
	
    private void writeObject(ObjectOutputStream oos) 
    	      throws IOException {
    	        oos.defaultWriteObject();
    	        oos.writeObject(network);
    	        //oos.writeUTF();
    	    }
    
    private void readObject(ObjectInputStream ois) 
    	      throws ClassNotFoundException, IOException {
    	        ois.defaultReadObject();
    	        this.network = (Network) ois.readObject();
    	        // Network network = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getNetwork();
    	    }
	
	public void addNetworkRoute(MRoute newRoute) {
		// consider changing name of route to: newRoute.routeID = (this.networkID+newRoute.routeID);
		routeMap.put(newRoute.routeID, newRoute);
	}
	
	public Map<String, MRoute> getRouteMap(){
		return this.routeMap;
	}
	
	public void setRouteMap(Map<String, MRoute> routeMap) {
		this.routeMap = routeMap;
	}

	public void addRoutes(List<MRoute> routesList) {
		for (MRoute route : routesList) {
			this.routeMap.put(route.routeID, route);
		}
		
	}
	
}
