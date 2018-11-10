package ch.ethz.matsim.students.samark;

public class CostBenefitParameters {

	double ptUsers = 0.0;
	double carUsers = 0.0;
	double otherUsers = 0.0;
	double carTimeTotal = 0.0;
	double carPersonDist = 0.0;			// [m]
	double ptTimeTotal = 0.0;
	double ptPersonDist = 0.0;			// [m]
	double averagePtTime = 0.0;
	double averageCartime = 0.0;
	// --- new parameters 08.11.18
	double metroPersonDist = 0.0;		// [m]
	// --- new parameters 10.11.18
	double totalTravelTime = 0.0;
	
	public CostBenefitParameters() {
	}
	
	public CostBenefitParameters( double ptUsers, double carUsers, double otherUsers,
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
	
	public CostBenefitParameters( double ptUsers, double carUsers, double otherUsers,
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
	
	public CostBenefitParameters( double ptUsers, double carUsers, double otherUsers,
			double carTimeTotal, double carPersonDist, double ptTimeTotal, double ptPersonDist, double metroPersonDist, double totalTravelTime) {
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

	public void calculateAverages() {
//		this.averageCartime = this.carTimeTotal/this.carUsers;
//		this.averagePtTime = this.ptTimeTotal/this.ptUsers;		
	}
	
}
