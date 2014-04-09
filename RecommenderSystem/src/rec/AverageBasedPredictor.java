package rec;

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
	 * @param data: an array of triples (user, movie, rating)
	 */
	public void train() {
		int ratingCount = 0;
		for (Integer user : data.getUserMovieRatings().keySet()) {
			for (Double rating : data.getUserMovieRatings().get(user).values()) {
				averageRating += rating;
				ratingCount++;
			}
		}
		averageRating = averageRating / (double) ratingCount;
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
