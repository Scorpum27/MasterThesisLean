package ch.ethz.matsim.students.samark;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VisualizerStdDev {

	public static void main(String[] args) throws IOException {
		
		List<CBPII> cbpList = new ArrayList<CBPII>();
		List<Double> carUsers = new ArrayList<Double>();
		List<Double> ptUsers = new ArrayList<Double>();
		List<Double> carTime = new ArrayList<Double>();
		List<Double> ptTime = new ArrayList<Double>();
		Map<Integer, Double> carTimes = new HashMap<Integer, Double>();
		Map<Integer, Double> ptTimes = new HashMap<Integer, Double>();
		
		CBPII cbpGlobal = XMLOps.readFromFile(CBPII.class,
				"zurich_1pm/cbpParametersOriginal/cbpParametersOriginalGlobal.xml");

		Integer notConsideredTransientIterNumber = 30;
		Integer lastIterationAvailable = 1000;
		for (Integer c=1; c<=1000; c++) {
			String fileName = "zurich_1pm/cbpParametersOriginal/cbpParametersOriginal"+c+".xml";
			if ((new File(fileName).exists())) {
				CBPII cbp = XMLOps.readFromFile(CBPII.class, fileName);
				cbpList.add(cbp);
//				carUsers.add(100 * (cbp.carUsers/(cbp.carUsers+cbp.ptUsers+cbp.otherUsers)) / (cbpGlobal.carUsers/(cbpGlobal.carUsers+cbpGlobal.ptUsers+cbpGlobal.otherUsers)));
				carUsers.add(100*cbp.carUsers/(cbp.carUsers+cbp.ptUsers+cbp.otherUsers));
//				ptUsers.add(100 * (cbp.ptUsers/(cbp.carUsers+cbp.ptUsers+cbp.otherUsers)) / (cbpGlobal.ptUsers/(cbpGlobal.carUsers+cbpGlobal.ptUsers+cbpGlobal.otherUsers)));
				ptUsers.add(100*cbp.ptUsers/(cbp.carUsers+cbp.ptUsers+cbp.otherUsers));
//				carTime.add(100*cbp.averageCartime/cbpGlobal.averageCartime);
				carTime.add(cbp.averageCartime-cbpGlobal.averageCartime);
//				ptTime.add(100*cbp.averagePtTime/cbpGlobal.averagePtTime);
				ptTime.add(cbp.averagePtTime-cbpGlobal.averagePtTime);
				if (c>notConsideredTransientIterNumber) {
//					carTimes.put(c, 100*cbp.averageCartime/cbpGlobal.averageCartime);
					carTimes.put(c, cbp.averageCartime-cbpGlobal.averageCartime);
//					ptTimes.put(c, 100*cbp.averagePtTime/cbpGlobal.averagePtTime);
					ptTimes.put(c, cbp.averagePtTime-cbpGlobal.averagePtTime);
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
		Map<Integer, Double> ptUserStdDevs = new HashMap<Integer, Double>();
		Map<Integer, Double> carTimeStdDevs = new HashMap<Integer, Double>();
		Map<Integer, Double> ptTimeStdDevs = new HashMap<Integer, Double>();
		
		for (Integer s=notConsideredTransientIterNumber+1; s<=1000; s++) {
//			System.out.println("s = "+s);
			if (s > lastIterationAvailable) {
				break;
			}
			List<Double> carUsersCut = new ArrayList<Double>();
			carUsersCut.addAll(carUsers.subList(notConsideredTransientIterNumber, s));
			carUserStdDevs.put(s, sampleStandardDeviation(carUsersCut));
			
			List<Double> ptUsersCut = new ArrayList<Double>();
			ptUsersCut.addAll(ptUsers.subList(notConsideredTransientIterNumber, s));
			ptUserStdDevs.put(s, sampleStandardDeviation(ptUsersCut));
			
			List<Double> carTimeCut = new ArrayList<Double>();
			carTimeCut.addAll(carTime.subList(notConsideredTransientIterNumber, s));
			carTimeStdDevs.put(s+20, sampleStandardDeviation(carTimeCut));
			
			List<Double> ptTimeCut = new ArrayList<Double>();
			ptTimeCut.addAll(ptTime.subList(notConsideredTransientIterNumber, s));
			ptTimeStdDevs.put(s+20, sampleStandardDeviation(ptTimeCut));
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
		Double percentileIntervalCar = getPercentileInterval(carTime, percentilePercentage);
		Double percentileIntervalPt = getPercentileInterval(ptTime, percentilePercentage);
		
		PrintWriter pw1 = new PrintWriter("zurich_1pm/cbpParametersOriginal/Percentiles_"+percentilePercentage+"EXTENDED.txt");	pw1.close();	// Prepare empty defaultLog file for run
		Log.writeSameLine("zurich_1pm/cbpParametersOriginal/Percentiles_"+percentilePercentage+"EXTENDED.txt",
				"90/10 Percentile Interval CAR = "+percentileIntervalCar+"\r\n"
						+ "90/10 Percentile Interval PT  = "+percentileIntervalPt);
		PrintWriter pw2 = new PrintWriter("zurich_1pm/cbpParametersOriginal/Percentiles_"+percentilePercentage+".txt");	pw2.close();
		Log.writeSameLine("zurich_1pm/cbpParametersOriginal/Percentiles_"+percentilePercentage+".txt",
				percentileIntervalCar+","+percentileIntervalPt);
		
		
		String censusSize = args[0];
		Visualizer.plot2D(" Standard Deviation of Transportation Mode Share After Iter.20 (Zurich "+censusSize+" Scenario) \r\n "
				+ "[ref. CAR mode share = "+(new DecimalFormat("##.00")).format(100*cbpGlobal.carUsers/(cbpGlobal.carUsers+cbpGlobal.ptUsers+cbpGlobal.otherUsers))+" %];"
						+ "   [ref. PT mode share = "+(new DecimalFormat("##.00")).format(100*cbpGlobal.ptUsers/(cbpGlobal.carUsers+cbpGlobal.ptUsers+cbpGlobal.otherUsers))+" %]",
				"MATSim Iterations Considered", "StdDev of Mode Share [%]",
				Arrays.asList(carUserStdDevs, ptUserStdDevs),
				Arrays.asList("Mode = Car   ", "Mode = PT"), 0.0, 0.0, null,
				"StdDev_ModeShare_"+censusSize+".png"); // rangeAxis.setRange(-21.0E1, // 1.5E1)
		
		Visualizer.plot2D(" Standard Deviation of Average Travel Time After Iter.20 (Zurich "+censusSize+" Scenario) \r\n "
				+ "[ref. CAR average travel time = "+(new DecimalFormat("##.00")).format(cbpGlobal.averageCartime)+" s];"
				+ "   [ref. PT average travel time = "+(new DecimalFormat("##.00")).format(cbpGlobal.averagePtTime)+" s]",
				"MATSim Iterations Considered", "StdDev of Average Travel Time [s]",
				Arrays.asList(carTimeStdDevs, ptTimeStdDevs),
				Arrays.asList("Mode = Car   ", "Mode = PT"), 0.0, 0.0, null,
				"StdDev_ModeTravelTimes_"+censusSize+".png"); //
		
		Visualizer.plot2D("Car Average Travel Time - (Zurich "+censusSize+" Scenario) \r\n ",
				"MATSim Iteration", "Travel time deviation from mean [%]",
				Arrays.asList(carTimes),
				Arrays.asList("Mode = Car"), 0.0, 0.0, null,
				"Car_AverageTravelTimesRatio_"+censusSize+".png"); //

		Visualizer.plot2D("Pt Average Travel Time - (Zurich "+censusSize+" Scenario) \r\n ",
				"MATSim Iteration", "Travel time deviation from mean [%]",
				Arrays.asList(ptTimes),
				Arrays.asList("Mode = Pt"), 0.0, 0.0, null,
				"Pt_AverageTravelTimesRatio_"+censusSize+".png"); //
		
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
	
}
