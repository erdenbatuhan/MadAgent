import java.util.HashMap;
import java.util.List;
import java.util.Map;

import list.Tuple;
import negotiator.AgentID;
import negotiator.Bid;
import negotiator.Deadline;
import negotiator.actions.Accept;
import negotiator.actions.Action;
import negotiator.actions.Offer;
import negotiator.issue.*;
import negotiator.parties.AbstractNegotiationParty;
import negotiator.persistent.PersistentDataContainer;
import negotiator.persistent.PersistentDataType;
import negotiator.persistent.StandardInfo;
import negotiator.persistent.StandardInfoList;
import negotiator.timeline.TimeLineInfo;
import negotiator.utility.AbstractUtilitySpace;
import negotiator.utility.EvaluatorDiscrete;

public class Group6 extends AbstractNegotiationParty {

    private Bid lastReceivedBid = null;
    private double numberOfRounds = 0;
    private double roundsToGetAlmostMad = 0;
    private double roundsToGetMad = 0;
    private double threshold = 0.80;
    private StandardInfoList history;
    private boolean control = true;

    @Override
	/* This will be called before the negotiation starts */
	/* initialize variables here */
    public void init(AbstractUtilitySpace utilSpace, Deadline dl, TimeLineInfo tl, long randomSeed, AgentID agentId,
                     PersistentDataContainer data) {

        super.init(utilSpace, dl, tl, randomSeed, agentId, data);

        roundsToGetMad = dl.getValue() - dl.getValue() / 10;
        roundsToGetAlmostMad = (int) roundsToGetMad / 2;
        System.out.println("Discount Factor is " + utilSpace.getDiscountFactor());
        System.out.println("Reservation Value is " + utilSpace.getReservationValueUndiscounted());

        if (getData().getPersistentDataType() != PersistentDataType.STANDARD) {
            throw new IllegalStateException("need standard persistent data");
        }

		/* Use history to get previous negotiation utilities */
		history = (StandardInfoList) getData().get();

        if (!history.isEmpty()) { // An example of using the history
			/* Compute for each party the maximum utility of the bids in last session. */
            Map<String, Double> maxutils = new HashMap<String, Double>();
            StandardInfo lastinfo = history.get(history.size() - 1); // Most recent history

            for (Tuple<String, Double> offered : lastinfo.getUtilities()) {
                String party = offered.get1();
                Double util = offered.get2();
                maxutils.put(party, maxutils.containsKey(party) ? Math.max(maxutils.get(party), util) : util);
            }

            System.out.println(maxutils); // Notice tournament suppresses all output.
        }
    }

    @Override
    public Action chooseAction(List<Class<? extends Action>> validActions) { // Your agent's turn
        numberOfRounds++;
        
        if (!history.isEmpty() && control) {
            analyzeHistory();
        }

        if (lastReceivedBid == null) { // If lastRecievedBid is null -> You are starter party, just generate a random offer
            return new Offer(getPartyId(), generateRandomBid());
        } else { // Else, Generate an offer
            if (utilitySpace.getUtility(lastReceivedBid) > threshold) { // If utility of the last received bid is higher than our threshold Accept
                return new Accept(getPartyId(), lastReceivedBid);
            } else { // Else, generate a new offer */
                Offer newOffer = new Offer(getPartyId(), generateBid());
                return new Offer(getPartyId(), newOffer.getBid());
            }
        }
    }

    private Bid generateBid (){
        Bid bestBid = null;
        Bid startingBids = null;

        try {
            bestBid = utilitySpace.getMaxUtilityBid();
            // TODO Implement opponent modeling to estimate Threshold Utility
            // TODO Decide TempThreshold, make it relative to real Threshold
            // TODO Make this work with Time Limited negotiations either
            double tempThreshold = 0.7;
            
            if (numberOfRounds < roundsToGetMad) {
                if (numberOfRounds < roundsToGetAlmostMad)
                    tempThreshold = 0.5;
                
                for (int trial = 0; true; ++trial) {
                    startingBids = generateRandomBid();
                    
                    if (utilitySpace.getUtility(startingBids) > tempThreshold || trial > 200)
                    	break;
                }

                bestBid = startingBids;
            }

        } catch (Exception e) {
            System.out.println("An exception thrown :(");
        }

        return bestBid;
    }

    @Override
    public void receiveMessage(AgentID sender, Action action) {
        super.receiveMessage(sender, action);

		/* Because action can be accept or offer */
		/* New class for OpponentModeling can be good */
		/* opponent model can be used here */
        if (action instanceof Offer) {
            lastReceivedBid = ((Offer) action).getBid();
        }

        if (!history.isEmpty() && control) {
            analyzeHistory();
        }
    }

    @Override
    public String getDescription() {
        return "accept Nth offer";
    }

    public void analyzeHistory() {
    	control = false;
        // from recent to older history records
        for (int h = history.size() - 1, counter = 0; h >= 0; h--) {
        	System.out.println("History index:  " + h);
            StandardInfo lastInfo = history.get(h); // Most recent negotiation history

            for (Tuple <String, Double> offered : lastInfo.getUtilities()) {
                String party = offered.get1(); // get Party Id
                Double util = offered.get2(); // get the offer utility

                System.out.println("PartyID: " + party + " utility : " + util);

                if (++counter == 3)
                    break;
            }
        }
    }
}
