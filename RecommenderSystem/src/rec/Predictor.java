package rec;

/**
 * This is the root class for all predictors. All subclasses need to implement
 * the methods train(.) and predict(.)
 * 
 */

public abstract class Predictor {

	/**
	 * This method trains the predictor using the data given in the input array
	 * 'data'
	 * 
	 * @param data
	 *            : an array of triples (user, movie, rating)
	 */
	public abstract void train(double[][] data);

	/**
	 * This method computes the predicted rating of user 'userID' for the movie
	 * 'movieID'
	 * 
	 * @param userID
	 *            : the id of the user (as given in the ml100 data file)
	 * @param movieID
	 *            : the id of the movie (as given in the ml100 data file)
	 * @return: predicted rating
	 */
	public abstract double predict(int userID, int movieID);

}
