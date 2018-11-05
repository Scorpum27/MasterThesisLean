package ch.ethz.matsim.students.samark;

import org.matsim.api.core.v01.Coord;

public class ODRoutePairX {

	Coord O;
	Coord D;
	String odPairNames;
	
	public ODRoutePairX() {
	}
	
	public ODRoutePairX(Coord Origin, Coord Destination) {
		this.O = Origin;
		this.D = Destination;
	}
	
	public ODRoutePairX(Coord Origin, Coord Destination, String names) {
		this.O = Origin;
		this.D = Destination;
		this.odPairNames = names;
	}
	
}
