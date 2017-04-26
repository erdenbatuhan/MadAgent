import java.util.*;
import negotiator.*;
import negotiator.actions.*;
import negotiator.boaframework.SortedOutcomeSpace;
import negotiator.parties.AbstractNegotiationParty;
import negotiator.persistent.*;
import negotiator.timeline.TimeLineInfo;
import negotiator.utility.AbstractUtilitySpace;

public class Group5 extends AbstractNegotiationParty {

	/* ------------------------------------------------ Agent5 ------------------------------------------------
	 * Agent Description: CS462 - Agent5 - Agent of Group5
	 * Agent uses several strategies to maximize its utility along with social welfare:
	 * 	- Agent use some randomness to make it difficult for opponents to model itself.
	 * 	- Agent will accept offers that is above his threshold value.
	 * 	- Threshold will be updated by observing the opponent to react better.
	 * 	- Agent will offer relatively lower utilities for first timeToGetAlmostMad portion of negotiation.
	 * 	- Agent will offer higher utilities after timeToGetAlmostMad portion to timeToGetMad portion of negotiation.
	 * 	- When agent becomes mad, it will offer bids that have higher utilities than the very first threshold value.
	 * 	- In order to reach an agreement agent will model its opponents at last 5% and will offer more suitable bids for them.
	 * 	- At final parts of negotiations if there is still no agreement, agent will offer the best bid that is given by opponent.
	 * 	- Agent will offer the most preferred bid by opponent as last call to reach an agreement. 
	 * */

	private static final int MAXIMUM_NUMBER_OF_TRIALS = 2000;	

	/* -------------------------------- RISK FUNCTION  --------------------------------
	 * f <- Round number to fake (Agent will fake in every f rounds)
	 * c <- Risk constant
	 * p <- Risk parameter
	 * Formula -> f = c / 2 ^ p
	 * We choose 5 as our parameter because we want our agent to be both aggressive and defensive
	 * */
	private static final double RISK_CONSTANT = 100000;
	private static final double RISK_PARAMETER = 5; // Risk Parameter: 0, 1, 2, ..., 8, 9, 10
	private static final int ROUND_NUMBER_TO_FAKE = (int) (RISK_CONSTANT / Math.pow(2, RISK_PARAMETER));

	private OpponentModel opponentModel = null;
	private SortedOutcomeSpace sortedOutcomeSpace = null;
	private Bid lastReceivedBid = null;
	private Bid bestReceivedBid = null;
	private Bid worstReceivedBid = null;
	private Bid secondBestBid = null;
	private String negotiationType = null;
	private double negotiationLimit = 0;
	private double numberOfRounds = 0;
	private double timeToGetAlmostMad = 0;
	private double timeToGetMad = 0;
	private double threshold = 0.95;
	private int shiftBids = 0;
	private List<Bid> bidsPreferredByOpponent = null;

	@Override
	public void init(AbstractUtilitySpace utilSpace, Deadline dl, TimeLineInfo tl, long randomSeed, AgentID agentId,
			PersistentDataContainer data) {
		super.init(utilSpace, dl, tl, randomSeed, agentId, data);

		opponentModel = new OpponentModel(utilitySpace, threshold);
		sortedOutcomeSpace = new SortedOutcomeSpace(utilitySpace);

		try {
			bestReceivedBid = utilSpace.getMinUtilityBid();
			worstReceivedBid = utilSpace.getMaxUtilityBid();
		} catch (Exception e) {
			System.out.println("An exception thrown at init..");
		}

		negotiationType = dl.getType().toString();
		negotiationLimit = dl.getValue();

		/* This values will be used for adapting threshold */
		timeToGetMad = negotiationLimit * 0.8; // Agent gets mad in the last 20% of the negotiation
		timeToGetAlmostMad = timeToGetMad * 0.625; // Agent gets almost mad in the last 50% of the negotiation

		if (getData().getPersistentDataType() != PersistentDataType.STANDARD)
			throw new IllegalStateException("need standard persistent data");
		
		/* Agent calculates the second best bid */
		try {
			calculateSecondBestBid();
		} catch (Exception e) {
			System.out.println("An exception thrown while calculating the second best bid..");
		}
	}
	
	private void calculateSecondBestBid() throws Exception {
		for (double u = utilitySpace.getUtility(utilitySpace.getMaxUtilityBid()); true; u -= 0.01) {
			secondBestBid = sortedOutcomeSpace.getBidNearUtility(u).getBid();
			
			if (utilitySpace.getUtility(secondBestBid) != utilitySpace.getUtility(utilitySpace.getMaxUtilityBid()))
				break;
		}
	}

	@Override
	public void receiveMessage(AgentID sender, Action action) { // ... Opponent's turn ...
		super.receiveMessage(sender, action);

		/* If the action is an Offer, get the last received bid and use it to form Opponent Model */
		if (action instanceof Offer) {
			lastReceivedBid = ((Offer) action).getBid(); 
			opponentModel.offer(lastReceivedBid, numberOfRounds);
		}
	}

