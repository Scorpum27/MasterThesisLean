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
		this.opsCost = 1.0E20;
		this.constrCost = 1.0E20;
		this.utilityBalance = -1.0E20;
		this.lastUtilityBalance = -1.0E20;
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
//			if (metroLinkAttributes.get(linkId) == null) {
//				Log.write("Link NOT FOUND in metroLinkAttributes: "+linkId);
//				continue;
//			}
//			else if (metroLinkAttributes.get(linkId).type == null) {
//				Log.write("Link has no TYPE metroLinkAttributes: "+linkId);
//				continue;
//			}
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
		else {
			this.NewUGpercentage = 0.0;
			this.DevelopUGPercentage = 0.0;
		}
		if (this.undergroundPercentage < 1.0) {
			this.NewOGpercentage = ogNewLength/ogLength;
			this.EquipOGPercentage = ogEquipLength/ogLength;
			this.DevelopOGPercentage = ogDevelopLength/ogLength;			
		}
		else {
			this.NewOGpercentage = 0.0;
			this.EquipOGPercentage = 0.0;
			this.DevelopOGPercentage = 0.0;
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
		
		InfrastructureParameters infrastructureParameters =
				XMLOps.readFromFile(InfrastructureParameters.class, "zurich_1pm/Evolution/Population/BaseInfrastructure/infrastructureCost.xml");
		
		final double ConstrCostUGnew = infrastructureParameters.ConstrCostUGnew;
		final double ConstrCostUGdevelop = infrastructureParameters.ConstrCostUGdevelop;
		final double ConstrCostOGnew = infrastructureParameters.ConstrCostOGnew;
		final double ConstrCostOGdevelop = infrastructureParameters.ConstrCostOGdevelop;
		final double ConstrCostOGequip = infrastructureParameters.ConstrCostOGequip;
		final double ConstrCostPerStationNew = infrastructureParameters.ConstrCostPerStationNew;
		final double ConstrCostPerStationExtend = infrastructureParameters.ConstrCostPerStationExtend;
		final double costVehicle = infrastructureParameters.costVehicle;
		
		final double OpsCostPerVehDistUG = infrastructureParameters.OpsCostPerVehDistUG;
		final double OpsCostPerVehDistOG = infrastructureParameters.OpsCostPerVehDistOG;
		final double occupancyRate = infrastructureParameters.occupancyRate;
		final double ptPassengerCostPerDist = infrastructureParameters.ptPassengerCostPerDist;
		final double taxPerVehicleDist = infrastructureParameters.taxPerVehicleDist;
		final double carCostPerVehDist = infrastructureParameters.carCostPerVehDist;
		final double externalCarCosts = infrastructureParameters.externalCarCosts;
		final double externalPtCosts = infrastructureParameters.externalPtCosts;

		final double ptTrafficIncreasePercentage = infrastructureParameters.ptTrafficIncreasePercentage;
		final double VATPercentage = infrastructureParameters.VATPercentage;
		final double utilityOfTimePT = infrastructureParameters.utilityOfTimePT;
		final double utilityOfTimeCar = infrastructureParameters.utilityOfTimeCar;

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

		// old utility module
//		Log.write("this.personMetroDist = "+this.personMetroDist);
//		double deltaCarPersonDist = 0.0; 
//		double deltaPtPersonDist = 0.0;
//		if (totalPersonMetroDist > 0.0) { // do this in case totalPersonMetroDist = 0.0, which would give INFINITY
//			deltaCarPersonDist = (newCase.carPersonDist-refCase.carPersonDist)*(this.personMetroDist/totalPersonMetroDist); 
//			deltaPtPersonDist = (newCase.ptPersonDist-refCase.ptPersonDist)*(this.personMetroDist/totalPersonMetroDist);
//		}
//		
//		// ---- Cost
//		double constructionCost = ConstrCostPerStationNew*this.nStationsNew + ConstrCostPerStationExtend*this.nStationsExtend +
//				ConstrCostUGnew*lengthUGnew + ConstrCostUGdevelop*lengthUGdevelopExisting +
//				ConstrCostOGnew*lengthOGnew + ConstrCostOGdevelop*lengthOGdevelopExisting + ConstrCostOGequip*lengthOGequip;
//		double rollingStockCost = this.vehiclesNr*costVehicle;
//		double opsCost = OpsCostPerVehDistUG*ptVehicleLengthDrivenUG*365 + OpsCostPerVehDistOG*ptVehicleLengthDrivenOG*365; // include here all ops cost of vehicles, infrastructure & overhead
//		double landCost = 0.01*constructionCost;
//		double maintenanceCost = 0.01*constructionCost;
//		double repairCost = 0.01*constructionCost;
//		double externalCost = (externalPtCosts*deltaPtPersonDist*365 + taxPerVehicleDist*(-deltaCarPersonDist)/occupancyRate*365); 	// external PT cost + MPT tax-losses
//		double ptPassengerCost = (deltaPtPersonDist*ptPassengerCostPerDist*365);
//		
//		// ---- Utility
//		double vehicleSavings = carCostPerVehDist*(-deltaCarPersonDist)/occupancyRate*365;
//		double extCostSavingsCar = externalCarCosts*(-deltaCarPersonDist);		// *0.70 at the end to account for externalCostPT, which are not considered above (see SBB)
//		double ptVatIncrease = VATPercentage*deltaPtPersonDist*ptPassengerCostPerDist*365*ptTrafficIncreasePercentage;
//			Double currentCongestionTimeLoss = 51.0*3600;	// annual time loss [s/person]; Source: INRIX2017 =55*3600/365s/person/day = 542s/person/day = 9min/person/day
//			Double futureCongestionTimeLoss = currentCongestionTimeLoss*1.33; // factor 1.33 for congestion in 2040 --> See DownloadedPDFs 16.10
//			Double congTimeSavingRatio = Math.max(0.0, (refCase.carTimeTotal-newCase.carTimeTotal)/refCase.carTimeTotal); // 0.02; //Math.sqrt(0.01);	// deltaKMcar/overallKMcar --> Use root to depict real life effects of congestion, e.g. quadratic
//			Double congTimeSavingsPerPerson = congTimeSavingRatio*futureCongestionTimeLoss;
//			Double nCarUsersNow = (this.personMetroDist/totalPersonMetroDist)*newCase.carUsers;
//			Double nCarUsersFuture = 1.14*nCarUsersNow;		// 
//			Double congTimeSaving = nCarUsersFuture*congTimeSavingsPerPerson;
//			Double utilityOfTime = 23.32/3600; // CHF/s [car]
//			Double congestionSavings = utilityOfTime*congTimeSaving;
//			Double travelTimeGainsCar = Math.max(0.0, (this.personMetroDist/totalPersonMetroDist)*365*(refCase.carTimeTotal-newCase.carTimeTotal)*utilityOfTimeCar);
//			Double travelTimeGainsPt = (this.personMetroDist/totalPersonMetroDist)*365*(refCase.ptTimeTotal-newCase.ptTimeTotal)*utilityOfTimePT; // Math.max(0.0, (this.personMetroDist/totalPersonMetroDist)*365*(refCase.ptTimeTotal-newCase.ptTimeTotal)*utilityOfTimePT);

		// new utility module
		double deltaPtUsers = newCase.ptUsers-refCase.ptUsers;
		double deltaCarUsers = newCase.carUsers - refCase.carUsers;
		double switchers = (1.2*deltaPtUsers+1.0*(-deltaCarUsers))/2.2;	// weight pt slightly stronger as other mode users may also switch to pt --> some extra benefit ok
		if (switchers < 0.0) { Log.write("Sim. fluctuations leading to positive pt2car-switching. This is unrealistic --> setting nSwitchers = 0"); switchers = 0.0; }
		double deltaCarPersonDist = 0.0; 
		double deltaPtPersonDist = 0.0;
		if (totalPersonMetroDist > 0.0) { // do this in case totalPersonMetroDist = 0.0, which would give INFINITY
			deltaCarPersonDist = (-switchers*newCase.carPersonDist/newCase.carUsers)*(this.personMetroDist/totalPersonMetroDist); 
			deltaPtPersonDist = (switchers*newCase.ptPersonDist/newCase.ptUsers)*(this.personMetroDist/totalPersonMetroDist);
		}
		
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
			Double congTimeSavingRatio = Math.max(0.0, (refCase.carTimeTotal-newCase.carTimeTotal)/refCase.carTimeTotal); // 0.02; //Math.sqrt(0.01);	// deltaKMcar/overallKMcar --> Use root to depict real life effects of congestion, e.g. quadratic
			Double congTimeSavingsPerPerson = congTimeSavingRatio*futureCongestionTimeLoss;
			Double nCarUsersNow = (this.personMetroDist/totalPersonMetroDist)*newCase.carUsers;
			Double nCarUsersFuture = 1.14*nCarUsersNow;		// 
			Double congTimeSaving = nCarUsersFuture*congTimeSavingsPerPerson;
			Double utilityOfTime = 23.32/3600; // CHF/s [car]
			Double congestionSavings = utilityOfTime*congTimeSaving;
			Double travelTimeGainsCar = (this.personMetroDist/totalPersonMetroDist)*365*(switchers*refCase.carTimeTotal/refCase.carUsers)*utilityOfTimeCar;
			Double travelTimeGainsPt = (this.personMetroDist/totalPersonMetroDist)*365*(-switchers*refCase.ptTimeTotal/refCase.ptUsers)*utilityOfTimePT +
					Math.max(0.0, (this.personMetroDist/totalPersonMetroDist)*365*utilityOfTimePT*refCase.ptUsers*(refCase.averagePtTime-newCase.averagePtTime));
			Double travelTimeGains = congestionSavings + travelTimeGainsCar + travelTimeGainsPt;
			// ---- annual total cost change
		double totalCost = (constructionCost+landCost+rollingStockCost)/lifeTime + opsCost + maintenanceCost + repairCost + externalCost + ptPassengerCost;
		// ---- annual total utility change
		double totalUtility = vehicleSavings + extCostSavingsCar + ptVatIncrease + travelTimeGains;
		if (totalPersonMetroDist <= 0.0) {
			totalCost = (constructionCost+landCost+rollingStockCost)/lifeTime + opsCost + maintenanceCost + repairCost;
			totalUtility = 0.0;
		}
		this.utilityBalance = totalUtility-totalCost;
		
		Log.write("---------  "+ this.routeID);
		Log.write("TotalMetroRouteLength / Vehicles = "+this.routeLength/1000+" / "+this.vehiclesNr);
		Log.write("lengthUG (%new / %develop) [Km] = "+lengthUG/1000 + " ("+this.NewUGpercentage+" / "+this.DevelopUGPercentage+")");
		Log.write("lengthOG (%new / %develop) [Km] = "+lengthOG/1000 + " ("+this.NewOGpercentage+" / "+this.DevelopOGPercentage+")");
//		Log.write("VehicleMetroDistance = " + this.totalDrivenDist);
		Log.write("PersonMetroDistanceDaily [Km] = " + this.personMetroDist/1000);
		Log.write("this.personMetroDist/totalPersonMetroDist = "+this.personMetroDist/totalPersonMetroDist);
		if (totalPersonMetroDist <= 0.0) {
			Log.write("this.personMetroDist/totalPersonMetroDist above =INFINITY because totalPersonMetroDist=0. Proceeding without utility, but all the cost for construction/operation.");
		}
		Log.write("--Annual Cost (-) [Construction/Operation] = " + totalCost +  " ["+constructionCost/lifeTime+" / "+opsCost+"]");
		if (totalPersonMetroDist <= 0.0) {
			Log.write("  Annual Utility (+) = 0.0 (--> no metro users!)");
		}
		else {
			Log.write("--Annual Utility (+) [TravelGainsPT / Car / Congestion + Other] = " + totalUtility +  
					" = [ "+travelTimeGainsPt+
					" / "+travelTimeGainsCar+"(="+ (this.personMetroDist/totalPersonMetroDist)*365*(-switchers*refCase.ptTimeTotal/refCase.ptUsers)*utilityOfTimePT + "|"+
					Math.max(0.0, (this.personMetroDist/totalPersonMetroDist)*365*utilityOfTimePT*refCase.ptUsers*(refCase.averagePtTime-newCase.averagePtTime)) +")"+
					" / "+congestionSavings + " + " + (vehicleSavings+extCostSavingsCar+ptVatIncrease)+" ]");
		}
		Log.write("--Annual Utility (+) [TravelGainsPT(switchersLoss/ptUsersWin)/Car/Congestion + Other] = " + totalUtility +  
				" = [ "+travelTimeGainsPt+
				"(="+ (this.personMetroDist/totalPersonMetroDist)*365*(-switchers*refCase.ptTimeTotal/refCase.ptUsers)*utilityOfTimePT + "|"+
				Math.max(0.0, (this.personMetroDist/totalPersonMetroDist)*365*utilityOfTimePT*refCase.ptUsers*(refCase.averagePtTime-newCase.averagePtTime)) +")"+" / "+travelTimeGainsCar+
				" / "+congestionSavings + " + " + (vehicleSavings+extCostSavingsCar+ptVatIncrease)+" ]");
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
			Log.write("CAUTION: ProbPositiveFreqMod < 0. Applying no frequency modification to "+this.routeID);
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

