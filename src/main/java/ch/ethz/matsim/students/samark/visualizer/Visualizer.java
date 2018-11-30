package ch.ethz.matsim.students.samark.visualizer;

import ch.ethz.matsim.students.samark.*;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.stream.XMLStreamException;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.Range;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.Layer;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.TextAnchor;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.charts.XYLineChart;

public class Visualizer {
	public static void main(String[] args) throws IOException, XMLStreamException {

		Map<Integer, Double> constrCostL = new HashMap<Integer, Double>();
		Map<Integer, Double> opsCostL = new HashMap<Integer, Double>();
//		Map<Integer, Double> travelTimeGainsL = new HashMap<Integer, Double>();
		
		Map<Integer, Double> constrCostS = new HashMap<Integer, Double>();
		Map<Integer, Double> opsCostS = new HashMap<Integer, Double>();
//		Map<Integer, Double> travelTimeGainsS = new HashMap<Integer, Double>();
		
		constrCostS.put(1700, 6.902977452339385E7);		// 1.7*1000m
		constrCostS.put(2720, 1.1100306526476438E8);	// 1.7*1600m
		constrCostS.put(4250, 1.6320536507177228E8);	// 1.7*2400m
		constrCostS.put(5950, 2.350633380775754E8);		// 1.7*3500m
		constrCostS.put(8500, 2.905941414059561E8);		// 1.7*5000m
		constrCostS.put(12750, 3.278998568289484E8);	// 1.7*7500m
		constrCostS.put(18700, 4.156194319926613E8);	// 1.7*11000m
		constrCostS.put(25500, 4.748733009056242E8);	// 1.7*15000m
		constrCostS.put(34000, 4.607198592194826E8);	// 1.7*20000m
		
		opsCostS.put(1700, 2.529917825217225E7);
		opsCostS.put(2720, 4.0698579703364246E7);
		opsCostS.put(4250, 5.983940898202526E7);
		opsCostS.put(5950, 9.019628921231318E7);
		opsCostS.put(8500, 1.326258232350149E8);
		opsCostS.put(12750, 1.9024728268439013E8);
		opsCostS.put(18700, 3.088052994235236E8);
		opsCostS.put(25500, 4.395053859601001E8);
		opsCostS.put(34000, 6.147799305661485E8);

		constrCostL.put(10, 2.3464058045818132E8);	// #lines = 10;
		constrCostL.put(20, 4.896106234818264E8);	// #lines = 20;
		constrCostL.put(40, 9.57577579432038E8);	// #lines = 40;
		constrCostL.put(70, 1.7663871201192355E9);	// #lines = 70;
		constrCostL.put(120, 2.770902645035945E9);	// #lines = 120;
		constrCostL.put(200, 4.876622639974809E9);	// #lines = 200;
		constrCostL.put(320, 7.380766295861484E9);	// #lines = 320;
		constrCostL.put(500, 1.175860213446533E10);	// #lines = 500;
		
		opsCostL.put(10, 9.539060067852162E7);	// #lines = 10;
		opsCostL.put(20, 2.0702397252031314E8);	// #lines = 20;
		opsCostL.put(40, 4.042777162022962E8);	// #lines = 40;
		opsCostL.put(70, 7.225828823353561E8);	// #lines = 70;
		opsCostL.put(120, 1.2627351762087545E9);	// #lines = 120;
		opsCostL.put(200, 2.067041009569469E9);	// #lines = 200;
		opsCostL.put(320, 3.262419325128492E9);	// #lines = 320;
		opsCostL.put(500, 5.070105129047192E9);	// #lines = 500;
				
		Visualizer.plot2D(" Dominant Costs for Different Metro Network Sizes [DepSpacing=420s] \r\n ",
				"Size of metro network (Number of Lines = 10)", "Annual Cost [CHF]",
				Arrays.asList(constrCostS, opsCostS),
				Arrays.asList("Construction Costs", "Operational Costs"), 0.0, 0.0, null,
				"Cost_SizeNetwork.png"); // rangeAxis.setRange(-21.0E1, // 1.5E1)		
		
		Visualizer.plot2D(" Dominant Costs for Different Numbers of Metro Lines [DepSpacing=420s] \r\n ",
				"Number of Metro Lines (Metro Network Radius = 6800m)", "Annual Cost [CHF]",
				Arrays.asList(constrCostL, opsCostL),
				Arrays.asList("Construction Costs", "Operational Costs"), 0.0, 0.0, null,
				"Cost_LinesNr.png"); // rangeAxis.setRange(-21.0E1, // 1.5E1)
		
/*		// %%% --- %% --- %% --- %% --- NETWORK PARAMETER IMPACT ON KEY INDICATORS --- %% --- %% --- %% --- %% --- %% --- %% --- %% --- %% --- %% ---
		// all data extracted manually from Export folder 23(ref scenario) and 24(parameters)
		Double refPtUsers = 349752.55;
		Double refCarUsers = 911639.49;
		Double refOtherUsers = 332805.31;
		Double refAveragePtTime = 6781.65;
		Double refAverageCarTime = 2648.16;
		Double refPtPersonDistance = 1.3896E10;
		Double refCarPersonDistance = 3.0434E10;
		// changing metro #lines parameter
		Integer lineNr;
		Map<Integer, Double> ptUsersL = new HashMap<Integer, Double>();
		Map<Integer, Double> carUsersL = new HashMap<Integer, Double>();
		Map<Integer, Double> ptTimeL = new HashMap<Integer, Double>();
		Map<Integer, Double> carTimeL = new HashMap<Integer, Double>();
		Map<Integer, Double> ptUsersS = new HashMap<Integer, Double>();
		Map<Integer, Double> carUsersS = new HashMap<Integer, Double>();
		Map<Integer, Double> ptTimeS = new HashMap<Integer, Double>();
		Map<Integer, Double> carTimeS = new HashMap<Integer, Double>();
		
//		ptUsersL.put(1, refPtUsers);	ptUsersL.put(1001, refPtUsers);   // horizontal ref line
//		carUsersL.put(1, refCarUsers);	carUsersL.put(1001, refCarUsers);   // horizontal ref line
//		ptTimeL.put(1, refAveragePtTime);	ptTimeL.put(1001, refAveragePtTime);   // horizontal ref line
//		carTimeL.put(1, refAverageCarTime);	carTimeL.put(1001, refAverageCarTime);   // horizontal ref line
		
		// #Lines = 10;
			lineNr = 10;
			ptUsersL.put(lineNr, (350477.34-refPtUsers)*100/(refPtUsers+refCarUsers+refOtherUsers));	// *100/(allRefUsersSummed)
			carUsersL.put(lineNr, (910518.23-refCarUsers)*100/(refPtUsers+refCarUsers+refOtherUsers));
			ptTimeL.put(lineNr, 100*(6737.24-refAveragePtTime)/refAveragePtTime);
			carTimeL.put(lineNr, 100*(2646.49-refAverageCarTime)/refAverageCarTime);
		// #Lines = 40;
			lineNr = 40;
			ptUsersL.put(lineNr, (350932.04-refPtUsers)*100/(refPtUsers+refCarUsers+refOtherUsers));
			carUsersL.put(lineNr, (910223.20-refCarUsers)*100/(refPtUsers+refCarUsers+refOtherUsers));
			ptTimeL.put(lineNr, 100*(6648.35-refAveragePtTime)/refAveragePtTime);
			carTimeL.put(lineNr, 100*(2640.11-refAverageCarTime)/refAverageCarTime);			
		// #Lines = 90;
			lineNr = 90;
			ptUsersL.put(lineNr, (350700.00-refPtUsers)*100/(refPtUsers+refCarUsers+refOtherUsers));
			carUsersL.put(lineNr, (908625.41-refCarUsers)*100/(refPtUsers+refCarUsers+refOtherUsers));
			ptTimeL.put(lineNr, 100*(6667.17-refAveragePtTime)/refAveragePtTime);
			carTimeL.put(lineNr, 100*(2634.39-refAverageCarTime)/refAverageCarTime);
		// #Lines = 150;
			lineNr = 150;
			ptUsersL.put(lineNr, (351107.18-refPtUsers)*100/(refPtUsers+refCarUsers+refOtherUsers));
			carUsersL.put(lineNr, (908865.19-refCarUsers)*100/(refPtUsers+refCarUsers+refOtherUsers));
			ptTimeL.put(lineNr, 100*(6601.23-refAveragePtTime)/refAveragePtTime);
			carTimeL.put(lineNr, 100*(2634.96-refAverageCarTime)/refAverageCarTime);
		// #Lines = 300;
			lineNr = 300;
			ptUsersL.put(lineNr, (350811.43-refPtUsers)*100/(refPtUsers+refCarUsers+refOtherUsers));
			carUsersL.put(lineNr, (908309.55-refCarUsers)*100/(refPtUsers+refCarUsers+refOtherUsers));
			ptTimeL.put(lineNr, 100*(6587.87-refAveragePtTime)/refAveragePtTime);
			carTimeL.put(lineNr, 100*(2634.11-refAverageCarTime)/refAverageCarTime);
		// #LineNr = 600;
			lineNr = 600;
			ptUsersL.put(lineNr, (350198.90-refPtUsers)*100/(refPtUsers+refCarUsers+refOtherUsers));
			carUsersL.put(lineNr, (907909.94-refCarUsers)*100/(refPtUsers+refCarUsers+refOtherUsers));
			ptTimeL.put(lineNr, 100*(6573.44-refAveragePtTime)/refAveragePtTime);
			carTimeL.put(lineNr, 100*(2636.17-refAverageCarTime)/refAverageCarTime);
		// #Lines = 1000;
			lineNr = 1000;
			ptUsersL.put(lineNr, (350188.40-refPtUsers)*100/(refPtUsers+refCarUsers+refOtherUsers));
			carUsersL.put(lineNr, (907997.24-refCarUsers)*100/(refPtUsers+refCarUsers+refOtherUsers));
			ptTimeL.put(lineNr, 100*(6558.50-refAveragePtTime)/refAveragePtTime);
			carTimeL.put(lineNr, 100*(2631.71-refAverageCarTime)/refAverageCarTime);
	// ---
			Integer size;
		// #Lines = 10;
			size = (int) 1.7*3000;
			ptUsersS.put(size, (350148.06-refPtUsers)*100/(refPtUsers+refCarUsers+refOtherUsers));	// *100/(allRefUsersSummed)
			carUsersS.put(size, (910697.79-refCarUsers)*100/(refPtUsers+refCarUsers+refOtherUsers));
			ptTimeS.put(size, 100*(6675.94-refAveragePtTime)/refAveragePtTime);
			carTimeS.put(size, 100*(2639.86-refAverageCarTime)/refAverageCarTime);
		// #Lines = 40;
			size = (int) 1.7*5000;
			ptUsersS.put(size, (351990.05-refPtUsers)*100/(refPtUsers+refCarUsers+refOtherUsers));
			carUsersS.put(size, (908356.35-refCarUsers)*100/(refPtUsers+refCarUsers+refOtherUsers));
			ptTimeS.put(size, 100*(6639.97-refAveragePtTime)/refAveragePtTime);
			carTimeS.put(size, 100*(2630.66-refAverageCarTime)/refAverageCarTime);			
		// #Lines = 90;
			size = (int) 1.7*7000;
			ptUsersS.put(size, (349774.03-refPtUsers)*100/(refPtUsers+refCarUsers+refOtherUsers));
			carUsersS.put(size, (910010.49-refCarUsers)*100/(refPtUsers+refCarUsers+refOtherUsers));
			ptTimeS.put(size, 100*(6596.89-refAveragePtTime)/refAveragePtTime);
			carTimeS.put(size, 100*(2635.23-refAverageCarTime)/refAverageCarTime);
		// #Lines = 150;
			size = (int) 1.7*9000;
			ptUsersS.put(size, (352900.55-refPtUsers)*100/(refPtUsers+refCarUsers+refOtherUsers));
			carUsersS.put(size, (908486.18-refCarUsers)*100/(refPtUsers+refCarUsers+refOtherUsers));
			ptTimeS.put(size, 100*(6538.18-refAveragePtTime)/refAveragePtTime);
			carTimeS.put(size, 100*(2632.04-refAverageCarTime)/refAverageCarTime);
		// #Lines = 300;
			size = (int) 1.7*13000;
			ptUsersS.put(size, (355356.35-refPtUsers)*100/(refPtUsers+refCarUsers+refOtherUsers));
			carUsersS.put(size, (907564.08-refCarUsers)*100/(refPtUsers+refCarUsers+refOtherUsers));
			ptTimeS.put(size, 100*(6499.45-refAveragePtTime)/refAveragePtTime);
			carTimeS.put(size, 100*(2622.92-refAverageCarTime)/refAverageCarTime);
		// #LineNr = 600;
			size = (int) 1.7*20000;
			ptUsersS.put(size, (358534.80-refPtUsers)*100/(refPtUsers+refCarUsers+refOtherUsers));
			carUsersS.put(size, (905602.20-refCarUsers)*100/(refPtUsers+refCarUsers+refOtherUsers));
			ptTimeS.put(size, 100*(6289.46-refAveragePtTime)/refAveragePtTime);
			carTimeS.put(size, 100*(2614.83-refAverageCarTime)/refAverageCarTime);

			
		Visualizer.plot2D(" Mode share deviation from ref. case without metro (ZH_1pct scenario) [r=8500m, DepSpacing=300s] \r\n ",
				"Number of metro lines", "Deviation from ref. case [%]",
				Arrays.asList(carUsersL, ptUsersL),
				Arrays.asList("Mode = Car", "Mode = PT"), 0.0, 0.0, null,
				"Dev_ModeShare_1pct.png"); // rangeAxis.setRange(-21.0E1, // 1.5E1)

		Visualizer.plot2D(" Average travel time deviation from ref. case without metro (ZH_1pct scenario) [r=8500m, DepSpacing=300s] \r\n ",
				"Number of metro lines", "Deviation from ref. case [%]",
				Arrays.asList(carTimeL, ptTimeL),
				Arrays.asList("Mode = Car", "Mode = PT"), 0.0, 0.0, null,
				"Dev_AverageTravelTime_1pct.png"); // rangeAxis.setRange(-21.0E1, // 1.5E1)
		
		Visualizer.plot2D(" Mode share deviation from ref. case without metro (ZH_1pct scenario) [#metroLines=100, DepSpacing=300s] \r\n ",
				"Metro network radius [m]", "Deviation from ref. case [%]",
				Arrays.asList(carUsersS, ptUsersS),
				Arrays.asList("Mode = Car", "Mode = PT"), 0.0, 0.0, null,
				"MetroNetworkSize_Dev_ModeShare_1pct.png"); // rangeAxis.setRange(-21.0E1, // 1.5E1)

		Visualizer.plot2D(" Average travel time deviation from ref. case without metro (ZH 1pct scenario) [#metroLines=100, DepSpacing=300s] \r\n ",
				"Metro network radius [m]", "Average travel time deviation from ref. case [%]",
				Arrays.asList(carTimeS, ptTimeS),
				Arrays.asList("Mode = Car", "Mode = PT"), 0.0, 0.0, null,
				"MetroNetworkSize_Dev_AverageTravelTime_1pct.png"); // rangeAxis.setRange(-21.0E1, // 1.5E1)
*/
		
		// %%% --- %% --- %% --- %% --- %% --- DISPLAY EVOLUTION OF NETWORKS --- %% --- %% --- %% --- %% --- %% --- %% --- %% --- %% --- %% ---

		// BufferedImage bgImage = null;
		// bgImage = ImageIO.read(new File("zurich_1pm/bgImgMedium.png"));
		// Double xSize = (double) bgImage.getWidth();
		// Double ySize = (double) bgImage.getHeight();
		//
		// // Coord zurich_NetworkCenterCoord = new Coord(2683360.00, 1248100.00); //
		// default Coord(2683360.00,1248100.00); old:(2683000.00, 1247700.00)
		// // Double xOffset = 1733436.0; // add this to QGis to get MATSim // Right
		// upper corner of Zürisee -- X_QGis=950040; X_MATSim= 2683476;
		// // Double yOffset = -4748525.0; // add this to QGis to get MATSim // Right
		// upper corner of Zürisee -- Y_QGis=5995336;
		// final Double x0 = 2669398.0; // top left corner of background image
		// final Double y0 = 1256012.0; // top left corner of background image
		// final Double x1 = 2696233.0; // bottom right corner of background image
		// final Double y1 = 1240325.0; // bottom right corner of background image
		// Double xScalingFactor = (x1-x0)/xSize;
		// Double yScalingFactor = (y1-y0)/ySize; // will be negative due to y-decrease
		// of COS for increasing graphicsCOS
		//
		// String finalNetwork = "Network2";
		// final Integer firstGeneration = 1;
		// final Integer finalGeneration = 29;
		// List<Map<String, String>> pedigreeTree = new ArrayList<Map<String,
		// String>>();
		//// String historyLogFolder =
		// "ForExport/19_UtilityAndFreq/1_smallMetroRadiusLoweredCost/zurich_1pm/Evolution/Population/HistoryLog/";
		// // GEN=1/38
		// String historyLogFolder =
		// "ForExport/19_UtilityAndFreq/3_longBlockLoweredDisutilityThreshold/zurich_1pm/Evolution/Population/HistoryLog/";
		// // GEN=1/29
		// pedigreeTree.addAll(XMLOps.readFromFile(pedigreeTree.getClass(),
		// historyLogFolder+"pedigreeTree.xml"));
		// System.out.println("pedigreeTreeSize = "+pedigreeTree.size());
		// if (pedigreeTree.size() != finalGeneration-1) {
		// Log.writeAndDisplay("CAUTION: Pedigree tree has not same number of
		// generations as finalGeneration! Please check. Aborting ...");
		// }
		//
		// String thisGenNetwork = finalNetwork;
		// generationLoop:
		// for (Integer gen=finalGeneration; gen>=firstGeneration; gen--) {
		//
		// BufferedImage blankImg = null;
		// blankImg = ImageIO.read(new File("zurich_1pm/bgImgMedium.png"));
		// Graphics2D g = (Graphics2D) blankImg.createGraphics();
		// g.setColor(Color.CYAN);
		// g.setStroke(new BasicStroke(3));
		//
		// String routesFolder = historyLogFolder+"Generation"+gen+"/MRoutes/";
		//
		// // alternative: display all MRoutes individually (can display with different
		// colors then)
		// // for (Integer r=0; r<50; r++) {
		// // if ((new File("routesFolder" + thisGenNetwork + "_Route"+r
		// +"_RoutesFile.xml")).exists()) {
		// // // display routes
		// // }
		// // }
		// File routesNetworkFile = new File(routesFolder + "MRoutes" + thisGenNetwork +
		// ".xml");
		// System.out.println("routesNetworkFile = "+routesNetworkFile);
		// if (routesNetworkFile.exists()) {
		// Config config = ConfigUtils.createConfig();
		// config.getModules().get("network").addParam("inputNetworkFile",
		// routesNetworkFile.toString());
		// Scenario scenario = ScenarioUtils.loadScenario(config);
		// Network routesNetwork = scenario.getNetwork();
		// for (Link link : routesNetwork.getLinks().values()) { // for links in network
		// Coord fromCoordImg = zh2img(link.getFromNode().getCoord(), xScalingFactor,
		// yScalingFactor, x0, y0);
		// Coord toCoordImg = zh2img(link.getToNode().getCoord(), xScalingFactor,
		// yScalingFactor, x0, y0);
		// Integer xA = (int) Math.ceil(fromCoordImg.getX());
		// Integer yA = (int) Math.ceil(fromCoordImg.getY());
		// Integer xB = (int) Math.floor(toCoordImg.getX());
		// Integer yB = (int) Math.floor(toCoordImg.getY());
		// if (xA > xSize || xB > xSize || yA > ySize || yB > ySize) {
		// Log.write("Link "+link.toString()+" violates max window size and is therefore
		// not displayed.");
		// continue;
		// }
		//// g.draw(new Line2D.Float(30, 20, 80, 90));
		// g.drawLine(xA, yA, xB, yB);
		// }
		// } else {
		// Log.write("CAUTION: Network routes file does not exist! Jumping to its
		// parent.");
		// continue generationLoop;
		// }
		//
		// try {
		// File outputfile = new
		// File(historyLogFolder+"XbloodLineFinal"+finalNetwork+"GEN"+gen+".png");
		// ImageIO.write(blankImg, "png", outputfile);
		// } catch (IOException e) {e.printStackTrace();}
		// if (gen>firstGeneration) { // do this loop to update the network (parent)
		// which shall be displayed in generation prior to this one.
		// System.out.println("Pedigree tree GEN"+(gen-2)+" = ");
		// System.out.println(pedigreeTree.get(gen-2));
		// thisGenNetwork = pedigreeTree.get(gen-2).get(thisGenNetwork); // -2 because
		// go from gen(minimum=1) to pedigreeList(minimum=0) and need one gen before
		// System.out.println("Parent Network = "+thisGenNetwork);
		// }
		// }

		// %%% --- %% --- %% --- %% --- %% --- %% --- %% --- %% --- CALCULATE CBA --- %% --- %% --- %% --- %% --- %% --- %% --- %% --- %% --- %% --- %%%

		// int lastIterationOriginal = 100;
		//
		// String finalPlansFile1 =
		// "zurich_1pm/Zurich_1pm_SimulationOutputEnriched/ITERS/it."+lastIterationOriginal+"/"+lastIterationOriginal+".plans.xml.gz";
		// String finalPlansFile2 =
		// "zurich_1pm/Zurich_1pm_SimulationOutputBasic/ITERS/it."+lastIterationOriginal+"/"+lastIterationOriginal+".plans.xml.gz";
		// String finalPlansFile3 =
		// "zurich_1pm/Evolution/Population/Network1/Simulation_Output/ITERS/it."+lastIterationOriginal+"/"+lastIterationOriginal+".plans.xml.gz";
		//
		// CostBenefitParameters cbp1 =
		// NetworkEvolutionImpl.calculateCBAStats(finalPlansFile1,
		// "zurich_1pm/CBA_Study/cbaParameters"+lastIterationOriginal+"ZurichOriginalEnriched.xml",
		// 1);
		//
		// CostBenefitParameters cbp2 =
		// NetworkEvolutionImpl.calculateCBAStats(finalPlansFile2,
		// "zurich_1pm/CBA_Study/cbaParameters"+lastIterationOriginal+"ZurichOriginalBasic.xml",
		// 1);
		//
		// CostBenefitParameters cbp3 =
		// NetworkEvolutionImpl.calculateCBAStats(finalPlansFile3,
		// "zurich_1pm/CBA_Study/cbaParameters"+lastIterationOriginal+"Metro.xml", 1);

		// %%% --- %% --- %% --- %% --- %% --- %% --- %% --- %% --- PLOTS FROM HISTORY
		// LOG - OLD --- %% --- %% --- %% --- %% --- %% --- %% --- %% --- %% --- %% ---
		// %%%

		// int populationSize = 16;
		// int initialRoutesPerNetwork = 5;
		// int generationsToPlot = 50; // = nEvolutions (NOT nEvolutions-1 !!)
		// int lastIteration = 20;
		// String folderName = "ForExport/15_XOverPositiveProp/";
		// // 15_XOverPositiveProp (50), 16_XOverRank (50), 17_XOverTournamentSelection
		// (50), 18_XOverLog (50)
		// String inputFileName = folderName +
		// "zurich_1pm/Evolution/Population/HistoryLog/";
		// String outputFileName1 = folderName +
		// "zurich_1pm/Evolution/Population/networkTravelTimesEvo.png";
		// SimulationProcessing.travelTimesEvolutionMap(generationsToPlot,
		// populationSize, initialRoutesPerNetwork, lastIteration, inputFileName,
		// outputFileName1);
		// SimulationProcessing.travelTimesEvolutionMap(
		// generationsToPlot, populationSize, initialRoutesPerNetwork, lastIteration,
		// inputFileName, folderName +
		// "networkTravelTimesGEN"+generationsToPlot+".png");
		// String outputFileName2 = folderName +
		// "zurich_1pm/Evolution/Population/networkScoreEvo.png";
		// SimulationProcessing.scoreEvolutionMap(generationsToPlot, populationSize,
		// initialRoutesPerNetwork, lastIteration, inputFileName, outputFileName2);
		// SimulationProcessing.scoreEvolutionMap(
		// generationsToPlot, populationSize, initialRoutesPerNetwork, lastIteration,
		// inputFileName, folderName + "networkScoreEvoGEN"+generationsToPlot+".png");

		

		// %%% --- %% --- %% --- %% --- %% --- %% --- %% --- %% --- Calculate OVERALL NETWORK SCORE --- %% --- %% --- %% --- %% --- %% --- %% --- %% --- %% --- %% --- %%%

		// PrintWriter pwDefault = new
		// PrintWriter("zurich_1pm/Evolution/Population/LogDefault.txt");
		// pwDefault.close(); // Prepare empty defaultLog file for run
		// MNetworkPop mnpop = new MNetworkPop("evoNetworks");
		// MNetwork mn1 = XMLOps.readFromFile(MNetwork.class,
		// "zurich_1pm/Evolution/Population/"+"Network1"+"/M"+"Network1"+".xml");
		//// MNetwork mn2 = XMLOps.readFromFile(MNetwork.class,
		// "zurich_1pm/Evolution/Population/"+"Network2"+"/M"+"Network2"+".xml");
		// mnpop.addNetwork(mn1);
		//// mnpop.addNetwork(mn2);
		// int lastIterationOriginal = 100;
		// double populationFactor = 1000;
		//
		// Config config = ConfigUtils.createConfig();
		// config.getModules().get("network").addParam("inputNetworkFile",
		// "zurich_1pm\\Evolution\\Population\\BaseInfrastructure/GlobalNetwork.xml");
		// Scenario scenario = ScenarioUtils.loadScenario(config);
		// Network globalNetwork = scenario.getNetwork();
		//
		// @SuppressWarnings("unchecked")
		// Map<Id<Link>, CustomMetroLinkAttributes> metroLinkAttributes =
		// XMLOps.readFromFile((new HashMap<Id<Link>,
		// CustomMetroLinkAttributes>()).getClass(),
		// "zurich_1pm\\Evolution\\Population\\BaseInfrastructure/metroLinkAttributes.xml");
		//
		// for (MNetwork mn : mnpop.networkMap.values()) {
		// mn.calculateNetworkScore2(lastIterationOriginal, populationFactor,
		// globalNetwork, metroLinkAttributes); // include here also part of
		// routesHandling
		// System.out.println(mn.overallScore);
		// XMLOps.writeToFile(mn,
		// "zurich_1pm/Evolution/Population/"+mn.networkID+"/M"+mn.networkID+".xml");
		// }


				
	} // end of main method

