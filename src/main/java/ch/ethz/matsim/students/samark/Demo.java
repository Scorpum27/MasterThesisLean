package ch.ethz.matsim.students.samark;

public class Demo {
	
	public static void main(String[] args) {
	
	// MNetwor & MRoute Tests
		// TEST: create a POPULATION as a map of networks
		MNetworkPop population = new MNetworkPop(); // create a network
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
		}
		
	// %%%%% Random %%%%%
		
		/*Set<String> sett = Sets.newHashSet("a", "b"," c");
		System.out.println(sett.toString());*/
			
	// %%%%% Network Converter Tester %%%%%	
		
		/*Id<Link> originalLinkRefId = Id.createLinkId("668_701");
		Id<Node> metroNodeId = Id.createNodeId("MetroNodeLinkRef_" + originalLinkRefId.toString());
		
		Id<Link> originalLinkRefIdOut = NetworkCreatorImpl.orginalLinkFromMetroNode(metroNodeId);
		System.out.println("Original Link Ref Id is "+originalLinkRefIdOut.toString());
		Id<Node> metroNodeIdOut = NetworkCreatorImpl.metroNodeFromOriginalLink(originalLinkRefId);
		System.out.println("Metro Node Id is "+metroNodeIdOut.toString());*/
		
		
	// %%%%% Network Route Creator Tester %%%%%	
		
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
		
		
		
		
	// %%%%% Config Tester %%%%%
		
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
		
	// %%%%% Config Tester2 %%%%%
		
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
} // end of Demo class








//%%%%%%%%%%%%%%%%%%%%%  Config Scanner %%%%%%%%%%%%%%%%%%%%%%%% Successful
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

		
//%%%%%%%%%%%%%%%%%%%%%  Generic Map Iterator %%%%%%%%%%%%%%%%%%%% Failed
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