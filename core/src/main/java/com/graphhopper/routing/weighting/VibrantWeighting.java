package com.graphhopper.routing.weighting;

import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.HintsMap;
import com.graphhopper.routing.util.RunningFlagEncoder;
import com.graphhopper.routing.util.TurnCostEncoder;
import com.graphhopper.storage.TurnCostExtension;
import com.graphhopper.util.EdgeIteratorState;


public class VibrantWeighting extends PriorityWeighting {

    private final RunningFlagEncoder runningFlagEncoder;
    private final TurnCostEncoder turnCostEncoder;
    private final TurnCostExtension turnCostExt=null;

    private String prevName;
    private HintsMap hintsMap;

    private double beta1 = (double)1/3;
    private double beta2 = (double)1/3;
    private double beta3 = (double)1/3;


    public VibrantWeighting(HintsMap hintsMap, FlagEncoder flagEncoder) {

        super(flagEncoder);

        this.hintsMap = hintsMap;
        this.turnCostEncoder = flagEncoder;
        this.runningFlagEncoder = (RunningFlagEncoder) flagEncoder;

        initHyperParameters();
    }


    public void initHyperParameters(){
        this.beta1 = this.hintsMap.getDouble("beta1", (double)1/3);
        this.beta2 = this.hintsMap.getDouble("beta2", (double)1/3);
        this.beta3 = this.hintsMap.getDouble("beta3", (double)1/3);
    }

    public void printHyperParameters(){
        System.out.println("Parameters: beta1="+this.beta1+", beta2="+this.beta2+", beta3="+this.beta3);
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
            turnCost = 10;
            prevName = currentName;
        }

        double sensorial = runningFlagEncoder.getBeautyScore(edgeState);
        long quality = runningFlagEncoder.getQualityScore(edgeState);

        if (Double.isInfinite(weight))
            return Double.POSITIVE_INFINITY;

//        return weight / (sensorial + quality + flagEncoder.getDouble(edgeState.getFlags(), KEY));
        return weight / (sensorial + flagEncoder.getDouble(edgeState.getFlags(), KEY)) + turnCost;
    }

    @Override
    public String getName() {
        return "vibrant";
    }

}


