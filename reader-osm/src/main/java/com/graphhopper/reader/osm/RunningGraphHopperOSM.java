package com.graphhopper.reader.osm;

import com.graphhopper.json.geo.JsonFeatureCollection;
import com.graphhopper.storage.DataAccess;
import com.graphhopper.storage.Directory;
import com.graphhopper.storage.WeightsStorage;
import com.graphhopper.util.BitUtil;
import com.graphhopper.reader.DataReader;
import com.graphhopper.storage.GraphHopperStorage;


public class RunningGraphHopperOSM extends GraphHopperOSM {

    private static RunningGraphHopperOSM INSTANCE;

    // mapping of internal edge ID to OSM way ID
    private DataAccess edgeMapping;
    private BitUtil bitUtil;

    private final JsonFeatureCollection landmarkSplittingFeatureCollection;

    private RunningGraphHopperOSM() {
        this(null);
    }

    private RunningGraphHopperOSM(JsonFeatureCollection landmarkSplittingFeatureCollection) {
        super();
        this.landmarkSplittingFeatureCollection = landmarkSplittingFeatureCollection;

        System.out.println("\n\n\nRunningGraphHopperOSM: init\n\n\n");

    }

    public static RunningGraphHopperOSM getInstance() {
        if(INSTANCE == null) {
            INSTANCE = new RunningGraphHopperOSM();
        }
        return INSTANCE;
    }

    public static RunningGraphHopperOSM getInstance(JsonFeatureCollection landmarkSplittingFeatureCollection) {
        if(INSTANCE == null) {
            INSTANCE = new RunningGraphHopperOSM(landmarkSplittingFeatureCollection);
        }
        return INSTANCE;
    }

    @Override
    public boolean load(String graphHopperFolder) {
        boolean loaded = super.load(graphHopperFolder);

        Directory dir = getGraphHopperStorage().getDirectory();
        bitUtil = BitUtil.get(dir.getByteOrder());
        edgeMapping = dir.find("edge_mapping");

        if (loaded) {
            edgeMapping.loadExisting();
        }

        return loaded;
    }

    @Override
    protected DataReader createReader(GraphHopperStorage ghStorage) {
        OSMReader reader = new OSMReader(ghStorage) {

            {
                edgeMapping.create(1000);
            }

            // this method is only in >0.6 protected, before it was private
            @Override
            protected void storeOsmWayID(int edgeId, long osmWayId) {
                super.storeOsmWayID(edgeId, osmWayId);

                long pointer = 8L * edgeId;
                edgeMapping.ensureCapacity(pointer + 8L);

                edgeMapping.setInt(pointer, bitUtil.getIntLow(osmWayId));
                edgeMapping.setInt(pointer + 4, bitUtil.getIntHigh(osmWayId));
            }

            @Override
            protected void finishedReading() {
                super.finishedReading();

                edgeMapping.flush();
            }
        };

        return initDataReader(reader);
    }

    public long getOSMWay(int internalEdgeId) {
        long pointer = 8L * internalEdgeId;
        return bitUtil.combineIntsToLong(edgeMapping.getInt(pointer), edgeMapping.getInt(pointer + 4L));
    }

}
