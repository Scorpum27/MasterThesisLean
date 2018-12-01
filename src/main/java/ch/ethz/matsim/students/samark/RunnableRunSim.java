package ch.ethz.matsim.students.samark;

import java.io.File;
import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationWriter;
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
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;

public class RunnableRunSim implements Runnable {
    
	String[] args;
	MNetwork mNetwork;
	String initialRouteType;
	String initialConfig;
	int lastIteration;
	boolean useFastSBahnModule;
	String ptRemoveScenario;
	String inputPlanStrategy;
	
	public RunnableRunSim(String[] args, MNetwork mNetwork, String initialRouteType,
			String initialConfig, int lastIteration, boolean useFastSBahnModule, String ptRemoveScenario, String inputPlanStrategy) {
		this.args = args;
		this.mNetwork = mNetwork;
		this.initialRouteType = initialRouteType;
		this.initialConfig = initialConfig;
		this.lastIteration = lastIteration;
		this.useFastSBahnModule = useFastSBahnModule;
		this.ptRemoveScenario = ptRemoveScenario;
		this.inputPlanStrategy = inputPlanStrategy;
	}
	
	
	public void run() {
			try {
				Log.write("  >> Running MATSim simulation on:  "+this.mNetwork.networkID);
			} catch (IOException e) {
				e.printStackTrace();
			}
		
		CommandLine cmd = null;
		try {
			cmd = new CommandLine.Builder(this.args)
					.allowOptions("model-type", "fallback-behaviour")
					.build();
		} catch (ConfigurationException e) {
			e.printStackTrace();
		}
		
		Config modConfig = ConfigUtils.loadConfig(this.initialConfig);
		String simulationPath = "zurich_1pm/Evolution/Population/"+this.mNetwork.networkID+"/Simulation_Output";
		new File(simulationPath).mkdirs();
		
		
		if (inputPlanStrategy.equals("default")) {
			// no nothing with inputPlansFile
			// leave blank if want to use initial plans
		}
		else if(inputPlanStrategy.equals("simEquil")) {
			modConfig.getModules().get("plans").addParam("inputPlansFile", "Zurich_1pm_SimulationOutputEnriched/output_plans.xml.gz");					
		}
		else if(inputPlanStrategy.equals("lastPlan")) {
			modConfig.getModules().get("plans").addParam("inputPlansFile", "Evolution/Population/"+this.mNetwork.networkID+"/Simulation_Output/output_plans.xml.gz");			
		}
		
		modConfig.getModules().get("controler").addParam("outputDirectory", simulationPath);
		modConfig.getModules().get("controler").addParam("overwriteFiles", "overwriteExistingFiles");
		modConfig.getModules().get("controler").addParam("lastIteration", Integer.toString(this.lastIteration));
		modConfig.getModules().get("controler").addParam("writeEventsInterval", "1");
		modConfig.getModules().get("controler").addParam("writePlansInterval", "1");
		String inputNetworkFile = "Evolution/Population/BaseInfrastructure/GlobalNetwork.xml";
		// See old versions BEFORE 06.09.2018 for how to load specific mergedNetworks OD/Random instead of Global Network with all links
		modConfig.getModules().get("network").addParam("inputNetworkFile", inputNetworkFile);
		if (this.useFastSBahnModule) {
			try {
				Metro_TransitScheduleImpl.TS_ModificationModule(this.mNetwork.networkID, ptRemoveScenario);
				Metro_TransitScheduleImpl.SpeedSBahnModule(this.mNetwork, "MergedScheduleModified.xml", "MergedScheduleSpeedSBahn.xml");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			modConfig.getModules().get("transit").addParam("transitScheduleFile","Evolution/Population/"+this.mNetwork.networkID+"/MergedScheduleSpeedSBahn.xml");			
		}
		else {
			modConfig.getModules().get("transit").addParam("transitScheduleFile","Evolution/Population/"+this.mNetwork.networkID+"/MergedSchedule.xml");
//			Metro_TransitScheduleImpl.TS_ModificationModule(mNetwork.networkID);
//			modConfig.getModules().get("transit").addParam("transitScheduleFile","Evolution/Population/"+mNetwork.networkID+"/MergedScheduleModified.xml");			
		}
		modConfig.getModules().get("transit").addParam("vehiclesFile","Evolution/Population/"+this.mNetwork.networkID+"/MergedVehicles.xml");
//		modConfig.getModules().get("qsim").addParam("flowCapacityFactor", "10000");
//		modConfig.getModules().get("global").addParam("numberOfThreads","1");
//		modConfig.getModules().get("parallelEventHandling").addParam("numberOfThreads","1");
//		modConfig.getModules().get("qsim").addParam("numberOfThreads","1");
		// See old versions BEFORE 06.09.2018 for how to write config to file (not necessary)
		
		Scenario modScenario = ScenarioUtils.createScenario(modConfig);
		modScenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DefaultEnrichedTransitRoute.class,
					new DefaultEnrichedTransitRouteFactory());
		ScenarioUtils.loadScenario(modScenario);
		PopulationWriter popWriter = new PopulationWriter(modScenario.getPopulation());
		popWriter.write(simulationPath+"/output_plans_backup.xml.gz");
			
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
	    strategy.setDisableAfter(10);
	    strategy.setWeight(0.05);	// 0.05
	    modConfig.strategy().addStrategySettings(strategy);

	    strategy = new StrategySettings();
	    strategy.setStrategyName("KeepLastSelected");
	    strategy.setWeight(0.85);	// 0.85
	    modConfig.strategy().addStrategySettings(strategy);
	    
	    boolean bestResponse = true;
		
	    Controler controler = new Controler(modScenario);
	    controler.addOverridingModule(new SwissRailRaptorModule());
		controler.addOverridingModule(new BaselineModule());
		controler.addOverridingModule(new BaselineTransitModule());
		controler.addOverridingModule(new ZurichModule());
		controler.addOverridingModule(new BaselineTrafficModule(3.0));
		controler.addOverridingModule(new CustomModeChoiceModule(cmd, bestResponse));
		controler.run();
		try {
			Log.write("  >> Completed MATSim simulation on:  "+this.mNetwork.networkID);
		} catch (IOException e) {
			e.printStackTrace();
		}

    }
}