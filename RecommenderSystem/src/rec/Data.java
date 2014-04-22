package rec;

import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;

public class Data {

	double[][] data = null;
	int[][] friends = null;

	// hash map: userID --> hash map: movieID --> rating
	private TreeMap<Integer, HashMap<Integer, Double>> userMovieRatings = null;

	// hash map: userID --> Set of movies that the user has rated
	private TreeMap<Integer, HashSet<Integer>> moviesByUser = null;

	// hash map: movieID --> Set of users that have rated the movie
	private TreeMap<Integer, HashSet<Integer>> usersByMovie = null;

	// hash map: userID --> average rating of that user
	private HashMap<Integer, Double> averageUserRatings = null;

	// hash map: movieID --> average rating of that movie
	private HashMap<Integer, Double> averageMovieRatings = null;

	// hash map: userID --> Set of users that are friends of this user
	private TreeMap<Integer, HashSet<Integer>> userFriends = null;

	// Data can be initialized either with or without social network data
	// (friend relations)
	public Data(double[][] inputData, int[][] inputFriends) {
		friends = inputFriends;
		data = inputData;
	}

	public Data(double[][] inputData) {
		data = inputData;
	}

	public void initializeUserMovieRatings() {
		userMovieRatings = new TreeMap<Integer, HashMap<Integer, Double>>();
		for (int i = 0; i < data.length; i++) {
			if (userMovieRatings.containsKey((int) data[i][0])) {
				userMovieRatings.get((int) data[i][0]).put((int) data[i][1],
						data[i][2]);
			} else {
				HashMap<Integer, Double> ratings = new HashMap<Integer, Double>();
				ratings.put((int) data[i][1], data[i][2]);
				userMovieRatings.put((int) data[i][0], ratings);
			}
		}
	}

	public void initializeMoviesByUser() {
		moviesByUser = new TreeMap<Integer, HashSet<Integer>>();
		for (int i = 0; i < data.length; i++) {
			if (moviesByUser.containsKey((int) data[i][0])) {
				moviesByUser.get((int) data[i][0]).add((int) data[i][1]);
			} else {
				HashSet<Integer> movies = new HashSet<Integer>();
				movies.add((int) data[i][1]);
				moviesByUser.put((int) data[i][0], movies);
			}

		}
	}

	public void initializeUsersByMovie() {
		usersByMovie = new TreeMap<Integer, HashSet<Integer>>();
		for (int i = 0; i < data.length; i++) {
			if (usersByMovie.containsKey((int) data[i][1])) {
				usersByMovie.get((int) data[i][1]).add((int) data[i][0]);
			} else {
				HashSet<Integer> users = new HashSet<Integer>();
				users.add((int) data[i][0]);
				usersByMovie.put((int) data[i][1], users);
			}
		}
	}

	public void initializeAverageUserRatings() {
		averageUserRatings = new HashMap<Integer, Double>();
		for (Integer userID : getMoviesByUser().keySet()) {
			double rating = 0;
			for (Integer movieID : getMoviesByUser().get(userID)) {
				rating += getUserMovieRatings().get(userID).get(movieID);
			}
			rating /= (double) getMoviesByUser().get(userID).size();
			averageUserRatings.put(userID, rating);
		}
	}

	public void initializeAverageMovieRatings() {
		averageMovieRatings = new HashMap<Integer, Double>();
		for (Integer movieID : getUsersByMovie().keySet()) {
			double rating = 0;
			for (Integer userID : getUsersByMovie().get(movieID)) {
				rating += getUserMovieRatings().get(userID).get(movieID);
			}
			rating /= (double) getUsersByMovie().get(movieID).size();
			averageMovieRatings.put(movieID, rating);
		}
	}

	public void initializeUserFriends() {
		userFriends = new TreeMap<Integer, HashSet<Integer>>();
		for (int i = 0; i < friends.length; i++) {
			if (userFriends.containsKey(friends[i][0])) {
				userFriends.get(friends[i][0]).add(friends[i][1]);
			} else {
				HashSet<Integer> hs = new HashSet<Integer>();
				hs.add(friends[i][1]);
				userFriends.put(friends[i][0], hs);
			}
			if (userFriends.containsKey(friends[i][1])) {
				userFriends.get(friends[i][1]).add(friends[i][0]);
			} else {
				HashSet<Integer> hs = new HashSet<Integer>();
				hs.add(friends[i][0]);
				userFriends.put(friends[i][1], hs);
			}
		}
	}

	// Getters

	public TreeMap<Integer, HashMap<Integer, Double>> getUserMovieRatings() {
		if (userMovieRatings == null)
			initializeUserMovieRatings();
		return userMovieRatings;
	}

	public TreeMap<Integer, HashSet<Integer>> getMoviesByUser() {
		if (moviesByUser == null)
			initializeMoviesByUser();
		return moviesByUser;
	}

	public TreeMap<Integer, HashSet<Integer>> getUsersByMovie() {
		if (usersByMovie == null)
			initializeUsersByMovie();
		return usersByMovie;
	}

	public HashMap<Integer, Double> getAverageUserRatings() {
		if (averageUserRatings == null)
			initializeAverageUserRatings();
		return averageUserRatings;
	}

	public HashMap<Integer, Double> getAverageMovieRatings() {
		if (averageMovieRatings == null)
			initializeAverageMovieRatings();
		return averageMovieRatings;
	}

	public TreeMap<Integer, HashSet<Integer>> getUserFriends() {
		if (userFriends == null)
			initializeUserFriends();
		return userFriends;
	}

}
