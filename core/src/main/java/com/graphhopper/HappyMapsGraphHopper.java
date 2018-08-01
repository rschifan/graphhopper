package com.graphhopper;

import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.HintsMap;
import com.graphhopper.routing.weighting.HappyMapsWeighting;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HappyMapsGraphHopper extends GraphHopper {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public Weighting createWeighting(HintsMap hintsMap, FlagEncoder encoder, Graph graph) {

        String weightingStr = hintsMap.getWeighting().toLowerCase();

        if ("happymaps".equals(weightingStr)) {
            return new HappyMapsWeighting(encoder);
        } else {
            return super.createWeighting(hintsMap, encoder, graph);
        }
    }


}
