package ch.ethz.matsim.students.samark;

import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.events.handler.GenericEventHandler;

import ch.ethz.matsim.baseline_scenario.transit.events.PublicTransitEvent;

public class Metro_HandlerGeneric implements GenericEventHandler {
	
	String ptLine;
	int counter;
	
	Metro_HandlerGeneric(String ptLine){
		this.ptLine = ptLine;
		this.counter = 0;
	}


	@Override
	public void handleEvent(GenericEvent event) {
		if (event instanceof PublicTransitEvent) {
			System.out.println("This pt line is "+((PublicTransitEvent) event).getTransitLineId().toString());
		}
		if (event.getEventType().contains("pt_transit")) {
			if(event.getAttributes().get("line").contains(ptLine)) {
				counter++;
				// System.out.println(event.getAttributes().get("line"));
			}
			// System.out.println(event.getAttributes().get("line"));
		}
	}

	
	
	
}
