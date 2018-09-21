package ch.ethz.matsim.students.samark;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

public class DemoDijkstra {

	public static List<Node> calculateShortestPath(Network networkIn, Id<Node> startNode, Id<Node> endNode) throws IOException {

		DemoGraph g = new DemoGraph(networkIn);
		DemoVertex origin = g.getVerticesMap().get(startNode);
		DemoVertex destination = g.getVerticesMap().get(endNode);
		Log.write("Origin node is: "+startNode.toString());
		Log.write("Destination node is: "+endNode.toString());
		if (origin == null) {
			Log.write("Origin node is null (check if metroNetwork contains node): "+startNode.toString());
			return null;
		}
		if (destination == null) {
			Log.write("Destination node is null (check if metroNetwork contains node): "+endNode.toString());
			return null;
		}
		calculate(origin);
		List<DemoVertex> path = destination.path;
		List<Id<Node>> nodePathId = new ArrayList<Id<Node>>();
		List<Node> nodePath = new ArrayList<Node>();
		for (DemoVertex v : path) {
			nodePathId.add(Id.createNodeId(v.name));
			nodePath.add(networkIn.getNodes().get(Id.createNodeId(v.name)));
		}
		Log.write("ShortestPath NodeList = "+nodePathId.toString());
		return nodePath;
	}
	
	
	/*public static void main(String[] arg) throws IOException{
		
		Network network = ScenarioUtils.loadScenario(ConfigUtils.createConfig()).getNetwork();
		NetworkFactory nf = network.getFactory();
		Node thisnode;
		Node lastnode = null;
		Link link;
		Link reverseLink;
		
		for (int n=0; n<25; n++) {
			thisnode = nf.createNode(Id.createNodeId(n), new Coord(1.0*(n%5), (double) Math.floor(n/5.0)));
			network.addNode(thisnode);
			if(n!=0) {
				link = nf.createLink(Id.createLinkId(lastnode.getId().toString()+"_"+thisnode.getId().toString()), lastnode, thisnode);
				reverseLink = nf.createLink(Id.createLinkId(thisnode.getId().toString()+"_"+lastnode.getId().toString()), thisnode, lastnode);
				network.addLink(link);
				network.addLink(reverseLink);
			}
			lastnode = thisnode;
		}
		
		// Create a new graph from network
		DemoGraph g = new DemoGraph(network);
		// Calculate Dijkstra.
		DemoVertex origin = g.getVerticesMap().get(Id.createNodeId("0"));
		DemoVertex destination = g.getVerticesMap().get(Id.createNodeId("23"));
		DemoDijkstra.calculate(origin);
		List<DemoVertex> path = destination.path;
		List<Id<Node>> nodePath = new ArrayList<Id<Node>>();
		for (DemoVertex v : path) {
			nodePath.add(Id.createNodeId(v.name));
		}
		System.out.println(nodePath);
		
		// Print the minimum Distance.
		for(DemoVertex v : g.getVerticesMap().values()){
			System.out.print("Vertex - "+v.name+" , Dist - "+ v.minDistance+" , Path - ");
			for(DemoVertex pathvert : v.path) {
				System.out.print(pathvert.name+" ");
			}
			System.out.println(""+v);
		}
	
		DemoDijkstra.calculateShortestPath(network, Id.createNodeId("0"), Id.createNodeId("23"));

	}*/

	public static void calculate(DemoVertex source){
		// Algo:
		// 1. Take the unvisited node with minimum weight.
		// 2. Visit all its neighbours.
		// 3. Update the distances for all the neighbours (In the Priority Queue).
		// Repeat the process till all the connected nodes are visited.
		
		source.minDistance = 0;
		PriorityQueue<DemoVertex> queue = new PriorityQueue<DemoVertex>();
		queue.add(source);
		
		while(!queue.isEmpty()){
			
			DemoVertex u = queue.poll();
		
			for(DemoEdge neighbour:u.neighbours){
				Double newDist = u.minDistance+neighbour.weight;
				
				if(neighbour.target.minDistance>newDist){
					// Remove the node from the queue to update the distance value.
					queue.remove(neighbour.target);
					neighbour.target.minDistance = newDist;
					
					// Take the path visited till now and add the new node.s
					neighbour.target.path = new LinkedList<DemoVertex>(u.path);
					neighbour.target.path.add(u);
					
					//Reenter the node with new distance.
					queue.add(neighbour.target);					
				}
			}
		}
	}
	
}