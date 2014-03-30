package rec;

public class Similarity {

	public static double calculateCosineSimilarity(double[] ratings1,
			double[] ratings2) {

		double numerator = 0, denominator1 = 0, denominator2 = 0;

		for (int i = 0; i < ratings1.length; i++) {
			numerator += ratings1[i] * ratings2[i];
			denominator1 += Math.pow(ratings1[i], 2);
			denominator2 += Math.pow(ratings2[i], 2);
		}

		if (denominator1 * denominator2 == 0) {
			return 0;
		} else {
			return numerator / Math.sqrt(denominator1 * denominator2);
		}

	}

	public static double calculatePearsonCorrelation(double[] ratings1,
			double[] ratings2, double average1, double average2) {

		double numerator = 0, denominator1 = 0, denominator2 = 0;

		for (int i = 0; i < ratings1.length; i++) {
			numerator += (ratings1[i] - average1) * (ratings2[i] - average2);
			denominator1 += Math.pow(ratings1[i] - average1, 2);
			denominator2 += Math.pow(ratings2[i] - average2, 2);
		}

		if (denominator1 * denominator2 == 0) {
			return 0;
		} else {
			return numerator / Math.sqrt(denominator1 * denominator2);
		}
	}

}
