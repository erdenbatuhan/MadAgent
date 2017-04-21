import java.util.*;
import list.Tuple;
import negotiator.*;
import negotiator.actions.*;
import negotiator.issue.*;
import negotiator.parties.AbstractNegotiationParty;
import negotiator.persistent.*;
import negotiator.timeline.TimeLineInfo;
import negotiator.utility.AbstractUtilitySpace;

public class OpponentModel {

	Bid bestBid = null;
	Offer recievedOffer = null;
	int round = 0;

	public OpponentModel() {
		
	}
	
	public void addPreference(Bid lastReceivedBid) {
		
//		int numberOfIssues = lastReceivedBid.getIssues().size();
//		
//		for (int i = 0; i < numberOfIssues; i++) {
//			Issue issue = lastReceivedBid.getIssues().get(i);
//			Value value = lastReceivedBid.getValue(i + 1);
//			
//		}
	}
	
	public void printPreferences() {
		
	}
}
