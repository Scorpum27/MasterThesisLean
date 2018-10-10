package ch.ethz.matsim.students.samark;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.network.Network;

public class EvoOpsCrossover {

	public EvoOpsCrossover() {
	}

	public static MNetworkPop applyCrossovers(Network globalNetwork, Map<String, NetworkScoreLog> networkScoreMap,
			MNetworkPop newPopulation, String populationName, 
			MNetwork eliteMNetwork, double alpha, double pCrossOver, String crossoverRouletteStrategy, boolean useOdPairsForInitialRoutes, 
			String vehicleTypeName, double vehicleLength, double maxVelocity, int vehicleSeats,
			int vehicleStandingRoom, String defaultPtMode, double stopTime, boolean blocksLane, boolean logEntireRoutes,
			double minCrossingDistanceFactorFromRouteEnd, double maxCrossingAngle) throws IOException {
		int nOldPop = newPopulation.networkMap.size();
		Log.writeEvo("START CROSS-OVER");
		if (nOldPop<2) {
			Log.write("Not enough network parents for crossover. Terminating...");
			Log.writeEvo("Not enough network parents for crossover. Terminating...");
			System.exit(0);
		}
		int nCrossOverCandidates = (int) Math.ceil(0.5*nOldPop);
		List<MNetwork> newOffspring = new ArrayList<MNetwork>();
		System.out.println("We will try nCrossOverCandidates="+nCrossOverCandidates);
		
		List<String> processedNetworks = new ArrayList<String>();
		Map<Integer, List<String>> executedMergers = new HashMap<Integer, List<String>>();
		CrossOverLoop:
		for (int n=0; n<nCrossOverCandidates; n++) {
			int nTries = 0;
			Random r = new Random();
			if (r.nextDouble()<pCrossOver) {
				String nameParent1;
				String nameParent2;
				do {
					nameParent1 = NetworkEvolutionImpl.selectMNetworkByRoulette(alpha, networkScoreMap, crossoverRouletteStrategy);
					System.out.println("ParentName 1="+nameParent1);
					do{
						nameParent2 = NetworkEvolutionImpl.selectMNetworkByRoulette(alpha, networkScoreMap, crossoverRouletteStrategy);
						System.out.println("ParentName 2="+nameParent2);
						nTries ++;
						if (nTries > 2000) {
							continue CrossOverLoop;
						}
					}while(nameParent1.equals(nameParent2));
				}while(NetworkEvolutionImpl.mergerHasBeenExecutedPreviously(executedMergers, nameParent1, nameParent2));
				executedMergers.put(n, Arrays.asList(nameParent1, nameParent2));
				Log.writeAndDisplay("  > Crossing:  " + nameParent1 + " X " + nameParent2);
				Log.writeEvo(" > Crossing Parents:  " + nameParent1 + " X " + nameParent2);
				MNetwork parentMNetwork1 = Clone.mNetwork(newPopulation.getNetworks().get(nameParent1));
				MNetwork parentMNetwork2 = Clone.mNetwork(newPopulation.getNetworks().get(nameParent2));
				MNetwork[] childrenMNetworks = NetworkEvolutionImpl.crossMNetworks(globalNetwork, parentMNetwork1, parentMNetwork2,
						vehicleTypeName, vehicleLength, maxVelocity, vehicleSeats, vehicleStandingRoom, defaultPtMode,
						stopTime, blocksLane,
						useOdPairsForInitialRoutes, minCrossingDistanceFactorFromRouteEnd, maxCrossingAngle);
				childrenMNetworks[0].setParents(nameParent1, nameParent2);
				newOffspring.add(childrenMNetworks[0]);
				newOffspring.add(childrenMNetworks[1]);
			}
		}
		int nNewOffspring = newOffspring.size();
		System.out.println("nNewOffspring="+nNewOffspring);
		if(nNewOffspring != 0) {
			List<String> deletedNetworkNames = RemoveWeakestNetworks(newPopulation, nNewOffspring);
			processedNetworks.addAll(deletedNetworkNames);
			Log.write("  >> Replace weakest networks: " + deletedNetworkNames.toString() + " by "+nNewOffspring+" nNewOffspring");
			Log.writeEvo(" > Replacing weakest parents: " + deletedNetworkNames.toString());
			for (int i=0; i<newOffspring.size(); i++) {
				RenameOffspring(deletedNetworkNames.get(i), newOffspring.get(i));	// renaming offspring with its MNetworkId and the Id of all its MRoutes
				newPopulation.addNetwork(newOffspring.get(i));
				Log.writeEvo(" >> New offspring network: " + newOffspring.get(i).networkID + "   parents=["+newOffspring.get(i).parents.get(0)+" / "+newOffspring.get(i).parents.get(1)+"]");
				//Log.write("   >>> Putting New Offspring Network = " + newOffspring.get(i).networkID);
			}
		}
		if (nNewOffspring == nOldPop) {										// check with this condition if all old networks have been deleted for new offspring
			newPopulation.addNetwork(eliteMNetwork);						// if also elite network has been deleted, add manually again (it will replace the new one with the same name)
			processedNetworks.remove(eliteMNetwork.networkID);							// because this network remains unchanged for this generation as if it were not processed
			Log.write("   >>> Putting back removed ELITE NETWORK = " + eliteMNetwork.networkID);
			Log.writeEvo(" >> Putting back removed ELITE NETWORK = " + eliteMNetwork.networkID);
		}
		Log.writeEvo(" >> Networks without crossover modifications: ");
		for (String networkName : newPopulation.networkMap.keySet()) {
			if (processedNetworks.contains(networkName)==false) {
				Log.writeEvo("    > "+networkName + 
					"   parents=["+newPopulation.networkMap.get(networkName).parents.get(0)+" / "+newPopulation.networkMap.get(networkName).parents.get(1)+"]"  );
			}
		}
		
		if (logEntireRoutes) {
			for (MNetwork mn : newPopulation.networkMap.values()) {
				for (String mString : mn.routeMap.keySet()) {
					MRoute mr = mn.routeMap.get(mString);
					Log.writeAndDisplay(
							"   >>> " + mString + " = " + mr.linkList.subList(0, mr.linkList.size() / 2).toString());
				}
			}
		}
		for(String networkName : processedNetworks) {
			if (newPopulation.modifiedNetworksInLastEvolution.contains(networkName)==false) {
				newPopulation.modifiedNetworksInLastEvolution.add(networkName);
			}
		}
		return newPopulation;
	}
	
	
	public static void RenameOffspring(String newNetworkName, MNetwork mNetwork) {
		mNetwork.networkID = newNetworkName;
		//thisNewNetworkName+"_Route"+lineNr
		Map<String, MRoute> newRoutesMap = new HashMap<String, MRoute>();
		int counter = 1;
		for (MRoute mRoute : mNetwork.routeMap.values()) {
			MRoute mrTemp = Clone.mRoute(mRoute);
			mrTemp.routeID = newNetworkName+"_Route"+counter;
			newRoutesMap.put(mrTemp.routeID, mrTemp);
			counter++;
		}
		mNetwork.routeMap = newRoutesMap;
	}

	public static List<String> RemoveWeakestNetworks(MNetworkPop newPopulation, int nDelete) {
		List<String> deletedNetworks = new ArrayList<String>();
		for (int n=0; n<nDelete; n++) {
			String weakestNetworkName = "";
			Double weakestScore = Double.MAX_VALUE;
			for (String networkName : newPopulation.networkMap.keySet()) {
				if (newPopulation.networkMap.get(networkName).overallScore < weakestScore) {
					weakestNetworkName = networkName;
					weakestScore = newPopulation.networkMap.get(networkName).overallScore;
				}
			}
			deletedNetworks.add(weakestNetworkName);
			newPopulation.networkMap.remove(weakestNetworkName);
		}		
		return deletedNetworks;
	}
	
}
