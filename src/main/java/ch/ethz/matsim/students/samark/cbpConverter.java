package ch.ethz.matsim.students.samark;

import java.io.File;
import java.io.IOException;
import javax.xml.stream.XMLStreamException;

public class cbpConverter {

	public static void main(String[] args) throws IOException, XMLStreamException {
		String scope = args[0];
		String simFolder = args[1];
		String networkNr = args[2];
		
		if (scope.equals("original")) {
			for (int n=1; n<=1000; n++) {
				File cbpOrigFile = new File(simFolder+"/zurich_1pm/cbpParametersOriginal/cbpParametersOriginal"+n+".xml");
				if (cbpOrigFile.exists()) {
					XMLOps.writeToFile(new CBPII(XMLOps.readFromFile(CostBenefitParameters.class, cbpOrigFile.getAbsolutePath())), cbpOrigFile.getAbsolutePath());
				}
				else { break; }
			}
			File cbpOrigGlobalFile = new File(simFolder+"/zurich_1pm/cbpParametersOriginal/cbpParametersOriginalGlobal.xml");	// simFolder+
			if (cbpOrigGlobalFile.exists()) {
				XMLOps.writeToFile(new CBPII(XMLOps.readFromFile(CostBenefitParameters.class, cbpOrigGlobalFile.getAbsolutePath())), cbpOrigGlobalFile.getAbsolutePath());
			}
		}
		else if(scope.equals("specific")) {
			for (int n=1; n<=1000; n++) {
				File cbpNewFile = new File(simFolder+"/zurich_1pm/Evolution/Population/Network"+networkNr+"/cbpParametersAveraged"+n+".xml");
				if (cbpNewFile.exists()) {
					XMLOps.writeToFile(new CBPII(XMLOps.readFromFile(CostBenefitParameters.class, cbpNewFile.getAbsolutePath())), cbpNewFile.getAbsolutePath());
				}
			}
			File cbpNewGlobalFile = new File(simFolder+"/zurich_1pm/Evolution/Population/Network"+networkNr+"/cbpParametersAveragedGlobal.xml");
			if (cbpNewGlobalFile.exists()) {
				XMLOps.writeToFile(new CBPII(XMLOps.readFromFile(CostBenefitParameters.class, cbpNewGlobalFile.getAbsolutePath())), cbpNewGlobalFile.getAbsolutePath());
			}
		}
		else if(scope.equals("both")) {
			for (int n=1; n<=1000; n++) {
				File cbpOrigFile = new File(simFolder+"/zurich_1pm/cbpParametersOriginal/cbpParametersOriginal"+n+".xml");
				if (cbpOrigFile.exists()) {
					XMLOps.writeToFile(new CBPII(XMLOps.readFromFile(CostBenefitParameters.class, cbpOrigFile.getAbsolutePath())), cbpOrigFile.getAbsolutePath());
				}
			}
			File cbpOrigGlobalFile = new File(simFolder+"/zurich_1pm/cbpParametersOriginal/cbpParametersOriginalGlobal.xml");
			if (cbpOrigGlobalFile.exists()) {
				XMLOps.writeToFile(new CBPII(XMLOps.readFromFile(CostBenefitParameters.class, cbpOrigGlobalFile.getAbsolutePath())), cbpOrigGlobalFile.getAbsolutePath());
			}
			for (int n=1; n<=1000; n++) {
				File cbpNewFile = new File(simFolder+"/zurich_1pm/Evolution/Population/Network"+networkNr+"/cbpParametersAveraged"+n+".xml");
				if (cbpNewFile.exists()) {
					XMLOps.writeToFile(new CBPII(XMLOps.readFromFile(CostBenefitParameters.class, cbpNewFile.getAbsolutePath())), cbpNewFile.getAbsolutePath());
				}
			}
			File cbpNewGlobalFile = new File(simFolder+"/zurich_1pm/Evolution/Population/Network"+networkNr+"/cbpParametersAveragedGlobal.xml");
			if (cbpNewGlobalFile.exists()) {
				XMLOps.writeToFile(new CBPII(XMLOps.readFromFile(CostBenefitParameters.class, cbpNewGlobalFile.getAbsolutePath())), cbpNewGlobalFile.getAbsolutePath());
			}
		}
		
	}

	
}
