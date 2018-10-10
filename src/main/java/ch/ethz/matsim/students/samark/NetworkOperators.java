package ch.ethz.matsim.students.samark;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class NetworkOperators {

	public NetworkOperators() {
	}

	
	public static Network networkOntoNetwork(Network ontopNetwork, Set<String> transportModes, Network baseNetwork, String fileName) {
		
		Metro_NetworkImpl.copyNetworkToNetwork(ontopNetwork, baseNetwork, transportModes);
		
		if (fileName != null) {
			NetworkWriter initialRoutesNetworkWriter = new NetworkWriter(baseNetwork);
			initialRoutesNetworkWriter.write(fileName);
		}
		
		return baseNetwork;
		
	}

	
	public static Network superimposeNetworks(Network network1, Set<String> transportModes1, Network network2, Set<String> transportModes2, String fileName) {
		
		// CAUTION: If same nodeIDs/linkIDs are featured in both, but you want to keep the att. of one network, than merge it first, set it as arg[0]
		
		Network mergedNetwork = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getNetwork();
		Metro_NetworkImpl.copyNetworkToNetwork(network1, mergedNetwork, transportModes1);
		Metro_NetworkImpl.copyNetworkToNetwork(network2, mergedNetwork, transportModes2);
		
		if (fileName != null) {
			NetworkWriter initialRoutesNetworkWriter = new NetworkWriter(mergedNetwork);
			initialRoutesNetworkWriter.write(fileName);
		}
		
		return mergedNetwork;
	}
	
	
	
}
