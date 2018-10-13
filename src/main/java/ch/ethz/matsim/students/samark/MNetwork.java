package ch.ethz.matsim.students.samark;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicles;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("MNetwork")
public class MNetwork implements Serializable{

	private static final long serialVersionUID = 1L;

	// CAUTION: When adding to MNetwork, also add in Clone.mNetwork!
		Network network;
		String networkFileLocation;
		String networkID;
		Map<String, MRoute> routeMap;
		double totalRouteLength;			// calculate from individual route lengths (one-way only) 
		TransitSchedule transitSchedule;
		Vehicles vehicles;
		double lifeTime;
		
		// from events
		double personMetroDist;		// NetworkEvolutionRunSim.runEventsProcessing
		double personKMdirect;			// to be implemented in: NetworkEvolutionRunSim.runEventsProcessing
		int nMetroUsers;				// NetworkEvolutionRunSim.runEventsProcessing
		double totalPtPersonDist;
		// from transitSchedule
		double totalDrivenDist;				// TODO: to be implemented in NetworkEvolution (may make separate scoring function!) --> Take lengths from route lengths and km from nDepartures*routeLengths
		double annualCost;					// to be implemented in NetworkEvolution
		double annualBenefit;				// to be implemented in NetworkEvolution
		int totalVehiclesNr;
		// from evolution loop
		int evolutionGeneration;		// NetworkEvolution --> Evolutionary loop
		double averageTravelTime;		// NetworkEvolutionRunSim.peoplePlansProcessingM
		double stdDeviationTravelTime;	// NetworkEvolutionRunSim.peoplePlansProcessingM
		double totalTravelTime; 		// NetworkEvolutionRunSim.peoplePlansProcessingM
		List<String> parents;
		String evoLog;
		// Calculate
		double overallScore;			// NetworkEvolution main separate line
	
	
	public MNetwork() {
		this.lifeTime = 40.0;
		this.personMetroDist = 0.0;
		this.averageTravelTime = Double.MAX_VALUE;
		this.stdDeviationTravelTime = Double.MAX_VALUE;
		this.totalTravelTime = Double.MAX_VALUE;
		this.personKMdirect = 0.0;
		this.nMetroUsers = 0;
		this.totalDrivenDist = 0.0;
		this.totalRouteLength = 0.0;		
		this.annualCost = Double.MAX_VALUE;
		this.annualBenefit = -Double.MAX_VALUE;
		this.evolutionGeneration = 0;
		this.overallScore = 0.0;
		this.totalVehiclesNr = 0;
		this.totalPtPersonDist = 0.0;
		this.parents = Arrays.asList("", "");
		this.evoLog = "";
	}
	
	public MNetwork(String name) {
		this.lifeTime = 40.0;
		this.networkID = name;
		this.routeMap = new HashMap<String, MRoute>();
		this.personMetroDist = 0.0;
		this.averageTravelTime = Double.MAX_VALUE;
		this.stdDeviationTravelTime = Double.MAX_VALUE;
		this.totalTravelTime = Double.MAX_VALUE;
		this.personKMdirect = 0.0;
		this.nMetroUsers = 0;
		this.totalDrivenDist = 0.0;
		this.annualCost = Double.MAX_VALUE;
		this.annualBenefit = -Double.MAX_VALUE;
		this.evolutionGeneration = 0;
		this.overallScore = 0.0;
		this.totalRouteLength = 0.0;	
		this.totalVehiclesNr = 0;
		this.totalPtPersonDist = 0.0;
		this.parents = Arrays.asList("", "");
		this.evoLog = "";
	}
	
	public boolean hasParents() {
		if (this.parents.contains("")) {
			return false;
		}
		else {
			return true;
		}
	}

	public void setParents(String parent1, String parent2) {
		this.parents = Arrays.asList(parent1, parent2);
	}
	
	public void addPedigreeData(String sin) {
		this.evoLog += ("\r\n" + sin);
	}
	
