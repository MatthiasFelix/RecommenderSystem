package rec;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * This is the main class of the recommender package. It reads the parameter
 * file, runs the specified predictors and computes the predictive errors. All
 * parameters should be specified using the parameterFile "parameters.txt"
 */
public class Recommender {

	// default settings
	private static int neighbourhoodSize = 20;
	private static double threshold;
	private static boolean useThreshold = false;
	private static double[] settings = new double[10];

	private static int minimumRating, maximumRating;
	private static boolean normalize = false;

	private static int[][] trainData;
	private static int[][] testData;

	private static double averageSizeOfSimilarityListUsers = 0;
	private static double averageSizeOfSimilarityListMovies = 0;

	private static String fileDirectory = "lastfm-2k/";
	private static String trainDataFile = "ua.base";
	private static String testDataFile = "ua.test";
	private static String parameterFile = "src/rec/parameters.txt";
	private static String[] predictors = { "AverageBased" };
	private static String[] smetrics = { "cosinesimilarity" };
	private static String[] pmetrics = { "average" };

	private static final boolean DEBUG = true;
	private static boolean logLevel = !DEBUG;

	/**
	 * The main function sets the parameters, runs read the training and test
	 * data and runs the predictors.
	 * 
	 * @param args
	 *            (not needed)
	 */
	public static void main(String[] args) {
		// set the parameters. unless the parameter is mentioned in the text
		// file, the default is set

		ParseInput.setParameters(parameterFile);

		// Assign the parameters for the different test runs
		if (useThreshold) {
			settings = new double[] { 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8,
					0.9, 1 };
		} else {
			settings = new double[] { 1, 20, 50, 100, 200, 400, 800 };
		}

		// read-in training and test data
		trainData = readData(fileDirectory + trainDataFile);
		testData = readData(fileDirectory + testDataFile);

		for (int n = 0; n < settings.length; n++) {
			if (useThreshold) {
				threshold = settings[n];
			} else {
				neighbourhoodSize = (int) settings[n];
			}
			// calculate the number of runs according to the used algorithms
			int runs = 0;
			for (int i = 0; i < predictors.length; i++) {
				if (predictors[i].equals("userbased"))
					runs += smetrics.length * pmetrics.length;
				else if (predictors[i].equals("itembased"))
					runs += smetrics.length * pmetrics.length;
				else
					runs++;
			}

			double[] predictiveErrors = new double[runs];
			double[] runTimes = new double[runs];

			int runCount = 0;

			// calculate the actual predictions, predictive errors and run times
			for (int i = 0; i < predictors.length; i++) {
				if (predictors[i].toLowerCase().equals("userbased")
						|| predictors[i].toLowerCase().equals("itembased")) {
					for (int j = 0; j < smetrics.length; j++) {
						for (int k = 0; k < pmetrics.length; k++) {
							runTimes[runCount] = System.nanoTime();
							predictiveErrors[runCount] = runTest(predictors[i],
									smetrics[j], pmetrics[k], trainData,
									testData);
							runTimes[runCount] = (System.nanoTime() - runTimes[runCount])
									/ Math.pow(10, 9);
							runCount++;
						}
					}
				} else {
					runTimes[runCount] = System.nanoTime();
					predictiveErrors[runCount] = runTest(predictors[i],
							smetrics[0], pmetrics[0], trainData, testData);
					runTimes[runCount] = (System.nanoTime() - runTimes[runCount])
							/ Math.pow(10, 9);
					runCount++;
				}
			}

			// display results
			System.out.println("========================================");
			if (useThreshold)
				System.out.println("Results for thresholdsize " + settings[n]
						+ "\naverage neighbourhoodSize for Users: "
						+ averageSizeOfSimilarityListUsers
						+ "\naverage neighbourhoodSize for Movies: "
						+ averageSizeOfSimilarityListMovies);
			else
				System.out.println("Results for neighbourhoodsize "
						+ neighbourhoodSize);

			System.out.println("========================================");

			System.out
					.println("Predictor \t\t Similarity metric \t Prediction metric \t RMSE \t\t Run Time (s)\n");
			int displayCount = 0;
			for (int i = 0; i < predictors.length; i++) {
				if (predictors[i].toLowerCase().equals("userbased")
						|| predictors[i].toLowerCase().equals("itembased")) {
					for (int j = 0; j < smetrics.length; j++) {
						for (int k = 0; k < pmetrics.length; k++) {
							System.out.println(String.format(
									"%s \t\t %s \t %s \t\t %.5f \t %.3f\n",
									predictors[i], smetrics[j],
									pmetrics[k].substring(0, 10),
									predictiveErrors[displayCount],
									runTimes[displayCount]));
							displayCount++;
						}
					}
				} else {
					System.out.println(String.format(
							"%s \t\t %s \t %s \t\t %.5f \t %.3f\n",
							predictors[i], "--------------", "------------",
							predictiveErrors[displayCount],
							runTimes[displayCount]));
					displayCount++;
				}
			}
		}
	}

