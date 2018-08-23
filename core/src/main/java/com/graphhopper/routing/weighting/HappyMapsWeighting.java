package com.graphhopper.routing.weighting;

import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.HappyMapsFlagEncoder;
import com.graphhopper.util.EdgeIteratorState;


public class HappyMapsWeighting extends PriorityWeighting {

    private final HappyMapsFlagEncoder happymapsFlagEncoder;


    public HappyMapsWeighting(FlagEncoder flagEncoder) {

        super(flagEncoder);

        happymapsFlagEncoder = (HappyMapsFlagEncoder) flagEncoder;
    }

    @Override
    public double getMinWeight(double currDistToGoal) {
        return currDistToGoal;
    }

    @Override
    public double calcWeight(EdgeIteratorState edgeState, boolean reverse, int prevOrNextEdgeId) {

        double weight = super.calcWeight(edgeState, reverse, prevOrNextEdgeId);

        double sensorial = happymapsFlagEncoder.getBeautyScore(edgeState);
        long quality = happymapsFlagEncoder.getQualityScore(edgeState);

        if (Double.isInfinite(weight))
            return Double.POSITIVE_INFINITY;

//        return weight / (sensorial + quality + flagEncoder.getDouble(edgeState.getFlags(), KEY));
        return weight / (sensorial + flagEncoder.getDouble(edgeState.getFlags(), KEY));
    }

    @Override
    public String getName() {
        return "happymaps";
    }

}


