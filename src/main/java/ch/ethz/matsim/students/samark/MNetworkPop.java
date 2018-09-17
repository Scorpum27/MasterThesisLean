package ch.ethz.matsim.students.samark;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MNetworkPop implements Serializable{

	private static final long serialVersionUID = 1L;
	
	// CAUTION: When adding to MNetworkPop, also add in Clone.mNetworkPop!
	String populationId;
	Map<String, MNetwork> networkMap;
	Map<String, String> mNetworkFileLocationMap;
	List<String> modifiedNetworksInLastEvolution;

	
	public MNetworkPop() {
		this.networkMap = new HashMap<String, MNetwork>();
		this.modifiedNetworksInLastEvolution = new ArrayList<String>();
	}
	
	public MNetworkPop(String id) {
		this.populationId = id;
		this.networkMap = new HashMap<String, MNetwork>();
		this.modifiedNetworksInLastEvolution = new ArrayList<String>();
	}
	
	public MNetworkPop(String id, int size) {
		this.populationId = id;
		this.networkMap = new HashMap<String, MNetwork>(size);
		this.modifiedNetworksInLastEvolution = new ArrayList<String>();
	}
	
	
	public Map<String, MNetwork> getNetworks(){
		return this.networkMap;
	}
	
	public void addNetwork(MNetwork newNetwork) {
		this.networkMap.put(newNetwork.networkID, newNetwork);
	}
	
	
}	
	
