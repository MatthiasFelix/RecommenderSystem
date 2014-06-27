package rec;

import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;

public class Data {

	double[][] data = null;
	int[][] friends = null;

	// hash map: userID --> hash map: itemID --> rating
	private TreeMap<Integer, HashMap<Integer, Double>> userItemRatings = null;

	// hash map: userID --> Set of items that the user has rated
	private TreeMap<Integer, HashSet<Integer>> itemsByUser = null;

	// hash map: itemID --> Set of users that have rated the item
	private TreeMap<Integer, HashSet<Integer>> usersByItem = null;

	// hash map: userID --> average rating of that user
	private HashMap<Integer, Double> averageUserRatings = null;

	// hash map: itemID --> average rating of that item
	private HashMap<Integer, Double> averageItemRatings = null;

	// hash map: userID --> Set of users that are friends of this user
	private TreeMap<Integer, HashSet<Integer>> userFriends = null;

	// Data can be initialized either with or without social network data
	// (friend relations)
	public Data(double[][] inputData, int[][] inputFriends) {
		friends = inputFriends;
		data = inputData;
	}

	public Data(double[][] inputData) {
		data = inputData;
	}

	public void initializeUserItemRatings() {
		userItemRatings = new TreeMap<Integer, HashMap<Integer, Double>>();
		for (int i = 0; i < data.length; i++) {
			if (userItemRatings.containsKey((int) data[i][0])) {
				userItemRatings.get((int) data[i][0]).put((int) data[i][1],
						data[i][2]);
			} else {
				HashMap<Integer, Double> ratings = new HashMap<Integer, Double>();
				ratings.put((int) data[i][1], data[i][2]);
				userItemRatings.put((int) data[i][0], ratings);
			}
		}
	}

	public void initializeItemsByUser() {
		itemsByUser = new TreeMap<Integer, HashSet<Integer>>();
		for (int i = 0; i < data.length; i++) {
			if (itemsByUser.containsKey((int) data[i][0])) {
				itemsByUser.get((int) data[i][0]).add((int) data[i][1]);
			} else {
				HashSet<Integer> items = new HashSet<Integer>();
				items.add((int) data[i][1]);
				itemsByUser.put((int) data[i][0], items);
			}

		}
	}

	public void initializeUsersByItem() {
		usersByItem = new TreeMap<Integer, HashSet<Integer>>();
		for (int i = 0; i < data.length; i++) {
			if (usersByItem.containsKey((int) data[i][1])) {
				usersByItem.get((int) data[i][1]).add((int) data[i][0]);
			} else {
				HashSet<Integer> users = new HashSet<Integer>();
				users.add((int) data[i][0]);
				usersByItem.put((int) data[i][1], users);
			}
		}
	}

	public void initializeAverageUserRatings() {
		averageUserRatings = new HashMap<Integer, Double>();
		for (Integer userID : getItemsByUser().keySet()) {
			double rating = 0;
			for (Integer itemID : getItemsByUser().get(userID)) {
				rating += getUserItemRatings().get(userID).get(itemID);
			}
			rating /= (double) getItemsByUser().get(userID).size();
			averageUserRatings.put(userID, rating);
		}
	}

	public void initializeAverageItemRatings() {
		averageItemRatings = new HashMap<Integer, Double>();
		for (Integer itemID : getUsersByItem().keySet()) {
			double rating = 0;
			for (Integer userID : getUsersByItem().get(itemID)) {
				rating += getUserItemRatings().get(userID).get(itemID);
			}
			rating /= (double) getUsersByItem().get(itemID).size();
			averageItemRatings.put(itemID, rating);
		}
	}

	public void initializeUserFriends() {
		userFriends = new TreeMap<Integer, HashSet<Integer>>();
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

	// Getters

	public TreeMap<Integer, HashMap<Integer, Double>> getUserItemRatings() {
		if (userItemRatings == null)
			initializeUserItemRatings();
		return userItemRatings;
	}

	public TreeMap<Integer, HashSet<Integer>> getItemsByUser() {
		if (itemsByUser == null)
			initializeItemsByUser();
		return itemsByUser;
	}

	public TreeMap<Integer, HashSet<Integer>> getUsersByItem() {
		if (usersByItem == null)
			initializeUsersByItem();
		return usersByItem;
	}

	public HashMap<Integer, Double> getAverageUserRatings() {
		if (averageUserRatings == null)
			initializeAverageUserRatings();
		return averageUserRatings;
	}

	public HashMap<Integer, Double> getAverageItemRatings() {
		if (averageItemRatings == null)
			initializeAverageItemRatings();
		return averageItemRatings;
	}

	public TreeMap<Integer, HashSet<Integer>> getUserFriends() {
		if (userFriends == null)
			initializeUserFriends();
		return userFriends;
	}

}
