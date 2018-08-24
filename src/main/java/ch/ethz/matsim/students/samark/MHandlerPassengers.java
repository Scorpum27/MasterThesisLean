package ch.ethz.matsim.students.samark;

import java.util.HashMap;
import java.util.Map;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.events.handler.GenericEventHandler;

public class MHandlerPassengers implements GenericEventHandler{
	// TODO change TransitLine Numbering and Naming from TransitLine_Nr4 to Network2Route4

	Map<String, Map<String, Double>> travelStats;	// Map< PersonID, Map< RouteName, TravelDistance > >
	double totalBeelineKM;
	//Map<String, Double> transitPersonKM;			// Map<RouteName, TotalPersonKM>
	// Map<String, Double> transitPersonBeelineKM;	// reactivate this section for beeline distances // Map<RouteName, TotalPersonKM>
	Map<String, Integer> routeBoardingCounter;		// Map<RouteName, nBoardingsOnThatRoute>
	
	public MHandlerPassengers() {
		this.travelStats = new HashMap<String, Map<String, Double>>();
		this.totalBeelineKM = 0.0;
		this.routeBoardingCounter = new HashMap<String, Integer>();
	}

	
	@Override
	public void handleEvent(GenericEvent event) {
		if (event.getEventType().contains("pt_transit") && event.getAttributes().get("accessStop").contains("Metro")) {
			String personId = event.getAttributes().get("person");
			String route = event.getAttributes().get("route");
			double distance = Double.parseDouble(event.getAttributes().get("travelDistance"));
			System.out.println("PT_Transit on Route "+ route + " --> "+ event.getAttributes().get("travelDistance") +" [m] travelled");
			Map<String, Double> routeDistances = new HashMap<String, Double>();
			if(this.travelStats.containsKey(personId)) {
				routeDistances = this.travelStats.get(personId);
				if(routeDistances.containsKey(route)) {
					double oldDistance = routeDistances.get(route);
					routeDistances.put(route, oldDistance+distance);
				}
				else {
					routeDistances.put(route, distance);
				}
			}
			else {
				routeDistances.put(route, distance);
			}
			this.travelStats.put(personId, routeDistances);
			//DELETE WHEN YOU SEE THIS: System.out.println("New Total on Route "+ route + " = "+ this.travelStats.get(personId).get(route));
			
			if (this.routeBoardingCounter.containsKey(route)) {
				this.routeBoardingCounter.put(route, this.routeBoardingCounter.get(route)+1);				
			}
			else {this.routeBoardingCounter.put(route, 1);}			
			System.out.println("And added one boarding for "+route+" to "+this.routeBoardingCounter.get(route));
		}
		// test
		/*for (String att : event.getAttributes().keySet()){
			System.out.println("Attributes: "+att+ " - "+event.getAttributes().get(att));
			System.out.println("Event type =" + event.getEventType());
		}
		if (personStats.containsKey(event.getAttributes().get("person"))){
			
			for (Entry<Double, String> entry : personStats.get(event.getAttributes().get("person")).entrySet()) {
				System.out.println("Person scoreStats= "+entry.toString());
			}
			if (event.getAttributes().containsKey("distance")) {
				System.out.println("It contains distance entry with d="+event.getAttributes().get("distance"));
			}
		}*/
		/*if (personStats.containsKey(event.getAttributes().get("person"))){
			System.out.println("Person is= "+event.getAttributes().get("person"));
			for (Entry<Double, String> entry : personStats.get(event.getAttributes().get("person")).entrySet()) {
				System.out.println("Person scoreStats= "+entry.toString());
			}
			if (event.getAttributes().containsKey("distance")) {
				System.out.println("It contains distance entry with d="+event.getAttributes().get("distance"));
			}
		}*/
		
		
		
		
		/*if (event.getEventType().contains("travelled")) {
			double time = event.getTime();
			String person = event.getAttributes().get("person");
			System.out.println("Travelled event found: Time="+Double.toString(time)+" and Person="+person+", while personStatsSize="+personStats.size());
			System.out.println("All person entries of person "+person+" :");
			for (Map<Double, String> personStat : personStats.values()) {
				for (Entry<Double, String> entry : personStat.entrySet())
				System.out.println("Entry: "+entry.toString());
			}
			if(personStats.get(person).keySet().contains(time)) {
				String route = personStats.get(person).get(time);
				double distanceTravelled = 0.00;
				if (this.transitPersonKM.containsKey(route)) {
					distanceTravelled += Integer.parseInt(event.getAttributes().get("distance"));					
				}
				this.transitPersonKM.put(route, distanceTravelled);
				System.out.println("Total Distance Route "+route+" updated to "+distanceTravelled);
				/*double beelineDistanceTravelled = 0.00;		// reactivate this section for beeline distances
				if (this.transitPersonBeelineKM.containsKey(route)) {
					distanceTravelled += Integer.parseInt(event.getAttributes().get("distance"));					
				}
				this.transitPersonBeelineKM.put(route, distanceTravelled);
				System.out.println("Total Beeline Distance Route "+route+" updated to "+beelineDistanceTravelled);
				int nBoardings = 0;
				if (this.routeBoardingCounter.containsKey(route)) {
					nBoardings += this.routeBoardingCounter.get(route);
				}
				this.routeBoardingCounter.put(route, nBoardings);
				System.out.println("And added one boarding for "+route+" to "+nBoardings);
			}*/
		
	}

	
	
}
