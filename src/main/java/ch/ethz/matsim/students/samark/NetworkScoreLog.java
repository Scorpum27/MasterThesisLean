package ch.ethz.matsim.students.samark;

public class NetworkScoreLog {

	double averageTravelTime;
	double stdDeviationTravelTime;
	double totalTravelTime;
	double totalMetroPersonKM;
	double personKMdirect;
	int nMetroUsers;
	double drivenKM;
	double opsCost;
	double constrCost;
	int evolutionGeneration;
	double overallScore;
	double totalRouteLength;
	double rouletteScore;



	public NetworkScoreLog() {
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
		this.overallScore  = 0.0;
		this.totalRouteLength = 0.0;		
	}
	
	
	public void NetworkScore2LogMap(MNetwork mn) {
		// this.averageTravelTime = mn.averageTravelTime;				// already done in main loop
		// this.stdDeviationTravelTime = mn.stdDeviationTravelTime;		// already done in main loop
		// this.totalTravelTime = mn.totalTravelTime;					// already done in main loop
		this.totalMetroPersonKM = mn.totalMetroPersonKM;
		this.personKMdirect = mn.personKMdirect;
		this.nMetroUsers = mn.nMetroUsers;
		this.drivenKM = mn.drivenKM;
		this.opsCost = mn.opsCost;
		this.constrCost = mn.constrCost;
		this.evolutionGeneration = mn.evolutionGeneration;
		this.overallScore = mn.overallScore;
		this.averageTravelTime = mn.averageTravelTime;
		this.stdDeviationTravelTime = mn.stdDeviationTravelTime;
		this.totalTravelTime = mn.totalTravelTime;
		this.totalRouteLength = mn.totalRouteLength;
	}
	
	
	
}
