package ch.ethz.matsim.students.samark;

import java.util.HashMap;
import java.util.Map;

public class MNetworkPop{

	public MNetworkPop() {
		this.networkMap = new HashMap<String, MNetwork>();
	}

	Map<String, MNetwork> networkMap;
	
	public Map<String, MNetwork> getNetworks(){
		return this.networkMap;
	}
	
	public void addNetwork(MNetwork newNetwork) {
		this.networkMap.put(newNetwork.networkID, newNetwork);
	}

	
}
