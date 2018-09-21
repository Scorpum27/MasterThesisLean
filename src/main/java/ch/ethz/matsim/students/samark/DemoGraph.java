package ch.ethz.matsim.students.samark;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

public class DemoGraph {

	public Map<Id<Node>, DemoVertex> verticesMap;

	public DemoGraph(Network network) throws IOException {
		this.verticesMap = new HashMap<Id<Node>, DemoVertex>();
		
		for (Link link : network.getLinks().values()) {
			DemoVertex toVertex;
			if (verticesMap.containsKey(link.getToNode().getId())==false) {
				toVertex = new DemoVertex(link.getToNode().getId().toString());
				verticesMap.put(link.getToNode().getId(), toVertex);
				Log.write("Adding to network node = "+link.getToNode().getId().toString());
			}
			else {
				toVertex = verticesMap.get(link.getToNode().getId());
			}
			DemoEdge edge = new DemoEdge(toVertex, link.getLength());
			
			DemoVertex fromVertex;
			
			if (verticesMap.containsKey(link.getFromNode().getId())==false) {
				fromVertex = new DemoVertex(link.getFromNode().getId().toString());
				verticesMap.put(link.getFromNode().getId(), fromVertex);
				Log.write("Adding to network node = "+link.getFromNode().getId().toString());

			}
			else {
				fromVertex = verticesMap.get(link.getFromNode().getId());
			}			
			fromVertex.neighbours.add(edge);
		}
	}

	public Map<Id<Node>, DemoVertex> getVerticesMap() {
		return this.verticesMap;
	}
}