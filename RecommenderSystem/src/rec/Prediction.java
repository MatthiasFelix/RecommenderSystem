package rec;

import java.util.ArrayList;

public class Prediction {

	public static double calculateWeightedSum(ArrayList<Double> similarities,
			ArrayList<Double> ratings) {

		double numerator = 0, denominator = 0;

		for (int i = 0; i < similarities.size(); i++) {
			if (similarities.get(i) > 0) {
				numerator += similarities.get(i) * ratings.get(i);
				denominator += similarities.get(i);
			}
		}

		if (denominator == 0)
			return 0;
		return numerator / denominator;

	}

	public static double calculateAdjustedSum(double average,
			ArrayList<Double> averageRatings, ArrayList<Double> ratings) {

		int denominator = ratings.size();
		double numerator = 0;

		for (int i = 0; i < ratings.size(); i++)
			numerator += ratings.get(i) - averageRatings.get(i);

		if (denominator == 0)
			return 0;
		return average + numerator / denominator;

	}

	public static double calculateAdjustedWeightedSum(double average,
			ArrayList<Double> averageRatings, ArrayList<Double> ratings,
			ArrayList<Double> similarities) {

		double denominator = 0, numerator = 0;

		for (int i = 0; i < ratings.size(); i++) {
			if (similarities.get(i) > 0) {
				numerator += similarities.get(i)
						* (ratings.get(i) - averageRatings.get(i));
				denominator += similarities.get(i);
			}
		}

		if (denominator == 0)
			return 0;
		return average + numerator / denominator;

	}

	public static double calculateCentralitySum(double average,
			ArrayList<Double> averageRatings, ArrayList<Double> ratings,
			ArrayList<Double> similarities, ArrayList<Double> centralityScores) {

		double denominator = 0, numerator = 0;

		for (int i = 0; i < ratings.size(); i++) {
			if (similarities.get(i) > 0 && centralityScores.get(i) > 0) {
				numerator += similarities.get(i) * centralityScores.get(i)
						* (ratings.get(i) - averageRatings.get(i));
				denominator += similarities.get(i) * centralityScores.get(i);
			}
		}

		if (denominator == 0)
			return 0;
		return average + numerator / denominator;

	}

}
