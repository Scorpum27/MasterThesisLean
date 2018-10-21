package ch.ethz.matsim.students.samark;

public class InfrastructureParameters {
	
	Double ConstrCostUGnew;
	Double ConstrCostUGdevelop;
	Double ConstrCostOGnew;
	Double ConstrCostOGdevelop;
	Double ConstrCostOGequip;
	Double ConstrCostPerStationNew;
	Double ConstrCostPerStationExtend;
	Double costVehicle;
	Double OpsCostPerVehDistUG;
	Double OpsCostPerVehDistOG;
//	final double EnergyCost;
//	final double energyPerPtPersDist;
	Double occupancyRate;
	Double ptPassengerCostPerDist;
	Double taxPerVehicleDist;
	Double carCostPerVehDist;
	Double externalCarCosts;
	Double externalPtCosts;
	Double ptTrafficIncreasePercentage;
	Double VATPercentage;
	Double utilityOfTimePT;
	Double utilityOfTimeCar;
	
	public InfrastructureParameters(Double ConstrCostUGnew, Double ConstrCostUGdevelop,	Double ConstrCostOGnew, Double ConstrCostOGdevelop,
			Double ConstrCostOGequip, Double ConstrCostPerStationNew, Double ConstrCostPerStationExtend, Double costVehicle, 
			Double OpsCostPerVehDistUG, Double OpsCostPerVehDistOG, Double occupancyRate, Double ptPassengerCostPerDist,
			Double taxPerVehicleDist, Double carCostPerVehDist, Double externalCarCosts, Double externalPtCosts, Double ptTrafficIncreasePercentage,
			Double VATPercentage, Double utilityOfTimePT, Double utilityOfTimeCar) {
		this.ConstrCostUGnew = ConstrCostUGnew;
		this.ConstrCostUGdevelop = ConstrCostUGdevelop;
		this.ConstrCostOGnew = ConstrCostOGnew;
		this.ConstrCostOGdevelop = ConstrCostOGdevelop;
		this.ConstrCostOGequip = ConstrCostOGequip;
		this.ConstrCostPerStationNew = ConstrCostPerStationNew;
		this.ConstrCostPerStationExtend = ConstrCostPerStationExtend;
		this.costVehicle = costVehicle;
		this.OpsCostPerVehDistUG = OpsCostPerVehDistUG;
		this.OpsCostPerVehDistOG = OpsCostPerVehDistOG;
		this.occupancyRate = occupancyRate;
		this.ptPassengerCostPerDist = ptPassengerCostPerDist;
		this.taxPerVehicleDist = taxPerVehicleDist;
		this.carCostPerVehDist = carCostPerVehDist;
		this.externalCarCosts = externalCarCosts;
		this.externalPtCosts = externalPtCosts;
		this.ptTrafficIncreasePercentage = ptTrafficIncreasePercentage;
		this.VATPercentage = VATPercentage;
		this.utilityOfTimePT = utilityOfTimePT;
		this.utilityOfTimeCar = utilityOfTimeCar;
	}
	
	
}
