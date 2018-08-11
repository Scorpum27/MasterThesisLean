package ch.ethz.matsim.students.samark;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Node;

public class GeomDistance {

	public static double calculate(Coord coord1, Coord coord2) {
		double distance = Math.sqrt((coord1.getX()-coord2.getX())*(coord1.getX()-coord2.getX())+(coord1.getY()-coord2.getY())*(coord1.getY()-coord2.getY()));
		return distance;
	}
	
	public static double betweenNodes(Node node1, Node node2) {
		Coord coord1 = node1.getCoord();
		Coord coord2 = node2.getCoord();
		double distance = Math.sqrt((coord1.getX()-coord2.getX())*(coord1.getX()-coord2.getX())+(coord1.getY()-coord2.getY())*(coord1.getY()-coord2.getY()));
		return distance;
	}
	
	public static Coord coordBetweenNodes(Node fromNode, Node toNode) {
		double x = 0.5*(fromNode.getCoord().getX()+toNode.getCoord().getX());
		double y = 0.5*(fromNode.getCoord().getY()+toNode.getCoord().getY());
		Coord betweenCoord = new Coord(x,y);
		return betweenCoord;
	}
	
}