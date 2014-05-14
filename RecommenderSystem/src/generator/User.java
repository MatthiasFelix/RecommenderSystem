package generator;

public class User {

	// Each user gets a unique ID, starting from 0
	private static int ID = 0;

	private int activity;
	private double bias;
	private int userID;

	private int ratingCounter;

	public User(int activity, double bias) {
		this.activity = activity;
		this.bias = bias;
		userID = ID++;
		ratingCounter = 0;
	}

	public int getActivity() {
		return activity;
	}

	public double getBias() {
		return bias;
	}

	public int getID() {
		return userID;
	}

	public void rate() {
		ratingCounter++;
	}

	public boolean canStillRate() {
		return ratingCounter < activity;
	}

}
