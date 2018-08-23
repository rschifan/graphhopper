package com.graphhopper.routing.util;


import com.graphhopper.reader.ReaderWay;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.PMap;

import java.io.*;
import java.util.*;

public class HappyMapsFlagEncoder extends FootFlagEncoder {

    static final int MAX_BEAUTY = 1;

    // Encoders
    protected EncodedDoubleValue beautyEncoder;
    protected EncodedValue qualityHighwayEncoder;

    protected EncodedValue highwayTypeEncoder;
    protected EncodedValue OSMWayIdEncoder;

    // Quality features
    protected Set<String> sidewalkTypes;
    protected Set<String> surfaceTypes;
    protected Set<String> trackType;
    protected Set<String> crossingTypes;

    protected Set<String> designatedSurfaceTypes;
    protected Set<String> accessibleSurfaceTypes;
    protected Set<String> avoidedSurfaceTypes;

    protected Set<String> designatedCrossingTypes;
    protected Set<String> accessibleCrossingTypes;
    protected Set<String> avoidedCrossingTypes;


    private final Map<Long, Map<String, Double>> wayid2weights = new HashMap<>();
    private final Map<String, Integer> highwayMap = new HashMap<>();
    private final Map<Long, Long> idx2OSMWayId = new HashMap<>();
    private long idx = 0;

    public HappyMapsFlagEncoder(PMap configuration) {
        super(configuration);

        // Quality Features
        initSurfaceProperty();
        initSidewalkProperty();
        initTrackTypeProperty();
        initCrossingProperty();

        // OSM Highway Type Features
        initOSMHighwayProperty();

        loadCustomWeights();
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
        safeHighwayTags.add("living_street");
        safeHighwayTags.add("pedestrian");
        safeHighwayTags.add("footway");
        safeHighwayTags.add("path");
        safeHighwayTags.add("track");
        safeHighwayTags.add("residential");

        allowedHighwayTags.clear();
        for (String current: allowedHighwayTagsArray)
            allowedHighwayTags.add(current);

        avoidHighwayTags.clear();
        for (String current: avoidHighwayTagsArray)
            avoidHighwayTags.add(current);
    }

    final int DEFAULT_CROSSING_SCORE = 0;
    final int DEFAULT_SURFACE_SCORE = 0;
    final int DEFAULT_SIDEWALK_SCORE = 0;
    final int DEFAULT_TRACKTYPE_SCORE = 1;

