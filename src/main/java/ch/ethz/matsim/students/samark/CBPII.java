package ch.ethz.matsim.students.samark;

import java.util.Arrays;

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
	double customVariable4 = 0.0;
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
	
}
