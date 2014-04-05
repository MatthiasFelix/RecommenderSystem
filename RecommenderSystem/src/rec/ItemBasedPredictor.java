package rec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * This class implements the item-based collaborative filtering algorithm
 * 
 */
public class ItemBasedPredictor extends Predictor {

	private int neighbourhoodSize;
	private String sMetric, pMetric;
	private boolean threshold;

	// hash map: movieID --> average rating
	private HashMap<Integer, Double> averageMovieRatings;

	// hash map: userID --> average rating
	private HashMap<Integer, Double> averageUserRatings;

	// hash map: movieID --> hash map: movieID --> similarity
	private HashMap<Integer, LinkedHashMap<Integer, Double>> similarities;

	// hash map: userID --> Set of movies that the user has rated
	// note that this map is sorted (==> has always the same iteration order)
	private TreeMap<Integer, HashSet<Integer>> userRatedMovies;

	/*
	 * hash map: userID --> hash map: movieID --> rating for every user a hash
	 * map of movie ratings note that this map is sorted (==> has always the
	 * same iteration order)
	 */
	private TreeMap<Integer, HashMap<Integer, Double>> userMovieRatings;

	// hash map: movieID --> Set of users that have rated the movie
	private TreeMap<Integer, HashSet<Integer>> usersByMovie;

	public ItemBasedPredictor(int neighbourhoodSize, String sMetric,
			String pMetric, boolean threshold) {

		this.neighbourhoodSize = neighbourhoodSize;
		this.sMetric = sMetric;
		this.pMetric = pMetric;
		this.threshold = threshold;

		userRatedMovies = new TreeMap<Integer, HashSet<Integer>>();
		userMovieRatings = new TreeMap<Integer, HashMap<Integer, Double>>();
		averageMovieRatings = new HashMap<Integer, Double>();
		averageUserRatings = new HashMap<Integer, Double>();
		similarities = new HashMap<Integer, LinkedHashMap<Integer, Double>>();
		usersByMovie = new TreeMap<Integer, HashSet<Integer>>();

	}

	/**
	 * This method trains the predictor using the data given in the input array
	 * 'data'
	 * 
	 * @param data
	 *            : an array of triples (user, movie, rating)
	 */
	@Override
	public void train(double[][] data) {

		initializeUserMovieRatings(data);
		initializeUserRatedMovies(data);
		initializeUsersByMovie(data);

		computeAverageMovieRatings(data);
		computeAverageUserRatings(data);

		// Compute all similarities between items

		double sim = 0;
		for (Integer movie1 : usersByMovie.keySet()) {
			for (Integer movie2 : usersByMovie.tailMap(movie1, false).keySet()) {

				sim = computeSimilarity(movie1, movie2);

				// If threshold is used, only put similarities above the
				// threshold into the hash map
				if (threshold) {
					if (sim < Recommender.getThreshold()) {
						continue;
					}

					// this is for movie1's list
					if (similarities.containsKey(movie1)) {
						LinkedHashMap<Integer, Double> s = similarities
								.get(movie1);
						s.put(movie2, sim);
					} else {
						LinkedHashMap<Integer, Double> s = new LinkedHashMap<Integer, Double>();
						s.put(movie2, sim);
						similarities.put(movie1, s);
					}

					// this is for movie2's list
					if (similarities.containsKey(movie2)) {
						HashMap<Integer, Double> s = similarities.get(movie2);
						s.put(movie1, sim);
					} else {
						LinkedHashMap<Integer, Double> s = new LinkedHashMap<Integer, Double>();
						s.put(movie1, sim);
						similarities.put(movie2, s);
					}

				} else {
					// Add similarity to neighbourhood only if it's big enough

					// movie1's list
					if (similarities.containsKey(movie1)) {
						if (similarities.get(movie1).size() < neighbourhoodSize) {
							LinkedHashMap<Integer, Double> s = similarities
									.get(movie1);
							s.put(movie2, sim);
						} else {
							int userToReplace = 0;
							for (Map.Entry<Integer, Double> entry : similarities
									.get(movie1).entrySet()) {
								if (sim > entry.getValue()) {
									userToReplace = entry.getKey();
								}
							}
							if (userToReplace != 0) {
								similarities.get(movie1).remove(userToReplace);
								similarities.get(movie1).put(movie2, sim);
							}
						}
					} else {
						LinkedHashMap<Integer, Double> s = new LinkedHashMap<Integer, Double>();
						s.put(movie2, sim);
						similarities.put(movie1, s);
					}

					// User2's list
					if (similarities.containsKey(movie2)) {
						if (similarities.get(movie2).size() < neighbourhoodSize) {
							LinkedHashMap<Integer, Double> s = similarities
									.get(movie2);
							s.put(movie1, sim);
						} else {
							int userToReplace = 0;
							for (Map.Entry<Integer, Double> entry : similarities
									.get(movie2).entrySet()) {
								if (sim > entry.getValue()) {
									userToReplace = entry.getKey();
								}
							}
							similarities.get(movie2).remove(userToReplace);
							similarities.get(movie2).put(movie1, sim);
						}
					} else {
						LinkedHashMap<Integer, Double> s = new LinkedHashMap<Integer, Double>();
						s.put(movie1, sim);
						similarities.put(movie2, s);
					}
				}

			}
		}

		// Calculate the average size of the similarity lists
		int similaritiesCount = 0;
		for (Map.Entry<Integer, LinkedHashMap<Integer, Double>> entry : similarities
				.entrySet()) {
			similaritiesCount += entry.getValue().size();
		}
		Recommender
				.setAverageSizeOfSimilarityListMovies((double) similaritiesCount
						/ (double) similarities.size());

	}

