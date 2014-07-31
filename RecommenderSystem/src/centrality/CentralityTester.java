package centrality;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import rec.Data;
import rec.Recommender;
import rec.Similarity;
import cwrapper.CWrapper;

public class CentralityTester {

	private static TreeMap<Integer, Integer> userCommunity;
	private static HashMap<Integer, Double> userCentrality;
	private static HashMap<Integer, HashSet<Integer>> communityUsers;
	private static HashMap<Integer, HashSet<Integer>> communityInfluencers;

	private static HashMap<Integer, Double> userErrorDifferences;

	private static HashMap<Integer, LinkedHashMap<Integer, Double>> userSimilarities;

	private static String userFriends = "/Users/matthiasfelix/git/RecommenderSystem/RecommenderSystem/lastfm-2k/user_friends_n.txt";
	private static String userRatings = "/Users/matthiasfelix/git/RecommenderSystem/RecommenderSystem/lastfm-2k/user_artists_n.data";

	private static Comparator<Map.Entry<Integer, Double>> comparator;

	private static Data data;

	private static int[] influencerSizes = new int[100];

	private static double[][] results = new double[influencerSizes.length][3];

	public static void main(String[] args) {

		for (int i = 0; i < 100; i++)
			influencerSizes[i] = i + 1;

		userCentrality = new HashMap<Integer, Double>();
		userCommunity = new TreeMap<Integer, Integer>();
		communityUsers = new HashMap<Integer, HashSet<Integer>>();
		communityInfluencers = new HashMap<Integer, HashSet<Integer>>();

		userErrorDifferences = new HashMap<Integer, Double>();

		userSimilarities = new HashMap<Integer, LinkedHashMap<Integer, Double>>();

		comparator = new Comparator<Map.Entry<Integer, Double>>() {
			public int compare(Entry<Integer, Double> o1, Entry<Integer, Double> o2) {
				return (o1.getValue().compareTo(o2.getValue()));
			}

		};

		readUserErrorDifferences("/Users/matthiasfelix/git/RecommenderSystem/RecommenderSystem/results/userErrors01.txt");

		ArrayList<Map.Entry<Integer, Double>> sortedErrorDifferences = new ArrayList<Map.Entry<Integer, Double>>(
				userErrorDifferences.entrySet());
		Collections.sort(sortedErrorDifferences, comparator);

		setUpCommunities();

		for (Map.Entry<Integer, HashSet<Integer>> entry : communityUsers.entrySet()) {
			System.out.print("Key: " + entry.getKey() + ", Size: " + entry.getValue().size()
					+ "\t\t");
			for (Integer u : entry.getValue()) {
				System.out.print(u + ",");
			}
			System.out.println();
		}

		// Sort users by centrality (biggest centrality first in the list)
		ArrayList<Map.Entry<Integer, Double>> sortedUserCentralities = new ArrayList<Map.Entry<Integer, Double>>(
				userCentrality.entrySet());
		Collections.sort(sortedUserCentralities, Collections.reverseOrder(comparator));

		ArrayList<Integer> errorUsers = new ArrayList<Integer>();
		for (Map.Entry<Integer, Double> entry : sortedErrorDifferences) {
			errorUsers.add(entry.getKey());
		}

		ArrayList<Integer> centralityUsers = new ArrayList<Integer>();
		for (Map.Entry<Integer, Double> entry : sortedUserCentralities) {
			centralityUsers.add(entry.getKey());
		}

		for (Integer user : userCommunity.keySet()) {
			System.out.println("Error: " + errorUsers.indexOf(user) + ", Centrality: "
					+ centralityUsers.indexOf(user));
		}

		System.out.println("\n\n SIZES: " + sortedErrorDifferences.size() + "   "
				+ sortedUserCentralities.size() + "\n\n");

		double[][] ratings = Recommender.readData(userRatings);
		int[][] friends = Recommender.readFriendsList(userFriends);

		data = new Data(ratings, friends, userFriends, userRatings);

		calculateSimilarities();

		// Make the test runs
		for (int i : influencerSizes) {
			System.out.println("i=" + i);
			runInfluencerTest(sortedUserCentralities, i);
		}
		System.out.println("Inf\tCom\tNet");
		for (double[] res : results) {
			System.out.println(res[0] + "\t" + res[1] + "\t" + res[2]);
		}

	}

