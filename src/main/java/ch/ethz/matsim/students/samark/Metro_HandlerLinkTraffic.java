package ch.ethz.matsim.students.samark;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;

public class Metro_HandlerLinkTraffic implements LinkEnterEventHandler {

	public Map<Id<Link>, Double> metroLinkTraffic;
	
	Metro_HandlerLinkTraffic(){
		this.metroLinkTraffic = new HashMap<Id<Link>, Double>();
	}
	
	public Map<Id<Link>, Double> getMetroLinkTraffic(){
		return this.metroLinkTraffic;
	}
	
	
	
	@Override
	public void handleEvent(LinkEnterEvent event) {
		// System.out.println("Vehicle is: "+event.getVehicleId().toString());
		if(event.getVehicleId().toString().contains("metro")) {
			if(this.metroLinkTraffic.containsKey(event.getLinkId())==false) {
				System.out.println("New metro vehicle link entry! Initializing link: "+event.getLinkId().toString()+" ...");
				this.metroLinkTraffic.put(event.getLinkId(), 1.0);
			}
			else {
				double newCount = this.metroLinkTraffic.get(event.getLinkId()) + 1;
				this.metroLinkTraffic.put(event.getLinkId(), newCount);
				System.out.println("New metro link enter count is: "+newCount);
			}
		}
		/*else {
			System.out.println("Failed. Vehicle is: "+event.getVehicleId().toString());
		}*/
	}

	
	
	
}