	/**
	 * This method computes the predicted rating of user 'userID' for the movie
	 * 'movieID' using the weighted-sum prediction
	 * 
	 * @param userID
	 *            : the id of the user (as given in the ml100 data file)
	 * @param movieID
	 *            : the id of the movie (as given in the ml100 data file)
	 * @return: predicted rating
	 */
	@Override
	public double predict(int userID, int movieID) {

		double prediction = 0;

		if (averageUserRatings.get(userID) != null) {
			prediction = averageUserRatings.get(userID);
		}

		ArrayList<Double> similaritiesList = new ArrayList<Double>();
		ArrayList<Double> ratingsList = new ArrayList<Double>();
		ArrayList<Double> averageList = new ArrayList<Double>();

		// If movie has not been rated yet, return the user's average rating
		// over all movies
		if (similarities.get(movieID) == null) {
			return prediction;
		}

		for (Map.Entry<Integer, Double> movie : similarities.get(movieID)
				.entrySet()) {

			if (userMovieRatings.get(userID).get(movie.getKey()) != null) {
				similaritiesList.add(movie.getValue());
				ratingsList.add(userMovieRatings.get(userID)
						.get(movie.getKey()));
				averageList.add(averageMovieRatings.get(movie.getKey()));

			}
		}

		if (pMetric.equals("weightedsum")) {
			prediction = Prediction.calculateWeightedSum(similaritiesList,
					ratingsList);
		} else if (pMetric.equals("adjustedsum")) {
			prediction = Prediction.calculateAdjustedSum(
					averageMovieRatings.get(movieID), averageList, ratingsList);
		} else if (pMetric.equals("adjustedweightedsum")) {
			prediction = Prediction.calculateAdjustedWeightedSum(
					averageMovieRatings.get(movieID), averageList, ratingsList,
					similaritiesList);
		}

		if (prediction == 0) {
			return averageMovieRatings.get(movieID);
		}

		return prediction;
	}

	/**
	 * Computes the similarity between movie1 and movie2 using the adjusted
	 * cosine similarity
	 * 
	 * @param movie1
	 * @param movie2
	 * @return similarity
	 */
	public double computeSimilarity(int movie1, int movie2) {

		double sim = 0;

		ArrayList<Integer> sharedUsers = new ArrayList<Integer>();
		for (Integer user : usersByMovie.get(movie1)) {
			if (usersByMovie.get(movie2).contains(user)) {
				sharedUsers.add(user);
			}
		}

		if (sharedUsers.isEmpty()) {
			return sim;
		}

		double[] movie1Ratings = new double[sharedUsers.size()];
		double[] movie2Ratings = new double[sharedUsers.size()];

		int i = 0;
		for (Integer user : sharedUsers) {
			movie1Ratings[i] = userMovieRatings.get(user).get(movie1);
			movie2Ratings[i] = userMovieRatings.get(user).get(movie2);
			i++;
		}

		// Calculate the similarity using the defined metric
		if (sMetric.equals("cosine")) {
			sim = Similarity.calculateCosineSimilarity(movie1Ratings,
					movie2Ratings);
		} else if (sMetric.equals("pearson")) {
			sim = Similarity.calculatePearsonCorrelation(movie1Ratings,
					movie2Ratings, averageMovieRatings.get(movie1),
					averageMovieRatings.get(movie2));
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
	 * Populate the hash map MoviesByUser with the data from the input file
	 * 
	 * @param data
	 *            : input data file
	 */
	public void initializeUsersByMovie(double[][] data) {
		for (int i = 0; i < data.length; i++) {
			if (usersByMovie.containsKey((int) data[i][1])) {
				usersByMovie.get((int) data[i][1]).add((int) data[i][0]);
			} else {
				HashSet<Integer> raters = new HashSet<Integer>();
				raters.add((int) data[i][0]);
				usersByMovie.put((int) data[i][1], raters);
			}
		}
	}

	public void computeAverageMovieRatings(double[][] data) {

		for (Integer movieID : usersByMovie.keySet()) {
			double rating = 0;
			for (Integer userID : usersByMovie.get(movieID)) {
				rating += userMovieRatings.get(userID).get(movieID);
			}
			rating /= (double) usersByMovie.get(movieID).size();
			averageMovieRatings.put(movieID, rating);
		}
	}

	public void computeAverageUserRatings(double[][] data) {

		for (Integer userID : userRatedMovies.keySet()) {
			double rating = 0;
			for (Integer movieID : userRatedMovies.get(userID)) {
				rating += userMovieRatings.get(userID).get(movieID);
			}
			rating /= (double) userRatedMovies.get(userID).size();
			averageUserRatings.put(userID, rating);
		}
	}

	public HashMap<Integer, LinkedHashMap<Integer, Double>> getSimilarities() {
		return similarities;
	}

	public TreeMap<Integer, HashSet<Integer>> getUsersByMovie() {
		return usersByMovie;
	}

}
