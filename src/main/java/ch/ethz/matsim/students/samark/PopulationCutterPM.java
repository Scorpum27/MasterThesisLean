package ch.ethz.matsim.students.samark;

import java.util.Random;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class PopulationCutterPM {

	// BUILD 3pm/6pm/xpm populations from 1pct scenario
	public static void main(String[] args) {
		Integer pmSize = 6;
		Config config = ConfigUtils.createConfig();
		config.getModules().get("plans").addParam("inputPlansFile", "zurich_1pm/zurich_population.xml.gz");
		Population pop1pct = ScenarioUtils.loadScenario(config).getPopulation();
		Population popCut = ScenarioUtils.loadScenario(ConfigUtils.createConfig()).getPopulation();
		int popSize = 0;
		for (Person person : pop1pct.getPersons().values()) {
			// from 10 agents pick pmSize random agents to be featured in cut population: 
			if ((new Random()).nextDouble() < 0.1*pmSize) {
				popCut.addPerson(person);
				popSize++;
			}
			if (popSize > pmSize*0.1*pop1pct.getPersons().size()) {
				break;
			}
		}
		PopulationWriter pw = new PopulationWriter(popCut);
		pw.write("zurich_1pm/zurich_population_"+pmSize+"pm.xml.gz");
		System.out.println("PopSize = "+popSize);
		System.out.println("PopSizeMax = "+pmSize*0.1*pop1pct.getPersons().size());
	}

}
