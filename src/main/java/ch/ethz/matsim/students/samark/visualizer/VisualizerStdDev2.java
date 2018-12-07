package ch.ethz.matsim.students.samark.visualizer;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.ethz.matsim.students.samark.*;

public class VisualizerStdDev2 {

//	java -Xmx10G -cp samark-0.0.1-SNAPSHOT.jar ch.ethz.matsim.students.samark.visualizer.VisualizerStdDev2 1pct 200 40 30
	
	public static void main(String[] args) throws IOException {
		
		List<CBPII> cbpList = new ArrayList<CBPII>();
		List<Double> carUsers = new ArrayList<Double>();
		List<Double> ptUsers = new ArrayList<Double>();
		List<Double> otherUsers = new ArrayList<Double>();
		List<Double> carTimeDelta = new ArrayList<Double>();
		List<Double> ptTimeDelta = new ArrayList<Double>();
		List<Double> otherTimeDelta = new ArrayList<Double>();
		List<Double> carTime = new ArrayList<Double>();
		List<Double> ptTime = new ArrayList<Double>();
		List<Double> otherTime = new ArrayList<Double>();
//		Map<Integer, Double> carTimesDelta = new HashMap<Integer, Double>();
//		Map<Integer, Double> ptTimesDelta = new HashMap<Integer, Double>();
//		Map<Integer, Double> otherTimesDelta = new HashMap<Integer, Double>();

		
		CBPII cbpGlobal = XMLOps.readFromFile(CBPII.class,
				"zurich_1pm/cbpParametersOriginal/cbpParametersOriginalGlobal.xml");

		String censusSize = args[0];
		Integer lastIterationAvailable = Integer.parseInt(args[1]);
		Integer notConsideredTransientIterNumber = Integer.parseInt(args[2]);
		Integer devIval = Integer.parseInt(args[3]);
		
		for (Integer c=1; c<=lastIterationAvailable; c++) {
			String fileName = "zurich_1pm/cbpParametersOriginal/cbpParametersOriginal"+c+".xml";
			if ((new File(fileName).exists())) {
				CBPII cbp = XMLOps.readFromFile(CBPII.class, fileName);
				cbpList.add(cbp);
//				carUsers.add(100 * (cbp.carUsers/(cbp.carUsers+cbp.ptUsers+cbp.otherUsers)) / (cbpGlobal.carUsers/(cbpGlobal.carUsers+cbpGlobal.ptUsers+cbpGlobal.otherUsers)));
				carUsers.add(100*cbp.carUsers/(cbp.carUsers+cbp.ptUsers+cbp.otherUsers));
//				ptUsers.add(100 * (cbp.ptUsers/(cbp.carUsers+cbp.ptUsers+cbp.otherUsers)) / (cbpGlobal.ptUsers/(cbpGlobal.carUsers+cbpGlobal.ptUsers+cbpGlobal.otherUsers)));
				ptUsers.add(100*cbp.ptUsers/(cbp.carUsers+cbp.ptUsers+cbp.otherUsers));
//				otherUsers.add(100 * (cbp.otherUsers/(cbp.carUsers+cbp.ptUsers+cbp.otherUsers)) / (cbpGlobal.ptUsers/(cbpGlobal.carUsers+cbpGlobal.ptUsers+cbpGlobal.otherUsers)));
				otherUsers.add(100*cbp.otherUsers/(cbp.carUsers+cbp.ptUsers+cbp.otherUsers));
//				carTime.add(100*cbp.averageCartime/cbpGlobal.averageCartime);
				carTime.add(cbp.averageCartime);
//				ptTime.add(100*cbp.averagePtTime/cbpGlobal.averagePtTime);
				ptTime.add(cbp.averagePtTime);
//				otherTime.add(100*(cbp.customVariable1/cbp.otherUsers)/(cbpGlobal.customVariable1/cbp.otherUsers));
				otherTime.add(cbp.customVariable1/cbp.otherUsers);
				if (c>notConsideredTransientIterNumber) {
					carTimeDelta.add(cbp.averageCartime-cbpGlobal.averageCartime);
					ptTimeDelta.add(cbp.averagePtTime-cbpGlobal.averagePtTime);
					otherTimeDelta.add(cbp.customVariable1/cbp.otherUsers-cbpGlobal.customVariable1/cbpGlobal.otherUsers);
////					carTimes.put(c, 100*cbp.averageCartime/cbpGlobal.averageCartime);
//					carTimesDelta.put(c, cbp.averageCartime-cbpGlobal.averageCartime);
////					ptTimes.put(c, 100*cbp.averagePtTime/cbpGlobal.averagePtTime);
//					ptTimesDelta.put(c, cbp.averagePtTime-cbpGlobal.averagePtTime);
////					otherTimes.put(c, 100*(cbp.customVariable1/cbp.otherUsers)/(cbpGlobal.customVariable1/cbp.otherUsers));
//					otherTimesDelta.put(c, cbp.customVariable1/cbp.otherUsers-cbpGlobal.customVariable1/cbpGlobal.otherUsers);
				}
//				System.out.println("Adding #carUsers = "+carUsers.get(c-1));
//				System.out.println("Adding #ptUsers = "+ptUsers.get(c-1));
//				System.out.println("Adding carTime = "+carTime.get(c-1));
//				System.out.println("Adding ptTime = "+ptTime.get(c-1));
			}
			else {
				lastIterationAvailable = c-1;
//				System.out.println("lastIterationAvailable = "+lastIterationAvailable);
				break;
			}
		}
		
//		Map<Integer, Double> stdDevs = new HashMap<Integer, Double>();
		Map<Integer, Double> carUserStdDevs = new HashMap<Integer, Double>();
		Map<Integer, Double> carUserAverageDevs = new HashMap<Integer, Double>();
		Map<Integer, Double> carUserStdDevs30 = new HashMap<Integer, Double>();
		Map<Integer, Double> carUserMap = new HashMap<Integer, Double>();
		Map<Integer, Double> ptUserStdDevs = new HashMap<Integer, Double>();
		Map<Integer, Double> ptUserAverageDevs = new HashMap<Integer, Double>();
		Map<Integer, Double> ptUserStdDevs30 = new HashMap<Integer, Double>();
		Map<Integer, Double> ptUserMap = new HashMap<Integer, Double>();
		Map<Integer, Double> otherUserStdDevs = new HashMap<Integer, Double>();
		Map<Integer, Double> otherUserAverageDevs = new HashMap<Integer, Double>();
		Map<Integer, Double> otherUserStdDevs30 = new HashMap<Integer, Double>();
		Map<Integer, Double> otherUserMap = new HashMap<Integer, Double>();
		Map<Integer, Double> carTimeStdDevs = new HashMap<Integer, Double>();
		Map<Integer, Double> carTimeAverageDevs = new HashMap<Integer, Double>();
		Map<Integer, Double> carTimeStdDevs30 = new HashMap<Integer, Double>();
		Map<Integer, Double> carTimeMap = new HashMap<Integer, Double>();
		Map<Integer, Double> ptTimeStdDevs = new HashMap<Integer, Double>();
		Map<Integer, Double> ptTimeAverageDevs = new HashMap<Integer, Double>();
		Map<Integer, Double> ptTimeStdDevs30 = new HashMap<Integer, Double>();
		Map<Integer, Double> ptTimeMap = new HashMap<Integer, Double>();
		Map<Integer, Double> otherTimeStdDevs = new HashMap<Integer, Double>();
		Map<Integer, Double> otherTimeAverageDevs = new HashMap<Integer, Double>();
		Map<Integer, Double> otherTimeStdDevs30 = new HashMap<Integer, Double>();
		Map<Integer, Double> otherTimeMap = new HashMap<Integer, Double>();

		Double globalCarShareAverage = mean(carUsers.subList(notConsideredTransientIterNumber+1, carUsers.size()));
		Double globalPtShareAverage = mean(ptUsers.subList(notConsideredTransientIterNumber+1, ptUsers.size()));
		Double globalOtherShareAverage = mean(otherUsers.subList(notConsideredTransientIterNumber+1, otherUsers.size()));
		Double globalCarTimeAverage = mean(carTime.subList(notConsideredTransientIterNumber+1, carTime.size()));
		Double globalPtTimeAverage = mean(ptTime.subList(notConsideredTransientIterNumber+1, ptTime.size()));
		Double globalOtherTimeAverage = mean(otherTime.subList(notConsideredTransientIterNumber+1, otherTime.size()));
		
		
		for (Integer s=notConsideredTransientIterNumber+2; s<=lastIterationAvailable; s++) {
//			System.out.println("s = "+s);
			if (s > lastIterationAvailable) {
				break;
			}
			carUserMap.put(s, carUsers.get(s-1)-100*cbpGlobal.carUsers/(cbpGlobal.carUsers+cbpGlobal.ptUsers+cbpGlobal.otherUsers));
			List<Double> carUsersCut = new ArrayList<Double>();
			carUsersCut.addAll(carUsers.subList(notConsideredTransientIterNumber, s));
//			System.out.println("Adding Car Users = "+carUsers.subList(notConsideredTransientIterNumber, s));
			carUserStdDevs.put(s, sampleStandardDeviation(carUsersCut));
//			System.out.println("StdDev = "+sampleStandardDeviation(carUsersCut));
			carUserAverageDevs.put(s, averageDeviation(carUsersCut, globalCarShareAverage));
//			System.out.println("StdDevAverage = "+averageDeviation(carUsersCut, globalCarShareAverage));
			if (s>=notConsideredTransientIterNumber+devIval) {
				carUserStdDevs30.put(s, sampleStandardDeviation(carUsersCut.subList(carUsersCut.size()-devIval, carUsersCut.size())));
//				System.out.println("StdDev30 = "+sampleStandardDeviation(carUsersCut.subList(carUsersCut.size()-30, carUsersCut.size())));
			}
			
			ptUserMap.put(s, ptUsers.get(s-1)-100*cbpGlobal.ptUsers/(cbpGlobal.carUsers+cbpGlobal.ptUsers+cbpGlobal.otherUsers));
			List<Double> ptUsersCut = new ArrayList<Double>();
			ptUsersCut.addAll(ptUsers.subList(notConsideredTransientIterNumber, s));
			ptUserStdDevs.put(s, sampleStandardDeviation(ptUsersCut));
			ptUserAverageDevs.put(s, averageDeviation(ptUsersCut, globalPtShareAverage));
			if (s>=notConsideredTransientIterNumber+devIval) {
				ptUserStdDevs30.put(s, sampleStandardDeviation(carUsersCut.subList(carUsersCut.size()-devIval, carUsersCut.size())));
			}
			
			List<Double> otherUsersCut = new ArrayList<Double>();
			otherUserMap.put(s, otherUsers.get(s-1)-100*cbpGlobal.otherUsers/(cbpGlobal.carUsers+cbpGlobal.ptUsers+cbpGlobal.otherUsers));
			otherUsersCut.addAll(otherUsers.subList(notConsideredTransientIterNumber, s));
			otherUserStdDevs.put(s, sampleStandardDeviation(otherUsersCut));
			otherUserAverageDevs.put(s, averageDeviation(otherUsersCut, globalOtherShareAverage));
			if (s>=notConsideredTransientIterNumber+devIval) {
				otherUserStdDevs30.put(s, sampleStandardDeviation(otherUsersCut.subList(otherUsersCut.size()-devIval, otherUsersCut.size())));
			}
			
			List<Double> carTimeCut = new ArrayList<Double>();
			carTimeMap.put(s, carTime.get(s-1)-cbpGlobal.averageCartime);
			carTimeCut.addAll(carTime.subList(notConsideredTransientIterNumber, s));
			carTimeStdDevs.put(s, sampleStandardDeviation(carTimeCut));
			carTimeAverageDevs.put(s, averageDeviation(carTimeCut, globalCarTimeAverage));
			if (s>=notConsideredTransientIterNumber+devIval) {
				carTimeStdDevs30.put(s, sampleStandardDeviation(carTimeCut.subList(carTimeCut.size()-devIval, carTimeCut.size())));
			}
			
			List<Double> ptTimeCut = new ArrayList<Double>();
			ptTimeMap.put(s, ptTime.get(s-1)-cbpGlobal.averagePtTime);
			ptTimeCut.addAll(ptTime.subList(notConsideredTransientIterNumber, s));
			ptTimeStdDevs.put(s, sampleStandardDeviation(ptTimeCut));
			ptTimeAverageDevs.put(s, averageDeviation(ptTimeCut, globalPtTimeAverage));
			if (s>=notConsideredTransientIterNumber+devIval) {
				ptTimeStdDevs30.put(s, sampleStandardDeviation(ptTimeCut.subList(ptTimeCut.size()-devIval, ptTimeCut.size())));
			}
			
			List<Double> otherTimeCut = new ArrayList<Double>();
			otherTimeMap.put(s, otherTime.get(s-1)-cbpGlobal.customVariable1/cbpGlobal.otherUsers);
			otherTimeCut.addAll(otherTime.subList(notConsideredTransientIterNumber, s));
			otherTimeStdDevs.put(s, sampleStandardDeviation(otherTimeCut));
			otherTimeAverageDevs.put(s, averageDeviation(otherTimeCut, globalOtherTimeAverage));
			if (s>=notConsideredTransientIterNumber+devIval) {
				otherTimeStdDevs30.put(s, sampleStandardDeviation(otherTimeCut.subList(otherTimeCut.size()-devIval, otherTimeCut.size())));
			}
		}
		
//		System.out.println("Mode Shares Car = "+carUserStdDevs.toString());
//		System.out.println("Mode Shares Pt = "+ptUserStdDevs.toString());
//		for (int m=1; m<20; m++) {
//			System.out.println(ptUserStdDevs.get(m*30));
//		}
//		System.out.println("Travel Times Car = "+carTimeStdDevs.toString());
//		System.out.println("Travel Times Pt = "+ptTimeStdDevs.toString());
		
		// calculate car/ptTimes 90/10 percentile
		Integer percentilePercentage = 90;
		Double percentileIntervalCar = getPercentileInterval(carTimeDelta, percentilePercentage);
		Double percentileIntervalPt = getPercentileInterval(ptTimeDelta, percentilePercentage);
		Double percentileIntervalOther = getPercentileInterval(otherTimeDelta, percentilePercentage);
		
		PrintWriter pw1 = new PrintWriter("zurich_1pm/cbpParametersOriginal/Percentiles_"+percentilePercentage+"EXTENDED.txt");	pw1.close();	// Prepare empty defaultLog file for run
		Log.writeSameLine("zurich_1pm/cbpParametersOriginal/Percentiles_"+percentilePercentage+"EXTENDED.txt",
				"90/10 Percentile Interval CAR = "+percentileIntervalCar+"\r\n"
				+ "90/10 Percentile Interval PT  = "+percentileIntervalPt+"\r\n"
				+ "90/10 Percentile Interval WALK/BIKE  = "+percentileIntervalOther);
		PrintWriter pw2 = new PrintWriter("zurich_1pm/cbpParametersOriginal/Percentiles_"+percentilePercentage+".txt");	pw2.close();
		Log.writeSameLine("zurich_1pm/cbpParametersOriginal/Percentiles_"+percentilePercentage+".txt",
				percentileIntervalCar+","+percentileIntervalPt+","+percentileIntervalOther);
		
//		Visualizer.plot2D(" Standard Deviation of Transportation Mode Share After Iter.30 (Zurich "+censusSize+" Scenario) \r\n "
//				+ "[ref. CAR mode share = "+(new DecimalFormat("##.00")).format(100*cbpGlobal.carUsers/(cbpGlobal.carUsers+cbpGlobal.ptUsers+cbpGlobal.otherUsers))+" %];"
//						+ "   [ref. PT mode share = "+(new DecimalFormat("##.00")).format(100*cbpGlobal.ptUsers/(cbpGlobal.carUsers+cbpGlobal.ptUsers+cbpGlobal.otherUsers))+" %]"
//						+ "   [ref. WALK/BIKE mode share = "+(new DecimalFormat("##.00")).format(100*cbpGlobal.otherUsers/(cbpGlobal.carUsers+cbpGlobal.ptUsers+cbpGlobal.otherUsers))+" %]",
//				"MATSim Iterations Considered", "StdDev of Mode Share [%]",
//				Arrays.asList(carUserStdDevs, ptUserStdDevs, otherUserStdDevs),
//				Arrays.asList("Mode = Car   ", "Mode = PT", "Mode = WALK/BIKE"), 0.0, 0.0, null,
//				"StdDev_ModeShare_"+censusSize+".png"); // rangeAxis.setRange(-21.0E1, // 1.5E1)
		
//		for (int i = 1; i<=100; i++) {
//			if (carUserAverageDevs.containsKey(i)) {
//				System.out.println(i + "=" + carUserAverageDevs.get(i));
//			}
//		}
		
		int nr = notConsideredTransientIterNumber;
		
		Visualizer.plot2D(" Stability of Car Mode Share After Iter. "+nr+" (Zurich "+censusSize+") \r\n ",
				"[Average CAR share = "+(new DecimalFormat("##.00")).format(100*cbpGlobal.carUsers/(cbpGlobal.carUsers+cbpGlobal.ptUsers+cbpGlobal.otherUsers))+" %] \r\n",
				"Last MATSim Iteration Considered", "Deviation in Mode Share [%]",
				Arrays.asList(carUserStdDevs, carUserStdDevs30, carUserAverageDevs),
				Arrays.asList("Standard Deviation (all up to this iter)", "Standard Deviation (last 30 iters)", "Mean deviation from global average (all up to this iter)"), 0.0, 0.0, null,
				"StdDev_ModeShareCarX_"+censusSize+".png"); // rangeAxis.setRange(-21.0E1, // 1.5E1)
		
		Visualizer.plot2D(" Stability of PT Mode Share After Iter. "+nr+" (Zurich "+censusSize+") \r\n ",
				"[Average PT share = "+(new DecimalFormat("##.00")).format(100*cbpGlobal.ptUsers/(cbpGlobal.carUsers+cbpGlobal.ptUsers+cbpGlobal.otherUsers))+" %] \r\n",
				"Last MATSim Iteration Considered", "Deviation in Mode Share PT [%]",
				Arrays.asList(ptUserStdDevs, ptUserStdDevs30, ptUserAverageDevs),
				Arrays.asList("Standard Deviation (all up to this iter)", "Standard Deviation (last 30 iters)", "Mean deviation from global average (all up to this iter)"), 0.0, 0.0, null,
				"StdDev_ModeSharePTX_"+censusSize+".png"); // rangeAxis.setRange(-21.0E1, // 1.5E1)

		Visualizer.plot2D(" Stability of \"Other\" Mode Share After Iter. "+nr+" (Zurich "+censusSize+") \r\n ",
				"[Average \"Other\" share = "+(new DecimalFormat("##.00")).format(100*cbpGlobal.otherUsers/(cbpGlobal.carUsers+cbpGlobal.ptUsers+cbpGlobal.otherUsers))+" %] \r\n",
				"Last MATSim Iteration Considered", "Deviation in Mode Share \"Other\" [%]",
				Arrays.asList(otherUserStdDevs, otherUserStdDevs30, otherUserAverageDevs),
				Arrays.asList("Standard Deviation (all up to this iter)", "Standard Deviation (last 30 iters)", "Mean deviation from global average (all up to this iter)"), 0.0, 0.0, null,
				"StdDev_ModeShareOTHERX_"+censusSize+".png"); // rangeAxis.setRange(-21.0E1, // 1.5E1)
		
		Visualizer.plot2D(" Stability of Average CAR Travel Time After Iter "+nr+" (Zurich "+censusSize+") \r\n ",
				"[Average CAR travel time = "+(new DecimalFormat("##.00")).format(cbpGlobal.averageCartime)+" s] \r\n",
				"Last MATSim Iteration Considered", "Average Travel Time Dev. CAR [s]",
				Arrays.asList(carTimeStdDevs, carTimeStdDevs30, carTimeAverageDevs),
				Arrays.asList("Standard Deviation (all up to this iter)", "Standard Deviation (last 30 iters)", "Mean deviation from global average (all up to this iter)"), 0.0, 0.0, null,
				"StdDev_TravelTimesCarX_"+censusSize+".png"); // rangeAxis.setRange(-21.0E1, // 1.5E1)
		
		Visualizer.plot2D(" Stability of Average PT Travel Time After Iter. "+nr+" (Zurich "+censusSize+") \r\n ",
				"[Average PT travel time = "+(new DecimalFormat("##.00")).format(cbpGlobal.averagePtTime)+" s] \r\n",
				"Last MATSim Iteration Considered", "Average Travel Time Dev. PT [s]",
				Arrays.asList(ptTimeStdDevs, ptTimeStdDevs30, ptTimeAverageDevs),
				Arrays.asList("StdDev. (all up to this iter)", "StdDev. (last 30 iters)", "MeanDev. from global average (all up to this iter)"), 0.0, 0.0, null,
				"StdDev_TravelTimesPtX_"+censusSize+".png"); // rangeAxis.setRange(-21.0E1, // 1.5E1)

		Visualizer.plot2D(" Stability of Average \"Other\" Travel Time After Iter. "+nr+" (Zurich "+censusSize+") \r\n ",
				"[Average \"Other\" travel time = "+(new DecimalFormat("##.00")).format(cbpGlobal.customVariable1/cbpGlobal.otherUsers)+" s] \r\n",
				"Last MATSim Iteration Considered", "Average Travel Time Dev. \"Other\" [s]",
				Arrays.asList(otherTimeStdDevs, otherTimeStdDevs30, otherTimeAverageDevs),
				Arrays.asList("StdDev. (all up to this iter)", "StdDev. (last 30 iters)", "MeanDev. from global average (all up to this iter)"), 0.0, 0.0, null,
				"StdDev_TravelTimesOtherX_"+censusSize+".png"); // rangeAxis.setRange(-21.0E1, // 1.5E1)
		
		Visualizer.plot2D(" Average Travel Time Deviations After Iter. "+nr+" (Zurich "+censusSize+") \r\n ", "",
				"Last MATSim Iteration Considered", "Average Travel Time Deviation [s]",
				Arrays.asList(carTimeMap, ptTimeMap, otherTimeMap),
				Arrays.asList("CAR", "PT", "Other"), 0.0, 0.0, null,
				"Dev_TravelTimes_CAR_PT_Other_X_"+censusSize+".png"); // rangeAxis.setRange(-21.0E1, // 1.5E1)
		
		Visualizer.plot2D(" Mode Share Deviations After Iter. "+nr+" (Zurich "+censusSize+") \r\n ",  "",
				"Last MATSim Iteration Considered", "Mode Share Deviation [%]",
				Arrays.asList(carUserMap, ptUserMap, otherUserMap),
				Arrays.asList("CAR", "PT", "Other"), 0.0, 0.0, null,
				"Dev_ModeShares_CAR_PT_Other_X_"+censusSize+".png"); // rangeAxis.setRange(-21.0E1, // 1.5E1)
		
//		Visualizer.plot2D(" Standard Deviation of Average Travel Time After Iter.30 (Zurich "+censusSize+" Scenario) \r\n "
//				+ "[ref. CAR average travel time = "+(new DecimalFormat("##.00")).format(cbpGlobal.averageCartime)+" s];"
//				+ "   [ref. PT average travel time = "+(new DecimalFormat("##.00")).format(cbpGlobal.averagePtTime)+" s]"
//				+ "   [ref. WALK/BIKE average travel time = "+(new DecimalFormat("##.00")).format(cbpGlobal.customVariable1/cbpGlobal.otherUsers)+" s]",
//				"MATSim Iterations Considered", "StdDev of Average Travel Time [s]",
//				Arrays.asList(carTimeStdDevs, ptTimeStdDevs, otherTimeStdDevs),
//				Arrays.asList("Mode = Car   ", "Mode = PT", "Mode = WALK/BIKE"), 0.0, 0.0, null,
//				"StdDev_ModeTravelTimes_"+censusSize+".png"); //
//		
//		Visualizer.plot2D("Car Average Travel Time - (Zurich "+censusSize+" Scenario) \r\n ",
//				"MATSim Iteration", "Travel time deviation from mean [%]",
//				Arrays.asList(carTimesDelta),
//				Arrays.asList("Mode = Car"), 0.0, 0.0, null,
//				"Car_AverageTravelTimesRatio_"+censusSize+".png"); //
//
//		Visualizer.plot2D("Pt Average Travel Time - (Zurich "+censusSize+" Scenario) \r\n ",
//				"MATSim Iteration", "Travel time deviation from mean [%]",
//				Arrays.asList(ptTimesDelta),
//				Arrays.asList("Mode = Pt"), 0.0, 0.0, null,
//				"Pt_AverageTravelTimesRatio_"+censusSize+".png"); //
//		
//		Visualizer.plot2D("Walk/Bike Average Travel Time - (Zurich "+censusSize+" Scenario) \r\n ",
//				"MATSim Iteration", "Travel time deviation from mean [%]",
//				Arrays.asList(otherTimesDelta),
//				Arrays.asList("Mode = Walk/Bike"), 0.0, 0.0, null,
//				"Other_AverageTravelTimesRatio_"+censusSize+".png"); //
	}

	
	public static Double getPercentileInterval(List<Double> values, Integer percentilePercentage) {
		List<Double> sortedValues = new ArrayList<Double>(Arrays.asList(-1.0));
		Double mean = mean(values);
		for (Double v : values) {
			Integer index = 0;
			Double insertValue = 0.0;
			for (Double vs : sortedValues) {
				if (Math.abs(v-mean) > vs) {
					insertValue = Math.abs(v-mean);
					break;
				}
				else {
					index++;
					continue;
				}
			}
			sortedValues.add(index, insertValue);
		}
		Double percentileInterval = sortedValues.get( (int) Math.ceil((1.0-percentilePercentage/100.0)*sortedValues.size()));
		return percentileInterval;
	}
	
