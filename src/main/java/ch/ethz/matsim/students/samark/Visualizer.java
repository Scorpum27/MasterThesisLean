package ch.ethz.matsim.students.samark;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.xml.stream.XMLStreamException;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class Visualizer {
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws IOException, XMLStreamException {

	
// %%% --- %% --- %% --- %% --- %% --- %% --- %% --- %% --- DISPLAY EVOLUTION OF NETWORKS --- %% --- %% --- %% --- %% --- %% --- %% --- %% --- %% --- %% --- %%%
			
//		BufferedImage bgImage = null;
//		bgImage = ImageIO.read(new File("zurich_1pm/bgImgMedium.png"));
//		Double xSize = (double) bgImage.getWidth();
//		Double ySize = (double) bgImage.getHeight();
//		
//		//	Coord zurich_NetworkCenterCoord = new Coord(2683360.00, 1248100.00);		// default Coord(2683360.00,1248100.00);  old:(2683000.00, 1247700.00)
//		//	Double xOffset = 1733436.0; 												// add this to QGis to get MATSim		// Right upper corner of Zürisee -- X_QGis=950040; 																					  																						X_MATSim= 2683476;
//		//	Double yOffset = -4748525.0;												// add this to QGis to get MATSim		// Right upper corner of Zürisee -- Y_QGis=5995336;
//		final Double x0 = 2669398.0;	// top left corner of background image
//		final Double y0 = 1256012.0;	// top left corner of background image
//		final Double x1 = 2696233.0;	// bottom right corner of background image
//		final Double y1 = 1240325.0;	// bottom right corner of background image
//		Double xScalingFactor = (x1-x0)/xSize;
//		Double yScalingFactor = (y1-y0)/ySize; // will be negative due to y-decrease of COS for increasing graphicsCOS
//	
//		String finalNetwork = "Network2";
//		final Integer firstGeneration = 1;
//		final Integer finalGeneration = 29;
//		List<Map<String, String>> pedigreeTree = new ArrayList<Map<String, String>>();
////		String historyLogFolder = "ForExport/19_UtilityAndFreq/1_smallMetroRadiusLoweredCost/zurich_1pm/Evolution/Population/HistoryLog/"; // GEN=1/38
//		String historyLogFolder = "ForExport/19_UtilityAndFreq/3_longBlockLoweredDisutilityThreshold/zurich_1pm/Evolution/Population/HistoryLog/"; // GEN=1/29
//		pedigreeTree.addAll(XMLOps.readFromFile(pedigreeTree.getClass(), historyLogFolder+"pedigreeTree.xml"));
//		System.out.println("pedigreeTreeSize = "+pedigreeTree.size());
//		if (pedigreeTree.size() != finalGeneration-1) {
//			Log.writeAndDisplay("CAUTION: Pedigree tree has not same number of generations as finalGeneration! Please check. Aborting ...");
//		}
//		
//		String thisGenNetwork = finalNetwork;
//		generationLoop:
//		for (Integer gen=finalGeneration; gen>=firstGeneration; gen--) {
//			
//			BufferedImage blankImg = null;
//			blankImg = ImageIO.read(new File("zurich_1pm/bgImgMedium.png"));
//			Graphics2D g = (Graphics2D) blankImg.createGraphics();
//		    g.setColor(Color.CYAN);
//	        g.setStroke(new BasicStroke(3));
//		    
//		    String routesFolder = historyLogFolder+"Generation"+gen+"/MRoutes/";
//			    
//			// alternative: display all MRoutes individually (can display with different colors then)
//			// for (Integer r=0; r<50; r++) {
//		    // 		if ((new File("routesFolder" + thisGenNetwork  + "_Route"+r +"_RoutesFile.xml")).exists()) {
//			//		// display routes
//			//		}
//		   	// }
//		    File routesNetworkFile = new File(routesFolder + "MRoutes" + thisGenNetwork + ".xml");
//			System.out.println("routesNetworkFile = "+routesNetworkFile);
//			if (routesNetworkFile.exists()) {
//				Config config = ConfigUtils.createConfig();
//				config.getModules().get("network").addParam("inputNetworkFile", routesNetworkFile.toString());
//				Scenario scenario = ScenarioUtils.loadScenario(config);
//				Network routesNetwork = scenario.getNetwork();
//				for (Link link : routesNetwork.getLinks().values()) { // for links in network
//					Coord fromCoordImg = zh2img(link.getFromNode().getCoord(), xScalingFactor, yScalingFactor, x0, y0);
//					Coord toCoordImg = zh2img(link.getToNode().getCoord(), xScalingFactor, yScalingFactor, x0, y0);
//					Integer xA = (int) Math.ceil(fromCoordImg.getX());
//					Integer yA = (int) Math.ceil(fromCoordImg.getY());
//					Integer xB = (int) Math.floor(toCoordImg.getX());
//					Integer yB = (int) Math.floor(toCoordImg.getY());
//					if (xA > xSize || xB > xSize || yA > ySize || yB > ySize) {
//						Log.write("Link "+link.toString()+" violates max window size and is therefore not displayed.");
//						continue;
//					}
////					g.draw(new Line2D.Float(30, 20, 80, 90));
//					g.drawLine(xA, yA, xB, yB);
//				}
//			} else {
//				Log.write("CAUTION: Network routes file does not exist! Jumping to its parent.");
//				continue generationLoop;
//			} 
//			    	
//		    try {
//		   	    File outputfile = new File(historyLogFolder+"XbloodLineFinal"+finalNetwork+"GEN"+gen+".png");
//		   	    ImageIO.write(blankImg, "png", outputfile);
//		    } catch (IOException e) {e.printStackTrace();}
//		    if (gen>firstGeneration) { // do this loop to update the network (parent) which shall be displayed in generation prior to this one.
//		    	System.out.println("Pedigree tree GEN"+(gen-2)+" = ");
//		    	System.out.println(pedigreeTree.get(gen-2));
//		    	thisGenNetwork = pedigreeTree.get(gen-2).get(thisGenNetwork);	// -2 because go from gen(minimum=1) to pedigreeList(minimum=0) and need one gen before
//		    	System.out.println("Parent Network = "+thisGenNetwork);
//		    }
//		}
		
// %%% --- %% --- %% --- %% --- %% --- %% --- %% --- %% --- CALCULATE CBA --- %% --- %% --- %% --- %% --- %% --- %% --- %% --- %% --- %% --- %%%

		
//		int lastIterationOriginal = 100;
//		
//		String finalPlansFile1 = "zurich_1pm/Zurich_1pm_SimulationOutputEnriched/ITERS/it."+lastIterationOriginal+"/"+lastIterationOriginal+".plans.xml.gz";
//		String finalPlansFile2 = "zurich_1pm/Zurich_1pm_SimulationOutputBasic/ITERS/it."+lastIterationOriginal+"/"+lastIterationOriginal+".plans.xml.gz";
//		String finalPlansFile3 = "zurich_1pm/Evolution/Population/Network1/Simulation_Output/ITERS/it."+lastIterationOriginal+"/"+lastIterationOriginal+".plans.xml.gz";
//		
//		CostBenefitParameters cbp1 = NetworkEvolutionImpl.calculateCBAStats(finalPlansFile1,
//				"zurich_1pm/CBA_Study/cbaParameters"+lastIterationOriginal+"ZurichOriginalEnriched.xml", 1);
//		
//		CostBenefitParameters cbp2 = NetworkEvolutionImpl.calculateCBAStats(finalPlansFile2,
//				"zurich_1pm/CBA_Study/cbaParameters"+lastIterationOriginal+"ZurichOriginalBasic.xml", 1);
//		
//		CostBenefitParameters cbp3 = NetworkEvolutionImpl.calculateCBAStats(finalPlansFile3,
//				"zurich_1pm/CBA_Study/cbaParameters"+lastIterationOriginal+"Metro.xml", 1);	

		
// %%% --- %% --- %% --- %% --- %% --- %% --- %% --- %% --- PLOTS FROM HISTORY LOG - OLD --- %% --- %% --- %% --- %% --- %% --- %% --- %% --- %% --- %% --- %%%
		
//		int populationSize = 16;
//		int initialRoutesPerNetwork = 5;
//		int generationsToPlot = 50; // = nEvolutions    (NOT nEvolutions-1 !!)
//		int lastIteration = 20;
//		String folderName = "ForExport/15_XOverPositiveProp/";
//		// 15_XOverPositiveProp (50), 16_XOverRank (50), 17_XOverTournamentSelection (50), 18_XOverLog (50)
//		String inputFileName = folderName + "zurich_1pm/Evolution/Population/HistoryLog/";
//		String outputFileName1 = folderName + "zurich_1pm/Evolution/Population/networkTravelTimesEvo.png";
//		SimulationProcessing.travelTimesEvolutionMap(generationsToPlot, populationSize, initialRoutesPerNetwork, lastIteration, inputFileName, outputFileName1);
//		SimulationProcessing.travelTimesEvolutionMap(
//			generationsToPlot, populationSize, initialRoutesPerNetwork, lastIteration, inputFileName, folderName + "networkTravelTimesGEN"+generationsToPlot+".png");
//		String outputFileName2 = folderName + "zurich_1pm/Evolution/Population/networkScoreEvo.png";
//		SimulationProcessing.scoreEvolutionMap(generationsToPlot, populationSize, initialRoutesPerNetwork, lastIteration, inputFileName, outputFileName2);
//		SimulationProcessing.scoreEvolutionMap(
//			generationsToPlot, populationSize, initialRoutesPerNetwork, lastIteration, inputFileName, folderName + "networkScoreEvoGEN"+generationsToPlot+".png");
		
// %%% --- %% --- %% --- %% --- %% --- %% --- %% --- %% --- PLOTS FROM HISTORY LOG - NEW --- %% --- %% --- %% --- %% --- %% --- %% --- %% --- %% --- %% --- %%%
		
		int populationSize = 8;
		int initialRoutesPerNetwork = 5;
		int generationsToPlot = 29; // = nEvolutions    (NOT nEvolutions-1 !!)
		int lastIteration = 20;
		String folderName = "ForExport/19_UtilityAndFreq/3_longBlockLoweredDisutilityThreshold/";
		// 1_smallMetroRadiusLoweredCost (38), 2_lowCost20percent (34), 3_longBlockLoweredDisutilityThreshold (29), 4_noFreqModNoLengthMod (38)
		String inputFileName = folderName + "zurich_1pm/Evolution/Population/networkScoreMaps.xml";
		NetworkEvolutionImpl.writeChartAverageTravelTimes(generationsToPlot, populationSize, initialRoutesPerNetwork, lastIteration,
				inputFileName, folderName+"zurich_1pm/Evolution/Population/networkTravelTimesGEN"+generationsToPlot+".png");
		NetworkEvolutionImpl.writeChartNetworkScore(generationsToPlot, populationSize, initialRoutesPerNetwork, lastIteration,
				inputFileName, folderName+"zurich_1pm/Evolution/Population/networkScoreEvoGEN"+generationsToPlot+".png");
		
// %%% --- %% --- %% --- %% --- %% --- %% --- %% --- %% --- Calculate OVERALL NETWORK SCORE --- %% --- %% --- %% --- %% --- %% --- %% --- %% --- %% --- %% --- %%%
	
//		PrintWriter pwDefault = new PrintWriter("zurich_1pm/Evolution/Population/LogDefault.txt");	pwDefault.close();	// Prepare empty defaultLog file for run
//		MNetworkPop mnpop = new MNetworkPop("evoNetworks");
//		MNetwork mn1 = XMLOps.readFromFile(MNetwork.class, "zurich_1pm/Evolution/Population/"+"Network1"+"/M"+"Network1"+".xml");
////		MNetwork mn2 = XMLOps.readFromFile(MNetwork.class, "zurich_1pm/Evolution/Population/"+"Network2"+"/M"+"Network2"+".xml");
//		mnpop.addNetwork(mn1);
////		mnpop.addNetwork(mn2);
//		int lastIterationOriginal = 100;
//		double populationFactor = 1000;
//		
//		Config config = ConfigUtils.createConfig();
//		config.getModules().get("network").addParam("inputNetworkFile", "zurich_1pm\\Evolution\\Population\\BaseInfrastructure/GlobalNetwork.xml");
//		Scenario scenario = ScenarioUtils.loadScenario(config);
//		Network globalNetwork = scenario.getNetwork();
//		
//		@SuppressWarnings("unchecked")
//		Map<Id<Link>, CustomMetroLinkAttributes> metroLinkAttributes =
//				XMLOps.readFromFile((new HashMap<Id<Link>, CustomMetroLinkAttributes>()).getClass(), "zurich_1pm\\Evolution\\Population\\BaseInfrastructure/metroLinkAttributes.xml");
//		
//		for (MNetwork mn : mnpop.networkMap.values()) {
//			mn.calculateNetworkScore2(lastIterationOriginal, populationFactor, globalNetwork, metroLinkAttributes); // include here also part of routesHandling
//			System.out.println(mn.overallScore);
//			XMLOps.writeToFile(mn, "zurich_1pm/Evolution/Population/"+mn.networkID+"/M"+mn.networkID+".xml");
//		}
	

	} // end of main method
	
	
	
	
	
// %%% --- %% --- %% --- %% --- %% --- %% --- %% --- %% --- HELPER METHODS --- %% --- %% --- %% --- %% --- %% --- %% --- %% --- %% --- %% --- %%%

	public static Coord zh2img(Coord zhCoord, Double xScalingFactor, Double yScalingFactor, Double x0, Double y0) {
		Double xImg = (zhCoord.getX()-x0)/xScalingFactor;
		Double yImg = (zhCoord.getY()-y0)/yScalingFactor;
		return new Coord(xImg, yImg);
	}
	
	
	
	
}