    protected void initSurfaceProperty(){

        // https://wiki.openstreetmap.org/wiki/Key:surface
        surfaceTypes = new HashSet<String>(){{
            // official
            add("paved");add("unpaved");add("asphalt");add("concrete");
            add("paving_stones");add("cobblestone");add("sett");
            add("metal");add("wood");add("compacted");add("fine_gravel");add("gravel");add("pebblestone");
            add("grass_paver");add("grass");add("dirt");add("earth");add("mud");add("ground");add("sand");
            // unofficial
            add("tarmac");add("bricks");add("brick");add("clay");add("grit");add("stone");add("tartan");
            add("woodchips");add("tiles");
        }};

        designatedSurfaceTypes = new HashSet<String>(){
            {
                add("paved");
                add("asphalt");
                add("concrete");
                add("tarmac");
                add("metal");
                add("wood");
                add("compacted");
                add("fine_gravel");
                add("unpaved");
                add("grass_paver");
                add("grass");
                add("earth");
                add("ground");
                add("grit");
            }};

        accessibleSurfaceTypes = new HashSet<String>(){
            {
                add("bricks");
                add("brick");
                add("stone");
                add("tartan");
                add("woodchips");
                add("tiles");
                add("pebblestone");
                add("cobblestone");
                add("sett");
            }};

        avoidedSurfaceTypes = new HashSet<String>(){
            {
                add("clay");
                add("gravel");
                add("mud");
                add("sand");
                add("dirt");
            }};
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

    protected void initTrackTypeProperty(){
        // https://wiki.openstreetmap.org/wiki/Key:tracktype
        trackType = new HashSet<String>(){{
            add("grade1");add("grade2");add("grade3");add("grade4");add("grade5");
        }};
    }

    protected void initCrossingProperty(){
        // https://wiki.openstreetmap.org/wiki/Approved_features/Road_crossings
        crossingTypes = new HashSet<String>(){{
            // official
            add("traffic_signals");add("uncontrolled");add("unmarked");add("island");add("no");
            // unofficial
            add("zebra");add("toucan");add("pelican");add("traffic_lights");add("pedestrian_signals");add("yes");
        }};

        designatedCrossingTypes = new HashSet<String>(){
            {
                add("uncontrolled");
                add("traffic_signals");
                add("zebra");
                add("toucan");
                add("pelican");
                add("pedestrian_signals");
            }};

        accessibleCrossingTypes = new HashSet<String>(){
            {
                add("unmarked");
                add("yes");
                add("island");
                add("traffic_lights");
            }};

        avoidedCrossingTypes = new HashSet<String>(){
            {
                add("no");
            }};



    }

    protected int getSurfaceScore(ReaderWay way){
        String surface = way.getTag("surface");

        if (surface!=null){
            if (designatedSurfaceTypes.contains(surface))
                return 2;
            if (accessibleSurfaceTypes.contains(surface))
                return 1;
            else
                return 0;
        }
        return DEFAULT_SURFACE_SCORE;
    }

    protected int getSidewalkScore(ReaderWay way){
        String sidewalk = way.getTag("sidewalk");

        // left, right, both, yes, separate, none, no, shared
        if (sidewalk!=null){
            if (sidewalk.equalsIgnoreCase("both"))
                return 2;
            if ((sidewalk.equalsIgnoreCase("yes")) ||
                    (sidewalk.equalsIgnoreCase("left")) ||
                    (sidewalk.equalsIgnoreCase("right")) ||
                    (sidewalk.equalsIgnoreCase("separate")))
                return 1;
            else
                return 0;
        }
        return DEFAULT_SIDEWALK_SCORE;
    }

    protected int getTrackTypeScore(ReaderWay way){
        String tracktype = way.getTag("tracktype");

        if (tracktype!=null){
            if ((tracktype.equalsIgnoreCase("grade1")) ||
                    (tracktype.equalsIgnoreCase("grade2")))
                return 2;
            if ((tracktype.equalsIgnoreCase("grade3")) ||
                    (tracktype.equalsIgnoreCase("grade4")))
                return 1;
            else
                return 0;
        }
        return DEFAULT_TRACKTYPE_SCORE;
    }

    protected int getCrossingScore(ReaderWay way){
        String crossing = way.getTag("crossing");

        if (crossing!=null){
            if (designatedCrossingTypes.contains(crossing))
                return 2;
            if (accessibleCrossingTypes.contains(crossing))
                return 1;
            else
                return 0;
        }
        return DEFAULT_CROSSING_SCORE;
    }

    protected int getQualityScore(ReaderWay way){
        int surfaceScore = getSurfaceScore(way);
        int sidewalkScore = getSidewalkScore(way);
        int tracktypeScore = getTrackTypeScore(way);
        int crossingScore = getCrossingScore(way);

        return (surfaceScore+sidewalkScore+tracktypeScore+crossingScore);
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

            // Rules for ways that are not highways
            // TODO: Disable ferries, trains, and public transportation
            long acceptPotentially = 0;

            if (way.hasTag("route", ferries)) {
                String footTag = way.getTag("foot");
                if (footTag == null || "yes".equals(footTag))
                    acceptPotentially = acceptBit | ferryBit;
            }

            // special case not for all acceptedRailways, only platform
            if (way.hasTag("railway", "platform"))
                acceptPotentially = acceptBit;

            if (way.hasTag("man_made", "pier"))
                acceptPotentially = acceptBit;

            if (acceptPotentially != 0) {
                if (way.hasTag(restrictions, restrictedValues) && !getConditionalTagInspector().isRestrictedWayConditionallyPermitted(way))
                    return 0;
                return acceptPotentially;
            }

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

        beautyEncoder = new EncodedDoubleValue("Nature", shift, 16, 0.001, 0, MAX_BEAUTY);
        shift += beautyEncoder.getBits();

        highwayTypeEncoder = new EncodedValue("highway", shift, 5, 1, 0, highwayMap.size(), true);
        shift += highwayTypeEncoder.getBits();

        OSMWayIdEncoder = new EncodedValue("wayid", shift, 24, 1, 0, 8388608);
        shift += OSMWayIdEncoder.getBits();

        qualityHighwayEncoder = new EncodedValue("quality", shift, 4, 1, 0, 15, true);
        shift += qualityHighwayEncoder.getBits();

        return shift;
    }

    @Override
    public long handleWayTags(ReaderWay way, long allowed, long relationFlags){

        long flags = super.handleWayTags(way, allowed, relationFlags);

        double nature = this.getCustomWeightByWayId(way.getId(), "nature");

        flags = beautyEncoder.setDoubleValue(flags, nature);
        // HIGHWAY
        int hwValue = getHighwayType(way);
        flags = highwayTypeEncoder.setValue(flags, hwValue);

        // WAYID
        idx2OSMWayId.put(new Long(idx), new Long(way.getId()));
        flags = OSMWayIdEncoder.setValue(flags, idx);
        idx++;

        // QUALITY
        flags = qualityHighwayEncoder.setValue(flags, getQualityScore(way));


        return flags;
    }

    void collect(ReaderWay way, TreeMap<Double, Integer> weightToPrioMap) {
        super.collect(way, weightToPrioMap);

        // TODO modify the priority according to the highways rules defined in the survey

//        String highway = way.getTag("highway");
//
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

    public double getBeautyScore(EdgeIteratorState edge) {
        long flags = edge.getFlags();
        return beautyEncoder.getDoubleValue(flags);
    }

    public long getQualityScore(EdgeIteratorState edge){
        long flags = edge.getFlags();
        return qualityHighwayEncoder.getValue(flags);
    }

    public long getOSMWayId(EdgeIteratorState edge) {
        long flags = edge.getFlags();
        long current = OSMWayIdEncoder.getValue(flags);
        return idx2OSMWayId.get(current);
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

    // Weights

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

    private Map<String, Double> getCustomWeightsByWayId(long wayid) {
        return this.getCustomWeights().get(new Long(wayid));
    }

    private Double getCustomWeightByWayId(long wayid, String key) {

        Map<String, Double> wayidInfo = this.getCustomWeightsByWayId(wayid);

        if (wayidInfo!=null)
            return wayidInfo.get(key);
        else
            return (double)0;
    }

    private Map<Long, Map<String, Double>> getCustomWeights() {
        return wayid2weights;
    }


    @Override
    public String toString() {
        return "runhappymaps";
    }



}
