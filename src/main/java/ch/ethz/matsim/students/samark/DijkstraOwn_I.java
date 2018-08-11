package ch.ethz.matsim.students.samark;

import java.lang.reflect.Array;
import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

public class DijkstraOwn_I {


   // assumes Nodes are numbered 0, 1, ... n and that the source Node is 0
   public static ArrayList<Node> findShortestPath(Network network, Node startNode, Node endNode) {
       // Node[] nodes, Edge[] edges, Node target
	    int maxNodeNr = findHighestNode(network);
		int[] nodeLocation = new int[maxNodeNr+1];

		@SuppressWarnings("unchecked")
		Id<Node>[] nodeIDs = (Id<Node>[]) Array.newInstance(Id.createNodeId("").getClass(), network.getNodes().size());
		nodeIDs = network.getNodes().keySet().toArray(nodeIDs);
		Node[] nodes = (Node[]) Array.newInstance(network.getFactory().createNode(null, null).getClass(), network.getNodes().size());		
		int kNode = 0;
		for (Id<Node> ni : nodeIDs) {
			nodes[kNode] = network.getNodes().get(ni);
			// node_i is stored in nodes[] at location nodeLocation[node_i_IdNr] -> node2 is stored at nodeLocation[2]=(third(!) position) of array
			nodeLocation[Integer.parseInt(ni.toString())] = kNode;
			kNode++;
		}
		
		@SuppressWarnings("unchecked")
		Id<Link>[] linkIDs = (Id<Link>[]) Array.newInstance(Id.createLinkId("").getClass(), network.getLinks().size());
		linkIDs = network.getLinks().keySet().toArray(linkIDs);
		Link[] links = (Link[]) Array.newInstance(network.getFactory().createLink(Id.createLinkId("testLink"), startNode, endNode).getClass(), network.getLinks().size());
		int kLink = 0;
		for (Id<Link> li : linkIDs) {
			links[kLink] = network.getLinks().get(li);
			kLink++;
		}
	   
	   
	   double[][] Weight = initializeWeight(nodes, links, nodeLocation);
       double[] D = new double[nodes.length];
       Node[] P = new Node[nodes.length];
       ArrayList<Node> C = new ArrayList<Node>();

       // initialize:
       // (C)andidate set,
       // (D)yjkstra special path length, and
       // (P)revious Node along shortest path
       for(int i=0; i<nodes.length; i++){
           C.add(nodes[i]);
           D[i] = Weight[nodeLocation[Integer.parseInt(startNode.getId().toString())]][i];	// TODO Weight[start node][i]
           if(D[i] != Double.MAX_VALUE){
               P[i] = nodes[nodeLocation[Integer.parseInt(startNode.getId().toString())]]; // TODO nodes[start node]
           }
       }

       // crawl the graph
       for(int i=0; i<nodes.length-1; i++){
           // find the lightest Edge among the candidates
           double l = Double.MAX_VALUE;
           Node n = nodes[nodeLocation[Integer.parseInt(startNode.getId().toString())]];	// TODO nodes[start node]
           for(Node j : C){
               if(D[nodeLocation[Integer.parseInt(j.getId().toString())]] < l){		// TODO D[j]
                   n = j;
                   l = D[nodeLocation[Integer.parseInt(j.getId().toString())]];		// TODO D[j]
               }
           }
           C.remove(n);

           // see if any Edges from this Node yield a shorter path than from source->that Node
           for(int j=0; j<nodes.length-1; j++){
               if(D[nodeLocation[Integer.parseInt(n.getId().toString())]] != Double.MAX_VALUE && Weight[nodeLocation[Integer.parseInt(n.getId().toString())]][j] != Double.MAX_VALUE && D[nodeLocation[Integer.parseInt(n.getId().toString())]]+Weight[nodeLocation[Integer.parseInt(n.getId().toString())]][j] < D[j]){
                   // found one, update the path
                   D[j] = D[nodeLocation[Integer.parseInt(n.getId().toString())]] + Weight[nodeLocation[Integer.parseInt(n.getId().toString())]][j];
                   P[j] = n;
               }
           }
       }
       // we have our path. reuse C as the result list
       C.clear();
       int loc = nodeLocation[Integer.parseInt(endNode.getId().toString())];
       C.add(endNode);
       // backtrack from the target by P(revious), adding to the result list
       if (P[loc] != nodes[nodeLocation[Integer.parseInt(startNode.getId().toString())]]) {
	       while(P[loc] != nodes[nodeLocation[Integer.parseInt(startNode.getId().toString())]]){
	           if(P[loc] == null){
	               // looks like there's no path from source to target
	        	   System.out.println("Error: No path found between start and end node!");
	               return null;
	           }
	           C.add(0, P[loc]);
	           loc = nodeLocation[Integer.parseInt(P[loc].getId().toString())];
	       }
	       C.add(0, nodes[nodeLocation[Integer.parseInt(startNode.getId().toString())]]);
	       return C;
       }
       else {
    	   return null;
       }
   }
   
   
   private static double[][] initializeWeight(Node[] nodes, Link[] links, int[] nodeLocation){
       double[][] Weight = new double[nodes.length][nodes.length];
       for(int i=0; i<nodes.length; i++){
           Arrays.fill(Weight[i], Integer.MAX_VALUE);
       }
       for(Link l : links){
    	   Weight[nodeLocation[Integer.parseInt(l.getFromNode().getId().toString())]][nodeLocation[Integer.parseInt(l.getToNode().getId().toString())]] = l.getLength();
       }
       return Weight;
   }

   
// %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
   
// assumes Nodes are numbered 0, 1, ... n and that the source Node is 0
   public static ArrayList<Node> findShortestPathVirtualNetwork(Network network, Id<Node> startNode, Id<Node> endNode) {
       // Node[] nodes, Edge[] edges, Node target
	    // int maxNodeNr = findHighestNode(network);
		Map<Id<Node>,Integer> nodeLocationMap = new HashMap<Id<Node>,Integer>(network.getNodes().size());

		@SuppressWarnings("unchecked")
		Id<Node>[] nodeIDs = (Id<Node>[]) Array.newInstance(Id.createNodeId("").getClass(), network.getNodes().size());
		nodeIDs = network.getNodes().keySet().toArray(nodeIDs);
		Node[] nodes = (Node[]) Array.newInstance(network.getFactory().createNode(null, null).getClass(), network.getNodes().size());		
		int kNode = 0;
		for (Id<Node> ni : nodeIDs) {
			nodes[kNode] = network.getNodes().get(ni);
			// node_i is stored in nodes[] at location nodeLocation[node_i_IdNr] -> node2 is stored at nodeLocation[2]=(third(!) position) of array
			nodeLocationMap.put(ni, kNode);
			kNode++;
		}
		
		@SuppressWarnings("unchecked")
		Id<Link>[] linkIDs = (Id<Link>[]) Array.newInstance(Id.createLinkId("").getClass(), network.getLinks().size());
		linkIDs = network.getLinks().keySet().toArray(linkIDs);
		Link[] links = (Link[]) Array.newInstance(network.getFactory().createLink(Id.createLinkId("testLink"), network.getNodes().get(startNode), network.getNodes().get(endNode)).getClass(), network.getLinks().size());
		int kLink = 0;
		for (Id<Link> li : linkIDs) {
			links[kLink] = network.getLinks().get(li);
			kLink++;
		}
	   
	   
	   double[][] Weight = initializeWeightMap(nodes, links, nodeLocationMap);
       double[] D = new double[nodes.length];
       Node[] P = new Node[nodes.length];
       ArrayList<Node> C = new ArrayList<Node>();
       ArrayList<Id<Node>> arrayOut = new ArrayList<Id<Node>>();


       // initialize:
       // (C)andidate set,
       // (D)yjkstra special path length, and
       // (P)revious Node along shortest path
       for(int i=0; i<nodes.length; i++){
           C.add(nodes[i]);
           D[i] = Weight[nodeLocationMap.get(startNode)][i];	// TODO Weight[start node][i]
           if(D[i] != Double.MAX_VALUE){
               P[i] = nodes[nodeLocationMap.get(startNode)]; // TODO nodes[start node]
           }
       }

       // crawl the graph
       for(int i=0; i<nodes.length-1; i++){
           // find the lightest Edge among the candidates
           double l = Double.MAX_VALUE;
           Node n = nodes[nodeLocationMap.get(startNode)];	// TODO nodes[start node]
           for(Node j : C){
               if(D[nodeLocationMap.get(j.getId())] < l){		// TODO D[j]
                   n = j;
                   l = D[nodeLocationMap.get(j.getId())];		// TODO D[j]
               }
           }
           C.remove(n);

           // see if any Edges from this Node yield a shorter path than from source->that Node
           for(int j=0; j<nodes.length-1; j++){
               if(D[nodeLocationMap.get(n.getId())] != Double.MAX_VALUE && Weight[nodeLocationMap.get(n.getId())][j] != Double.MAX_VALUE && D[nodeLocationMap.get(n.getId())]+Weight[nodeLocationMap.get(n.getId())][j] < D[j]){
                   // found one, update the path
                   D[j] = D[nodeLocationMap.get(n.getId())] + Weight[nodeLocationMap.get(n.getId())][j];
                   P[j] = n;
               }
           }
       }
       // we have our path. reuse C as the result list
       C.clear();
       int loc = nodeLocationMap.get(endNode);
       C.add(network.getNodes().get(endNode));
       arrayOut.add(endNode);
       // backtrack from the target by P(revious), adding to the result list
       if (P[loc] != nodes[nodeLocationMap.get(startNode)]) {
	       while(P[loc] != nodes[nodeLocationMap.get(startNode)]){
	           if(P[loc] == null){
	               // looks like there's no path from source to target
	        	   System.out.println("Error: No path found between start and end node!");
	               return null;
	           }
	           C.add(0, P[loc]);
	           arrayOut.add(0, P[loc].getId());
	           loc = nodeLocationMap.get(P[loc].getId());
	       }
	       C.add(0, nodes[nodeLocationMap.get(startNode)]);
	       arrayOut.add(0, nodes[nodeLocationMap.get(startNode)].getId());
	       // return arrayOut;
	       return C;
       }
       else {
    	   return null;
       }
   }
   
