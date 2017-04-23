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
		/* Boulware level is used for modifying the behaviour of our agent
		 * If the boulware level is high, agent will increase threshold and will become more boulware

		 * The boulware level is decided by checking the offers of the opponent
		 * If the utilities of offers that is given by the opponent is low, according to our utility function
		 * the boulware level will be high. */

		/* After the edge of conceding, the boulware level becomes 0 which means agent is ready to concede */
		final double EDGE_OF_CONCEDING = threshold * 0.8;

		if (lastLastReceivedBid == null)
			lastLastReceivedBid = lastReceivedBid;
		/* Boulware Level is calculated by checking previous offers */
		double lastLastReceivedUtility = utilitySpace.getUtility(lastLastReceivedBid);
		double lastReceivedUtility = utilitySpace.getUtility(lastReceivedBid);
		double finalReceivedUtility = (lastLastReceivedUtility + lastReceivedUtility) / 2;
		
		if (finalReceivedUtility > EDGE_OF_CONCEDING)
			boulwareLevel = 0;
		else
			boulwareLevel = (int) ((EDGE_OF_CONCEDING - finalReceivedUtility) * BOULWARE_MULTIPLIER);
		
		if (boulwareLevel > MAXIMUM_BOULWARE_LEVEL)
			boulwareLevel = MAXIMUM_BOULWARE_LEVEL;

		lastLastReceivedBid = lastReceivedBid;
	}

	private void addPreference(Bid lastReceivedBid) {
		/* Whenever a new offer is given, this method will be called
		*  For each item, number of occurences will be stored in preferences list */
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
	}

	private int getIndexIfPreferredBefore(Value value) {
		for (int i = 0; i < preferences.size(); i++)
			if (preferences.get(i).value == value)
				return i;

		return -1;
	}
	
	public List<Bid> getAcceptableBids() {
		/* Return list of bids that will possibly be accepted by the opponent */
		computeMostPreferredBid();
		
		List<Bid> acceptableBids = new ArrayList<Bid>();
		Weight[] weights = getWeights();
		
		for (double i = 1, previousWeight = 0; i < weights.length; i++) {
			if (i > 2 && weights[(int) i].value - previousWeight > 0.02)
				break;
			
			Issue currentIssue = weights[(int) i].issue;
			addBidsWithDifferentValues(acceptableBids, currentIssue);
				
			previousWeight = weights[(int) i].value;
		}
		
		return acceptableBids;
	}
	
	public void computeMostPreferredBid() {
		HashMap<Integer, Value> values = new HashMap<Integer, Value>();
		sortPreferences();

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

	private void sortPreferences() {
		preferences.sort(new Comparator<Preference>() {
			@Override
			public int compare(Preference a, Preference b) {
				return b.count - a.count;
			}
		});
	}

	private Weight[] getWeights() {
		/* Estimate the weight of each issue */
		Weight[] weights = new Weight[numberOfIssues + 1];

		calculateWeights(weights);
		sortWeights(weights);
		
		return weights;
	}

	private void calculateWeights(Weight[] weights) {
		double sum = 0;
		
		for (int i = 0, j = 0; i < preferences.size() && j < numberOfIssues; i++) {
			Issue currentIssue = preferences.get(i).issue;
			int issueId = currentIssue.getNumber();

			if (weights[issueId] == null) {
				weights[issueId] = new Weight();
				
				weights[issueId].issue = currentIssue;
				weights[issueId].value = preferences.get(i).count;
				
				sum += weights[issueId].value;
				j++;
			}
		}
		
		for (int i = 1; i < weights.length; i++)
			weights[i].value /= sum;
	}
	
	private void sortWeights(Weight[] weights) {
		weights[0] = new Weight();
		
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
		/* Modify the most preffered bid by opponent to increase utility for our agent */
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
		return threshold * (1 + boulwareLevel / c);
	}
	
	public Bid getMostPreferredBid() {
		return mostPreferredBid;
	}
}
