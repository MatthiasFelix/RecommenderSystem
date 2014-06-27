package predictors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import rec.Data;
import rec.Prediction;
import rec.Recommender;
import rec.Similarity;

/**
 * This class implements the item-based collaborative filtering algorithm
 * 
 */
public class ItemBasedPredictor extends Predictor {

	private Data data;
	private String neighbourhood, sMetric, pMetric;
	private int size;
	private double threshold;

	// hash map: itemID --> hash map: itemID --> similarity
	private HashMap<Integer, LinkedHashMap<Integer, Double>> itemSimilarities;

	// Depending on which constructor is used, the algorithm runs with either a
	// neighbourhood size or a threshold
	public ItemBasedPredictor(int neighbourhoodSize, String sMetric,
			String pMetric, Data d) {
		this.data = d;
		this.neighbourhood = "size";
		this.size = neighbourhoodSize;
		this.sMetric = sMetric;
		this.pMetric = pMetric;

		itemSimilarities = new HashMap<Integer, LinkedHashMap<Integer, Double>>();

	}

	public ItemBasedPredictor(double threshold, String sMetric, String pMetric,
			Data d) {
		this.data = d;
		this.neighbourhood = "threshold";
		this.threshold = threshold;
		this.sMetric = sMetric;
		this.pMetric = pMetric;

		itemSimilarities = new HashMap<Integer, LinkedHashMap<Integer, Double>>();

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

		// Compute all similarities between items

		double sim = 0;
		for (Integer item1 : data.getUsersByItem().keySet()) {
			for (Integer item2 : data.getUsersByItem().tailMap(item1, false)
					.keySet()) {

				sim = computeSimilarity(item1, item2);

				// If threshold is used, only put similarities above the
				// threshold into the hash map
				if (neighbourhood.equals("threshold")) {
					if (sim < threshold) {
						continue;
					}

					// this is for item1's list
					if (itemSimilarities.containsKey(item1)) {
						LinkedHashMap<Integer, Double> s = itemSimilarities
								.get(item1);
						s.put(item2, sim);
					} else {
						LinkedHashMap<Integer, Double> s = new LinkedHashMap<Integer, Double>();
						s.put(item2, sim);
						itemSimilarities.put(item1, s);
					}

					// this is for item2's list
					if (itemSimilarities.containsKey(item2)) {
						HashMap<Integer, Double> s = itemSimilarities
								.get(item2);
						s.put(item1, sim);
					} else {
						LinkedHashMap<Integer, Double> s = new LinkedHashMap<Integer, Double>();
						s.put(item1, sim);
						itemSimilarities.put(item2, s);
					}

				} else if (neighbourhood.equals("size")) {
					// Add similarity to neighbourhood only if it's big enough

					// item1's list
					if (itemSimilarities.containsKey(item1)) {
						if (itemSimilarities.get(item1).size() < size) {
							LinkedHashMap<Integer, Double> s = itemSimilarities
									.get(item1);
							s.put(item2, sim);
						} else {
							int userToReplace = 0;
							for (Map.Entry<Integer, Double> entry : itemSimilarities
									.get(item1).entrySet()) {
								if (sim > entry.getValue()) {
									userToReplace = entry.getKey();
								}
							}
							if (userToReplace != 0) {
								itemSimilarities.get(item1).remove(
										userToReplace);
								itemSimilarities.get(item1).put(item2, sim);
							}
						}
					} else {
						LinkedHashMap<Integer, Double> s = new LinkedHashMap<Integer, Double>();
						s.put(item2, sim);
						itemSimilarities.put(item1, s);
					}

					// item2's list
					if (itemSimilarities.containsKey(item2)) {
						if (itemSimilarities.get(item2).size() < size) {
							LinkedHashMap<Integer, Double> s = itemSimilarities
									.get(item2);
							s.put(item1, sim);
						} else {
							int userToReplace = 0;
							for (Map.Entry<Integer, Double> entry : itemSimilarities
									.get(item2).entrySet()) {
								if (sim > entry.getValue()) {
									userToReplace = entry.getKey();
								}
							}
							itemSimilarities.get(item2).remove(userToReplace);
							itemSimilarities.get(item2).put(item1, sim);
						}
					} else {
						LinkedHashMap<Integer, Double> s = new LinkedHashMap<Integer, Double>();
						s.put(item1, sim);
						itemSimilarities.put(item2, s);
					}
				}

			}
		}

		// Calculate the average size of the similarity lists
		int similaritiesCount = 0;
		for (Map.Entry<Integer, LinkedHashMap<Integer, Double>> entry : itemSimilarities
				.entrySet()) {
			similaritiesCount += entry.getValue().size();
		}
		Recommender
				.setAverageSizeOfSimilarityListItems((double) similaritiesCount
						/ (double) itemSimilarities.size());

	}