	// TODO: QUESTIONS:
	// - UtilityOfTime for MPT->PT Switchers
	// - Substantial accident cost rate (what about accident cost in PT?)
	
	public void calculateRoutesAndNetworkScore(int lastIterationOriginal, double populationFactor,
			Network globalNetwork, Map<Id<Link>, CustomMetroLinkAttributes> metroLinkAttributes) throws IOException {
		CostBenefitParameters cbpOriginal =
				XMLOps.readFromFile((new CostBenefitParameters()).getClass(), "zurich_1pm/cbaParametersOriginal"+lastIterationOriginal+".xml");
		CostBenefitParameters cbpNew =
				XMLOps.readFromFile((new CostBenefitParameters()).getClass(), "zurich_1pm/Evolution/Population/"+this.networkID+"/cbaParameters"+lastIterationOriginal+".xml");		

		this.calculateTotalRouteLengthAndDrivenKM();
		
		// CALCULATE ROUTE SCORES
		for (MRoute mr : this.routeMap.values()) {
			Log.write("Processing MRoute = " + mr.routeID);
			mr.lifeTime = this.lifeTime;
			mr.calculatePercentages(globalNetwork, metroLinkAttributes);
			mr.sumStations(); // both, nStationsNew/Extended
			mr.performCostBenefitAnalysisRoute(cbpOriginal, cbpNew, this.personMetroDist, globalNetwork, metroLinkAttributes);

//			mr.calculateConstCost();
//			mr.calculateOpsCost(populationFactor);
		}

		// calculate here percentages for entire networks from single mroutes
		double routeWeight = 0.0;
		double overallUGpercentage = 0.0;
		double newUGpercentage = 0.0;
		double developUGpercentage = 0.0;
		double newOGpercentage = 0.0;
		double equipOGpercentage = 0.0;
		double developOGpercentage = 0.0;
		int nVehicles = 0;
		int nStationsNew = 0;
		int nStationsExtend = 0;
		for (MRoute mr : this.routeMap.values()) {
			nVehicles += mr.vehiclesNr;
			nStationsNew += mr.nStationsNew;
			nStationsExtend += mr.nStationsExtend;
			routeWeight = mr.routeLength/this.totalRouteLength;
			overallUGpercentage += mr.undergroundPercentage*routeWeight;
			newUGpercentage += mr.NewUGpercentage*overallUGpercentage*routeWeight;
			developUGpercentage += mr.DevelopUGPercentage*overallUGpercentage*routeWeight;
			newOGpercentage += mr.NewOGpercentage*(1-overallUGpercentage)*routeWeight;
			equipOGpercentage += mr.EquipOGPercentage*(1-overallUGpercentage)*routeWeight;
			developOGpercentage += mr.DevelopOGPercentage*(1-overallUGpercentage)*routeWeight;
		}	
		
		this.overallScore = this.performCostBenefitAnalysisNetwork(40.0, populationFactor, cbpOriginal, cbpNew, this.totalRouteLength, this.totalDrivenDist, 
				overallUGpercentage, newUGpercentage, developUGpercentage, newOGpercentage, equipOGpercentage, developOGpercentage,
				nVehicles, nStationsNew, nStationsExtend);
	}
	
