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

/**
 * This class is used to artificially generate ratings.
 * 
 * @author matthiasfelix
 *
 */
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
	private static String ratingFile = "/Users/matthiasfelix/git/RecommenderSystem/RecommenderSystem/artificial/RATINGS_040814_3_9.txt";
	private static double a = 0. / 3.;
	private static double b = 1. / 3.;
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

		friends = readFriendsList(friendsFile);
		initializeUserFriends();

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

		for (Integer userID : userList) {
			userListeningCounts.put(userID, (int) (nextGaussianValue() * 540));
		}

		generateRatings();

		writeRatings(ratingFile);

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
	}

	public static void generateTastes() {

		for (Integer itemID : itemList) {
			itemQuality.put(itemID, nextExponentialValue());
		}

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

		// Generate user taste
		for (Integer userID : userList) {
			HashMap<Integer, Double> userTastes = new HashMap<Integer, Double>();
			for (Integer itemID : itemList) {
				userTastes.put(itemID, nextExponentialValue());
			}
			userTaste.put(userID, userTastes);
		}
	}

	public static void generateGraphAndCommunityStructure() {
		CWrapper cwrapper = CWrapper.getInstance();
		double[] communities = cwrapper.generateGraph(friendsFile, 1500, 9, 5, 3, 100);

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
