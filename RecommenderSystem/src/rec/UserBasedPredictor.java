package rec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * This class implements the user-based collaborative filtering algorithm
 * 
 */
public class UserBasedPredictor extends Predictor {

	private int neighbourhoodSize;
	private String sMetric, pMetric;

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
	// needed to sort the neighbour lists by similarity
	private Comparator<Map.Entry<Integer, Double>> comparator;

	public UserBasedPredictor(int neighbourhoodSize, String sMetric,
			String pMetric) {

		this.neighbourhoodSize = neighbourhoodSize;
		this.sMetric = sMetric;
		this.pMetric = pMetric;

		userRatedMovies = new TreeMap<Integer, HashSet<Integer>>();
		userMovieRatings = new TreeMap<Integer, HashMap<Integer, Double>>();
		averageRatings = new HashMap<Integer, Double>();
		similarities = new HashMap<Integer, LinkedHashMap<Integer, Double>>();

		comparator = new Comparator<Map.Entry<Integer, Double>>() {
			public int compare(Map.Entry<Integer, Double> o1,
					Map.Entry<Integer, Double> o2) {
				return (o1.getValue()).compareTo(o2.getValue());
			}
		};
	}

	/**
	 * This method trains the predictor using the data given in the input array
	 * 'data'
	 * 
	 * @param data
	 *            : an array of triples (user, movie, rating)
	 */
	@Override
	public void train(int[][] data) {

		initializeUserMovieRatings(data);

		if (Recommender.isNormalize()) {
			DataManipulator.normalizeRatings(userMovieRatings);
		}

		initializeUserRatedMovies(data);

		computeUserAverageRatings(data);

		// Compute all pairs similarity between users and add them to the
		// hash map similarities

		double sim = 0;
		for (Integer user1 : userRatedMovies.keySet()) {
			for (Integer user2 : userRatedMovies.tailMap(user1, false).keySet()) {

				sim = computeSimilarity(user1, user2);

				// this is for user1's list
				if (similarities.containsKey(user1)) {
					LinkedHashMap<Integer, Double> s = similarities.get(user1);
					s.put(user2, sim);
				} else {
					LinkedHashMap<Integer, Double> s = new LinkedHashMap<Integer, Double>();
					s.put(user2, sim);
					similarities.put(user1, s);
				}

				// this is for user2's list
				if (similarities.containsKey(user2)) {
					HashMap<Integer, Double> s = similarities.get(user2);
					s.put(user1, sim);
				} else {
					LinkedHashMap<Integer, Double> s = new LinkedHashMap<Integer, Double>();
					s.put(user1, sim);
					similarities.put(user2, s);
				}
			}
		}

		// sort the similarities by decreasing similarity and take the N
		// most similar users
		for (Map.Entry<Integer, LinkedHashMap<Integer, Double>> entry : similarities
				.entrySet()) {
			ArrayList<Entry<Integer, Double>> list = new ArrayList<Entry<Integer, Double>>(
					entry.getValue().entrySet());

			Collections.sort(list, Collections.reverseOrder(comparator));

			// take N most similar users
			LinkedHashMap<Integer, Double> m = new LinkedHashMap<Integer, Double>();
			int count = 0;
			for (Entry<Integer, Double> e : list) {
				if (count == neighbourhoodSize)
					break;
				m.put(e.getKey(), e.getValue());
				count++;
			}
			// replace current map with the map with most similar users
			similarities.put(entry.getKey(), m);

		}

	}

	/**
	 * This method computes the predicted rating of user 'userID' for the movie
	 * 'movieID' using the adjusted weighted-sum prediction
	 * 
	 * @param userID
	 *            : the id of the user (as given in the ml100 data file)
	 * @param movieID
	 *            : the id of the movie (as given in the ml100 data file)
	 * @return: predicted rating
	 */
	@Override
	public double predict(int userID, int movieID) {

		if (!userMovieRatings.keySet().contains(userID)) {
			return 0;
		}

		double prediction = averageRatings.get(userID);

		ArrayList<Double> similaritiesList = new ArrayList<Double>();
		ArrayList<Double> ratingsList = new ArrayList<Double>();
		ArrayList<Double> averageList = new ArrayList<Double>();

		for (Map.Entry<Integer, Double> neighbour : similarities.get(userID)
				.entrySet()) {
			if (userMovieRatings.get(neighbour.getKey()).get(movieID) != null) {
				similaritiesList.add(neighbour.getValue());
				ratingsList.add(userMovieRatings.get(neighbour.getKey()).get(
						movieID));
				averageList.add(averageRatings.get(neighbour.getKey()));
			}
		}

		if (pMetric.equals("weightedsum")) {
			prediction = Prediction.calculateWeightedSum(similaritiesList,
					ratingsList);
		} else if (pMetric.equals("adjustedsum")) {
			prediction = Prediction.calculateAdjustedSum(
					averageRatings.get(userID), averageList, ratingsList);
		} else if (pMetric.equals("adjustedweightedsum")) {
			prediction = Prediction.calculateAdjustedWeightedSum(
					averageRatings.get(userID), averageList, ratingsList,
					similaritiesList);
		}

		if (prediction == 0) {
			return averageRatings.get(userID);
		}

		return prediction;

	}

	/**
	 * Computes the similarity between user1 and user2 using the Pearson
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
	public void initializeUserRatedMovies(int[][] data) {
		for (int i = 0; i < data.length; i++) {
			// user already in the hash map
			if (userRatedMovies.containsKey(data[i][0])) {
				HashSet<Integer> movies = userRatedMovies.get(data[i][0]);
				movies.add(data[i][1]);
			} else {
				HashSet<Integer> movies = new HashSet<Integer>();
				movies.add(data[i][1]);
				userRatedMovies.put(data[i][0], movies);
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
			if (userMovieRatings.containsKey(data[i][0])) {
				userMovieRatings.get(data[i][0]).put(data[i][1],
						(double) data[i][2]);
			} else {
				HashMap<Integer, Double> ratings = new HashMap<Integer, Double>();
				ratings.put(data[i][1], (double) data[i][2]);
				userMovieRatings.put(data[i][0], ratings);
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

		for (Integer userID : userRatedMovies.keySet()) {
			double rating = 0;
			for (Integer movieID : userRatedMovies.get(userID)) {
				rating += userMovieRatings.get(userID).get(movieID);
			}
			rating /= (double) userRatedMovies.get(userID).size();
			averageRatings.put(userID, rating);
		}
	}

	public HashMap<Integer, LinkedHashMap<Integer, Double>> getSimilarities() {
		return similarities;
	}

}