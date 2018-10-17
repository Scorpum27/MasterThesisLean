package ch.ethz.matsim.students.samark;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;


// where MRoutes are generated/changed:
// - 

public class MRoute implements Serializable{

	private static final long serialVersionUID = 1L;

	// CAUTION: When adding to MRoute, also add in Clone.mRoute!
	String routeID;
	NetworkRoute networkRoute;
	List<Id<Node>> nodeList;
	List<Id<Link>> linkList;
	TransitLine transitLine;
	double routeLength;
	// from eventsFile
	String eventsFile;
	int nBoardings;
	double personMetroDist;
	// from transitScheduleFile
	double lifeTime;
	int nDepartures;
	double departureSpacing;
	Boolean isInitialDepartureSpacing;
	double firstDeparture;
	double lastDeparture;
	double roundtripTravelTime;
	String transitScheduleFile;
	double totalDrivenDist;		// [m] = old:drivenKM total distance traveled by pt vehicles of this mroute
	double opsCost;
	double constrCost;
	double utilityBalance;
	
	Double lastUtilityBalance;
	Boolean freqModOccured;
	Boolean significantRouteModOccured;
	List<String> attemptedFrequencyModifications;
	Integer blockedFreqModGenerations;
	String lastFreqMod;
	Double probNextFreqModPositive;
	Boolean hasBeenShortened;
	
	double undergroundPercentage;
	double NewUGpercentage;
	double DevelopUGPercentage;
	double NewOGpercentage;
	double EquipOGPercentage;
	double DevelopOGPercentage;
	int vehiclesNr;
	int nStationsNew;
	int nStationsExtend;
	// from evolution
	List<Id<Link>> facilityBlockedLinks;
	
	public MRoute() {
		this.lifeTime = 40.0;
		this.routeLength = Double.MAX_VALUE;
		this.eventsFile = "";
		this.undergroundPercentage = 0.0;
		this.NewUGpercentage = 0.0;
		this.DevelopUGPercentage = 0.0;
		this.NewOGpercentage = 0.0;
		this.EquipOGPercentage = 0.0;
		this.DevelopOGPercentage = 0.0;
		this.personMetroDist = 0.0;
		this.nBoardings = 0;
		this.nDepartures = 0;
		this.departureSpacing = 0.0;
		this.isInitialDepartureSpacing = true;
		this.firstDeparture = 0.0;
		this.lastDeparture = 0.0;
		this.transitScheduleFile = "";
		this.totalDrivenDist = 0.0;
		this.opsCost = Double.MAX_VALUE;
		this.constrCost = Double.MAX_VALUE;
		this.utilityBalance = -Double.MAX_VALUE;
		this.lastUtilityBalance = -Double.MAX_VALUE;
		this.attemptedFrequencyModifications = new ArrayList<String>();
		this.blockedFreqModGenerations = 0;
		this.freqModOccured = false;
		this.significantRouteModOccured = false;
		this.hasBeenShortened = false;
		this.lastFreqMod = "none";
		this.probNextFreqModPositive = -1.0;
		this.vehiclesNr = 0;
		this.nStationsExtend = 0;
		this.nStationsNew = 0;
		this.roundtripTravelTime = Double.MAX_VALUE;
		this.facilityBlockedLinks = new ArrayList<Id<Link>>();
	}
	
	public MRoute(String name) {
		this();
		this.routeID = name;
	}
	
	final double ConstrCostUGnew = 1.5E5; 						// [CHF/m]
	final double ConstrCostUGdevelop = 1.0E5;					// [CHF/m]
	final double ConstrCostOGnew = 4.0E4;	 					// [CHF/m]
	final double ConstrCostOGdevelop = 3.0E4;	 				// [CHF/m]
	final double ConstrCostOGequip = 0.5E4;	 					// [CHF/m]
	final double ConstrCostPerStationNew = 6.0E4;	 			// [CHF]
	final double ConstrCostPerStationExtend = 3.0E4;			// [CHF]
	final double costVehicle = 2*6.0E6;		 					// [CHF] x2 because assumed to be replaced once for 40y total lifetime (=2x20y)
	
