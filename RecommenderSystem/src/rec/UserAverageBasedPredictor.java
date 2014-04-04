package rec;

import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;

/**
 * This class implements a predictor that for a given user always predicts his
 * average rating (over all ratings that user has submitted)
 * 
 */
public class UserAverageBasedPredictor extends Predictor {

	// average ratings by every user
	private HashMap<Integer, Double> averageRatings;

	// hash map: userID --> Set of movies that the user has rated
	private HashMap<Integer, HashSet<Integer>> UserRatedMovies;

	// hashmap: userID --> hashmap: movieID --> rating
	// for every user a hashmap of movie ratings
	private TreeMap<Integer, HashMap<Integer, Double>> UserMovieRatings;

	private double average = 0;

	public UserAverageBasedPredictor() {
		UserRatedMovies = new HashMap<Integer, HashSet<Integer>>();
		UserMovieRatings = new TreeMap<Integer, HashMap<Integer, Double>>();
		averageRatings = new HashMap<Integer, Double>();
	}

	@Override
	/**
	 * This method trains the predictor using the data given in the input array 'data'
	 * @param data: an array of triples (user, movie, rating)
	 */
	public void train(double[][] data) {

		initializeUserMovieRatings(data);
		initializeUserRatedMovies(data);

		computeUserAverageRatings(data);
		computeAverage();

	}

	@Override
	/**
	 * This method computes the predicted rating of user 'userID' for the movie 'movieID'
	 * @param userID: the id of the user (as given in the ml100 data file)
	 * @param movieID: the id of the movie (as given in the ml100 data file)
	 * @return: predicted rating
	 */
	public double predict(int userID, int movieID) {
		if (averageRatings.get(userID) != null) {
			return averageRatings.get(userID);
		} else {
			return average;
		}
	}

	/**
	 * Populate the hash map UserRatedMovies with the data from the input file
	 * 
	 * @param data
	 *            : the input data file
	 */
	public void initializeUserRatedMovies(double[][] data) {
		for (int i = 0; i < data.length; i++) {
			// user already in the hash map
			if (UserRatedMovies.containsKey(data[i][0])) {
				HashSet<Integer> movies = UserRatedMovies.get(data[i][0]);
				movies.add((int) data[i][1]);
			} else {
				HashSet<Integer> movies = new HashSet<Integer>();
				movies.add((int) data[i][1]);
				UserRatedMovies.put((int) data[i][0], movies);
			}

		}
	}

	/**
	 * Populate the hash set UserMovieRatings with the data from the input file
	 * 
	 * @param data
	 *            : the input data file
	 */
	public void initializeUserMovieRatings(double[][] data) {
		for (int i = 0; i < data.length; i++) {
			if (UserMovieRatings.containsKey((int) data[i][0])) {
				UserMovieRatings.get((int) data[i][0]).put((int) data[i][1],
						data[i][2]);
			} else {
				HashMap<Integer, Double> ratings = new HashMap<Integer, Double>();
				ratings.put((int) data[i][1], data[i][2]);
				UserMovieRatings.put((int) data[i][0], ratings);
			}
		}
	}

	/**
	 * Compute the average ratings for all users
	 * 
	 * @param data
	 *            : input data
	 */
	public void computeUserAverageRatings(double[][] data) {

		for (Integer userID : UserRatedMovies.keySet()) {
			double rating = 0;
			for (Integer movieID : UserRatedMovies.get(userID)) {
				rating += UserMovieRatings.get(userID).get(movieID);
			}
			rating /= (double) UserRatedMovies.get(userID).size();
			averageRatings.put(userID, rating);
		}
	}

	public void computeAverage() {
		for (Double avg : averageRatings.values()) {
			average += avg;
		}
		average /= averageRatings.size();
	}

	public HashMap<Integer, HashSet<Integer>> getUserRatedMovies() {
		return UserRatedMovies;
	}

	public TreeMap<Integer, HashMap<Integer, Double>> getUserMovieRatings() {
		return UserMovieRatings;
	}

	public HashMap<Integer, Double> getAverageRatings() {
		return averageRatings;
	}

}
