package ch.ethz.matsim.students.samark;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

public class Metro_ProcessorImpl {

	public static Map<Id<Link>, Double> handleMetroLinkTraffic(int iterationToRead) {
		Metro_HandlerLinkTraffic metroLinkTrafficHandler = new Metro_HandlerLinkTraffic();

		EventsManager eventsManager = EventsUtils.createEventsManager();		
		eventsManager.addHandler(metroLinkTrafficHandler);
		
		MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager);
		eventsReader.readFile("zurich_1pm/Metro/Simulation_Output/ITERS/it." + iterationToRead + "/" + iterationToRead + ".events.xml.gz");
		
		return metroLinkTrafficHandler.metroLinkTraffic;
	}
	
	public static int handleMetroLineTraffic(int iterationToRead, String ptLine) {
		Metro_HandlerGeneric metroGenericHandler = new Metro_HandlerGeneric(ptLine);

		EventsManager eventsManager = EventsUtils.createEventsManager();		
		eventsManager.addHandler(metroGenericHandler);
		
		MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager);
		eventsReader.readFile("zurich_1pm/Metro/Simulation_Output/ITERS/it." + iterationToRead + "/" + iterationToRead + ".events.xml.gz");
		return metroGenericHandler.counter;
	}

	
	public static Map<String, Double> handleMetroPeopleTraffic(int iterationToRead){
		Metro_HandlerPeopleTraffic metroPeopleTrafficHandler = new Metro_HandlerPeopleTraffic();

		EventsManager eventsManager = EventsUtils.createEventsManager();		
		eventsManager.addHandler(metroPeopleTrafficHandler);
		
		MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager);
		eventsReader.readFile("zurich_1pm/Metro/Simulation_Output/ITERS/it." + iterationToRead + "/" + iterationToRead + ".events.xml.gz");
		
		return metroPeopleTrafficHandler.statsMap;
	}
	
}
