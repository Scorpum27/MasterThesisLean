package ch.ethz.matsim.students.samark;

import java.io.File;
import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
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

public class MATSimRunnable implements Runnable {

	String[] args;
	MNetwork mNetwork;
	String initialRouteType;
	String initialConfig;
	int lastIteration;
	
	public MATSimRunnable(String[] args, MNetwork mNetwork, String initialRouteType, String initialConfig, int lastIteration) {
		this.args = args;
		this.mNetwork = mNetwork;
		this.initialRouteType = initialRouteType;
		this.initialConfig = initialConfig;
		this.lastIteration = lastIteration;
	}

	@Override
	public void run() {
		
		try {
			Log.write("  >> Running MATSim simulation on:  "+mNetwork.networkID);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		CommandLine cmd = null;
		try {
			cmd = new CommandLine.Builder(args)
					.allowOptions("model-type", "fallback-behaviour")
					.build();
		} catch (ConfigurationException e) {
			e.printStackTrace();
		}
		
		Config modConfig = ConfigUtils.loadConfig(initialConfig);
		String simulationPath = "zurich_1pm/Evolution/Population/"+mNetwork.networkID+"/Simulation_Output";
		new File(simulationPath).mkdirs();
		modConfig.getModules().get("controler").addParam("outputDirectory", simulationPath);
		modConfig.getModules().get("controler").addParam("overwriteFiles", "overwriteExistingFiles");
		modConfig.getModules().get("controler").addParam("lastIteration", Integer.toString(lastIteration));
		modConfig.getModules().get("controler").addParam("writeEventsInterval", "1");
		String inputNetworkFile = "Evolution/Population/BaseInfrastructure/GlobalNetwork.xml"; 
		// See old versions BEFORE 06.09.2018 for how to load specific mergedNetworks OD/Random instead of Global Network with all links
		modConfig.getModules().get("network").addParam("inputNetworkFile", inputNetworkFile);
		modConfig.getModules().get("transit").addParam("transitScheduleFile","Evolution/Population/"+mNetwork.networkID+"/MergedSchedule.xml");
		modConfig.getModules().get("transit").addParam("vehiclesFile","Evolution/Population/"+mNetwork.networkID+"/MergedVehicles.xml");
		// See old versions BEFORE 06.09.2018 for how to write config to file (not necessary)
		
		Scenario modScenario = ScenarioUtils.createScenario(modConfig);
		modScenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DefaultEnrichedTransitRoute.class,
					new DefaultEnrichedTransitRouteFactory());
		ScenarioUtils.loadScenario(modScenario);	
			
		// Do this to delete initial plans in order to have same chances of success for metro as other traffic!
		TripsToLegsAlgorithm t2l = new TripsToLegsAlgorithm(new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE), new MainModeIdentifierImpl());
		for (Person person : modScenario.getPopulation().getPersons().values()) {
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
		new LongPlanFilter(10, new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE)).run(modScenario.getPopulation());

		// new mode choice strategies in order to consider metro as other pt
		modConfig.strategy().clearStrategySettings();
		modConfig.strategy().setMaxAgentPlanMemorySize(1);
	    StrategySettings strategy;
	    // See MATSIM-766 (https://matsim.atlassian.net/browse/MATSIM-766)
	    strategy = new StrategySettings();
	    strategy.setStrategyName("SubtourModeChoice");
	    strategy.setDisableAfter(0);
	    strategy.setWeight(0.0);
	    modConfig.strategy().addStrategySettings(strategy);

	    strategy = new StrategySettings();
	    strategy.setStrategyName("custom");
	    strategy.setWeight(0.15);
	    modConfig.strategy().addStrategySettings(strategy);

	    strategy = new StrategySettings();
	    strategy.setStrategyName("ReRoute");
	    strategy.setWeight(0.05);
	    modConfig.strategy().addStrategySettings(strategy);

	    strategy = new StrategySettings();
	    strategy.setStrategyName("KeepLastSelected");
	    strategy.setWeight(0.85);
	    modConfig.strategy().addStrategySettings(strategy);
		
	    Controler controler = new Controler(modScenario);
		controler.addOverridingModule(new BaselineModule());
		controler.addOverridingModule(new BaselineTransitModule());
		controler.addOverridingModule(new ZurichModule());
		controler.addOverridingModule(new BaselineTrafficModule(3.0));
		controler.addOverridingModule(new CustomModeChoiceModule(cmd));
		controler.run();

		try {
			Log.write("Completed MATSim Run of network="+mNetwork.networkID);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
}
