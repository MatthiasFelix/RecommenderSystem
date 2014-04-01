package test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import rec.UserBasedPredictor;

public class UserBasedTest {

	private final double epsilon = 0.0001;

	double[][] trainData = { { 1, 1, 5 }, { 1, 2, 3 }, { 1, 3, 4 },
			{ 2, 1, 4 }, { 2, 2, 2 }, { 2, 3, 4 }, { 2, 5, 3 }, { 3, 2, 2 },
			{ 3, 4, 4 }, { 3, 5, 3 } };
	double[][] testData = { { 1, 5, 4 }, { 2, 4, 4 }, { 3, 1, 4 },
			{ 1, 1582, 1 } };

	@Test
	public void testUserBasedPredictor() {

		UserBasedPredictor p = new UserBasedPredictor(20, "pearson",
				"adjustedweightedsum");
		p.train(trainData);

		// Test pearson similarities between all users

		assertEquals(0.86266, p.computeSimilarity(1, 2), epsilon);
		assertEquals(1, p.computeSimilarity(1, 3), epsilon);
		assertEquals(0.98058, p.computeSimilarity(3, 2), epsilon);

		int N = testData.length;
		double[] predictions = new double[N];

		for (int i = 0; i < N; i++) {
			predictions[i] = p.predict((int) testData[i][0],
					(int) testData[i][1]);
		}

		// Test predictions

		assertEquals(3.88422, predictions[0], epsilon);
		assertEquals(4.25, predictions[1], epsilon);
		assertEquals(3.87623, predictions[2], epsilon);
		assertEquals(4, predictions[3], epsilon);

	}
}
