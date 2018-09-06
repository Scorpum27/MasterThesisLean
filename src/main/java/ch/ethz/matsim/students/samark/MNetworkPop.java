package ch.ethz.matsim.students.samark;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class MNetworkPop implements Serializable{

	private static final long serialVersionUID = 1L;
	
	String populationId;
	Map<String, MNetwork> networkMap;
	Map<String, String> mNetworkFileLocationMap;

	
	public MNetworkPop() {
		this.networkMap = new HashMap<String, MNetwork>();
	}
	
	public MNetworkPop(String id) {
		this.populationId = id;
		this.networkMap = new HashMap<String, MNetwork>();
	}
	
	public MNetworkPop(String id, int size) {
		this.populationId = id;
		this.networkMap = new HashMap<String, MNetwork>(size);
	}
	
	
	public Map<String, MNetwork> getNetworks(){
		return this.networkMap;
	}
	
	public void addNetwork(MNetwork newNetwork) {
		this.networkMap.put(newNetwork.networkID, newNetwork);
	}
	
	
}	
	