   private static double[][] initializeWeightMap(Node[] nodes, Link[] links, Map<Id<Node>,Integer> nodeLocationMap){
       double[][] Weight = new double[nodes.length][nodes.length];
       for(int i=0; i<nodes.length; i++){
           Arrays.fill(Weight[i], Integer.MAX_VALUE);
       }
       for(Link l : links){
    	   Weight[nodeLocationMap.get(l.getFromNode().getId())][nodeLocationMap.get(l.getToNode().getId())] = l.getLength();
       }
       return Weight;
   }
   
   
// %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
   
   public static int findHighestNode(Network network) {
	   int max = 0;
	   for (Map.Entry<Id<Node>, ? extends Node> entry : network.getNodes().entrySet()) {
		   if(max<Integer.parseInt(entry.getKey().toString())) {
			   max = Integer.parseInt(entry.getKey().toString());
		   }
	   }
	   return max;
   }

   
	public static Link makeSureExists(Link feasibleLinkSearcher, Link lastLink, Network network) {
		Link outLink;
		int iter = 0;
		int tries = 100;
		do{
			outLink = feasibleLinkSearcher;
			iter++;
			if(iter==tries-1) {
				System.out.println("No shortest path found. Sorry! Aborting search.");
				return lastLink;
			}
		}while(DijkstraOwn_I.findShortestPath(network, lastLink.getToNode(), feasibleLinkSearcher.getFromNode()) == null && iter<tries);
		// System.out.println(DijkstraOwn_I.findShortestPath(network, lastLink.getToNode(), feasibleLinkSearcher.getFromNode()).toString());
		return outLink;
	}
   
}