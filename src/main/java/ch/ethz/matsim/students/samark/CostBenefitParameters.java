package ch.ethz.matsim.students.samark;

public class CostBenefitParameters {

	double ptUsers = 0.0;
	double carUsers = 0.0;
	double otherUsers = 0.0;
	double carTimeTotal = 0.0;
	double carPersonKM = 0.0;
	double ptTimeTotal = 0.0;
	double ptPersonKM = 0.0;
	
	public CostBenefitParameters() {
	}
	
	public CostBenefitParameters( double ptUsers, double carUsers, double otherUsers,
			double carTimeTotal, double carPersonKM, double ptTimeTotal, double ptPersonKM) {
		this.ptUsers = ptUsers;
		this.carUsers = carUsers;
		this.otherUsers = otherUsers;
		this.carTimeTotal = carTimeTotal;
		this.carPersonKM = carPersonKM;
		this.ptTimeTotal = ptTimeTotal;
		this.ptPersonKM = ptPersonKM;
	}

	
	
}
