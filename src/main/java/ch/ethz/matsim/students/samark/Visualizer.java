package ch.ethz.matsim.students.samark;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

public class Visualizer {


	public static void main(String[] args) throws IOException, XMLStreamException {

		// %%% Calculate CBA %%%
		
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
		
	// %%% START - Plots From History Log %%%
		
		int populationSize = 16;
		int initialRoutesPerNetwork = 5;
		int generationsToPlot = 14; // = nEvolutions    (NOT nEvolutions-1 !!)
		int lastIteration = 20;
		String folderName = "ForExport/17_XOverTournamentSelection/";
		String inputFileName = folderName + "zurich_1pm/Evolution/Population/HistoryLog/";
		String outputFileName1 = folderName + "zurich_1pm/Evolution/Population/networkTravelTimesEvo.png";
		SimulationProcessing.travelTimesEvolutionMap(generationsToPlot, populationSize, initialRoutesPerNetwork, lastIteration, inputFileName, outputFileName1);
		SimulationProcessing.travelTimesEvolutionMap(
				generationsToPlot, populationSize, initialRoutesPerNetwork, lastIteration, inputFileName, folderName + "networkTravelTimes14GEN.png");
		String outputFileName2 = folderName + "zurich_1pm/Evolution/Population/networkScoreEvo.png";
		SimulationProcessing.scoreEvolutionMap(generationsToPlot, populationSize, initialRoutesPerNetwork, lastIteration, inputFileName, outputFileName2);
		SimulationProcessing.scoreEvolutionMap(
				generationsToPlot, populationSize, initialRoutesPerNetwork, lastIteration, inputFileName, folderName + "networkScoreEvo14GEN.png");
		
		
		
	// %%% Calculate OVERALL NETWORK SCORE %%%
	
		/*PrintWriter pwDefault = new PrintWriter("zurich_1pm/Evolution/Population/LogDefault.txt");	pwDefault.close();	// Prepare empty defaultLog file for run
		
		MNetworkPop mnpop = new MNetworkPop("evoNetworks");
	
		MNetwork mn1 = XMLOps.readFromFile(MNetwork.class, "zurich_1pm/Evolution/Population/"+"Network1"+"/M"+"Network1"+".xml");
//		MNetwork mn2 = XMLOps.readFromFile(MNetwork.class, "zurich_1pm/Evolution/Population/"+"Network2"+"/M"+"Network2"+".xml");
		mnpop.addNetwork(mn1);
//		mnpop.addNetwork(mn2);
		int lastIterationOriginal = 100;
		double populationFactor = 1000;
		
		Config config = ConfigUtils.createConfig();
		config.getModules().get("network").addParam("inputNetworkFile", "zurich_1pm\\Evolution\\Population\\BaseInfrastructure/GlobalNetwork.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Network globalNetwork = scenario.getNetwork();
		
		@SuppressWarnings("unchecked")
		Map<Id<Link>, CustomMetroLinkAttributes> metroLinkAttributes =
				XMLOps.readFromFile((new HashMap<Id<Link>, CustomMetroLinkAttributes>()).getClass(), "zurich_1pm\\Evolution\\Population\\BaseInfrastructure/metroLinkAttributes.xml");
		
		for (MNetwork mn : mnpop.networkMap.values()) {
			mn.calculateNetworkScore2(lastIterationOriginal, populationFactor, globalNetwork, metroLinkAttributes); // include here also part of routesHandling
			System.out.println(mn.overallScore);
			XMLOps.writeToFile(mn, "zurich_1pm/Evolution/Population/"+mn.networkID+"/M"+mn.networkID+".xml");
		}
		*/
	

	}
}
