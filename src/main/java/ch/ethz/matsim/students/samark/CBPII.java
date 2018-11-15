package ch.ethz.matsim.students.samark;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;

public class CBPII{

	
	// from CostBenefitParameters
	double ptUsers = 0.0;
	double carUsers = 0.0;
	double otherUsers = 0.0;
	double carTimeTotal = 0.0;
	double carPersonDist = 0.0;			// [m]
	double ptTimeTotal = 0.0;
	double ptPersonDist = 0.0;			// [m]
	double averagePtTime = 0.0;
	double averageCartime = 0.0;
	double metroPersonDist = 0.0;		// [m]
	double totalTravelTime = 0.0;
	
	// new ones 
	double constCost = 0.0;				// CHF/a
	double opsCost = 0.0; 				// CHF/a
	double mrCost = 0.0;				// CHF/a
	double travelUtil = 0.0;			// CHF/a
	double extCostSavings = 0.0;		// CHF/a
	double totalAnnualCost = 0.0;		// CHF/a
	double totalAnnualBenefit = 0.0;	// CHF/a
	
	// if needed in future:
	double rollingStockCost = 0.0;		// CHF/a
	double landCost = 0.0;
	double externalCost = 0.0;
	double ptPassengerCost = 0.0;
	double travelTimeGainsCar = 0.0;
	double travelTimeGainsPt = 0.0;
	double travelTotalUtilGainsPt = 0.0;
	double travelTimeGains = 0.0;
	double ptVatIncrease = 0.0;
	double customVariable1 = 0.0;	// other travel TIME 		= WALK/BIKE (not car/pt)
	double customVariable2 = 0.0;	// other travel TIME GAINS  = WALK/BIKE (not car/pt)
	double customVariable3 = 0.0;	// ptDisutilityEquivalentTimeTotal
	double customVariable4 = 0.0;	// vehicleSavings
	double customVariable5 = 0.0;
	double customVariable6 = 0.0;
	double customVariable7 = 0.0;
	double customVariable8 = 0.0;
	double customVariable9 = 0.0;
	double customVariable10 = 0.0;
	
	public CBPII() {
	}
	
//	public CBPII(double constCost, double opsCost, double mrCost, double travelUtil,
//			double extCostSavings, double totalAnnualCost, double totalAnnualBenefit) {
//		this.constCost = constCost;
//		this.opsCost = opsCost;
//		this.mrCost = mrCost;
//		this.travelUtil = travelUtil;
//		this.extCostSavings = extCostSavings;
//		this.totalAnnualCost = totalAnnualCost;
//		this.totalAnnualBenefit = totalAnnualBenefit;
//	}
	
	public CBPII(CostBenefitParameters cbp) {
		this.ptUsers = cbp.ptUsers;
		this.carUsers = cbp.carUsers;
		this.otherUsers = cbp.otherUsers;
		this.carTimeTotal = cbp.carTimeTotal;
		this.carPersonDist = cbp.carPersonDist;
		this.ptTimeTotal = cbp.ptTimeTotal;
		this.ptPersonDist = cbp.ptPersonDist;
		this.averageCartime = cbp.averageCartime;
		this.averagePtTime = cbp.averagePtTime;
		this.metroPersonDist = cbp.metroPersonDist;
		this.totalTravelTime = cbp.totalTravelTime;
	}
	
	public CBPII(double ptUsers, double carUsers, double otherUsers,
			double carTimeTotal, double carPersonDist, double ptTimeTotal, double ptPersonDist) {
		this.ptUsers = ptUsers;
		this.carUsers = carUsers;
		this.otherUsers = otherUsers;
		this.carTimeTotal = carTimeTotal;
		this.carPersonDist = carPersonDist;
		this.ptTimeTotal = ptTimeTotal;
		this.ptPersonDist = ptPersonDist;
		this.averageCartime = carTimeTotal / carUsers;
		this.averagePtTime = ptTimeTotal / ptUsers;
	}
	
