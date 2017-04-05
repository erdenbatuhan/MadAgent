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
import negotiator.parties.AbstractNegotiationParty;
import negotiator.persistent.PersistentDataContainer;
import negotiator.persistent.PersistentDataType;
import negotiator.persistent.StandardInfo;
import negotiator.persistent.StandardInfoList;
import negotiator.timeline.TimeLineInfo;
import negotiator.utility.AbstractUtilitySpace;

/**
 * Sample party that accepts the Nth offer, where N is the number of sessions
 * this [agent-profile] already did.
 */
public class Group6 extends AbstractNegotiationParty {

    private Bid lastReceivedBid = null;
    private int nrChosenActions = 0; // number of times chosenAction was called.
    private StandardInfoList history;
    boolean control = true;

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
			/* Compute for each party the maximum utility of the bids in last session. */
            Map<String, Double> maxutils = new HashMap<String, Double>();
            StandardInfo lastinfo = history.get(history.size() - 1); /* Most recent history */

            for (Tuple<String, Double> offered : lastinfo.getUtilities()) {
                String party = offered.get1();
                Double util = offered.get2();
                maxutils.put(party, maxutils.containsKey(party) ? Math.max(maxutils.get(party), util) : util);
            }

            System.out.println(maxutils); // notice tournament suppresses all output.
            System.out.flush();
        }
    }

    @Override
    public Action chooseAction(List<Class<? extends Action>> validActions) { // Your agent's turn
        nrChosenActions++;
              
        if(!history.isEmpty() && control){
            analyzeHistory();
        }
        
		/* if lastRecievedBid is null -> You are starter party */
        if (nrChosenActions > history.size() & lastReceivedBid != null) {
            return new Accept(getPartyId(), lastReceivedBid);
        } else {
			/* offer ve bid farklı classlar, Bid vector , Offer bu bidi içeren vector */
			/* getPartyID() returns bizim partynin ID */
            return new Offer(getPartyId(), generateRandomBid());
        }
    }

    @Override
    public void receiveMessage(AgentID sender, Action action) {
        super.receiveMessage(sender, action);

		/* Because action can be accep or offer */
		/* OpponentModel için bir class yazsak güzel olur */
		/* opponent model burada kullanılabilir */
        if (action instanceof Offer) {
            lastReceivedBid = ((Offer) action).getBid();
        }

        // history bos degilse
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
