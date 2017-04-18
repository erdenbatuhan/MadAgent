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
    private double roundsToGetAlmostMad = 30;
    private double roundsToGetMad = 60;
    private double threshold = 0.80;
    private StandardInfoList history;
    private boolean control = true;

    @Override
	/* This will be called before the negotiation starts */
	/* initialize variables here */
    public void init(AbstractUtilitySpace utilSpace, Deadline dl, TimeLineInfo tl, long randomSeed, AgentID agentId,
                     PersistentDataContainer data) {

        super.init(utilSpace, dl, tl, randomSeed, agentId, data);

        System.out.println("Discount Factor is " + utilSpace.getDiscountFactor());
        System.out.println("Reservation Value is " + utilSpace.getReservationValueUndiscounted());

        if (getData().getPersistentDataType() != PersistentDataType.STANDARD) {
            throw new IllegalStateException("need standard persistent data");
        }

		/* Use history to get previous negotiation utilities */
		history = (StandardInfoList) getData().get();

        if (!history.isEmpty()) {
            // example of using the history.
			/* Compute for each party the maximum utility of the bids in last session.  */
            Map<String, Double> maxutils = new HashMap<String, Double>();
            StandardInfo lastinfo = history.get(history.size() - 1); /* Most recent history */

            for (Tuple<String, Double> offered : lastinfo.getUtilities()) {
                String party = offered.get1();
                Double util = offered.get2();
                maxutils.put(party, maxutils.containsKey(party) ? Math.max(maxutils.get(party), util) : util);
            }

            System.out.println(maxutils); // notice tournament suppresses all output.
        }
    }

    @Override
    public Action chooseAction(List<Class<? extends Action>> validActions) { // Your agent's turn
        numberOfRounds++;
        if(!history.isEmpty() && control){
            analyzeHistory();
        }

		/* if lastRecievedBid is null -> You are starter party, just generate a random offer */
        if (lastReceivedBid == null) {
            return new Offer(getPartyId(), generateRandomBid());
        }
        /* Generate an offer */
        else {
            /* If utility of the last recieved bid is higher than our threshold Accept */
            if(utilitySpace.getUtility(lastReceivedBid) > threshold){
                return new Accept(getPartyId(), lastReceivedBid);
            }
            /* else generate a new offer */
            else{
                Offer newOffer = new Offer(getPartyId(), generateBid());
                System.out.println("Round : " + numberOfRounds);
                System.out.println("Offer : " + newOffer);
                System.out.println("Utility : " + utilitySpace.getUtility(newOffer.getBid()));

                return new Offer(getPartyId(), newOffer.getBid());
            }
        }
    }

    private Bid generateBid (){
        Bid bestBid = null;
        Bid startingBids = null;
        HashMap<Integer, Value> values = new HashMap<Integer, Value>();
        List<Issue> issues = utilitySpace.getDomain().getIssues();

        try{
            /* TODO For starting rounds, just generate reasanoble offers than stick with the best one */
            bestBid = utilitySpace.getMaxUtilityBid();
            double tempThreshold = 0.7;
            if(numberOfRounds < roundsToGetMad){
                if(numberOfRounds < roundsToGetAlmostMad){
                    tempThreshold = 0.5;
                }
                int trial = 0;
                while (true){
                    trial++;
                    startingBids = generateRandomBid();
                    if(utilitySpace.getUtility(startingBids) > tempThreshold || trial > 200) break;
                }

                bestBid = startingBids;
            }

        }
        catch (Exception e){
            System.out.println("EXCEPTION");
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

        if(!history.isEmpty() && control){
            analyzeHistory();
        }
    }

    @Override
    public String getDescription() {
        return "accept Nth offer";
    }

    public void analyzeHistory() {
    	control = false;
        //from recent to older history records
        for(int h = history.size() - 1; h >= 0; h--){
        	System.out.println("History index:  " + h);
        	
            StandardInfo lastInfo = history.get(h); // Most recent negotitaion history
            int counter = 0;

            for(Tuple < String, Double> offered : lastInfo.getUtilities()){
                counter++;
                String party = offered.get1(); // get Party Id
                Double util = offered.get2(); // get the offer utility

                System.out.println("PartyID: " + party + " utility : " + util);

                if(counter == 3)
                    break;
            }
        }
    }
}