	public CBPII( double ptUsers, double carUsers, double otherUsers,
			double carTimeTotal, double carPersonDist, double ptTimeTotal, double ptPersonDist, double metroPersonDist) {
		this.ptUsers = ptUsers;
		this.carUsers = carUsers;
		this.otherUsers = otherUsers;
		this.carTimeTotal = carTimeTotal;
		this.carPersonDist = carPersonDist;
		this.ptTimeTotal = ptTimeTotal;
		this.ptPersonDist = ptPersonDist;
		this.averageCartime = carTimeTotal / carUsers;
		this.averagePtTime = ptTimeTotal / ptUsers;
		this.metroPersonDist = metroPersonDist;
	}
	
	
	public CBPII( double ptUsers, double carUsers, double otherUsers, double carTimeTotal, double carPersonDist,
			double ptTimeTotal, double ptPersonDist, double metroPersonDist, double totalTravelTime) {
		this.ptUsers = ptUsers;
		this.carUsers = carUsers;
		this.otherUsers = otherUsers;
		this.carTimeTotal = carTimeTotal;
		this.carPersonDist = carPersonDist;
		this.ptTimeTotal = ptTimeTotal;
		this.ptPersonDist = ptPersonDist;
		this.averageCartime = carTimeTotal / carUsers;
		this.averagePtTime = ptTimeTotal / ptUsers;
		this.metroPersonDist = metroPersonDist;
		this.totalTravelTime = totalTravelTime;
	}
	
	public void mNetwork2CBPII(MNetwork mNetwork) {
		this.constCost = mNetwork.constructionCost;
		this.opsCost = mNetwork.operationalCost;
		this.travelUtil = mNetwork.travelTimeGainsCar + mNetwork.travelTimeGainsPT;
		this.totalAnnualCost = mNetwork.annualCost;
		this.totalAnnualBenefit = mNetwork.annualBenefit;
	}
	
	public void calculateAverages() {
		//this.averageCartime = this.carTimeTotal/this.carUsers;
		//this.averagePtTime = this.ptTimeTotal/this.ptUsers;		
	}

