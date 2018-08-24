package ch.ethz.matsim.students.samark;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;


public class XMLOps {

	public static void writeToFile(Object object, String fileName) throws FileNotFoundException {

		String filePath = fileName.substring(0, fileName.lastIndexOf("/"));
		new File(filePath).mkdirs();
		System.out.println("Creating File Path: " + filePath);
		
		FileOutputStream fos = new FileOutputStream(fileName);
		
		XStream xstream = new XStream(new StaxDriver());
		// Include the following for aliasing with xStream. Else, use "" for alias definition in individual classes
		/*String aliasString;
		if (object.getClass().getName().contains(".")) {
			aliasString = object.getClass().getName().substring(object.getClass().getName().lastIndexOf(".")+1,object.getClass().getName().length());
		}
		else {
			aliasString = object.getClass().getName();
		}
		xstream.alias(aliasString, object.getClass());*/	
		xstream.toXML(object, fos);
		System.out.println("Written to: " + fileName);
	}
	
	public static <T extends Object> T readFromFile(Class<T> type, String fileName) throws FileNotFoundException {	
		FileInputStream fis = new FileInputStream(fileName);
		XStream xstream = new XStream(new StaxDriver());
		Object object = xstream.fromXML(fis);
		System.out.println("Loaded: " + fileName);
		return type.cast(object);
	}
	
}
