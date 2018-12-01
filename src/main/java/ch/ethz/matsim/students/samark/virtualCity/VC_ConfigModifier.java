package ch.ethz.matsim.students.samark.virtualCity;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;

public class VC_ConfigModifier {

	public static Config modifyConfig(Config config, int lastIteration, String networkFile, String populationName,
			String scheduleName, String vehiclesName) {
		
		config.getModules().get("controler").addParam("overwriteFiles", "overwriteExistingFiles");
		// config.getModules().get("changeMode").addParam("modes", "walk,car,pt");
		config.getModules().get("counts").addParam("analyzedModes", "car,pt");
		config.getModules().get("controler").addParam("lastIteration", Integer.toString(lastIteration));
		// System.out.println(config.getModules().get("controler").getParams().get("lastIteration").toString());
		config.getModules().get("controler").addParam("outputDirectory", "zurich_1pm/VirtualCity/Simulation_Output");		
		config.getModules().get("ptCounts").addParam("outputformat", "txt");		
		config.getModules().remove("facilities");
		config.getModules().remove("households");
		config.getModules().get("network").addParam("inputNetworkFile", "VirtualCity/Input/Generated_Networks/"+networkFile);
		config.getModules().get("plans").addParam("inputPersonAttributesFile", "null"); // may have to add attributes in population creation !!
		config.getModules().get("plans").addParam("inputPlansFile", "VirtualCity/Input/Generated_Population/"+populationName);
		// may have to add whole new ParameterSet here by creating it...  old attempt was: config.getModules().get("strategy").getParameterSets().get("strategysettings") //
		config.getModules().get("transit").addParam("transitScheduleFile","VirtualCity/Input/Generated_PT_Files/"+scheduleName);
		config.getModules().get("transit").addParam("vehiclesFile","VirtualCity/Input/Generated_PT_Files/"+vehiclesName);
		config.getModules().get("changeMode").addParam("modes", "walk,car,pt");
		config.getModules().get("changeMode").addParam("modes", "walk,car,pt");		
		
		ConfigWriter configWriter = new ConfigWriter(config);
		configWriter.write("zurich_1pm/VirtualCity/Input/Generated_Config/VirtualCity_config.xml");
		
		return config;
	}
	
public static Config modifyConfigFromFile(String configFile,String networkFile, String populationName,
		String scheduleName, String vehiclesName) {
		
		Config modConfig = ConfigUtils.loadConfig(configFile);
		modConfig.getModules().get("controler").addParam("overwriteFiles", "overwriteExistingFiles");
		modConfig.getModules().get("changeMode").addParam("modes", "walk,car,pt");
		modConfig.getModules().get("controler").addParam("lastIteration", "100");
		// System.out.println(config.getModules().get("controler").getParams().get("lastIteration").toString());
		modConfig.getModules().get("controler").addParam("outputDirectory", "zurich_1pm/VirtualCity/Simulation_Output");
		modConfig.getModules().remove("facilities");
		modConfig.getModules().remove("households");
		modConfig.getModules().get("network").addParam("inputNetworkFile", "zurich_1pm/VirtualCity/Input/Generated_Networks/"+networkFile);
		modConfig.getModules().get("plans").addParam("inputPersonAttributesFile", "null"); // may have to add attributes in population creation !!
		modConfig.getModules().get("plans").addParam("inputPlansFile", "zurich_1pm/VirtualCity/Input/Generated_Population/"+populationName);
		// may have to add whole new ParameterSet here by creating it...  old attempt was: config.getModules().get("strategy").getParameterSets().get("strategysettings") //
		modConfig.getModules().get("transit").addParam("transitScheduleFile","zurich_1pm/VirtualCity/Input/Generated_PT_Files/"+scheduleName);
		modConfig.getModules().get("transit").addParam("vehiclesFile","zurich_1pm/VirtualCity/Input/Generated_PT_Files/"+vehiclesName);
		modConfig.getModules().get("changeMode").addParam("modes", "walk,car,pt");
		modConfig.getModules().get("changeMode").addParam("modes", "walk,car,pt");
		
		ConfigWriter configWriter = new ConfigWriter(modConfig);
		configWriter.write("VirtualCity/Input/Generated_Config/zurich_1pm/VirtualCity_config.xml");
		
		return modConfig;
	}
	
}
