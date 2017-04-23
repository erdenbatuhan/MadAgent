import java.util.*;
import negotiator.*;
import negotiator.issue.*;
import negotiator.utility.UtilitySpace;

public class OpponentModel {

	private static final int MAXIMUM_BOULWARE_LEVEL = 5;
	private static final int BOULWARE_MULTIPLIER = 10;

	private class Preference {
		private Issue issue;
		private Value value;
		private int count;
	}
	
	private class Weight {
		private Issue issue;
		private double value;
	}

	private UtilitySpace utilitySpace = null;
	private Bid lastLastReceivedBid = null;
	private Bid mostPreferredBid = null;
	private double threshold = 0;
	private int boulwareLevel = 0;
	private int numberOfIssues = 0;
	private List<Preference> preferences = null;

	public OpponentModel(UtilitySpace utilitySpace, double threshold) {
		this.utilitySpace = utilitySpace;
		this.threshold = threshold;

		numberOfIssues = utilitySpace.getDomain().getIssues().size();	
		preferences = new ArrayList<Preference>();
	}

	public void offer(Bid lastReceivedBid, double numberOfRounds) {
		decideBoulwareLevel(lastReceivedBid);
		addPreference(lastReceivedBid);
	}

	public void decideBoulwareLevel(Bid lastReceivedBid) {
		final double EDGE_OF_CONCEDING = threshold * 0.8;
		
		if (lastLastReceivedBid == null)
			lastLastReceivedBid = lastReceivedBid;

		double lastLastReceivedUtility = utilitySpace.getUtility(lastLastReceivedBid);
		double lastReceivedUtility = utilitySpace.getUtility(lastReceivedBid);
		double finalReceivedUtility = (lastLastReceivedUtility + lastReceivedUtility) / 2;
		
		if (finalReceivedUtility > EDGE_OF_CONCEDING)
			boulwareLevel = 0; // Conceder
		else
			boulwareLevel = (int) ((EDGE_OF_CONCEDING - finalReceivedUtility) * BOULWARE_MULTIPLIER);
		
		if (boulwareLevel > MAXIMUM_BOULWARE_LEVEL)
			boulwareLevel = MAXIMUM_BOULWARE_LEVEL;

		lastLastReceivedBid = lastReceivedBid;
	}

	private void addPreference(Bid lastReceivedBid) {
		for (int issueNr = 1; issueNr <= numberOfIssues; issueNr++) {
			Preference preference = new Preference();

			preference.issue = lastReceivedBid.getIssues().get(issueNr - 1);
			preference.value = lastReceivedBid.getValue(issueNr);
			preference.count = 1;

			int index = getIndexIfPreferredBefore(preference.value);

			if (index == -1)
				preferences.add(preference);
			else
				preferences.get(index).count++;
		}
		
		sortPreferences();
		computeMostPreferredBid(lastReceivedBid);
	}

	private int getIndexIfPreferredBefore(Value value) {
		for (int i = 0; i < preferences.size(); i++)
			if (preferences.get(i).value == value)
				return i;

		return -1;
	}

	private void sortPreferences() {
		preferences.sort(new Comparator<Preference>() {
			@Override
			public int compare(Preference a, Preference b) {
				return b.count - a.count;
			}
		});
	}
	
	private void computeMostPreferredBid(Bid lastReceivedBid) {
		HashMap<Integer, Value> values = new HashMap<Integer, Value>();

		for (int i = 0, j = 0; i < preferences.size() && j < numberOfIssues; i++) {
			Issue currentIssue = preferences.get(i).issue;
			Value currentValue = preferences.get(i).value;

			if (!values.containsKey(currentIssue.getNumber())) {
				values.put(currentIssue.getNumber(), currentValue);
				j++;
			}
		}

		mostPreferredBid = new Bid(utilitySpace.getDomain(), values);
	}
	
	public List<Bid> getAcceptableBids() {
		List<Bid> acceptableBids = new ArrayList<Bid>();
		Weight[] weights = getWeights();
		
		for (int i = 1; i <= 3; i++) {
			Issue currentIssue = weights[i].issue;
			addBidsWithDifferentValues(acceptableBids, currentIssue);
		}
		
		return acceptableBids;
	}

	private Weight[] getWeights() {
		Weight[] weights = new Weight[numberOfIssues + 1];

		calculateWeights(weights);
		normalizeWeights(weights);
		sortWeights(weights);
		
		return weights;
	}

	private void calculateWeights(Weight[] weights) {
		weights[0] = new Weight();
		
		for (int i = 0, j = 0; i < preferences.size() && j < numberOfIssues; i++) {
			Issue currentIssue = preferences.get(i).issue;
			int issueId = currentIssue.getNumber();

			if (weights[issueId] == null) {
				weights[issueId] = new Weight();
				
				weights[issueId].issue = currentIssue;
				weights[issueId].value = preferences.get(i).count;
				
				j++;
			}
		}
	}

	private void normalizeWeights(Weight[] weights) {
		double sum = 0;
		
		for (int i = 1; i < weights.length; i++)
			sum += preferences.get(i).count;
		
		for (int i = 1; i < weights.length; i++)
			weights[i].value /= sum;
	}
	
	private void sortWeights(Weight[] weights) {
		Arrays.sort(weights, new Comparator<Weight>() {
			@Override
			public int compare(Weight a, Weight b) {
				if (a.value > b.value) return 1;
				if (a.value < b.value) return -1;
				
				return 0;
			}
		});
	}

	private void addBidsWithDifferentValues(List<Bid> acceptableBids, Issue currentIssue) {
		int issueId = currentIssue.getNumber();
		
		List<Value> values = new ArrayList<Value>();
		values.add(mostPreferredBid.getValue(issueId));
		
		for (int j = 0; j < preferences.size(); j++)
			if (preferences.get(j).issue == currentIssue)
				if (!values.contains((preferences.get(j).value)))
					values.add(preferences.get(j).value);

		for (int j = 1; j < values.size(); j++) {
			HashMap<Integer, Value> valueMap = mostPreferredBid.getValues();    
			valueMap.put(issueId, values.get(j));	
			
			acceptableBids.add(new Bid(utilitySpace.getDomain(), valueMap));
		}
	}
	
	public double getNewThreshold() {
		final double c = (threshold * MAXIMUM_BOULWARE_LEVEL) / (1 - threshold);
		
		System.out.println("Boulware Level: " + boulwareLevel);	
		System.out.println("Old Threshold: " + threshold);	
		System.out.println("New Threshold: " + threshold * (1 + boulwareLevel / c));
		
		return threshold * (1 + boulwareLevel / c);
	}
	
	public Bid getMostPreferredBid() {
		return mostPreferredBid;
	}
}
