package ch.ethz.matsim.students.samark;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.algorithms.TripsToLegsAlgorithm;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.PtConstants;

import ch.ethz.matsim.baseline_scenario.BaselineModule;
import ch.ethz.matsim.baseline_scenario.config.CommandLine;
import ch.ethz.matsim.baseline_scenario.config.CommandLine.ConfigurationException;
import ch.ethz.matsim.baseline_scenario.traffic.BaselineTrafficModule;
import ch.ethz.matsim.baseline_scenario.transit.BaselineTransitModule;
import ch.ethz.matsim.baseline_scenario.transit.routing.DefaultEnrichedTransitRoute;
import ch.ethz.matsim.baseline_scenario.transit.routing.DefaultEnrichedTransitRouteFactory;
import ch.ethz.matsim.baseline_scenario.zurich.ZurichModule;
import ch.ethz.matsim.papers.mode_choice_paper.CustomModeChoiceModule;
import ch.ethz.matsim.papers.mode_choice_paper.utils.LongPlanFilter;

public class Run_MetroScenario {

	public static void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args)
				.allowOptions("model-type", "fallback-behaviour")
				.build();
				
		//Config metroConfig = Metro_ConfigModifier.modifyFromFile("zurich_1pm/zurich_config.xml",
		//		"Metro/Input/Generated_Networks/MergedNetworkODInitialRoutes.xml");
		Config metroConfig = Metro_ConfigModifier.modifyFromFile("zurich_1pm/zurich_config.xml",
				"Metro/Input/Generated_Networks/MergedNetworkRandomInitialRoutes.xml");
		
		metroConfig.getModules().get("controler").addParam("lastIteration", "10");
		
		// do this line to check whether more agents use metro if we cancel all other transit!
		// metroConfig.getModules().get("transit").addParam("transitScheduleFile","Metro/Input/Generated_PT_Files/MetroSchedule.xml");
		
		Scenario metroScenario = ScenarioUtils.createScenario(metroConfig);
		metroScenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DefaultEnrichedTransitRoute.class,
				new DefaultEnrichedTransitRouteFactory());
		ScenarioUtils.loadScenario(metroScenario);	
		
		// Do this to delete initial plans in order to have same chances of success for metro as other traffic!
		TripsToLegsAlgorithm t2l = new TripsToLegsAlgorithm(new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE), new MainModeIdentifierImpl());
		for (Person person : metroScenario.getPopulation().getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				for (PlanElement element : plan.getPlanElements()) {
					if (element instanceof Leg) {
						Leg leg = (Leg) element;
						leg.setRoute(null);
					}
				}
				
				t2l.run(plan);
			}
		}
		
		// filters too long plans, which bloat simulation time
		new LongPlanFilter(10, new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE)).run(metroScenario.getPopulation());

		// new mode choice strategies in order to consider metro as other pt
		metroConfig.strategy().clearStrategySettings();
        metroConfig.strategy().setMaxAgentPlanMemorySize(1);
        StrategySettings strategy;
        // See MATSIM-766 (https://matsim.atlassian.net/browse/MATSIM-766)
        strategy = new StrategySettings();
        strategy.setStrategyName("SubtourModeChoice");
        strategy.setDisableAfter(0);
        strategy.setWeight(0.0);
        metroConfig.strategy().addStrategySettings(strategy);

        strategy = new StrategySettings();
        strategy.setStrategyName("custom");
        strategy.setWeight(0.15);
        metroConfig.strategy().addStrategySettings(strategy);

        strategy = new StrategySettings();
        strategy.setStrategyName("ReRoute");
        strategy.setWeight(0.05);
        metroConfig.strategy().addStrategySettings(strategy);

        strategy = new StrategySettings();
        strategy.setStrategyName("KeepLastSelected");
        strategy.setWeight(0.85);
        metroConfig.strategy().addStrategySettings(strategy);

		
		Controler controler = new Controler(metroScenario);
		controler.addOverridingModule(new BaselineModule());
		controler.addOverridingModule(new BaselineTransitModule());
		controler.addOverridingModule(new ZurichModule());
		controler.addOverridingModule(new BaselineTrafficModule(3.0));
		controler.addOverridingModule(new CustomModeChoiceModule(cmd));
		controler.run();
		
		
		
	}

}



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