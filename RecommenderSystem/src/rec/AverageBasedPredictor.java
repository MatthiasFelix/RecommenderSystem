package rec;

/**
 * This class implements a simple predictor that always predicts the average of
 * all ratings from the data.
 * 
 */
public class AverageBasedPredictor extends Predictor {

	private static double averageRating;

	public AverageBasedPredictor() {
		averageRating = 0;
	}

	@Override
	/**
	 * This method trains the predictor using the data given in the input array 'data'
	 * @param data: an array of triples (user, movie, rating)
	 */
	public void train(int[][] data) {
		for (int i = 0; i < data.length; i++) {
			averageRating += data[i][2];
		}
		averageRating = averageRating / (double) data.length;
	}

	@Override
	/**
	 * This method computes the predicted rating of user 'userID' for the movie 'movieID'
	 * @param userID: the id of the user (as given in the ml100 data file)
	 * @param movieID: the id of the movie (as given in the ml100 data file)
	 * @return: predicted rating
	 */
	public double predict(int userID, int movieID) {
		return averageRating;
	}

}
