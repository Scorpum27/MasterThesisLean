package ch.ethz.matsim.students.samark;

import java.io.File;
import java.io.FileNotFoundException;

public class CbpSpecific2Orig {

	public static void main(String[] args) throws FileNotFoundException {
		String specSimFolder = args[0];
		String networkName = "Network"+args[1];
		String origSimFolder = args[2];
		for (Integer c=1; c<=1000; c++) {
			File cbpSpecFile = new File(specSimFolder+"/zurich_1pm/Evolution/Population/"+networkName+"/cbpParametersAveraged"+c+".xml");
			if (cbpSpecFile.exists()) {
				XMLOps.writeToFile(XMLOps.readFromFile(CBPII.class, cbpSpecFile.getPath()),
						origSimFolder+"/zurich_1pm/cbpParametersOriginal/cbpParametersOriginal"+c+".xml");
			}
		}
		File cbpSpecFileGlobal = new File(specSimFolder+"/zurich_1pm/Evolution/Population/"+networkName+"/cbpParametersAveragedGlobal.xml");
		if (cbpSpecFileGlobal.exists()) {
			XMLOps.writeToFile(XMLOps.readFromFile(CBPII.class, cbpSpecFileGlobal.getPath()),
					origSimFolder+"/zurich_1pm/cbpParametersOriginal/cbpParametersOriginalGlobal.xml");
		}
	}

}
