package rec;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * This class reads through the parameter file and sets the values accordingly.
 * 
 */
public class ParseInput {

	private static final String loglevel = "loglevel";
	private static final String dataset = "dataset";
	private static final String traindata = "traindata";
	private static final String testdata = "testdata";
	private static final String neighbourhood = "neighbourhood";
	private static final String neighbourhoodsizes = "nsizes";
	private static final String thresholds = "thresholds";
	private static final String predictors = "predictors";
	private static final String smetrics = "smetrics";
	private static final String pmetrics = "pmetrics";
	private static final String socialNeighbourhood = "socialneighbourhood";

	public static void setParameters(String fileName) {

		try {
			BufferedReader b = new BufferedReader(new FileReader(fileName));
			String line;
			while ((line = b.readLine()) != null) {

				String[] s;
				if (line.contains("#")) {
					// Do nothing, line is a comment
				}

				else if (line.contains(loglevel)) {
					s = line.split(" ");
					Recommender.setLoglevel(s[1]);
				}

				else if (line.contains(dataset)) {
					s = line.split(" ");
					Recommender.setDataSet(s[1]);
				}

				else if (line.contains(traindata)) {
					s = line.split(" ");
					String[] data = new String[s.length - 1];
					for (int i = 1; i < s.length; i++) {
						data[i - 1] = s[i];
					}
					Recommender.setTrainDataFiles(data);
				}

				else if (line.contains(testdata)) {
					s = line.split(" ");
					String[] data = new String[s.length - 1];
					for (int i = 1; i < s.length; i++) {
						data[i - 1] = s[i];
					}
					Recommender.setTestDataFiles(data);
				}

				else if (line.startsWith(neighbourhood)) {
					s = line.split(" ");
					String[] n = new String[s.length - 1];
					for (int i = 1; i < s.length; i++) {
						n[i - 1] = s[i];
					}
					Recommender.setNeighbourhood(n);
				}

				else if (line.contains(neighbourhoodsizes)) {
					s = line.split(" ");
					int[] sizes = new int[s.length - 1];
					for (int i = 1; i < s.length; i++) {
						sizes[i - 1] = new Integer(s[i]);
					}
					Recommender.setNeighbourhoodSizes(sizes);
				}

				else if (line.contains(thresholds)) {
					s = line.split(" ");
					double[] thresh = new double[s.length - 1];
					for (int i = 1; i < s.length; i++) {
						thresh[i - 1] = new Double(s[i]);
					}
					Recommender.setThresholds(thresh);
				}

				else if (line.contains(predictors)) {
					s = line.split(" ");
					String[] pred = new String[s.length - 1];
					for (int i = 1; i < s.length; i++) {
						pred[i - 1] = s[i];
					}
					Recommender.setPredictors(pred);
				}

				else if (line.contains(smetrics)) {
					s = line.split(" ");
					String[] smet = new String[s.length - 1];
					for (int i = 1; i < s.length; i++) {
						smet[i - 1] = s[i];
					}
					Recommender.setSimilarityMetrics(smet);
				}

				else if (line.contains(pmetrics)) {
					s = line.split(" ");
					String[] pmet = new String[s.length - 1];
					for (int i = 1; i < s.length; i++) {
						pmet[i - 1] = s[i];
					}
					Recommender.setPredictionMetrics(pmet);
				}

				else if (line.contains(socialNeighbourhood)) {
					s = line.split(" ");
					String[] socialN = new String[s.length - 1];
					for (int i = 1; i < s.length; i++) {
						socialN[i - 1] = s[i];
					}
					Recommender.setSocialNeighbourhood(socialN);
				}

			}
			b.close();
		} catch (FileNotFoundException e) {
			System.err
					.println("Couldn't read the parameter file. Check the file name.");
			e.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

}