	final double OpsCostPerVehDistUG = 17.0/1000;
	final double OpsCostPerVehDistOG = 11.3/1000;
	final double EnergyCost = 0.03; // 0.03.-/kWh = 30.-/MWh
	final double energyPerPtPersDist = 0.157/1000; // kWh/personKM
//	final double PtVehicleDist = totalDrivenDist;
//	final double energyPerPtVehDist = energyPerPtPersDist*newCase.ptPersonDist/PtVehicleDist;
	final double taxPerVehicleDist = 0.06/1000;
	final double occupancyRate = 1.42; // personsPerVehicle
	final double ptPassengerCostPerDist = 0.1407/1000; // average price/km to buy a ticket for a trip with a certain distance
	final double carCostPerVehDist = 0.1403/1000; // CHF/KM (operations such as service, repairs etc.)
	final double externalVehicleCosts = (0.06 + 0.11 + 0.13)/1000;  // noise, pollution, climate, accidents, fuel, write-off
//	final double externalVehicleCosts = (0.0111 + 0.0179 + 0.008 + 0.2862 + 0.11 + 0.13)/1000;  // noise, pollution, climate, accidents, fuel, write-off
	final double VATPercentage = 0.08;
	final double utilityOfTimePT = 14.43/3600; // CHF/s
	final double utilityOfTimeCar = 23.29/3600; // CHF/s
	
	
	public void calculatePercentages(Network globalNetwork, Map<Id<Link>, CustomMetroLinkAttributes> metroLinkAttributes) throws IOException {
		final Coord UGcenterCoord = new Coord(2683466.0, 1249967.0);
		final double UGradius = 5000.0;
		final double OGdevelopRadius = UGradius*1.5;
		double totalLength = 0.0;
		double ugLength = 0.0;
		double ugNewLength = 0.0;
		double ugDevelopLength = 0.0;
		double ogLength = 0.0;
		double ogNewLength = 0.0;
		double ogDevelopLength = 0.0;
		double ogEquipLength = 0.0;
		
		for (Id<Link> linkId : this.linkList.subList(0, (int) this.linkList.size()/2)) {
			Link link = globalNetwork.getLinks().get(linkId);
			totalLength += link.getLength();
//			Log.write("Link Distance from Center = " + GeomDistance.calculate(link.getFromNode().getCoord(), UGcenterCoord));
			if (GeomDistance.calculate(link.getFromNode().getCoord(), UGcenterCoord) < UGradius) {
				ugLength += link.getLength();
//				Log.write("LinkType = "+metroLinkAttributes.get(linkId).type);
				if (metroLinkAttributes.get(linkId).type.equals("rail2newMetro")) {
					ugDevelopLength += link.getLength();
//					Log.write("Adding ugDevelopLength = "+link.getLength());
				}
				else {
					ugNewLength += link.getLength();
//					Log.write("Adding ugNewLength = "+link.getLength());
				}
			}
			else {
				ogLength += link.getLength();
//				Log.write("LinkType = "+metroLinkAttributes.get(linkId).type);
				if (metroLinkAttributes.get(linkId).type.equals("rail2newMetro")) {
					if (GeomDistance.calculate(link.getFromNode().getCoord(), UGcenterCoord) < OGdevelopRadius) {
						ogDevelopLength += link.getLength();
//						Log.write("Adding ogDevelopLength = "+link.getLength());
					}
					else {
						ogEquipLength += link.getLength();
//						Log.write("Adding ogNewLength = "+link.getLength());
					}
				}
				else {
					ogNewLength += link.getLength();
//					Log.write("Adding ogNewLength = "+link.getLength());
				}
			}
		}
		
		this.routeLength = totalLength;
		
		this.undergroundPercentage = ugLength/totalLength;
		if (this.undergroundPercentage > 0.0) {
			this.NewUGpercentage = ugNewLength/ugLength;
			this.DevelopUGPercentage = ugDevelopLength/ugLength;			
		}
		if (this.undergroundPercentage < 1.0) {
			this.NewOGpercentage = ogNewLength/ogLength;
			this.EquipOGPercentage = ogEquipLength/ogLength;
			this.DevelopOGPercentage = ogDevelopLength/ogLength;			
		}
//		Log.write(Double.toString(this.undergroundPercentage));
//		Log.write(Double.toString(this.NewUGpercentage));
//		Log.write(Double.toString(this.DevelopUGPercentage));
//		Log.write(Double.toString(this.NewOGpercentage));
//		Log.write(Double.toString(this.EquipOGPercentage));		
//		Log.write(Double.toString(this.DevelopOGPercentage));
	}

	

	
	@SuppressWarnings("unchecked")
	public void sumStations() throws FileNotFoundException {
//		System.out.println("Summing new stops found on: "+this.routeID.toString());
		Map<String, CustomStop> railStops = new HashMap<String, CustomStop>();
		railStops.putAll(XMLOps.readFromFile(railStops.getClass(), "zurich_1pm/Evolution/Population/BaseInfrastructure/railStopAttributes.xml"));
		List<Id<TransitStopFacility>> stopFacilities = new ArrayList<Id<TransitStopFacility>>();
		for (TransitRoute tr : this.transitLine.getRoutes().values()) {
			stopsLoop:
			for (TransitRouteStop trs : tr.getStops()) {
				TransitStopFacility tsf = trs.getStopFacility();
				if (!stopFacilities.contains(tsf.getId())) {
					stopFacilities.add(tsf.getId());
					for (CustomStop cs : railStops.values()) {
						if (cs.transitStopFacility.getId().equals(tsf.getId())) {
							this.nStationsExtend ++;
//							System.out.println("StopFacility based on railStop +1-"+tsf.getId().toString()+" ("+tsf.getName().toString()+")");
							continue stopsLoop;
						}
					}
					this.nStationsNew ++; // if did not manage to assign stop to a priorly existing rail stop
//					System.out.println("StopFacility to be built new +1-"+tsf.getId().toString()+" ("+tsf.getName().toString()+")");
				}
			}
		}
	}
	
//	public double calculateConstAndOpsCost(double populationFactor) throws IOException {
//		double lengthUG = this.routeLength*this.undergroundPercentage;
//		double lengthOG = this.routeLength*(1-this.undergroundPercentage);
//		double lengthOGnew = lengthOG*this.NewOGpercentage*(1-this.undergroundPercentage);
//		double lengthOGequip = lengthOG*this.EquipOGPercentage*(1-this.undergroundPercentage);
//		double lengthOGdevelopExisting = lengthOG*this.DevelopOGPercentage*(1-this.undergroundPercentage);
//		double lengthUGnew = lengthUG*this.NewUGpercentage*this.undergroundPercentage;
//		double lengthUGdevelopExisting = lengthUG*this.DevelopUGPercentage*this.undergroundPercentage;		
//		double ptVehicleLengthDrivenUG = totalDrivenDist*this.undergroundPercentage;
//		double ptVehicleLengthDrivenOG = totalDrivenDist*(1-this.undergroundPercentage);
//		
//		double constructionCost = ConstrCostPerStationNew*this.nStationsNew + ConstrCostPerStationExtend*this.nStationsExtend +
//				ConstrCostUGnew*lengthUGnew + ConstrCostUGdevelop*lengthUGdevelopExisting +
//				ConstrCostOGnew*lengthOGnew + ConstrCostOGdevelop*lengthOGdevelopExisting + ConstrCostOGequip*lengthOGequip;
//		double landCost = 0.01*constructionCost;
//		double rollingStockCost = this.vehiclesNr*costVehicle;
//		this.constrCost = (landCost + constructionCost + rollingStockCost);
//		Log.write("Overall Yearly ConstrCost (Split onto 40y)= "+this.constrCost/40);
//		
//		double opsCost = OpsCostPerVehDistUG*ptVehicleLengthDrivenUG*365 + OpsCostPerVehDistOG*ptVehicleLengthDrivenOG*365; // include here all ops cost of vehicles, infrastructure & overhead
//		double maintenanceCost = 0.01*this.constrCost;	// CAUTION: Have to calculate MRoute.constrCost first!!	
//		double repairCost = 0.01*this.constrCost;
//		this.opsCost = opsCost + maintenanceCost + repairCost;
//		Log.write("Yearly(Ops)Cost = "+this.opsCost);
//		
//		return this.constrCost + this.opsCost;
//	}
	
	
	public double performCostBenefitAnalysisRoute(CostBenefitParameters refCase, CostBenefitParameters newCase, double totalPersonMetroDist,
			Network globalNetwork, Map<Id<Link>, CustomMetroLinkAttributes> metroLinkAttributes) throws IOException {
		
		this.sumStations();
		this.calculatePercentages(globalNetwork, metroLinkAttributes);
		
		// suffix "x" means populationFactor was accounted for in this parameter
		final double ConstrCostUGnew = 1.5E5;
		final double ConstrCostUGdevelop = 1.0E5;
		final double ConstrCostOGnew = 4.0E4;
		final double ConstrCostOGdevelop = 3.0E4;
		final double ConstrCostOGequip = 0.5E4;
		final double ConstrCostPerStationNew = 6.0E4;
		final double ConstrCostPerStationExtend = 3.0E4;
		final double costVehicle = 2*6.0E6;	// x2 because assumed to be replaced once for 40y total lifetime (=2x20y)
		
		final double OpsCostPerVehDistUG = 17.0/1000;
		final double OpsCostPerVehDistOG = 11.3/1000;
		final double occupancyRate = 1.42; // personsPerVehicle
		final double ptPassengerCostPerDist = 0.1407/1000; // average price/km to buy a ticket for a trip with a certain distance
		final double taxPerVehicleDist = 0.06/1000;
		final double carCostPerVehDist = (0.1403 + 0.11 + 0.13)/1000; 				// CHF/vehicleKM generalCost(repair etc.) + fuel + write-off
		final double externalCarCosts = 0.077/1000;  	// CHF/personKM  [noise, pollution, climate, accidents, energy]    OLD:(0.0111 + 0.0179 + 0.008 + 0.03)/1000
		final double externalPtCosts = 0.032/1000;	// CHF/personKM [noise, pollution, climate, accidents] + [energyForInfrastructure]   || OLD: 0.023/1000 + EnergyCost*energyPerPtPersDist;

		final double ptTrafficIncreasePercentage = 0.28; // by 2040 --> because we build infra anyways, this is just higher ticket revenue!!
		final double VATPercentage = 0.08;
		final double utilityOfTimePT = 14.43/3600; // CHF/s
		final double utilityOfTimeCar = 23.29/3600; // CHF/s

		double lengthUG = this.routeLength*this.undergroundPercentage;
		double lengthOG = this.routeLength*(1-this.undergroundPercentage);
		double lengthOGnew = lengthOG*this.NewOGpercentage;
		double lengthOGequip = lengthOG*this.EquipOGPercentage;
		double lengthOGdevelopExisting = lengthOG*this.DevelopOGPercentage;
		double lengthUGnew = lengthUG*this.NewUGpercentage;
		double lengthUGdevelopExisting = lengthUG*this.DevelopUGPercentage;		
		double ptVehicleLengthDrivenUG = totalDrivenDist*this.undergroundPercentage;
		double ptVehicleLengthDrivenOG = totalDrivenDist*(1-this.undergroundPercentage);
		
		// Proportional Network Usage Projected To Route
		double deltaCarPersonDist = (newCase.carPersonDist-refCase.carPersonDist)*(this.personMetroDist/totalPersonMetroDist); 
		double deltaPtPersonDist = (newCase.ptPersonDist-refCase.ptPersonDist)*(this.personMetroDist/totalPersonMetroDist);
		
		// ---- Cost
		double constructionCost = ConstrCostPerStationNew*this.nStationsNew + ConstrCostPerStationExtend*this.nStationsExtend +
				ConstrCostUGnew*lengthUGnew + ConstrCostUGdevelop*lengthUGdevelopExisting +
				ConstrCostOGnew*lengthOGnew + ConstrCostOGdevelop*lengthOGdevelopExisting + ConstrCostOGequip*lengthOGequip;
		double rollingStockCost = this.vehiclesNr*costVehicle;
		double opsCost = OpsCostPerVehDistUG*ptVehicleLengthDrivenUG*365 + OpsCostPerVehDistOG*ptVehicleLengthDrivenOG*365; // include here all ops cost of vehicles, infrastructure & overhead
		double landCost = 0.01*constructionCost;
		double maintenanceCost = 0.01*constructionCost;
		double repairCost = 0.01*constructionCost;
		double externalCost = (externalPtCosts*deltaPtPersonDist*365 + taxPerVehicleDist*(-deltaCarPersonDist)/occupancyRate*365); 	// external PT cost + MPT tax-losses
		double ptPassengerCost = (deltaPtPersonDist*ptPassengerCostPerDist*365);
		
		// ---- Utility
		double vehicleSavings = carCostPerVehDist*(-deltaCarPersonDist)/occupancyRate*365;
		double extCostSavingsCar = externalCarCosts*(-deltaCarPersonDist);		// *0.70 at the end to account for externalCostPT, which are not considered above (see SBB)
		double ptVatIncrease = VATPercentage*deltaPtPersonDist*ptPassengerCostPerDist*365*ptTrafficIncreasePercentage;
			Double currentCongestionTimeLoss = 51.0*3600;	// annual time loss [s/person]; Source: INRIX2017 =55*3600/365s/person/day = 542s/person/day = 9min/person/day
			Double futureCongestionTimeLoss = currentCongestionTimeLoss*1.33; // factor 1.33 for congestion in 2040 --> See DownloadedPDFs 16.10
			Double congTimeSavingRatio = 0.02; //Math.sqrt(0.01);	// deltaKMcar/overallKMcar --> Use root to depict real life effects of congestion, e.g. quadratic
			Double congTimeSavingsPerPerson = congTimeSavingRatio*futureCongestionTimeLoss;
			Double nCarUsersNow = (this.personMetroDist/totalPersonMetroDist)*newCase.carUsers;
			Double nCarUsersFuture = 1.14*nCarUsersNow;		// 
			Double congTimeSaving = nCarUsersFuture*congTimeSavingsPerPerson;
			Double utilityOfTime = 23.32/3600; // CHF/s [car]
			Double congestionSavings = utilityOfTime*congTimeSaving;
			double travelTimeGains = congestionSavings + (this.personMetroDist/totalPersonMetroDist)*
					365*((refCase.carTimeTotal-newCase.carTimeTotal)*utilityOfTimeCar+(refCase.ptTimeTotal-newCase.ptTimeTotal)*utilityOfTimePT); // Total Utility of Time Approach
		// ---- annual total cost change
		double totalCost = (constructionCost+landCost+rollingStockCost)/lifeTime + opsCost + maintenanceCost + repairCost + externalCost + ptPassengerCost;
		// ---- annual total utility change
		double totalUtility = vehicleSavings + extCostSavingsCar + ptVatIncrease + travelTimeGains;
		this.utilityBalance = totalUtility-totalCost;
		
		Log.write("---------  "+ this.routeID);
//		Log.write("lengthUG = "+lengthUG/1000);
//		Log.write("lengthOG = "+lengthOG/1000);
//		Log.write("lengthOGnew = "+lengthOGnew/1000);
//		Log.write("lengthOGequip = "+lengthOGequip/1000);
//		Log.write("lengthOGdevelopExisting = "+lengthOGdevelopExisting/1000);
//		Log.write("lengthUGnew = "+lengthUGnew/1000);
//		Log.write("lengthUGdevelopExisting = "+lengthUGdevelopExisting/1000);
//
//		Log.write("this.personMetroDist/totalPersonMetroDist = "+this.personMetroDist/totalPersonMetroDist);
//		Log.write("deltaCarPersonDistDaily [km] = "+deltaCarPersonDist/1000);
//		Log.write("deltaPtPersonDistDaily [km] = "+deltaPtPersonDist/1000);
//		Log.write("constructionCostAnnual = "+constructionCost/lifeTime);
//		Log.write("opsCostAnnual = "+opsCost);
//		Log.write("landCostAnnual + maintenanceCostAnnual + repairCostAnnual = "+ landCost/lifeTime+maintenanceCost+repairCost);
//		Log.write("rollingStockCostAnnual = "+rollingStockCost/lifeTime);
//		Log.write("externalCostAnnual = ExternalPTCost + TaxLossCars = " + externalCost);
//		Log.write("ptCostAnnual = "+ptPassengerCost);
//		Log.write("-------------- Annual Cost (-) = " + totalCost +  "--------------");
		Log.write("  Annual Cost (-) [Construction/Operation] = " + totalCost +  "["+constructionCost/lifeTime+"/"+opsCost+"]");
//		Log.write("vehicleSavingsAnnual = "+vehicleSavings);
//		Log.write("extCostSavingsAnnual = "+extCostSavingsCar);
//		Log.write("ptVatIncreaseAnnual = "+ptVatIncrease);
//		Log.write("travelTimeGainsAnnual = car2PtGainsAnnual + pt2PtGainsAnnual = "+travelTimeGains);
//		Log.write("-------------- Annual Utility (+) = " + totalUtility +  "--------------");
//		Log.write("-------------- UTILITY BALANCE " + this.routeID + " = "+(totalUtility-totalCost) + "--------------");
		Log.write("  Annual Utility (+) [TravelGainsPT/Car/Congestion + Other] = " + totalUtility +  
				" = ["+(this.personMetroDist/totalPersonMetroDist)*365*(refCase.ptTimeTotal-newCase.ptTimeTotal)*utilityOfTimePT+
				"+"+(this.personMetroDist/totalPersonMetroDist)*365*(refCase.carTimeTotal-newCase.carTimeTotal)*utilityOfTimeCar+
				"/"+congestionSavings + " + " + (vehicleSavings+extCostSavingsCar+ptVatIncrease)+"]");
		Log.write("  UTILITY BALANCE " + this.routeID + " = "+(totalUtility-totalCost));
		
		return this.utilityBalance;
	}
	
	
	
