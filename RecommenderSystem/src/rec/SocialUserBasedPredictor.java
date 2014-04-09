package rec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class SocialUserBasedPredictor extends Predictor {

	private Data data;
	private String neighbourhood, sMetric, pMetric, socialNeighbourhood;
	private int size;
	private double threshold;

	// hash map: userID --> hash map: userID --> similarity
	private HashMap<Integer, LinkedHashMap<Integer, Double>> userSimilarities;

	// Depending on which constructor is used, the algorithm runs with either a
	// neighbourhood size or a threshold
	public SocialUserBasedPredictor(int neighbourhoodSize, String sMetric,
			String pMetric, String socialNeighbourhood, Data d) {
		this.data = d;
		this.neighbourhood = "size";
		this.size = neighbourhoodSize;
		this.sMetric = sMetric;
		this.pMetric = pMetric;
		this.socialNeighbourhood = socialNeighbourhood;

		userSimilarities = new HashMap<Integer, LinkedHashMap<Integer, Double>>();

	}

	public SocialUserBasedPredictor(double threshold, String sMetric,
			String pMetric, String socialNeighbourhood, Data d) {
		this.data = d;
		this.neighbourhood = "threshold";
		this.threshold = threshold;
		this.sMetric = sMetric;
		this.pMetric = pMetric;
		this.socialNeighbourhood = socialNeighbourhood;

		userSimilarities = new HashMap<Integer, LinkedHashMap<Integer, Double>>();

	}

	@Override
	public void train() {

		// TODO Compute similarities, use different neighbourhoods

	}

	@Override
	public double predict(int userID, int movieID) {

		// If the user hasn't rated any movie yet, return 0
		if (!data.getUserMovieRatings().keySet().contains(userID)) {
			return 0;
		}

		double prediction = data.getAverageUserRatings().get(userID);

		ArrayList<Double> ratingsList = new ArrayList<Double>();
		ArrayList<Double> averageList = new ArrayList<Double>();

		if (socialNeighbourhood.equals("all")) {

			for (Integer friend : data.getUserFriends().get(userID)) {
				if (data.getUserMovieRatings().get(friend) != null
						&& data.getUserMovieRatings().get(friend).get(movieID) != null) {
					ratingsList.add(data.getUserMovieRatings().get(friend)
							.get(movieID));
					averageList.add(data.getAverageUserRatings().get(friend));
				}
			}

			prediction = Prediction.calculateAdjustedSum(data
					.getAverageUserRatings().get(userID), averageList,
					ratingsList);

		} else if (socialNeighbourhood.equals("similar")) {
			// TODO use different socialneighbourhoods
		} else if (socialNeighbourhood.equals("friendsoffriends")) {

			ArrayList<Integer> friendsInList = new ArrayList<Integer>();

			for (Integer friend : data.getUserFriends().get(userID)) {
				if (!friendsInList.contains(friend)
						&& data.getUserMovieRatings().get(friend) != null
						&& data.getUserMovieRatings().get(friend).get(movieID) != null) {
					ratingsList.add(data.getUserMovieRatings().get(friend)
							.get(movieID));
					averageList.add(data.getAverageUserRatings().get(friend));
					friendsInList.add(friend);
				}
				for (Integer friendsFriend : data.getUserFriends().get(friend)) {
					if (!friendsInList.contains(friendsFriend)
							&& data.getUserMovieRatings().get(friendsFriend) != null
							&& data.getUserMovieRatings().get(friendsFriend)
									.get(movieID) != null) {
						ratingsList.add(data.getUserMovieRatings()
								.get(friendsFriend).get(movieID));
						averageList.add(data.getAverageUserRatings().get(
								friendsFriend));
						friendsInList.add(friendsFriend);
					}
				}
			}

			// TODO use different prediction measures
			prediction = Prediction.calculateAdjustedSum(data
					.getAverageUserRatings().get(userID), averageList,
					ratingsList);

		}

		if (prediction == 0) {
			// System.out.println("Returning average");
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

		// Create list with all movies rated by both user1 and user2
		ArrayList<Integer> sharedMovies = new ArrayList<Integer>();
		for (Integer movie : data.getMoviesByUser().get(user1)) {
			if (data.getMoviesByUser().get(user2).contains(movie)) {
				sharedMovies.add(movie);
			}
		}

		// Create the rating vectors for both users
		double[] ratingsUser1 = new double[sharedMovies.size()];
		double[] ratingsUser2 = new double[sharedMovies.size()];

		int i = 0;
		for (Integer movie : sharedMovies) {
			ratingsUser1[i] = data.getUserMovieRatings().get(user1).get(movie);
			ratingsUser2[i] = data.getUserMovieRatings().get(user2).get(movie);
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