	public void calculateBenefits(CBPII refCase) throws FileNotFoundException {

		// all values in CHF|m|s --> convert seconds to years below for annual utility! 
			InfrastructureParameters infrastructureParameters =
					XMLOps.readFromFile(InfrastructureParameters.class, "zurich_1pm/Evolution/Population/BaseInfrastructure/infrastructureCost.xml");

			
			final double occupancyRate = infrastructureParameters.occupancyRate;
			final double carCostPerVehDist = infrastructureParameters.carCostPerVehDist;
			final double externalCarCosts = infrastructureParameters.externalCarCosts;
			final double externalPtCosts = infrastructureParameters.externalPtCosts;
			
			final double utilityOfTimePT = infrastructureParameters.utilityOfTimePT;
			final double utilityOfTimeCar = infrastructureParameters.utilityOfTimeCar;
			final double utilityOfTimeOther = 33.20/3600.0;	// infrastructureParameters.utilityOfTimeOther;		
				
			// TRAFFIC MODEL SIMULATION
			double discountFactor = 1.02;
			int lifeTime = 40;
			double averageDiscountFactor = MNetwork.getAverageDiscountFactor(discountFactor, lifeTime);					//	[-], used to average discount over lifetime of yearly recurring cost
			double annualDeltaCarPersonDist2020 = 250*(this.carPersonDist-refCase.carPersonDist);			//  [m/y], double annualDeltaCarVehicleDist2020 = annualDeltaCarPersonDist2020/occupancyRate
			double annualDeltaPtPersonDist2020 = 250*(this.ptPersonDist-refCase.ptPersonDist);				//  [m/y]
			List<Double> annualDeltaCarPersonDist20xx = MNetwork.makeMptUsagePrognosis(annualDeltaCarPersonDist2020);	//  [m/y]	// initiate with expected annual deltaCarPersonDist with 2020 MATSim result
			List<Double> annualDeltaPtPersonDist20xx = MNetwork.makePtUsagePrognosis(annualDeltaPtPersonDist2020);		//  [m/y]	// initiate with expected annual deltaPtPersonDist with 2020 MATSim result
			double annualDeltaCarPersonTime2020 = 250*(this.carTimeTotal-refCase.carTimeTotal);				//  [s/y], double annualDeltaCarVehicleDist2020 = annualDeltaCarPersonDist2020/occupancyRate
			double annualDeltaPtPersonTime2020 = 250*(this.customVariable3 - refCase.customVariable3);		//  [s/y]			
			double annualDeltaOtherPersonTime2020 = 250*((this.ptTimeTotal-refCase.ptTimeTotal));			//  [s/y]
			List<Double> annualDeltaCarPersonTime20xx = MNetwork.makeMptUsagePrognosis(annualDeltaCarPersonTime2020);	//  [s/y]	// initiate with expected annual deltaCarPersonDist with 2020 MATSim result
			List<Double> annualDeltaPtPersonTime20xx = MNetwork.makePtUsagePrognosis(annualDeltaPtPersonTime2020);		//  [s/y]	// initiate with expected annual deltaPtPersonDist with 2020 MATSim result		
			List<Double> annualDeltaOtherPersonTime20xx = MNetwork.makeWalkBikeUsagePrognosis(annualDeltaOtherPersonTime2020);		//  [s/y]	// initiate with expected annual deltaOtherPersonDist with 2020 MATSim result
			// BENEFIT
			double vehicleSavings = -MNetwork.timeCorrectedUtility((int) lifeTime, Arrays.asList(annualDeltaCarPersonDist20xx), carCostPerVehDist/occupancyRate, discountFactor, true); // [CHF/year]
			this.customVariable4 = vehicleSavings;
			double extCostSavings = MNetwork.timeCorrectedUtility((int) lifeTime, Arrays.asList(annualDeltaPtPersonDist20xx), externalPtCosts, discountFactor, true) - 
					MNetwork.timeCorrectedUtility((int) lifeTime, Arrays.asList(annualDeltaCarPersonDist20xx), externalCarCosts, discountFactor, true); // [CHF/year]
			this.extCostSavings = extCostSavings;
			Double travelTimeGainsCar = -MNetwork.timeCorrectedUtility((int) lifeTime, Arrays.asList(annualDeltaCarPersonTime20xx), utilityOfTimeCar, discountFactor, true); // [CHF/year]
			Double travelTimeGainsPt = -MNetwork.timeCorrectedUtility((int) lifeTime, Arrays.asList(annualDeltaPtPersonTime20xx), utilityOfTimePT, discountFactor, true); // [CHF/year]			
			Double travelTimeGainsWalkBike = -MNetwork.timeCorrectedUtility((int) lifeTime, Arrays.asList(annualDeltaOtherPersonTime20xx), utilityOfTimeOther, discountFactor, true); // [CHF/year]
			Double travelTimeGains = travelTimeGainsCar + travelTimeGainsPt + travelTimeGainsWalkBike;
			this.travelTimeGainsCar = travelTimeGainsCar;
			this.travelTimeGainsPt = travelTimeGainsPt;
			this.travelTimeGains = travelTimeGains;
			this.customVariable2 = travelTimeGainsWalkBike;
			// Double otherTravelUtil = newCase.travelUtil; // must make travelUtil in plansProcessing // add -timeCorrectedUtility( here!!!
			double ptVatIncrease = 0.0;
			this.ptVatIncrease = ptVatIncrease;
			double congestionSavings = 0.0;
			// ---- annual total utility change
			Double totalUtility = vehicleSavings + extCostSavings + ptVatIncrease + travelTimeGains + congestionSavings;
			this.totalAnnualBenefit = totalUtility;
			this.travelTimeGainsCar = travelTimeGainsCar;
			this.travelTimeGainsPt  = travelTimeGainsPt;
			//this.otherUtil? = vehicleSavings+extCostSavings+ptVatIncrease;
	}
	
