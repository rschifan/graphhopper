package com.graphhopper.routing.util;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.PMap;

import static com.graphhopper.routing.util.PriorityCode.*;

import java.util.*;


public class RunningFlagEncoder extends FootFlagEncoder {

    // Encoders
    protected EncodedValue highwayTypeEncoder;
    protected EncodedValue OSMWayIdEncoder;


    protected Set<String> sidewalkTypes;

    private final Map<String, Integer> highwayMap = new HashMap<>();
    private final Map<Long, Long> idx2OSMWayId = new HashMap<>();
    private long idx = 0;

    public RunningFlagEncoder(PMap configuration) {
        super(configuration);

        initSidewalkProperty();
        initOSMHighwayProperty();
    }

    protected void initOSMHighwayProperty(){
        List<String> highwayList = Arrays.asList(
                /* reserve index=0 for unset roads (not accessible) */

                "_default",

                "motorway", "motorway_link", "motorroad", "trunk", "trunk_link",
                "bus_guideway", "escape", "cycleway", "raceway", "bridleway", "proposed", "construction",

                "primary", "primary_link", "secondary", "secondary_link", "tertiary", "tertiary_link",
                "unclassified", "residential", "living_street", "service", "road", "track",
                "steps", "path", "footway", "pedestrian");

        int counter = 0;
        for (String hw : highwayList) {
            highwayMap.put(hw, counter++);
        }

        // set up allowed, safe, and avoided highway maps
        String[] avoidHighwayTagsArray = {
                // Roads
                "motorway",
                // Link roads
                "motorway_link"
        };

        String[] allowedHighwayTagsArray = {
                // Roads
                "trunk", "primary", "secondary", "tertiary",
                "unclassified", "residential", "service",
                // Link roads
                "trunk_link", "primary_link", "secondary_link", "tertiary_link",
                // Special road types
                "living_street", "pedestrian", "track", "bus_guideway", "escape", "raceway", "road",
                // Paths
                "footway", "bridleway", "steps", "path", "cycleway",
                // Lifecycle
                "proposed", "construction"
        };

        // Original: [path, residential, service, footway, pedestrian, living_street, track, steps]
        safeHighwayTags.clear();
        safeHighwayTags.add("pedestrian");
        safeHighwayTags.add("track");
        safeHighwayTags.add("living_street");
        safeHighwayTags.add("footway");
        safeHighwayTags.add("path");
        safeHighwayTags.add("residential");

        allowedHighwayTags.clear();
        for (String current: allowedHighwayTagsArray)
            allowedHighwayTags.add(current);

        avoidHighwayTags.clear();
        for (String current: avoidHighwayTagsArray)
            avoidHighwayTags.add(current);
    }

    protected void initSidewalkProperty(){
        // https://wiki.openstreetmap.org/wiki/Key:sidewalk
        sidewalkTypes = new HashSet<String>(){{
            // official
            add("no");add("left");add("right");add("both");
            add("separate");add("none");add("yes");
            // unofficial
            add("shared");
        }};
    }

    @Override
    public double getTurnCost(long flag) {
        return 0;
    }

    @Override
    public int defineWayBits(int index, int shift) {

        shift = super.defineWayBits(index, shift);

        highwayTypeEncoder = new EncodedValue("highway", shift, 5, 1, 0, highwayMap.size(), true);
        shift += highwayTypeEncoder.getBits();

        OSMWayIdEncoder = new EncodedValue("wayid", shift, 24, 1, 0, 8388608);
        shift += OSMWayIdEncoder.getBits();

        return shift;
    }


