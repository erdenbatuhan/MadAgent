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
	
	/* ------------------------------------------------ Agent5 ------------------------------------------------
	 * Agent Description: CS462 - Group5 Agent
	 * Agent uses several strategies to maximize its utility along with social welfare:
	 * 	- Agent use some randomness to make it difficult for opponents to model itself.
	 * 	- Agent will accept offers that is above his threshold value.
	 * 	- Threshold will be updated by observing the opponent to react better.
	 * 	- Agent will offer relatively lower utilities for first timeToGetAlmostMad portion of negotiation.
	 * 	- Agent will offer higher utilities after timeToGetAlmostMad portion to timeToGetMad portion of negotiation.
	 * 	- When agent becomes mad, it will constantly offer the max utility bid.
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
	 * We choose 6 as our parameter because we want our agent to be both aggressive and defensive 
	 * */
	private static final double RISK_CONSTANT = 100000;
	private static final double RISK_PARAMETER = 6; // Risk Parameter: 0, 1, 2, ..., 8, 9, 10
	private static final int ROUND_NUMBER_TO_FAKE = (int) (RISK_CONSTANT / Math.pow(2, RISK_PARAMETER));

	private OpponentModel opponentModel = null;
	private SortedOutcomeSpace sortedOutcomeSpace = null;
	private Bid lastReceivedBid = null;
	private Bid bestReceivedBid = null;
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
	}

	@Override
	public Action chooseAction(List<Class<? extends Action>> validActions) { // ... Your agent's turn ...
		numberOfRounds++;

		if (lastReceivedBid == null) { // You are the starter party, offer the best possible bid
			return new Offer(getPartyId(), getBestBidPossible());
		} else { // You are not the starter party
			if (utilitySpace.getUtility(lastReceivedBid) > utilitySpace.getUtility(bestReceivedBid))
				bestReceivedBid = lastReceivedBid;

			/* If utility of the last received bid is higher than the threshold, accept the offer. */
			/* Else, offer a new bid. */
			if (utilitySpace.getUtility(lastReceivedBid) > threshold)
				return new Accept(getPartyId(), lastReceivedBid);
			else
				return new Offer(getPartyId(), getBestBidPossible());
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

	private Bid getBestBidPossible() {
		Bid bestBid = null;

		try {
			/* If the negotiation is Round limited, use number of rounds as current status */
			double currentStatus = numberOfRounds;

			/* If the negotiation is Time limited, use time as current status */
			if (negotiationType.equals("TIME"))
				currentStatus = timeline.getTime() * timeline.getTotalTime();

			/* At first 40% of negotiation, agent generates a random bid to fake his opponent with certain frequency */
			if ((int) numberOfRounds % ROUND_NUMBER_TO_FAKE == 0 && currentStatus <= negotiationLimit * 0.4) {
				bestBid = generateRandomBid();
			} else {
				/* Agent offers 1 utility at first 10% of negotiation */
				if (currentStatus <= negotiationLimit * 0.1) {
					bestBid = utilitySpace.getMaxUtilityBid();
				} else {
					/* Agent generates a random offer with a utility that is above threshold value */
					bestBid = getBestBidWithThreshold(bestBid, currentStatus);
					/* Agent generates an offer to maximize agreement chance at final parts of negotiation
            		   This method will return a bid only at specific situations, see method for more */
					bestBid = getBestBidToAgree(bestBid, currentStatus);
				}
			}
		} catch (Exception e) {
			System.out.println("An exception thrown while generating bid..");
		}

		return bestBid;
	}

	private Bid getBestBidWithThreshold(Bid bestBid, double currentStatus) throws Exception {
		Bid initialBid = null;

		/* Threshold value is updated according to the agent's boulware level */
		threshold = opponentModel.getNewThreshold();
		double innerThreshold = threshold * 0.975; // 97.5% of the threshold

		if (currentStatus < timeToGetMad) {
			if (currentStatus < timeToGetAlmostMad)
				innerThreshold = threshold * 0.95; // 95% of the threshold

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
		/* Initialize bids preferred by opponent after 95% of negotiation */
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
		if (currentStatus > negotiationLimit * 0.999) {
			opponentModel.computeMostPreferredBid();
			bestBid = opponentModel.getMostPreferredBid();
		}

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
