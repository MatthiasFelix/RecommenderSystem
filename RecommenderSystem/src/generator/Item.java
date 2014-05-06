package generator;

public class Item {

	// Each item gets a unique ID, starting from 0
	private static int ID = 0;

	private double quality;
	private int famosity;
	private int itemID;

	public Item(double quality, int famosity) {
		this.quality = quality;
		this.famosity = famosity;
		itemID = ID++;
	}

	public double getQuality() {
		return quality;
	}

	public int getFamosity() {
		return famosity;
	}

	public int getID() {
		return itemID;
	}

}
