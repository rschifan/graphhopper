package com.graphhopper.routing.weighting;

import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.HappyMapsFlagEncoder;
import com.graphhopper.util.EdgeIteratorState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HappyMapsWeighting extends AbstractWeighting {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final HappyMapsFlagEncoder happymapsFlagEncoder;

    public HappyMapsWeighting(FlagEncoder flagEncoder) {
        super(flagEncoder);

        happymapsFlagEncoder = (HappyMapsFlagEncoder) flagEncoder;

        logger.info("\n\n\n\nHappyMapsWeighting: create\n\n\n\n");

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


