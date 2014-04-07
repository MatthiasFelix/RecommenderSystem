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
	private static final String traindata = "traindata";
	private static final String testdata = "testdata";
	private static final String neighbourhoodsize = "neighbourhoodsize";
	private static final String predictors = "predictors";
	private static final String smetrics = "smetrics";
	private static final String pmetrics = "pmetrics";
	private static final String threshold = "threshold";
	private static final String dataset = "dataset";
	private static final String crossvalidations = "crossvalidations";
	private static final String socialNeighbourhood = "socialNeighbourhood";

	public static void setParameters(String fileName) {

		try {
			BufferedReader b = new BufferedReader(new FileReader(fileName));
			String line;
			while ((line = b.readLine()) != null) {
				String[] s;
				// do nothing, line is a comments
				if (line.contains("#")) {
				}

				else if (line.contains(traindata)) {
					s = line.split(" ");
					Recommender.setTrainDataFile(s[1]);
				}

				else if (line.contains(testdata)) {
					s = line.split(" ");
					Recommender.setTestDataFile(s[1]);
				}

				else if (line.contains(neighbourhoodsize)) {
					s = line.split(" ");
					try {
						Recommender.setNeighbourhoodSize(new Integer(s[1]));
					} catch (NumberFormatException e) {
						System.err
								.println("neighbourhoodsize must be an integer.");
					}
				}

				else if (line.contains(threshold)) {
					s = line.split(" ");
					if (s[1].equals("true")) {
						Recommender.setThreshold(true);
					} else {
						Recommender.setThreshold(false);
					}
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

				else if (line.contains(dataset)) {
					s = line.split(" ");
					Recommender.setFileDirectory(s[1]);
				}

				else if (line.contains(crossvalidations)) {
					s = line.split(" ");
					Recommender.setCrossValidations(Integer.parseInt(s[1]));
				}

				else if (line.contains(socialNeighbourhood)) {
					s = line.split(" ");
					String[] socialN = new String[s.length - 1];
					for (int i = 1; i < s.length; i++) {
						socialN[i - 1] = s[i];
					}
					Recommender.setSocialNeighbourhood(socialN);
				}

				else if (line.contains(loglevel)) {
					s = line.split(" ");
					Recommender.setLoglevel(s[1]);
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
