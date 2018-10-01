package ch.ethz.matsim.students.samark;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
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

	public static double angleBetweenPoints(Coord c1, Coord c2, Coord c3) {
		double x1A = c1.getX();
		double y1A = c1.getY();
		double x2A = c2.getX();
		double y2A = c2.getY();
		double directionA = GeomDistance.absoluteAngle(x1A, x2A, y1A, y2A);
		double x1B = c2.getX();
		double y1B = c2.getY();
		double x2B = c3.getX();
		double y2B = c3.getY();
		double directionB = GeomDistance.absoluteAngle(x1B, x2B, y1B, y2B);
		double intersectingAngle = Math.abs(directionB-directionA);
		if (intersectingAngle > 180.0) {
			intersectingAngle = 360.0-intersectingAngle;
		}
		return intersectingAngle;
	}
	
	public static double angleBetweenLinks(Link linkA, Link linkB) {
		double x1A = linkA.getFromNode().getCoord().getX();
		double y1A = linkA.getFromNode().getCoord().getY();
		double x2A = linkA.getToNode().getCoord().getX();
		double y2A = linkA.getToNode().getCoord().getY();
		double directionA = GeomDistance.absoluteAngle(x1A, x2A, y1A, y2A);
		double x1B = linkB.getFromNode().getCoord().getX();
		double y1B = linkB.getFromNode().getCoord().getY();
		double x2B = linkB.getToNode().getCoord().getX();
		double y2B = linkB.getToNode().getCoord().getY();
		double directionB = GeomDistance.absoluteAngle(x1B, x2B, y1B, y2B);
		double intersectingAngle = Math.abs(directionB-directionA);
		if (intersectingAngle > 180.0) {
			intersectingAngle = 360.0-intersectingAngle;
		}
		return intersectingAngle;
	}
	
	public static double absoluteAngle(double x1, double x2, double y1, double y2) {
		double absAngle = 0.0;
		if (x2>=x1 && y2>=y1) {
			absAngle = Math.atan((y2-y1)/(x2-x1))*180.0/Math.PI;
		}
		if (x2<x1 && y2>=y1) {
			absAngle = 180.0+Math.atan((y2-y1)/(x2-x1))*180.0/Math.PI;
		}
		if (x2<x1 && y2<y1) {
			absAngle = 180.0+Math.atan((y2-y1)/(x2-x1))*180.0/Math.PI;
		}
		if (x2>=x1 && y2<y1) {
			absAngle = Math.atan((y2-y1)/(x2-x1))*180.0/Math.PI;
		}
		return absAngle;
	}
	
	public static double angleBetweenPoints(double x1A, double y1A, double x2A, double y2A, double x1B, double y1B, double x2B, double y2B) {
		double directionA = GeomDistance.absoluteAngle(x1A, x2A, y1A, y2A);
		double directionB = GeomDistance.absoluteAngle(x1B, x2B, y1B, y2B);
		double intersectingAngle = Math.abs(directionB-directionA);
		if (intersectingAngle > 180.0) {
			intersectingAngle = 360.0-intersectingAngle;
		}
		return intersectingAngle;
	}
	
	
	
}