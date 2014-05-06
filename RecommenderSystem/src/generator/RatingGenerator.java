package generator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

public class RatingGenerator {

	// hash map: userID --> hash map: movieID --> rating
	private static TreeMap<Integer, HashMap<Integer, Double>> userMovieRatings = null;

	private static ArrayList<User> userList;
	private static ArrayList<Item> itemList;

	public static void main(String[] args) {

		userMovieRatings = new TreeMap<Integer, HashMap<Integer, Double>>();

		userList = new ArrayList<User>();
		itemList = new ArrayList<Item>();

		// generateRatings(1024, 5000, 10000, 1, 5);

	}

	public static void generateUsers(int users) {
		for (int i = 0; i < users; i++) {
			User u = new User();
			userList.add(u);
		}
	}

	public static void generateItems(int items) {
		Random rnd = new Random();
		for (int i = 0; i < items; i++) {
			Item item = new Item(rnd.nextDouble(), rnd.nextInt(100) + 1);
			itemList.add(item);
		}
	}

	public static void generateRatings(int users, int items, int ratings,
			double min, double max) {

		int user, item;
		double rating;
		Random rnd = new Random();

		// Randomly generate ratings and put them into HashMap
		for (int i = 0; i < ratings; i++) {
			user = rnd.nextInt(users);
			item = rnd.nextInt(items);
			rating = min + ((max - min) * rnd.nextDouble());

			if (!userMovieRatings.containsKey(user)) {
				HashMap<Integer, Double> h = new HashMap<Integer, Double>();
				h.put(item, rating);
				userMovieRatings.put(user, h);
			} else if (!userMovieRatings.get(user).containsKey(item)) {
				userMovieRatings.get(user).put(item, rating);
			} else {
				// A user cannot rate the same item twice --> we have to
				// generate one more rating
				i--;
			}

		}

		// Write the ratings from the userMovieRatings HashMap to the output
		// file

		File file = new File(
				"/Users/matthiasfelix/git/RecommenderSystem/RecommenderSystem/artificial/ratings.txt");

		FileWriter fileWriter;

		try {

			fileWriter = new FileWriter(file);
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

			for (Integer userID : userMovieRatings.keySet()) {
				for (Map.Entry<Integer, Double> entry : userMovieRatings.get(
						userID).entrySet()) {
					bufferedWriter.write(userID + "\t" + entry.getKey() + "\t"
							+ entry.getValue() + "\n");
				}
			}

			bufferedWriter.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
