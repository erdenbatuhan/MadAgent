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
	private List<Preference> preferences = null;

    public OpponentModel(Domain domain, Deadline dl, TimeLineInfo tl) {
    	this.domain = domain;
    	this.dl = dl;
		this.tl = tl;
		
    	numberOfIssues = domain.getIssues().size();
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
    
    public void calculateMostPreferredBid(Bid lastReceivedBid) {
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

	public Bid getMostPreferredBid() {
		return mostPreferredBid;
	}

	// hello
	
	
	
	public boolean isBoulware() {
    	/* Returns true if the opponent is boulware */
		boolean result = true;
    	if(concedeRatio > 0.1) // TODO UPDATE VALUE
    		result = false;

		return result;
	}
}
