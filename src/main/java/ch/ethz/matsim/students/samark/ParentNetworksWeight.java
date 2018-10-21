package ch.ethz.matsim.students.samark;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ParentNetworksWeight {

	public ParentNetworksWeight() {
		child1.put(1, 0.0);
		child1.put(2, 0.0);
		child2.put(1, 0.0);
		child2.put(2, 0.0);
	}
	
	Map<Integer, Double> child1 = new HashMap<Integer, Double>();
	Map<Integer, Double> child2 = new HashMap<Integer, Double>();

	public String getDominantParentOfChild1(String parent1, String parent2) throws IOException {
		if (!child1.containsKey(1) && !child1.containsKey(2)) {
			Log.write("CAUTION, both ParentContributions not available. Random guessing dominantNetwork!");
			return parent2;	// can also use parent1 (parent1/parent2 assignment at beginning of Crossover operation is random anyways.)
		}
		else if (!child1.containsKey(1)) {
			Log.write("CAUTION, ParentContributions of parent1 not available. Setting parent2 as dominantNetwork!");
			return parent2;
		}
		else if (!child1.containsKey(2)) {
			Log.write("CAUTION, ParentContributions of parent2 not available. Setting parent1 as dominantNetwork!");
			return parent1;
		}
		else if (child1.get(1) > child1.get(2)) {
			return parent1;
		}
		else {
			return parent2;
		}
	}
	
	public String getDominantParentOfChild2(String parent1, String parent2) throws IOException {
		if (!child2.containsKey(1) && !child2.containsKey(2)) {
			Log.write("CAUTION, both ParentContributions not available. Random guessing dominantNetwork!");
			return parent2;	// can also use parent1 (parent1/parent2 assignment at beginning of Crossover operation is random anyways.)
		}
		else if (!child2.containsKey(1)) {
			Log.write("CAUTION, ParentContributions of parent1 not available. Setting parent2 as dominantNetwork!");
			return parent2;
		}
		else if (!child2.containsKey(2)) {
			Log.write("CAUTION, ParentContributions of parent2 not available. Setting parent1 as dominantNetwork!");
			return parent1;
		}
		else if (child2.get(1) > child2.get(2)) {
			return parent1;
		}
		else {
			return parent2;
		}
	}
	
}
