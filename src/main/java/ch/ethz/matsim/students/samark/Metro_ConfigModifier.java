package ch.ethz.matsim.students.samark;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;

public class Metro_ConfigModifier {

	public static Config modifyFromFile(String configFile) {

		Config modConfig = ConfigUtils.loadConfig(configFile);
		modConfig.getModules().get("controler").addParam("outputDirectory", "zurich_1pm/Metro/Simulation_Output");
		modConfig.getModules().get("controler").addParam("overwriteFiles", "overwriteExistingFiles");
		modConfig.getModules().get("network").addParam("inputNetworkFile", "Metro/Input/Generated_Networks/MergedNetwork.xml");
		modConfig.getModules().get("transit").addParam("transitScheduleFile","Metro/Input/Generated_PT_Files/MergedSchedule.xml");
		modConfig.getModules().get("transit").addParam("vehiclesFile","Metro/Input/Generated_PT_Files/MergedVehicles.xml");

		ConfigWriter configWriter = new ConfigWriter(modConfig);
		configWriter.write("zurich_1pm/Metro/Input/Generated_Config/zurich_config_metro_justForLookup.xml");
		
		return modConfig;
	}
	
}
