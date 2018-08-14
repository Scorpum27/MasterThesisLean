package ch.ethz.matsim.students.samark;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;

public class OD_ProcessorImpl {
	
	/* get all OD pairs (input = file)
	 * make coord transformation (input = conversion)
	 * find max nRoutes pair values (consider only those within min/max bounds) (input = min/max bounds)
	 */
	// load OD-CSV file
	// load NAME/COORD file
	// check all names in names file and delete those which are not within bounds specified above
	// search for nRoutes highest values and store as list of Coords[][]!
	// Convert coords to closest nodes!
	
	public static Node[][] findODPairs(Network metroNetwork, String csvFileODValues, String csvFileODLocations, 
			double minRadius, double maxRadius, Coord cityCenterCoord, int nRoutes, double xOffset, double yOffset){
		
		// String[] odCodesColumns;
		// int odLocationsNumber;
		List<String[]> odLocations = new ArrayList<String[]>();
		List<String[]> odValues = new ArrayList<String[]>();
		
		/*BufferedReader reader0 = null;
		try {
		    reader0 = new BufferedReader(new FileReader(new File(csvFileODValues)));
		    // String line = reader0.readLine();
		    // odCodesColumns = line.split(";");								// CAUTION: It may also be split by comma "," !!
		    //odLocationsNumber = Integer.parseInt(odCodesColumns[0]);
		} catch (IOException e) {
		    e.printStackTrace();
		} finally {
		    try {
		        reader0.close();
		    } catch (IOException e) {
		        e.printStackTrace();
		    }
		}*/
		
		BufferedReader reader1 = null;
		try {
		    reader1 = new BufferedReader(new FileReader(new File(csvFileODLocations)));
		    String line;
		    while ((line = reader1.readLine()) != null) {
		    	odLocations.add(line.split(";"));								// CAUTION: Comma separation has failed in the past because location names also include commas sometimes!!
		    }
		} catch (IOException e) {
		    e.printStackTrace();
		} finally {
		    try {
		        reader1.close();
		    } catch (IOException e) {
		        e.printStackTrace();
		    }
		}
		
		
		
		BufferedReader reader2 = null;
		try {
		    reader2 = new BufferedReader(new FileReader(new File(csvFileODValues)));
		    String line;
		    while ((line = reader2.readLine()) != null) {
		    	odValues.add(line.split(";"));								// CAUTION: It may also be split by comma "," !!
		    }
		} catch (IOException e) {
		    e.printStackTrace();
		} finally {
		    try {
		        reader2.close();
		    } catch (IOException e) {
		        e.printStackTrace();
		    }
		}

		
		// Store all zone codes here, which have coordinate data because they are stored in ODLocations list
		Set<String> storedLocationCodes = new HashSet<String>(odLocations.size());
		for (int row=0; row<odLocations.size(); row++) {
			storedLocationCodes.add(odLocations.get(row)[0]);
		}
		
		System.out.println("size of storedLocs is: "+storedLocationCodes.size());
		System.out.println("odValues size: "+odValues.size());
		System.out.println("odValues length: "+odValues.get(0).length);
		int counter = 1;


		for (int row = odValues.size()-1; row>0; row--) {					// delete row if the node to be removed is found in the left column=origins
			String[] line = odValues.get(row);
			if(storedLocationCodes.contains(line[0])==false) {
				System.out.println("Removing row with zone code: "+line[0]);
				System.out.println("Remove counter: "+counter);
				counter++;
				odValues.remove(row);
			}
		}
		for (int col = 1; col<odValues.get(0).length; col++) {			// delete column if the node to be removed is found in the top row=destinations
			if (storedLocationCodes.contains(odValues.get(0)[col])==false) {
				System.out.println("Setting to zero zone code: "+odValues.get(0)[col]);
				System.out.println("Remove counter: "+counter);
				counter++;
				for (int r=1; r<odValues.size(); r++) {
					odValues.get(r)[col] = "0";
				}
				// break;
			}
		}
		
		System.out.println("counted removals: "+counter);
		System.out.println("odValues size: "+odValues.size());
		System.out.println("odValues size: "+odValues.size());

		double x;
		double y;
		
		// convert all location strings to ZH CRS
		for (String[] locationCode : odLocations) {
			x = Double.parseDouble(locationCode[2]) + xOffset;
			y = Double.parseDouble(locationCode[3]) + yOffset;
			locationCode[2] = Double.toString(x);			
			locationCode[3] = Double.toString(y);
		}
		
		// delete all zones not within appropriate range
		String removeCode;
		Coord coord;
		for (String[] locationCode : odLocations) {
			x = Double.parseDouble(locationCode[2]);
			y = Double.parseDouble(locationCode[3]);
			coord = new Coord(x, y);
			if (GeomDistance.calculate(coord, cityCenterCoord) < minRadius || GeomDistance.calculate(coord, cityCenterCoord) > maxRadius) {
				removeCode = locationCode[0];
				for (int row = odValues.size()-1; row>0; row--) {					// delete row if the node to be removed is found in the left column=origins
					String[] line = odValues.get(row);
					if(line[0].contains(removeCode)) {
						//System.out.println("Removing row with zone code: "+line[0]);
						odValues.remove(row);
						break;	// remove this break if an origin could appear twice in OD left column!
					}
				}
				for (int col = 1; col<odValues.get(0).length; col++) {			// delete column if the node to be removed is found in the top row=destinations
					if (odValues.get(0)[col].contains(removeCode)) {
						//System.out.println("Removing column with zone code: "+odValues.get(0)[col]);
						for (int r=1; r<odValues.size(); r++) {
							odValues.get(r)[col] = "0";
						}
						break;		// remove this break if an origin could appear twice in OD top row!
					}
				}
			}
		}
		
		/*for (String[] line : odValues) {
			System.out.println("odValueName: "+line[0]);
			System.out.println("odValueName: "+line[1]);
			System.out.println("odValueName: "+line[2]);
			System.out.println("odValueName: "+line[3]);
			System.out.println("odValueName: "+line[4]);
			System.out.println("odValueName: "+line[5]);
			
		}*/
		System.out.println("Remaining origin zones: "+odValues.size());

		// at this point odValues only contain the origins and destinations within the specified metro bounds
		
		Map<String[], Double> odPairs = new HashMap<String[], Double>();
		// initialize Map:
		for (int n=0; n<nRoutes; n++) {
			odPairs.put(new String[]{"",""}, 0.0);
		}

		// %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
		
		for (int row=1; row<odValues.size(); row++) {
			for (int col=1; col<odValues.get(row).length; col++) {
				if (row==col) {
					continue; // do not consider identical OD-Zones
				}
				double thisValue = Double.parseDouble(odValues.get(row)[col]);
				if (thisValue < 0.1) {
					continue;
				}
				String[] minSelectedODpair = findWeakestODpair(odPairs);
				//System.out.println("Weakest (origin): "+minSelectedODpair[0]);
				//System.out.println("Weakest (destination): "+minSelectedODpair[1]);
				double minSelectedODvalue = odPairs.get(minSelectedODpair);
				if (thisValue > minSelectedODvalue) {
					System.out.println("Old min was: "+minSelectedODvalue+" ... This value is "+thisValue+" ... "
							+ "from Zone "+odValues.get(row)[0]+" to Zone "+odValues.get(0)[col]);
					odPairs.remove(minSelectedODpair);
					String[] newODpair= new String[] {odValues.get(row)[0], odValues.get(0)[col]};
					odPairs.put(newODpair, thisValue);
				}
			}
		}
		
		if (odPairs.size() != nRoutes) {
			System.out.println("Fatal Error: nRoutes="+nRoutes+" || odPairs has size"+odPairs.size());
		}
		
		
		Node[][] OD_Terminals = new Node[nRoutes][2];
		int t=0;
		for (String[] terminalPair : odPairs.keySet()) {
			System.out.println("Terminal pair zone1 (origin): "+terminalPair[0].toString());
			System.out.println("Terminal pair zone2 (destination): "+terminalPair[1].toString());
			Coord originCoord = zoneToCoord(terminalPair[0], odLocations);
			Coord destinationCoord = zoneToCoord(terminalPair[1], odLocations);
			OD_Terminals[t][0] = findClosestNode(originCoord, metroNetwork);
			OD_Terminals[t][1] = findClosestNode(destinationCoord, metroNetwork);
			t++;
		}
		
		//convert OD pairs to closest metro candidate node (input = metro candidate nodes)
		return OD_Terminals;
	}
	
		
	private static Node findClosestNode(Coord coordIn, Network metroNetwork) {
		// System.out.println("CoordIn node is: "+coordIn.toString());
		double closestDistance = Integer.MAX_VALUE;
		double distance;
		Node closestNode = null;
		for (Node metroNode : metroNetwork.getNodes().values()) {
			distance = GeomDistance.calculate(coordIn, metroNode.getCoord());
			if (distance<closestDistance) {
				closestDistance = distance;
				closestNode = metroNode;
			}
		}
		System.out.println("Closest node is: "+closestNode.getId().toString());
		System.out.println("Closest distance is: "+closestDistance);
		return closestNode;
	}


