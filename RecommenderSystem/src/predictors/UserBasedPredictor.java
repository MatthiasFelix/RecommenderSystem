package predictors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import rec.Data;
import rec.Prediction;
import rec.Recommender;
import rec.Similarity;

/**
 * This class implements the user-based collaborative filtering algorithm
 * 
 */
public class UserBasedPredictor extends Predictor {

	private Data data;
	private String neighbourhood, sMetric, pMetric;
	private int size;
	private double threshold;

	// hash map: userID --> hash map: userID --> similarity
	private HashMap<Integer, LinkedHashMap<Integer, Double>> userSimilarities;

	// needed to sort the neighbour lists by similarity
	private Comparator<Map.Entry<Integer, Double>> comparator;

	// Depending on which constructor is used, the algorithm runs with either a
	// neighbourhood size or a threshold
	public UserBasedPredictor(int neighbourhoodSize, String sMetric,
			String pMetric, Data d) {
		this.data = d;
		this.neighbourhood = "size";
		this.size = neighbourhoodSize;
		this.sMetric = sMetric;
		this.pMetric = pMetric;

		userSimilarities = new HashMap<Integer, LinkedHashMap<Integer, Double>>();

		comparator = new Comparator<Map.Entry<Integer, Double>>() {
			public int compare(Map.Entry<Integer, Double> o1,
					Map.Entry<Integer, Double> o2) {
				return (o1.getValue()).compareTo(o2.getValue());
			}
		};

	}

	public UserBasedPredictor(double threshold, String sMetric, String pMetric,
			Data d) {
		this.data = d;
		this.neighbourhood = "threshold";
		this.threshold = threshold;
		this.sMetric = sMetric;
		this.pMetric = pMetric;

		userSimilarities = new HashMap<Integer, LinkedHashMap<Integer, Double>>();

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
	 *            : an array of triples (user, item, rating)
	 */
	@Override
	public void train() {

		// Compute all pairs similarity between users and add them to the
		// hash map similarities

		double sim = 0;

		for (Integer user1 : data.getItemsByUser().keySet()) {
			for (Integer user2 : data.getItemsByUser().tailMap(user1, false)
					.keySet()) {

				sim = computeSimilarity(user1, user2);

				// If threshold is used, only put similarities above the
				// threshold into the hash map
				if (neighbourhood.equals("threshold")) {
					if (sim < threshold) {
						continue;
					}
				}

				// this is for user1's list
				if (userSimilarities.containsKey(user1)) {
					userSimilarities.get(user1).put(user2, sim);
				} else {
					LinkedHashMap<Integer, Double> s = new LinkedHashMap<Integer, Double>();
					s.put(user2, sim);
					userSimilarities.put(user1, s);
				}

				// this is for user2's list
				if (userSimilarities.containsKey(user2)) {
					userSimilarities.get(user2).put(user1, sim);
				} else {
					LinkedHashMap<Integer, Double> s = new LinkedHashMap<Integer, Double>();
					s.put(user1, sim);
					userSimilarities.put(user2, s);
				}

			}
		}

		if (neighbourhood.equals("size")) {
			// sort the maps by decreasing similarity and take the N most
			// similar
			// users
			for (Map.Entry<Integer, LinkedHashMap<Integer, Double>> entry : userSimilarities
					.entrySet()) {
				ArrayList<Entry<Integer, Double>> list = new ArrayList<Entry<Integer, Double>>(
						entry.getValue().entrySet());

				Collections.sort(list, Collections.reverseOrder(comparator));

				// take N most similar users
				LinkedHashMap<Integer, Double> m = new LinkedHashMap<Integer, Double>();
				int count = 0;
				for (Entry<Integer, Double> e : list) {
					if (count == size)
						break;
					m.put(e.getKey(), e.getValue());
					count++;
				}
				// replace current map with the map with most similar users
				userSimilarities.put(entry.getKey(), m);

			}
		}

		// Calculate the average size of the similarity lists
		int similaritiesCount = 0;
		for (Map.Entry<Integer, LinkedHashMap<Integer, Double>> entry : userSimilarities
				.entrySet()) {
			similaritiesCount += entry.getValue().size();
		}
		Recommender
				.setAverageSizeOfSimilarityListUsers((double) similaritiesCount
						/ (double) userSimilarities.size());

	}

	/**
	 * This method computes the predicted rating of user 'userID' for the item
	 * 'itemID' using the adjusted weighted-sum prediction
	 * 
	 * @param userID
	 *            : the id of the user (as given in the ml100 data file)
	 * @param itemID
	 *            : the id of the item (as given in the ml100 data file)
	 * @return: predicted rating
	 */
	@Override
	public double predict(int userID, int itemID) {

		// If the user hasn't rated any item yet, return 0
		if (!data.getUserItemRatings().keySet().contains(userID)) {
			return 0;
		}

		double prediction = data.getAverageUserRatings().get(userID);

		// If there are no similarities stored, return the user's average
		if (userSimilarities.get(userID) == null) {
			return prediction;
		}

		ArrayList<Double> similaritiesList = new ArrayList<Double>();
		ArrayList<Double> ratingsList = new ArrayList<Double>();
		ArrayList<Double> averageList = new ArrayList<Double>();

		for (Map.Entry<Integer, Double> neighbour : userSimilarities
				.get(userID).entrySet()) {
			if (data.getUserItemRatings().get(neighbour.getKey()).get(itemID) != null) {
				similaritiesList.add(neighbour.getValue());
				ratingsList.add(data.getUserItemRatings()
						.get(neighbour.getKey()).get(itemID));
				averageList.add(data.getAverageUserRatings().get(
						neighbour.getKey()));
			}
		}

		if (pMetric.equals("weighted")) {
			prediction = Prediction.calculateWeightedSum(similaritiesList,
					ratingsList);
		} else if (pMetric.equals("adjusted")) {
			prediction = Prediction.calculateAdjustedSum(data
					.getAverageUserRatings().get(userID), averageList,
					ratingsList);
		} else if (pMetric.equals("adjweighted")) {
			prediction = Prediction.calculateAdjustedWeightedSum(data
					.getAverageUserRatings().get(userID), averageList,
					ratingsList, similaritiesList);
		}

		if (prediction == 0) {
			return data.getAverageUserRatings().get(userID);
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

		// Create list with all items rated by both user1 and user2
		ArrayList<Integer> sharedItems = new ArrayList<Integer>();
		for (Integer item : data.getItemsByUser().get(user1)) {
			if (data.getItemsByUser().get(user2).contains(item)) {
				sharedItems.add(item);
			}
		}

		// Create the rating vectors for both users
		double[] ratingsUser1 = new double[sharedItems.size()];
		double[] ratingsUser2 = new double[sharedItems.size()];

		int i = 0;
		for (Integer item : sharedItems) {
			ratingsUser1[i] = data.getUserItemRatings().get(user1).get(item);
			ratingsUser2[i] = data.getUserItemRatings().get(user2).get(item);
			i++;
		}

		if (sMetric.equals("cosine")) {
			sim = Similarity.calculateCosineSimilarity(ratingsUser1,
					ratingsUser2);
		} else if (sMetric.equals("pearson")) {
			sim = Similarity.calculatePearsonCorrelation(ratingsUser1,
					ratingsUser2, data.getAverageUserRatings().get(user1), data
							.getAverageUserRatings().get(user2));
		}

		return sim;
	}

}