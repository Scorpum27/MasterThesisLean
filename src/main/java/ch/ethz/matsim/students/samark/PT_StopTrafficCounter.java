package ch.ethz.matsim.students.samark;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;

public class PT_StopTrafficCounter implements PersonArrivalEventHandler, PersonDepartureEventHandler {


	public Map<Id<Link>, CustomLinkAttributes> CustomLinkMap;
	
	PT_StopTrafficCounter(){
	}
	
	PT_StopTrafficCounter(Map<Id<Link>,CustomLinkAttributes> emptyCustomLinkMap){
		this.CustomLinkMap = emptyCustomLinkMap;
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if(event.getLegMode()=="pt") {
			double oldTrafficCount = this.CustomLinkMap.get(event.getLinkId()).getTotalTraffic();
			CustomLinkAttributes newLinkAttributes = this.CustomLinkMap.get(event.getLinkId());
			newLinkAttributes.setTotalTraffic(oldTrafficCount+1);
			this.CustomLinkMap.put(event.getLinkId(), newLinkAttributes);
			// System.out.println("[Departure] -- New traffic count is "+newLinkAttributes.getTotalTraffic());
		}
		
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if(event.getLegMode()=="pt") {
			double oldTrafficCount = this.CustomLinkMap.get(event.getLinkId()).getTotalTraffic();
			CustomLinkAttributes newLinkAttributes = this.CustomLinkMap.get(event.getLinkId());
			newLinkAttributes.setTotalTraffic(oldTrafficCount+1);
			this.CustomLinkMap.put(event.getLinkId(), newLinkAttributes);	
			// System.out.println("[Arrival] -- New traffic count is "+newLinkAttributes.getTotalTraffic());
		}	
	}
	

}