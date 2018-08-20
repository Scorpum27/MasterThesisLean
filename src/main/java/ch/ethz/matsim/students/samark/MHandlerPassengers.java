package ch.ethz.matsim.students.samark;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.events.handler.GenericEventHandler;

public class MHandlerPassengers implements GenericEventHandler{
	// TODO change TransitLine Numbering and Naming from TransitLine_Nr4 to Network2Route4

	Map<String, Map<Double, String>> personStats;	// Map< PersonID, Map< BoardingTime, RouteName > >
	Map<String, Double> transitPersonKM;			// Map<RouteName, TotalPersonKM>
	// Map<String, Double> transitPersonBeelineKM;	// reactivate this section for beeline distances // Map<RouteName, TotalPersonKM>
	Map<String, Integer> routeBoardingCounter;		// Map<RouteName, nBoardingsOnThatRoute>
	
	public MHandlerPassengers() {
		this.personStats = new HashMap<String, Map<Double, String>>();
		this.transitPersonKM  = new HashMap<String, Double>();
		this.routeBoardingCounter = new HashMap<String, Integer>();
	}
	
	@Override
	public void handleEvent(GenericEvent event) {
		if (event.getEventType().contains("pt_transit") && event.getAttributes().get("accessStop").contains("Metro")) {
			String personId = event.getAttributes().get("person");
			String route = event.getAttributes().get("route");
			double time = event.getTime();
			Map<Double, String> boardingTimes = new HashMap<Double, String>();
			if(this.personStats.containsKey(personId)) {
				boardingTimes = this.personStats.get(personId);
			}
			boardingTimes.put(time, route);
			System.out.println("Something on Route"+boardingTimes.get(time));
			this.personStats.put(personId, boardingTimes);
			System.out.println("New boarding on Route "+route+" at "+Double.toString(time)+"s");

		}
		if (event.getEventType().contains("travelled")) {
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
				System.out.println("Total Beeline Distance Route "+route+" updated to "+beelineDistanceTravelled);*/
				int nBoardings = 0;
				if (this.routeBoardingCounter.containsKey(route)) {
					nBoardings += this.routeBoardingCounter.get(route);
				}
				this.routeBoardingCounter.put(route, nBoardings);
				System.out.println("And added one boarding for "+route+" to "+nBoardings);
			}
		}
	}

	
	
}
