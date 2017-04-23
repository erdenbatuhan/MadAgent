import java.util.*;
import negotiator.*;
import negotiator.actions.*;
import negotiator.boaframework.SortedOutcomeSpace;
import negotiator.parties.AbstractNegotiationParty;
import negotiator.persistent.*;
import negotiator.timeline.TimeLineInfo;
import negotiator.utility.AbstractUtilitySpace;

@SuppressWarnings("unused")
public class Group5 extends AbstractNegotiationParty {

	private static final int MAXIMUM_NUMBER_OF_TRIALS = 2000;
	private OpponentModel opponentModel = null;
	private SortedOutcomeSpace sortedOutcomeSpace = null;
    private Bid lastReceivedBid = null;
    private Bid bestReceivedBid = null;
    private String negotiationType = null;
    private double negotiationLimit = 0;
    private double numberOfRounds = 0;
    private double timeToGetAlmostMad = 0;
    private double timeToGetMad = 0;
    private double threshold = 0.9;
    private int shiftBids = 0;
    private List<Bid> bidsPreferredByOpponent = null;

    /* This will be called before the negotiation starts */
    /* initialize variables here */
    @Override
    public void init(AbstractUtilitySpace utilSpace, Deadline dl, TimeLineInfo tl, long randomSeed, AgentID agentId,
                     PersistentDataContainer data) {
        super.init(utilSpace, dl, tl, randomSeed, agentId, data);

        opponentModel = new OpponentModel(utilitySpace, threshold);
        sortedOutcomeSpace = new SortedOutcomeSpace(utilitySpace);
        
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
    public Action chooseAction(List<Class<? extends Action>> validActions) { // Your agent's turn
        numberOfRounds++;

        if (lastReceivedBid == null) { // If lastRecievedBid is null -> You are starter party, just generate an offer
            return new Offer(getPartyId(), getBestBidPossible());
        } else { // Else, Generate an offer
            if (utilitySpace.getUtility(lastReceivedBid) > utilitySpace.getUtility(bestReceivedBid))
                bestReceivedBid = lastReceivedBid;

            if (utilitySpace.getUtility(lastReceivedBid) > threshold) {
                // If utility of the last received bid is higher than our threshold, Accept
                return new Accept(getPartyId(), lastReceivedBid);
            } else { // Else, generate a new offer */
                Offer newOffer = new Offer(getPartyId(), getBestBidPossible());
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

    private Bid getBestBidPossible() {
        Bid bestBid = null;

        try {
            double currentStatus = numberOfRounds;
            
            if (negotiationType.equals("TIME"))
                currentStatus = timeline.getTime() * timeline.getTotalTime();
            
            bestBid = getBestBidWithThreshold(bestBid, currentStatus);   
            bestBid = getBestBidToAgree(bestBid, currentStatus);
        } catch (Exception e) {
        	e.printStackTrace();
            System.out.println("An exception thrown while generating bid..");
        }

        return bestBid;
    }

	private Bid getBestBidWithThreshold(Bid bestBid, double currentStatus) throws Exception {
		Bid initialBid = null;
		
        threshold = opponentModel.getNewThreshold();
        double innerThreshold = threshold * 0.975; // 97.5%
        
		if (currentStatus < timeToGetMad) {
		    if (currentStatus < timeToGetAlmostMad)
		        innerThreshold = threshold * 0.95; // 95%

		    for (int trial = 1; trial <= MAXIMUM_NUMBER_OF_TRIALS; trial++) {
		        initialBid = generateRandomBid();

		        if (trial == MAXIMUM_NUMBER_OF_TRIALS)
		        	initialBid = utilitySpace.getMaxUtilityBid();
		        else if (utilitySpace.getUtility(initialBid) >= innerThreshold)
		            break;
		    }
			
			bestBid = initialBid;
		} else { // You finally got mad!!
		    bestBid = utilitySpace.getMaxUtilityBid();
		}
		
		return bestBid;
	}

	private Bid getBestBidToAgree(Bid bestBid, double currentStatus) throws Exception {
		/* Initialize bids preferred by opponent after 95% */
		if (currentStatus > negotiationLimit * 0.95 && bidsPreferredByOpponent == null)
			initializeBidsPreferredByOpponent();

		/* If deadline is approaching, offer using opponent model */
		if (currentStatus > negotiationLimit * 0.975)
			bestBid = bidsPreferredByOpponent.get(shiftBids++ % bidsPreferredByOpponent.size());
		
		/* Offer your best bid in every 10 rounds */
		if (numberOfRounds % 10 == 0)
    		bestBid = utilitySpace.getMaxUtilityBid();
		
		/* Offer your best received bid as the negotiation is almost over */
		if (currentStatus > negotiationLimit * 0.995)
			bestBid = bestReceivedBid;
		
		/* Offer the most preferred bid by the opponent in order to reach an agreement */
		if (currentStatus > negotiationLimit * 0.999)
			bestBid = opponentModel.getMostPreferredBid();
		
		return bestBid;
	}

	private void initializeBidsPreferredByOpponent() throws Exception {
		bidsPreferredByOpponent = opponentModel.getAcceptableBids();
		sortBids(bidsPreferredByOpponent);
		
		/* There should be at least 2 elements in the array */
		while (bidsPreferredByOpponent.size() <= 2)
			bidsPreferredByOpponent.add(utilitySpace.getMaxUtilityBid());
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
    public String getDescription() {
        return "CS462 - Group5 Agent";
    }
    
    @Override
    public HashMap<String, String> negotiationEnded(Bid acceptedBid) {
    	System.out.println("Negotiation has ended..");
		return null;
    }
}
