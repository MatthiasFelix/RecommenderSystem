package rec;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

/**
 * 
 * @author matthiasfelix
 * 
 *         This class can be used to split a data set into multiple training and
 *         test data sets
 * 
 */
public class CrossValidator {

	public static void main(String[] args) {
		divide("lastfm-2k/user_artists.data", 1, 5);
	}

	/**
	 * This method splits a given data set into a training set and a test set,
	 * with sizes according to the percentage chosen. This can be repeated
	 * multiple times, to generate multiple trainings and test data sets.
	 * 
	 * @param fileName
	 *            The location of the data set file
	 * @param basePercentage
	 *            The percentage of the whole data set that is put into the
	 *            training set
	 * @param repetitions
	 *            The number of training-test pairs the are generated
	 */
	public static void divide(String fileName, double basePercentage,
			int repetitions) {

		// The percentage of the training set has to be between 0 and 1
		if (0 > basePercentage || basePercentage > 1) {
			System.err
					.println("The percentage of the training set has to be between 0 and 1.");
			return;
		}

		// count number of lines
		int N = 0;
		try {
			BufferedReader b = new BufferedReader(new FileReader(fileName));
			while (b.readLine() != null) {
				N++;
			}
			b.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		for (int i = 1; i <= repetitions; i++) {
			File baseFile = new File(
					"/Users/matthiasfelix/Documents/Programming/Eclipse_Workspace/RecommenderSystem/lastfm-2k/set"
							+ i + ".base");
			File testFile = new File(
					"/Users/matthiasfelix/Documents/Programming/Eclipse_Workspace/RecommenderSystem/lastfm-2k/set"
							+ i + ".test");
			FileWriter baseFileWriter;
			FileWriter testFileWriter;
			try {
				baseFileWriter = new FileWriter(baseFile);
				testFileWriter = new FileWriter(testFile);
				BufferedWriter baseBufferedWriter = new BufferedWriter(
						baseFileWriter);
				BufferedWriter testBufferedWriter = new BufferedWriter(
						testFileWriter);

				int baseMax = Math.round((float) (N * basePercentage));
				int testMax = N - baseMax;

				int baseSize = 0, testSize = 0;

				BufferedReader b = new BufferedReader(new FileReader(fileName));
				String line;

				for (int j = 0; j < N; j++) {
					line = b.readLine();
					if (baseSize == baseMax) {
						testBufferedWriter.write(line + "\n");
					} else if (testSize == testMax) {
						baseBufferedWriter.write(line + "\n");
					} else {
						Random rnd = new Random();
						if (rnd.nextDouble() < basePercentage) {
							baseBufferedWriter.write(line + "\n");
							baseSize++;
						} else {
							testBufferedWriter.write(line + "\n");
							testSize++;
						}
					}
				}

				b.close();
				baseBufferedWriter.close();
				testBufferedWriter.close();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}
}