	public String getId() {
		return this.routeID;
	}
	public void setId(String stringID) {
		this.routeID = stringID;
	}
	
	
	public NetworkRoute getNetworkRoute() {
		return this.networkRoute;
	}
	public void setNetworkRoute(NetworkRoute networkRoute) {
		this.networkRoute = networkRoute;
	}
	
	public List<Id<Node>> getNodeList() {
		return this.nodeList;
	}
	public void setNodeList(List<Id<Node>> list) {
		this.nodeList = list;
	}
	
	public List<Id<Link>> getLinkList() {
		return this.getLinkList();
	}
	public void setLinkList(List<Id<Link>> list) {
		this.linkList = list;
	}
	
	public String getTransitScheduleFile() {
		return this.transitScheduleFile;
	}
	public void setTransitScheduleFile(String transitScheduleFile) {
		this.transitScheduleFile = transitScheduleFile;
	}
	
	public String getEventsFile() {
		return this.eventsFile;
	}
	public void setEventsFile(String eventsFile) {
		this.eventsFile = eventsFile;
	}
	
	public double getTotalDrivenDist() {
		return this.totalDrivenDist;
	}
	public void setTotalDrivenDist(double totalDrivenDist) {
		this.totalDrivenDist = totalDrivenDist;
	}
	
