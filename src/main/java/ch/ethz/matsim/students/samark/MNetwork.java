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
		this.totalMetroPersonKM = 0.0;
		this.averageTravelTime = Double.MAX_VALUE;
		this.stdDeviationTravelTime = Double.MAX_VALUE;
		this.totalTravelTime = Double.MAX_VALUE;
		this.totalMetroPersonKM = 0.0;
		this.personKMdirect = 0.0;
		this.nMetroUsers = 0;
		this.drivenKM = 0.0;
		this.totalRouteLength = 0.0;		
		this.opsCost = Double.MAX_VALUE;
		this.constrCost = Double.MAX_VALUE;
		this.evolutionGeneration = 0;
		this.overallScore = 0.0;
		this.totalVehiclesNr = 0;

	}
	
	public MNetwork(String name) {
		this.networkID = name;
		this.routeMap = new HashMap<String, MRoute>();
		this.totalMetroPersonKM = 0.0;
		this.averageTravelTime = Double.MAX_VALUE;
		this.stdDeviationTravelTime = Double.MAX_VALUE;
		this.totalTravelTime = Double.MAX_VALUE;
		this.totalMetroPersonKM = 0.0;
		this.personKMdirect = 0.0;
		this.nMetroUsers = 0;
		this.drivenKM = 0.0;
		this.opsCost = Double.MAX_VALUE;
		this.constrCost = Double.MAX_VALUE;
		this.evolutionGeneration = 0;
		this.overallScore = 0.0;
		this.totalRouteLength = 0.0;	
		this.totalVehiclesNr = 0;

	}
	
	public void calculateNetworkScore() {
		double a1 = 100.0;
		double b = 18000000.0;
		double c = -0.266;
		double d = 0.764;
//		this.overallScore = Math.exp((this.averageTravelTime-60)/(-a1))  +  this.totalMetroPersonKM/100000 * (c+Math.exp((this.drivenKM)/(b)-d));	// CostPerMetroKM = C(m) = c+exp(this.drivenKM/b-d)
		this.overallScore = 				0.0						     +  this.totalMetroPersonKM/100000 * (c+Math.exp((this.drivenKM)/(b)-d));	// CostPerMetroKM = C(m) = c+exp(this.drivenKM/b-d)

//		double a1 = 100.0;  double b = 18000000.0;  double c = -0.266;  double d = 0.764;
//		System.out.println("Math.exp((this.averageTravelTime-60)/(-a1))  +  this.totalMetroPersonKM/100000 * (c+Math.exp((this.drivenKM)/(b)-d)); = " + Math.exp((mnetwork.averageTravelTime-60)/(-a1)) +" + " +mnetwork.totalMetroPersonKM/100000 * (c+Math.exp((mnetwork.drivenKM)/(b)-d)));
		
		// double a2 = 100.0;
		// old scoring function: this.overallScore = Math.exp((this.averageTravelTime-60)/(-a1))  +  Math.exp((this.drivenKM/this.totalMetroPersonKM)/(-a2));

	}
	
	public void calculateTotalRouteLengthAndDrivenKM() {
		double totalRouteLength = 0.0;
		double totalDrivenKM = 0.0;
		for (MRoute mroute : this.routeMap.values()) {
			totalRouteLength += mroute.routeLength;
			totalDrivenKM += mroute.drivenKM;
		}
		this.totalRouteLength = totalRouteLength;
		this.drivenKM = totalDrivenKM;
	}
	
	// CAUTION: When adding to MNetwork, also add in Clone.mNetwork!
	Network network;
	String networkFileLocation;
	String networkID;
	Map<String, MRoute> routeMap;
	double totalRouteLength;			// calculate from individual route lengths (one-way only) 
	TransitSchedule transitSchedule;
	Vehicles vehicles;
	
	// from events
	double totalMetroPersonKM;		// NetworkEvolutionRunSim.runEventsProcessing
	double personKMdirect;			// to be implemented in: NetworkEvolutionRunSim.runEventsProcessing
	int nMetroUsers;				// NetworkEvolutionRunSim.runEventsProcessing
	// from transitSchedule
	double drivenKM;				// TODO: to be implemented in NetworkEvolution (may make separate scoring function!) --> Take lengths from route lengths and km from nDepartures*routeLengths
	double opsCost;					// to be implemented in NetworkEvolution
	double constrCost;				// to be implemented in NetworkEvolution
	int totalVehiclesNr;
	// from evolution loop
	int evolutionGeneration;		// NetworkEvolution --> Evolutionary loop
	double averageTravelTime;		// NetworkEvolutionRunSim.peoplePlansProcessingM
	double stdDeviationTravelTime;	// NetworkEvolutionRunSim.peoplePlansProcessingM
	double totalTravelTime; 		// NetworkEvolutionRunSim.peoplePlansProcessingM
	// Calculate
	double overallScore;			// NetworkEvolution main separate line
	
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