	public static Double getPercentileIntervalMap(Map<Integer, Double> values, Integer percentilePercentage) {
		List<Double> sortedValues = new ArrayList<Double>(Arrays.asList(-1.0));
		Double mean = meanMap(values);
		for (Double v : values.values()) {
			Integer index = 0;
			Double insertValue = 0.0;
			for (Double vs : sortedValues) {
				if (Math.abs(v-mean) > vs) {
					insertValue = Math.abs(v-mean);
					break;
				}
				else {
					index++;
					continue;
				}
			}
			sortedValues.add(index, insertValue);
		}
		Double percentileInterval = sortedValues.get( (int) Math.ceil((1.0-percentilePercentage/100.0)*sortedValues.size()));
		return percentileInterval;
	}


	public static double mean(List<Double> dataSet){
		double result = 0, sum = 0;
		
		for(int dataSet_i=0; dataSet_i<dataSet.size(); dataSet_i++){
			sum += dataSet.get(dataSet_i);
		}
		
		result = sum / dataSet.size();
//		System.out.println("Returning Mean = "+result);

		return result;
	}

	public static double meanMap(Map<Integer,Double> dataSet){
		double result = 0, sum = 0;
		
		for(double dataPoint : dataSet.values()){
			sum += dataPoint;
		}
		
		result = sum / dataSet.size();
//		System.out.println("Returning Mean = "+result);

		return result;
	}
	
	public static double sampleVariance(List<Double> dataSet){
		double result = 0;
		
//		System.out.println("Returning mean = "+mean(dataSet));
		
		for(int dataSet_i=0;dataSet_i<dataSet.size();dataSet_i++){
			result += Math.pow((dataSet.get(dataSet_i)- mean(dataSet)) ,2);
		}
		
		result /= (dataSet.size() - 1);
//		System.out.println("Returning sampleVariance = "+result);

		return result;
	}
	
	public static double sampleStandardDeviation(List<Double> dataSet){
		Double stdDev = Math.sqrt(sampleVariance(dataSet));
//		System.out.println("Returning StdDev = "+stdDev);
		return stdDev;
	}
	
	
	public static double averageDeviation(List<Double> dataSet, Double devFromValue){
		double result = 0;
		
//		System.out.println("Returning mean = "+mean(dataSet));
		
		for(int dataSet_i=0;dataSet_i<dataSet.size();dataSet_i++){
			result += Math.sqrt(Math.pow((dataSet.get(dataSet_i)-devFromValue) ,2));
		}
		
		result /= (dataSet.size() - 1);
//		System.out.println("Returning sampleVariance = "+result);

		return result;
	}
	
	
}
