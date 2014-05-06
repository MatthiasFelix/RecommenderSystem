package generator;

public class User {

	// Each user gets a unique ID, starting from 0
	private static int ID = 0;

	private int userID;

	public User() {
		userID = ID++;
	}

	public int getID() {
		return userID;
	}

}
