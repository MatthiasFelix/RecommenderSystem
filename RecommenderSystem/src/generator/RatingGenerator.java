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

import org.uncommons.maths.random.ExponentialGenerator;
import org.uncommons.maths.random.GaussianGenerator;
import org.uncommons.maths.random.MersenneTwisterRNG;

import cwrapper.CWrapper;

public class RatingGenerator {

	// hash map: userID --> hash map: itemID --> rating
	private static TreeMap<Integer, HashMap<Integer, Double>> userItemRatings = null;

	// hash map: userID --> Set of users that are friends of this user
	private static TreeMap<Integer, HashSet<Integer>> userFriends = null;

	// hash map: userID --> hash map: userID --> correlation between the two
	// users
	private static TreeMap<Integer, HashMap<Integer, Double>> friendCorrelations = null;

	// hash map: communityID --> hash map: itemID --> taste
	private static TreeMap<Integer, HashMap<Integer, Double>> communityTaste = null;

	// hash map: userID --> hash map: itemID --> taste
	private static TreeMap<Integer, HashMap<Integer, Double>> userTaste = null;

	// hash map: itemID --> quality
	private static TreeMap<Integer, Double> itemQuality = null;

	// hash map: userID --> community the user belongs to
	private static TreeMap<Integer, Integer> userCommunity = null;

	// hash map: userID --> hash map: itemID --> probability that user likes
	// this item
	private static TreeMap<Integer, HashMap<Integer, Double>> probabilities = null;

	// hash map: userID --> number of items user will rate
	private static TreeMap<Integer, Integer> userListeningCounts = null;

	// hash map: communityID --> Set of items that this community rates more
	// often
	private static TreeMap<Integer, ArrayList<Integer>> communityItems = null;

	private static int[][] friends;
	private static int[] communityStructure;

	private static ArrayList<Integer> userList;
	private static ArrayList<Integer> itemList;

	// Parameters to set
	private static final int ITEMS = 3000;
	private static final int MIN = 1;
	private static final int MAX = 5;
	private static String friendsFile = "/Users/matthiasfelix/git/RecommenderSystem/RecommenderSystem/artificial/friendsA01.txt";
	private static String ratingFile = "/Users/matthiasfelix/git/RecommenderSystem/RecommenderSystem/artificial/RATINGS_290714_1.txt";
	private static double a = 1. / 3.;
	private static double b = 0.0;
	private static double p = 1. / 3.;
	private static int numberOfCommunityItems = 100;

	private static Random random;
	private static GaussianGenerator gg;
	private static ExponentialGenerator eg;
	private static MersenneTwisterRNG mt;

	public static void main(String[] args) {

		if (args.length != 0) {
			friendsFile = args[0];
			ratingFile = args[1];
		}
		if (args.length > 2) {
			numberOfCommunityItems = Integer.parseInt(args[2]);
			a = Double.parseDouble(args[3]);
			b = Double.parseDouble(args[4]);
			p = Double.parseDouble(args[5]);
		}

		// Initializations
		userItemRatings = new TreeMap<Integer, HashMap<Integer, Double>>();
		userFriends = new TreeMap<Integer, HashSet<Integer>>();
		friendCorrelations = new TreeMap<Integer, HashMap<Integer, Double>>();
		communityTaste = new TreeMap<Integer, HashMap<Integer, Double>>();
		userTaste = new TreeMap<Integer, HashMap<Integer, Double>>();
		itemQuality = new TreeMap<Integer, Double>();
		userCommunity = new TreeMap<Integer, Integer>();
		probabilities = new TreeMap<Integer, HashMap<Integer, Double>>();
		userListeningCounts = new TreeMap<Integer, Integer>();
		communityItems = new TreeMap<Integer, ArrayList<Integer>>();

		random = new Random(System.currentTimeMillis());
		gg = new GaussianGenerator(0.5, 0.13, random);
		eg = new ExponentialGenerator(0.7, random);
		mt = new MersenneTwisterRNG();

		// Generate the graph and store the resulting community structure
		generateGraphAndCommunityStructure();
		System.out.println("graph and communities");

		friends = readFriendsList(friendsFile);
		initializeUserFriends();
		System.out.println("friends");

		userList = new ArrayList<Integer>();
		for (Integer userID : userFriends.keySet()) {
			userList.add(userID);
		}

		itemList = new ArrayList<Integer>();
		for (int i = 0; i < ITEMS; i++) {
			itemList.add(i);
		}

		// Generate itemQuality, userTaste and communityTaste
		generateTastes();
		normalizeTastes();

		initializeCommunityItems();

		// Initialize user listening counts (following a Gaussian distribution)

		int c = 0;
		for (Integer userID : userList) {
			userListeningCounts.put(userID, (int) (nextGaussianValue() * 200));
			System.out.println(userListeningCounts.get(userID));
			c += userListeningCounts.get(userID);
		}
		System.out.println("number of listeningcounts: " + c);
		System.out.println("userlisteningcounts");

		generateRatings();
		System.out.println("generate ratings");

		writeRatings(ratingFile);
		System.out.println("write ratings");

	}

