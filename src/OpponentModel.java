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
	private TimeLineInfo timeLineInfo = null;
	private double negotiationLimit;
	private int numberOfIssues = 0;
	private List<Preference> preferences = null;
	private double concedeRatio = 0;
	private String dlType;
	private Bid mostPreferredBid = null;

    public OpponentModel(Domain domain, TimeLineInfo tl, double negotiationLimit, String dlType) {
    	this.domain = domain;
		this.timeLineInfo = tl;
    	this.negotiationLimit = negotiationLimit;
    	this.dlType = dlType;
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
		/* Update the concede ratio whenever new item is offered */
		double timePassed = numberOfRounds;
		if(dlType.equals("TIME"))
			timePassed = timeLineInfo.getTime() * timeLineInfo.getTotalTime();

    	concedeRatio = preferences.size() * (timePassed / negotiationLimit);
    	sortPreferences();
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

	public Bid getMostPreferredBid() {return mostPreferredBid;}

	public boolean isBoulware(){
    	/* Returns true if the opponent is boulware */
		boolean result = true;
    	if(concedeRatio > 0.1) // TODO UPDATE VALUE
    		result = false;

		return result;
	}
}
