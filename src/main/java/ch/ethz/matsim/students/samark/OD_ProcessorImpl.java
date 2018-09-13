package ch.ethz.matsim.students.samark;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
	
	public static ArrayList<NetworkRoute> createInitialRoutesOD(Network metroNetwork,
			int nRoutes, double minRadius, double maxRadius, double odConsiderationThreshold, Coord cityCenterCoord,
			String csvFileODValues, String csvFileODLocations, double xOffset, double yOffset) throws IOException {
		
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
		
		// convert OdValues to a matrix of Strings for easier processing
		String[][] odValuesX = new String[odValues.size()][odValues.get(0).length];
		for (int row = 0; row < odValues.size(); row++) {
			for (int col = 0; col < odValues.get(0).length; col++) {
				odValuesX[row][col] = odValues.get(row)[col];
			}
		}

		
		Map<String, NetworkRoute> odRoutes = new HashMap<String, NetworkRoute>();
		Map<String, NetworkRoute> odRoutesTemp = new HashMap<String, NetworkRoute>();
		Map<String, Double> odRoutesValues = new HashMap<String, Double>();
		// initialize Map:
		
		for (int n=0; n<nRoutes; n++) {
			odRoutes.put(Integer.toString(n), RouteUtils.createNetworkRoute( List.of(Id.createLinkId("initialLink")), metroNetwork));
			odRoutesValues.put(Integer.toString(n), 0.0);
		}

		boolean changeListener;
		int loopCounter = 0;
		do {
			changeListener = false;
			for (int row=1; row<odValues.size(); row++) {
				InnerOdLoop:
				for (int col=1; col<odValues.get(row).length; col++) {
					if (row==col) {
						continue; // do not consider identical OD-Zones
					}
					double thisValue = Double.parseDouble(odValuesX[row][col]);
					if (thisValue < 0.1) {
						odValuesX[row][col] = "0.0";
						continue;
					}
					String weakestRouteName = findWeakestODroute(odRoutesValues);
					double weakestSelectedODvalue = odRoutesValues.get(weakestRouteName);
					if (thisValue > odConsiderationThreshold*weakestSelectedODvalue) {
						changeListener = true;
						Coord originCoord = zoneToCoord(odValuesX[0][col], odLocations);
						Coord destinationCoord = zoneToCoord(odValuesX[row][0], odLocations);
						Node originNode = findClosestNode(originCoord, metroNetwork);
						Node destinationNode = findClosestNode(destinationCoord, metroNetwork);
						//
						ArrayList<Node> nodeList = DijkstraOwn_I.findShortestPathVirtualNetwork(metroNetwork, originNode.getId(), destinationNode.getId());
						if (nodeList == null) {
								//System.out.println("Oops, no shortest path available. Trying to create next networkRoute. Please lower minTerminalDistance"
								//		+ " ,or increase maxNewMetroLinkDistance (and - last - increase nMostFrequentLinks if required)!");
								continue;
						}
						else {
							//System.out.println("Shortest Path found :-)");
						}
						List<Id<Link>> originalLinkList = Metro_NetworkImpl.nodeListToNetworkLinkList(metroNetwork, nodeList);	// this is the new route candidate;						
						for (Id<Link> thisLinkId : originalLinkList) {
							for (Id<Link> otherLinkId : originalLinkList) {
								if(thisLinkId.equals(otherLinkId)) {
									continue;
								}
								if(thisLinkId.equals(ReverseLink(otherLinkId))) {
									continue InnerOdLoop;
								}
							}
						}
						List<Id<Link>> reverseLinkList = OppositeLinkListOf(originalLinkList);									// this is the reverse version of new route candidate;
						List<List<Id<Link>>> linkListVersions = Arrays.asList(originalLinkList, reverseLinkList);
						//check if one version is already part of an existing route or if we can place it as continuation
						
						odRoutesTemp = CopyRoutesMap(odRoutes);
						for (String networkRouteName : odRoutesTemp.keySet()) { // check against all already selected OD
																				// pairs
							Id<Link> otherStartTerminal = odRoutes.get(networkRouteName).getStartLinkId();
							Id<Link> otherEndTerminal = odRoutes.get(networkRouteName).getEndLinkId();
							List<Id<Link>> otherLinkIds = odRoutes.get(networkRouteName).getLinkIds();
							List<Id<Link>> entireOtherLinkList = new ArrayList<Id<Link>>(otherLinkIds.size() + 2);
							entireOtherLinkList.add(otherStartTerminal);
							entireOtherLinkList.addAll(otherLinkIds);
							entireOtherLinkList.add(otherEndTerminal);
							for (List<Id<Link>> linkList : linkListVersions) {
								// A1
								if (linkList.contains(otherStartTerminal) && linkList.contains(otherEndTerminal)) {
									odRoutes.put(networkRouteName,
											RouteUtils.createNetworkRoute(linkList, metroNetwork));
									odRoutesValues.put(networkRouteName,
											odRoutesValues.get(networkRouteName) + thisValue);
									odValuesX[row][col] = "0.0"; // we have used this OD-pair value now and must set it
									continue InnerOdLoop;
								}
								// A2
								else if (entireOtherLinkList.contains(linkList.get(0))
										&& entireOtherLinkList.contains(linkList.get(linkList.size() - 1))) {
									odRoutesValues.put(networkRouteName,
											odRoutesValues.get(networkRouteName) + thisValue);
									odValuesX[row][col] = "0.0"; // we have used this OD-pair value now and must set it
									continue InnerOdLoop;
								}
							}
							for (List<Id<Link>> linkList : linkListVersions) {
								// B1
								if (entireOtherLinkList.contains(linkList.get(linkList.size() - 1))
										&& linkList.contains(otherStartTerminal)
										&& (entireOtherLinkList.contains(ReverseLink(linkList.get(0))) == false)) {
									// the third condition is for the case that the new link is exactly the
									int S2inN1 = linkList.indexOf(otherStartTerminal);
									linkList.removeAll(linkList.subList(S2inN1, linkList.size()));
									linkList.addAll(entireOtherLinkList);
									odRoutes.put(networkRouteName,
											RouteUtils.createNetworkRoute(linkList, metroNetwork));
									odRoutesValues.put(networkRouteName,
											odRoutesValues.get(networkRouteName) + thisValue);
									odValuesX[row][col] = "0.0"; // we have used this OD-pair value now and must set it
																	// to 0.0.
									continue InnerOdLoop;
								}
								// B2
								else if (entireOtherLinkList.contains(linkList.get(0))
										&& linkList.contains(otherEndTerminal)) {
									int T2inN1 = linkList.indexOf(otherEndTerminal);
									linkList.removeAll(linkList.subList(0, T2inN1 + 1));
									List<Id<Link>> concatenatedLinkList = new ArrayList<Id<Link>>();
									concatenatedLinkList.addAll(entireOtherLinkList);
									concatenatedLinkList.addAll(linkList);
									odRoutes.put(networkRouteName,
											RouteUtils.createNetworkRoute(concatenatedLinkList, metroNetwork));
									odRoutesValues.put(networkRouteName,
											odRoutesValues.get(networkRouteName) + thisValue);
									odValuesX[row][col] = "0.0"; // we have used this OD-pair value now and must set it
									continue InnerOdLoop;
								}
							}
						}
						// C: comes into effect if for loop through all selectedNetwokRoutes has never found a match and did therfore not jump into InnerOdLoop
						if (thisValue > weakestSelectedODvalue) {
							odRoutes.put(weakestRouteName, RouteUtils.createNetworkRoute(originalLinkList, metroNetwork));
							odRoutesValues.put(weakestRouteName, thisValue);
							odValuesX[row][col] = "0.0";
						}
					}
				}
			} 
			loopCounter++;
		}while((changeListener == true || odRoutesValues.values().contains(0.0)) && loopCounter < 200);				// while either a new better OD pair could be added,
																													// a new overlap or extension was found or
																													// bestRouteMaps still has a blank space with value 0.0
		if (odRoutes.size() != nRoutes) {
			System.out.println("Fatal Error: nRoutes="+nRoutes+" || odPairs has size"+odRoutes.size());
		}
		
		//convert ODRoutes Map to a networkRoutes Array
		ArrayList<NetworkRoute> networkRouteArray = new ArrayList<NetworkRoute>();
		// int nr = 0;
		for (NetworkRoute onewayRoute : odRoutes.values()) {
			// extend networkRoutes here with their reverseOption
			List<Id<Link>> twowayLinkList = new ArrayList<Id<Link>>();
			twowayLinkList.add(onewayRoute.getStartLinkId());
			twowayLinkList.addAll(onewayRoute.getLinkIds());
			twowayLinkList.add(onewayRoute.getEndLinkId());
			twowayLinkList.addAll(OppositeLinkListOf(twowayLinkList));
			NetworkRoute twowayRoute = RouteUtils.createNetworkRoute(twowayLinkList, metroNetwork);
			networkRouteArray.add(twowayRoute);
			// nr++;
			// For displaying single routes:
			// NetworkEvolutionImpl.NetworkRouteToNetwork(twowayRoute, metroNetwork, Sets.newHashSet("pt"),
			//		("zurich_1pm/Evolution/Population/Network1/5_zurich_network_MetroInitialRoute"+nr+"_OD.xml"));
		}
		
		return networkRouteArray;
	}
	
	
	
	// %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%   HELPER METHODS   %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
	
	public static List<Id<Link>> OppositeLinkListOf(List<Id<Link>> linkList){
		List<Id<Link>> oppositeLinkList = new ArrayList<Id<Link>>(linkList.size());
		for (int c=0; c<linkList.size(); c++) {
			oppositeLinkList.add(ReverseLink(linkList.get(linkList.size()-1-c)));
		}
		return oppositeLinkList;
	}
	
	public static Id<Link> ReverseLink(Id<Link> linkId){
		String[] linkIdStrings = linkId.toString().split("_");
		Id<Link> reverseId = Id.createLinkId("MetroNodeLinkRef_"+linkIdStrings[3]+"_MetroNodeLinkRef_"+linkIdStrings[1]);
		return reverseId;
	}
	
	public static Map<String, NetworkRoute> CopyRoutesMap(Map<String, NetworkRoute> map) {
		Map<String, NetworkRoute> copiedMap = new HashMap<String, NetworkRoute>();
		for (Map.Entry<String, NetworkRoute> mapEntry : map.entrySet()) {
			copiedMap.put(mapEntry.getKey(), mapEntry.getValue());
		}
		return copiedMap;
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
		return closestNode;
	}


	public static String findWeakestODroute(Map<String, Double> odRoutesValues) {
		double min = Integer.MAX_VALUE;
		String weakestRoute = "";
		for (String routeName : odRoutesValues.keySet()) {
			if (odRoutesValues.get(routeName) < min) {
				min = odRoutesValues.get(routeName);
				weakestRoute = routeName;
			}
		}
		if (weakestRoute == "") {
			System.out.println("CAUTION: No minimum been found for this map of pairs!   ... Returning null ...");
			return null;
		}
		return weakestRoute;
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
