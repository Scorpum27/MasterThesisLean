package ch.ethz.matsim.students.samark;

import java.util.ArrayList;
import java.util.LinkedList;

public class DemoVertex implements Comparable<DemoVertex>{

	public final String name;
	public ArrayList<DemoEdge> neighbours;
	public LinkedList<DemoVertex> path;
	public double minDistance = Double.POSITIVE_INFINITY;
	public DemoVertex previous;

	@Override
	public int compareTo(DemoVertex other) {
		return Double.compare(minDistance, other.minDistance);
	}

	public DemoVertex(String name){
		this.name = name;
		neighbours = new ArrayList<DemoEdge>();
		path = new LinkedList<DemoVertex>();
	}

	public String toString() {
		return name;
	}


}