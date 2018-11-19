package ch.ethz.matsim.students.samark;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
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

public class VisualizerPedigree {
	
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws IOException, XMLStreamException, URISyntaxException {
	// %%% --- %% --- %% --- %% --- %% --- %% --- %% --- %% --- DISPLAY EVOLUTION OF NETWORKS---%%---%%---%%---%%---%%---%%---%%---%%---%%---%%%
	
		// args[0] = Sim folder name;
		// args[1] = firstGenerationPedigreeTree;
		// args[2] = finalGenerationPedigreeTree;
		// args[3] = networkName;
		
		final Integer finalNetworkNrFirst = Integer.parseInt(args[3]); // e.g. "1"
		final Integer finalNetworkNrLast = Integer.parseInt(args[4]); // e.g. "10"
		String simName = args[0];
					
		BufferedImage bgImage = null;
		bgImage = ImageIO.read(new FileInputStream(simName + "/zurich_1pm/bgImgMedium.png"));
		// bgImage = ImageIO.read(BufferedImage.class.getResource(simName + "/zurich_1pm/bgImgMedium.png"));
		// bgImage = ImageIO.read(new File(BufferedImage.class.getResource(simName + "/zurich_1pm/bgImgMedium.png").toURI()));
		Double xSize = (double) bgImage.getWidth();
		Double ySize = (double) bgImage.getHeight();
				
		//	Coord zurich_NetworkCenterCoord = new Coord(2683360.00, 1248100.00);		// default Coord(2683360.00,1248100.00);  old:(2683000.00, 1247700.00)
		//	Double xOffset = 1733436.0; 												// add this to QGis to get MATSim		// Right upper corner of Zürisee -- X_QGis=950040; 																					  																						X_MATSim= 2683476;
		//	Double yOffset = -4748525.0;												// add this to QGis to get MATSim		// Right upper corner of Zürisee -- Y_QGis=5995336;
		final Double x0 = 2669398.0; // top left corner of background image
		final Double y0 = 1256012.0; // top left corner of background image
		final Double x1 = 2696233.0; // bottom right corner of background image
		final Double y1 = 1240325.0; // bottom right corner of background image
		Double xScalingFactor = (x1 - x0) / xSize;
		Double yScalingFactor = (y1 - y0) / ySize; // will be negative due to y-decrease of COS for increasing graphicsCOS

		List<Map<String, String>> pedigreeTree = new ArrayList<Map<String, String>>();
		String historyLogFolder = simName + "/zurich_1pm/Evolution/Population/HistoryLog/";
		pedigreeTree.addAll(XMLOps.readFromFile(pedigreeTree.getClass(), historyLogFolder + "pedigreeTree.xml"));
		System.out.println("pedigreeTreeSize = " + pedigreeTree.size());

		final Integer firstGeneration;
		final Integer finalGeneration;
		if (args[1].equals("null")) {
			firstGeneration = 1;
		} else {
			firstGeneration = Integer.parseInt(args[1]);
		}
		if (args[2].equals("null")) {
			finalGeneration = pedigreeTree.size() + 1;
		} else {
			finalGeneration = Integer.parseInt(args[2]);
		}
		if (args[2].equals("null")) {
			System.out.println(args[1]);
		}

		if (pedigreeTree.size() != finalGeneration - 1) {
//			Log.writeAndDisplay(
//					"CAUTION: Pedigree tree has not same number of generations as finalGeneration! Please check.");
		}

		for (Integer n=finalNetworkNrFirst; n<=finalNetworkNrLast; n++) {
			String finalNetwork = "Network"+n;
			String thisGenNetwork = finalNetwork;
			generationLoop:
			for (Integer gen = finalGeneration; gen >= firstGeneration; gen--) {
	
				BufferedImage blankImg = null;
				blankImg = ImageIO.read(new File(simName + "/zurich_1pm/bgImgMedium.png"));
				Graphics2D g = (Graphics2D) blankImg.createGraphics();
				g.setColor(Color.CYAN);
				g.setStroke(new BasicStroke(3));
	
				String routesFolder = historyLogFolder + "Generation" + gen + "/MRoutes/";
	
				// alternative: display all MRoutes individually (can display with different colors then)
				// for (Integer r=0; r<50; r++) {
				// 		if ((new File("routesFolder" + thisGenNetwork + "_Route"+r +"_RoutesFile.xml")).exists()) {
				// 		// display routes
				// 		}
				// }
				File routesNetworkFile = new File(routesFolder + "MRoutes" + thisGenNetwork + ".xml");
				System.out.println("routesNetworkFile = " + routesNetworkFile);
				if (routesNetworkFile.exists()) {
					
					Config config = ConfigUtils.createConfig();
					config.getModules().get("network").addParam("inputNetworkFile", routesNetworkFile.toString());
					Scenario scenario = ScenarioUtils.loadScenario(config);
					Network routesNetwork = scenario.getNetwork();
					for (Link link : routesNetwork.getLinks().values()) { // for links in network
						Coord fromCoordImg = zh2img(link.getFromNode().getCoord(), xScalingFactor, yScalingFactor, x0, y0);
						Coord toCoordImg = zh2img(link.getToNode().getCoord(), xScalingFactor, yScalingFactor, x0, y0);
						Integer xA = (int) Math.ceil(fromCoordImg.getX());
						Integer yA = (int) Math.ceil(fromCoordImg.getY());
						Integer xB = (int) Math.floor(toCoordImg.getX());
						Integer yB = (int) Math.floor(toCoordImg.getY());
						if (xA > xSize || xB > xSize || yA > ySize || yB > ySize) {
	//						Log.write("Link " + link.toString()
	//								+ " violates max window size and is therefore not displayed.");
							continue;
						}
						// g.draw(new Line2D.Float(30, 20, 80, 90));
						g.drawLine(xA, yA, xB, yB);
					}
				} else {
	//				Log.write("CAUTION: Network routes file does not exist! Jumping to its parent.");
					continue generationLoop;
				}
	
				try {
					File outputfile = new File(simName + "/PedigreeBloodLine_" + finalNetwork + "GEN" + gen + ".png");
					ImageIO.write(blankImg, "png", outputfile);
				} catch (IOException e) {
					e.printStackTrace();
				}
				if (gen > firstGeneration) { // do this loop to update the network (parent) which shall be displayed in
												// generation prior to this one.
					System.out.println("Pedigree tree GEN" + (gen - 2) + " = ");
					System.out.println(pedigreeTree.get(gen - 2));
					thisGenNetwork = pedigreeTree.get(gen - 2).get(thisGenNetwork); // -2 because go from gen(minimum=1) to
																					// pedigreeList(minimum=0) and need one
																					// gen before
					System.out.println("Parent Network = " + thisGenNetwork);
				}
			}
		}
	}

	public static Coord zh2img(Coord zhCoord, Double xScalingFactor, Double yScalingFactor, Double x0, Double y0) {
		Double xImg = (zhCoord.getX() - x0) / xScalingFactor;
		Double yImg = (zhCoord.getY() - y0) / yScalingFactor;
		return new Coord(xImg, yImg);
	}

}
