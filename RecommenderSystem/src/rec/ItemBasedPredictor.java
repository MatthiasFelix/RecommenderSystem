package rec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This class implements the item-based collaborative filtering algorithm
 * 
 */
public class ItemBasedPredictor extends Predictor {

	private Data data;
	private String neighbourhood, sMetric, pMetric;
	private int size;
	private double threshold;

	// hash map: movieID --> hash map: movieID --> similarity
	private HashMap<Integer, LinkedHashMap<Integer, Double>> movieSimilarities;

	// Depending on which constructor is used, the algorithm runs with either a
	// neighbourhood size or a threshold
	public ItemBasedPredictor(int neighbourhoodSize, String sMetric,
			String pMetric, Data d) {
		this.data = d;
		this.neighbourhood = "size";
		this.size = neighbourhoodSize;
		this.sMetric = sMetric;
		this.pMetric = pMetric;

		movieSimilarities = new HashMap<Integer, LinkedHashMap<Integer, Double>>();

	}

	public ItemBasedPredictor(double threshold, String sMetric, String pMetric,
			Data d) {
		this.data = d;
		this.neighbourhood = "threshold";
		this.threshold = threshold;
		this.sMetric = sMetric;
		this.pMetric = pMetric;

		movieSimilarities = new HashMap<Integer, LinkedHashMap<Integer, Double>>();

	}

	/**
	 * This method trains the predictor using the data given in the input array
	 * 'data'
	 * 
	 * @param data
	 *            : an array of triples (user, movie, rating)
	 */
	@Override
	public void train() {

		// Compute all similarities between items

		double sim = 0;
		for (Integer movie1 : data.getUsersByMovie().keySet()) {
			for (Integer movie2 : data.getUsersByMovie().tailMap(movie1, false)
					.keySet()) {

				sim = computeSimilarity(movie1, movie2);

				// If threshold is used, only put similarities above the
				// threshold into the hash map
				if (neighbourhood.equals("threshold")) {
					if (sim < threshold) {
						continue;
					}

					// this is for movie1's list
					if (movieSimilarities.containsKey(movie1)) {
						LinkedHashMap<Integer, Double> s = movieSimilarities
								.get(movie1);
						s.put(movie2, sim);
					} else {
						LinkedHashMap<Integer, Double> s = new LinkedHashMap<Integer, Double>();
						s.put(movie2, sim);
						movieSimilarities.put(movie1, s);
					}

					// this is for movie2's list
					if (movieSimilarities.containsKey(movie2)) {
						HashMap<Integer, Double> s = movieSimilarities
								.get(movie2);
						s.put(movie1, sim);
					} else {
						LinkedHashMap<Integer, Double> s = new LinkedHashMap<Integer, Double>();
						s.put(movie1, sim);
						movieSimilarities.put(movie2, s);
					}

				} else if (neighbourhood.equals("size")) {
					// Add similarity to neighbourhood only if it's big enough

					// movie1's list
					if (movieSimilarities.containsKey(movie1)) {
						if (movieSimilarities.get(movie1).size() < size) {
							LinkedHashMap<Integer, Double> s = movieSimilarities
									.get(movie1);
							s.put(movie2, sim);
						} else {
							int userToReplace = 0;
							for (Map.Entry<Integer, Double> entry : movieSimilarities
									.get(movie1).entrySet()) {
								if (sim > entry.getValue()) {
									userToReplace = entry.getKey();
								}
							}
							if (userToReplace != 0) {
								movieSimilarities.get(movie1).remove(
										userToReplace);
								movieSimilarities.get(movie1).put(movie2, sim);
							}
						}
					} else {
						LinkedHashMap<Integer, Double> s = new LinkedHashMap<Integer, Double>();
						s.put(movie2, sim);
						movieSimilarities.put(movie1, s);
					}

					// User2's list
					if (movieSimilarities.containsKey(movie2)) {
						if (movieSimilarities.get(movie2).size() < size) {
							LinkedHashMap<Integer, Double> s = movieSimilarities
									.get(movie2);
							s.put(movie1, sim);
						} else {
							int userToReplace = 0;
							for (Map.Entry<Integer, Double> entry : movieSimilarities
									.get(movie2).entrySet()) {
								if (sim > entry.getValue()) {
									userToReplace = entry.getKey();
								}
							}
							movieSimilarities.get(movie2).remove(userToReplace);
							movieSimilarities.get(movie2).put(movie1, sim);
						}
					} else {
						LinkedHashMap<Integer, Double> s = new LinkedHashMap<Integer, Double>();
						s.put(movie1, sim);
						movieSimilarities.put(movie2, s);
					}
				}

			}
		}

		// Calculate the average size of the similarity lists
		int similaritiesCount = 0;
		for (Map.Entry<Integer, LinkedHashMap<Integer, Double>> entry : movieSimilarities
				.entrySet()) {
			similaritiesCount += entry.getValue().size();
		}
		Recommender
				.setAverageSizeOfSimilarityListMovies((double) similaritiesCount
						/ (double) movieSimilarities.size());

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

		if (data.getAverageUserRatings().get(userID) != null) {
			prediction = data.getAverageUserRatings().get(userID);
		}

		ArrayList<Double> similaritiesList = new ArrayList<Double>();
		ArrayList<Double> ratingsList = new ArrayList<Double>();
		ArrayList<Double> averageList = new ArrayList<Double>();

		// If movie has not been rated yet, return the user's average rating
		// over all movies
		if (movieSimilarities.get(movieID) == null) {
			return prediction;
		}

		for (Map.Entry<Integer, Double> movie : movieSimilarities.get(movieID)
				.entrySet()) {

			if (data.getUserMovieRatings().get(userID).get(movie.getKey()) != null) {
				similaritiesList.add(movie.getValue());
				ratingsList.add(data.getUserMovieRatings().get(userID)
						.get(movie.getKey()));
				averageList.add(data.getAverageMovieRatings().get(
						movie.getKey()));

			}
		}

		if (pMetric.equals("weighted")) {
			prediction = Prediction.calculateWeightedSum(similaritiesList,
					ratingsList);
		} else if (pMetric.equals("adjusted")) {
			prediction = Prediction.calculateAdjustedSum(data
					.getAverageMovieRatings().get(movieID), averageList,
					ratingsList);
		} else if (pMetric.equals("adjweighted")) {
			prediction = Prediction.calculateAdjustedWeightedSum(data
					.getAverageMovieRatings().get(movieID), averageList,
					ratingsList, similaritiesList);
		}

		if (prediction == 0) {
			return data.getAverageMovieRatings().get(movieID);
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
		for (Integer user : data.getUsersByMovie().get(movie1)) {
			if (data.getUsersByMovie().get(movie2).contains(user)) {
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
			movie1Ratings[i] = data.getUserMovieRatings().get(user).get(movie1);
			movie2Ratings[i] = data.getUserMovieRatings().get(user).get(movie2);
			i++;
		}

		// Calculate the similarity using the defined metric
		if (sMetric.equals("cosine")) {
			sim = Similarity.calculateCosineSimilarity(movie1Ratings,
					movie2Ratings);
		} else if (sMetric.equals("pearson")) {
			sim = Similarity.calculatePearsonCorrelation(movie1Ratings,
					movie2Ratings, data.getAverageMovieRatings().get(movie1),
					data.getAverageMovieRatings().get(movie2));
		}

		return sim;
	}

}