	// %%% --- %% --- %% --- %% --- %% --- %% --- %% --- %% --- HELPER METHODS ---
	// %% --- %% --- %% --- %% --- %% --- %% --- %% --- %% --- %% --- %%%

	@SuppressWarnings("unchecked")
	public static void writeChartNetworkScore(int lastGeneration, int populationSize, int routesPerNetwork,
			int lastIteration, String inFileName, String outFileName) throws IOException {

		List<Map<String, NetworkScoreLog>> networkScoreMaps = new ArrayList<Map<String, NetworkScoreLog>>();
		networkScoreMaps.addAll(XMLOps.readFromFile(networkScoreMaps.getClass(), inFileName));

		Map<Integer, Double> generationsAverageNetworkScore = new HashMap<Integer, Double>();
		Map<Integer, Double> generationsBestNetworkScore = new HashMap<Integer, Double>();

		int g = 0;
		for (Map<String, NetworkScoreLog> networkScoreMap : networkScoreMaps.subList(0, lastGeneration - 1)) {
			g++;
			double averageNetworkScoreThisGeneration = 0.0;
			double bestNetworkScoreThisGeneration = -Double.MAX_VALUE;
			for (NetworkScoreLog nsl : networkScoreMap.values()) {
				if (nsl.overallScore > bestNetworkScoreThisGeneration) {
					bestNetworkScoreThisGeneration = nsl.overallScore;
				}
				averageNetworkScoreThisGeneration += nsl.overallScore / networkScoreMap.size();
			}
			System.out.println("Best    Network Score This Generation = " + bestNetworkScoreThisGeneration);
			System.out.println("Average Network Score This Generation = " + averageNetworkScoreThisGeneration);
			generationsAverageNetworkScore.put(g, averageNetworkScoreThisGeneration);
			generationsBestNetworkScore.put(g, bestNetworkScoreThisGeneration);
		}

//		XYLineChart chart = new XYLineChart("Perform. Evol. [nNetw=" + populationSize + "], [nSimIter=" + lastIteration
//				+ "], [nInitRoutes/Netw=" + routesPerNetwork + "]  -  MCHF", "Generation", "Score");
//		chart.addSeries("Average Network Score", generationsAverageNetworkScore);
//		chart.addSeries("Best Network Score in Generation", generationsBestNetworkScore);
//		chart.saveAsPng(outFileName, 800, 600);

		//
		JFreeChart lineChart = ChartFactory
				.createXYLineChart(
						"[#Networks=" + populationSize + "];  [#MATSimIter=" + lastIteration
								+ "];  [#InitNetworkRoutes=" + routesPerNetwork + "] \r\n ",
						"Generation", "Annual Utility [Mio CHF]", null); // dataset, PlotOrientation.VERTICAL, true,
																			// true, false
		LegendTitle legend = lineChart.getLegend();
		legend.setPosition(RectangleEdge.TOP); // RectangleEdge.RIGHT
		legend.setItemFont(new Font("Arial", Font.PLAIN, 20));
		// BufferedImage image = new BufferedImage(200, 200,
		// BufferedImage.TYPE_INT_ARGB); Graphics2D g2 = image.createGraphics();
		// g2.drawString("heeeello", 50f, 50f); Rectangle2D r2DD = new
		// Rectangle2D.Double(550, 550, 120, 180); legend.draw(g2,r2DD);

		XYPlot plot = (XYPlot) lineChart.getPlot();

		final XYSeries sAverage = new XYSeries("Average annual utility [Mio CHF]");
		for (Entry<Integer, Double> genAverageScoreEntry : generationsAverageNetworkScore.entrySet()) {
			sAverage.add((double) genAverageScoreEntry.getKey(), genAverageScoreEntry.getValue() / 1.0E6);
		}
		final XYSeries sBest = new XYSeries("Best annual utility [Mio CHF]");
		for (Entry<Integer, Double> genBestScoreEntry : generationsBestNetworkScore.entrySet()) {
			sBest.add((double) genBestScoreEntry.getKey(), genBestScoreEntry.getValue() / 1.0E6);
		}

		XYSeriesCollection dAverage = new XYSeriesCollection();
		XYSeriesCollection dBest = new XYSeriesCollection();
		dAverage.addSeries(sAverage);
		dBest.addSeries(sBest);
		XYDataset dAverageX = (XYDataset) dAverage;
		XYDataset dBestX = (XYDataset) dBest;

		XYLineAndShapeRenderer r1 = new XYLineAndShapeRenderer();
		// r1.setSeriesPaint(0, new Color(0xff, 0xff, 0x00));
		// r1.setSeriesPaint(1, new Color(0x00, 0xff, 0xff));
		r1.setSeriesPaint(0, Color.BLUE);
		r1.setSeriesShapesVisible(0, false);
		r1.setSeriesShapesVisible(1, false);
		r1.setSeriesStroke(0, new BasicStroke(5.0f));

		XYLineAndShapeRenderer r2 = new XYLineAndShapeRenderer();
		// r2.setSeriesPaint(0, new Color(0xff, 0x00, 0x00));
		// r2.setSeriesPaint(1, new Color(0x00, 0xff, 0x00));
		r2.setSeriesPaint(0, Color.RED);
		r2.setSeriesShapesVisible(0, false);
		r2.setSeriesShapesVisible(1, false);
		r2.setSeriesStroke(0, new BasicStroke(5.0f));

		plot.setDataset(0, dAverageX);
		plot.setRenderer(0, r1);
		plot.setDataset(1, dBestX);
		plot.setRenderer(1, r2);

		// NumberAxis numberAxis = new NumberAxis();
		// numberAxis.setRange(-21.0E1, 1.5E1);
		//// numberAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		// numberAxis.setTickUnit(new NumberTickUnit(3.0E1));
		// plot.setRangeAxis(numberAxis);

		Font font = new Font("Arial Bold", Font.BOLD, 30);
		NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
		domainAxis.setLabelFont(font);
		domainAxis.setTickUnit(new NumberTickUnit(3));
		domainAxis.setTickLabelFont(new Font("Arial", Font.PLAIN, 20));
		NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
		rangeAxis.setLabelFont(font);
		// rangeAxis.setRange(-21.0E1, 1.5E1);
		rangeAxis.setAutoRange(true);
		rangeAxis.setTickUnit(new NumberTickUnit(3.0E1));
		rangeAxis.setTickLabelFont(new Font("Arial", Font.PLAIN, 20));
		//
		plot.setDomainCrosshairVisible(true);
		plot.setRangeCrosshairVisible(true);
		// domainAxis.setVerticalTickLabels(true);
		//
		plot.setDomainAxis(domainAxis);
		plot.setRangeAxis(rangeAxis);

		// plot.mapDatasetToRangeAxis(1, 1); //2nd dataset to 2nd y-axi

		plot.setBackgroundPaint(new Color(0xFF, 0xFF, 0xFF));
		plot.setDomainGridlinePaint(new Color(0x00, 0x00, 0xff));
		plot.setRangeGridlinePaint(new Color(0xff, 0x00, 0x00));

//		Log.write("should be making png here!");
		
		File file = new File(outFileName);
		ChartUtilities.saveChartAsPNG(file, lineChart, 1280, 960);

	}