	public static void initializeCommunityItems() {
		for (Integer communityID : communityTaste.keySet()) {
			ArrayList<Integer> items = new ArrayList<Integer>();
			while (items.size() < numberOfCommunityItems) {
				int newItem = mt.nextInt(ITEMS);
				if (!items.contains(newItem)) {
					items.add(mt.nextInt(ITEMS));
				}
			}
			communityItems.put(communityID, items);
		}
	}

	public static void normalizeTastes() {
		// Normalize itemQuality
		double itemMin = Double.MAX_VALUE, itemMax = Double.MIN_VALUE, factor;
		for (Double quality : itemQuality.values()) {
			if (quality < itemMin)
				itemMin = quality;
			if (quality > itemMax)
				itemMax = quality;
		}
		for (Map.Entry<Integer, Double> entry : itemQuality.entrySet()) {
			factor = (entry.getValue() - itemMin) / (double) (itemMax - itemMin);
			itemQuality.put(entry.getKey(), factor * (MAX - MIN) + MIN);
		}

		// Normalize userTaste
		for (Integer userID : userTaste.keySet()) {
			double userMin = Double.MAX_VALUE, userMax = Double.MIN_VALUE;
			for (Double taste : userTaste.get(userID).values()) {
				if (taste < userMin)
					userMin = taste;
				if (taste > userMax)
					userMax = taste;
			}
			for (Map.Entry<Integer, Double> entry : userTaste.get(userID).entrySet()) {
				factor = (entry.getValue() - userMin) / (double) (userMax - userMin);
				userTaste.get(userID).put(entry.getKey(), factor * (MAX - MIN) + MIN);
			}
		}

		// Normalize communityTaste
		for (Integer communityID : communityTaste.keySet()) {
			double communityMin = Double.MAX_VALUE, communityMax = Double.MIN_VALUE;
			for (Double taste : communityTaste.get(communityID).values()) {
				if (taste < communityMin)
					communityMin = taste;
				if (taste > communityMax)
					communityMax = taste;
			}
			for (Map.Entry<Integer, Double> entry : communityTaste.get(communityID).entrySet()) {
				factor = (entry.getValue() - communityMin) / (double) (communityMax - communityMin);
				communityTaste.get(communityID).put(entry.getKey(), factor * (MAX - MIN) + MIN);
			}
		}
	}

	public static void generateRatings() {

		for (Integer userID : userListeningCounts.keySet()) {
			HashMap<Integer, Double> userRatings = new HashMap<Integer, Double>();
			double r;
			while (userRatings.size() < userListeningCounts.get(userID)) {
				r = mt.nextDouble();
				int chosenItem;

				// boolean allCommunityItemsRated = true;
				//
				// for (Integer item : communityItems.get(userID)) {
				// if (!userRatings.containsKey(item)) {
				// allCommunityItemsRated = false;
				// System.out.println("Item " + item);
				// break;
				// }
				// }
				//
				// System.out.println(allCommunityItemsRated);

				// If all community items are already rated, choose from
				// individual set
				// if (allCommunityItemsRated) {
				// chosenItem = mt.nextInt(ITEMS); } else
				if (r < p) { // from individual set
					chosenItem = mt.nextInt(ITEMS);
				} else { // from community set
					int k = mt.nextInt(communityItems.get(userCommunity.get(userID)).size());
					chosenItem = communityItems.get(userCommunity.get(userID)).get(k);
				}

				if (!userRatings.containsKey(chosenItem)) {
					double c = 1 - a - b;
					double rating = a * itemQuality.get(chosenItem) + b
							* userTaste.get(userID).get(chosenItem) + c
							* communityTaste.get(userCommunity.get(userID)).get(chosenItem);
					userRatings.put(chosenItem, rating);
				}
			}
			userItemRatings.put(userID, userRatings);
		}

	}

	public static void generateProbabilities() {
		for (Integer userID : userList) {
			HashMap<Integer, Double> userProbabilities = new HashMap<Integer, Double>();
			for (Integer itemID : itemList) {
				userProbabilities.put(itemID, generateRating(userID, itemID));
			}
			probabilities.put(userID, userProbabilities);
		}
		System.out.println("probabilities");
	}

