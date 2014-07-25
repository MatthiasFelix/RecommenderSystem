package cwrapper;

public class CWrapper {

	private static CWrapper instance = new CWrapper();

	private CWrapper() {
		System.loadLibrary("IGraph");
	}

	public static CWrapper getInstance() {
		return instance;
	}

	public native double[] generateGraph(String fileName, int k, int maxCliqueSize,
			double expFactor, double expMult, int openNodesEnd);

	public native double[] getCentrality(String fileName, int centralityMode);

	public native double[] getCommunities(String fileName, int community);

	/**
	 * This method calculates a centrality score for each node in the graph.
	 * There are different measures: centralityMode = 0 --> degree
	 * centralityMode = 1 --> closeness centrality centralityMode = 2 -->
	 * betweenness centrality
	 * 
	 * @param fileName
	 *            The name of the file containing the edgelist of the graph
	 * @param centralityMode
	 *            The centrality measure with which the centrality scores will
	 *            be calculated
	 * @return An array with a centrality score for each vertex in the graph
	 */
	public double[] getNormalizedCentrality(String fileName, int centralityMode) {
		double[] result;

		if (centralityMode == 0) {
			result = getCentrality(fileName, 0);
		} else if (centralityMode == 1) {
			result = getCentrality(fileName, 1);
		} else if (centralityMode == 2) {
			result = getCentrality(fileName, 2);
		} else {
			System.err.println("This centrality mode doesn't exist.");
			return null;
		}

		// Normalize the centrality scores to be between 0 and 1
		double min = Double.MAX_VALUE, max = Double.MIN_VALUE;
		for (double c : result) {
			if (min > c)
				min = c;
			if (max < c)
				max = c;
		}
		for (int i = 0; i < result.length; i++) {
			result[i] = (result[i] - min) / (max - min);
		}

		return result;
	}

}
