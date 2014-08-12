package rec;

import generator.CrossValidator;
import generator.RatingGenerator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import predictors.AverageBasedPredictor;
import predictors.ItemBasedPredictor;
import predictors.Predictor;
import predictors.SocialUserBasedPredictor;
import predictors.UserAverageBasedPredictor;
import predictors.UserBasedPredictor;

/**
 * This is the main class of the recommender package. It reads the parameter
 * file, runs the specified predictors and computes the predictive errors. All
 * parameters should be specified using the parameter file "parameters.txt"
 */
public class Recommender {

	private static String rootPath = "/Users/matthiasfelix/git/RecommenderSystem/RecommenderSystem/";

	private static String parameterFile = "src/parameters.txt";

	private static String resultFile = "/Users/matthiasfelix/git/RecommenderSystem/RecommenderSystem/results/RESULTS_SOCIALUSERBASED_310714.txt";

	private static final boolean DEBUG = true;

	// default settings
	private static boolean logLevel = !DEBUG;
	private static String dataSet = "lastfm-2k/";
	private static String[] trainDataFiles = { "set1.base" };
	private static String[] testDataFiles = { "set1.test" };
	private static String currentTrainSet = "set1.base";
	private static String currentTestSet = "set1.test";
	private static String[] neighbourhood = { "size" };
	private static int[] neighbourhoodSizes = { 50 };
	private static double[] thresholds = { 0.1, 0.5 };
	private static String[] predictors = { "userbased" };
	private static String[] smetrics = { "cosine" };
	private static String[] pmetrics = { "adjweighted" };
	private static String[] socialNeighbourhood = { "friends_1" };
	private static double[] socialThresholds = { 0.2, 0.7 };

	private static String ratingsDataFile = "user_artists_n.data";
	private static String friendsDataFile = "user_friends_n.txt";

	private static final boolean GENERATENETWORKANDRATINGS = false;
	private static final boolean CROSSVALIDATE = false;
	private static final boolean CREATERESULTFILE = false;

	private static String splitBy = "\t";

	// 2D arrays to store the data from the input files
	private static double[][] trainData;
	private static double[][] testData;
	private static int[][] userFriends;

	// Average neighbourhoodSizes when threshold is used
	private static double averageSizeOfSimilarityListUsers = 0;
	private static double averageSizeOfSimilarityListItems = 0;

	private static int averageUsers = 0;

	// ResultSaver class (writes all results to a file)
	private static ResultSaver resultSaver;

	// CrossValidator parameters
	private static int k = 5;
	private static int repetitions = 1;

	private static HashMap<Integer, ArrayList<Double>> userbasedErrors = new HashMap<Integer, ArrayList<Double>>();
	private static HashMap<Integer, ArrayList<Double>> socialuserbasedErrors = new HashMap<Integer, ArrayList<Double>>();

	private static HashMap<Integer, Double> userbasedAverages = new HashMap<Integer, Double>();
	private static HashMap<Integer, Double> socialuserbasedAverages = new HashMap<Integer, Double>();