	public static CBPII calculateAveragesX(List<CBPII> CBPs) {
		double ptUsersX = 0.0;
		double carUsersX = 0.0;
		double otherUsersX = 0.0;
		double carTimeTotalX = 0.0;
		double carPersonDistX = 0.0;
		double ptTimeTotalX = 0.0;
		double ptPersonDistX = 0.0;
		double metroPersonDistX = 0.0;
		double averagePtTimeX = 0.0;
		double averageCartimeX = 0.0;
		double totalTravelTimeX = 0.0;
		double constCostX = 0.0;			// CHF/a
		double opsCostX = 0.0; 				// CHF/a
		double mrCostX = 0.0;				// CHF/a
		double travelUtilX = 0.0;			// CHF/a
		double extCostSavingsX = 0.0;		// CHF/a
		double totalAnnualCostX = 0.0;		// CHF/a
		double totalAnnualBenefitX = 0.0;	// CHF/a
		double rollingStockCostX = 0.0;		// CHF/a
		double landCostX = 0.0;
		double externalCostX = 0.0;
		double ptPassengerCostX = 0.0;
		double travelTimeGainsCarSumX = 0.0;
		double travelTimeGainsPtSumX = 0.0;
		double travelTotalUtilGainsPtSumX = 0.0;
		double travelTimeGainsSumX = 0.0;
		double ptVatIncreaseX = 0.0;
		double customVariable1X = 0.0;	// other travel TIME 		= WALK/BIKE (not car/pt)
		double customVariable2X = 0.0;	// other travel TIME GAINS  = WALK/BIKE (not car/pt)
		double customVariable3X = 0.0;	// ptDisutilityEquivalentTimeTotal
		double customVariable4X = 0.0;
		double customVariable5X = 0.0;
		double customVariable6X = 0.0;
		double customVariable7X = 0.0;
		double customVariable8X = 0.0;
		double customVariable9X = 0.0;
		double customVariable10X = 0.0;
		int iterGlobalAverage = CBPs.size();
		for (CBPII cbpi : CBPs) {	
			ptUsersX += cbpi.ptUsers;
			carUsersX += cbpi.carUsers;
			otherUsersX += cbpi.otherUsers;
			carTimeTotalX += cbpi.carTimeTotal;
			carPersonDistX += cbpi.carPersonDist;
			ptTimeTotalX += cbpi.ptTimeTotal;
			ptPersonDistX += cbpi.ptPersonDist;
			metroPersonDistX += cbpi.metroPersonDist;
			averagePtTimeX += cbpi.averagePtTime;
			averageCartimeX += cbpi.averageCartime;
			totalTravelTimeX += cbpi.totalTravelTime;
			constCostX += cbpi.constCost;
			opsCostX += cbpi.opsCost;
			mrCostX += cbpi.mrCost;
			travelUtilX += cbpi.travelUtil;
			extCostSavingsX += cbpi.extCostSavings;
			totalAnnualCostX += cbpi.totalAnnualCost;
			totalAnnualBenefitX += cbpi.totalAnnualBenefit;
			rollingStockCostX += cbpi.rollingStockCost;
			landCostX += cbpi.landCost;
			externalCostX += cbpi.externalCost;
			ptPassengerCostX += cbpi.ptPassengerCost;
			travelTimeGainsCarSumX += cbpi.travelTimeGainsCar;
			travelTimeGainsPtSumX += cbpi.travelTimeGainsPt;
			travelTotalUtilGainsPtSumX += cbpi.travelTotalUtilGainsPt;
			travelTimeGainsSumX += cbpi.travelTimeGains;
			ptVatIncreaseX += cbpi.ptVatIncrease;
			customVariable1X += cbpi.customVariable1;	// other travel TIME 		= WALK/BIKE (not car/pt)
			customVariable2X += cbpi.customVariable2;	// other travel TIME GAINS  = WALK/BIKE (not car/pt)
			customVariable3X += cbpi.customVariable3;	// ptDisutilityEquivalentTimeTotal
			customVariable4X += cbpi.customVariable4;
			customVariable5X += cbpi.customVariable5;
			customVariable6X += cbpi.customVariable6;
			customVariable7X += cbpi.customVariable7;
			customVariable8X += cbpi.customVariable8;
			customVariable9X += cbpi.customVariable9;
			customVariable10X += cbpi.customVariable10;
		}
		CBPII cbpGlobal = new CBPII(ptUsersX/iterGlobalAverage, carUsersX/iterGlobalAverage, otherUsersX/iterGlobalAverage,
				carTimeTotalX/iterGlobalAverage, carPersonDistX/iterGlobalAverage, ptTimeTotalX/iterGlobalAverage, ptPersonDistX/iterGlobalAverage,
				metroPersonDistX/iterGlobalAverage);
		cbpGlobal.averagePtTime= averagePtTimeX/iterGlobalAverage;
		cbpGlobal.averageCartime = averageCartimeX/iterGlobalAverage;
		cbpGlobal.totalTravelTime = totalTravelTimeX/iterGlobalAverage;
		cbpGlobal.constCost = constCostX/iterGlobalAverage;
		cbpGlobal.opsCost = opsCostX/iterGlobalAverage;
		cbpGlobal.mrCost = mrCostX/iterGlobalAverage;
		cbpGlobal.travelUtil = travelUtilX/iterGlobalAverage;
		cbpGlobal.extCostSavings = extCostSavingsX/iterGlobalAverage;
		cbpGlobal.totalAnnualCost = totalAnnualCostX/iterGlobalAverage;
		cbpGlobal.totalAnnualBenefit = totalAnnualBenefitX/iterGlobalAverage;
		cbpGlobal.rollingStockCost = rollingStockCostX/iterGlobalAverage;
		cbpGlobal.landCost = landCostX/iterGlobalAverage;
		cbpGlobal.externalCost = externalCostX/iterGlobalAverage;
		cbpGlobal.ptPassengerCost = ptPassengerCostX/iterGlobalAverage;
		cbpGlobal.travelTimeGainsCar = travelTimeGainsCarSumX/iterGlobalAverage;
		cbpGlobal.travelTimeGainsPt = travelTimeGainsPtSumX/iterGlobalAverage;
		cbpGlobal.travelTotalUtilGainsPt = travelTotalUtilGainsPtSumX/iterGlobalAverage;
		cbpGlobal.travelTimeGains = travelTimeGainsSumX/iterGlobalAverage;
		cbpGlobal.ptVatIncrease = ptVatIncreaseX/iterGlobalAverage;
		cbpGlobal.customVariable1 = customVariable1X/iterGlobalAverage;	// other travel TIME 		= WALK/BIKE (not car/pt)
		cbpGlobal.customVariable2= customVariable2X/iterGlobalAverage;	// other travel TIME GAINS  = WALK/BIKE (not car/pt)
		cbpGlobal.customVariable3= customVariable3X/iterGlobalAverage;	// ptDisutilityEquivalentTimeTotal
		cbpGlobal.customVariable4 = customVariable4X/iterGlobalAverage;
		cbpGlobal.customVariable5 = customVariable5X/iterGlobalAverage;
		cbpGlobal.customVariable6 = customVariable6X/iterGlobalAverage;
		cbpGlobal.customVariable7= customVariable7X/iterGlobalAverage;
		cbpGlobal.customVariable8= customVariable8X/iterGlobalAverage;
		cbpGlobal.customVariable9 = customVariable9X/iterGlobalAverage;
		cbpGlobal.customVariable10 = customVariable10X/iterGlobalAverage;		
		return cbpGlobal;
	}
}
