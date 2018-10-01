package ch.ethz.matsim.students.samark;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.io.xml.StaxDriver;


public class XMLOps {
	
	public static void writeToFile(Object object, String fileName) throws FileNotFoundException {
		String encodingType = Charset.defaultCharset().toString(); // Examples: "windows-1252", "UTF-8", Charset.defaultCharset().toString();

		if (fileName.contains("/")) {
			String filePath = fileName.substring(0, fileName.lastIndexOf("/"));
			new File(filePath).mkdirs();
			System.out.println("Creating File Path: " + filePath);
		}
		
		FileOutputStream fos = new FileOutputStream(fileName);		
		XStream xstream = new XStream(new DomDriver(encodingType)); 		// Default: new XStream(new StaxDriver()); but will only work if xml is UTF-8!!

		xstream.toXML(object, fos);
		System.out.println("Written to: " + fileName);
	}
	
	public static void writeToFileMNetwork(MNetwork mnetwork, String fileName) throws FileNotFoundException {
		String encodingType = Charset.defaultCharset().toString();

		if (fileName.contains(".xml")) {
			fileName = fileName.split(".xml")[0];
		}
		if (fileName.contains("/")) {
			String filePath = fileName.substring(0, fileName.lastIndexOf("/"));
			new File(filePath).mkdirs();
			System.out.println("Creating File Path: " + filePath);
		}
		System.out.println("MNetwork will be stored to: " + fileName + ".xml");
		NetworkWriter nw = new NetworkWriter(mnetwork.network);
		String matsimNetworkFileName = fileName + "_MatsimNetwork.xml";
		nw.write(matsimNetworkFileName);
		System.out.println("Matsim Network written to: " + matsimNetworkFileName);
		mnetwork.networkFileLocation = matsimNetworkFileName;		
		FileOutputStream fos = new FileOutputStream(fileName+".xml");		
		XStream xstream = new XStream(new DomDriver(encodingType)); 		// Default: new XStream(new StaxDriver()); but will only work if xml is UTF-8!!
		xstream.toXML(mnetwork, fos);
		System.out.println("MNetwork written to: " + fileName+".xml");
	}
	
	public static MNetwork readFromFileMNetwork(String fileName) throws FileNotFoundException {	
		String encodingType = Charset.defaultCharset().toString();

		if(fileName.contains(".xml")==false) {
			fileName = fileName + ".xml";
		}
		FileInputStream fis = new FileInputStream(fileName);
		XStream xstream = new XStream(new DomDriver(encodingType)); 		// Default: new XStream(new StaxDriver()); but will only work if xml is UTF-8!!
		MNetwork mnetwork = (MNetwork) xstream.fromXML(fis);
		System.out.println("Loaded: " + fileName);
		Config config = ConfigUtils.createConfig();
		config.getModules().get("network").addParam("inputNetworkFile", mnetwork.networkFileLocation);
		Network network = ScenarioUtils.loadScenario(config).getNetwork();
		mnetwork.network = network;
		return mnetwork;
	}
	
	
	public static void writeToFileMNetworkPop(MNetworkPop mnetworkPop, String fileName) throws FileNotFoundException {
		String encodingType = Charset.defaultCharset().toString();

		if(fileName.contains(".xml")==false) {
			fileName = fileName + ".xml";
		}
		
		String filePath = "";
		if (fileName.contains("/")) {
			filePath = fileName.substring(0, fileName.lastIndexOf("/"));
			new File(filePath).mkdirs();
			System.out.println("Creating File Path: " + filePath);
		}
		Map<String, String> mnetworkFileLocationMap = new HashMap<String, String>();
		for (String mnetworkName : mnetworkPop.networkMap.keySet()) {
			MNetwork mnetwork = mnetworkPop.networkMap.get(mnetworkName);
			String mNetworkFileName = "";
			if (fileName.contains("/")) {
				mNetworkFileName = filePath + "/" + mnetworkName + "/" + mnetworkName + ".xml";
			}
			else {
				mNetworkFileName = mnetworkName + "/" + mnetworkName + ".xml";
			}
			writeToFileMNetwork(mnetwork, mNetworkFileName);
			mnetworkFileLocationMap.put(mnetworkName, mNetworkFileName);
			mnetworkPop.networkMap.put(mnetworkName, null);		// do this to save memory... we only store the location of the mnetworks, not the actual mnetworks
		}
		mnetworkPop.mNetworkFileLocationMap = mnetworkFileLocationMap;
		
		FileOutputStream fos = new FileOutputStream(fileName);		
		XStream xstream = new XStream(new DomDriver(encodingType)); 		// Default: new XStream(new StaxDriver()); but will only work if xml is UTF-8!!
		xstream.toXML(mnetworkPop, fos);
		System.out.println("MNetworkPop written to: " + fileName);
	}
	
	public static MNetworkPop readFromFileMNetworkPop(String fileName) throws FileNotFoundException {	
		String encodingType = Charset.defaultCharset().toString();

		if(fileName.contains(".xml")==false) {
			fileName = fileName + ".xml";
		}		
		FileInputStream fis = new FileInputStream(fileName);
		XStream xstream = new XStream(new DomDriver(encodingType)); 		// Default: new XStream(new StaxDriver()); but will only work if xml is UTF-8!!
		MNetworkPop mnetworkPop = (MNetworkPop) xstream.fromXML(fis);
		System.out.println("Loaded: " + fileName);
		for (String mnetworkName : mnetworkPop.mNetworkFileLocationMap.keySet()) {
			String mnetworkFileLocation = mnetworkPop.mNetworkFileLocationMap.get(mnetworkName);
			mnetworkPop.networkMap.put(mnetworkName, XMLOps.readFromFileMNetwork(mnetworkFileLocation));
		}
		return mnetworkPop;
	}
	
	public static <T extends Object> T readFromFile(Class<T> type, String fileName) throws FileNotFoundException {	
		String encodingType = Charset.defaultCharset().toString(); // Examples: "windows-1252", "UTF-8", Charset.defaultCharset().toString();

		FileInputStream fis = new FileInputStream(fileName);
		XStream xstream = new XStream(new DomDriver(encodingType)); 		// Default: new XStream(new StaxDriver()); but will only work if xml is UTF-8!!
		
		//XStream.setupDefaultSecurity(xstream);
		//xstream.allowTypes(new Class[] {type});
		Object object = xstream.fromXML(fis);
		System.out.println("Loaded: " + fileName);
		return type.cast(object);
	}
	
}