	/**
	 * Runs the predictor specified and computes the root mean squared error
	 * 
	 * @param predictor
	 *            : name of the predictor: averagebased, useraverage, userbased,
	 *            itembased, mypredictor
	 * @param trainData
	 *            : data used to train the predictor
	 * @param testData
	 *            : data used to test the accuracy of the predictions
	 * @return root mean square error of the predictions
	 */
	public static double runTest(String predictor, String smetric,
			String pmetric, int[][] trainData, int[][] testData) {

		Predictor p = null;

		if (predictor.toLowerCase().equals("averagebased")) {
			p = new AverageBasedPredictor();
		} else if (predictor.toLowerCase().equals("useraverage")) {
			p = new UserAverageBasedPredictor();
		} else if (predictor.toLowerCase().equals("userbased")) {
			p = new UserBasedPredictor(neighbourhoodSize, smetric, pmetric);
		} else if (predictor.toLowerCase().equals("itembased")) {
			p = new ItemBasedPredictor(neighbourhoodSize, smetric, pmetric);
		}

		// train the predictor
		p.train(trainData);

		// compute the predictions
		int N = testData.length;
		double[] predictions = new double[N];
		for (int i = 0; i < N; i++) {
			predictions[i] = p.predict(testData[i][0], testData[i][1]);
		}

		if (logLevel == DEBUG) {
			System.out.println("========================================");
			System.out.println("Predictions from " + predictor);
			System.out.println("========================================");
			System.out.println("User \t Movie \t Rating  Prediction");
			for (int i = 0; i < N; i++) {
				System.out.println((String.format("%d \t %d \t %d \t %.3f \n",
						testData[i][0], testData[i][1], testData[i][2],
						predictions[i])));
			}
		}

		// compute the root mean squared error
		double RMSE = 0;
		for (int i = 0; i < N; i++) {
			RMSE += (predictions[i] - testData[i][2])
					* (predictions[i] - testData[i][2]);
		}
		RMSE = Math.sqrt(RMSE / (double) N);

		return RMSE;
	}

	/**
	 * Reads in data from a file and converts them into an integer array Data
	 * are expected to be of the form (user, movie, rating)_i
	 * 
	 * @param fileName
	 *            : the data file
	 * @return an integer array (user, movie, rating)_i
	 */
	public static int[][] readData(String fileName) {
		BufferedReader b;
		int[][] data = null;
		try {
			b = new BufferedReader(new FileReader(fileName));
			String line;
			// count number of lines
			int N = 0;
			while ((line = b.readLine()) != null) {
				N++;
			}
			b.close();

			// data is of the form: (userID, movieID,rating)_i
			data = new int[N][3];
			int count = 0;
			b = new BufferedReader(new FileReader(fileName));
			while ((line = b.readLine()) != null) {
				String[] s = line.split("\t");
				for (int i = 0; i < 3; i++) {
					try {
						data[count][i] = new Integer(s[i]);
					} catch (NumberFormatException nfe) {
						System.err
								.println("input data must be of type integer.");
						nfe.printStackTrace();
					}

				}
				count++;
			}
			b.close();

		} catch (FileNotFoundException e) {
			System.err
					.println("Couldn't read the parameter file. Check the file name: "
							+ fileName);
			e.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

		return data;
	}

	public static int getNeighbourhoodSize() {
		return neighbourhoodSize;
	}

	public static void setNeighbourhoodSize(int neighbourhoodSize) {
		Recommender.neighbourhoodSize = neighbourhoodSize;
	}

	public static void setLoglevel(String log) {
		if (log.toLowerCase().equals("default")) {
			logLevel = !DEBUG;
		} else if (log.toLowerCase().equals("debug")) {
			logLevel = DEBUG;
		}
	}

	public static void setTrainDataFile(String trainDataFile) {
		Recommender.trainDataFile = trainDataFile;
	}

	public static void setTestDataFile(String testDataFile) {
		Recommender.testDataFile = testDataFile;
	}

	public static void setPredictors(String[] predictors) {
		Recommender.predictors = predictors;
	}

	public static void setSimilarityMetrics(String[] smetrics) {
		Recommender.smetrics = smetrics;
	}

	public static void setPredictionMetrics(String[] pmetrics) {
		Recommender.pmetrics = pmetrics;
	}

	public static String getTrainDataFile() {
		return trainDataFile;
	}

	public static String getTestDataFile() {
		return testDataFile;
	}

	public static String[] getPredictors() {
		return predictors;
	}

	public static boolean getLogLevel() {
		return logLevel;
	}

	public static boolean isThreshold() {
		return useThreshold;
	}

	public static double getThreshold() {
		return threshold;
	}

	public static void setThreshold(boolean threshold) {
		useThreshold = threshold;
	}

	public static void setAverageSizeOfSimilarityListMovies(double size) {
		averageSizeOfSimilarityListMovies = size;
	}

	public static void setAverageSizeOfSimilarityListUsers(double size) {
		averageSizeOfSimilarityListUsers = size;
	}

	public static void setMinAndMax(int min, int max) {
		minimumRating = min;
		maximumRating = max;
		normalize = true;
	}

	public static int getMin() {
		return minimumRating;
	}

	public static int getMax() {
		return maximumRating;
	}

	public static boolean isNormalize() {
		return normalize;
	}

}
