package ch.ethz.matsim.students.samark;

import java.util.LinkedList;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;

import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;

public class VC_ScenarioImpl {
	
	Link kindergartenLink;
	Coord kindergartenCoord;
	Link homeLink;
	Coord homeCoord;

	public VC_ScenarioImpl () {}			// Constructor
	
	public Population createNewDemand(Scenario scenario, Network network, double networkSize, int nNewPeople, String populationPrefix) {

		Population population = scenario.getPopulation();
		PopulationFactory populationFactory = population.getFactory();
		
	// for nNewPeople people	
		for (int p=1; p<=nNewPeople; p++) {																
		System.out.println("Making plan for person: "+p);
			Id<Person> newPersonID = Id.createPersonId(populationPrefix+"Person_"+p);
			Person newPerson = populationFactory.createPerson(newPersonID);
			if (population.getPersons().containsKey(newPersonID)) {
				population.removePerson(newPersonID);
			}
			population.addPerson(newPerson);
			Plan newPlan = populationFactory.createPlan();
			newPerson.addPlan(newPlan);
			Link lastLink;
			
		// make home activity for current person // TODO merge both of these
			this.homeLink = randomLinkGenerator(network);
			this.homeCoord = linkToRandomCoord(homeLink);
			double minHomeStay = 6.0*60*60;
			double leaveTimeFrame = 2.0*60*60;
			createAndAddHomeActivityToPlan(population, newPlan, "home", homeLink, homeCoord, minHomeStay, leaveTimeFrame);
			lastLink = homeLink;

			
		// make kindergarten activity for person
			double maxDistanceToKindergarten = networkSize/3;
			double minDistanceToKindergarten = 0.0;
			boolean kindergartenParent = false;
			if(new Random().nextDouble() < 0.2) {
				// System.out.println("in kindergarten loop");
				kindergartenParent = true;
				this.kindergartenLink = DijkstraOwn_I.makeSureExists(confinedLinkGenerator(newPerson, network, networkSize, homeCoord, minDistanceToKindergarten, maxDistanceToKindergarten), lastLink, network);
				this.kindergartenCoord = linkToRandomCoord(this.kindergartenLink);
				String legName = "toKindergarten";
				String legMode = "walk";
				createLeg(network, population, newPlan, legName, legMode, lastLink, this.kindergartenLink);
				double dropOffDuration = 15*60;
				createAndAddActivityToPlan(population, newPlan, "kindergarten", kindergartenLink, kindergartenCoord, dropOffDuration);
				lastLink = kindergartenLink;
			}
			
		// make work activity for current person
			double workRandom = new Random().nextDouble();
			if(workRandom < 0.9) {
				// System.out.println("in work loop");
				double minDistanceToWork = networkSize/4;
				double maxDistanceToWork = networkSize;
				Link workLink = DijkstraOwn_I.makeSureExists(confinedLinkGenerator(newPerson, network, networkSize, homeCoord, minDistanceToWork, maxDistanceToWork), lastLink, network);	
				Coord workCoord = linkToRandomCoord(workLink);
				String legName = "toWork";
				String legMode = "walk";
				createLeg(network, population, newPlan, legName, legMode, lastLink, workLink);
				double workDuration = 8.0*60*60 + (new Random().nextDouble())*2*60*60 - 1.0*60*60;
				createAndAddActivityToPlan(population, newPlan, "work", workLink, workCoord, workDuration);
				lastLink = workLink;
			}

			
		// make shopping activity for current person
			if(new Random().nextDouble() < 0.3) {
				double minDistanceToShop = 0;
				double maxDistanceToShop = networkSize/5;
				Link shopLink = DijkstraOwn_I.makeSureExists(confinedLinkGenerator(newPerson, network, networkSize, homeCoord, minDistanceToShop, maxDistanceToShop), lastLink, network);
				Coord shopCoord = linkToRandomCoord(shopLink);
				String legName = "toShop";
				String legMode = "walk";
				createLeg(network, population, newPlan, legName, legMode, lastLink, shopLink);
				double shopDuration = 45*60;
				createAndAddActivityToPlan(population, newPlan, "shop", shopLink, shopCoord, shopDuration);			
				lastLink = shopLink;
			}
			
		// fetch kids from kindergarten
			// TODO merge both of these
			if(kindergartenParent) {
				double pickUpDuration = 15*60;
				String legName = "toKindergartenFetch";
				String legMode = "walk";
				createLeg(network, population, newPlan, legName, legMode, lastLink, this.kindergartenLink);
				createAndAddActivityToPlan(population, newPlan, "kindergartenFetch", this.kindergartenLink , this.kindergartenCoord, pickUpDuration);
				lastLink = this.kindergartenLink;
				this.kindergartenCoord = null;
				this.kindergartenLink = null;
			}
			
		// go back home
			// TODO create leg
			createAndAddHomeActivityToPlan(population, newPlan, "homeFinal", this.homeLink, this.homeCoord, 0.0, 0.0);
			lastLink = this.homeLink;	
			
		}

		this.homeLink = null;
		this.homeCoord = null;
		// System.out.println("home again");
		
		return scenario.getPopulation();
	}
	
