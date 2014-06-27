package test;

import static org.junit.Assert.*;

import org.junit.Test;

import predictors.ItemBasedPredictor;
import rec.Data;

public class ItemBasedTest {

	private final double epsilon = 0.001;

	double[][] trainData = { { 1, 1, 5 }, { 1, 2, 3 }, { 1, 3, 4 },
			{ 2, 1, 4 }, { 2, 2, 2 }, { 2, 3, 4 }, { 2, 5, 3 }, { 3, 2, 2 },
			{ 3, 4, 4 }, { 3, 5, 3 } };
	double[][] testData = { { 1, 5, 4 }, { 2, 4, 4 }, { 3, 1, 4 },
			{ 1, 1582, 1 } };

	@Test
	public void testItemBasedPredictor() {

		Data data = new Data(trainData);

		ItemBasedPredictor p = new ItemBasedPredictor(20, "cosine", "weighted",
				data);
		p.train();

		// Test cosine similarities between all items

		assertEquals(0.99624, p.computeSimilarity(1, 2), epsilon);
		assertEquals(0.99388, p.computeSimilarity(1, 3), epsilon);
		assertEquals(0, p.computeSimilarity(1, 4), epsilon);
		assertEquals(1, p.computeSimilarity(1, 5), epsilon);
		assertEquals(0.98058, p.computeSimilarity(2, 3), epsilon);
		assertEquals(1, p.computeSimilarity(2, 4), epsilon);
		assertEquals(1, p.computeSimilarity(2, 5), epsilon);
		assertEquals(0, p.computeSimilarity(3, 4), epsilon);
		assertEquals(1, p.computeSimilarity(3, 5), epsilon);
		assertEquals(1, p.computeSimilarity(4, 5), epsilon);

		int N = testData.length;
		double[] predictions = new double[N];

		for (int i = 0; i < N; i++) {
			predictions[i] = p.predict((int) testData[i][0],
					(int) testData[i][1]);
		}

		// Test predictions

		assertEquals(4, predictions[0], epsilon);
		assertEquals(2.5, predictions[1], epsilon);
		assertEquals(2.50153, predictions[2], epsilon);
		assertEquals(4, predictions[3], epsilon);

	}

}
