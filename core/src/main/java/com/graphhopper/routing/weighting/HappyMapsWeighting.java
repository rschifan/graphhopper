package com.graphhopper.routing.weighting;

import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.HappyMapsFlagEncoder;
import com.graphhopper.util.EdgeIteratorState;


public class HappyMapsWeighting extends AbstractWeighting {

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

        double weight;

        weight = happymapsFlagEncoder.getNature(edgeState);
        return weight;

//        return edgeState.getDistance();
    }

    @Override
    public String getName() {
        return "happymaps";
    }

}


