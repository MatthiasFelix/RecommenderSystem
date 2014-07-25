package generator;

import rec.Recommender;

public class GeneratorRunner {

	public static void main(String[] args) {

		String friendsFile = "/Users/matthiasfelix/git/RecommenderSystem/RecommenderSystem/artificial/friendsA01.txt";
		String ratingsFile = "/Users/matthiasfelix/git/RecommenderSystem/RecommenderSystem/artificial/";

		String rootPath = "/Users/matthiasfelix/git/RecommenderSystem/RecommenderSystem/";
		String parameterFile = "src/parameters.txt";

		int[] numberOfCommunityItems = { 50, 100, 200, 400 };
		double[] as = { 0.0, 1 / 3.0, 2 / 3.0, 1.0 };
		double[] bs = { 0.0, 1 / 3.0, 2 / 3.0, 1.0 };
		double[] ps = { 0.05, 1 / 3.0, 2 / 3.0, 1.0 };

		for (int n : numberOfCommunityItems) {
			for (double a : as) {
				for (double b : bs) {
					for (double p : ps) {
						RatingGenerator.main(new String[] { friendsFile,
								ratingsFile + "Rating2_" + n + "_" + a + "_" + b + "_" + p + ".txt",
								Integer.toString(n), Double.toString(a), Double.toString(b),
								Double.toString(p) });
						if (a + b <= 1) {
							Recommender.main(new String[] { rootPath, parameterFile,
									"Result2_" + n + "_" + a + "_" + b + "_" + p + ".txt",
									"Rating2_" + n + "_" + a + "_" + b + "_" + p + ".txt" });
						}
					}
				}
			}
		}

	}

}