	public static String[] findWeakestODpair(Map<String[], Double> odPairs) {
		double min = Integer.MAX_VALUE;
		String[] minODpair = new String[]{"",""};
		for (String[] odPair : odPairs.keySet()) {
			if (odPairs.get(odPair)<min) {
				min = odPairs.get(odPair);
				minODpair = odPair;
			}
		}
		if (minODpair == new String[]{"",""}) {
			System.out.println("CAUTION: No minimum been found for this map of pairs!   ... Returning null ...");
			return null;
		}
		//System.out.println("Minimum value is: "+min);
		//System.out.println("Minimum pair is: "+minODpair[0].toString());
		//System.out.println("Minimum pair is: "+minODpair[1].toString());
		return minODpair;
	}
	
	public static Coord zoneToCoord(String zone, List<String[]> odLocations) {
		Coord coord = new Coord(Double.MAX_VALUE, Double.MAX_VALUE);
		for (String[] line : odLocations) {
			System.out.println("This line's zone is: "+line[0]);
			//System.out.println("This odLocation line is: "+line[0].toString());
			//System.out.println("This odLocation line is: "+line[1].toString());
			//System.out.println("This odLocation line is: "+line[2].toString());
			//System.out.println("This odLocation line is: "+line[3].toString());
			//System.out.println("zone is: "+zone);
			//if (zone.toString() == line[0].toString()) {
			if(Objects.equals(zone, line[0])) {
				coord = new Coord(Double.parseDouble(line[2]), Double.parseDouble(line[3]));
				System.out.println("Good zone found with coords: "+coord.toString());
				break;
			}
		}
		if (coord.getX()>(1.0000000000000000E20)) {
			System.out.println("zone is: "+zone);
			System.out.println("CAUTION: No center coordinates have been found for this zone!   ... Returning MAX_DOUBLE_VALUE ...");
		}
		System.out.println("Coords are: "+coord.toString());
		return coord;
	}
	
	
	public static ArrayList<NetworkRoute> createInitialRoutes(Network metroNetwork,
			int nRoutes, double minRadius, double maxRadius, Coord cityCenterCoord,
			String csvFileODValues, String csvFileODLocations, double xOffset, double yOffset) {
		// TODO Auto-generated method stub
		
		ArrayList<NetworkRoute> networkRouteArray = new ArrayList<NetworkRoute>();
		
		Node[][] OD_Terminals = findODPairs(metroNetwork, csvFileODValues, csvFileODLocations, minRadius, maxRadius, cityCenterCoord, 
				nRoutes, xOffset, yOffset);
		
		int n = 0;
		OuterNetworkRouteLoop:
		while (networkRouteArray.size() < nRoutes) {
			Id<Node> terminalNode1 = OD_Terminals[n][0].getId();
			Id<Node> terminalNode2 = OD_Terminals[n][1].getId();		
			
			// Find Djikstra --> nodeList
			ArrayList<Node> nodeList = DijkstraOwn_I.findShortestPathVirtualNetwork(metroNetwork, terminalNode1, terminalNode2);
			if (nodeList == null) {
					System.out.println("Oops, no shortest path available. Trying to create next networkRoute. Please lower minTerminalDistance"
							+ " ,or increase maxNewMetroLinkDistance (and - last - increase nMostFrequentLinks if required)!");
					continue OuterNetworkRouteLoop;
			}
			List<Id<Link>> linkList = Metro_NetworkImpl.nodeListToNetworkLinkList(metroNetwork, nodeList);
			NetworkRoute networkRoute = RouteUtils.createNetworkRoute(linkList, metroNetwork);
			networkRouteArray.add(networkRoute);
			System.out.println("The new networkRoute is: [Length="+(networkRoute.getLinkIds().size()+2)+"] - " + networkRoute.toString());
			n++;
		}
		
	return networkRouteArray;
}

	
	
	
	
}