	/**
	 * The main function sets the parameters, reads the training and test data
	 * and runs the predictors.
	 * 
	 * @param args
	 *
	 */
	public static void main(String[] args) {

		if (args.length != 0) {
			rootPath = args[0];
			parameterFile = args[1];
			resultFile = args[2];
		}
		if (args.length > 3) {
			ratingsDataFile = args[3];
		}

		// Set the parameters. Unless the parameter is mentioned in the text
		// file, the default is set.
		ParseInput.setParameters(parameterFile);

		if (GENERATENETWORKANDRATINGS) {
			RatingGenerator.main(new String[] { rootPath + dataSet + friendsDataFile,
					rootPath + dataSet + ratingsDataFile });
		}

		if (CROSSVALIDATE) {
			CrossValidator.main(new String[] { rootPath, dataSet, ratingsDataFile,
					Integer.toString(k), Integer.toString(repetitions) });
		}

		if (trainDataFiles.length != testDataFiles.length) {
			System.err.println("The number of train and test files must be equal.");
			System.exit(-1);
		}

		// Read the file with the friends relations
		userFriends = readFriendsList(dataSet + friendsDataFile);

		// Initialize the ResultSaver
		resultSaver = new ResultSaver();

		// Run the tests for all sets of .base and .test files
		for (int c = 0; c < trainDataFiles.length; c++) {

			System.out
					.println("================================================================================");
			System.out.println("Training set: " + trainDataFiles[c]);
			System.out.println("Test set: " + testDataFiles[c]);
			System.out
					.println("================================================================================");

			// read-in training and test data
			trainData = readData(dataSet + trainDataFiles[c]);
			testData = readData(dataSet + testDataFiles[c]);

			// Set current train- and testSet
			currentTrainSet = trainDataFiles[c];
			currentTestSet = testDataFiles[c];

			Data data = new Data(trainData, userFriends, rootPath + dataSet + friendsDataFile,
					rootPath + dataSet + ratingsDataFile);

			// Run all tests (all combinations of the specified settings)
			runAllTests(data);

			// Compute average errors

			for (Map.Entry<Integer, ArrayList<Double>> entry : userbasedErrors.entrySet()) {
				double average = 0.0;
				for (Double d : entry.getValue()) {
					average += d;
				}
				average = Math.sqrt(average / entry.getValue().size());
				userbasedAverages.put(entry.getKey(), average);
			}
			for (Map.Entry<Integer, ArrayList<Double>> entry : socialuserbasedErrors.entrySet()) {
				double average = 0.0;
				for (Double d : entry.getValue()) {
					average += d;
				}
				average = Math.sqrt(average / entry.getValue().size());
				socialuserbasedAverages.put(entry.getKey(), average);
			}

			// Test users
			File f = new File(rootPath + "results/userErrors03.txt");

			try {

				BufferedWriter bw = new BufferedWriter(new FileWriter(f));

				for (Integer user : userbasedAverages.keySet()) {
					Double userAverage = userbasedAverages.get(user);
					Double socialuserAverage = socialuserbasedAverages.get(user);
					if (data.getUserFriends().containsKey(user)) {
						bw.write(user + "\t" + (userAverage - socialuserAverage) + "\t"
								+ data.getUserFriends().get(user).size() + "\n");
					} else {
						bw.write(user + "\t" + (userAverage - socialuserAverage) + "\t0\n");
					}

				}

				bw.close();

			} catch (IOException e) {
				e.printStackTrace();
			}

			if (CREATERESULTFILE) {
				resultSaver.writeToFile(resultFile);
			}
		}

	}

