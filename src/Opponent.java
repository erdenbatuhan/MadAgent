import java.util.*;
import negotiator.*;
import negotiator.boaframework.*;
import negotiator.utility.UtilitySpace;

public class Opponent extends OpponentModel {

	@Override
	public void init(NegotiationSession negotiationSession, Map<String, Double> parameters) {
		super.init(negotiationSession, parameters);
	}

	@Override
	public void updateModel(Bid arg0, double arg1) {
		/* Returns the estimated utility of the given bid. */
		double estimatedUtility = getBidEvaluation(arg0);
		
		/* Returns the opponent’s preference profile. */
		UtilitySpace preferenceProfile = getOpponentUtilitySpace();
		System.out.println("OPP: " + preferenceProfile.getReservationValue());
	}
}