	public static void runInfluencerTest(
			ArrayList<Map.Entry<Integer, Double>> sortedUserCentralities, int influencerSize) {

		for (Map.Entry<Integer, HashSet<Integer>> entry : communityUsers.entrySet()) {
			HashSet<Integer> hs = new HashSet<Integer>();
			communityInfluencers.put(entry.getKey(), hs);
			for (Map.Entry<Integer, Double> sortedUser : sortedUserCentralities) {
				if (entry.getValue().contains(sortedUser.getKey())
						&& communityInfluencers.get(entry.getKey()).size() < influencerSize) {
					communityInfluencers.get(entry.getKey()).add(sortedUser.getKey());
				}
			}
		}

		// First entry in the ArrayList is correlation with influencer, second
		// entry is correlation with rest of community, third entry is
		// correlation with rest of network
		HashMap<Integer, ArrayList<Double>> correlations = new HashMap<Integer, ArrayList<Double>>();

		for (int user : userCommunity.keySet()) {
			if (!communityInfluencers.get(userCommunity.get(user)).contains(user)) {
				ArrayList<Double> corrs = new ArrayList<Double>();

				int counter = 0;
				double influencerCorr = 0.0;
				for (int infMember : communityInfluencers.get(userCommunity.get(user))) {
					influencerCorr += userSimilarities.get(user).get(infMember);
					counter++;
				}
				influencerCorr /= counter;
				corrs.add(influencerCorr);

				counter = 0;
				double restOfCommunity = 0.0;
				for (int comMember : communityUsers.get(userCommunity.get(user))) {
					if (comMember != user
							&& !communityInfluencers.get(userCommunity.get(user)).contains(
									comMember)) {
						restOfCommunity += userSimilarities.get(user).get(comMember);
						counter++;
					}
				}
				restOfCommunity /= counter;
				corrs.add(restOfCommunity);

				counter = 0;
				double restOfNetwork = 0.0;
				for (Integer networkMember : userCommunity.keySet()) {
					if (networkMember != user
							&& !communityUsers.get(userCommunity.get(user)).contains(networkMember)) {
						restOfNetwork += userSimilarities.get(user).get(networkMember);
						counter++;
					}
				}
				restOfNetwork /= counter;
				corrs.add(restOfNetwork);
				correlations.put(user, corrs);
			}
		}

		double influencerAverage = 0.0;
		double communityAverage = 0.0;
		double networkAverage = 0.0;

		double influencerCommunityDifference = 0.0;
		double influencerNetworkDifference = 0.0;
		double communityNetworkDifference = 0.0;
		int counter = 0;

		DecimalFormat df = new DecimalFormat("#.####");

		for (Map.Entry<Integer, ArrayList<Double>> entry : correlations.entrySet()) {
			if (Double.isNaN(entry.getValue().get(1))) {
				// Community only consists of that user and the influencer
				continue;
			}
			influencerAverage += entry.getValue().get(0);
			communityAverage += entry.getValue().get(1);
			networkAverage += entry.getValue().get(2);

			influencerCommunityDifference += (entry.getValue().get(0) - entry.getValue().get(1));
			influencerNetworkDifference += (entry.getValue().get(0) - entry.getValue().get(2));
			communityNetworkDifference += (entry.getValue().get(1) - entry.getValue().get(2));

			counter++;
		}

		System.out.println("influencerAverage: " + df.format(influencerAverage / counter));
		System.out.println("communityAverage: " + df.format(communityAverage / counter));
		System.out.println("networkAverage: " + df.format(networkAverage / counter));

		results[influencerSize - 1][0] = influencerAverage / counter;
		results[influencerSize - 1][1] = communityAverage / counter;
		results[influencerSize - 1][2] = networkAverage / counter;

		System.out.println("influencer-community: "
				+ df.format(influencerCommunityDifference / counter));
		System.out.println("influencer-network: "
				+ df.format(influencerNetworkDifference / counter));
		System.out.println("community-network: " + df.format(communityNetworkDifference / counter));
	}