	@SuppressWarnings("unchecked")
	public static void writeChartAverageTravelTimes(int lastGeneration, int populationSize, int routesPerNetwork,
			int lastIteration, String inFileName, String outFileName) throws FileNotFoundException {

		List<Map<String, NetworkScoreLog>> networkScoreMaps = new ArrayList<Map<String, NetworkScoreLog>>();
		networkScoreMaps.addAll(XMLOps.readFromFile(networkScoreMaps.getClass(), inFileName));

		Map<Integer, Double> generationsAverageTravelTime = new HashMap<Integer, Double>();
		Map<Integer, Double> generationsAverageTravelTimeStdDev = new HashMap<Integer, Double>();
		Map<Integer, Double> generationsBestTravelTime = new HashMap<Integer, Double>();

		int g = 0;
		for (Map<String, NetworkScoreLog> networkScoreMap : networkScoreMaps.subList(0, lastGeneration - 1)) {
			g++;
			double averageTravelTimeThisGeneration = 0.0;
			double averageTravelTimeStdDevThisGeneration = 0.0;
			double bestAverageTravelTimeThisGeneration = Double.MAX_VALUE;
			for (NetworkScoreLog nsl : networkScoreMap.values()) {
				if (nsl.averageTravelTime < bestAverageTravelTimeThisGeneration) {
					bestAverageTravelTimeThisGeneration = nsl.averageTravelTime;
				}
				averageTravelTimeThisGeneration += nsl.averageTravelTime / networkScoreMap.size();
				averageTravelTimeStdDevThisGeneration += nsl.stdDeviationTravelTime / networkScoreMap.size();
			}
			System.out.println("bestAverageTravelTimeThisGeneration = " + bestAverageTravelTimeThisGeneration);
			System.out.println("Average AverageTravelTime This Generation = " + averageTravelTimeThisGeneration);
			generationsAverageTravelTime.put(g, averageTravelTimeThisGeneration);
			generationsAverageTravelTimeStdDev.put(g, averageTravelTimeStdDevThisGeneration);
			generationsBestTravelTime.put(g, bestAverageTravelTimeThisGeneration);
		}
		XYLineChart chart = new XYLineChart("Perform. Evol. [nNetw=" + populationSize + "], [nSimIter=" + lastIteration
				+ "], [nInitRoutes/Netw=" + routesPerNetwork + "]", "Generation", "Score");
		chart.addSeries("Average Travel Time [min]", generationsAverageTravelTime);
		chart.addSeries("Average Travel Time - Std Deviation [min]", generationsAverageTravelTimeStdDev);
		chart.addSeries("Best Average Travel Time [min]", generationsBestTravelTime);
		chart.saveAsPng(outFileName, 800, 600);
	}

