package ch.ethz.matsim.students.samark;

public class CostBenefitParameters {

	double ptUsers = 0.0;
	double carUsers = 0.0;
	double otherUsers = 0.0;
	double carTimeTotal = 0.0;
	double carPersonDist = 0.0;
	double ptTimeTotal = 0.0;
	double ptPersonDist = 0.0;
	
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
	}

	
	
}