	public double performCostBenefitAnalysisNetwork(double lifeTime, double populationFactor, CostBenefitParameters refCase, CostBenefitParameters newCase,
			double totalRouteLength, double totalDrivenDist, double overallUGpercentage, double newUGpercentage, double developUGpercentage, double newOGpercentage, 
			double equipOGpercentage, double developOGpercentage, int nVehicles, int nStationsNew, int nStationsExtend) throws IOException {
		
		double lengthUG = totalRouteLength*overallUGpercentage;
		double lengthOG = totalRouteLength*(1-overallUGpercentage);
		double lengthOGnew = lengthOG*newOGpercentage*(1-overallUGpercentage);
		double lengthOGequip = lengthOG*equipOGpercentage*(1-overallUGpercentage);
		double lengthOGdevelopExisting = lengthOG*developOGpercentage*(1-overallUGpercentage);
		double lengthUGnew = lengthUG*newOGpercentage*overallUGpercentage;
		double lengthUGdevelopExisting = lengthUG*developUGpercentage*overallUGpercentage;		
		double ptVehicleLengthDrivenUG = totalDrivenDist*overallUGpercentage;
		double ptVehicleLengthDrivenOG = totalDrivenDist*(1-overallUGpercentage);
		
		
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
//		final double EnergyCost = 0.03; // 0.03.-/kWh = 30.-/MWh
//		final double energyPerPtPersDist = 0.157/1000; // kWh/personKM
//		final double PtVehicleDist = totalDrivenDist;
//		final double energyPerPtVehDist = energyPerPtPersDist*newCase.ptPersonDist/PtVehicleDist;
		final double occupancyRate = 1.42; // personsPerVehicle
		final double ptPassengerCostPerDist = 0.1407/1000; // average price/km to buy a ticket for a trip with a certain distance
		final double taxPerVehicleDist = 0.06/1000;
		final double carCostPerVehDist = (0.1403 + 0.11 + 0.13)/1000; 				// CHF/vehicleKM generalCost(repair etc.) + fuel + write-off
		final double externalCarCosts = 0.077/1000;  	// CHF/personKM  [noise, pollution, climate, accidents, energy]    OLD:(0.0111 + 0.0179 + 0.008 + 0.03)/1000
		final double externalPtCosts = 0.032/1000;	// CHF/personKM [noise, pollution, climate, accidents] + [energyForInfrastructure]   || OLD: 0.023/1000 + EnergyCost*energyPerPtPersDist;

		final double VATPercentage = 0.08;
		final double utilityOfTimePT = 14.43/3600; // CHF/s
		final double utilityOfTimeCar = 23.29/3600; // CHF/s

		// ---- 
		double deltaCarPersonDist = newCase.carPersonDist-refCase.carPersonDist;
		double deltaCarVehicleDist = deltaCarPersonDist/occupancyRate;
		double deltaPtPersonDist = newCase.ptPersonDist-refCase.ptPersonDist;
		// ---- 
		double constructionCost = ConstrCostPerStationNew*nStationsNew + ConstrCostPerStationExtend*nStationsExtend +
				ConstrCostUGnew*lengthUGnew + ConstrCostUGdevelop*lengthUGdevelopExisting +
				ConstrCostOGnew*lengthOGnew + ConstrCostOGdevelop*lengthOGdevelopExisting + ConstrCostOGequip*lengthOGequip;
		double opsCost = OpsCostPerVehDistUG*ptVehicleLengthDrivenUG*365 + OpsCostPerVehDistOG*ptVehicleLengthDrivenOG*365; // include here all ops cost of vehicles, infrastructure & overhead
		double landCost = 0.01*constructionCost;
		double maintenanceCost = 0.01*constructionCost;
		double repairCost = 0.01*constructionCost;
		double rollingStockCost = nVehicles*costVehicle;
		double externalCost = externalPtCosts*deltaPtPersonDist*365 + taxPerVehicleDist*(-deltaCarPersonDist)/occupancyRate*365;	// external PT cost + MPT tax-losses
		double ptPassengerCost = deltaPtPersonDist*ptPassengerCostPerDist*365;
		// ---- 
		double vehicleSavings = carCostPerVehDist*(-deltaCarPersonDist)/occupancyRate*365;
		double extCostSavingsCar = externalCarCosts*(-deltaCarPersonDist)*365;		// *0.70 at the end to account for externalCostPT, which are not considered above (see SBB)
		double ptVatIncrease = VATPercentage*deltaPtPersonDist*ptPassengerCostPerDist*365;	

		// Option 1 : Total Utility of Time Approach
			double travelTimeGains = 365*((refCase.carTimeTotal-newCase.carTimeTotal)*utilityOfTimeCar+(refCase.ptTimeTotal-newCase.ptTimeTotal)*utilityOfTimePT);
		// Option 2 : Combined Approach
		//	double travelTimeGains = 365*(
		//		(refCase.carUsers-newCase.carUsers)*(refCase.carTimeTotal/refCase.carUsers*utilityOfTimeCar-newCase.ptTimeTotal/newCase.ptUsers*utilityOfTimePT)
		//		+refCase.ptUsers*(refCase.ptTimeTotal/refCase.ptUsers-newCase.ptTimeTotal/newCase.ptUsers)*utilityOfTimePT
		//	);
		// Option 3 : Marginal Utility for Delta Approach
		//	double travelTimeGains = 365*(
		//		((refCase.carUsers-newCase.carUsers)*(refCase.carTimeTotal/refCase.carUsers-newCase.ptTimeTotal/newCase.ptUsers)
		//		+refCase.ptUsers*(refCase.ptTimeTotal/refCase.ptUsers-newCase.ptTimeTotal/newCase.ptUsers))*utilityOfTimePT
		//	);
		
		// ---- annual total cost change
		double totalCost = (constructionCost+landCost+rollingStockCost)/lifeTime + opsCost + maintenanceCost + repairCost + externalCost + ptPassengerCost;
		this.annualCost = totalCost;
		// ---- annual total utility change
		double totalUtility = vehicleSavings + extCostSavingsCar + ptVatIncrease + travelTimeGains;
		this.annualBenefit = totalUtility;

		Log.write("----%%%%%%----  "+ this.networkID +"  ----%%%%%%----");
		Log.write("deltaCarPersonDistDaily [km] = "+deltaCarPersonDist/1000);
		Log.write("deltaCarVehicleDistDaily [km] = "+deltaCarVehicleDist/1000);
		Log.write("deltaPtPersonDistDaily [km] = "+deltaPtPersonDist/1000);
		Log.write("constructionCostAnnual = "+constructionCost/lifeTime);
		Log.write("opsCostAnnual = "+opsCost);
		Log.write("land/maintenance/repairCostAnnual = "+landCost/lifeTime+maintenanceCost+repairCost);
		Log.write("rollingStockCostAnnual = "+rollingStockCost/lifeTime);
		Log.write("externalCostAnnual = ExternalPTCost + TaxLossCars = "+ externalPtCosts*deltaPtPersonDist*365 + "+" + taxPerVehicleDist*(-deltaCarPersonDist)/occupancyRate*365 + "=" + externalCost);
		Log.write("ptCostAnnual = "+ptPassengerCost);
		Log.write("-------------------- Annual Cost (-) = " + totalCost +  "------------------------");
		Log.write("vehicleSavingsAnnual = "+vehicleSavings);
		Log.write("extCostSavingsAnnual = "+extCostSavingsCar);
		Log.write("ptVatIncreaseAnnual = "+ptVatIncrease);
		Log.write("travelTimeGainsAnnual = car2PtGainsAnnual + pt2PtGainsAnnual = "+travelTimeGains + " = " +
				365*(refCase.carUsers-newCase.carUsers)*(refCase.carTimeTotal/refCase.carUsers-newCase.ptTimeTotal/newCase.ptUsers)*utilityOfTimePT +
				" + " + 365*refCase.ptUsers*(refCase.ptTimeTotal/refCase.ptUsers-newCase.ptTimeTotal/newCase.ptUsers)*utilityOfTimePT);
		Log.write("-------------------- Annual Utility (+) = " + totalUtility +  "------------------------");
		Log.write("-------------------- UTILITY BALANCE " + this.networkID + " = "+(totalUtility-totalCost) + "--------------------");

		return totalUtility-totalCost;
	}
	
	
	
