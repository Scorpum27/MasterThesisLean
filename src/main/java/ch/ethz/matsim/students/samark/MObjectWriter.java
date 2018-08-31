package ch.ethz.matsim.students.samark;

import java.beans.ExceptionListener;
import java.beans.XMLEncoder;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

// %%%%%%%%%%%% DEPRECATED 24.08.2018 %%%%%%%%%%%%%%%%%%%%%%% 

public class MObjectWriter {

	public static void serializeToXML2 (MNetwork mNetwork, String fileName) {
	    try {

	      System.out.println("Writing to file " + fileName);

	      // ... and serialize it via XMLEncoder to file testbeanlist.xml
	      final XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(
	          new FileOutputStream(fileName)));
	      encoder.writeObject(mNetwork);
	      encoder.close();

	      /*// Use XMLDecoder to read the same XML file in.
	      final XMLDecoder decoder = new XMLDecoder(new FileInputStream(FILENAME));
	      final List<TestBean> listFromFile = (List<TestBean>) decoder.readObject();
	      decoder.close();

	      System.out.println("Reading list: " + listFromFile);*/
	    } catch (FileNotFoundException e) {
	      e.printStackTrace();
	    }
	  }
	
	 public static void serializeToXML (MNetwork mNetwork, String fileName) throws IOException {
	    	String filePath = fileName.substring(0, fileName.lastIndexOf("/"));
			System.out.println(filePath);
			new File(filePath).mkdirs();
			
	        FileOutputStream fos = null;
	        XMLEncoder encoder = null;

	        try {
	        	fos = new FileOutputStream(fileName);
	        	encoder = new XMLEncoder(fos);
	        	ExceptionListener exceptionListener = new ExceptionListener() {
	        		public void exceptionThrown(Exception e) {
	        			System.out.println("Exception! :" + e.toString());
	        		}
	        	};
	        	encoder.setExceptionListener(exceptionListener);
	        	encoder.writeObject(mNetwork);
	        }catch (Exception ex) {
				ex.printStackTrace();
			} finally {
				if (fos != null) {
					try {
						fos.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
	        encoder.close();
	    }

	 
	 
	public static void writeObject(Object object, String fileName) {

		String filePath = fileName.substring(0, fileName.lastIndexOf("/"));
		//String[] strings = fileName.split("/", fileName.lastIndexOf("/")-1);
		System.out.println(filePath);
		// System.out.println();
		//String filePath = strings[0];
		new File(filePath).mkdirs();
		
		FileOutputStream fout = null;
		ObjectOutputStream oos = null;

		try {

			fout = new FileOutputStream(fileName);
			oos = new ObjectOutputStream(fout);
			oos.writeObject(object);

		} catch (Exception ex) {

			ex.printStackTrace();

		} finally {

			if (fout != null) {
				try {
					fout.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			if (oos != null) {
				try {
					oos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}
	}
	
}
