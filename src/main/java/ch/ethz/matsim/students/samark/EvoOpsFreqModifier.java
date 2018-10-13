package ch.ethz.matsim.students.samark;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class EvoOpsFreqModifier {

	public EvoOpsFreqModifier() {
	}

	public static MNetworkPop applyFrequencyModification(MNetworkPop newPopulation, String eliteNetworkName) throws IOException {
		for(MNetwork mn : newPopulation.networkMap.values()) {
			if (mn.networkID.toString().equals(eliteNetworkName)) {
				continue;
			}
			boolean hasHadMutation = false;
			Map<String, Double> routePerformances = new HashMap<String, Double>();
			for (MRoute mr : mn.routeMap.values()) {
				routePerformances.put(mr.routeID, mr.utilityBalance);
			}
			List<String> routePerformanceOrder = NetworkEvolutionImpl.sortMapByValueScore(routePerformances);
			if (mn.routeMap.size()>3) {
				Random r1 = new Random();
				Random r2 = new Random();
				if (r1.nextDouble() < 0.67) {
					hasHadMutation = true;
					Log.write("Shifting one vehicle from weakest " + routePerformanceOrder.get(routePerformanceOrder.size()-1) + " to strongest " + routePerformanceOrder.get(0));
					mn.routeMap.get(routePerformanceOrder.get(0)).vehiclesNr++;
					mn.routeMap.get(routePerformanceOrder.get(routePerformanceOrder.size()-1)).vehiclesNr--;
					if (mn.routeMap.get(routePerformanceOrder.get(routePerformanceOrder.size()-1)).vehiclesNr < 1) {
						Log.write("Oops, " + routePerformanceOrder.get(routePerformanceOrder.size()-1) + " has died due to no more vehicles");
						mn.routeMap.remove(routePerformanceOrder.get(routePerformanceOrder.size()-1));
					}
				}
				if (r2.nextDouble() < 0.33) {
					hasHadMutation = true;
					Log.write("Shifting one vehicle from second weakest " + routePerformanceOrder.get(routePerformanceOrder.size()-1) + " to second strongest" + routePerformanceOrder.get(0));
					mn.routeMap.get(routePerformanceOrder.get(1)).vehiclesNr++;
					mn.routeMap.get(routePerformanceOrder.get(routePerformanceOrder.size()-2)).vehiclesNr--;
					if (mn.routeMap.get(routePerformanceOrder.get(routePerformanceOrder.size()-2)).vehiclesNr < 1) {
						Log.write("Oops, " + routePerformanceOrder.get(routePerformanceOrder.size()-2) + " has died due to no more vehicles");
						mn.routeMap.remove(routePerformanceOrder.get(routePerformanceOrder.size()-2));
					}
				}
			}
			else if ( 4 > mn.routeMap.size() && mn.routeMap.size() > 1 ) {
				Random r1 = new Random();
				if (r1.nextDouble() < 0.67) {
					hasHadMutation = true;
					// Log.write("Shifting one vehicle from weakest " + routePerformanceOrder.get(routePerformanceOrder.size()-1) + " to strongest " + routePerformanceOrder.get(0));
					mn.routeMap.get(routePerformanceOrder.get(0)).vehiclesNr++;
					mn.routeMap.get(routePerformanceOrder.get(routePerformanceOrder.size()-1)).vehiclesNr--;
					if (mn.routeMap.get(routePerformanceOrder.get(routePerformanceOrder.size()-1)).vehiclesNr < 1) {
						Log.write("   >> Oops, " + routePerformanceOrder.get(routePerformanceOrder.size()-1) + " has died due to no more vehicles");
						mn.routeMap.remove(routePerformanceOrder.get(routePerformanceOrder.size()-1));
					}
				}
			}
			if (hasHadMutation && newPopulation.modifiedNetworksInLastEvolution.contains(mn.networkID) == false) {
				newPopulation.modifiedNetworksInLastEvolution.add(mn.networkID);
			}
		}
		return newPopulation;
	}
	
}
