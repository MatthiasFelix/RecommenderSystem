package rec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

public class SocialUserBasedPredictor extends Predictor {

	private Data data;
	private String sMetric, pMetric, socialNeighbourhood;
	private Double socialThreshold = null;

	// hash map: userID --> hash map: userID --> similarity
	private HashMap<Integer, LinkedHashMap<Integer, Double>> userSimilarities;

	public SocialUserBasedPredictor(String sMetric, String pMetric,
			String socialNeighbourhood, Data d) {
		this.data = d;
		this.sMetric = sMetric;
		this.pMetric = pMetric;
		this.socialNeighbourhood = socialNeighbourhood;

		userSimilarities = new HashMap<Integer, LinkedHashMap<Integer, Double>>();

	}

	public SocialUserBasedPredictor(String sMetric, String pMetric,
			String socialNeighbourhood, Double socialThreshold, Data d) {
		this.data = d;
		this.sMetric = sMetric;
		this.pMetric = pMetric;
		this.socialNeighbourhood = socialNeighbourhood;
		this.socialThreshold = socialThreshold;

		userSimilarities = new HashMap<Integer, LinkedHashMap<Integer, Double>>();

	}

	@Override
	public void train() {

		double sim = 0;

		String[] t = socialNeighbourhood.split("_");
		int k = new Integer(t[1]);

		for (Integer user1 : data.getMoviesByUser().keySet()) {
			if (data.getUserFriends().get(user1) == null) {
				// User has no friends :)
				continue;
			}

			ArrayList<Integer> friendsKthLevel = getFriendsKthLevel(user1, k);

			for (Integer user2 : friendsKthLevel) {

				sim = computeSimilarity(user1, user2);

				if (socialThreshold != null) {
					if (sim < socialThreshold) {
						continue;
					}
				}

				// this is for user1's list
				if (userSimilarities.containsKey(user1)) {
					LinkedHashMap<Integer, Double> s = userSimilarities
							.get(user1);
					s.put(user2, sim);
				} else {
					LinkedHashMap<Integer, Double> s = new LinkedHashMap<Integer, Double>();
					s.put(user2, sim);
					userSimilarities.put(user1, s);
				}

				// this is for user2's list
				if (userSimilarities.containsKey(user2)) {
					HashMap<Integer, Double> s = userSimilarities.get(user2);
					s.put(user1, sim);
				} else {
					LinkedHashMap<Integer, Double> s = new LinkedHashMap<Integer, Double>();
					s.put(user1, sim);
					userSimilarities.put(user2, s);
				}

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

	@Override
	public double predict(int userID, int movieID) {

		// If the user hasn't rated any movie yet, return 0
		if (!data.getUserMovieRatings().keySet().contains(userID)) {
			return 0;
		}

		double prediction = data.getAverageUserRatings().get(userID);

		ArrayList<Double> similaritiesList = new ArrayList<Double>();
		ArrayList<Double> ratingsList = new ArrayList<Double>();
		ArrayList<Double> averageList = new ArrayList<Double>();

		if (userSimilarities.get(userID) == null) {
			return prediction;
		}

		for (Map.Entry<Integer, Double> friend : userSimilarities.get(userID)
				.entrySet()) {
			if (data.getUserMovieRatings().get(friend.getKey()).get(movieID) != null) {
				similaritiesList.add(friend.getValue());
				ratingsList.add(data.getUserMovieRatings().get(friend.getKey())
						.get(movieID));
				averageList.add(data.getAverageUserRatings().get(
						friend.getKey()));
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

		// wrong part
		if (socialNeighbourhood.equals("friendsoffriends")) {

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

		}

		if (prediction == 0) {
			// System.out.println("Returning average");
			return data.getAverageUserRatings().get(userID);
		}

		return prediction;

	}

	public ArrayList<Integer> getFriendsKthLevel(int userID, int k) {

		ArrayList<Integer> friends = new ArrayList<Integer>();

		// Initialize HashMap that stores a boolean (true if node is visited in
		// BFS, false otherwise). In the beginning, no nodes are visited yet.
		HashMap<Integer, Boolean> visited = new HashMap<Integer, Boolean>();
		for (Integer user : data.getUserMovieRatings().keySet()) {
			visited.put(user, false);
		}

		HashMap<Integer, Integer> distance = new HashMap<Integer, Integer>();

		LinkedList<Integer> queue = new LinkedList<Integer>();

		for (Integer friend : data.getUserFriends().get(userID)) {
			queue.offer(friend);
			distance.put(friend, 1);
		}

		while (!queue.isEmpty()) {
			int currentNode = queue.poll();
			friends.add(currentNode);
			visited.put(currentNode, true);
			// Only go further if distance is smaller than maximal distance k
			if (distance.get(currentNode) < k) {
				for (Integer friend : data.getUserFriends().get(currentNode)) {
					if (!queue.contains(friend) && visited.get(friend) == false
							&& friend != userID) {
						queue.offer(friend);
						distance.put(friend, distance.get(currentNode) + 1);
					}
				}
			}
		}

		return friends;
	}

	// Slow, should not be used!!
	public ArrayList<Integer> getFriendsKthLevelRecursive(int userID, int k) {

		ArrayList<Integer> friends = new ArrayList<Integer>();

		if (k == 1) {

			for (Integer friend : data.getUserFriends().get(userID)) {
				friends.add(friend);
			}
			return friends;

		} else {

			// Recursively call method to traverse the friend graph until k-th
			// level
			for (Integer friend : data.getUserFriends().get(userID)) {
				ArrayList<Integer> furtherFriends = getFriendsKthLevelRecursive(
						friend, k - 1);
				for (Integer furtherFriend : furtherFriends) {
					if (!friends.contains(furtherFriend)
							&& furtherFriend != userID) {
						friends.add(furtherFriend);
					}
				}
				if (!friends.contains(friend)) {
					friends.add(friend);
				}
			}
			return friends;

		}
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

		// HACK!
		if (data.getMoviesByUser().get(user1) == null
				|| data.getMoviesByUser().get(user2) == null) {
			return sim;
		}

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
