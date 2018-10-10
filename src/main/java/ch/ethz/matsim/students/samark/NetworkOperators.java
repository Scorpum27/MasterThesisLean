package ch.ethz.matsim.students.samark;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;

public class NetworkOperators {

	public NetworkOperators() {
	}

	
	public static Network copyNetworkToNetwork(Network fromNetwork, Network toNetwork, Set<String> transportModes) {
		NetworkFactory networkFactory = toNetwork.getFactory();
		// connectingLinkToToNode.setAllowedModes(transportModes);
		for (Link link : fromNetwork.getLinks().values()) {
			Node tempFromNode = networkFactory.createNode(Id.createNodeId(link.getFromNode().getId()), link.getFromNode().getCoord());
			Node tempToNode = networkFactory.createNode(Id.createNodeId(link.getToNode().getId()), link.getToNode().getCoord());
			Link tempLink = networkFactory.createLink(Id.createLinkId(link.getId()), tempFromNode, tempToNode);
			

			if (! toNetwork.getNodes().keySet().contains(tempFromNode.getId())) {
				toNetwork.addNode(tempFromNode);
			}
			if (! toNetwork.getNodes().keySet().contains(tempToNode.getId())) {
				toNetwork.addNode(tempToNode);
			}
			if (! toNetwork.getLinks().keySet().contains(tempLink.getId())) {
				if (transportModes == null) {
					tempLink.setAllowedModes(link.getAllowedModes());				
				}
				else {
					tempLink.setAllowedModes(transportModes);
				}
				tempLink.setCapacity(link.getCapacity());
				tempLink.setFreespeed(link.getFreespeed());
				tempLink.setNumberOfLanes(link.getNumberOfLanes());
				tempLink.setLength(link.getLength());
				toNetwork.addLink(tempLink);
			}
		}
		
		return toNetwork;
	}
	
	public static Network networkIntoNetwork(Network ontopNetwork, Set<String> transportModes, Network baseNetwork, String fileName) {
		
		NetworkOperators.copyNetworkToNetwork(ontopNetwork, baseNetwork, transportModes);
		
		if (fileName != null) {
			NetworkWriter initialRoutesNetworkWriter = new NetworkWriter(baseNetwork);
			initialRoutesNetworkWriter.write(fileName);
		}
		
		return baseNetwork;
		
	}

	
	public static Network superimpose2separateNetwork(Network network1, Set<String> transportModes1, Network network2, Set<String> transportModes2, String fileName) {
		
		// CAUTION: If same nodeIDs/linkIDs are featured in both, but you want to keep the att. of one network, than merge it first, set it as arg[0]
		
		Network mergedNetwork = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getNetwork();
		NetworkOperators.copyNetworkToNetwork(network1, mergedNetwork, transportModes1);
		NetworkOperators.copyNetworkToNetwork(network2, mergedNetwork, transportModes2);
		
		if (fileName != null) {
			NetworkWriter initialRoutesNetworkWriter = new NetworkWriter(mergedNetwork);
			initialRoutesNetworkWriter.write(fileName);
		}
		
		return mergedNetwork;
	}

	public static Network networkRoutesToNetwork(ArrayList<NetworkRoute> networkRoutes, Network network, Set<String> networkRouteModes, String fileName) {
		// Store all new networkRoutes in a separate network file for visualization
			Network routesNetwork = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getNetwork();
			NetworkFactory networkFactory = routesNetwork.getFactory();
			for (NetworkRoute nR : networkRoutes) {
				List<Id<Link>> routeLinkList = new ArrayList<Id<Link>>();
				routeLinkList.add(nR.getStartLinkId());
				routeLinkList.addAll(nR.getLinkIds());
				routeLinkList.add(nR.getEndLinkId());
				for (Id<Link> linkID : routeLinkList) {
					Node tempToNode = networkFactory.createNode(network.getLinks().get(linkID).getToNode().getId(),
							network.getLinks().get(linkID).getToNode().getCoord());
					Node tempFromNode = networkFactory.createNode( network.getLinks().get(linkID).getFromNode().getId(),
							network.getLinks().get(linkID).getFromNode().getCoord());
					Link tempLink = networkFactory.createLink(network.getLinks().get(linkID).getId(), tempFromNode, tempToNode);
					tempLink.setAllowedModes(networkRouteModes);
					if (routesNetwork.getNodes().containsKey(tempToNode.getId()) == false) {
						routesNetwork.addNode(tempToNode);
					}
					if (routesNetwork.getNodes().containsKey(tempFromNode.getId()) == false) {
						routesNetwork.addNode(tempFromNode);
					}
					if (routesNetwork.getLinks().containsKey(tempLink.getId()) == false) {
						routesNetwork.addLink(tempLink);
					}
				}
			}
		NetworkWriter initialRoutesNetworkWriter = new NetworkWriter(routesNetwork);
		initialRoutesNetworkWriter.write(fileName);
		
		return routesNetwork;
	}
	