	public void calculateTotalRouteLengthAndDrivenKM() {
		double totalRouteLength = 0.0;
		double totalDrivenDist = 0.0;
		for (MRoute mroute : this.routeMap.values()) {
			totalRouteLength += mroute.routeLength;
			totalDrivenDist += mroute.totalDrivenDist;
		}
		this.totalRouteLength = totalRouteLength;
		this.totalDrivenDist = totalDrivenDist;
	}
	
	
	
	public double getTotalTravelTime() {
		return this.totalTravelTime;
	}
	public void setTotalTravelTime(double totalTravelTime) {
		this.totalTravelTime = totalTravelTime;
	}
	
	public double getStdDeviationTravelTime() {
		return this.stdDeviationTravelTime;
	}
	public void setStdDeviationTravelTime(double stdDeviationTravelTime) {
		this.stdDeviationTravelTime = stdDeviationTravelTime;
	}	
	
	public double getAverageTravelTime() {
		return this.averageTravelTime;
	}
	public void setAverageTravelTime(double averageTravelTime) {
		this.averageTravelTime = averageTravelTime;
	}	
	
	public int getEvolutionGeneration() {
		return this.evolutionGeneration;
	}
	public void setEvolutionGeneration(int evolutionGeneration) {
		this.evolutionGeneration = evolutionGeneration;
	}
	
