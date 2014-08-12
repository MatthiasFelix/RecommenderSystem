package generator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import rec.Recommender;

/**
 * This class is used to normalize the ratings and clean up the whole last.fm
 * dataset, including the friends file.
 * 
 * @author matthiasfelix
 *
 */
public class DataManipulator {

	private static int[][] data;
	private static TreeMap<Integer, HashMap<Integer, Double>> userItemRatings;

	public static void main(String[] args) {
		userItemRatings = new TreeMap<Integer, HashMap<Integer, Double>>();

		data = readData("lastfm-2k/user_artists.data");
		initializeUserItemRatings(data);

		cleanDataSet();

		cleanUserFriends("lastfm-2k/user_friends.txt", "lastfm-2k/user_friends_n.txt");

		normalizeRatings(userItemRatings, 1, 5);
		writeNormalizedData("lastfm-2k/user_artists_n.data");
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

			// data is of the form: (userID, itemID,rating)_i
			data = new int[N][3];
			int count = 0;
			b = new BufferedReader(new FileReader(fileName));
			while ((line = b.readLine()) != null) {
				String[] s = line.split("\t");
				for (int i = 0; i < 3; i++) {
					try {
						data[count][i] = new Integer(s[i]);
					} catch (NumberFormatException nfe) {
						System.err.println("input data must be of type integer.");
						nfe.printStackTrace();
					}

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

	public static void initializeUserItemRatings(int[][] data) {
		for (int i = 0; i < data.length; i++) {
			if (userItemRatings.containsKey(data[i][0])) {
				userItemRatings.get(data[i][0]).put(data[i][1], (double) data[i][2]);
			} else {
				HashMap<Integer, Double> ratings = new HashMap<Integer, Double>();
				ratings.put(data[i][1], (double) data[i][2]);
				userItemRatings.put(data[i][0], ratings);
			}
		}
	}

	public static void cleanDataSet() {
		// Remove users that have the same rating for each item OR that have
		// less than 10 ratings
		ArrayList<Integer> usersToRemove = new ArrayList<Integer>();
		for (Integer userID : userItemRatings.keySet()) {
			if (findMinimum(userItemRatings.get(userID).values()) == findMaximum(userItemRatings
					.get(userID).values())) {
				usersToRemove.add(userID);
			} else if (userItemRatings.get(userID).size() < 10) {
				usersToRemove.add(userID);
			}
		}

		for (Integer userID : usersToRemove) {
			userItemRatings.remove(userID);
		}

	}

	public static void normalizeRatings(TreeMap<Integer, HashMap<Integer, Double>> userItemRatings,
			double minimum, double maximum) {

		for (Integer userID : userItemRatings.keySet()) {

			double currentMin = findMinimum(userItemRatings.get(userID).values());
			double currentMax = findMaximum(userItemRatings.get(userID).values());

			// Normalize the ratings and put them back into the userItemRatings
			// hash map
			for (Map.Entry<Integer, Double> entry : userItemRatings.get(userID).entrySet()) {
				double newRating;
				if (currentMin == currentMax) {
					newRating = minimum + (double) (maximum - minimum) / 2.;
				} else {
					double normalizingFactor = (double) (entry.getValue() - currentMin)
							/ (double) (currentMax - currentMin);
					newRating = normalizingFactor * (maximum - minimum) + minimum;
				}
				userItemRatings.get(userID).put(entry.getKey(), newRating);
			}

		}

	}

	public static void writeNormalizedData(String fileName) {

		File file = new File("/Users/matthiasfelix/git/RecommenderSystem/RecommenderSystem/"
				+ fileName);
		FileWriter fileWriter;

		try {
			fileWriter = new FileWriter(file);
			BufferedWriter b = new BufferedWriter(fileWriter);

			for (Integer userID : userItemRatings.keySet()) {
				for (Integer itemID : userItemRatings.get(userID).keySet()) {
					b.write(userID + "\t" + itemID + "\t" + userItemRatings.get(userID).get(itemID)
							+ "\n");
				}
			}

			b.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private static double findMaximum(Collection<Double> ratings) {
		double currentMax = Integer.MIN_VALUE;
		for (Double rating : ratings) {
			if (rating > currentMax) {
				currentMax = rating;
			}
		}
		return currentMax;
	}

	private static double findMinimum(Collection<Double> ratings) {
		double currentMin = Integer.MAX_VALUE;
		for (Double rating : ratings) {
			if (rating < currentMin) {
				currentMin = rating;
			}
		}
		return currentMin;
	}

	public static void cleanUserFriends(String oldFileDirection, String newFileName) {

		int[][] friendsList = Recommender.readFriendsList(oldFileDirection);

		File file = new File("/Users/matthiasfelix/git/RecommenderSystem/RecommenderSystem/"
				+ newFileName);
		FileWriter fileWriter;

		try {
			fileWriter = new FileWriter(file);
			BufferedWriter b = new BufferedWriter(fileWriter);

			for (int i = 0; i < friendsList.length; i++) {
				if (userItemRatings.containsKey(friendsList[i][0])
						&& userItemRatings.containsKey(friendsList[i][1])
						&& friendsList[i][0] < friendsList[i][1]) {
					b.write(friendsList[i][0] + "\t" + friendsList[i][1] + "\n");
				}
			}

			b.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
