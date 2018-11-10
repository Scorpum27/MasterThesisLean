package ch.ethz.matsim.students.samark;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VisualizerStdDev {

	public static void main(String[] args) throws IOException {
		
		List<CostBenefitParameters> cbpList = new ArrayList<CostBenefitParameters>();
		List<Double> carUsers = new ArrayList<Double>();
		List<Double> ptUsers = new ArrayList<Double>();
		List<Double> carTime = new ArrayList<Double>();
		List<Double> ptTime = new ArrayList<Double>();
		
		CostBenefitParameters cbpGlobal = XMLOps.readFromFile(CostBenefitParameters.class,
				"zurich_1pm/cbpParametersOriginal/cbpParametersOriginalGlobal.xml");
		
		Integer lastIterationAvailable = 1000;
		for (Integer c=1; c<=1000; c++) {
			String fileName = "zurich_1pm/cbpParametersOriginal/cbpParametersOriginal"+c+".xml";
			if ((new File(fileName).exists())) {
				CostBenefitParameters cbp = XMLOps.readFromFile(CostBenefitParameters.class, fileName);
				cbpList.add(cbp);
				carUsers.add(100 * (cbp.carUsers/(cbp.carUsers+cbp.ptUsers+cbp.otherUsers)) / (cbpGlobal.carUsers/(cbpGlobal.carUsers+cbpGlobal.ptUsers+cbpGlobal.otherUsers)));
//				System.out.println("Adding #carUsers = "+carUsers.get(c-1));
				ptUsers.add(100 * (cbp.ptUsers/(cbp.carUsers+cbp.ptUsers+cbp.otherUsers)) / (cbpGlobal.ptUsers/(cbpGlobal.carUsers+cbpGlobal.ptUsers+cbpGlobal.otherUsers)));
//				System.out.println("Adding #ptUsers = "+ptUsers.get(c-1));
				carTime.add(100*cbp.averageCartime/cbpGlobal.averageCartime);
//				System.out.println("Adding carTime = "+carTime.get(c-1));
				ptTime.add(100*cbp.averagePtTime/cbpGlobal.averagePtTime);
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
		
		for (Integer s=30; s<=1000; s++) {
//			System.out.println("s = "+s);
			if (s > lastIterationAvailable) {
				break;
			}
			List<Double> carUsersCut = new ArrayList<Double>();
			carUsersCut.addAll(carUsers.subList(20, s));
			carUserStdDevs.put(s, sampleStandardDeviation(carUsersCut));
			
			List<Double> ptUsersCut = new ArrayList<Double>();
			ptUsersCut.addAll(ptUsers.subList(20, s));
			ptUserStdDevs.put(s, sampleStandardDeviation(ptUsersCut));
			
			List<Double> carTimeCut = new ArrayList<Double>();
			carTimeCut.addAll(carTime.subList(20, s));
			carTimeStdDevs.put(s, sampleStandardDeviation(carTimeCut));
			
			List<Double> ptTimeCut = new ArrayList<Double>();
			ptTimeCut.addAll(ptTime.subList(20, s));
			ptTimeStdDevs.put(s, sampleStandardDeviation(ptTimeCut));
		}
		
//		System.out.println("Mode Shares Car = "+carUserStdDevs.toString());
//		System.out.println("Mode Shares Pt = "+ptUserStdDevs.toString());
//		for (int m=1; m<20; m++) {
//			System.out.println(ptUserStdDevs.get(m*30));
//		}
//		System.out.println("Travel Times Car = "+carTimeStdDevs.toString());
//		System.out.println("Travel Times Pt = "+ptTimeStdDevs.toString());
		
		
		Visualizer.plot2D(" Standard Deviation of Transportation Mode Share After Iter.20 (Zurich 1pct Scenario) \r\n ",
				"MATSim Iterations Considered", "StdDev of Mode Share [%]",
				Arrays.asList(carUserStdDevs, ptUserStdDevs),
				Arrays.asList("Mode = Car   ", "Mode = PT"), 0.0, 0.0, null,
				"StdDev_ModeShare_1pct.png"); // rangeAxis.setRange(-21.0E1, // 1.5E1)
		
		Visualizer.plot2D(" Standard Deviation of Average Travel Time After Iter.20 (Zurich 1pct Scenario) \r\n ",
				"MATSim Iterations Considered", "StdDev of Average Travel Time [%]",
				Arrays.asList(carTimeStdDevs, ptTimeStdDevs),
				Arrays.asList("Mode = Car   ", "Mode = PT"), 0.0, 0.0, null,
				"StdDev_ModeTravelTimes_1pct.png"); //
		
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