	public static void createLeg(Network network, Population population, Plan plan, String legName, String legMode, Link fromLink, Link toLink) {
		// make work leg by car and add to plan
			Leg leg = population.getFactory().createLeg(legName);
			leg.setMode(legMode);
			// toWorkLeg.setTravelTime(5*60 + new Random().nextInt(15*60));
			LinkedList<Id<Link>> routeLinkIds = new LinkedList<Id<Link>>();
			routeLinkIds.add(fromLink.getId());
			routeLinkIds.add(toLink.getId());			
			NetworkRoute networkRoute = RouteUtils.createNetworkRoute(routeLinkIds, network);
			leg.setRoute(networkRoute);
			leg.setTravelTime(25*60);
			plan.addLeg(leg);
	}
	
	public static void createAndAddActivityToPlan(Population population, Plan plan, String actName, Link actLink, Coord actCoord, double maxDuration) {
		Activity activity = population.getFactory().createActivityFromCoord(actName, actCoord);
		activity.setLinkId(actLink.getId());
		//Random r = new Random();
		// activity.setEndTime(maxDuration);
		activity.setMaximumDuration(maxDuration);
		plan.addActivity(activity);
	}
	
	public static Link randomLinkGenerator(Network network) {
		int nLinks = network.getLinks().size();
		Random r = new Random();
		int randomLinkNr = r.nextInt(nLinks);
		int counter = 0;
		for (Link link : network.getLinks().values()) {
			if(counter == randomLinkNr) {
				Link randomLink = link;
				return randomLink;
			}
			counter++;
		}
		System.out.println("Error: No random link has been selected.");
		return null;
	}
	
	public static Link confinedLinkGenerator(Person person, Network network, double networkSize, Coord homeCoord, double minDistanceToNextActivity, double maxDistanceToNextActivity) {
		Link nextActivityLink;
		int iter = 0;
		int tries = 1000;
		do {
			nextActivityLink = randomLinkGenerator(network);
			iter++;
			/* System.out.println("NextActivityLink is: "+nextActivityLink.getId().toString());
			System.out.println("Iteration: "+iter);
			System.out.println("minDistanceToNextActivity: "+minDistanceToNextActivity);
			System.out.println("maxDistanceToNextActivity: "+maxDistanceToNextActivity);
			System.out.println("nextActivityLink_X_coord: "+nextActivityLink.getCoord().getX());
			System.out.println("nextActivityLink_Y_coord: "+nextActivityLink.getCoord().getY());
			System.out.println("linkToRandomCoord(nextActivityLink): "+linkToRandomCoord(nextActivityLink).toString());
			System.out.println("homeCoord_X_coord: "+homeCoord.getX());
			System.out.println("homeCoord_Y_coord: "+homeCoord.getY());
			System.out.println("distanceBetweenCoords(homeCoord, linkToRandomCoord(nextActivityLink)): "+distanceBetweenCoords(homeCoord, linkToRandomCoord(nextActivityLink)));
			*/
			if(iter==tries-1) {
				System.out.println("No feasible link found. Sorry! Aborting search for this link.");
			}
		} while(iter<tries && (distanceBetweenCoords(homeCoord, linkToRandomCoord(nextActivityLink)) < minDistanceToNextActivity || maxDistanceToNextActivity < distanceBetweenCoords(homeCoord, linkToRandomCoord(nextActivityLink))));  
		return nextActivityLink;
	}

	public static Coord linkToRandomCoord(Link link) {
		double xStart = link.getFromNode().getCoord().getX();
		double yStart = link.getFromNode().getCoord().getY();
		double xEnd = link.getToNode().getCoord().getX();
		double yEnd = link.getToNode().getCoord().getY();
		Random rand = new Random();
		double xBetween = xStart + rand.nextDouble()*(xEnd-xStart);
		double yBetween = yStart + rand.nextDouble()*(yEnd-yStart);
		return new Coord(xBetween, yBetween);
	}
	
	public static double distanceBetweenCoords(Coord coord1, Coord coord2) {
		return Math.sqrt((coord1.getX()-coord2.getX())*(coord1.getX()-coord2.getX())+(coord1.getY()-coord2.getY())*(coord1.getY()-coord2.getY()));
	}
	
	public static void createAndAddHomeActivityToPlan(Population population, Plan plan, String activityName, Link homeLink, Coord homeCoord, double minHomeStay, double leaveTimeFrame){
		Activity homeActivity = population.getFactory().createActivityFromCoord(activityName, homeCoord);
		homeActivity.setLinkId(homeLink.getId());
		Random r = new Random();
		if(activityName != "homeFinal") {
			homeActivity.setEndTime(minHomeStay+r.nextDouble()*leaveTimeFrame);		// homeActivity.setEndTime(6.0*60*60+r.nextInt(2*60*60));
		}
		plan.addActivity(homeActivity);	
	}

}