package rec;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/**
 * 
 * @author matthiasfelix
 * 
 *         This class can be used to split a data set into multiple training and
 *         test data sets
 * 
 */
public class CrossValidator {

	private static double[][] data;
	private static TreeMap<Integer, HashMap<Integer, Double>> userMovieRatings;

	public static void main(String[] args) {
		userMovieRatings = new TreeMap<Integer, HashMap<Integer, Double>>();

		data = Recommender.readData("lastfm-2k/user_artists_n.data");
		initializeUserMovieRatings(data);

		divide(0.8, 5);
	}

	public static void initializeUserMovieRatings(double[][] data) {
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
	 * This method splits a given data set into a training set and a test set,
	 * with sizes according to the percentage chosen. This can be repeated
	 * multiple times, to generate multiple trainings and test data sets.
	 * 
	 * @param basePercentage
	 *            The percentage of the whole data set that is put into the
	 *            training set
	 * @param repetitions
	 *            The number of training-test pairs the are generated
	 */
	public static void divide(double basePercentage, int repetitions) {

		// The percentage of the training set has to be between 0 and 1
		if (0 > basePercentage || basePercentage > 1) {
			System.err
					.println("The percentage of the training set has to be between 0 and 1.");
			return;
		}

		for (int i = 1; i <= repetitions; i++) {

			File baseFile = new File(
					"/Users/matthiasfelix/git/RecommenderSystem/RecommenderSystem/lastfm-2k/set"
							+ i + ".base");
			File testFile = new File(
					"/Users/matthiasfelix/git/RecommenderSystem/RecommenderSystem/lastfm-2k/set"
							+ i + ".test");
			FileWriter baseFileWriter;
			FileWriter testFileWriter;

			try {

				baseFileWriter = new FileWriter(baseFile);
				testFileWriter = new FileWriter(testFile);
				BufferedWriter baseBufferedWriter = new BufferedWriter(
						baseFileWriter);
				BufferedWriter testBufferedWriter = new BufferedWriter(
						testFileWriter);

				for (Integer user : userMovieRatings.keySet()) {

					int N = userMovieRatings.get(user).size();
					System.out.println("N: " + N);

					int baseMax = Math.round((float) (N * basePercentage));
					int testMax = N - baseMax;

					int baseSize = 0, testSize = 0;

					for (Map.Entry<Integer, Double> entry : userMovieRatings
							.get(user).entrySet()) {
						String line = user + "\t" + entry.getKey() + "\t"
								+ entry.getValue();
						if (baseSize == baseMax) {
							testBufferedWriter.write(line + "\n");
						} else if (testSize == testMax) {
							baseBufferedWriter.write(line + "\n");
						} else {
							Random rnd = new Random();
							if (rnd.nextDouble() < basePercentage) {
								baseBufferedWriter.write(line + "\n");
								baseSize++;
							} else {
								testBufferedWriter.write(line + "\n");
								testSize++;
							}
						}
					}
				}

				baseBufferedWriter.close();
				testBufferedWriter.close();

			} catch (IOException e) {
				e.printStackTrace();
			}

		}

	}
}
