package ch.ethz.matsim.students.samark;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import ch.ethz.matsim.baseline_scenario.BaselineModule;
import ch.ethz.matsim.baseline_scenario.transit.BaselineTransitModule;
import ch.ethz.matsim.baseline_scenario.transit.routing.DefaultEnrichedTransitRoute;
import ch.ethz.matsim.baseline_scenario.transit.routing.DefaultEnrichedTransitRouteFactory;
import ch.ethz.matsim.baseline_scenario.zurich.ZurichModule;

public class Run_ZurichScenario {
	
static public void main(String[] args) {
		
		Config config = ConfigUtils.loadConfig(args[0]);
		config.getModules().get("controler").addParam("lastIteration", "100");
		config.getModules().get("controler").addParam("writeEventsInterval", "1");

		// config.transit().setTransitScheduleFile(null); // potentially "null", but should set TransitSchedule

		Scenario scenario = ScenarioUtils.createScenario(config);
		scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DefaultEnrichedTransitRoute.class,
				new DefaultEnrichedTransitRouteFactory());
		ScenarioUtils.loadScenario(scenario);
		
		Controler controler = new Controler(scenario);

		controler.addOverridingModule(new BaselineModule());
		controler.addOverridingModule(new BaselineTransitModule());
		controler.addOverridingModule(new ZurichModule());

		controler.run();
	}
	
}


