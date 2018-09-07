package com.graphhopper.util.details;

import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.RunningFlagEncoder;
import com.graphhopper.util.EdgeIteratorState;

import static com.graphhopper.util.Parameters.DETAILS.WAYID;

public class WayidDetails extends AbstractPathDetailsBuilder {

    private long wayid = -1;
    private final RunningFlagEncoder encoder;

    public WayidDetails(FlagEncoder encoder) {

        super(WAYID);
        this.encoder = (RunningFlagEncoder) encoder;
    }

    @Override
    public boolean isEdgeDifferentToLastEdge(EdgeIteratorState edge) {

        long current = encoder.getOSMWayId(edge);

        if (wayid == -1 || wayid!=current) {
            wayid = current;
            return true;
        }
        return false;
    }

    @Override
    public Object getCurrentValue() {
        return wayid;
    }
}