    @Override
    public long acceptWay(ReaderWay way) {

        String highwayValue = way.getTag("highway");

        if (highwayValue == null) {

//            long acceptPotentially = 0;
//
//            if (acceptPotentially != 0) {
//                if (way.hasTag(restrictions, restrictedValues) && !getConditionalTagInspector().isRestrictedWayConditionallyPermitted(way))
//                    return 0;
//                return acceptPotentially;
//            }

            return 0;
        }

//        if (way.hasTag("foot", intendedValues))
//            return acceptBit;

        // check access restrictions
        if (way.hasTag(restrictions, restrictedValues) && !getConditionalTagInspector().isRestrictedWayConditionallyPermitted(way))
            return 0;

//        if (way.hasTag("sidewalk", sidewalkValues))
//            return acceptBit;

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
    public long handleWayTags(ReaderWay way, long allowed, long relationFlags){
        long flags = super.handleWayTags(way, allowed, relationFlags);

        int hwValue = getHighwayType(way);
        flags = highwayTypeEncoder.setValue(flags, hwValue);

        // WAYID
        idx2OSMWayId.put(new Long(idx), new Long(way.getId()));
        flags = OSMWayIdEncoder.setValue(flags, idx);
        idx++;

        return flags;
    }





    String[] permittedHighwayTagsArray = {
            "primary",
            "secondary",
            "tertiary",
            "primary_link",
            "secondary_link",
            "tertiary_link",
            "service",
            "road",
            "unclassified",
            "residential",
            "trunk"
    };
    ArrayList<String> permittedHighwayTagsList =
            new ArrayList<String>(Arrays.asList(permittedHighwayTagsArray));




    void collect(ReaderWay way, TreeMap<Double, Integer> weightToPrioMap) {
//        super.collect(way, weightToPrioMap);

        // TODO modify the priority according to the highways rules defined in the survey

        String highway = way.getTag("highway");

        if (safeHighwayTags.contains(highway))
            weightToPrioMap.put(100d, BEST.getValue());
        else if (permittedHighwayTagsList.contains(highway)){
            if (way.hasTag("sidewalk", sidewalksNoValues))
                weightToPrioMap.put(45d, AVOID_IF_POSSIBLE.getValue());
            else weightToPrioMap.put(45d, UNCHANGED.getValue());
        }
        else weightToPrioMap.put(45d, REACH_DEST.getValue());

//        if (way.hasTag("foot", "designated"))
//            weightToPrioMap.put(100d, PREFER.getValue());
//
//        double maxSpeed = getMaxSpeed(way);
//        if (safeHighwayTags.contains(highway) || maxSpeed > 0 && maxSpeed <= 20) {
//            weightToPrioMap.put(40d, PREFER.getValue());
//            if (way.hasTag("tunnel", intendedValues)) {
//                if (way.hasTag("sidewalk", sidewalksNoValues))
//                    weightToPrioMap.put(40d, AVOID_IF_POSSIBLE.getValue());
//                else
//                    weightToPrioMap.put(40d, UNCHANGED.getValue());
//            }
//        } else if (maxSpeed > 50 || avoidHighwayTags.contains(highway)) {
//            if (!way.hasTag("sidewalk", sidewalkValues))
//                weightToPrioMap.put(45d, AVOID_IF_POSSIBLE.getValue());
//        }
//
//        if (way.hasTag("bicycle", "official") || way.hasTag("bicycle", "designated"))
//            weightToPrioMap.put(44d, AVOID_IF_POSSIBLE.getValue());

    }


    public long getOSMWayId(EdgeIteratorState edge) {
        long flags = edge.getFlags();
        long current = OSMWayIdEncoder.getValue(flags);
        return current;
//        return idx2OSMWayId.get(current);
    }

    public String getHighwayTypeString(Integer code){

        String key= null;

        for(Map.Entry entry: highwayMap.entrySet()){
            if(code.equals(entry.getValue())) {
                key = (String) entry.getKey();
                break; //breaking because its one to one map
            }
        }
        return key;
    }

    public int getHighwayType(EdgeIteratorState edge) {
        return (int) highwayTypeEncoder.getValue(edge.getFlags());
    }

    private int getHighwayType(ReaderWay way) {
        String highwayValue = way.getTag("highway");

        Integer hwValue = highwayMap.get(highwayValue);

        if (way.hasTag("impassable", "yes") || way.hasTag("status", "impassable"))
            hwValue = 0;

        if (hwValue == null)
            return 0;

        return hwValue;
    }

    @Override
    public String toString() {
        return "running";
    }

}