	/**
	 * This method computes the predicted rating of user 'userID' for the item
	 * 'itemID' using the weighted-sum prediction
	 * 
	 * @param userID
	 *            : the id of the user (as given in the ml100 data file)
	 * @param itemID
	 *            : the id of the item (as given in the ml100 data file)
	 * @return: predicted rating
	 */
	@Override
	public double predict(int userID, int itemID) {

		double prediction = 0;

		if (data.getAverageUserRatings().get(userID) != null) {
			prediction = data.getAverageUserRatings().get(userID);
		}

		ArrayList<Double> similaritiesList = new ArrayList<Double>();
		ArrayList<Double> ratingsList = new ArrayList<Double>();
		ArrayList<Double> averageList = new ArrayList<Double>();

		// If item has not been rated yet, return the user's average rating
		// over all items
		if (itemSimilarities.get(itemID) == null) {
			return prediction;
		}

		for (Map.Entry<Integer, Double> item : itemSimilarities.get(itemID)
				.entrySet()) {

			if (data.getUserItemRatings().get(userID).get(item.getKey()) != null) {
				similaritiesList.add(item.getValue());
				ratingsList.add(data.getUserItemRatings().get(userID)
						.get(item.getKey()));
				averageList.add(data.getAverageItemRatings()
						.get(item.getKey()));

			}
		}

		if (pMetric.equals("weighted")) {
			prediction = Prediction.calculateWeightedSum(similaritiesList,
					ratingsList);
		} else if (pMetric.equals("adjusted")) {
			prediction = Prediction.calculateAdjustedSum(data
					.getAverageItemRatings().get(itemID), averageList,
					ratingsList);
		} else if (pMetric.equals("adjweighted")) {
			prediction = Prediction.calculateAdjustedWeightedSum(data
					.getAverageItemRatings().get(itemID), averageList,
					ratingsList, similaritiesList);
		}

		if (prediction == 0) {
			return data.getAverageItemRatings().get(itemID);
		}

		return prediction;
	}

	/**
	 * Computes the similarity between item1 and item2 using the adjusted
	 * cosine similarity
	 * 
	 * @param item1
	 * @param item2
	 * @return similarity
	 */
	public double computeSimilarity(int item1, int item2) {

		double sim = 0;

		ArrayList<Integer> sharedUsers = new ArrayList<Integer>();
		for (Integer user : data.getUsersByItem().get(item1)) {
			if (data.getUsersByItem().get(item2).contains(user)) {
				sharedUsers.add(user);
			}
		}

		if (sharedUsers.isEmpty()) {
			return sim;
		}

		double[] item1Ratings = new double[sharedUsers.size()];
		double[] item2Ratings = new double[sharedUsers.size()];

		int i = 0;
		for (Integer user : sharedUsers) {
			item1Ratings[i] = data.getUserItemRatings().get(user).get(item1);
			item2Ratings[i] = data.getUserItemRatings().get(user).get(item2);
			i++;
		}

		// Calculate the similarity using the defined metric
		if (sMetric.equals("cosine")) {
			sim = Similarity.calculateCosineSimilarity(item1Ratings,
					item2Ratings);
		} else if (sMetric.equals("pearson")) {
			sim = Similarity.calculatePearsonCorrelation(item1Ratings,
					item2Ratings, data.getAverageItemRatings().get(item1),
					data.getAverageItemRatings().get(item2));
		}

		return sim;
	}

}
