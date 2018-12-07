package ch.ethz.matsim.students.samark.visualizer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import ch.ethz.matsim.students.samark.*;

public class VisualizerScoresEvolution {

	public static void main(String[] args) throws IOException {

		String simPath = args[0];
		int nNetworks = Integer.parseInt(args[1]);
		int finalGen = Integer.parseInt(args[2]);
		
		CBPII refCBP = null;
		String refCbpPath = simPath+"/zurich_1pm/cbpParametersOriginal/cbpParametersOriginalGlobal.xml";
		if (new File(refCbpPath).exists()) { refCBP =  XMLOps.readFromFile(CBPII.class, refCbpPath);}
		else {Log.write("No reference cbp file found. Aborting..."); System.exit(0);}
		
		Map<Integer, Double> bestScore = new HashMap<Integer, Double>();
		Map<Integer, Double> averageScore = new HashMap<Integer, Double>();
		Map<Integer, Double> bestAveragePtTime = new HashMap<Integer, Double>();
		Map<Integer, Double> bestAveragePtDisutilityEquivalentTime = new HashMap<Integer, Double>();
		Map<Integer, Double> bestAverageCarTime = new HashMap<Integer, Double>();
		Map<Integer, Double> bestAverageOtherTime = new HashMap<Integer, Double>();
		Map<Integer, Double> bestAverageDeltaPtTime = new HashMap<Integer, Double>();
		Map<Integer, Double> bestAverageDeltaCarTime = new HashMap<Integer, Double>();
		Map<Integer, Double> bestAverageDeltaOtherTime = new HashMap<Integer, Double>();
		Map<Integer, Double> bestDeltaPtUsers = new HashMap<Integer, Double>();
		Map<Integer, Double> bestDeltaCarUsers = new HashMap<Integer, Double>();
		Map<Integer, Double> bestDeltaOtherUsers = new HashMap<Integer, Double>();
		Map<Integer, Double> bestMetroPersonDist = new HashMap<Integer, Double>();
		Map<Integer, Double> bestTotalBenefit = new HashMap<Integer, Double>();
		Map<Integer, Double> bestTotalCost = new HashMap<Integer, Double>();
		Map<Integer, Double> bestTravelTimeGains = new HashMap<Integer, Double>();
		Map<Integer, Double> bestTravelTimeGainsPt = new HashMap<Integer, Double>();
		Map<Integer, Double> bestTravelTimeGainsCar = new HashMap<Integer, Double>();
		Map<Integer, Double> bestTravelTimeGainsOther = new HashMap<Integer, Double>();
		
		for (int gen=1; gen<=finalGen; gen++) {
			CBPII bestCBP = new CBPII();
			bestCBP.totalAnnualCost = 1.0E12;
			Boolean betterValueFound = false;
			double averageNetworkScore = 0.0;
			for (int n=1; n<=nNetworks; n++) {
				String cbpPath = simPath+"/zurich_1pm/Evolution/Population/HistoryLog/Generation"+gen+"/Network"+n+"/cbpAveraged.xml";
				if (new File(cbpPath).exists()) {
					CBPII thisCBP = XMLOps.readFromFile(CBPII.class, cbpPath);
					averageNetworkScore += (thisCBP.totalAnnualBenefit-thisCBP.totalAnnualCost)/nNetworks;
					if (thisCBP.totalAnnualBenefit-thisCBP.totalAnnualCost > bestCBP.totalAnnualBenefit-bestCBP.totalAnnualCost) {
						bestCBP = thisCBP;
						betterValueFound = true;
					}					
				}
				else {
					continue;
				}
			}
			if (betterValueFound) {
				
				bestScore.put(gen, bestCBP.totalAnnualBenefit-bestCBP.totalAnnualCost);
				averageScore.put(gen, averageNetworkScore);
				bestTotalBenefit.put(gen, bestCBP.totalAnnualBenefit);
				bestTotalCost.put(gen, bestCBP.totalAnnualCost);
				
				bestAveragePtDisutilityEquivalentTime.put(gen, (bestCBP.customVariable3/bestCBP.ptUsers)/60.0);
				bestAveragePtTime.put(gen, bestCBP.averagePtTime/60.0);
				bestAverageCarTime.put(gen, bestCBP.averageCartime/60.0);
				bestAverageOtherTime.put(gen, (bestCBP.customVariable1/bestCBP.otherUsers)/60.0);
				bestAverageDeltaPtTime.put(gen, (bestCBP.averagePtTime-refCBP.averagePtTime)/60.0);
				bestAverageDeltaCarTime.put(gen, (bestCBP.averageCartime-refCBP.averageCartime)/60.0);
				bestAverageDeltaOtherTime.put(gen, (bestCBP.customVariable1/bestCBP.otherUsers-refCBP.customVariable1/refCBP.otherUsers)/60.0);
				bestDeltaPtUsers.put(gen, bestCBP.ptUsers-refCBP.ptUsers);
				bestDeltaCarUsers.put(gen, bestCBP.carUsers-refCBP.carUsers);
				bestDeltaOtherUsers.put(gen, bestCBP.otherUsers-refCBP.otherUsers);
				bestMetroPersonDist.put(gen, bestCBP.metroPersonDist);
				bestTravelTimeGains.put(gen, bestCBP.travelTimeGains);
				bestTravelTimeGainsPt.put(gen, bestCBP.travelTimeGainsPt);
				bestTravelTimeGainsCar.put(gen, bestCBP.travelTimeGainsCar);
				bestTravelTimeGainsOther.put(gen, bestCBP.customVariable2);
			}
			else {
				continue;
			}
		}
		
		Visualizer.plot2D(" Total Welfare \r\n ", "", "Generation", "Annual Welfare [CHF p.a.]",
				Arrays.asList(bestScore, averageScore),
				Arrays.asList("Elite Network", "Average"), 0.0, 0.0, null, // new Range(0.0E8, 4.5E8)
				simPath+"/x1_welfare.png");

		Visualizer.plot2D(" Elite Network Cost vs. Benefit \r\n ", "", "Generation", "Annual Welfare [CHF p.a.]",
				Arrays.asList(bestScore, bestTotalBenefit, bestTotalCost),
				Arrays.asList("Total Welfare", "Benefit(+)", "Cost(-)"), 0.0, 0.0, null,
				simPath+"/x2_costBenefit.png");
		
		Visualizer.plot2D(" Travel Utility Gains (Time + Comfort) \r\n ", "", "Generation", "Monetized Travel Benefits [CHF p.a.]",
				Arrays.asList(bestTotalBenefit, bestTravelTimeGains, bestTravelTimeGainsPt, bestTravelTimeGainsCar, bestTravelTimeGainsOther),
				Arrays.asList("Total Benefit", "Total Travel Utility Gains", "PT Utility Gains", "Car Utility Gains", "Walk/Bike Utility Gains"), 0.0, 0.0, null,
				simPath+"/x3_travelUtilities.png");
		
		Visualizer.plot2D(" Difference in Mode Users to Reference Case w/o Metro \r\n ", "", "Generation", "Delta Users",
				Arrays.asList(bestDeltaPtUsers, bestDeltaCarUsers, bestDeltaOtherUsers),
				Arrays.asList("PT", "Car", "Walk/Bike"), 0.0, 0.0, null,
				simPath+"/x4_modeUsers.png");
		
		Visualizer.plot2D(" Average Travel Disutility in Travel Time Equivalents \r\n ", "", "Generation", "Time Equivalent [min]",
				Arrays.asList(bestAveragePtDisutilityEquivalentTime, bestAveragePtTime),
				Arrays.asList("Average PT Travel Utilty Time Equivalent", "Average PT Travel Time"), 0.0, 0.0, null,
				simPath+"/x5_travelTimeEquivalents.png");
		
		Visualizer.plot2D(" Difference in Average Travel Time to Reference Case w/o Metro \r\n ", "", "Generation", "Difference in Average Travel Time [min]",
				Arrays.asList(bestAverageDeltaPtTime, bestAverageDeltaCarTime, bestAverageDeltaOtherTime),
				Arrays.asList("PT", "Car", "Walk/Bike"), 0.0, 0.0, null,
				simPath+"/x6_travelTimesDelta.png");
		
		Visualizer.plot2D(" Average Travel Time \r\n ", "", "Generation", "Average Travel Time [min]",
				Arrays.asList(bestAveragePtTime, bestAverageCarTime, bestAverageOtherTime),
				Arrays.asList("PT", "Car", "Walk/Bike"), 0.0, 0.0, null,
				simPath+"/x7_travelTimes.png");
		
	} // end main

} // end class
