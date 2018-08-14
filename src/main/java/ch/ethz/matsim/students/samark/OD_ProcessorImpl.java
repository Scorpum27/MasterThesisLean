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
	
	public static ArrayList<NetworkRoute> createInitialRoutes(Network metroNetwork,
			int nRoutes, double minRadius, double maxRadius, Coord cityCenterCoord,
			String csvFileODValues, String csvFileODLocations, double xOffset, double yOffset) {
		
		List<String[]> odLocations = new ArrayList<String[]>();
		List<String[]> odValues = new ArrayList<String[]>();
		
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
		
		for (int row = odValues.size()-1; row>0; row--) {					// delete row if the node to be removed is found in the left column=origins
			String[] line = odValues.get(row);
			if(storedLocationCodes.contains(line[0])==false) {
				odValues.remove(row);
			}
		}
		for (int col = 1; col<odValues.get(0).length; col++) {			// delete column if the node to be removed is found in the top row=destinations
			if (storedLocationCodes.contains(odValues.get(0)[col])==false) {
				for (int r=1; r<odValues.size(); r++) {
					odValues.get(r)[col] = "0";
				}
			}
		}
		
		//System.out.println("counted removals: "+counter);
		//System.out.println("odValues size: "+odValues.size());
		//System.out.println("odValues size: "+odValues.size());

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
						odValues.remove(row);
						break;	// remove this break if an origin could appear twice in OD left column!
					}
				}
				for (int col = 1; col<odValues.get(0).length; col++) {			// delete column if the node to be removed is found in the top row=destinations
					if (odValues.get(0)[col].contains(removeCode)) {
						for (int r=1; r<odValues.size(); r++) {
							odValues.get(r)[col] = "0";
						}
						break;		// remove this break if an origin could appear twice in OD top row!
					}
				}
			}
		}	// at this point odValues only contain the origins and destinations within the specified metro bounds
		
		Map<NetworkRoute, Double> odRoutes = new HashMap<NetworkRoute, Double>();
		// initialize Map:
		for (int n=0; n<nRoutes; n++) {
			odRoutes.put(RouteUtils.createNetworkRoute( List.of(metroNetwork.getLinks().keySet().iterator().next()), metroNetwork), 0.0);
		}

		for (int row=1; row<odValues.size(); row++) {
			for (int col=1; col<odValues.get(row).length; col++) {
				if (row==col) {
					continue; // do not consider identical OD-Zones
				}
				double thisValue = Double.parseDouble(odValues.get(row)[col]);
				if (thisValue < 0.1) {
					continue;
				}
				NetworkRoute minSelectedODroute = findWeakestODroute(odRoutes);
				double minSelectedODvalue = odRoutes.get(minSelectedODroute);
				if (thisValue > minSelectedODvalue) {
					Coord originCoord = zoneToCoord(odValues.get(0)[col], odLocations);
					Coord destinationCoord = zoneToCoord(odValues.get(row)[0], odLocations);
					Node originNode = findClosestNode(originCoord, metroNetwork);
					Node destinationNode = findClosestNode(destinationCoord, metroNetwork);
					//
					ArrayList<Node> nodeList = DijkstraOwn_I.findShortestPathVirtualNetwork(metroNetwork, originNode.getId(), destinationNode.getId());
					if (nodeList == null) {
							System.out.println("Oops, no shortest path available. Trying to create next networkRoute. Please lower minTerminalDistance"
									+ " ,or increase maxNewMetroLinkDistance (and - last - increase nMostFrequentLinks if required)!");
							continue;
					}
					List<Id<Link>> linkList = Metro_NetworkImpl.nodeListToNetworkLinkList(metroNetwork, nodeList);
					NetworkRoute networkRoute = RouteUtils.createNetworkRoute(linkList, metroNetwork);
					odRoutes.remove(minSelectedODroute);
					odRoutes.put(networkRoute, thisValue);
					//System.out.println("Old min was: "+minSelectedODvalue+" ... This value is "+thisValue+" ... "
					//		+ "from Zone "+odValues.get(row)[0]+" to Zone "+odValues.get(0)[col]);
				}
			}
		}
		
		if (odRoutes.size() != nRoutes) {
			System.out.println("Fatal Error: nRoutes="+nRoutes+" || odPairs has size"+odRoutes.size());
		}
		
		//convert ODRoutes Map to a networkRoutes Array
		ArrayList<NetworkRoute> networkRouteArray = new ArrayList<NetworkRoute>();
		for (NetworkRoute thisRoute : odRoutes.keySet()) {
			System.out.println("The new networkRoute is: [Length="+(thisRoute.getLinkIds().size()+2)+"] - " +thisRoute.toString());		
			networkRouteArray.add(thisRoute);
		}
		
		return networkRouteArray;
	}
	
	
	// %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%   HELPER METHODS   %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
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
		return closestNode;
	}


	public static NetworkRoute findWeakestODroute(Map<NetworkRoute, Double> odRoutes) {
		double min = Integer.MAX_VALUE;
		NetworkRoute minODroute = null;
		for (NetworkRoute odRoute : odRoutes.keySet()) {
			if (odRoutes.get(odRoute)<min) {
				min = odRoutes.get(odRoute);
				minODroute = odRoute;
			}
		}
		if (minODroute == null) {
			System.out.println("CAUTION: No minimum been found for this map of pairs!   ... Returning null ...");
			return null;
		}
		return minODroute;
	}
	
	public static Coord zoneToCoord(String zone, List<String[]> odLocations) {
		Coord coord = new Coord(Double.MAX_VALUE, Double.MAX_VALUE);
		for (String[] line : odLocations) {
			if(Objects.equals(zone, line[0])) {
				coord = new Coord(Double.parseDouble(line[2]), Double.parseDouble(line[3]));
				break;
			}
		}
		if (coord.getX()>(1.0000000000000000E20)) {
			System.out.println("zone is: "+zone);
			System.out.println("CAUTION: No center coordinates have been found for this zone!   ... Returning MAX_DOUBLE_VALUE ...");
		}
		return coord;
	}

}
