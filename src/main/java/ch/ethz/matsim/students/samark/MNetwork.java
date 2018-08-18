package ch.ethz.matsim.students.samark;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.network.Network;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicles;

public class MNetwork {

	public MNetwork(String name) {
		this.networkID = name;
		this.routeMap = new HashMap<String, MRoute>();
	}
	
	String networkID;
	Network network;
	Map<String, MRoute> routeMap;
	TransitSchedule transitSchedule;
	Vehicles vehicles;
	
	// from events
	double personKM;
	double personKMdirect;
	int nPassengers;
	// from transitSchedule
	double drivenKM;
	double opsCost;
	double constrCost;
	// from evolution loop
	int evolutionGeneration;
	// from individual routes
	double averageScore;
	double stdScoreDeviation;
	double totalTravelTime;
	
	
	public void addNetworkRoute(MRoute newRoute) {
		// consider changing name of route to: newRoute.routeID = (this.networkID+newRoute.routeID);
		routeMap.put(newRoute.routeID, newRoute);
	}
	
	public Map<String, MRoute> getNetworkRoutes(){
		return this.routeMap;
	}

	public void addRoutes(List<MRoute> routesList) {
		for (MRoute route : routesList) {
			this.routeMap.put(route.routeID, route);
		}
		
	}
	
}
