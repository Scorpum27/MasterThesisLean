package ch.ethz.matsim.students.samark;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.xml.stream.XMLStreamException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;


public class Demo {
	
	
	
	
	
	public static void main(String[] args) throws IOException, XMLStreamException {

		PrintWriter pwDefault = new PrintWriter("zurich_1pm/Evolution/Population/LogDefault.txt");	pwDefault.close();	// Prepare empty defaultLog file for run
		
		MNetworkPop mnpop = new MNetworkPop("evoNetworks");
		MNetwork mn1 = XMLOps.readFromFile(MNetwork.class, "Network1TEMP.xml");
//		MNetwork mn1x = XMLOps.readFromFileMNetwork("Network1TEMP.xml");
		MNetwork mn2 = XMLOps.readFromFile(MNetwork.class, "Network2TEMP.xml");
		mnpop.addNetwork(mn1);
		mnpop.addNetwork(mn2);
		int lastIterationOriginal = 1;
		double populationFactor = 1000;
		
		Config config = ConfigUtils.createConfig();
		config.getModules().get("network").addParam("inputNetworkFile", "C:\\Users\\Sascha\\eclipse-workspace\\MATSim-Workspace\\MasterThesis\\zurich_1pm\\Evolution\\Population\\BaseInfrastructure/GlobalNetwork.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Network globalNetwork = scenario.getNetwork();
		
		Map<Id<Link>, CustomMetroLinkAttributes> metroLinkAttributes =
				XMLOps.readFromFile((new HashMap<Id<Link>, CustomMetroLinkAttributes>()).getClass(), "C:\\Users\\Sascha\\eclipse-workspace\\MATSim-Workspace\\MasterThesis\\zurich_1pm\\Evolution\\Population\\BaseInfrastructure/metroLinkAttributes.xml");
		
		for (MNetwork mn : mnpop.networkMap.values()) {
			mn.calculateNetworkScore2(lastIterationOriginal, populationFactor, globalNetwork, metroLinkAttributes); // include here also part of routesHandling
			System.out.println(mn.overallScore);
		}
		
		
		/*
//		Config config = ConfigUtils.createConfig();
//		int n = 20;
//		//String plans = "C:\\Users\\Sascha\\eclipse-workspace\\MATSim-Workspace\\MasterThesis\\zurich_1pm\\Evolution\\Population\\Network1\\Simulation_Output\\ITERS\\it."+n;
//		//config.getModules().get("plans").addParam("inputPlansFile", plans+"/"+n+".plans.xml.gz");
//		String plans = "C:\\Users\\Sascha\\eclipse-workspace\\MATSim-Workspace\\MasterThesis\\zurich_1pm\\Zurich_1pm_SimulationOutput\\ITERS\\it.100\\100.plans.xml.gz";
//		config.getModules().get("plans").addParam("inputPlansFile", plans);
//		Scenario s = ScenarioUtils.loadScenario(config);
//		
//
//		double totalPersons = 0.0;
//		double ptPersons = 0.0;
//		double carPersons = 0.0;
//		double otherTravelTypePersons = 0.0;
//		double carTime = 0.0;
//		double carDist = 0.0;
//		double ptTime = 0.0;
//		double ptDist = 0.0;
//		
//		for (Person p : s.getPopulation().getPersons().values()) {
//			boolean isPtTraveler = false;
//			boolean isCarTraveler = false;
//			totalPersons++;
//			Plan selectedPlan = p.getSelectedPlan();
//			for (PlanElement e : selectedPlan.getPlanElements()) {
//				if (e instanceof Leg) {
//					Leg leg = (Leg) e;
//					if (leg.getMode().contains("car")) {
//						carTime += leg.getTravelTime();
//						carDist += leg.getRoute().getDistance();
//						isCarTraveler = true;
//					}
//					if (leg.getMode().contains("pt") || leg.getMode().contains("access_walk") ||
//							leg.getMode().contains("transit_walk") || leg.getMode().contains("egress_walk")) {
//						ptTime += leg.getTravelTime();
//						ptDist += leg.getRoute().getDistance();
//						isPtTraveler = true;
//					}
//				}
//			}
//			if (isCarTraveler && isPtTraveler) {
//				ptPersons += 0.5;
//				carPersons += 0.5;
//			}
//			else if (isCarTraveler) {
//				carPersons ++;
//			}
//			else if (isPtTraveler) {
//				ptPersons ++;
//			}
//			else {
//				otherTravelTypePersons ++;
//			}
//		}
//		System.out.println("CarTravelers = "+carPersons);
//		System.out.println("PtTravelers = "+ptPersons);
//		System.out.println("OtherTypeTravelers = "+otherTravelTypePersons);
//		System.out.println("Average CarTime [min] = "+carTime/60/carPersons);
//		System.out.println("Average PtTime [min] = "+ptTime/60/ptPersons);
//		System.out.println("Average CarDist [km] = "+carDist/1000/carPersons);
//		System.out.println("Average PtDist [km] = "+ptDist/1000/ptPersons);
		*/
		
		// test plans %%%%%%%%%%%%%%%%%%%%%
		
		/*List<Strings>
        ExecutorService executor = Executors.newFixedThreadPool(4);
		for (MNetwork mNetwork : latestPopulation.getNetworks().values()) {
			if (latestPopulation.modifiedNetworksInLastEvolution.contains(mNetwork.getNetworkID())==false) {
				// must not simulate this loop again, because it has not been changed in last evolution
				// Comment this if lastIteration changes over evolutions !!
				continue;
			}
			mNetwork.evolutionGeneration = generationNr;
			String initialConfig = "zurich_1pm/zurich_config.xml";
			MATSimRunnable matsimrunnable = new MATSimRunnable(args, mNetwork, initialRouteType, initialConfig, lastIteration);
			executor.execute(matsimrunnable);
			NetworkEvolutionRunSim.run(args, mNetwork, initialRouteType, initialConfig, lastIteration);
		} // End Network Simulation Loop
        executor.shutdown();			        // Wait until all threads are finished
        try {
        	  executor.awaitTermination(180*60, TimeUnit.SECONDS);
        	} catch (InterruptedException e) {
        	  Log.writeAndDisplay(e.getMessage().toString());
        }
		Log.write("Completed all MATSim runs.");*/
		
		// another stream 
		/*NetworkEvolutionImpl.saveCurrentMRoutes2HistoryLog(latestPopulation, generationNr, globalNetwork, storeScheduleInterval);			
		
		// - SIMULATION LOOP:
	
		Log.write("SIMULATION of GEN"+generationNr+": ("+lastIteration+" iterations)");
		Log.write("  >> A modification has occured for networks: "+latestPopulation.modifiedNetworksInLastEvolution.toString());
		final int tmpGen = generationNr;

		ForkJoinPool fork.JoinPool = new ForkJoinPool(nrThreads);
		forkJoinPool.submit(()-> plans.parallelStream().forEach(plan->getPlanAlgoInstance().run(plan)));
		
		final MNetworkPop tmpPop = latestPopulation;
		final int tmpGen = generationNr;
		ForkJoinPool forkJoinPool = new ForkJoinPool(4);
		forkJoinPool.submit(()-> tmpPop.getNetworks().values().parallelStream().forEach(mNetwork -> {
			mNetwork.evolutionGeneration = tmpGen;
			String initialConfig = "zurich_1pm/zurich_config.xml";
			try {
				Log.write("Inside parallel MATSim loop of network " + mNetwork.networkID);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
				try {
					NetworkEvolutionRunSim.run(args, mNetwork, initialRouteType, initialConfig, lastIteration);
				} catch (ConfigurationException | IOException e) {
					e.printStackTrace();
				}
		}));
		
		---
		latestPopulation.getNetworks().values().parallelstream().filter(mNetwork -> tmp.modifiedNetworksInLastEvolution.contains(mNetwork.getNetworkID())).forEach(mNetwork -> {
//			if (==false) {
//				// must not simulate this loop again, because it has not been changed in last evolution
//				// Comment this if lastIteration changes over evolutions !!
//				continue;
//			}
			mNetwork.evolutionGeneration = tmpGen;
			String initialConfig = "zurich_1pm/zurich_config.xml";
//			MATSimRunnable matsimrunnable = new MATSimRunnable(args, mNetwork, initialRouteType, initialConfig, lastIteration);
//			executor.execute(matsimrunnable);
			// Alternative: (new ThreadMATSimRun(args, mNetwork, initialRouteType, initialConfig, lastIteration)).start();
			try {
				NetworkEvolutionRunSim.run(args, mNetwork, initialRouteType, initialConfig, lastIteration);
			} catch (ConfigurationException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}); 
//		{
//        executor.shutdown();			        // Wait until all threads are finished
//		} // End Network Simulation Loop

//        try {
//        	  executor.awaitTermination(120*60, TimeUnit.SECONDS);
//        	} catch (InterruptedException e) {
//        	  Log.writeAndDisplay(e.getMessage().toString());
//        	}
		Log.write("Completed all MATSim runs.");
		*/
		
		
		/*Config c2 = ConfigUtils.createConfig();
		c2.getModules().get("network").addParam("inputNetworkFile", "zurich_1pm/Evolution/Population/BaseInfrastructure/TotalMetroNetwork.xml");
		Network metroNetwork = ScenarioUtils.loadScenario(c2).getNetwork();
		
		MRoute mr = XMLOps.readFromFile(MRoute.class, "zurich_1pm/Evolution/Population/HistoryLog/Generation2/MRoutes/Network2_Route2_RoutesFile.xml");
		List<Id<Link>> linkList = NetworkEvolutionImpl.NetworkRoute2LinkIdList(mr.networkRoute);
		System.out.println("LinkList = "+linkList.toString());
		
		List<Link> links = new ArrayList<Link>();
		for (Id<Link> linkId : linkList) {
			links.add(metroNetwork.getLinks().get(linkId));
		}
		
		Link lastLink = links.get(0);
		for (Link thisLink : links.subList(1, links.size())) {
			System.out.println(GeomDistance.angleBetweenLinks(lastLink, thisLink) + "     LastLink="+lastLink.getId().toString());
			lastLink = thisLink;
		}
		
		
//		List<String> strings = new LinkedList<>();
//		strings.stream().parallel().forEach(string -> {
//			
//		});
		*/
		
		
		
		/*Map<String, CustomStop> allMetroStops = new HashMap<String, CustomStop>();
		allMetroStops.putAll(XMLOps.readFromFile(allMetroStops.getClass(), "zurich_1pm/Evolution/Population/BaseInfrastructure/metroStopAttributes.xml"));
		
		Config c1 = ConfigUtils.createConfig();
		c1.getModules().get("network").addParam("inputNetworkFile", "zurich_1pm/Evolution/Population/BaseInfrastructure/GlobalNetwork.xml");
		Network globalNetwork = ScenarioUtils.loadScenario(c1).getNetwork();
		Config c2 = ConfigUtils.createConfig();
		c2.getModules().get("network").addParam("inputNetworkFile", "zurich_1pm/Evolution/Population/BaseInfrastructure/TotalMetroNetwork.xml");
		Network metroNetwork = ScenarioUtils.loadScenario(c2).getNetwork();
		
		for (CustomStop cs : allMetroStops.values()) {
			if (globalNetwork.getNodes().containsKey(cs.newNetworkNode)==false) {
				Log.write("testLogNodes.txt","GlobalNetwork does not contain the node "+cs.newNetworkNode.toString() + " referenced by facility " + cs.transitStopFacility.getName());
			}
			if (metroNetwork.getNodes().containsKey(cs.newNetworkNode)==false) {
				Log.write("testLogNodes.txt","MetroNetwork does not contain the node "+cs.newNetworkNode.toString() + " referenced by facility " + cs.transitStopFacility.getName());
			}
		}*/
		
		
		/*
		MRoute mr = new MRoute("mRoute1���");
		MNetwork mn = new MNetwork("mNetwork1���");
		mn.network = ScenarioUtils.loadScenario(ConfigUtils.createConfig()).getNetwork();
		MNetworkPop mnp = new MNetworkPop("mNetworkPop1���");
		mn.addNetworkRoute(mr);
		mnp.addNetwork(mn);
		
		CustomMetroLinkAttributes cmla = new CustomMetroLinkAttributes("testLink���", Id.createLinkId("originalLinkId"));
		Map<Id<Link>, CustomMetroLinkAttributes> metroLinkAtt = new HashMap<Id<Link>, CustomMetroLinkAttributes>();
		metroLinkAtt.put(Id.createLinkId("testLinkId"), cmla);
		
		XMLOps.writeToFile(metroLinkAtt, "metroLinkAtt.xml");
		XMLOps.writeToFileMNetwork(mn, "mn.xml");
		XMLOps.writeToFileMNetworkPop(mnp, "mnp.xml");
		
		Map<Id<Link>, CustomMetroLinkAttributes> metroLinkAtt2 = XMLOps.readFromFile(metroLinkAtt.getClass(), "metroLinkAtt.xml");
		MNetwork mn2 = XMLOps.readFromFileMNetwork("mn.xml");
		MNetworkPop mnp2 = XMLOps.readFromFileMNetworkPop("mnp.xml");
		
		System.out.println(metroLinkAtt2.get(Id.createLinkId("testLinkId")).type.toString());
		System.out.println(mn2.networkID);
		System.out.println(mnp2.populationId);*/
		
		
		// DISPLAY TIME
//        Calendar cal = Calendar.getInstance();
//        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
//        System.out.println( (new SimpleDateFormat("HH:mm:ss")).format(Calendar.getInstance().getTime()) );
		
		// RUNNABLE
		
		/*
		System.out.println("Inside : " + Thread.currentThread().getName());
		int i = 2;
        System.out.println("Creating Runnable...");
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                System.out.println("Inside : " + Thread.currentThread().getName());
            }
        };

        System.out.println("Creating Thread...");
        Thread thread = new Thread(runnable);

        System.out.println("Starting Thread...");
        thread.start();*/
        
		
		// EVOLUTIONARY PROCESS
		/*Config config = ConfigUtils.createConfig();
		config.getModules().get("network").addParam("inputNetworkFile", "zurich_1pm/Evolution/Population/TotalMetroNetwork.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Network globalNetwork = scenario.getNetwork();
		
		Map<Id<Link>, CustomMetroLinkAttributes> metroLinkAttributes = new HashMap<Id<Link>, CustomMetroLinkAttributes>();
		metroLinkAttributes = XMLOps.readFromFile(metroLinkAttributes.getClass(), "zurich_1pm/Evolution/Population/MetroLinkAttributes.xml");

		for (Id<Link> linkid : globalNetwork.getLinks().keySet()) {
			System.out.println(linkid.toString());
			if (metroLinkAttributes.get(linkid) == null) {
				System.out.println("Cannot find this linkId in metroLinkAttributes!");
				break;
			}
			TransitStopFacility tsf = NetworkEvolutionImpl.searchStopFacilitiesOnLink(metroLinkAttributes, globalNetwork.getLinks().get(linkid));
			if (tsf == null) {
				System.out.println("No facility found on link="+linkid.toString());
			}
			else {
				System.out.println("Stop facility=" + tsf.getName());
			}
		}*/
		
//		System.out.println(Charset.defaultCharset().toString());
		
//		int generationNr = 2;
//		int storeScheduleInterval = 1;
//		if ((generationNr % storeScheduleInterval) == 0) {
//			File sourceSchedule = new File("zurich_1pm/Evolution/Population/MergedSchedule.xml");
//			File destSchedule = new File("zurich_1pm/Evolution/Population/HistoryLog/Generation"+(generationNr)+"/Network1/MergedSchedule.xml");
//			File sourceVehicles = new File("zurich_1pm/Evolution/Population/MergedVehicles.xml");
//			File destVehicles = new File("zurich_1pm/Evolution/Population/HistoryLog/Generation"+(generationNr)+"/Network1/MergedVehicles.xml");		
//			try {
//			    FileUtils.copyFile(sourceSchedule, destSchedule);
//			    FileUtils.copyFile(sourceVehicles, destVehicles);
//			} catch (IOException e) {
//			    e.printStackTrace();
//			}
//		}
		
//		System.out.println((4%3));
		
//		MNetwork loadedNetwork = new MNetwork("Network1");
//		String routeFilePath = "zurich_1pm/Evolution/Population/HistoryLog/Generation3/MRoutes/Network1_Route1_RoutesFile.xml";
//		MRoute loadedRoute = XMLOps.readFromFile(MRoute.class, routeFilePath);
//		for (TransitRoute tr : loadedRoute.transitLine.getRoutes().values()) {
//			for (TransitRouteStop trs : tr.getStops()) {
//				System.out.println(trs.getStopFacility().getName());
//			}
//		}
//		System.out.println(loadedRoute.routeID);
//		
//		XMLInputFactory factory = XMLInputFactory.newInstance();
//		loadedRoute = (MRoute) factory.createXMLStreamReader(new FileInputStream(routeFilePath));
//		System.out.println(loadedRoute.routeID);

		
		
	// String splitting trials:
	
		/*String s1 = " he.l,-/__//\\'.-'\\,lo-,";
		String s2 = NetworkEvolutionImpl.removeSpecialChar(s1);
		String s3 = NetworkEvolutionImpl.removeStrings(s1, Arrays.asList("_","-",","," ",".","'"));
		System.out.println(s2);*/
		
		
	// Merging network trials:

//		System.out.println(NetworkEvolutionImpl.removeString(s1, "."));

//		Network nw1 = ScenarioUtils.loadScenario(ConfigUtils.createConfig()).getNetwork();
//		NetworkFactory nwf1 = nw1.getFactory();
//		Node a1 = nwf1.createNode(Id.createNodeId("a1"), new Coord(0.0, 0.0));
//		Node a2 = nwf1.createNode(Id.createNodeId("a2"), new Coord(1.0, 0.0));
//		Link l1 = nwf1.createLink(Id.createLinkId("l1"), a1, a2);
//		nw1.addNode(a1);
//		nw1.addNode(a2);
//		nw1.addLink(l1);
//
//		Network nw2 = ScenarioUtils.loadScenario(ConfigUtils.createConfig()).getNetwork();
//		NetworkFactory nwf2 = nw2.getFactory();
//		Node a3 = nwf2.createNode(Id.createNodeId("a3"), new Coord(-1.0, 0.0));
//		Node a4 = nwf2.createNode(Id.createNodeId("a1"), new Coord(0.0, 0.0));
//		Node a5 = nwf2.createNode(Id.createNodeId("a5"), new Coord(0.0, 1.0));
//		Link l2 = nwf2.createLink(Id.createLinkId("l2"), a4, a5);
//		Link l3 = nwf2.createLink(Id.createLinkId("l3"), a4, a3);
//		nw2.addNode(a3);
//		nw2.addNode(a4);
//		nw2.addNode(a5);
//		nw2.addLink(l2);
//		nw2.addLink(l3);
//		
//		Network nw4 = Metro_NetworkImpl.mergeNetworks(nw1, nw2, null);
//		Network nw3 = NetworkEvolutionImpl.mergeNetworksX(nw1, nw2);
//		for (Node node : nw3.getNodes().values()) {
//			System.out.println(node);
//		}
//		for (Link link : nw3.getLinks().values()) {
//			System.out.println(link);
//			System.out.println("FromNode = "+link.getFromNode());
//			System.out.println("ToNode = "+link.getToNode());
//		}
		
		
	// ApplyFrequencyModification --> Explanation:
			// Error happened because last and second last route were deleted in the same top loop, while their place was not deleted in perf order.
			// Lower loop than took elements from perfOrderList and tried to find in routesMap. However, they had already been deleted --> NullPointerException
		
		
		/*PrintWriter pw = new PrintWriter("demoLog.txt");	pw.close();		// Prepare empty log file for run
		
		MNetworkPop pop = new MNetworkPop("evoNetworks");
		MNetwork mn1 = new MNetwork("Network1");
		pop.addNetwork(mn1);
		MRoute mr1 = new MRoute("Network1_Route1");
		MRoute mr2 = new MRoute("Network1_Route2");
		MRoute mr3 = new MRoute("Network1_Route3");
		MRoute mr4 = new MRoute("Network1_Route4");
		MRoute mr5 = new MRoute("Network1_Route5");
		mn1.addNetworkRoute(mr1);
		mn1.addNetworkRoute(mr2);
		mn1.addNetworkRoute(mr3);
		mn1.addNetworkRoute(mr4);
		mn1.addNetworkRoute(mr5);
		mr1.personMetroKM = 1;
		mr1.drivenKM = 1;
		mr1.vehiclesNr = 1;
		mr2.personMetroKM = 2;
		mr2.drivenKM = 1;
		mr2.vehiclesNr = 1;
		mr3.personMetroKM = 3;
		mr3.drivenKM = 1;
		mr3.vehiclesNr = 1;
		mr4.personMetroKM = 4;
		mr4.drivenKM = 1;
		mr4.vehiclesNr = 1;
		mr5.personMetroKM = 5;
		mr5.drivenKM = 1;
		mr5.vehiclesNr = 1;
		
		for (int i=0; i<30; i++) {
			Log.write("demoLog.txt","ITER = "+i);
			for(MNetwork mn : pop.networkMap.values()) {
				Map<String, Double> routePerformances = new HashMap<String, Double>();
				for (MRoute mr : mn.routeMap.values()) {
					Log.write("demoLog.txt","ROUTE = "+mr.routeID);
					routePerformances.put(mr.routeID, mr.personMetroKM/mr.drivenKM);
				}
				List<String> routePerformanceOrder = NetworkEvolutionImpl.sortMapByValueScore(routePerformances);
				Log.write("demoLog.txt","RouteMapSize="+routePerformanceOrder.size()+"  -->PerformanceOrder="+routePerformanceOrder.toString());
				if (mn.routeMap.size()>3) {
					Log.write("demoLog.txt","1st loop shift with SIZE="+routePerformanceOrder.size());
					Random r1 = new Random();
					Random r2 = new Random();
					if (r1.nextDouble() < 0.67) {
						Log.write("demoLog.txt","Shifting one vehicle from weakest " + routePerformanceOrder.get(routePerformanceOrder.size()-1) + " to strongest " + routePerformanceOrder.get(0));
						mn.routeMap.get(routePerformanceOrder.get(0)).vehiclesNr++;
						mn.routeMap.get(routePerformanceOrder.get(routePerformanceOrder.size()-1)).vehiclesNr--;
						if (mn.routeMap.get(routePerformanceOrder.get(routePerformanceOrder.size()-1)).vehiclesNr < 1) {
							Log.write("Oops, " + routePerformanceOrder.get(routePerformanceOrder.size()-1) + " has died due to no more vehicles");
							mn.routeMap.remove(routePerformanceOrder.get(routePerformanceOrder.size()-1));
						}
					}
					if (r2.nextDouble() < 1.00) {//0.33) {
						Log.write("demoLog.txt","Shifting one vehicle from second weakest " + routePerformanceOrder.get(routePerformanceOrder.size()-2) + " to second strongest" + routePerformanceOrder.get(1));
						mn.routeMap.get(routePerformanceOrder.get(1)).vehiclesNr++;
						mn.routeMap.get(routePerformanceOrder.get(routePerformanceOrder.size()-2)).vehiclesNr--;
						if (mn.routeMap.get(routePerformanceOrder.get(routePerformanceOrder.size()-2)).vehiclesNr < 1) {
							Log.write("demoLog.txt","Oops, " + routePerformanceOrder.get(routePerformanceOrder.size()-2) + " has died due to no more vehicles");
							mn.routeMap.remove(routePerformanceOrder.get(routePerformanceOrder.size()-2));
						}
					}
				}
				if ( 4 > mn.routeMap.size() && mn.routeMap.size() > 1 ) {
					Random r1 = new Random();
					if (r1.nextDouble() < 0.67) {
						Log.write("demoLog.txt","2nd loop shift with SIZE="+routePerformanceOrder.size());
						// Log.write("Shifting one vehicle from weakest " + routePerformanceOrder.get(routePerformanceOrder.size()-1) + " to strongest " + routePerformanceOrder.get(0));
						mn.routeMap.get(routePerformanceOrder.get(0)).vehiclesNr++;
						mn.routeMap.get(routePerformanceOrder.get(routePerformanceOrder.size()-1)).vehiclesNr--;
						if (mn.routeMap.get(routePerformanceOrder.get(routePerformanceOrder.size()-1)).vehiclesNr < 1) {
							Log.write("demoLog.txt","   >> Oops, " + routePerformanceOrder.get(routePerformanceOrder.size()-1) + " has died due to no more vehicles");
							mn.routeMap.remove(routePerformanceOrder.get(routePerformanceOrder.size()-1));
						}
					}
				}
			}
		}*/
		
		
	// Event reading

//		System.out.println(NetworkEvolutionImpl.removeString("hello", "l"));
//		Network nw = ScenarioUtils.loadScenario(ConfigUtils.createConfig()).getNetwork();
//		NetworkFactory nf = nw.getFactory();
//		nf.createLink(Id.createLinkId("testLink"), null, null);
		
		
	// Extracting S-Bahn stops in ZH
			
		/*Config config = ConfigUtils.createConfig();
		config.getModules().get("transit").addParam("transitScheduleFile","zurich_1pm/zurich_transit_schedule.xml.gz");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		TransitSchedule ts = scenario.getTransitSchedule();
		//		Network globalNetwork = scenario.getNetwork();
		
		Map<String,CustomRailStop> railStops = new HashMap<String,CustomRailStop>();
		
		for (TransitLine tl : ts.getTransitLines().values()) {
			for (TransitRoute tr : tl.getRoutes().values()) {
				if (tr.getTransportMode().toString().equals("rail")) {
					for (TransitRouteStop trs : tr.getStops()) {
						TransitStopFacility tsf = trs.getStopFacility();
						String stopName = tsf.getId().toString().substring(0, tsf.getId().toString().indexOf("."));
						if (railStops.keySet().contains(stopName) == false) {
							railStops.put(stopName, new CustomRailStop(tsf.getName(), "rail", tsf.getCoord(), tsf.getLinkId()));							
						}
						else {
							if (railStops.get(stopName).linkRefIds.contains(tsf.getLinkId())==false) {
								railStops.get(stopName).linkRefIds.add(tsf.getLinkId());
							}
						}
					}
				}
			}
		}
		
		Iterator<Entry<String, CustomRailStop>> iter = railStops.entrySet().iterator();
		int n=0;
		while (iter.hasNext() && n<100) {
			n++;
			Entry<String, CustomRailStop> entry = iter.next();
			if(entry.getValue().linkRefIds.size()>3) {
			System.out.println(entry.getKey()+", Nr_LinkRefIds="+entry.getValue().linkRefIds.size());
			}
		}
		
		
		double radius = 50000.0;
		Coord zurich_NetworkCenterCoord = new Coord(2683000.00, 1247700.00);
		Iterator<Entry<String, CustomRailStop>> stopEntryIter = railStops.entrySet().iterator();
		while(stopEntryIter.hasNext()) {
			Entry<String, CustomRailStop> stopEntry = stopEntryIter.next();
			if(GeomDistance.calculate(zurich_NetworkCenterCoord, stopEntry.getValue().coord) > radius) {
				stopEntryIter.remove();
			}
		}
		
		Iterator<Entry<String, CustomRailStop>> iterX = railStops.entrySet().iterator();
		while (iterX.hasNext()) {
			Entry<String, CustomRailStop> entryX = iterX.next();
			System.out.println(entryX.getValue().name);
		}
		
		
		Network railNetwork = scenario.getNetwork();
		NetworkFactory nf = railNetwork.getFactory();
		for(String stop : railStops.keySet()) {
			railNetwork.addNode(nf.createNode(Id.createNodeId(stop), railStops.get(stop).coord));
		}
		NetworkWriter nw = new NetworkWriter(railNetwork);
		nw.write("zurich_1pm/zurich_networkRailStationsRadius"+((int) radius)+".xml");*/
		
		
	// SORTING
		
		/*Map<String, Double> r = new HashMap<String, Double>();
		r.put("two", 2.0);
		r.put("four", 4.0);
		r.put("three", 3.0);
		r.put("one", 1.0);
		
		System.out.println(NetworkEvolutionImpl.sortMapByValueScore(r));*/
		
	// PLOTTING

		// NetworkEvolutionImpl.writeChartAverageTravelTimes(5, 6, 5, 4, "zurich_1pm/Evolution/Population/networkTravelTimesEvolution.png");
		// NetworkEvolutionImpl.writeChartNetworkScore(5, 6, 5, 4, "zurich_1pm/Evolution/Population/networkScoreEvolution.png");
		
	// Geometry
		
		/*System.out.println(GeomDistance.angleBetweenPoints(-0.0,-0.0, 0.5,-1.0 ,0.0,0.0, -1.0,1.0));
		System.out.println(GeomDistance.absoluteAngle(0.0,0.5,0.0,-1.0));*/
		
	// Short Routes
		/*List<String> list = new ArrayList<String>();
		list.add("1");
		list.add("2");
		list.add("3");
		list.add("4");
		list.add("5");
		list.add("6");
		list.add("7");
		list.add("8");
		list.add("9");
		list.add("10");
		System.out.println(list.subList((int) Math.floor((1.0/3.0)*list.size()), (int) Math.ceil((2.0/3.0)*list.size())));
		System.out.println(list.subList(0, list.size()/2));
		System.out.println(list.subList(list.size()/2, list.size()));
		//list.remove("5");
		System.out.println(list.toString());
		System.out.println(list.subList(0, 0).toString());*/
		
	// Simple Maths
		 /*int a =  (int) Math.round(0.5*(0.5+0.3));
		 int b = (int) Math.round(0.5*(0.5+1.3));
		 System.out.println(a);
		 System.out.println(b);*/
		
	// Clone Tester
		
		/*Set<String> l1 = new HashSet<String>();
		l1.add("hello");
		l1.add("hope it works");
		Set<String> l2 = Clone.set(l1);
		System.out.println(l2.toString());*/
		
	// MRoute Dynamic map during loop tester
		
		/*MNetworkPop mpop = new MNetworkPop("Mpop");
		MNetwork mn1 = new MNetwork("mn1");
		MNetwork mn2 = new MNetwork("mn2");
		MNetwork mn3 = new MNetwork("mn3");
		mpop.addNetwork(mn1);
		mpop.addNetwork(mn2);
		mpop.addNetwork(mn3);
		
		Map<String, MNetwork> mnMap = new HashMap<String, MNetwork>();
		mnMap = mpop.getNetworks();
		mnMap.get("mn1").networkID = "mn2";
		System.out.println(mnMap.get("mn1").networkID);
		for (String mns : mpop.networkMap.keySet()) {
			System.out.println(mpop.networkMap.get(mns).networkID);
		}
		
		mpop.addNetwork(mn1);*/
		
			
	// %%%%%%%%%%%%%%%%%%%% SCORES PLOTTER %%%%%%%%%%%%%%%%%%%%

		/*int generationsToPlot = 4-1;	// always one less than last generation (bc last evolution is not simulated)
		NetworkEvolutionImpl.writeChartAverageTravelTimes(generationsToPlot, "zurich_1pm/Evolution/Population/networkTravelTimesEvolution.png");
		NetworkEvolutionImpl.writeChartNetworkScore(generationsToPlot, "zurich_1pm/Evolution/Population/networkScoreEvolution.png");*/
		
	// %%%%%%%%%%%%%%%%%%%% CROSSOVER TESTER %%%%%%%%%%%%%%%%%%%%

	// Test Network:	//	   14      19
						//	    |       |
						//	5---6---7---8---9
						//	    |       |		
						//	   12       17
						//	    |       |		   
						//	0---1---2---3---4
						//	    |       |	
						//	   12       15
		
		/*Config config = ConfigUtils.createConfig();
		Scenario scenario0 = ScenarioUtils.loadScenario(config);
		Network network = scenario0.getNetwork();
		NetworkFactory nf = network.getFactory();
		config.getModules().get("network").addParam("inputNetworkFile", "zurich_1pm/Evolution/Population/GlobalMetroNetwork.xml");
		Scenario scenario1 = ScenarioUtils.loadScenario(config);
		Network globalNetwork = scenario1.getNetwork();
		
		
		Node thisnode;
		Node lastnode = null;
		Link link;
		Link reverseLink;
		
		List<Id<Link>> linkList1 = new ArrayList<Id<Link>>();
		for (int n=0; n<5; n++) {
			thisnode = nf.createNode(Id.createNodeId(n), new Coord(1.0*n, 0.0));
			network.addNode(thisnode);
			if(n!=0) {
				link = nf.createLink(Id.createLinkId("MetroNodeLinkRef_"+lastnode.getId().toString()+"_MetroNodeLinkRef_"+thisnode.getId().toString()), lastnode, thisnode);
				reverseLink = nf.createLink(Id.createLinkId("MetroNodeLinkRef_"+thisnode.getId().toString()+"_MetroNodeLinkRef_"+lastnode.getId().toString()), thisnode, lastnode);
				network.addLink(link);
				network.addLink(reverseLink);
				linkList1.add(link.getId());
			}
			lastnode = thisnode;
		}
		linkList1.addAll(NetworkEvolutionImpl.OppositeLinkListOf(linkList1));
		NetworkRoute nr1 = RouteUtils.createNetworkRoute(linkList1, network);
		MRoute mr1 = new MRoute("Network1_Route1");
		mr1.networkRoute = nr1;
		mr1.linkList = linkList1;

		
		List<Id<Link>> linkList2 = new ArrayList<Id<Link>>();
		for (int n=0; n<5; n++) {
			thisnode = nf.createNode(Id.createNodeId(n+5), new Coord(1.0*n, 2.0));
			network.addNode(thisnode);
			if(n!=0) {
				link = nf.createLink(Id.createLinkId("MetroNodeLinkRef_"+lastnode.getId().toString()+"_MetroNodeLinkRef_"+thisnode.getId().toString()), lastnode, thisnode);
				reverseLink = nf.createLink(Id.createLinkId("MetroNodeLinkRef_"+thisnode.getId().toString()+"_MetroNodeLinkRef_"+lastnode.getId().toString()), thisnode, lastnode);
				network.addLink(link);
				network.addLink(reverseLink);
				linkList2.add(link.getId());
			}
			lastnode = thisnode;
		}
		linkList2.addAll(NetworkEvolutionImpl.OppositeLinkListOf(linkList2));
		NetworkRoute nr2 = RouteUtils.createNetworkRoute(linkList2, network);
		MRoute mr2 = new MRoute("Network1_Route2");
		mr2.networkRoute = nr2;
		mr2.linkList = linkList2;


		
		List<Id<Link>> linkList3 = new ArrayList<Id<Link>>();
		for (int n=0; n<5; n++) {
			thisnode = nf.createNode(Id.createNodeId(n+10), new Coord(1.0, 1.0*(n-1)));
			if(n==1) {
				thisnode = network.getNodes().get(Id.createNodeId(1));
			}
			else if(n==3) {
				thisnode = network.getNodes().get(Id.createNodeId(6));
			}
			else {network.addNode(thisnode);}
			if(n!=0) {
				link = nf.createLink(Id.createLinkId("MetroNodeLinkRef_"+lastnode.getId().toString()+"_MetroNodeLinkRef_"+thisnode.getId().toString()), lastnode, thisnode);
				reverseLink = nf.createLink(Id.createLinkId("MetroNodeLinkRef_"+thisnode.getId().toString()+"_MetroNodeLinkRef_"+lastnode.getId().toString()), thisnode, lastnode);
				network.addLink(link);
				network.addLink(reverseLink);
				linkList3.add(link.getId());
			}
			lastnode = thisnode;
		}
		linkList3.addAll(NetworkEvolutionImpl.OppositeLinkListOf(linkList3));
		NetworkRoute nr3 = RouteUtils.createNetworkRoute(linkList3, network);
		MRoute mr3 = new MRoute("Network2_Route1");
		mr3.networkRoute = nr3;
		mr3.linkList = linkList3;


		
		List<Id<Link>> linkList4 = new ArrayList<Id<Link>>();
		for (int n=0; n<5; n++) {
			thisnode = nf.createNode(Id.createNodeId(n+15), new Coord(3.0, 1.0*(n-1)));
			if(n==1) {
				thisnode = network.getNodes().get(Id.createNodeId(3));
			}
			else if(n==3) {
				thisnode = network.getNodes().get(Id.createNodeId(8));
			}
			else{ network.addNode(thisnode); }
			if(n!=0) {
				link = nf.createLink(Id.createLinkId("MetroNodeLinkRef_"+lastnode.getId().toString()+"_MetroNodeLinkRef_"+thisnode.getId().toString()), lastnode, thisnode);
				reverseLink = nf.createLink(Id.createLinkId("MetroNodeLinkRef_"+thisnode.getId().toString()+"_MetroNodeLinkRef_"+lastnode.getId().toString()), thisnode, lastnode);
				network.addLink(link);
				network.addLink(reverseLink);
				linkList4.add(link.getId());
			}
			lastnode = thisnode;
		}
		linkList4.addAll(NetworkEvolutionImpl.OppositeLinkListOf(linkList4));
		NetworkRoute nr4 = RouteUtils.createNetworkRoute(linkList4, network);
		MRoute mr4 = new MRoute("Network2_Route2");
		mr4.networkRoute = nr4;
		mr4.linkList = linkList4;



		MNetworkPop evoNetworksToProcessPlans = new MNetworkPop("evoNetworks");
		MNetwork mn1 = new MNetwork("Network1");
		mn1.network = network;
		mn1.routeMap.put(mr1.routeID, mr1);
		mn1.routeMap.put(mr2.routeID, mr2);
		evoNetworksToProcessPlans.addNetwork(mn1);
		MNetwork mn2 = new MNetwork("Network2");
		mn2.network = network;
		mn2.routeMap.put(mr3.routeID, mr3);
		mn2.routeMap.put(mr4.routeID, mr4);
		evoNetworksToProcessPlans.addNetwork(mn2);		
		
		
		Map<String,NetworkScoreLog> networkScoreMap = new HashMap<String,NetworkScoreLog>();
		NetworkScoreLog nsl1 = new NetworkScoreLog();
		nsl1.overallScore = 1.00;
		networkScoreMap.put("Network1", nsl1);
		NetworkScoreLog nsl2 = new NetworkScoreLog();
		nsl2.overallScore = 1.01;
		networkScoreMap.put("Network2", nsl2);
		

	for (int g=0; g<10; g++) {
		
		MNetworkPop newPopulation = new MNetworkPop(evoNetworksToProcessPlans.populationId);
		newPopulation.networkMap = evoNetworksToProcessPlans.networkMap;
		int nOldPop = newPopulation.networkMap.size();
		System.out.println("Old Pop size ="+nOldPop);
		
		// List<MRoute> offspringRoutes = new ArrayList<MRoute>();
		// find and store Elite network
		String eliteNetwork = "";
		if (networkScoreMap.size() == 0) {		System.out.println("CAUTION: NetworkScoreMapSize is zero!");	}
		double maxNetworkScore = -Double.MAX_VALUE;
		for (String networkName : networkScoreMap.keySet()) {
			if (networkScoreMap.get(networkName).overallScore > maxNetworkScore) {
				maxNetworkScore = networkScoreMap.get(networkName).overallScore;
				eliteNetwork = networkName;
			}
		}
		MNetwork eliteMNetwork = evoNetworksToProcessPlans.getNetworks().get(eliteNetwork);
		System.out.println("EliteNetwork="+eliteNetwork);

		
		String vehicleTypeName = "metro";  double maxVelocity = 70/3.6;
		double vehicleLength = 50;  int vehicleSeats = 100; int vehicleStandingRoom = 100;
//		double tFirstDep = 6.0*60*60;  double tLastDep = 20.5*60*60;  double depSpacing = 7.5*60;
//		int nDepartures = (int) ((tLastDep-tFirstDep)/depSpacing);
		double stopTime = 30.0;  String defaultPtMode = "metro";  boolean blocksLane = false;
		double metroOpsCostPerKM = 1000; double metroConstructionCostPerKmOverground = 1000000; double metroConstructionCostPerKmUnderground = 10000000;
		String initialRouteType = "Random";											// Options: {"OD","Random"}	-- Choose method to create initial routes [OD=StrongestOriginDestinationShortestPaths, Random=RandomTerminals in outer frame of specified network]
		boolean useOdPairsForInitialRoutes = false;									// For OD also modify as follows: minTerminalRadiusFromCenter = 0.00*metroCityRadius
		if (initialRouteType.equals("OD")) { useOdPairsForInitialRoutes = true; }
		int iterationToReadOriginalNetwork = 100;
		
		// CROSS-OVERS
		int nCrossOverCandidates = (int) Math.ceil(0.5*nOldPop);
		List<MNetwork> newOffspring = new ArrayList<MNetwork>();
		System.out.println("We will try nCrossOverCandidates="+nCrossOverCandidates);
		
		double pCrossOver=1.0;
		double alpha = 10.0;
		for (int n=0; n<nCrossOverCandidates; n++) {
			Random r = new Random();
			if (r.nextDouble()<pCrossOver) {
				String nameParent1;
				String nameParent2;
				nameParent1 = NetworkEvolutionImpl.selectMNetworkByRoulette(alpha, networkScoreMap);
				System.out.println("ParentName 1="+nameParent1);
				do{
					nameParent2 = NetworkEvolutionImpl.selectMNetworkByRoulette(alpha, networkScoreMap);
				}while(nameParent1.equals(nameParent2));
				System.out.println("ParentName 2="+nameParent2);
				MNetwork parentMNetwork1 = evoNetworksToProcessPlans.getNetworks().get(nameParent1);
				MNetwork parentMNetwork2 = evoNetworksToProcessPlans.getNetworks().get(nameParent2);
				MNetwork[] offspringMNetworks = NetworkEvolutionImpl.crossMNetworks(network, parentMNetwork1, parentMNetwork2,
						vehicleTypeName, vehicleLength, maxVelocity, vehicleSeats, vehicleStandingRoom, defaultPtMode,
						stopTime, blocksLane, metroConstructionCostPerKmOverground,
						metroConstructionCostPerKmUnderground, metroOpsCostPerKM, iterationToReadOriginalNetwork,
						useOdPairsForInitialRoutes); // (make sure IDs are same as parent Networks to remove old network adding to newPopulation)
				newOffspring.add(offspringMNetworks[0]);
				newOffspring.add(offspringMNetworks[1]);
				for (String mr : newOffspring.get(0).routeMap.keySet()) {
					System.out.println("New offspring Network1 includes route = "+newOffspring.get(0).routeMap.get(mr).linkList.toString());
				}
				for (String mr : newOffspring.get(1).routeMap.keySet()) {
					System.out.println("New offspring Network2 includes route = "+newOffspring.get(1).routeMap.get(mr).linkList.toString());
				}
			}
		}
		int nNewOffspring = newOffspring.size();
		System.out.println("nNewOffspring="+nNewOffspring);
		if(nNewOffspring != 0) {
			List<String> deletedNetworkNames = NetworkEvolutionImpl.RemoveWeakestNetworks(newPopulation, nNewOffspring);
			System.out.println("deletedNetworkNames="+deletedNetworkNames.toString());
			do {
				NetworkEvolutionImpl.RenameOffspring(deletedNetworkNames.get(0), newOffspring.get(0));	// renaming offspring with its MNetworkId and the Id of all its MRoutes
				newPopulation.networkMap.put(newOffspring.get(0).networkID, newOffspring.get(0));
				deletedNetworkNames.remove(0);
				newOffspring.remove(0);
			}while(deletedNetworkNames.size()>0);
		}
		if (nNewOffspring == nOldPop) {										// check with this condition if all old networks have been deleted for new offspring
			newPopulation.networkMap.put(eliteNetwork, eliteMNetwork);		// if also elite network has been deleted, add manually again (it will replace the new one with the same name)
		}
		
		
		// read out all final network routes !!
		for(MNetwork mnetwork : newPopulation.networkMap.values()) {
			for (String mr : mnetwork.routeMap.keySet()) {
				System.out.println("Network="+mnetwork.networkID+"   |   Route="+mnetwork.routeMap.get(mr).linkList.toString());
			}
		}
		
		evoNetworksToProcessPlans = newPopulation;
	}*/
	
		
	// %%%%%%%%%%%%%%%%%%%% MODIFYING MAP WHILE LOOPING IT --> TEMP_MAP %%%%%%%%%%%%%%%%%%%%

				/*Map<Integer, Integer> mergedInt = new HashMap<Integer, Integer>();
				for (int i=0; i<13; i++) {
					mergedInt.put(i, 1);
				}
				
				// Map<Integer, Integer> mergedIntCopy = mergedInt;
				Map<Integer, Integer> mergedIntX = new HashMap<Integer, Integer>();
				
				do {	
					List<Integer> toBeDeletedInts = new ArrayList<Integer>();
					Iterator<Integer> intIter = mergedInt.keySet().iterator();
					int thisInt = intIter.next();
					System.out.println("thisInt="+thisInt);

					toBeDeletedInts.add(thisInt);
					System.out.println("Adding to be deleted="+thisInt);

					mergedIntX.put(thisInt, 1);
					for (int otherInt : mergedInt.keySet()) {
						System.out.println("OtherInt="+otherInt);
						if(thisInt == otherInt) {
							continue;
						}
						if(Math.abs(thisInt-otherInt)<=3) {
							mergedIntX.put(thisInt, mergedIntX.get(thisInt)+1);
							toBeDeletedInts.add(otherInt);
							System.out.println("Adding to be deleted="+otherInt);
						}				
					}
					for (int l : toBeDeletedInts) {
						mergedInt.remove(l);
					}
				}while(mergedInt.size()>0);
				
				
				for (int m : mergedIntX.keySet()) {
					System.out.println("Key "+m+" has score "+mergedIntX.get(m));
				}*/
		
	// %%%%%%%%%%%%%%%%%%%% REMOVE from List - RETURN TYPE %%%%%%%%%%%%%%%%%%%%
		
		/*List<String> strings = new ArrayList<String>();
		strings.add("hello1");
		System.out.println(strings.remove(0));*/
		
	// %%%%%%%%%%%%%%%%%%%% NULL Testing %%%%%%%%%%%%%%%%%%%%
		
		/*MNetwork mn1 = new MNetwork("MN1");
		MNetwork mn2 = new MNetwork("MN2");
		MNetworkPop mnPop1 = new MNetworkPop("MNPOP1");
		mnPop1.networkMap = new HashMap<String, MNetwork>();
		mnPop1.addNetwork(mn1);
		mnPop1.addNetwork(mn2);
		MNetworkPop mnPop2 = new MNetworkPop("MNPOP2");
		// mnPop2.addNetwork(mnPop1.networkMap.get("MN1"));
		mnPop2.networkMap = mnPop1.networkMap;
		// mnPop1.networkMap.remove("MN1");
		mnPop1.networkMap = null;
		System.out.println("Name OUT: "+mnPop2.networkMap.get("MN1").networkID.toString());*/
		
		
		
	// %%%%%%%%%%%%%%%%%%%% Save & Load MNetwork(Pop) %%%%%%%%%%%%%%%%%%%%
		
		/*Scenario sc1 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network1 = sc1.getNetwork();
		Node toNode1 = network1.getFactory().createNode(Id.createNodeId("toNode1"), new Coord(0.0,0.0));
		Node fromNode1 = network1.getFactory().createNode(Id.createNodeId("fromNode1"), new Coord(1.0,1.0));
		network1.addNode(fromNode1);
		network1.addNode(toNode1);
		Link link1 = network1.getFactory().createLink(Id.createLinkId("Link2"), fromNode1, toNode1);
		network1.addLink(link1);
		
		Scenario sc2 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network2 = sc2.getNetwork();
		Node toNode2 = network2.getFactory().createNode(Id.createNodeId("toNode2"), new Coord(2.0,2.0));
		Node fromNode2 = network2.getFactory().createNode(Id.createNodeId("fromNode2"), new Coord(3.0,3.0));
		network2.addNode(fromNode2);
		network2.addNode(toNode2);
		Link link2 = network2.getFactory().createLink(Id.createLinkId("Link2"), fromNode2, toNode2);
		network2.addLink(link2);
		
		MNetwork mnetwork1 = new MNetwork("mnetwork1");
		MNetwork mnetwork2 = new MNetwork("mnetwork2");	
		mnetwork1.network = network1;
		mnetwork2.network = network2;
		
		MNetworkPop mnetworkPop = new MNetworkPop("mnetworkPopulation");
		mnetworkPop.networkMap.put(mnetwork1.networkID, mnetwork1);
		mnetworkPop.networkMap.put(mnetwork2.networkID, mnetwork2);
		
		XMLOps.writeToFileMNetworkPop(mnetworkPop, "zurich_1pm/Evolution/Population/PopTest.xml");
		
		MNetworkPop mnp = XMLOps.readFromFileMNetworkPop("zurich_1pm/Evolution/Population/PopTest.xml");
		for (String mns : mnp.networkMap.keySet()) {
			System.out.println("MNetwork "+mns+" is named "+mnp.networkMap.get(mns).networkID+" and its network has the following nodes: "+mnp.networkMap.get(mns).network.getNodes().toString());
		}*/
		
			
			
	// %%%%%%%%%%%%%%%%%%%% PeoplePlansProcessing %%%%%%%%%%%%%%%%%%%%
		
		//NetworkEvolutionRunSim.peoplePlansProcessingStandard("zurich_1pm/Zurich_1pm_SimulationOutput/output_plans.xml.gz", 240);
		//NetworkEvolutionRunSim.peoplePlansProcessingStandard("zurich_1pm/Zurich_1pm_SimulationOutput_BACKUP__100/output_plans.xml.gz", 240);
		//NetworkEvolutionRunSim.peoplePlansProcessingStandard("zurich_1pm/Evolution/Population/Network1/Simulation_Output/output_plans.xml.gz", 240);

	// %%%%%%%%%%%%%%%%%%%% Plotting network evolution performance %%%%%%%%%%%%%%%%%%%%

		/*int generationsToPlot = 50;
		//NetworkEvolutionImpl.writeChartAverageGenerationNetworkAverageTravelTimes(generationsToPlot, "zurich_1pm/Evolution/Population/scoreEvolutionAverageOfGeneration.png");
		//NetworkEvolutionImpl.writeChartBestGenerationNetworkAverageTravelTimes(generationsToPlot, "zurich_1pm/Evolution/Population/scoreEvolutionBestScoreOfGeneration.png");
		NetworkEvolutionImpl.writeChartAverageTravelTimes(generationsToPlot, "zurich_1pm/Evolution/Population/scoreEvolution.png");*/
		
	// %%%%%%%%%%%%%%%%%%%% Displaying and Operating on an ArrayList %%%%%%%%%%%%%%%%%%%%
		/*List<String> stringList = new ArrayList<String>();
		stringList.add("String1");
		stringList.add("String2");
		stringList.add("String3");
		stringList.add("String4");
		stringList.add("String5");
		System.out.println(stringList.subList(2, 4));*/

		/*System.out.println(stringList.toString());
		List<String> stringListCut = new ArrayList<String>();
		stringListCut = stringList;
		int index3 = stringList.indexOf("String3");
		//System.out.println("Index of String3 = "+index3);
		//int index3cut = stringListCut.indexOf("String3");
		//System.out.println("Index of String3cut = "+index3cut);	
		System.out.println("Size = "+stringList.size());
		System.out.println("The test: "+stringListCut.subList(0, index3));
		System.out.println(stringListCut.subList(index3, stringListCut.size()));
		System.out.println(stringListCut.toString());
		stringListCut.removeAll(stringListCut.subList(index3, stringListCut.size()));
		System.out.println(stringListCut.toString());	
		System.out.println(stringListCut.get(stringListCut.size()-1).toString());		
		System.out.println(OppositeOf(stringListCut).toString());

		String metroTestString = "MetroNodeLinkRef_840312_MetroNodeLinkRef_776361";
		String[] splits1 = metroTestString.split("_");
		//String[] splits2 = metroTestString.split("MetroNodeLinkRef_");
		System.out.println(splits1[0].toString());
		System.out.println(splits1[1].toString());
		System.out.println(splits1[2].toString());
		System.out.println(splits1[3].toString());
		//System.out.println(splits2.toString());
		
		List<Id<Link>> linkIds = new ArrayList<Id<Link>>();
		linkIds.add(Id.createLinkId("MetroNodeLinkRef_1_MetroNodeLinkRef_2"));
		linkIds.add(Id.createLinkId("MetroNodeLinkRef_2_MetroNodeLinkRef_3"));
		linkIds.add(Id.createLinkId("MetroNodeLinkRef_3_MetroNodeLinkRef_4"));
		linkIds.add(Id.createLinkId("MetroNodeLinkRef_4_MetroNodeLinkRef_5"));
		System.out.println(linkIds.toString());
		System.out.println(OD_ProcessorImpl.OppositeLinkListOf(linkIds).toString());*/


	// %%%%%%%%%%%%%%%%%%%% ... %%%%%%%%%%%%%%%%%%%%
		
	// %%%%%%%%%%%%%%%%%%%% Plotter %%%%%%%%%%%%%%%%%%%%

		/*int generationsToPlot = 3;
		NetworkEvolutionImpl.writeChartAverageGenerationNetworkAverageTravelTimes(generationsToPlot, "zurich_1pm/Evolution/Population/scoreEvolutionAverageOfGeneration.png");
		NetworkEvolutionImpl.writeChartBestGenerationNetworkAverageTravelTimes(generationsToPlot, "zurich_1pm/Evolution/Population/scoreEvolutionBestScoreOfGeneration.png");
		NetworkEvolutionImpl.writeChartAverageTravelTimes(generationsToPlot, "zurich_1pm/Evolution/Population/scoreEvolution.png");*/
		
		/*Double personTravelTime = 1.0;
		String travTime = "00:10:30";
		String[] HourMinSec = travTime.split(":");
		System.out.println("Person Travel Time of this leg in [s] = "+travTime);
		System.out.println("Hours = "+HourMinSec[0]);
		System.out.println("Mins = "+HourMinSec[1]);
		System.out.println("Mins = "+Double.parseDouble(HourMinSec[1]));
		System.out.println("Hours = "+Double.parseDouble(HourMinSec[0]));
		//personTravelTime += (1)*Double.parseDouble(HourMinSec[1])/60;
		personTravelTime = personTravelTime + (Double.parseDouble(HourMinSec[0])*3600+Double.parseDouble(HourMinSec[1])*60+Double.parseDouble(HourMinSec[2]))/60;
		System.out.println("Total Person Travel Time of this leg in [s] = "+personTravelTime);*/
		
	// %%%%%%%%%%%%%%%%%%%% XMLWriter %%%%%%%%%%%%%%%%%%%%
		
		/*XStream xstream = new XStream(new StaxDriver());
		xstream.alias("mnetwork", MNetwork.class);	
		MNetwork mnetwork1 = new MNetwork("mnetwork1");
		MNetwork mnetwork2 = new MNetwork("mnetwork2");
		Network network = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getNetwork();
		network.setName("It worked!!!");
		mnetwork1.network = network;
		mnetwork2.network = network;
		String xml1 = xstream.toXML(mnetwork1);
		String xml2 = xstream.toXML(mnetwork2);
		String fileName1 = "zurich_1pm/Evolution/Population/Network1/Objects/"+mnetwork1.networkID+"new.xml";
		String fileName2 = "zurich_1pm/Evolution/Population/Network2/Objects/"+mnetwork2.networkID+"new.xml";
		XMLOps.writeToFile(mnetwork1, fileName1);
		XMLOps.writeToFile(mnetwork2, fileName2);
		//FileOutputStream fos = new FileOutputStream(fileName2);
		//xstream.toXML(mnetwork2, fos);
		System.out.println(xml1);
		System.out.println(xml2);
		MNetwork mnetwork3 = XMLOps.readFromFile(mnetwork1.getClass(), fileName1);
		MNetwork mnetwork4 = XMLOps.readFromFile(mnetwork2.getClass(), fileName2);
		System.out.println("MNetwork1's network name is: "+mnetwork3.network.getName());
		System.out.println("MNetwork2's name is: "+mnetwork4.networkID);*/
		
		
	// %%%%%%%%%%%%%%%%%%%% FAILED: Serialization of Objects without Serializable %%%%%%%%%%%%%%%%%%%%
		
		//MObjectWriter.serializeToXML2(mnetwork1, "zurich_1pm/Evolution/Population/Network1/Objects/"+mnetwork1.networkID+".ser");
		//MObjectWriter.serializeToXML2(mnetwork2, "zurich_1pm/Evolution/Population/Network2/Objects/"+mnetwork2.networkID+".ser");
		
		/*MRoute mRoute1 = new MRoute("testRoute1");
		MRoute mRoute2 = new MRoute("testRoute2");
		
		Network network1 = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getNetwork();
		Network network2 = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getNetwork();
		MNetworkSerializable mnetwork1 = new MNetworkSerializable("mnetwork1");
		MNetworkSerializable mnetwork2 = new MNetworkSerializable("mnetwork2");
		Network network = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getNetwork();
		network.setName("It worked!!!");
		mnetwork1.network = network;
		//MObjectWriter.writeObject(mRoute1, "zurich_1pm/Evolution/Population/Network1/Objects/"+mRoute1.routeID+".ser");
		//MObjectWriter.writeObject(mRoute2, "zurich_1pm/Evolution/Population/Network2/Objects/"+mRoute2.routeID+".ser");
		MObjectWriter.writeObject(mnetwork1, "zurich_1pm/Evolution/Population/Network1/Objects/mnetwork1.ser");
		MObjectWriter.writeObject(mnetwork2, "zurich_1pm/Evolution/Population/Network2/Objects/mnetwork2.ser");
		MNetwork mnetwork3 = (MNetwork) MObjectReader.readObject(new MNetwork("foo").getClass(), "zurich_1pm/Evolution/Population/Network1/Objects/mnetwork1.ser");
		MNetwork mnetwork4 = (MNetwork) MObjectReader.readObject(new MNetwork("foo").getClass(), "zurich_1pm/Evolution/Population/Network2/Objects/mnetwork2.ser");
		System.out.println("MRoute 3 = "+mnetwork3.networkID);
		System.out.println("MNetwork 3 - actual networkName = "+mnetwork3.network.getName());
		System.out.println("MRoute 4 = "+mnetwork4.networkID);*/
		
	// %%%%%%%%%%%%%%%%%%%%%%%%% Make Directory %%%%%%%%%%%%%%%%%%%%%%%%
		// how to make a new directory (it will not overwrite directory/folder if it already exists :))
		/*String mNetworkPath = "zurich_1pm/Evolution/Population/"+"Network2";
		new File(mNetworkPath).mkdirs();*/
		
	// %%%%%%%%%%%%%%%%%%%%%%%%% MNetwor & MRoute Tests %%%%%%%%%%%%%%%%%%%%%%%%%
		
		// TEST: create a POPULATION as a map of networks
		/*MNetworkPop population = new MNetworkPop(); // create a network
		int nNetworks = 10;
		int nRoutesPerNetwork = 5;
		for (int n = 1; n <= nNetworks; n++) {
			MNetwork newNetwork = new MNetwork("Network" + Integer.toString(n));
			for (int r = 1; r <= nRoutesPerNetwork; r++) {
				MRoute newRoute = new MRoute("Route" + Integer.toString(r));
				newNetwork.addNetworkRoute(newRoute);
			}
			population.addNetwork(newNetwork);
		}

		// TEST: Test if initialized correctly
		for (MNetwork m : population.getNetworks().values()) {
			System.out.println(m.networkID + " contains routes:");
			for (MRoute r : m.getNetworkRoutes().values()) {
				System.out.println(r.routeID);
			}
		}*/
		
	// %%%%%%%%%%%%%%%%%%%%%%%%% Random %%%%%%%%%%%%%%%%%%%%%%%%%
		
		/*Set<String> sett = Sets.newHashSet("a", "b"," c");
		System.out.println(sett.toString());*/
			
	// %%%%% Network Converter Tester %%%%%	
		
		/*Id<Link> originalLinkRefId = Id.createLinkId("668_701");
		Id<Node> metroNodeId = Id.createNodeId("MetroNodeLinkRef_" + originalLinkRefId.toString());
		
		Id<Link> originalLinkRefIdOut = NetworkCreatorImpl.orginalLinkFromMetroNode(metroNodeId);
		System.out.println("Original Link Ref Id is "+originalLinkRefIdOut.toString());
		Id<Node> metroNodeIdOut = NetworkCreatorImpl.metroNodeFromOriginalLink(originalLinkRefId);
		System.out.println("Metro Node Id is "+metroNodeIdOut.toString());*/
		
		
	// %%%%%%%%%%%%%%%%%%%%%%%%% Network Route Creator Tester %%%%%%%%%%%%%%%%%%%%%%%%%	
		
		/*Network routesNetwork = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getNetwork();
		NetworkFactory nf = routesNetwork.getFactory();
		Node n1 = nf.createNode(Id.createNodeId("node1"), new Coord(0.0, 0.0));
		Node n2 = nf.createNode(Id.createNodeId("node2"), new Coord(1.0, 0.0));
		Node n3 = nf.createNode(Id.createNodeId("node3"), new Coord(2.0, 0.0));
		Node n4 = nf.createNode(Id.createNodeId("node4"), new Coord(3.0, 0.0));
		routesNetwork.addNode(n1);
		routesNetwork.addNode(n2);
		routesNetwork.addNode(n3);
		routesNetwork.addNode(n4);
		Link l1 = nf.createLink(Id.createLinkId("link1"), n1, n2);
		Link l2 = nf.createLink(Id.createLinkId("link2"), n2, n3);
		Link l3 = nf.createLink(Id.createLinkId("link3"), n1, n4);
		Link l4 = nf.createLink(Id.createLinkId("link4"), n4, n3);
		routesNetwork.addLink(l1);
		routesNetwork.addLink(l2);
		routesNetwork.addLink(l3);
		routesNetwork.addLink(l4);
		ArrayList<Id<Link>> linkList = new ArrayList<Id<Link>>();
		for (int i =1; i<1+4; i++) {
			linkList.add(Id.createLinkId("link"+i));
		}
		ArrayList<Id<Link>> linksBetween = new ArrayList<Id<Link>>(linkList.size()-2);
		for (int i = 0; i<linksBetween.size(); i++) {
			linksBetween.add(linkList.get(i+1));
		}		
		NetworkRoute nr = RouteUtils.createNetworkRoute(linkList, routesNetwork);
		System.out.println(nr.getLinkIds().toString());
		System.out.println(nr.toString());
		*/
		
		
		
		
	// %%%%%%%%%%%%%%%%%%%%%%%%% Config Tester %%%%%%%%%%%%%%%%%%%%%%%%%
		
		/* %%% Config Module Scanner %%%
		 * Takes config file and scans through its modules and parameters
		 * > Config
		 * 	>> ConfigGroup(come as a set of configGroups=Modules)
		 * 	 >>> Parameter(come as a set of parameterSet)
		 * 	  >>>> Values(one for each parameter in the set)
		 */
		/*Config config = ConfigUtils.createConfig();
		ConfigTester.scanConfigModules(config);*/
		
		/* %%% Config Modifier %%%
		 * Add ...
		 * Change ...
		 */
		//ConfigTester.configModifier(config);
		
		
		//static Config		loadConfig(String filename, ConfigGroup... customModules) 
		
		/* %%% Config Writer %%%
		 */
		/*ConfigWriter configWriter = new ConfigWriter(config);
		configWriter.write("myOutput/ConfigScannerTestFile.xml");*/
		
	// %%%%%%%%%%%%%%%%%%%%%%%%% Config Tester2 %%%%%%%%%%%%%%%%%%%%%%%%%
		
		/*// Config > ConfigGroup(come as a set of configGroups=Modules) > Parameter(come as a set of parameterSet) > Values(one for each parameter in the set)
		public static void scanConfigModules(Config config) {
			Iterator<Entry<String, ConfigGroup>> it = config.getModules().entrySet().iterator();
			while(it.hasNext()) {
				try {System.out.println(it.next().toString());}
				catch(RuntimeException RE) {
					System.out.println("had a runtime exception");
					continue;
					}
			}
		}
		
		
		// Create and add new modules
			public static void configModifier(Config config) {
				
				System.out.println("Creating a new configModule ... ");
				ConfigGroup myConfigModule1 = new ConfigGroup("myConfigModule1");
				myConfigModule1.addParam("SpeedFactor", "Highspeed_100");
				myConfigModule1.addParam("Strategy", "Drive_Fast");
				
				ConfigGroup myConfigModule2 = new ConfigGroup("myConfigModule2");
				myConfigModule2.addParam("SpeedFactor2", "Lowspeed_50");
				myConfigModule2.addParam("Strategy2", "Drive_Slow");
				
				System.out.println("Name: "+myConfigModule1.getName().toString());
				System.out.println("Parameters: "+myConfigModule1.getParams().entrySet().toString());
				System.out.println("ParameterSets: "+myConfigModule1.getParameterSets().entrySet().toString());

				myConfigModule1.addParameterSet(myConfigModule2);
				System.out.println("Added new module: "+myConfigModule2.getName().toString());
				
				System.out.println("Name: "+myConfigModule1.getName().toString());
				System.out.println("Parameters: "+myConfigModule1.getParams().entrySet().toString());
				System.out.println("ParameterSets: "+myConfigModule1.getParameterSets().entrySet().toString());
				
				config.addModule(myConfigModule1);
				if(config.getModules().containsKey(myConfigModule1.getName().toString())) {
					config.getModules().remove(myConfigModule1.getName().toString());
					System.out.println("Had to remove "+myConfigModule1.getName().toString());
					config.addModule(myConfigModule1);
				}
				config.addModule(myConfigModule2);

			}*/
		

		
		

	} // end of main method
	
	public static List<String> OppositeOf(List<String> listIn){
		List<String> oppositeList = new ArrayList<String>(listIn.size());
		for (int i=0; i<listIn.size(); i++) {
			oppositeList.add(listIn.get(listIn.size()-1-i));
		}
		return oppositeList;
	}
	
} // end of Demo class








//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%  Config Scanner %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% Successful
/*		Config config = ConfigUtils.createConfig();								// in this case it is empty files and structures
		Scenario scenario = ScenarioUtils.createScenario(config);
		scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DefaultEnrichedTransitRoute.class,
				new DefaultEnrichedTransitRouteFactory());						// why do we need this again?
		//Network network = scenario.getNetwork();								// NetworkFactory netFac = network.getFactory();
		Iterator<Entry<String, ConfigGroup>> it = config.getModules().entrySet().iterator();
		while(it.hasNext()) {
			try {System.out.println(it.next().toString());}
			catch(RuntimeException RE) {
				System.out.println("had a runtime exception");
				continue;
				}
		}*/


//%%%%%%%%%%%%%%%%%%%%%  Event Handler Example %%%%%%%%%%%%%%%%%%%%%%%%

/*	
import org.matsim.api.core.v01.events.handler.GenericEventHandler;
import ch.ethz.matsim.baseline_scenario.transit.events.PublicTransitEvent;

	public class ExampleHandler implements GenericEventHandler {
		public void handleEvent(GenericEvent event) {
			if (event instanceof PublicTransitEvent) {
				PublicTransitEvent ptEvent = (PublicTransitEvent) event;
	
				ptEvent.getTransitLineId();
			}
		}
	}*/

