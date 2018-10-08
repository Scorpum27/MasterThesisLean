package ch.ethz.matsim.students.samark;

import java.io.FileNotFoundException;
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

	public MNetwork() {
		this.totalMetroPersonKM = 0.0;
		this.averageTravelTime = Double.MAX_VALUE;
		this.stdDeviationTravelTime = Double.MAX_VALUE;
		this.totalTravelTime = Double.MAX_VALUE;
		this.totalMetroPersonKM = 0.0;
		this.personKMdirect = 0.0;
		this.nMetroUsers = 0;
		this.drivenKM = 0.0;
		this.totalRouteLength = 0.0;		
		this.opsCost = Double.MAX_VALUE;
		this.constrCost = Double.MAX_VALUE;
		this.evolutionGeneration = 0;
		this.overallScore = 0.0;
		this.totalVehiclesNr = 0;
		this.totalPtTransitPersonKM = 0.0;
		this.parents = Arrays.asList("", "");
		this.evoLog = "";
	}
	
	public MNetwork(String name) {
		this.networkID = name;
		this.routeMap = new HashMap<String, MRoute>();
		this.totalMetroPersonKM = 0.0;
		this.averageTravelTime = Double.MAX_VALUE;
		this.stdDeviationTravelTime = Double.MAX_VALUE;
		this.totalTravelTime = Double.MAX_VALUE;
		this.totalMetroPersonKM = 0.0;
		this.personKMdirect = 0.0;
		this.nMetroUsers = 0;
		this.drivenKM = 0.0;
		this.opsCost = Double.MAX_VALUE;
		this.constrCost = Double.MAX_VALUE;
		this.evolutionGeneration = 0;
		this.overallScore = 0.0;
		this.totalRouteLength = 0.0;	
		this.totalVehiclesNr = 0;
		this.totalPtTransitPersonKM = 0.0;
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
	
	public void calculateNetworkScore() {
		double a1 = 100.0;
		double b = 18000000.0;
		double c = -0.266;
		double d = 0.764;
		this.overallScore = 0.0 +  this.totalMetroPersonKM/100000 * (c+Math.exp((this.drivenKM)/(b)-d));	// CostPerMetroKM = C(m) = c+exp(this.drivenKM/b-d)
	}
	
	public void calculateNetworkScore2(int lastIterationOriginal, double populationFactor,
			Network globalNetwork, Map<Id<Link>, CustomMetroLinkAttributes> metroLinkAttributes) throws IOException {
		CostBenefitParameters cbpOriginal =
				XMLOps.readFromFile((new CostBenefitParameters()).getClass(), "zurich_1pm/cbaParametersOriginal"+lastIterationOriginal+".xml");
		CostBenefitParameters cbpNew =
				XMLOps.readFromFile((new CostBenefitParameters()).getClass(), "zurich_1pm/Evolution/Population/"+this.networkID+"/cbaParameters"+lastIterationOriginal+".xml");		

		for (MRoute mr : this.routeMap.values()) {
			Log.write("Processing MRoute = " + mr.routeID);
			mr.calculatePercentages(globalNetwork, metroLinkAttributes);
			mr.sumStations(); // both, nStationsNew/Extended
			mr.calculateConstCost();
			mr.calculateOpsCost(populationFactor);
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
		Log.write(Integer.toString(nVehicles));
		Log.write(Integer.toString(nStationsNew));
		Log.write(Integer.toString(nStationsExtend));
		Log.write(Double.toString(routeWeight));
		Log.write(Double.toString(overallUGpercentage));
		Log.write(Double.toString(newUGpercentage));		
		Log.write(Double.toString(developUGpercentage));
		Log.write(Double.toString(newOGpercentage));		
		Log.write(Double.toString(equipOGpercentage));
		Log.write(Double.toString(developOGpercentage));		
		
		this.overallScore = MNetwork.performCostBenefitAnalysis(40.0, populationFactor, cbpOriginal, cbpNew, this.totalRouteLength, this.drivenKM, 
				overallUGpercentage, newUGpercentage, developUGpercentage, newOGpercentage, equipOGpercentage, developOGpercentage,
				nVehicles, nStationsNew, nStationsExtend);
	}
	
	public static double performCostBenefitAnalysis(double lifeTime, double populationFactor, CostBenefitParameters refCase, CostBenefitParameters newCase,
			double totalLength, double drivenLength, double overallUGpercentage, double newUGpercentage, double developUGpercentage, double newOGpercentage, 
			double equipOGpercentage, double developOGpercentage, int nVehicles, int nStationsNew, int nStationsExtend) {
		
		double lengthUG = totalLength*overallUGpercentage/1000;
		double lengthOG = totalLength*(1-overallUGpercentage)/1000;
		double lengthOGnew = lengthOG*newOGpercentage*(1-overallUGpercentage);
		double lengthOGequip = lengthOG*equipOGpercentage*(1-overallUGpercentage);
		double lengthOGdevelopExisting = lengthOG*developOGpercentage*(1-overallUGpercentage);
		double lengthUGnew = lengthUG*newOGpercentage*overallUGpercentage;
		double lengthUGdevelopExisting = lengthUG*developUGpercentage*overallUGpercentage;		
		double ptVehicleLengthDrivenUG = drivenLength*overallUGpercentage/1000;
		double ptVehicleLengthDrivenOG = drivenLength*(1-overallUGpercentage)/1000;
		
		
		
		// suffix "x" means populationFactor was accounted for in this parameter
		
		final double ConstrCostPerKmUGnew = 1.5E8;
		final double ConstrCostPerKmUGdevelop = 1.0E8;
		final double ConstrCostPerKmOGnew = 4.0E7;
		final double ConstrCostPerKmOGdevelop = 3.0E7;
		final double ConstrCostPerKmOGequip = 0.5E7;
		final double ConstrCostPerStationNew = 6.0E7;
		final double ConstrCostPerStationExtend = 3.0E7;
		final double costPerVehicle = 2*6.0E6;	// x2 because assumed to be replaced once for 40y total lifetime (=2x20y)

		final double OpsCostPerVehicleKmUG = 17.0;
		final double OpsCostPerVehicleKmOG = 11.3;
		final double EnergyCost = 0.03; // 0.03.-/kWh = 30.-/MWh
		final double energyPerPtPersonKM = 0.157; // kWh/personKM
		final double PtVehicleKM = ptVehicleLengthDrivenUG+ptVehicleLengthDrivenOG;
		final double energyPerPtVehicleKMx = energyPerPtPersonKM*((newCase.ptPersonKM/1000)*populationFactor/(PtVehicleKM/1000));
		final double taxPerVehicleKM = 0.06;
		final double occupancyRate = 1.42; // personsPerVehicle
		final double ptPassengerCostPerKM = 0.1407; // average price/km to buy a ticket for a trip with a certain distance
		final double carCostPerVehicleKM = 0.1403; // CHF/KM
		final double externalVehicleCosts = 0.0111 + 0.0179 + 0.008 + 0.2862 + 0.11 + 0.13;  // noise, pollution, climate, accidents, fuel, write-off
		final double VATPercentage = 0.08;
		final double utilityOfTimePT = 14.43/3600; // CHF/s
		final double utilityOfTimeCar = 23.29/3600; // CHF/s
		
		
		double deltaCarPersonKMx = (refCase.carPersonKM-newCase.carPersonKM)/1000;
		double deltaCarVehicleKMx = deltaCarPersonKMx*occupancyRate/1000;
		double deltaPtPersonKMx = refCase.ptPersonKM-newCase.ptPersonKM/1000;

		double constructionCost = ConstrCostPerStationNew*nStationsNew + ConstrCostPerStationExtend*nStationsExtend +
				ConstrCostPerKmUGnew*lengthUGnew + ConstrCostPerKmUGdevelop*lengthUGdevelopExisting +
				ConstrCostPerKmOGnew*lengthOGnew + ConstrCostPerKmOGdevelop*lengthOGdevelopExisting + ConstrCostPerKmOGequip*lengthOGequip;
		double opsCost = OpsCostPerVehicleKmUG*ptVehicleLengthDrivenUG*365 + OpsCostPerVehicleKmOG*ptVehicleLengthDrivenOG*365; // include here all ops cost of vehicles, infrastructure & overhead
		double landCost = 0.01*constructionCost;
		double maintenanceCost = 0.01*constructionCost;		
		double repairCost = 0.01*constructionCost;
		double rollingStockCost = nVehicles*costPerVehicle; // TODO: Norm this (maybe for 10min frequency case)
		double externalCost = EnergyCost*energyPerPtVehicleKMx*(ptVehicleLengthDrivenUG+ptVehicleLengthDrivenOG)*365 + taxPerVehicleKM/occupancyRate*deltaCarPersonKMx*365;
		double ptCost = populationFactor*(newCase.ptPersonKM-refCase.ptPersonKM)/1000*ptPassengerCostPerKM*365;
		// --------------------- annual total cost change
		double totalCost = constructionCost/lifeTime + opsCost + landCost/lifeTime + maintenanceCost + repairCost + rollingStockCost/lifeTime + externalCost + ptCost;
		// --------------------------------------------
		
		
		double vehicleSavings = carCostPerVehicleKM/occupancyRate*(refCase.carPersonKM-newCase.carPersonKM)/1000*populationFactor*365;
		double extCostSavings = deltaCarVehicleKMx*externalVehicleCosts*365;
		double ptVatIncrease = deltaPtPersonKMx*VATPercentage*365;
		double travelTimeGains = 365*24*3600*((refCase.carUsers-newCase.carUsers)*populationFactor*(refCase.carTimeTotal/refCase.carUsers*utilityOfTimeCar-newCase.ptTimeTotal/newCase.ptUsers*utilityOfTimePT)
				+refCase.ptUsers*populationFactor*(refCase.ptTimeTotal/refCase.ptUsers-newCase.ptTimeTotal/newCase.ptUsers));
		// --------------------- annual total utility change
		double totalUtility = vehicleSavings + extCostSavings + ptVatIncrease + travelTimeGains;
		// --------------------------------------------

		return totalUtility-totalCost;
	}
	
	public void calculateTotalRouteLengthAndDrivenKM() {
		double totalRouteLength = 0.0;
		double totalDrivenKM = 0.0;
		for (MRoute mroute : this.routeMap.values()) {
			totalRouteLength += mroute.routeLength;
			totalDrivenKM += mroute.drivenKM;
		}
		this.totalRouteLength = totalRouteLength;
		this.drivenKM = totalDrivenKM;
	}
	
	// CAUTION: When adding to MNetwork, also add in Clone.mNetwork!
	Network network;
	String networkFileLocation;
	String networkID;
	Map<String, MRoute> routeMap;
	double totalRouteLength;			// calculate from individual route lengths (one-way only) 
	TransitSchedule transitSchedule;
	Vehicles vehicles;
	
	// from events
	double totalMetroPersonKM;		// NetworkEvolutionRunSim.runEventsProcessing
	double personKMdirect;			// to be implemented in: NetworkEvolutionRunSim.runEventsProcessing
	int nMetroUsers;				// NetworkEvolutionRunSim.runEventsProcessing
	double totalPtTransitPersonKM;
	// from transitSchedule
	double drivenKM;				// TODO: to be implemented in NetworkEvolution (may make separate scoring function!) --> Take lengths from route lengths and km from nDepartures*routeLengths
	double opsCost;					// to be implemented in NetworkEvolution
	double constrCost;				// to be implemented in NetworkEvolution
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
	
	public double getConstrCost() {
		return this.constrCost;
	}
	public void setConstrCost(double constrCost) {
		this.constrCost = constrCost;
	}		
	
	public double getOpsCost() {
		return this.opsCost;
	}
	public void setOpsCost(double opsCost) {
		this.opsCost = opsCost;
	}	
	
	public double getDrivenKM() {
		return this.drivenKM;
	}
	public void setDrivenKM(double drivenKM) {
		this.drivenKM = drivenKM;
	}
	
	public double getPersonKMdirect() {
		return this.personKMdirect;
	}
	public void setPersonKMdirect(double personKMdirect) {
		this.personKMdirect = personKMdirect;
	}		
	
	public double getTotalMetroPersonKM() {
		return this.totalMetroPersonKM;
	}
	public void setTotalMetroPersonKM(double personKM) {
		this.totalMetroPersonKM = personKM;
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
