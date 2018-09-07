package com.graphhopper.routing.weighting;

import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.RunningFlagEncoder;
import com.graphhopper.routing.util.TurnCostEncoder;
import com.graphhopper.storage.TurnCostExtension;
import com.graphhopper.util.EdgeIteratorState;


public class MindfulWeighting extends PriorityWeighting {

    private final RunningFlagEncoder runningFlagEncoder;
    private final TurnCostEncoder turnCostEncoder;
    private final TurnCostExtension turnCostExt=null;

    private String prevName;


    public MindfulWeighting(FlagEncoder flagEncoder) {

        super(flagEncoder);

        this.turnCostEncoder = flagEncoder;
//        this.turnCostExt = turnCostExt;

        runningFlagEncoder = (RunningFlagEncoder) flagEncoder;
    }

    @Override
    public double getMinWeight(double currDistToGoal) {
        return currDistToGoal;
    }


    /**
     * This method calculates the turn weight separately.
     */
    public double calcTurnWeight(int edgeFrom, int nodeVia, int edgeTo) {
        long turnFlags = turnCostExt.getTurnCostFlags(edgeFrom, nodeVia, edgeTo);
        System.out.println(turnCostExt.getClass().getName());
//        if (turnCostEncoder.isTurnRestricted(turnFlags))
//            return Double.POSITIVE_INFINITY;

        return turnCostEncoder.getTurnCost(turnFlags);
    }

    @Override
    public double calcWeight(EdgeIteratorState edgeState, boolean reverse, int prevOrNextEdgeId) {

        double weight = super.calcWeight(edgeState, reverse, prevOrNextEdgeId);
        double turnCost = 0;

        String currentName = edgeState.getName();
        if (!currentName.equals(prevName)) {
            turnCost = 0;
            prevName = currentName;
        }

//        System.out.println(currentName+" "+prevName);



//        System.out.println(edgeState.fetchWayGeometry(1));
//        System.out.println(edgeState.getClass().getName());
//        int edgeId = edgeState.getEdge();
//        System.out.println(edgeId+" "+edgeState.getBaseNode()+" " + prevOrNextEdgeId);


//        int edgeId = edgeState.getEdge();
//        double turnCosts = calcTurnWeight(edgeId, edgeState.getBaseNode(), prevOrNextEdgeId);

        double sensorial = runningFlagEncoder.getBeautyScore(edgeState);
        long quality = runningFlagEncoder.getQualityScore(edgeState);

        if (Double.isInfinite(weight))
            return Double.POSITIVE_INFINITY;

//        return weight / (sensorial + quality + flagEncoder.getDouble(edgeState.getFlags(), KEY));
        return weight / (sensorial + flagEncoder.getDouble(edgeState.getFlags(), KEY)) + turnCost;
    }

    @Override
    public String getName() {
        return "mindful";
    }

}


