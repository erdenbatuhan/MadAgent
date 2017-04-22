import java.util.*;
import negotiator.Bid;
import negotiator.issue.*;

public class OpponentModel {
	
	private int numberOfIssues = 0;
	private HashMap<Issue, Integer> issueMap = null;;
    private List<Preference> preferences = null;
	private Bid mostPreferredBid = null;

    public OpponentModel(List<Issue> issues) {
    	numberOfIssues = issues.size();
    	issueMap = new HashMap<Issue, Integer>();
    	
    	for (int i = 1; i <= numberOfIssues; i++)
    		issueMap.put(issues.get(i - 1), i);
    	
    	preferences = new ArrayList<Preference>();
    }
    
    public void addPreference(Bid lastReceivedBid) {
    	for (int i = 1; i <= numberOfIssues; i++) {
    		Preference preference = new Preference();
    		
    		preference.issue = lastReceivedBid.getIssues().get(i - 1);
    		preference.value = lastReceivedBid.getValue(i);
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
    
    public void calculateMostPreferredBid(Bid lastReceivedBid) {
    	List<Issue> issues = new ArrayList<Issue>();
    	mostPreferredBid = lastReceivedBid;
    	
    	sortPreferences();
    	
    	for (int i = 0; i < preferences.size(); i++) {
    		Issue currentIssue = preferences.get(i).issue;
    		Value currentValue = preferences.get(i).value;
    		
        	System.out.println("Issue: " + currentIssue);
        	System.out.println("Value: " + currentValue);
        	System.out.println("Count: " + preferences.get(i).count);
        	
        	System.out.println(issues.contains(currentIssue));
        	
        	for (Issue issue : issues)
        		System.out.print(issue + ", ");
        	
        	System.out.println();
    		
    		if (!issues.contains(currentIssue)) {
    			issues.add(currentIssue);
            	mostPreferredBid.putValue(issueMap.get(currentIssue), currentValue);
            	
            	System.out.println(currentIssue + ", " + currentValue);
    		}
    	}
    }
    
    private void sortPreferences() {
    	preferences.sort(new Comparator<Preference>() {
			@Override
			public int compare(Preference a, Preference b) {
    	        return b.count - a.count;
			}
    	});
    }

	public Bid getMostPreferredBid() {
		return mostPreferredBid;
	}
	
    public void printPreferences() {
    	for (Preference preference : preferences) {
    		System.out.println("Issue: " + preference.issue);
    		System.out.println("Value: " + preference.value);
    		System.out.println("Count: " + preference.count);
    		System.out.println(preferences.size());
    	}
    }
    
    class Preference {	
    	Issue issue;
        Value value;
        int count;
    }
}
