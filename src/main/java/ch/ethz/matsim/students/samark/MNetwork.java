package ch.ethz.matsim.students.samark;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.network.Network;

public class MNetwork {

	public MNetwork(String name) {
		this.networkID = name;
		this.routeMap = new HashMap<String, MRoute>();
	}
	
	String networkID;
	Network network;
	Map<String, MRoute> routeMap;
	
	double averageScore;
	double stdScoreDeviation;
	double personKM;
	double personKMdirect;
	double drivenKM;
	double opsCost;
	double constrCost;
	int evolutionGeneration;
	
	public void addNetworkRoute(MRoute newRoute) {
		// consider changing name of route to: newRoute.routeID = (this.networkID+newRoute.routeID);
		routeMap.put(newRoute.routeID, newRoute);
	}
	
	public Map<String, MRoute> getNetworkRoutes(){
		return this.routeMap;
	}
	
}
