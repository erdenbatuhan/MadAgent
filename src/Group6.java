import java.util.*;
import negotiator.*;
import negotiator.actions.*;
import negotiator.boaframework.SortedOutcomeSpace;
import negotiator.parties.AbstractNegotiationParty;
import negotiator.persistent.*;
import negotiator.timeline.TimeLineInfo;
import negotiator.utility.AbstractUtilitySpace;

public class Group6 extends AbstractNegotiationParty {

	private static final int MAXIMUM_NUMBER_OF_TRIALS = 2000;
	private OpponentModel opponentModel = null;
//    private SortedOutcomeSpace sortedOutcomeSpace = null;
    private Bid lastReceivedBid = null;
    private Bid bestReceivedBid = null;
    private String negotiationType = null;
    private double negotiationLimit = 0;
    private double numberOfRounds = 0;
    private double timeToGetAlmostMad = 0;
    private double timeToGetMad = 0;
    private double threshold = 0.85;
    private int shiftBids = 0;
//    private List<Bid> bids = new ArrayList<Bid>();

    /* This will be called before the negotiation starts */
    /* initialize variables here */
    @Override
    public void init(AbstractUtilitySpace utilSpace, Deadline dl, TimeLineInfo tl, long randomSeed, AgentID agentId,
                     PersistentDataContainer data) {
        super.init(utilSpace, dl, tl, randomSeed, agentId, data);

        opponentModel = new OpponentModel(utilitySpace.getDomain(), dl, tl);
//        sortedOutcomeSpace = new SortedOutcomeSpace(utilitySpace);
//        
//        System.out.println("Max: " + sortedOutcomeSpace.getMaxBidPossible().getBid());
//        System.out.println("Min: " + sortedOutcomeSpace.getMinBidPossible().getBid());
//        
//        for (double utility = 1; utility > threshold; utility -= 0.01)
//        	bids.add(sortedOutcomeSpace.getBidNearUtility(utility).getBid());
        
        try {
            bestReceivedBid = utilSpace.getMinUtilityBid();
        } catch (Exception e) {
            System.out.println("An exception thrown at init..");
        }

        negotiationType = dl.getType().toString();
        negotiationLimit = dl.getValue();
        
        timeToGetMad = negotiationLimit * 0.8; // 80%
        timeToGetAlmostMad = timeToGetMad * 0.625; // 50%

        System.out.println("Discount Factor is " + utilSpace.getDiscountFactor());
        System.out.println("Reservation Value is " + utilSpace.getReservationValueUndiscounted());

        if (getData().getPersistentDataType() != PersistentDataType.STANDARD)
            throw new IllegalStateException("need standard persistent data");
    }

    @Override
    public String getDescription() {
        return "CS462_Group6 Agent";
    }

    @Override
    public Action chooseAction(List<Class<? extends Action>> validActions) { // Your agent's turn
        numberOfRounds++;

        if (lastReceivedBid == null) { // If lastRecievedBid is null -> You are starter party, just generate an offer
            return new Offer(getPartyId(), generateBid());
        } else { // Else, Generate an offer
            if (utilitySpace.getUtility(lastReceivedBid) > utilitySpace.getUtility(bestReceivedBid))
                bestReceivedBid = lastReceivedBid;

            if (utilitySpace.getUtility(lastReceivedBid) > threshold) {
                // If utility of the last received bid is higher than our threshold, Accept
                return new Accept(getPartyId(), lastReceivedBid);
            } else { // Else, generate a new offer */
                Offer newOffer = new Offer(getPartyId(), generateBid());
                return new Offer(getPartyId(), newOffer.getBid());
            }
        }
    }

    @Override
    public void receiveMessage(AgentID sender, Action action) {
        super.receiveMessage(sender, action);

        /* Because action can be accept or offer */
        /* New class for OpponentModeling can be good */
        /* opponent model can be used here */
        if (action instanceof Offer) {
            lastReceivedBid = ((Offer) action).getBid(); 
            opponentModel.offer(lastReceivedBid, numberOfRounds);
        }
    }

    private Bid generateBid() {
        Bid initialBid = null;
        Bid bestBid = null;

        try {
            bestBid = utilitySpace.getMaxUtilityBid();
        	
            // TODO Implement opponent modeling to estimate Threshold Utility
            double innerThreshold = threshold * 0.95; // threshold * 0.95
            double currentStatus = numberOfRounds;

            if (negotiationType.equals("TIME"))
                currentStatus = timeline.getTime() * timeline.getTotalTime();
            
            if (currentStatus < timeToGetMad) {
                if (currentStatus < timeToGetAlmostMad)
                    innerThreshold = threshold * 0.9; // threshold * 0.9

                for (int trial = 1; trial <= MAXIMUM_NUMBER_OF_TRIALS; trial++) {
                    initialBid = generateRandomBid();

                    if (utilitySpace.getUtility(initialBid) > innerThreshold)
                        break;
                }
            	
            	bestBid = initialBid;
            }

            /* If deadline is approaching, offer using opponent model */
            if (currentStatus > negotiationLimit * 0.8) {
            	List<Bid> bidsPreferredByOpponent = opponentModel.getAcceptableBids();
            	sortBids(bidsPreferredByOpponent);

            	bestBid = bidsPreferredByOpponent.get(shiftBids++ % bidsPreferredByOpponent.size());
            }
            
            /* Offer your best bid in every 10 rounds */
            if (currentStatus % 10 == 0)
                bestBid = utilitySpace.getMaxUtilityBid();
            
            /* Offer your best received bid as the negotiation is almost over */
            if (currentStatus >= negotiationLimit * 0.99)
            	bestBid = bestReceivedBid;
        } catch (Exception e) {
            System.out.println("An exception thrown while generating bid");
        }

        return bestBid;
    }
    
    private void sortBids(List<Bid> bids) {
    	bids.sort(new Comparator<Bid>() {
    		@Override
			public int compare(Bid a, Bid b) {
    			if (utilitySpace.getUtility(a) < utilitySpace.getUtility(b)) return 1;
				if (utilitySpace.getUtility(a) > utilitySpace.getUtility(b)) return -1;
				
				return 0;
			}
    	});
    }
    
    @Override
    public HashMap<String, String> negotiationEnded(Bid acceptedBid) {
    	System.out.println("Negotiation has ended..");
		return null;
    }
}
