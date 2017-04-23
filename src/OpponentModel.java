import java.util.*;
import negotiator.*;
import negotiator.issue.*;
import negotiator.timeline.TimeLineInfo;

public class OpponentModel {

	private static final double ONE_TENTH = 0.10;

	private class Preference {
		private Issue issue;
		private Value value;
		private int count;
	}
	
	private class Weight {
		private Issue issue;
		private double value;
	}

	private Domain domain = null;
	private TimeLineInfo tl = null;
	private Deadline dl = null;
	private Bid firstReceivedBid = null;
	private Bid mostPreferredBid = null;
	private int numberOfIssues = 0;
	private int numberOfSameIssues = 0;
	private byte boulwareLevel = 0;
	private List<Preference> preferences = null;

	public OpponentModel(Domain domain, Deadline dl, TimeLineInfo tl) {
		this.domain = domain;
		this.dl = dl;
		this.tl = tl;

		numberOfIssues = domain.getIssues().size();	
		preferences = new ArrayList<Preference>();
	}

	public void offer(Bid lastReceivedBid, double numberOfRounds) {
		if (firstReceivedBid == null) // This is the opponent's very first bid..
			firstReceivedBid = lastReceivedBid;
		else if (firstReceivedBid == lastReceivedBid && numberOfSameIssues != -1) // The opponent is not conceding..
			numberOfSameIssues++;
		else // The opponent has started conceding..
			numberOfSameIssues = -1;
		
		decideBoulwareLevel();
		addPreference(lastReceivedBid);
	}

	public void decideBoulwareLevel() {	
		boulwareLevel = 0;
		
		for (int multiplier = 1; numberOfSameIssues != -1; multiplier++) {
			if (numberOfSameIssues > dl.getValue() * ONE_TENTH * multiplier)
				boulwareLevel++;
			else
				break;
		}
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

		mostPreferredBid = new Bid(domain, values);
	}
	
	public List<Bid> getAcceptableBids() {
		List<Bid> acceptableBids = new ArrayList<Bid>();
		Weight[] weights = getWeights();
		
		acceptableBids.add(mostPreferredBid);
		
		for (int i = 1; i < weights.length; i++) {
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
			
			acceptableBids.add(new Bid(domain, valueMap));
		}
	}
}
