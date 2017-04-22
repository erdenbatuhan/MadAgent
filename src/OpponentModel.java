import negotiator.Bid;
import negotiator.issue.Issue;
import negotiator.issue.Value;

import java.util.ArrayList;

public class OpponentModel {
    private Bid mostPrefferedBid;
    private double[] weights;
    private ArrayList<Item> prefs;
    private int[][] preferences; // [1][1] How many times 1st item is offered for 1st issue

    public OpponentModel(int numberOfIssues){

    }

}
class Item{
    private Issue issue;
    private Value value;
    private int count;

    Item(Issue issue, Value value, int count){
        this.issue = issue;
        this.value = value;
        this.count = count;
    }
}

