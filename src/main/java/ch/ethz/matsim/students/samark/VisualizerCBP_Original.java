package ch.ethz.matsim.students.samark;

import java.io.File;
import java.io.IOException;

// Calculate individual CBPs first for original reference simulations
// Then select individual2global to average these values over #iterations = iterationsToAverage

public class VisualizerCBP_Original {

	public static void main(String[] args) throws IOException {

		// args: 600 1 1000 individual
		Integer maxIterations = Integer.parseInt(args[0]);
		Integer iterationsToAverage = Integer.parseInt(args[1]);
		Integer populationFactor = Integer.parseInt(args[2]);
		String recalculateOriginalCBPStrategy = args[3]; // "individual", "global", "individual2global"

		(new File("zurich_1pm/cbpParametersOriginal")).mkdirs();

		if (recalculateOriginalCBPStrategy.equals("individual")) {
			for (Integer lastIteration = 1; lastIteration <= maxIterations; lastIteration++) {
				String plansFolder = "zurich_1pm/Zurich_1pm_SimulationOutputEnriched/ITERS";
				String outputFile = "zurich_1pm/cbpParametersOriginal/cbpParametersOriginal" + lastIteration + ".xml";
				if (lastIteration < iterationsToAverage) { // then use all available (=lastIteration) for averaging
					NetworkEvolutionImpl.calculateCBAStats(plansFolder, outputFile, (int) populationFactor, lastIteration,
							lastIteration);
				} else {
					NetworkEvolutionImpl.calculateCBAStats(plansFolder, outputFile, (int) populationFactor, lastIteration,
							iterationsToAverage);
				}
			}
		}
		else if (recalculateOriginalCBPStrategy.equals("global")) {
			String plansFolder = "zurich_1pm/Zurich_1pm_SimulationOutputEnriched/ITERS";
			String outputFile = "zurich_1pm/cbpParametersOriginal/cbpParametersOriginalGlobal.xml";
			NetworkEvolutionImpl.calculateCBAStats(plansFolder, outputFile, (int) populationFactor, maxIterations,
						iterationsToAverage);
		}
		else if (recalculateOriginalCBPStrategy.equals("individual2global")) {
			double ptUsers = 0.0;
			double carUsers = 0.0;
			double otherUsers = 0.0;
			double carTimeTotal = 0.0;
			double carPersonDist = 0.0;
			double ptTimeTotal = 0.0;
			double ptPersonDist = 0.0;
			for (Integer i = maxIterations-iterationsToAverage+1; i<=maxIterations; i++) {
				CostBenefitParameters cbpi = XMLOps.readFromFile(CostBenefitParameters.class,
						"zurich_1pm/cbpParametersOriginal/cbpParametersOriginal" + i + ".xml");
				ptUsers += cbpi.ptUsers;
				carUsers += cbpi.carUsers;
				otherUsers += cbpi.otherUsers;
				carTimeTotal += cbpi.carTimeTotal;
				carPersonDist += cbpi.carPersonDist;
				ptTimeTotal += cbpi.ptTimeTotal;
				ptPersonDist += cbpi.ptPersonDist;
			}
			CostBenefitParameters cbpGlobal = new CostBenefitParameters(ptUsers/iterationsToAverage, carUsers/iterationsToAverage, otherUsers/iterationsToAverage,
					carTimeTotal/iterationsToAverage, carPersonDist/iterationsToAverage, ptTimeTotal/iterationsToAverage, ptPersonDist/iterationsToAverage);
			XMLOps.writeToFile(cbpGlobal, "zurich_1pm/cbpParametersOriginal/cbpParametersOriginalGlobal.xml");
		}
		else {
			System.out.println("CAUTION: Invalid strategy. Choose from individual/global. Aborting...");
			System.exit(0);
		}

	}

}
