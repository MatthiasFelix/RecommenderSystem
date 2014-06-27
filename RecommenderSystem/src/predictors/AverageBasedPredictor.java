package predictors;

import rec.Data;

/**
 * This class implements a simple predictor that always predicts the average of
 * all ratings from the data.
 * 
 */
public class AverageBasedPredictor extends Predictor {

	private Data data;
	private double averageRating;

	public AverageBasedPredictor(Data d) {
		data = d;
	}

	@Override
	/**
	 * This method trains the predictor using the data given in the input array 'data'
	 * @param data: an array of triples (user, item, rating)
	 */
	public void train() {
		int ratingCount = 0;
		for (Integer user : data.getUserItemRatings().keySet()) {
			for (Double rating : data.getUserItemRatings().get(user).values()) {
				averageRating += rating;
				ratingCount++;
			}
		}
		averageRating = averageRating / (double) ratingCount;
	}

	@Override
	/**
	 * This method computes the predicted rating of user 'userID' for the item 'itemID'
	 * @param userID: the id of the user (as given in the ml100 data file)
	 * @param itemID: the id of the item (as given in the ml100 data file)
	 * @return: predicted rating
	 */
	public double predict(int userID, int itemID) {
		return averageRating;
	}

}
