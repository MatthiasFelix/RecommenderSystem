package predictors;

import rec.Data;

/**
 * This class implements a predictor that for a given user always predicts his
 * average rating (over all ratings that user has submitted)
 * 
 */
public class UserAverageBasedPredictor extends Predictor {

	private Data data;
	private double average;

	public UserAverageBasedPredictor(Data d) {
		data = d;
	}

	@Override
	/**
	 * This method trains the predictor using the data given in the input array 'data'
	 * @param data: an array of triples (user, item, rating)
	 */
	public void train() {
		computeAverage();
	}

	@Override
	/**
	 * This method computes the predicted rating of user 'userID' for the item 'itemID'
	 * @param userID: the id of the user (as given in the ml100 data file)
	 * @param itemID: the id of the item (as given in the ml100 data file)
	 * @return: predicted rating
	 */
	public double predict(int userID, int itemID) {
		if (data.getAverageUserRatings().get(userID) != null) {
			return data.getAverageUserRatings().get(userID);
		} else {
			return average;
		}
	}

	public void computeAverage() {
		for (Double avg : data.getAverageUserRatings().values()) {
			average += avg;
		}
		average /= data.getAverageUserRatings().size();
	}

}