	public double getAnnualCost() {
		return this.annualCost;
	}
	public void setAnnualCost(double annualCost) {
		this.annualCost = annualCost;
	}		
	
	public double getAnnualBenefit() {
		return this.annualBenefit;
	}
	public void setAnnualBenfit(double annualBenefit) {
		this.annualBenefit = annualBenefit;
	}	
	
	public double getDrivenKM() {
		return this.totalDrivenDist;
	}
	public void setDrivenKM(double drivenKM) {
		this.totalDrivenDist = drivenKM;
	}
	
	public double getPersonKMdirect() {
		return this.personKMdirect;
	}
	public void setPersonKMdirect(double personKMdirect) {
		this.personKMdirect = personKMdirect;
	}		
	
	public double getTotalMetroPersonKM() {
		return this.personMetroDist;
	}
	public void setTotalMetroPersonKM(double personKM) {
		this.personMetroDist = personKM;
	}	
	
	public int getNMetroUsers() {
		return this.nMetroUsers;
	}
	public void setnMetroUsers(int n) {
		this.nMetroUsers = n;
	}
	
	public Vehicles getVehicles() {
		return this.vehicles;
	}
	
	public void setVehicles(Vehicles vehicles) {
		this.vehicles = vehicles;
	}
	
	public void setTransitSchedule(TransitSchedule ts) {
		this.transitSchedule = ts;
	}
	
	public TransitSchedule getTransitSchedule() {
		return this.transitSchedule;
	}
	
	public void setNetwork(Network network) {
		this.network = network;
	}
	
	public Network getNetwork() {
		return this.network;
	}
	
	public void setNetworkID(String networkID) {
		this.networkID = networkID;
	}
	
	public String getNetworkID() {
		return this.networkID;
	}
	
    private void writeObject(ObjectOutputStream oos) 
    	      throws IOException {
    	        oos.defaultWriteObject();
    	        oos.writeObject(network);
    	        //oos.writeUTF();
    	    }
    
    private void readObject(ObjectInputStream ois) 
    	      throws ClassNotFoundException, IOException {
    	        ois.defaultReadObject();
    	        this.network = (Network) ois.readObject();
    	        // Network network = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getNetwork();
    	    }
	
	public void addNetworkRoute(MRoute newRoute) {
		// consider changing name of route to: newRoute.routeID = (this.networkID+newRoute.routeID);
		routeMap.put(newRoute.routeID, newRoute);
	}
	
	public Map<String, MRoute> getRouteMap(){
		return this.routeMap;
	}
	
	public void setRouteMap(Map<String, MRoute> routeMap) {
		this.routeMap = routeMap;
	}

	public void addRoutes(List<MRoute> routesList) {
		for (MRoute route : routesList) {
			this.routeMap.put(route.routeID, route);
		}
		
	}
	
}
