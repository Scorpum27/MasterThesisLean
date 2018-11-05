package ch.ethz.matsim.students.samark;

import java.io.File;
import java.io.IOException;


public class VisualizerCBP_Original {

	public static void main(String[] args) throws IOException {

		// args: 100 1 1000 individual
		Integer maxIterations = Integer.parseInt(args[0]);
		Integer iterationsToAverage = Integer.parseInt(args[1]);
		Integer populationFactor = Integer.parseInt(args[2]);
		String recalculateOriginalCBPStrategy = args[3];

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
		else {
			System.out.println("CAUTION: Invalid strategy. Choose from individual/global. Aborting...");
			System.exit(0);
		}

	}

}
