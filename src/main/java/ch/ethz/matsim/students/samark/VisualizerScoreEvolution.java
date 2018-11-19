package ch.ethz.matsim.students.samark;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javax.xml.stream.XMLStreamException;

public class VisualizerScoreEvolution {
	
	public static void main(String[] args) throws IOException, XMLStreamException {
	// %%% --- %% --- %% --- %% --- %% --- %% --- %% --- %% --- DISPLAY EVOLUTION OF NETWORKS---%%---%%---%%---%%---%%---%%---%%---%%---%%---%%%
	
//		java -Xmx10G -cp samark-0.0.1-SNAPSHOT.jar ch.ethz.matsim.students.samark.VisualizerScoreEvolution 10 12 10 50 33_FastSBahn_1pct/3_Evo

		
//		List<Integer> populationSizeList = Arrays.asList(12, 12, 8, 8, 8);
//		List<Integer> initialRoutesPerNetworkList = Arrays.asList(5, 5, 5, 5, 5);
//		List<Integer> generationsToPlotList = Arrays.asList(34, 43, 18, 28, 32);
//		List<Integer> lastIterationList = Arrays.asList(24, 24, 24, 24, 24);
//		List<String> folderNameList = Arrays.asList("ForExport/21_stabilityTests/1default",
//				"ForExport/21_stabilityTests/2sameInitialRoutes1", "ForExport/21_stabilityTests/6highCost",
//				"ForExport/21_stabilityTests/7smallerInitialRoutes",
//				"ForExport/21_stabilityTests/8noSmootheningCopy7");
		// 1_smallMetroRadiusLoweredCost (38), 2_lowCost20percent (34),
		// 3_longBlockLoweredDisutilityThreshold (29), 4_noFreqModNoLengthMod (38)
		List<Integer> populationSizeList = Arrays.asList(Integer.parseInt(args[0]));
		List<Integer> initialRoutesPerNetworkList = Arrays.asList(Integer.parseInt(args[1]));
		List<Integer> generationsToPlotList = Arrays.asList(Integer.parseInt(args[2]));
		List<Integer> lastIterationList = Arrays.asList(Integer.parseInt(args[3]));
		List<String> folderNameList = Arrays.asList(args[4]);

		for (Integer list = 0; list < folderNameList.size(); list++) { //
			String folderName = folderNameList.get(list);
			String inputFileName = folderName + "/zurich_1pm/Evolution/Population/networkScoreMaps.xml";
//			Visualizer.writeChartAverageTravelTimes(generationsToPlotList.get(list), populationSizeList.get(list),
//					initialRoutesPerNetworkList.get(list), lastIterationList.get(list), inputFileName,
//					folderName + "zurich_1pm/Evolution/Population/networkTravelTimesGEN"
//							+ generationsToPlotList.get(list) + ".png");
//			Visualizer.writeChartNetworkScore(generationsToPlotList.get(list), populationSizeList.get(list),
//					initialRoutesPerNetworkList.get(list), lastIterationList.get(list), inputFileName,
//					folderName + "zurich_1pm/Evolution/Population/networkScoreEvoGEN" + generationsToPlotList.get(list)
//							+ ".png");
			Visualizer.writeChartAverageTravelTimes(generationsToPlotList.get(list), populationSizeList.get(list),
					initialRoutesPerNetworkList.get(list), lastIterationList.get(list), inputFileName,
					folderName + "/networkTravelTimesGEN"
							+ generationsToPlotList.get(list) + ".png");
			Visualizer.writeChartNetworkScore(generationsToPlotList.get(list), populationSizeList.get(list),
					initialRoutesPerNetworkList.get(list), lastIterationList.get(list), inputFileName,
					folderName + "/networkScoreEvoGEN" + generationsToPlotList.get(list)
							+ ".png");
		}
	 
	}

}