	public double getPersonMetroKM() {
		return this.personMetroDist;
	}
	public void setMetroPersonKM(double personKM) {
		this.personMetroDist = personKM;
	}
	
	public Double getUndergroundPercentage() {
		return this.undergroundPercentage;
	}
	public void setUGPercentage(double UGPercentage) {
		this.undergroundPercentage = UGPercentage;
	}
	
	public Integer getBoardingNr() {
		return this.nBoardings;
	}
	public void setBoardingNr(int nPassengers) {
		this.nBoardings = nPassengers;
	}
	
	public Double getRouteLength() {
		return this.routeLength;
	}
	public void setRouteLength(double routeLength) {
		this.routeLength = routeLength;
	}
	
	public void setRouteLength(Network network) {
		double totalLength = 0.0;
		for (Id<Link> linkID : this.linkList) {
			totalLength += network.getLinks().get(linkID).getLength();
		}
		this.routeLength = totalLength;
	}

	public TransitLine getTransitLine() {
		return this.transitLine;
	}
	
	public void setTransitLine(TransitLine transitLine) {
		this.transitLine = transitLine;
	}

	
	public boolean modifyFrequency(Double probPositiveFreqMod) throws IOException {
		if (probPositiveFreqMod < 0.0) {
			Log.write("CAUTION: ProbPositiveFreqMod < 0. Applying no frequency modification.");
			return false;
		}
		if ((new Random()).nextDouble() < probPositiveFreqMod) {
			this.vehiclesNr++;
			this.lastFreqMod = "positive";
			this.attemptedFrequencyModifications.add("positive");
		}
		else {
			this.vehiclesNr--;
			this.lastFreqMod = "negative";
			this.attemptedFrequencyModifications.add("negative");
			this.blockedFreqModGenerations = 1;
		}
		this.lastUtilityBalance = this.utilityBalance;
		this.freqModOccured = true;
		return true;
	}
	
}