	public static void runAllTests(Data data) {
		for (String nbHood : neighbourhood) {
			if (nbHood.equals("size")) {
				for (Integer size : neighbourhoodSizes) {
					System.out.println("----------------------------------");
					System.out.println("Neighbourhood Size: " + size);
					System.out.println("----------------------------------");
					System.out
							.println("Predictor \t\t Similarity metric \t Prediction metric \t RMSE \t\t Run Time (s)\n");
					for (String predictor : predictors) {
						if (!predictor.contains("social")) {
							for (String smetric : smetrics) {
								for (String pmetric : pmetrics) {
									long startTime = System.nanoTime();
									double rmse = runTest(nbHood, (double) size, predictor,
											smetric, pmetric, "", data);
									double runTime = ((System.nanoTime() - startTime) / Math.pow(
											10, 9));
									System.out.println(String.format(
											"%s \t\t %s \t\t %s \t\t %.5f \t %.3f\n", predictor,
											smetric, pmetric, rmse, runTime));
									resultSaver.putResult(currentTrainSet, currentTestSet, nbHood,
											size, null, predictor, smetric, pmetric, null, null,
											rmse, runTime);
								}
							}
						}
					}
				}
			}

			else if (nbHood.equals("threshold")) {
				for (Double threshold : thresholds) {
					System.out.println("----------------------------------");
					System.out.println("Threshold: " + threshold);
					System.out.println("----------------------------------");
					System.out
							.println("Predictor \t\t Similarity metric \t Prediction metric \t RMSE \t\t Run Time (s)\n");
					for (String predictor : predictors) {
						if (!predictor.contains("social")) {
							for (String smetric : smetrics) {
								for (String pmetric : pmetrics) {
									long startTime = System.nanoTime();
									double rmse = runTest(nbHood, threshold, predictor, smetric,
											pmetric, "", data);
									double runTime = ((System.nanoTime() - startTime) / Math.pow(
											10, 9));
									System.out.println(String.format(
											"%s \t\t %s \t\t %s \t\t %.5f \t %.3f\n", predictor,
											smetric, pmetric, rmse, runTime));
									resultSaver.putResult(currentTrainSet, currentTestSet, nbHood,
											null, threshold, predictor, smetric, pmetric, null,
											null, rmse, runTime);
									if (predictor.equals("userbased"))
										System.out.println("Average neighbourhood size for users: "
												+ String.format("%.2f",
														averageSizeOfSimilarityListUsers) + "\n");
									else if (predictor.equals("itembased"))
										System.out.println("Average neighbourhood size for items: "
												+ String.format("%.2f",
														averageSizeOfSimilarityListItems) + "\n");
								}
							}
						}
					}
				}
			} else if (nbHood.equals("social")) {
				for (String socialN : socialNeighbourhood) {
					System.out.println("----------------------------------");
					System.out.println("Social Neighbourhood: " + socialN);
					System.out.println("----------------------------------");
					System.out
							.println("Predictor \t\t Similarity metric \t Prediction metric \t RMSE \t\t Run Time (s)\n");
					for (String predictor : predictors) {
						// Only run the social settings if a social predictor is
						// used
						if (predictor.contains("social")) {
							for (String smetric : smetrics) {
								for (String pmetric : pmetrics) {
									long startTime = System.nanoTime();
									double rmse = runTest(nbHood, null, predictor, smetric,
											pmetric, socialN, data);
									double runTime = ((System.nanoTime() - startTime) / Math.pow(
											10, 9));
									System.out.println(String.format(
											"%s \t\t %s \t\t %s \t\t %.5f \t %.3f\n", predictor,
											smetric, pmetric, rmse, runTime));
									resultSaver.putResult(currentTrainSet, currentTestSet, nbHood,
											null, null, predictor, smetric, pmetric, socialN, null,
											rmse, runTime);
									if (predictor.equals("socialuser"))
										System.out.println("Average neighbourhood size for users: "
												+ String.format("%.2f",
														averageSizeOfSimilarityListUsers) + "\n");
								}
							}
						}
					}
				}
			} else if (nbHood.equals("socialthreshold")) {
				for (String socialN : socialNeighbourhood) {
					System.out.println("----------------------------------");
					System.out.println("Social Neighbourhood: " + socialN);
					System.out.println("----------------------------------");
					for (Double socialThreshold : socialThresholds) {
						System.out.println("----------------------------------");
						System.out.println("Threshold: " + socialThreshold);
						System.out.println("----------------------------------");
						System.out
								.println("Predictor \t\t Similarity metric \t Prediction metric \t RMSE \t\t Run Time (s)\n");
						for (String predictor : predictors) {
							// Only run the social settings if a social
							// predictor is
							// used
							if (predictor.contains("social")) {
								for (String smetric : smetrics) {
									for (String pmetric : pmetrics) {
										long startTime = System.nanoTime();
										double rmse = runTest(nbHood, socialThreshold, predictor,
												smetric, pmetric, socialN, data);
										double runTime = ((System.nanoTime() - startTime) / Math
												.pow(10, 9));
										System.out.println(String.format(
												"%s \t\t %s \t\t %s \t\t %.5f \t %.3f\n",
												predictor, smetric, pmetric, rmse, runTime));
										resultSaver.putResult(currentTrainSet, currentTestSet,
												nbHood, null, null, predictor, smetric, pmetric,
												socialN, socialThreshold, rmse, runTime);
										if (predictor.equals("socialuser"))
											System.out
													.println("Average neighbourhood size for users: "
															+ String.format("%.2f",
																	averageSizeOfSimilarityListUsers)
															+ "\n");
									}
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Runs the predictor specified and computes the root mean squared error
	 * 
	 * @return root mean square error of the predictions
	 */
	public static double runTest(String nbHood, Double threshOrSize, String predictor,
			String smetric, String pmetric, String socialNeighbourhood, Data d) {

		Predictor p = null;

		if (predictor.equals("averagebased")) {
			p = new AverageBasedPredictor(d);
		} else if (predictor.equals("useraverage")) {
			p = new UserAverageBasedPredictor(d);
		} else if (predictor.equals("userbased")) {
			if (nbHood.equals("size"))
				p = new UserBasedPredictor(threshOrSize.intValue(), smetric, pmetric, d);
			else
				p = new UserBasedPredictor(threshOrSize, smetric, pmetric, d);
		} else if (predictor.equals("itembased")) {
			if (nbHood.equals("size"))
				p = new ItemBasedPredictor(threshOrSize.intValue(), smetric, pmetric, d);
			else
				p = new ItemBasedPredictor(threshOrSize, smetric, pmetric, d);
		} else if (predictor.equals("socialuser")) {
			if (nbHood.equals("social")) {
				p = new SocialUserBasedPredictor(smetric, pmetric, socialNeighbourhood, d);
			} else if (nbHood.equals("socialthreshold")) {
				p = new SocialUserBasedPredictor(smetric, pmetric, socialNeighbourhood,
						threshOrSize, d);
			}
		}

		// train the predictor
		p.train();

		averageSizeOfSimilarityListUsers = 0;
		averageUsers = 0;

		// compute the predictions
		int N = testData.length;
		double[] predictions = new double[N];
		for (int i = 0; i < N; i++) {
			predictions[i] = p.predict((int) testData[i][0], (int) testData[i][1]);
		}

		System.out.println("Number of predictions N: " + N);
		System.out.println("Average sim size: " + (averageSizeOfSimilarityListUsers / N));
		System.out.println("Users where average is used: " + averageUsers);

		if (logLevel == DEBUG) {
			System.out.println("========================================");
			System.out.println("Predictions from " + predictor);
			System.out.println("========================================");
			System.out.println("User \t Item \t Rating  Prediction");
			for (int i = 0; i < N; i++) {
				System.out
						.println((String.format("%d \t %d \t %.3f \t %.3f \n",
								(int) testData[i][0], (int) testData[i][1], testData[i][2],
								predictions[i])));
			}
		}

		// compute the root mean squared error
		double RMSE = 0;
		for (int i = 0; i < N; i++) {
			double error = (predictions[i] - testData[i][2]) * (predictions[i] - testData[i][2]);

			if (predictor.equals("userbased")) {
				if (!userbasedErrors.containsKey((int) testData[i][0])) {
					ArrayList<Double> hs = new ArrayList<Double>();
					userbasedErrors.put((int) testData[i][0], hs);
				}
				userbasedErrors.get((int) testData[i][0]).add(error);
			} else if (predictor.equals("socialuser")) {
				if (!socialuserbasedErrors.containsKey((int) testData[i][0])) {
					ArrayList<Double> hs = new ArrayList<Double>();
					socialuserbasedErrors.put((int) testData[i][0], hs);
				}
				socialuserbasedErrors.get((int) testData[i][0]).add(error);
			}

			RMSE += error;
		}
		RMSE = Math.sqrt(RMSE / (double) N);

		return RMSE;
	}

	/**
	 * Reads in data from a file and converts them into an integer array Data
	 * are expected to be of the form (user, item, rating)_i
	 * 
	 * @param fileName
	 *            : the data file
	 * @return an integer array (user, item, rating)_i
	 */
	public static double[][] readData(String fileName) {
		BufferedReader b;
		double[][] data = null;
		try {
			b = new BufferedReader(new FileReader(fileName));
			String line;
			// count number of lines
			int N = 0;
			while ((line = b.readLine()) != null) {
				N++;
			}
			b.close();

			// data is of the form: (userID, itemID,rating)_i
			data = new double[N][3];
			int count = 0;
			b = new BufferedReader(new FileReader(fileName));
			while ((line = b.readLine()) != null) {
				String[] s = line.split("\t");
				for (int i = 0; i < 2; i++) {
					try {
						data[count][i] = new Integer(s[i]);
					} catch (NumberFormatException nfe) {
						System.err.println("input data must be of type integer.");
						nfe.printStackTrace();
					}

				}
				try {
					data[count][2] = new Double(s[2]);
				} catch (NumberFormatException nfe) {
					System.err.println("rating data must be of type double.");
					nfe.printStackTrace();
				}
				count++;
			}
			b.close();

		} catch (FileNotFoundException e) {
			System.err
					.println("Couldn't read the parameter file. Check the file name: " + fileName);
			e.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

		return data;
	}

	public static int[][] readFriendsList(String fileName) {

		int[][] friendsList = null;

		try {

			// Count the number of friend relations
			BufferedReader b = new BufferedReader(new FileReader(fileName));
			int N = 0;
			while (b.readLine() != null) {
				N++;
			}
			b.close();

			friendsList = new int[N][2];

			// Fill the list with all friends relations

			b = new BufferedReader(new FileReader(fileName));

			for (int i = 0; i < N; i++) {
				String[] s = b.readLine().split(splitBy);
				try {
					friendsList[i][0] = new Integer(s[0]);
					friendsList[i][1] = new Integer(s[1]);
				} catch (NumberFormatException nfe) {
					System.err.println("input data must be of type integer.");
					nfe.printStackTrace();
				}
			}

		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

		return friendsList;
	}

	// Setters that are used by the ParseInput class
	public static void setLoglevel(String log) {
		if (log.equals("default")) {
			logLevel = !DEBUG;
		} else if (log.equals("debug")) {
			logLevel = DEBUG;
		}
	}

	public static void setDataSet(String dataSet) {
		Recommender.dataSet = dataSet + "/";
	}

	public static void setTrainDataFiles(String[] trainDataFiles) {
		Recommender.trainDataFiles = trainDataFiles;
	}

	public static void setTestDataFiles(String[] testDataFiles) {
		Recommender.testDataFiles = testDataFiles;
	}

	public static void applyKFold(String[] fileNames) {
		Recommender.trainDataFiles = new String[fileNames.length];
		Recommender.testDataFiles = new String[fileNames.length];
		for (int i = 0; i < fileNames.length; i++) {
			Recommender.trainDataFiles[i] = fileNames[i] + ".base";
			Recommender.testDataFiles[i] = fileNames[i] + ".test";
		}
	}

	public static void setNeighbourhood(String[] n) {
		Recommender.neighbourhood = n;
	}

	public static void setNeighbourhoodSizes(int[] sizes) {
		Recommender.neighbourhoodSizes = sizes;
	}

	public static void setThresholds(double[] thresholds) {
		Recommender.thresholds = thresholds;
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

	public static void setSocialNeighbourhood(String[] s) {
		socialNeighbourhood = s;
	}

	public static void setSocialThresholds(double[] thresholds) {
		socialThresholds = thresholds;
	}

	// Setters for averages (used by UserBasedPredictor/ItemBasedPredictor)
	public static void setAverageSizeOfSimilarityListItems(double size) {
		averageSizeOfSimilarityListItems = size;
	}

	public static void setAverageSizeOfSimilarityListUsers(double size) {
		averageSizeOfSimilarityListUsers = size;
	}

	public static void addSimilarityListSize(double size) {
		averageSizeOfSimilarityListUsers += size;
	}

	public static void addAverageUser() {
		averageUsers++;
	}

	public static String getDataSet() {
		return dataSet;
	}

}