	@Override
	public Action chooseAction(List<Class<? extends Action>> validActions) { // ... Your agent's turn ...
		numberOfRounds++;

		if (lastReceivedBid == null) { // You are the starter party, offer the best possible bid
			return new Offer(getPartyId(), getBestBidPossible());
		} else { // You are not the starter party
			/* Determine the best received bid */
			if (utilitySpace.getUtility(lastReceivedBid) > utilitySpace.getUtility(bestReceivedBid))
				bestReceivedBid = lastReceivedBid;

			/* Determine the worst received bid */
			if (utilitySpace.getUtility(lastReceivedBid) < utilitySpace.getUtility(worstReceivedBid))
				worstReceivedBid = lastReceivedBid;
			
			/* TODO: Instead of keeping the best received bid, keep top 10 received bids,
			 *		 because the opponent can change his acceptance strategy.
			 * */

			/* If utility of the last received bid is higher than the threshold, accept the offer. */
			/* Else, offer a new bid. */
			if (utilitySpace.getUtility(lastReceivedBid) > threshold)
				return new Accept(getPartyId(), lastReceivedBid);
			else
				return new Offer(getPartyId(), getBestBidPossible());
		}
	}

	private Bid getBestBidPossible() {
		try {
			double currentStatus = getCurrentStatus();

			if (currentStatus <= negotiationLimit * 0.05) // First 5% of the negotiation
				return secondBestBid;
			else if ((int) numberOfRounds % ROUND_NUMBER_TO_FAKE <= 10 && currentStatus <= negotiationLimit * 0.9)
				return getFakeBid();
			else
				if (currentStatus < negotiationLimit * 0.975)
					return getBestBidWithThreshold(currentStatus);
				else  // Last 2.5% of the negotiation
					return getBestBidToAgree(currentStatus);
		} catch (Exception e) {
			System.out.println("An exception thrown while generating bid..");
		}
		
		return generateRandomBid(); // This line will never be executed!!
	}

	/* Current status is the time/number of rounds passed */
	private double getCurrentStatus() {
		/* If the negotiation is time limited, use time as current status */
		if (negotiationType.equals("TIME"))
			return timeline.getTime() * timeline.getTotalTime();
		
		/* If the negotiation is round limited, use number of rounds as current status */
		return numberOfRounds;
	}

	/* At first 90% of negotiation, agent generates a random bid to fake his opponent with certain frequency */
	private Bid getFakeBid() {
		for (int trial = 1; trial <= MAXIMUM_NUMBER_OF_TRIALS; trial++) {
			Bid bid = generateRandomBid();
			
			/* The utility of the bid should be greater than 80% of the threshold */
			if (utilitySpace.getUtility(bid) >= threshold * 0.8)
				return bid;
		}
		
		return generateRandomBid();
	}

	/* Agent generates a random offer with a utility that is above threshold value */
	private Bid getBestBidWithThreshold(double currentStatus) throws Exception {
		double innerThreshold = getInnerThreshold(currentStatus);

		for (int trial = 1; trial <= MAXIMUM_NUMBER_OF_TRIALS; trial++) {
			Bid bid = generateRandomBid();

			if (utilitySpace.getUtility(bid) >= innerThreshold)
				return bid;
		}
		
		return utilitySpace.getMaxUtilityBid();
	}
	
	private double getInnerThreshold(double currentStatus) {
		/* Threshold value is updated according to the agent's boulware level */
		threshold = opponentModel.getNewThreshold();

		/* In every 10 rounds, inner threshold remains unchanged */
		if (numberOfRounds % 10 == 0) {
			return threshold;
		} else if (currentStatus < timeToGetMad) {
			double innerThreshold = threshold * 0.975; // 97.5% of the threshold
					
			if (currentStatus < timeToGetAlmostMad)
				innerThreshold = threshold * 0.95; // 95% of the threshold
			
			return innerThreshold;
		}
		
		return threshold;
	}

	/* Agent generates an offer to maximize agreement chance at final parts of the negotiation */
	private Bid getBestBidToAgree(double currentStatus) throws Exception {
		/* Initialize bids preferred by opponent if it's null */
		if (bidsPreferredByOpponent == null) {
			opponentModel.computeMostPreferredBid();
			initializeBidsPreferredByOpponent();
		}

		return getBidUsingOpponentModeling(currentStatus);
	}

	private void initializeBidsPreferredByOpponent() throws Exception {
		bidsPreferredByOpponent = opponentModel.getAcceptableBids();
		sortBids(bidsPreferredByOpponent);

		/* There should be at least 2 elements in the array */
		while (bidsPreferredByOpponent.size() <= 2)
			bidsPreferredByOpponent.add(bestReceivedBid);
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

	private Bid getBidUsingOpponentModeling(double currentStatus) throws Exception {
		Bid bid = (bidsPreferredByOpponent != null) ? bidsPreferredByOpponent.get(shiftBids++ % bidsPreferredByOpponent.size()) : bestReceivedBid;

		if (currentStatus < negotiationLimit * 0.9875) { // Between last 2.5% and 1.25% of the negotiation
			/* Offer your best received bid if its utility is greater */
			if (utilitySpace.getUtility(bestReceivedBid) > utilitySpace.getUtility(bid)) {
				bid = bestReceivedBid;
				shiftBids = 0;
			}
		}

		/* Offer the most preferred bid by the opponent in order to reach an agreement */
		if (currentStatus > negotiationLimit * 0.999)
			bid = opponentModel.getMostPreferredBid();
		
		return bid;
	}

	@Override
	public String getDescription() {
		return "CS462 - Agent5 - Agent of Group5";
	}

	@Override
	public HashMap<String, String> negotiationEnded(Bid acceptedBid) {
		System.out.println("Negotiation has ended..");
		return null;
	}
}
