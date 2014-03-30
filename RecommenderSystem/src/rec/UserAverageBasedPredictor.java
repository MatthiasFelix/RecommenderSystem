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
	private TreeMap<Integer, HashMap<Integer, Integer>> UserMovieRatings;

	public UserAverageBasedPredictor() {
		UserRatedMovies = new HashMap<Integer, HashSet<Integer>>();
		UserMovieRatings = new TreeMap<Integer, HashMap<Integer, Integer>>();
		averageRatings = new HashMap<Integer, Double>();
	}

	@Override
	/**
	 * This method trains the predictor using the data given in the input array 'data'
	 * @param data: an array of triples (user, movie, rating)
	 */
	public void train(int[][] data) {

		initializeUserMovieRatings(data);
		initializeUserRatedMovies(data);

		computeUserAverageRatings(data);

	}

	@Override
	/**
	 * This method computes the predicted rating of user 'userID' for the movie 'movieID'
	 * @param userID: the id of the user (as given in the ml100 data file)
	 * @param movieID: the id of the movie (as given in the ml100 data file)
	 * @return: predicted rating
	 */
	public double predict(int userID, int movieID) {
		return averageRatings.get(userID);
	}

	/**
	 * Populate the hash map UserRatedMovies with the data from the input file
	 * 
	 * @param data
	 *            : the input data file
	 */
	public void initializeUserRatedMovies(int[][] data) {
		for (int i = 0; i < data.length; i++) {
			// user already in the hash map
			if (UserRatedMovies.containsKey(data[i][0])) {
				HashSet<Integer> movies = UserRatedMovies.get(data[i][0]);
				movies.add(data[i][1]);
			} else {
				HashSet<Integer> movies = new HashSet<Integer>();
				movies.add(data[i][1]);
				UserRatedMovies.put(data[i][0], movies);
			}

		}
	}

	/**
	 * Populate the hash set UserMovieRatings with the data from the input file
	 * 
	 * @param data
	 *            : the input data file
	 */
	public void initializeUserMovieRatings(int[][] data) {
		for (int i = 0; i < data.length; i++) {
			if (UserMovieRatings.containsKey(data[i][0])) {
				UserMovieRatings.get(data[i][0]).put(data[i][1], data[i][2]);
			} else {
				HashMap<Integer, Integer> ratings = new HashMap<Integer, Integer>();
				ratings.put(data[i][1], data[i][2]);
				UserMovieRatings.put(data[i][0], ratings);
			}
		}
	}

	/**
	 * Compute the average ratings for all users
	 * 
	 * @param data
	 *            : input data
	 */
	public void computeUserAverageRatings(int[][] data) {

		for (Integer userID : UserRatedMovies.keySet()) {
			double rating = 0;
			for (Integer movieID : UserRatedMovies.get(userID)) {
				rating += UserMovieRatings.get(userID).get(movieID);
			}
			rating /= (double) UserRatedMovies.get(userID).size();
			averageRatings.put(userID, rating);
		}
	}

	public HashMap<Integer, HashSet<Integer>> getUserRatedMovies() {
		return UserRatedMovies;
	}

	public TreeMap<Integer, HashMap<Integer, Integer>> getUserMovieRatings() {
		return UserMovieRatings;
	}

	public HashMap<Integer, Double> getAverageRatings() {
		return averageRatings;
	}

}
