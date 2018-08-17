package ch.ethz.matsim.students.samark;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import ch.ethz.matsim.baseline_scenario.BaselineModule;
import ch.ethz.matsim.baseline_scenario.transit.BaselineTransitModule;
import ch.ethz.matsim.baseline_scenario.transit.routing.DefaultEnrichedTransitRoute;
import ch.ethz.matsim.baseline_scenario.transit.routing.DefaultEnrichedTransitRouteFactory;
import ch.ethz.matsim.baseline_scenario.zurich.ZurichModule;

public class Run_MetroScenario {

	public static void main(String[] args) {
				
		Config metroConfig = Metro_ConfigModifier.modifyFromFile("zurich_1pm/zurich_config.xml",
				"Metro/Input/Generated_Networks/MergedNetworkODInitialRoutes.xml");
		metroConfig.getModules().get("controler").addParam("lastIteration", "100");
		Scenario metroScenario = ScenarioUtils.createScenario(metroConfig);
		metroScenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DefaultEnrichedTransitRoute.class,
				new DefaultEnrichedTransitRouteFactory());
		ScenarioUtils.loadScenario(metroScenario);		

		/*Config metroConfig = ConfigUtils.loadConfig("zurich_1pm/zurich_config.xml"); 		
		metroConfig.getModules().get("controler").addParam("outputDirectory", "SimulationOutputMerged");
		metroConfig.getModules().get("network").addParam("inputNetworkFile", "created_input/Networks/MergedNetwork.xml");
		metroConfig.getModules().get("transit").addParam("transitScheduleFile","created_input/PT_Files/MergedSchedule.xml");
		metroConfig.getModules().get("transit").addParam("vehiclesFile","created_input/PT_Files/MergedVehicles.xml");

		Scenario metroScenario = ScenarioUtils.createScenario(metroConfig);
		metroScenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DefaultEnrichedTransitRoute.class,
				new DefaultEnrichedTransitRouteFactory());
		ScenarioUtils.loadScenario(metroScenario);			
		*/
		
		Controler controler = new Controler(metroScenario);
		controler.addOverridingModule(new BaselineModule());
		controler.addOverridingModule(new BaselineTransitModule());
		controler.addOverridingModule(new ZurichModule());
		controler.run();
		
		
		
	}

}