	public static void generateTastes() {

		for (Integer itemID : itemList) {
			itemQuality.put(itemID, nextExponentialValue());
		}
		System.out.println("itemQuality");

		// Generate community taste
		for (Integer communityID : communityStructure) {
			if (!communityTaste.containsKey(communityID)) {
				HashMap<Integer, Double> itemTastes = new HashMap<Integer, Double>();
				for (Integer itemID : itemList) {
					itemTastes.put(itemID, nextExponentialValue());
				}
				communityTaste.put(communityID, itemTastes);
			}
		}
		System.out.println("community taste");

		// Generate user taste
		for (Integer userID : userList) {
			HashMap<Integer, Double> userTastes = new HashMap<Integer, Double>();
			for (Integer itemID : itemList) {
				userTastes.put(itemID, nextExponentialValue());
			}
			userTaste.put(userID, userTastes);
		}
		System.out.println("user taste");
	}

	public static void generateGraphAndCommunityStructure() {
		// CWrapper cwrapper = CWrapper.getInstance();
		// double[] communities = cwrapper.generateGraph(friendsFile, 1500, 9,
		// 5, 3, 100);

		// Hardcoded, so always the same network is used.
		double[] communities = { 6, 6, 6, 6, 0, 6, 3, 6, 6, 0, 6, 6, 2, 6, 92, 6, 6, 45, 45, 6, 45,
				6, 93, 0, 94, 6, 6, 95, 6, 6, 6, 6, 6, 6, 49, 91, 0, 91, 22, 6, 6, 10, 96, 6, 6,
				14, 6, 14, 0, 6, 6, 9, 6, 6, 11, 52, 0, 89, 6, 89, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 2,
				2, 6, 6, 6, 25, 0, 6, 6, 6, 6, 6, 6, 6, 47, 97, 28, 6, 0, 98, 6, 19, 6, 6, 6, 6, 6,
				6, 11, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 12, 8, 8, 8, 6, 34, 18, 6, 6, 5, 5, 6, 6, 6,
				12, 6, 9, 1, 6, 43, 0, 6, 25, 90, 90, 6, 14, 2, 11, 11, 99, 14, 27, 6, 6, 0, 6, 26,
				15, 6, 100, 0, 6, 6, 52, 7, 6, 14, 14, 101, 1, 6, 6, 37, 102, 19, 6, 19, 103, 104,
				105, 53, 0, 61, 20, 61, 61, 106, 0, 0, 6, 23, 30, 107, 6, 6, 6, 6, 6, 108, 6, 52,
				6, 6, 109, 88, 88, 110, 6, 6, 0, 6, 6, 6, 111, 35, 86, 86, 26, 112, 6, 6, 6, 3,
				113, 6, 2, 2, 2, 2, 6, 6, 6, 1, 2, 85, 85, 114, 6, 2, 6, 6, 115, 49, 6, 6, 6, 6, 6,
				6, 0, 0, 6, 116, 6, 44, 44, 6, 6, 6, 6, 117, 118, 119, 120, 22, 17, 47, 65, 6, 34,
				14, 6, 6, 1, 8, 6, 30, 6, 0, 6, 6, 0, 0, 16, 12, 12, 121, 6, 6, 6, 0, 6, 51, 51,
				31, 6, 6, 6, 21, 6, 122, 60, 6, 46, 2, 6, 6, 6, 0, 0, 39, 12, 6, 13, 6, 1, 6, 17,
				7, 123, 82, 82, 59, 4, 6, 6, 19, 63, 6, 32, 32, 6, 84, 124, 84, 2, 9, 6, 35, 125,
				18, 2, 6, 16, 6, 14, 6, 6, 126, 127, 22, 128, 129, 6, 21, 6, 6, 83, 83, 130, 6, 8,
				17, 131, 6, 0, 17, 14, 0, 0, 132, 14, 19, 34, 48, 48, 6, 6, 11, 3, 6, 6, 133, 6, 5,
				64, 64, 64, 6, 134, 51, 10, 6, 135, 6, 6, 136, 137, 24, 6, 6, 50, 50, 6, 6, 138,
				59, 139, 140, 37, 14, 141, 57, 57, 13, 142, 143, 144, 38, 75, 33, 75, 18, 18, 6,
				145, 146, 8, 2, 12, 6, 6, 77, 77, 12, 22, 25, 6, 81, 81, 147, 148, 149, 7, 6, 150,
				151, 6, 152, 2, 153, 0, 0, 6, 154, 14, 155, 156, 157, 6, 63, 63, 41, 6, 26, 6, 6,
				16, 6, 58, 12, 12, 0, 34, 6, 29, 29, 158, 54, 6, 80, 80, 6, 43, 54, 159, 6, 79, 79,
				2, 2, 6, 6, 2, 1, 24, 55, 31, 6, 31, 6, 6, 0, 160, 161, 162, 14, 6, 163, 164, 71,
				71, 71, 71, 165, 166, 10, 10, 6, 6, 167, 168, 169, 14, 170, 13, 74, 74, 171, 78,
				78, 3, 3, 36, 172, 20, 14, 27, 38, 60, 173, 26, 26, 76, 76, 76, 26, 1, 174, 41, 6,
				6, 19, 14, 175, 0, 62, 62, 176, 14, 6, 177, 6, 6, 6, 178, 37, 179, 180, 15, 6, 6,
				6, 6, 23, 6, 39, 181, 65, 182, 6, 6, 183, 6, 6, 6, 34, 6, 14, 184, 0, 16, 0, 53, 6,
				185, 186, 66, 56, 14, 14, 0, 1, 25, 6, 72, 72, 0, 0, 9, 6, 6, 50, 187, 0, 188, 73,
				73, 9, 9, 10, 6, 0, 189, 190, 191, 192, 193, 194, 6, 6, 70, 70, 40, 40, 195, 9,
				196, 197, 22, 42, 42, 42, 1, 1, 198, 6, 12, 15, 199, 25, 200, 201, 6, 202, 6, 6,
				14, 66, 203, 6, 204, 205, 69, 69, 6, 49, 6, 6, 58, 58, 206, 6, 6, 14, 207, 6, 208,
				209, 210, 28, 14, 7, 30, 4, 16, 211, 6, 212, 6, 213, 214, 215, 2, 216, 46, 46, 217,
				67, 67, 2, 6, 218, 7, 219, 6, 220, 37, 221, 68, 68, 6, 6, 1, 222, 223, 224, 5, 225,
				0, 6, 6, 6, 226, 227, 228, 229, 230, 12, 12, 33, 56, 6, 36, 231, 6, 232, 233, 3,
				234, 6, 235, 236, 237, 6, 238, 6, 6, 239, 240, 6, 55, 241, 6, 6, 0, 242, 243, 6, 6,
				1, 244, 18, 22, 245, 6, 246, 247, 248, 6, 87, 87 };

		communityStructure = new int[communities.length];
		for (int i = 0; i < communities.length; i++) {
			communityStructure[i] = (int) Math.round(communities[i]);
		}

		for (int i = 0; i < communityStructure.length; i++) {
			userCommunity.put(i, communityStructure[i]);
		}
	}