	private static void readUserErrorDifferences(String fileName) {

		try {
			BufferedReader bf = new BufferedReader(new FileReader(fileName));
			String line;
			while ((line = bf.readLine()) != null) {
				String[] s = line.split("\t");
				userErrorDifferences.put(Integer.parseInt(s[0]), Double.parseDouble(s[1]));
			}

			bf.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static void calculateSimilarities() {
		double sim;

		for (Integer user1 : userCommunity.keySet()) {
			for (Integer user2 : userCommunity.tailMap(user1, false).keySet()) {

				sim = computeSimilarity(user1, user2);

				// this is for user1's list
				if (userSimilarities.containsKey(user1)) {
					userSimilarities.get(user1).put(user2, sim);
				} else {
					LinkedHashMap<Integer, Double> s = new LinkedHashMap<Integer, Double>();
					s.put(user2, sim);
					userSimilarities.put(user1, s);
				}

				// this is for user2's list
				if (userSimilarities.containsKey(user2)) {
					userSimilarities.get(user2).put(user1, sim);
				} else {
					LinkedHashMap<Integer, Double> s = new LinkedHashMap<Integer, Double>();
					s.put(user1, sim);
					userSimilarities.put(user2, s);
				}

			}
		}
	}

	public static void setUpCommunities() {
		CWrapper cwrapper = CWrapper.getInstance();
		double[] communitiesDouble = cwrapper.getCommunities(userFriends, 1);
		double[] centralities = cwrapper.getNormalizedCentrality(userFriends, 0);
		double[] degreesDouble = cwrapper.getCentrality(userFriends, 0);

		int[] communities = new int[communitiesDouble.length];
		for (int i = 0; i < communitiesDouble.length; i++) {
			communities[i] = (int) Math.round(communitiesDouble[i]);
		}

		int[] degrees = new int[degreesDouble.length];
		for (int i = 0; i < degreesDouble.length; i++) {
			degrees[i] = (int) Math.round(degreesDouble[i]);
		}

		System.out.println("com: " + communities.length + ", cent: " + centralities.length
				+ ", degree: " + degrees.length);

		for (int i = 0; i < communities.length; i++) {
			userCommunity.put(i, communities[i]);
			userCentrality.put(i, centralities[i]);
			if (communityUsers.containsKey(communities[i])) {
				communityUsers.get(communities[i]).add(i);
			} else {
				HashSet<Integer> users = new HashSet<Integer>();
				users.add(i);
				communityUsers.put(communities[i], users);
			}
		}

		// Remove users without friends
		for (int i = 0; i < degrees.length; i++) {
			if (degrees[i] == 0) {
				userCommunity.remove(i);
				userCentrality.remove(i);
				ArrayList<Integer> toRemove = new ArrayList<Integer>();
				for (Integer community : communityUsers.keySet()) {
					if (communityUsers.get(community).contains(i)) {
						communityUsers.get(community).remove(i);
						if (communityUsers.get(community).size() == 0) {
							toRemove.add(community);
						}
					}
				}
				for (Integer j : toRemove) {
					communityUsers.remove(j);
				}
			}
		}
	}

	public static ArrayList<Integer> getUsers(String fileName) {
		ArrayList<Integer> users = new ArrayList<Integer>();

		try {

			// Count the number of friend relations
			BufferedReader b = new BufferedReader(new FileReader(fileName));

			String s;
			while ((s = b.readLine()) != null) {
				int u = Integer.parseInt(s.split("\t")[0]);
				if (!users.contains(u))
					users.add(u);
			}

			b.close();

		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

		return users;
	}

	/**
	 * Computes the similarity between user1 and user2 using the specified
	 * similarity coefficient
	 * 
	 * @param user1
	 * @param user2
	 * @return similarity
	 */
	public static double computeSimilarity(int user1, int user2) {

		double sim = 0;

		// Create list with all items rated by both user1 and user2
		ArrayList<Integer> sharedItems = new ArrayList<Integer>();
		for (Integer item : data.getItemsByUser().get(user1)) {
			if (data.getItemsByUser().get(user2).contains(item)) {
				sharedItems.add(item);
			}
		}

		// Create the rating vectors for both users
		double[] ratingsUser1 = new double[sharedItems.size()];
		double[] ratingsUser2 = new double[sharedItems.size()];

		int i = 0;
		for (Integer item : sharedItems) {
			ratingsUser1[i] = data.getUserItemRatings().get(user1).get(item);
			ratingsUser2[i] = data.getUserItemRatings().get(user2).get(item);
			i++;
		}

		sim = Similarity.calculateCosineSimilarity(ratingsUser1, ratingsUser2);

		return sim;
	}
}