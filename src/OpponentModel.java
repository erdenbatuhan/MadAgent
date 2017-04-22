import java.util.*;
import negotiator.*;
import negotiator.issue.*;
import negotiator.timeline.TimeLineInfo;

public class OpponentModel {
    
    private class Preference {
    	private Issue issue;
    	private Value value;
    	private int count;
    }
	
	private Domain domain = null;
	private TimeLineInfo tl = null;
	private Deadline dl = null;
	private Bid mostPreferredBid = null; 
	private int numberOfIssues = 0;
	private double concedeRatio = 0;
	private double[] weights = null;
	private boolean isBoulware = false;
	private List<Preference> preferences = null;

    public OpponentModel(Domain domain, Deadline dl, TimeLineInfo tl) {
    	this.domain = domain;
    	this.dl = dl;
		this.tl = tl;
		
    	numberOfIssues = domain.getIssues().size();
    	
    	weights = new double[numberOfIssues + 1];
    	preferences = new ArrayList<Preference>();
    }
    
    public void addPreference(Bid lastReceivedBid, double numberOfRounds) {
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

    	setConcedeRatio(numberOfRounds);
    	sortPreferences();
    }
    
    private int getIndexIfPreferredBefore(Value value) {
    	for (int i = 0; i < preferences.size(); i++)
    		if (preferences.get(i).value == value)
    			return i;
    	
    	return -1;
    }
    
    private void setConcedeRatio(double numberOfRounds) {
		double timePassed = numberOfRounds;
		
		if (dl.getType().toString().equals("TIME"))
			timePassed = tl.getTime() * tl.getTotalTime();

    	concedeRatio = preferences.size() * (timePassed / dl.getValue());
    }
    
    private void sortPreferences() {
    	preferences.sort(new Comparator<Preference>() {
			@Override
			public int compare(Preference a, Preference b) {
    	        return b.count - a.count;
			}
    	});
    }
    
    public void computeMostPreferredBid(Bid lastReceivedBid) {
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
    
    /* For each issue, the number of times that the opponent chooses the value he loves the most. */
    /* Then normalize them, this will give you the weights. */
    public void calculateWeights() {
    	double sum = 0;
    	
    	for (int i = 0, j = 0; i < preferences.size() && j < numberOfIssues; i++) {
    		Issue currentIssue = preferences.get(i).issue;	
    		
    		if (weights[currentIssue.getNumber()] == 0) {
    			weights[currentIssue.getNumber()] = preferences.get(i).count;
    			sum += preferences.get(i).count;
    			
    			j++;
    		}
    	}
    	
    	for (int i = 1; i < weights.length; i++)
    		weights[i] /= sum;
    }

	public Bid getMostPreferredBid() {
		return mostPreferredBid;
	}
	
	public boolean isBoulware() {
    	/* Returns true if the opponent is boulware */
		boolean result = true;
    	if(concedeRatio > 0.1) // TODO UPDATE VALUE
    		result = false;

		return result;
	}
    
    public void report() {
    	for (Preference preference : preferences) {		
    		System.out.println("Issue: " + preference.issue);		
    		System.out.println("Value: " + preference.value);		
    		System.out.println("Count: " + preference.count);		
    		System.out.println(preferences.size());		
    	}
    	
    	for (int i = 1; i < weights.length; i++)
    		System.out.println("Weight of issue " + i + " is " + weights[i]);
    }
}
