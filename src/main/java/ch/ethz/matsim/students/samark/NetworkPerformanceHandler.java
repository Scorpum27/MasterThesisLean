package ch.ethz.matsim.students.samark;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;

public class NetworkPerformanceHandler implements PersonDepartureEventHandler, PersonArrivalEventHandler {

	MNetwork mNetwork;
	
	NetworkPerformanceHandler(MNetwork mNetwork){
		this.mNetwork = mNetwork;
	}
	
// public class Metro_HandlerPeopleTraffic implements PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler{
// public class Metro_HandlerPeopleTraffic implements PersonDepartureEventHandler, PersonArrivalEventHandler {

	
	Map<String, Double> statsMap = new HashMap<String, Double>();
	
	public NetworkPerformanceHandler() {
		statsMap.put("metroBoardingNr", 0.0);
		statsMap.put("metroDisembarkingNr", 0.0);
	}
	
	public Double getMetroBoardingNr(){
		return this.statsMap.get("metroBoardingNr");
	}
	
	public Double getMetroDisembarkingNr(){
		return this.statsMap.get("metroDisembarkingNr");
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if(event.getLinkId().toString().contains("Metro") && event.getLegMode().contains("pt")) {
			System.out.println("Yes, link ID contains /Metro/! Adding one Boarding counter.");
			Double newCount = statsMap.get("metroBoardingNr") + 1;
			statsMap.put("metroBoardingNr", newCount);
			System.out.println("New Boarding count is: "+newCount);
		}
		
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if(event.getLinkId().toString().contains("Metro") && event.getLegMode().contains("pt")) {
			System.out.println("Yes, link ID contains /Metro/! Adding one Disembarking counter.");
			Double newCount = statsMap.get("metroDisembarkingNr") + 1;
			statsMap.put("metroDisembarkingNr", newCount);
			System.out.println("New Disembarking count is: "+newCount);
		}
	}
	
	
	
	/*@Override
	public void handleEvent(PersonLeavesVeEvent event) {
		if(event.getVehicleId().toString().contains("Metro")) {
			System.out.println("Yes, vehicle ID contains /Metro/! Adding one Disembarking counter.");
			Double newCount = statsMap.get("metroDisembarkingNr") + 1;
			statsMap.put("metroDisembarkingNr", newCount);
			System.out.println("New disembark count is: "+newCount);
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if(event.getVehicleId().toString().contains("metro")) {
			event.getEventType()
			System.out.println("Yes, vehicle ID contains /metro/! Adding one Boarding counter.");
			Double newCount = statsMap.get("metroBoardingNr") + 1;
			statsMap.put("metroBoardingNr", newCount);
			System.out.println("New Boarding count is: "+newCount);
		}
	}*/
	
	
	

}
