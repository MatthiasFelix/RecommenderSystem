package generator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

public class RatingGenerator {

	// hash map: userID --> hash map: movieID --> rating
	private static TreeMap<Integer, HashMap<Integer, Double>> userMovieRatings = null;

	// hash map: userID --> Set of users that are friends of this user
	private static TreeMap<Integer, HashSet<Integer>> userFriends = null;

	private static TreeMap<Integer, HashMap<Integer, Double>> friendCorrelations = null;

	private static int[][] friends;

	private static ArrayList<User> userList;
	private static ArrayList<Item> itemList;

	// Parameters to set
	private static final int USERS = 1024;
	private static final int ITEMS = 5000;
	private static final int RATINGS = 100000;
	private static final int MIN = 1;
	private static final int MAX = 5;

	private static Random random;

	public static void main(String[] args) {

		userMovieRatings = new TreeMap<Integer, HashMap<Integer, Double>>();
		userFriends = new TreeMap<Integer, HashSet<Integer>>();
		friendCorrelations = new TreeMap<Integer, HashMap<Integer, Double>>();
		random = new Random();

		friends = readFriendsList("/Users/matthiasfelix/git/RecommenderSystem/RecommenderSystem/artificial/user_friends_n.txt");
		initializeUserFriends();
		System.out.println("friends");
		
		initializeFriendCorrelations();

		userList = new ArrayList<User>();
		itemList = new ArrayList<Item>();

		generateUsers();
		System.out.println("users");
		generateItems();
		System.out.println("items");

		// Debugging
		int uCount = 0, iCount = 0;
		for (User u : userList) {
			uCount += u.getActivity();
			if (u.canStillRate())
				System.out.println(u.getID());
		}
		for (Item i : itemList) {
			iCount += i.getFamosity();
			if (i.canStillBeRated())
				System.out.println(i.getID());
		}
		System.out.println(uCount + " " + iCount);

		generateRatings();
		System.out.println("ratings");
		
		correlateRatings();

		writeRatings("/Users/matthiasfelix/git/RecommenderSystem/RecommenderSystem/artificial/ratings.txt");

	}

	public static void generateUsers() {

		int activity;
		double bias;
		int maxActivity = 3 * (RATINGS / USERS);
		int ratingsLeft = RATINGS;

		for (int i = USERS - 1; i >= 0; i--) {

			activity = random.nextInt(maxActivity - 9) + 10;

			while (ratingsLeft < activity
					|| (ratingsLeft - activity) < (10 * i)
					|| (ratingsLeft - activity) > (maxActivity * i)) {
				activity = random.nextInt(maxActivity - 9) + 10;
			}
			ratingsLeft -= activity;
			bias = random.nextDouble() * (MAX - MIN) - (MAX - MIN) / 1.75;

			User u = new User(activity, bias);
			userList.add(u);
		}

	}

	public static void generateItems() {
		double quality;
		int famosity;
		int maxFamosity = 3 * (RATINGS / ITEMS);
		int ratingsLeft = RATINGS;

		for (int i = ITEMS - 1; i >= 0; i--) {

			famosity = random.nextInt(maxFamosity) + 1;

			while (ratingsLeft < famosity || (ratingsLeft - famosity) < i
					|| (ratingsLeft - famosity) > (maxFamosity * i)) {
				famosity = random.nextInt(maxFamosity) + 1;
			}
			ratingsLeft -= famosity;

			quality = random.nextDouble() * (MAX - MIN) - (MAX - MIN) / 1.75;

			Item item = new Item(quality, famosity);
			itemList.add(item);
		}
	}

	public static void generateRatings() {
		int c = 0;
		for (User u : userList) {
			while (u.canStillRate()) {
				Item i = itemList.get(random.nextInt(itemList.size()));
				u.rate();
				i.rate();
				if (userMovieRatings.containsKey(u.getID())) {
					userMovieRatings.get(u.getID()).put(i.getID(),
							getRating(u, i));
				} else {
					HashMap<Integer, Double> ratings = new HashMap<Integer, Double>();
					ratings.put(i.getID(), getRating(u, i));
					userMovieRatings.put(u.getID(), ratings);
				}
				c++;
				if (!i.canStillBeRated()) {
					itemList.remove(i);
				}
			}
		}
		System.out.println("c: " + c);
	}

	public static double getRating(User user, Item item) {

		double rating = MIN + random.nextDouble() * (MAX - MIN);
		rating = rating + item.getQuality() + user.getBias();

		if (rating > MAX)
			return MAX;
		else if (rating < MIN)
			return MIN;
		else
			return rating;

	}

	public static void writeRatings(String fileName) {

		// Write the ratings from the userMovieRatings HashMap to the output
		// file

		File file = new File(fileName);

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

	public static void initializeFriendCorrelations() {
		for (Integer user : userFriends.keySet()) {
			HashMap<Integer, Double> friends = new HashMap<Integer, Double>();
			for (Integer friend : userFriends.get(user)) {
				friends.put(friend, 0.3);
			}
			friendCorrelations.put(user, friends);
		}
	}

	public static void correlateRatings() {

		for (Integer user : userMovieRatings.keySet()) {
			for (Map.Entry<Integer, Double> entry : userMovieRatings.get(user)
					.entrySet()) {
				for (Integer friend : friendCorrelations.get(user).keySet()) {
					if (userMovieRatings.get(friend)
							.containsKey(entry.getKey())) {
						double difference = entry.getValue()
								- userMovieRatings.get(friend).get(
										entry.getKey());
						entry.setValue(entry.getValue()
								+ friendCorrelations.get(user).get(friend)
								* difference);
					}
				}
			}
		}

	}

	public static void initializeUserFriends() {
		for (int i = 0; i < friends.length; i++) {
			if (userFriends.containsKey(friends[i][0])) {
				userFriends.get(friends[i][0]).add(friends[i][1]);
			} else {
				HashSet<Integer> hs = new HashSet<Integer>();
				hs.add(friends[i][1]);
				userFriends.put(friends[i][0], hs);
			}
			if (userFriends.containsKey(friends[i][1])) {
				userFriends.get(friends[i][1]).add(friends[i][0]);
			} else {
				HashSet<Integer> hs = new HashSet<Integer>();
				hs.add(friends[i][0]);
				userFriends.put(friends[i][1], hs);
			}
		}
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
				String[] s = b.readLine().split("\t");
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

}
