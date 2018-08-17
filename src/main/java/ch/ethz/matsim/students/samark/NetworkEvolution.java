package ch.ethz.matsim.students.samark;

public class NetworkEvolution {

/* DATA STRUCTURE
 * Multiple networks = Population 	= Map<MNetwork.Id, MNetwork> = networkMap
 * Single Network = Chromosome 		= Map<MRoute.Id, MRoute> = routesMap
 * Single Route = Gene				= MRoute
 */
	
	public static void main(String[] args) {
	
		// create a POPULATION as a map of networks
		MNetworkPop population = new MNetworkPop();				// create a network
		int nNetworks = 10;
		int nRoutesPerNetwork = 5;
		for (int n=1; n<=nNetworks; n++) {
			MNetwork newNetwork = new MNetwork("Network"+Integer.toString(n));
			for (int r=1; r<=nRoutesPerNetwork; r++) {
				MRoute newRoute = new MRoute("Route"+Integer.toString(r));
				newNetwork.addNetworkRoute(newRoute);
			}
			population.addNetwork(newNetwork);
		}
		
		// TEST if initialized correctly
		for (MNetwork m : population.getNetworks().values()) {
			System.out.println(m.networkID+" contains routes:");
			for (MRoute r : m.getNetworkRoutes().values()) {
				System.out.println(r.routeID);
			}
		}
		
		
		//TODO make a custom network class with attributes as in MRoute
		
	
	
	}
}
