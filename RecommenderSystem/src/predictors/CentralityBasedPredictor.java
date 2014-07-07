package predictors;

import java.util.HashMap;
import java.util.LinkedHashMap;

import rec.Data;

public class CentralityBasedPredictor extends Predictor {

	private Data data;
	private String sMetric, pMetric;

	// hash map: userID --> hash map: userID --> similarity
	private HashMap<Integer, LinkedHashMap<Integer, Double>> userSimilarities;

	public CentralityBasedPredictor(String sMetric, String pMetric, Data d) {
		this.data = d;
		this.sMetric = sMetric;
		this.pMetric = pMetric;

		userSimilarities = new HashMap<Integer, LinkedHashMap<Integer, Double>>();

	}

	@Override
	public void train() {

	}

	@Override
	public double predict(int userID, int itemID) {

		return 0;

	}

}