	public static void writeRatings(String fileName) {

		// Write the ratings from the userItemRatings HashMap to the output
		// file

		File file = new File(fileName);

		FileWriter fileWriter;

		try {

			fileWriter = new FileWriter(file);
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

			for (Integer userID : userItemRatings.keySet()) {
				for (Map.Entry<Integer, Double> entry : userItemRatings.get(userID).entrySet()) {
					bufferedWriter.write(userID + "\t" + entry.getKey() + "\t" + entry.getValue()
							+ "\n");
				}
			}

			bufferedWriter.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void initializeUserFriends() {
		for (int i = 0; i < friends.length; i++) {
			if (friends[i][0] == friends[i][1]) { // User with no friends
				HashSet<Integer> hs = new HashSet<Integer>();
				userFriends.put(friends[i][0], hs);
			} else { // User with friends
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
				String[] s = b.readLine().split(" ");
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

	public static double generateRating(int userID, int itemID) {
		// According to softmax-function
		double c = 1 - a - b;
		double quality = itemQuality.get(itemID);
		double usertaste = userTaste.get(userID).get(itemID);
		double communitytaste = communityTaste.get(userCommunity.get(userID)).get(itemID);

		double num = Math.exp(a * quality + b * usertaste + c * communitytaste);

		double denom = 0.0;

		for (Integer item : itemList) {
			denom += Math.exp(a * itemQuality.get(item) + b * userTaste.get(userID).get(item) + c
					* communityTaste.get(userCommunity.get(userID)).get(item));
		}

		double rating = num / denom;

		return rating;

	}

	public static double nextGaussianValue() {
		double value;
		do {
			value = gg.nextValue();
		} while (value < 0 || value > 1);
		// return value * (MAX - MIN) + MIN;
		return value;
	}

	public static double nextExponentialValue() {
		double value = eg.nextValue();
		if (value > MAX) {
			return MAX;
		}
		return value;
	}

	public static double nextUniformValue() {
		double value = mt.nextDouble() * (MAX - MIN) + MIN;
		return value;
	}

}
