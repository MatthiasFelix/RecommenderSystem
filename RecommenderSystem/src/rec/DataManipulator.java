package rec;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class DataManipulator {

	private static int[][] data;
	private static TreeMap<Integer, HashMap<Integer, Double>> userMovieRatings;

	public static void main(String[] args) {
		userMovieRatings = new TreeMap<Integer, HashMap<Integer, Double>>();

		data = readData("lastfm-2k/user_artists.data");
		initializeUserMovieRatings(data);
		normalizeRatings(userMovieRatings, 1, 5);
		writeNormalizedData("lastfm-2k/user_artists_n.data");
	}

	public static void initializeUserMovieRatings(int[][] data) {
		for (int i = 0; i < data.length; i++) {
			if (userMovieRatings.containsKey(data[i][0])) {
				userMovieRatings.get(data[i][0]).put(data[i][1],
						(double) data[i][2]);
			} else {
				HashMap<Integer, Double> ratings = new HashMap<Integer, Double>();
				ratings.put(data[i][1], (double) data[i][2]);
				userMovieRatings.put(data[i][0], ratings);
			}
		}
	}

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

	public static void normalizeRatings(
			TreeMap<Integer, HashMap<Integer, Double>> userMovieRatings,
			double minimum, double maximum) {

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
				if (currentMin == currentMax) {
					newRating = minimum + (double) (maximum - minimum) / 2.;
				} else {
					double normalizingFactor = (double) (entry.getValue() - currentMin)
							/ (double) (currentMax - currentMin);
					newRating = normalizingFactor * (maximum - minimum)
							+ minimum;
				}
				System.out.println("Replace " + entry.getValue() + " with "
						+ newRating);
				userMovieRatings.get(userID).put(entry.getKey(), newRating);
			}

		}

	}

	public static void writeNormalizedData(String fileName) {
		File file = new File(
				"/Users/matthiasfelix/git/RecommenderSystem/RecommenderSystem/"
						+ fileName);
		FileWriter fileWriter;

		try {
			fileWriter = new FileWriter(file);
			BufferedWriter b = new BufferedWriter(fileWriter);

			for (Integer userID : userMovieRatings.keySet()) {
				for (Integer movieID : userMovieRatings.get(userID).keySet()) {
					b.write(userID + "\t" + movieID + "\t"
							+ userMovieRatings.get(userID).get(movieID) + "\n");
				}
			}

			b.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
