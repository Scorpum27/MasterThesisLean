package ch.ethz.matsim.students.samark;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.imageio.ImageIO;
import javax.xml.stream.XMLStreamException;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.Range;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleEdge;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.charts.XYLineChart;

public class Visualizer {
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws IOException, XMLStreamException {

		// %%% --- %% --- %% --- %% --- %% --- %% --- %% --- %% --- DISPLAY EVOLUTION OF
		// NETWORKS --- %% --- %% --- %% --- %% --- %% --- %% --- %% --- %% --- %% ---
		// %%%

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

		Log.write("should be making png here!");
		
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

		// old version charts
		// XYLineChart chart = new XYLineChart(title, xAxisName, yAxisName);
		// for (Integer seriesNr=0; seriesNr<inputSeries.size(); seriesNr++) {
		// chart.addSeries(inputSeriesName.get(seriesNr), inputSeries.get(seriesNr));
		// }
		// chart.saveAsPng(outFileName, 800, 600);

		// new version
		JFreeChart lineChart = ChartFactory.createXYLineChart(title, xAxisName, yAxisName, null);
		LegendTitle legend = lineChart.getLegend();
		legend.setPosition(RectangleEdge.TOP); // RectangleEdge.RIGHT
		legend.setItemFont(new Font("Arial", Font.PLAIN, 20));

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

		Font font = new Font("Arial Bold", Font.BOLD, 30);
		NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
		domainAxis.setLabelFont(font);
		if (tickUnitX > 0.0) {
			domainAxis.setTickUnit(new NumberTickUnit(tickUnitX));
		}
		domainAxis.setTickLabelFont(new Font("Arial", Font.PLAIN, 20));
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
		rangeAxis.setTickLabelFont(new Font("Arial", Font.PLAIN, 20));

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
