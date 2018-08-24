package ch.ethz.matsim.students.samark;

import java.beans.XMLDecoder;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

//%%%%%%%%%%%% DEPRECATED 24.08.2018 %%%%%%%%%%%%%%%%%%%%%%% 

public class MObjectReader {

    public static <T extends Object> T deserializeFromXML(Class<T> type, String fileName) throws IOException {
        FileInputStream fis = new FileInputStream(fileName);
        XMLDecoder decoder = new XMLDecoder(fis);
        Object object = decoder.readObject();
        decoder.close();
        fis.close();
        return type.cast(object);
    }
	
	/*public static MRoute readMRoute(String filePath) { // filePath--> filePath + "/" + mRoute.routeID + ".ser"

		MRoute mRoute = null;
		FileInputStream fin = null;
		ObjectInputStream ois = null;

		try {

			fin = new FileInputStream(filePath);
			ois = new ObjectInputStream(fin);
			mRoute = (MRoute) ois.readObject();

		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {

			if (fin != null) {
				try {
					fin.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			if (ois != null) {
				try {
					ois.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}

		return mRoute;
	}*/
	
	public static <T extends Object> T readObject(Class<T> type, String filePath) { // filePath--> filePath + "/" + mRoute.routeID + ".ser"

		Object object = null;
		FileInputStream fin = null;
		ObjectInputStream ois = null;

		try {

			fin = new FileInputStream(filePath);
			ois = new ObjectInputStream(fin);
			object = ois.readObject();

		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {

			if (fin != null) {
				try {
					fin.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			if (ois != null) {
				try {
					ois.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}

		return type.cast(object);
	}
}