	/**
	 * Zum Auslesen aus Events XML:
	 * 
	 * EventsManager eventsManager = EventsUtils.createEventsManager();
	 * eventsManager.addHandler(tripListener);
	 * 
	 * EventsReaderXMLv1 reader = new EventsReaderXMLv1(eventsManager);
	 * reader.addCustomEventMapper(PublicTransitEvent.TYPE, new
	 * PublicTransitEventMapper()); reader.readFile(eventsPath);
	 */

		
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%  Generic Map Iterator %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% Failed
/*		Map map = new HashMap<String, Long>();
     map.put("1$", new Long(10));
     map.put("2$", new Long(20));
     public static Link randomMapElementKey(Map map) {
 		int nElements = map.size();
 		Random r = new Random();
 		int randomElementNr = r.nextInt(nElements);
 		int counter = 0;
 		Set<?> set = map.entrySet();
         Iterator<?> iterator = set.iterator();
         iterator.next();
         if(iterator.hasNext()) {;
         	Map.Entry entry = (Entry) iterator.next();
             String valueClassType = entry.getValue().getClass().getSimpleName();
             String keyClassType = entry.getKey().getClass().getSimpleName();
             Class valueClass = entry.getValue().getClass();
             Class keyClass = entry.getKey().getClass();
             System.out.println("key type : "+keyClassType);
             System.out.println("value type : "+valueClassType);
     		for (keyClassType. elementKey : map.keySet()) {
     			if(counter == randomElementNr) {
     				 randomElement = elementKey;
     				return randomLink;
     			}
     			counter++;
     		}
     		System.out.println("Error: No random link has been selected.");
     		return null;
         }
         /* Field testMap = Test.class.getDeclaredField("map");
 	     testMap.setAccessible(true);
 	     ParameterizedType type = (ParameterizedType) testMap.getGenericType();
 	     Type key = type.getActualTypeArguments()[0];
 	     System.out.println("Key: " + key);
 	     Type value = type.getActualTypeArguments()[1];
 	     System.out.println("Value: " + value);
 	  	*/




/*
 * 
 */