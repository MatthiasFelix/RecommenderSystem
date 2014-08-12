package generator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import rec.Recommender;

/**
 * This class can be used to split a data set into multiple training and test
 * data sets using k-fold cross-validation
 * 
 * @author matthiasfelix
 *
 */
public class CrossValidator {

	private static double[][] data;
	private static TreeMap<Integer, HashMap<Integer, Double>> userItemRatings;

	private static String rootPath;
	private static String dataSet;
	private static String ratingsFile;

	private static int k;
	private static int repetitions;

	public static void main(String[] args) {

		userItemRatings = new TreeMap<Integer, HashMap<Integer, Double>>();

		if (args.length != 0) {
			rootPath = args[0];
			dataSet = args[1];
			ratingsFile = args[2];
			k = Integer.parseInt(args[3]);
			repetitions = Integer.parseInt(args[4]);
		} else {
			// Set parameters manually here;
			rootPath = "/Users/matthiasfelix/git/RecommenderSystem/RecommenderSystem/";
			dataSet = "artificial/";
			ratingsFile = "_50_0.15_0.0_0.1.txt";
			k = 5;
			repetitions = 1;
		}

		data = Recommender.readData(dataSet + ratingsFile);

		initializeUserItemRatings(data);

		Recommender.applyKFold(kFold());

	}

	public static void initializeUserItemRatings(double[][] data) {
		for (int i = 0; i < data.length; i++) {
			if (userItemRatings.containsKey((int) data[i][0])) {
				userItemRatings.get((int) data[i][0]).put((int) data[i][1], data[i][2]);
			} else {
				HashMap<Integer, Double> ratings = new HashMap<Integer, Double>();
				ratings.put((int) data[i][1], data[i][2]);
				userItemRatings.put((int) data[i][0], ratings);
			}
		}
	}

	// This method makes the k-fold cross validation and returns a String[] with
	// the names of all the files.
	public static String[] kFold() {

		String[] fileNames = new String[k * repetitions];

		for (int i = 1; i <= repetitions; i++) {

			String files[] = new String[k];
			for (int j = 0; j < k; j++) {
				files[j] = "";
			}

			for (Integer user : userItemRatings.keySet()) {

				int N = userItemRatings.get(user).size();

				int setSize = N / k;
				int rest = N - (k * setSize);

				// There are k different partitions of the ratings (files[]),
				// and their number
				// of lines is stored in fileLines[]
				int fileLines[] = new int[k];
				for (int j = 0; j < k; j++) {
					fileLines[j] = setSize;
				}
				for (int j = 0; rest > 0; j++) {
					fileLines[j]++;
					rest--;
				}

				for (Map.Entry<Integer, Double> entry : userItemRatings.get(user).entrySet()) {
					String line = user + "\t" + entry.getKey() + "\t" + entry.getValue() + "\n";
					Random rnd = new Random();
					double r = rnd.nextDouble();
					int partition = (int) (r / (1.0 / k));

					if (fileLines[partition] > 0) {
						files[partition] = files[partition].concat(line);
						fileLines[partition]--;
					}

				}
			}
			// Now the different partitions are made and the files have to
			// be created.

			for (int j = 1; j <= k; j++) {
				String fileName = ratingsFile.split(".txt")[0] + "__" + i + "_" + j;
				fileNames[(i - 1) * k + (j - 1)] = fileName;
				File base = new File(rootPath + dataSet + fileName + ".base");
				File test = new File(rootPath + dataSet + fileName + ".test");

				FileWriter baseFileWriter;
				FileWriter testFileWriter;

				try {

					baseFileWriter = new FileWriter(base);
					testFileWriter = new FileWriter(test);
					BufferedWriter baseBufferedWriter = new BufferedWriter(baseFileWriter);
					BufferedWriter testBufferedWriter = new BufferedWriter(testFileWriter);

					for (int l = 1; l <= k; l++) {
						if (l == j) {
							testBufferedWriter.write(files[l - 1]);
						} else {
							baseBufferedWriter.write(files[l - 1]);
						}
					}

					baseBufferedWriter.close();
					testBufferedWriter.close();

				} catch (IOException e) {
					e.printStackTrace();
				}

			}

		}

		return fileNames;

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
			System.err.println("The percentage of the training set has to be between 0 and 1.");
			return;
		}

		for (int i = 1; i <= repetitions; i++) {

			File baseFile = new File(
					"/Users/matthiasfelix/git/RecommenderSystem/RecommenderSystem/" + dataSet
							+ basePercentage + "_set" + i + ".base");
			File testFile = new File(
					"/Users/matthiasfelix/git/RecommenderSystem/RecommenderSystem/" + dataSet
							+ basePercentage + "_set" + i + ".test");
			FileWriter baseFileWriter;
			FileWriter testFileWriter;

			try {

				baseFileWriter = new FileWriter(baseFile);
				testFileWriter = new FileWriter(testFile);
				BufferedWriter baseBufferedWriter = new BufferedWriter(baseFileWriter);
				BufferedWriter testBufferedWriter = new BufferedWriter(testFileWriter);

				for (Integer user : userItemRatings.keySet()) {

					int N = userItemRatings.get(user).size();

					int baseMax = Math.round((float) (N * basePercentage));
					int testMax = N - baseMax;

					int baseSize = 0, testSize = 0;

					for (Map.Entry<Integer, Double> entry : userItemRatings.get(user).entrySet()) {
						String line = user + "\t" + entry.getKey() + "\t" + entry.getValue();
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
