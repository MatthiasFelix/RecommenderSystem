package rec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.TreeMap;

public class SocialUserBasedPredictor extends Predictor {

	private String sMetric, pMetric;
	private String socialNeighbourhood;

	// hash map: userID --> average rating the user has given
	private HashMap<Integer, Double> averageRatings;

	// hash map: userID --> hash map: userID --> similarity
	private HashMap<Integer, LinkedHashMap<Integer, Double>> similarities;

	// hash map: userID --> Set of movies that the user has rated
	// note that this map is sorted (==> has always the same iteration order)
	private TreeMap<Integer, HashSet<Integer>> userRatedMovies;

	/*
	 * hash map: userID --> hash map: movieID --> rating. For every user a hash
	 * map of movie ratings note that this map is sorted (==> has always the
	 * same iteration order)
	 */
	private TreeMap<Integer, HashMap<Integer, Double>> userMovieRatings;

	// hash map: userID --> Set of users that are friends of this user
	private TreeMap<Integer, HashSet<Integer>> userFriends;

	public SocialUserBasedPredictor(int[][] userFriends, String sMetric,
			String pMetric, String socialNeighbourhood) {
		this.sMetric = sMetric;
		this.pMetric = pMetric;
		this.socialNeighbourhood = socialNeighbourhood;
		this.userFriends = initializeUserFrinends(userFriends);

		userRatedMovies = new TreeMap<Integer, HashSet<Integer>>();
		userMovieRatings = new TreeMap<Integer, HashMap<Integer, Double>>();
		averageRatings = new HashMap<Integer, Double>();
		similarities = new HashMap<Integer, LinkedHashMap<Integer, Double>>();
	}

	@Override
	public void train(double[][] data) {

		initializeUserMovieRatings(data);
		initializeUserRatedMovies(data);

		computeUserAverageRatings(data);

		// Compute similarities

	}

	@Override
	public double predict(int userID, int movieID) {

		// If the user hasn't rated any movie yet, return 0
		if (!userMovieRatings.keySet().contains(userID)) {
			return 0;
		}

		double prediction = averageRatings.get(userID);

		ArrayList<Double> ratingsList = new ArrayList<Double>();
		ArrayList<Double> averageList = new ArrayList<Double>();

		if (socialNeighbourhood.equals("all")) {

			for (Integer friend : userFriends.get(userID)) {
				if (userMovieRatings.get(friend) != null
						&& userMovieRatings.get(friend).get(movieID) != null) {
					ratingsList.add(userMovieRatings.get(friend).get(movieID));
					averageList.add(averageRatings.get(friend));
				}
			}

			prediction = Prediction.calculateAdjustedSum(
					averageRatings.get(userID), averageList, ratingsList);

		} else if (socialNeighbourhood.equals("similar")) {
			// TODO
		} else if (socialNeighbourhood.equals("friendsoffriends")) {

			ArrayList<Integer> friendsInList = new ArrayList<Integer>();

			for (Integer friend : userFriends.get(userID)) {
				if (!friendsInList.contains(friend)
						&& userMovieRatings.get(friend) != null
						&& userMovieRatings.get(friend).get(movieID) != null) {
					ratingsList.add(userMovieRatings.get(friend).get(movieID));
					averageList.add(averageRatings.get(friend));
					friendsInList.add(friend);
				}
				for (Integer friendsFriend : userFriends.get(friend)) {
					if (!friendsInList.contains(friendsFriend)
							&& userMovieRatings.get(friendsFriend) != null
							&& userMovieRatings.get(friendsFriend).get(movieID) != null) {
						ratingsList.add(userMovieRatings.get(friendsFriend)
								.get(movieID));
						averageList.add(averageRatings.get(friendsFriend));
						friendsInList.add(friendsFriend);
					}
				}
			}

			prediction = Prediction.calculateAdjustedSum(
					averageRatings.get(userID), averageList, ratingsList);

		}

		if (prediction == 0) {
			// System.out.println("Returning average");
			return averageRatings.get(userID);
		}

		return prediction;

	}

	/**
	 * Computes the similarity between user1 and user2 using the specified
	 * similarity coefficient
	 * 
	 * @param user1
	 * @param user2
	 * @return similarity
	 */
	public double computeSimilarity(int user1, int user2) {

		double sim = 0;

		// Create list with all movies rated by both user1 and user2
		ArrayList<Integer> sharedMovies = new ArrayList<Integer>();
		for (Integer movie : userRatedMovies.get(user1)) {
			if (userRatedMovies.get(user2).contains(movie)) {
				sharedMovies.add(movie);
			}
		}

		// Create the rating vectors for both users
		double[] ratingsUser1 = new double[sharedMovies.size()];
		double[] ratingsUser2 = new double[sharedMovies.size()];

		int i = 0;
		for (Integer movie : sharedMovies) {
			ratingsUser1[i] = userMovieRatings.get(user1).get(movie);
			ratingsUser2[i] = userMovieRatings.get(user2).get(movie);
			i++;
		}

		if (sMetric.equals("cosine")) {
			sim = Similarity.calculateCosineSimilarity(ratingsUser1,
					ratingsUser2);
		} else if (sMetric.equals("pearson")) {
			sim = Similarity.calculatePearsonCorrelation(ratingsUser1,
					ratingsUser2, averageRatings.get(user1),
					averageRatings.get(user2));
		}

		return sim;
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
			if (userRatedMovies.containsKey((int) data[i][0])) {
				HashSet<Integer> movies = userRatedMovies.get((int) data[i][0]);
				movies.add((int) data[i][1]);
			} else {
				HashSet<Integer> movies = new HashSet<Integer>();
				movies.add((int) data[i][1]);
				userRatedMovies.put((int) data[i][0], movies);
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

	/**
	 * Compute the average ratings for all users
	 * 
	 * @param data
	 *            : input data
	 */
	public void computeUserAverageRatings(double[][] data) {

		for (Integer userID : userRatedMovies.keySet()) {
			double rating = 0;
			for (Integer movieID : userRatedMovies.get(userID)) {
				rating += userMovieRatings.get(userID).get(movieID);
			}
			rating /= (double) userRatedMovies.get(userID).size();
			averageRatings.put(userID, rating);
		}
	}

	public TreeMap<Integer, HashSet<Integer>> initializeUserFrinends(
			int[][] userFriends) {

		TreeMap<Integer, HashSet<Integer>> friends = new TreeMap<Integer, HashSet<Integer>>();

		for (int i = 0; i < userFriends.length; i++) {
			int userID = userFriends[i][0];
			if (friends.containsKey(userID)) {
				friends.get(userID).add(userFriends[i][1]);
			} else {
				HashSet<Integer> hs = new HashSet<Integer>();
				hs.add(userFriends[i][1]);
				friends.put(userFriends[i][0], hs);
			}
		}

		return friends;
	}

}
