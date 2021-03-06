package com.graphhopper.util.details;

import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.RunningFlagEncoder;
import com.graphhopper.util.EdgeIteratorState;

import static com.graphhopper.util.Parameters.DETAILS.HIGHWAY;

public class HighwayDetails extends AbstractPathDetailsBuilder {

    private int highway = -1;
    private final RunningFlagEncoder encoder;

    public HighwayDetails(FlagEncoder encoder) {

        super(HIGHWAY);
        this.encoder = (RunningFlagEncoder) encoder;
    }

    @Override
    public boolean isEdgeDifferentToLastEdge(EdgeIteratorState edge) {

        int current = encoder.getHighwayType(edge);

        if (highway == -1 || highway!=current) {
            highway = current;
            return true;
        }
        return false;
    }

    @Override
    public Object getCurrentValue() {
        return encoder.getHighwayTypeString(new Integer(highway));
    }
}