	public static void plot2D(String title, String xAxisName, String yAxisName, List<Map<Integer, Double>> inputSeries,
			List<String> inputSeriesName, Double tickUnitX, Double tickUnitY, Range yRange, String outFileName)
			throws IOException {

//		 old version charts
//		 XYLineChart chart = new XYLineChart(title, xAxisName, yAxisName);
//		 for (Integer seriesNr=0; seriesNr<inputSeries.size(); seriesNr++) {
//		 chart.addSeries(inputSeriesName.get(seriesNr), inputSeries.get(seriesNr));
//		 }
//		 chart.saveAsPng("x"+outFileName, 800, 600);

		// new version
		JFreeChart lineChart = ChartFactory.createXYLineChart(title, xAxisName, yAxisName, null);
		LegendTitle legend = lineChart.getLegend();
		legend.setPosition(RectangleEdge.TOP); // RectangleEdge.RIGHT
		legend.setItemFont(new Font("Arial", Font.PLAIN, 30));
		TextTitle plotTitle = lineChart.getTitle();
		plotTitle.setFont(new Font("Arial Bold", Font.BOLD, 35));

		XYPlot plot = (XYPlot) lineChart.getPlot();
		List<XYDataset> dataSets = new ArrayList<XYDataset>();
		for (Integer seriesNr = 0; seriesNr < inputSeries.size(); seriesNr++) {
			final XYSeries thisSeries = new XYSeries(inputSeriesName.get(seriesNr));
			for (Entry<Integer, Double> inputSeriesEntry : inputSeries.get(seriesNr).entrySet()) {
				thisSeries.add((double) inputSeriesEntry.getKey(), inputSeriesEntry.getValue());
			}
			 XYSeriesCollection thisSeriesCollection = new XYSeriesCollection();
			 thisSeriesCollection.addSeries(thisSeries);
			 dataSets.add((XYDataset) thisSeriesCollection);
//			dataSets.add((XYDataset) thisSeries);
		}

		List<Color> defaultColors = Arrays.asList(Color.RED, Color.BLUE, Color.GREEN, Color.CYAN, Color.MAGENTA,
				Color.BLACK, Color.ORANGE, Color.GRAY, Color.YELLOW);

		for (Integer dataSetNr = 0; dataSetNr < dataSets.size(); dataSetNr++) {
			XYLineAndShapeRenderer r = new XYLineAndShapeRenderer();
			r.setSeriesPaint(0, defaultColors.get(dataSetNr));
			r.setSeriesShapesVisible(0, false);
			r.setSeriesShapesVisible(1, false);
			r.setSeriesStroke(0, new BasicStroke(4.0f));
			plot.setDataset(dataSetNr, dataSets.get(dataSetNr));
			plot.setRenderer(dataSetNr, r);
		}

		Font font = new Font("Arial Bold", Font.BOLD, 40);
		NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
		domainAxis.setLabelFont(font);
		if (tickUnitX > 0.0) {
			domainAxis.setTickUnit(new NumberTickUnit(tickUnitX));
		}
		domainAxis.setTickLabelFont(new Font("Arial", Font.PLAIN, 40));
		NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
		rangeAxis.setLabelFont(font);
		if (yRange != null) {
			rangeAxis.setRange(yRange);
		} else {
			rangeAxis.setAutoRange(true);
		}
		if (!tickUnitY.equals(null) && tickUnitY > 0.0) {
			rangeAxis.setTickUnit(new NumberTickUnit(tickUnitY));
		}
		rangeAxis.setTickLabelFont(new Font("Arial", Font.PLAIN, 40));

		plot.setDomainCrosshairVisible(true);
		plot.setRangeCrosshairVisible(true);
		plot.setDomainAxis(domainAxis);
		plot.setRangeAxis(rangeAxis);
		plot.setBackgroundPaint(new Color(0xFF, 0xFF, 0xFF));
		plot.setDomainGridlinePaint(new Color(0x00, 0x00, 0xff));
		plot.setRangeGridlinePaint(new Color(0xff, 0x00, 0x00));

		File file = new File(outFileName);
		ChartUtilities.saveChartAsPNG(file, lineChart, 1280, 960);
	}
	
