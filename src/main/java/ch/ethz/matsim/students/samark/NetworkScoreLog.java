package ch.ethz.matsim.students.samark;

public class NetworkScoreLog {

	double averageTravelTime;
	double stdDeviationTravelTime;
	double totalTravelTime;
	double personMetroDist;
	int nMetroUsers;
	double totalDrivenDist;
	double annualCost;
	double annualBenefit;
	double overallScore;
	double totalRouteLength;
	double rouletteScore;
	int evolutionGeneration;

	// depreceated
//	double personKMdirect;
//	double opsCost;
//	double constrCost;


	public NetworkScoreLog() {
		this.personMetroDist = 0.0;
		this.averageTravelTime = Double.MAX_VALUE;
		this.stdDeviationTravelTime = Double.MAX_VALUE;
		this.totalTravelTime = Double.MAX_VALUE;
		this.personMetroDist = 0.0;
		this.nMetroUsers = 0;
		this.totalDrivenDist = 0.0;
		this.annualCost = Double.MAX_VALUE;
		this.annualBenefit = -Double.MAX_VALUE;
		this.evolutionGeneration = 0;
		this.overallScore  = 0.0;
		this.totalRouteLength = 0.0;		
	}
	
	
	public void NetworkScore2LogMap(MNetwork mn) {
		// this.averageTravelTime = mn.averageTravelTime;				// already done in main loop
		// this.stdDeviationTravelTime = mn.stdDeviationTravelTime;		// already done in main loop
		// this.totalTravelTime = mn.totalTravelTime;					// already done in main loop
		this.personMetroDist = mn.personMetroDist;
		this.nMetroUsers = mn.nMetroUsers;
		this.totalDrivenDist = mn.totalDrivenDist;
		this.annualCost = mn.annualCost;
		this.annualBenefit = mn.annualBenefit;
		this.evolutionGeneration = mn.evolutionGeneration;
		this.overallScore = mn.overallScore;
		this.averageTravelTime = mn.averageTravelTime;
		this.stdDeviationTravelTime = mn.stdDeviationTravelTime;
		this.totalTravelTime = mn.totalTravelTime;
		this.totalRouteLength = mn.totalRouteLength;
	}
	
	
	
}
