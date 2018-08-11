package ch.ethz.matsim.students.samark;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;

public class linkTrafficCounter implements LinkEnterEventHandler {

	public Map<Id<Link>,Double> trafficMap;
	
	linkTrafficCounter(){
	}
	
	linkTrafficCounter(Map<Id<Link>,Double> emptyTrafficMap){
		this.trafficMap = emptyTrafficMap;
	}

	
	@Override
	public void handleEvent(LinkEnterEvent event) {
		// TODO Auto-generated method stub
		double oldTrafficCount = this.trafficMap.get(event.getLinkId());
		trafficMap.put(event.getLinkId(), oldTrafficCount+1);
	}

}