	public static void plot2DConfIntervals(String title, String xAxisName, String yAxisName, List<Map<Integer, Double>> inputSeries,
			List<String> inputSeriesName, Double tickUnitX, Double tickUnitY, Range yRange, String outFileName, List<List<Double>> confIntervals)
			throws IOException {

//		 old version charts
//		 XYLineChart chart = new XYLineChart(title, xAxisName, yAxisName);
//		 for (Integer seriesNr=0; seriesNr<inputSeries.size(); seriesNr++) {
//		 chart.addSeries(inputSeriesName.get(seriesNr), inputSeries.get(seriesNr));
//		 }
//		 chart.saveAsPng("x"+outFileName, 800, 600);

		// new version
		JFreeChart lineChart = ChartFactory.createXYLineChart(title, xAxisName, yAxisName, null);
		LegendTitle legend = lineChart.getLegend();
		legend.setPosition(RectangleEdge.TOP); // RectangleEdge.RIGHT
		legend.setItemFont(new Font("Arial", Font.PLAIN, 30));
		TextTitle plotTitle = lineChart.getTitle();
		plotTitle.setFont(new Font("Arial Bold", Font.BOLD, 35));

		XYPlot plot = (XYPlot) lineChart.getPlot();
		List<XYDataset> dataSets = new ArrayList<XYDataset>();
		for (Integer seriesNr = 0; seriesNr < inputSeries.size(); seriesNr++) {
			final XYSeries thisSeries = new XYSeries(inputSeriesName.get(seriesNr));
			for (Entry<Integer, Double> inputSeriesEntry : inputSeries.get(seriesNr).entrySet()) {
				thisSeries.add((double) inputSeriesEntry.getKey(), inputSeriesEntry.getValue());
			}
			 XYSeriesCollection thisSeriesCollection = new XYSeriesCollection();
			 thisSeriesCollection.addSeries(thisSeries);
			 dataSets.add((XYDataset) thisSeriesCollection);
//			dataSets.add((XYDataset) thisSeries);
		}

		List<Color> defaultColors = Arrays.asList(Color.RED, Color.BLUE, Color.GREEN, Color.CYAN, Color.MAGENTA,
				Color.BLACK, Color.ORANGE, Color.GRAY, Color.YELLOW);

		
		for (Integer confIntNr = 0; confIntNr < confIntervals.size(); confIntNr++) {
			final IntervalMarker confInterval = new IntervalMarker(confIntervals.get(confIntNr).get(0), confIntervals.get(confIntNr).get(1));
			confInterval.setLabel("90th Percentile");
			confInterval.setLabelFont(new Font("SansSerif", Font.ITALIC, 20));
			confInterval.setLabelAnchor(RectangleAnchor.RIGHT);
			confInterval.setLabelTextAnchor(TextAnchor.CENTER_RIGHT);
			confInterval.setPaint(
					new Color(defaultColors.get(confIntNr).getRed(), defaultColors.get(confIntNr).getGreen(), defaultColors.get(confIntNr).getBlue(), 90));
			plot.addRangeMarker(confInterval, Layer.BACKGROUND);
		}

	    
		for (Integer dataSetNr = 0; dataSetNr < dataSets.size(); dataSetNr++) {
			XYLineAndShapeRenderer r = new XYLineAndShapeRenderer();
			r.setSeriesPaint(0, defaultColors.get(dataSetNr));
			r.setSeriesShapesVisible(0, false);
			r.setSeriesShapesVisible(1, false);
			r.setSeriesStroke(0, new BasicStroke(4.0f));
			plot.setDataset(dataSetNr, dataSets.get(dataSetNr));
			plot.setRenderer(dataSetNr, r);
		}

		Font font = new Font("Arial Bold", Font.BOLD, 40);
		NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
		domainAxis.setLabelFont(font);
		if (tickUnitX > 0.0) {
			domainAxis.setTickUnit(new NumberTickUnit(tickUnitX));
		}
		domainAxis.setTickLabelFont(new Font("Arial", Font.PLAIN, 40));
		NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
		rangeAxis.setLabelFont(font);
		if (yRange != null) {
			rangeAxis.setRange(yRange);
		} else {
			rangeAxis.setAutoRange(true);
		}
		if (!tickUnitY.equals(null) && tickUnitY > 0.0) {
			rangeAxis.setTickUnit(new NumberTickUnit(tickUnitY));
		}
		rangeAxis.setTickLabelFont(new Font("Arial", Font.PLAIN, 40));

		plot.setDomainCrosshairVisible(true);
		plot.setRangeCrosshairVisible(true);
		plot.setDomainAxis(domainAxis);
		plot.setRangeAxis(rangeAxis);
		plot.setBackgroundPaint(new Color(0xFF, 0xFF, 0xFF));
		plot.setDomainGridlinePaint(new Color(0x00, 0x00, 0xff));
		plot.setRangeGridlinePaint(new Color(0xff, 0x00, 0x00));

		File file = new File(outFileName);
		ChartUtilities.saveChartAsPNG(file, lineChart, 1280, 960);
	}

	public static Coord zh2img(Coord zhCoord, Double xScalingFactor, Double yScalingFactor, Double x0, Double y0) {
		Double xImg = (zhCoord.getX() - x0) / xScalingFactor;
		Double yImg = (zhCoord.getY() - y0) / yScalingFactor;
		return new Coord(xImg, yImg);
	}

}