	public static Network NetworkRouteToNetwork(NetworkRoute networkRoute, Network metroNetwork, Set<String> networkRouteModes, String fileName) {
		// Store all new networkRoutes in a separate network file for visualization
			Network routesNetwork = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getNetwork();
			NetworkFactory networkFactory = routesNetwork.getFactory();
				List<Id<Link>> routeLinkList = new ArrayList<Id<Link>>();
				routeLinkList.add(networkRoute.getStartLinkId());
				routeLinkList.addAll(networkRoute.getLinkIds());
				routeLinkList.add(networkRoute.getEndLinkId());
				for (Id<Link> linkID : routeLinkList) {
					Node tempToNode = networkFactory.createNode(metroNetwork.getLinks().get(linkID).getToNode().getId(),
							metroNetwork.getLinks().get(linkID).getToNode().getCoord());
					Node tempFromNode = networkFactory.createNode( metroNetwork.getLinks().get(linkID).getFromNode().getId(),
							metroNetwork.getLinks().get(linkID).getFromNode().getCoord());
					Link tempLink = networkFactory.createLink(metroNetwork.getLinks().get(linkID).getId(), tempFromNode, tempToNode);
					tempLink.setAllowedModes(networkRouteModes);
					if (routesNetwork.getNodes().containsKey(tempToNode.getId()) == false) {
						routesNetwork.addNode(tempToNode);
					}
					if (routesNetwork.getNodes().containsKey(tempFromNode.getId()) == false) {
						routesNetwork.addNode(tempFromNode);
					}
					if (routesNetwork.getLinks().containsKey(tempLink.getId()) == false) {
						routesNetwork.addLink(tempLink);
					}
				}
		NetworkWriter initialRoutesNetworkWriter = new NetworkWriter(routesNetwork);
		initialRoutesNetworkWriter.write(fileName);
		return routesNetwork;
	}
	
	
//	DEPRECEATED:
	
//	public static Network mergeNetworks(Network Network1, Network Network2, Set<String> transportModes) {	
//		
//		Network outNetwork = ScenarioUtils.loadScenario(ConfigUtils.createConfig()).getNetwork();
//		NetworkFactory networkFactory = outNetwork.getFactory();
// 		for (Link link : Network1.getLinks().values()) {
//			Node tempFromNode = networkFactory.createNode(Id.createNodeId(link.getFromNode().getId()), link.getFromNode().getCoord());
//			Node tempToNode = networkFactory.createNode(Id.createNodeId(link.getToNode().getId()), link.getToNode().getCoord());
//			Link tempLink = networkFactory.createLink(Id.createLinkId(link.getId()), tempFromNode, tempToNode);
//			if (transportModes == null) {
//				tempLink.setAllowedModes(link.getAllowedModes());				
//			}
//			else {
//				tempLink.setAllowedModes(transportModes);
//			}
//			if (outNetwork.getNodes().keySet().contains(tempFromNode.getId())==false) {
//				outNetwork.addNode(tempFromNode);
//			}
//			if (outNetwork.getNodes().keySet().contains(tempToNode.getId())==false) {
//				outNetwork.addNode(tempToNode);
//			}
//			if (outNetwork.getLinks().keySet().contains(tempLink.getId())==false) {
//				outNetwork.addLink(tempLink);
//			}
//		}
//		
//		for (Link link : Network2.getLinks().values()) {
//			Node tempFromNode = networkFactory.createNode(Id.createNodeId(link.getFromNode().getId()), link.getFromNode().getCoord());
//			Node tempToNode = networkFactory.createNode(Id.createNodeId(link.getToNode().getId()), link.getToNode().getCoord());
//			Link tempLink = networkFactory.createLink(Id.createLinkId(link.getId()), tempFromNode, tempToNode);
//			if (transportModes == null) {
//				tempLink.setAllowedModes(link.getAllowedModes());				
//			}
//			else {
//				tempLink.setAllowedModes(transportModes);
//			}
//			if (outNetwork.getNodes().keySet().contains(tempFromNode.getId())==false) {
//				outNetwork.addNode(tempFromNode);
//			}
//			if (outNetwork.getNodes().keySet().contains(tempToNode.getId())==false) {
//				outNetwork.addNode(tempToNode);
//			}
//			if (outNetwork.getLinks().keySet().contains(tempLink.getId())==false) {
//				outNetwork.addLink(tempLink);
//			}
//		}
//		
//		return outNetwork;
//	}
	
}
