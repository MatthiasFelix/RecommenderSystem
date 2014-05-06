package rec;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class ResultSaver {

	private ArrayList<String> trainSets;
	private ArrayList<String> testSets;
	private ArrayList<String> neighbourhoods;
	private ArrayList<Integer> neighbourhoodSizes;
	private ArrayList<Double> thresholds;
	private ArrayList<String> predictors;
	private ArrayList<String> similarityMetrics;
	private ArrayList<String> predictionMetrics;
	private ArrayList<String> socialNeighbourhoods;
	private ArrayList<Double> socialThresholds;

	private ArrayList<Double> RMSEs;
	private ArrayList<Double> runTimes;

	public ResultSaver() {
		trainSets = new ArrayList<String>();
		testSets = new ArrayList<String>();
		neighbourhoods = new ArrayList<String>();
		neighbourhoodSizes = new ArrayList<Integer>();
		thresholds = new ArrayList<Double>();
		predictors = new ArrayList<String>();
		similarityMetrics = new ArrayList<String>();
		predictionMetrics = new ArrayList<String>();
		socialNeighbourhoods = new ArrayList<String>();
		socialThresholds = new ArrayList<Double>();
		RMSEs = new ArrayList<Double>();
		runTimes = new ArrayList<Double>();
	}

	public void putResult(String trainSet, String testSet,
			String neighbourhood, Integer neighbourhoodSize, Double threshold,
			String predictor, String similarityMetric, String predictionMetric,
			String socialNeighbourhood, Double socialThreshold, Double RMSE,
			Double runTime) {
		trainSets.add(trainSet);
		testSets.add(testSet);
		neighbourhoods.add(neighbourhood);
		neighbourhoodSizes.add(neighbourhoodSize);
		thresholds.add(threshold);
		predictors.add(predictor);
		similarityMetrics.add(similarityMetric);
		predictionMetrics.add(predictionMetric);
		socialNeighbourhoods.add(socialNeighbourhood);
		socialThresholds.add(socialThreshold);
		RMSEs.add(RMSE);
		runTimes.add(runTime);
	}

	public void writeToFile(String fileName) {

		File file = new File(fileName);
		FileWriter fileWriter;

		try {

			fileWriter = new FileWriter(file);
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

			bufferedWriter
					.write("trainSet\ttestSet\tneighbourhood\tneighbourhoodSize\tthreshold\tpredictor\tsimilarityMetric\t"
							+ "predictionMetric\tsocialNeighbourhood\tsocialThreshold\tRMSE\trunTime\n");

			// All ArrayLists will be of the same size, since putResult puts one
			// element into each ArrayList
			for (int i = 0; i < trainSets.size(); i++) {
				bufferedWriter.write(trainSets.get(i) + "\t" + testSets.get(i)
						+ "\t" + neighbourhoods.get(i) + "\t"
						+ neighbourhoodSizes.get(i) + "\t" + thresholds.get(i)
						+ "\t" + predictors.get(i) + "\t"
						+ similarityMetrics.get(i) + "\t"
						+ predictionMetrics.get(i) + "\t"
						+ socialNeighbourhoods.get(i) + "\t"
						+ socialThresholds.get(i) + "\t" + RMSEs.get(i) + "\t"
						+ runTimes.get(i) + "\n");
			}

			bufferedWriter.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
