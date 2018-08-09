package com.graphhopper.routing.util;


import com.graphhopper.reader.ReaderWay;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.PMap;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Arrays;

public class HappyMapsFlagEncoder extends FootFlagEncoder {
//    private final static Logger logger = LoggerFactory.getLogger(HappyMapsFlagEncoder.class);

    static final int MAX_NATURE = 3;

    protected EncodedDoubleValue natureEncoder;
    protected EncodedValue highwayEncoder;

    private final Map<Long, Map<String, Double>> wayid2weights = new HashMap<>();
    private final Map<String, Integer> highwayMap = new HashMap<>();

    public HappyMapsFlagEncoder(PMap configuration) {
        super(configuration);

        // highway and certain tags like ferry and shuttle_train which can be used here (no logical overlap)
        List<String> highwayList = Arrays.asList(
                /* reserve index=0 for unset roads (not accessible) */
                "_default",
                "motorway", "motorway_link", "motorroad", "trunk", "trunk_link",
                "primary", "primary_link", "secondary", "secondary_link", "tertiary", "tertiary_link",
                "unclassified", "residential", "living_street", "service", "road", "track",
                "forestry", "cycleway", "steps", "path", "footway", "pedestrian",
                "ferry", "shuttle_train");
        int counter = 0;
        for (String hw : highwayList) {
            highwayMap.put(hw, counter++);
        }

        loadCustomWeights();
    }

    public String getHighwayString(Integer code){

        String key= null;

        for(Map.Entry entry: highwayMap.entrySet()){
            if(code.equals(entry.getValue())) {
                key = (String) entry.getKey();
                break; //breaking because its one to one map
            }
        }
        return key;
    }


    public int getHighway(EdgeIteratorState edge) {
        return (int) highwayEncoder.getValue(edge.getFlags());
    }

    /**
     * Do not use within weighting as this is suboptimal from performance point of view.
     */
    public String getHighwayAsString(EdgeIteratorState edge) {
        int val = getHighway(edge);
        for (Map.Entry<String, Integer> e : highwayMap.entrySet()) {
            if (e.getValue() == val)
                return e.getKey();
        }
        return null;
    }

    int getHighwayValue(ReaderWay way) {
        String highwayValue = way.getTag("highway");

        Integer hwValue = highwayMap.get(highwayValue);

        if (way.hasTag("impassable", "yes") || way.hasTag("status", "impassable"))
            hwValue = 0;

        if (hwValue == null)
            return 0;

        return hwValue;
    }


    @Override
    public double getTurnCost(long flag) {
        return 0;
    }


    /**
     * Some ways are okay but not separate for pedestrians.
     * <p>
     */
    @Override
    public long acceptWay(ReaderWay way) {
        String highwayValue = way.getTag("highway");

        if (highwayValue == null) {

            if ((way.hasTag("route", ferries)) ||
                    (way.hasTag("railway", "platform")) ||
                    (way.hasTag("man_made", "pier")))
                return 0;
        }

        String sacScale = way.getTag("sac_scale");
        if (sacScale != null) {
            if (!"hiking".equals(sacScale) && !"mountain_hiking".equals(sacScale)
                    && !"demanding_mountain_hiking".equals(sacScale) && !"alpine_hiking".equals(sacScale))
                // other scales are too dangerous, see http://wiki.openstreetmap.org/wiki/Key:sac_scale
                return 0;
        }

        // no need to evaluate ferries or fords - already included here
        if (way.hasTag("foot", intendedValues))
            return acceptBit;

        // check access restrictions
        if (way.hasTag(restrictions, restrictedValues) && !getConditionalTagInspector().isRestrictedWayConditionallyPermitted(way))
            return 0;

        if (way.hasTag("sidewalk", sidewalkValues))
            return acceptBit;

        if (!allowedHighwayTags.contains(highwayValue))
            return 0;

        if (way.hasTag("motorroad", "yes"))
            return 0;

        // do not get our feet wet, "yes" is already included above
        if (isBlockFords() && (way.hasTag("highway", "ford") || way.hasTag("ford")))
            return 0;

        if (getConditionalTagInspector().isPermittedWayConditionallyRestricted(way))
            return 0;

        return acceptBit;
    }



    @Override
    public int defineWayBits(int index, int shift) {

        shift = super.defineWayBits(index, shift);

        natureEncoder = new EncodedDoubleValue("Nature", shift, 16, 0.001, 0, MAX_NATURE);
        shift += natureEncoder.getBits();

        highwayEncoder = new EncodedValue("highway", shift, 5, 1, 0, highwayMap.size(), true);
        shift += highwayEncoder.getBits();

        return shift;
    }

    @Override
    public long handleWayTags(ReaderWay way, long allowed, long relationFlags){

        long flags = super.handleWayTags(way, allowed, relationFlags);

        double nature = this.getCustomWeightByWayId(way.getId(), "nature");

        flags = natureEncoder.setDoubleValue(flags, nature);

        // HIGHWAY
        int hwValue = getHighwayValue(way);
        flags = highwayEncoder.setValue(flags, hwValue);

        return flags;
    }


    private void loadCustomWeights() {
        System.out.println("loadCustomWeights");

        HashMap<String, Double> tagsHashMap;

        try {

            File inputF = new File("data/weights/london_happy_maps_norms.csv");
            InputStream inputFS = new FileInputStream(inputF);
            BufferedReader br = new BufferedReader(new InputStreamReader(inputFS));

            long wayid, nWayIds=0;
            double nature;

            // skip header
//          String header = br.readLine();
            String line = br.readLine();

            while (line != null) {

                String[] tt = line.split(",");

                try {
                    wayid = Long.valueOf(tt[0]);
                    nature = Double.valueOf(tt[1]);

                    tagsHashMap = new HashMap<String, Double>();
                    tagsHashMap.put("nature", nature);
                    this.getCustomWeights().put(wayid, tagsHashMap);

                    nWayIds+=1;

                } catch (NumberFormatException e) {
                    System.out.println(e.getMessage());
                }

                line = br.readLine();
            }

            System.out.printf("Read %d custom weights wayids\n",nWayIds);

            br.close();
            inputFS.close();

        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

    }

    public double getNature(EdgeIteratorState edge) {
        long flags = edge.getFlags();
        return natureEncoder.getDoubleValue(flags);
    }


    public Map<String, Double> getCustomWeightsByWayId(long wayid) {
        return this.getCustomWeights().get(new Long(wayid));
    }


    public Double getCustomWeightByWayId(long wayid, String key) {

        Map<String, Double> wayidInfo = this.getCustomWeightsByWayId(wayid);

        if (wayidInfo!=null)
            return wayidInfo.get(key);
        else
            return (double)MAX_NATURE;
    }

    public Map<Long, Map<String, Double>> getCustomWeights() {
        return wayid2weights;
    }


    @Override
    public String toString() {
        return "runhappymaps";
    }



}
