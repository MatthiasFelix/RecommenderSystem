package rec;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class DataManipulator {

	private static int minimum = Recommender.getMin();
	private static int maximum = Recommender.getMax();

	public static void normalizeRatings(
			TreeMap<Integer, HashMap<Integer, Double>> userMovieRatings) {

		for (Integer userID : userMovieRatings.keySet()) {

			double currentMin = Integer.MAX_VALUE;
			double currentMax = Integer.MIN_VALUE;

			// For each user, find his minimum and maximum rating
			for (Double rating : userMovieRatings.get(userID).values()) {
				if (rating < currentMin) {
					currentMin = rating;
				}
				if (rating > currentMax) {
					currentMax = rating;
				}
			}

			// Normalize the ratings and put them back into the userMovieRatings
			// hash map
			for (Map.Entry<Integer, Double> entry : userMovieRatings
					.get(userID).entrySet()) {
				double newRating;
				double normalizingFactor = (double) (entry.getValue() - currentMin)
						/ (double) (currentMax - currentMin);
				newRating = normalizingFactor * (maximum - minimum) + minimum;
				System.out.println("Replace " + entry.getValue() + " with "
						+ newRating);
				userMovieRatings.get(userID).put(entry.getKey(), newRating);
			}

		}

	}
}
