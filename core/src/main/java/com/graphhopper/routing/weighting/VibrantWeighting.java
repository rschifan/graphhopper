package com.graphhopper.routing.weighting;

import com.graphhopper.GraphHopper;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.HintsMap;
import com.graphhopper.routing.util.RunningFlagEncoder;
import com.graphhopper.routing.util.TurnCostEncoder;
import com.graphhopper.storage.TurnCostExtension;
import com.graphhopper.storage.WeightsStorage;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.PMap;


public class VibrantWeighting extends FastestWeighting {

    public static final int KEY = 101;
    private final double minFactor;

    private final RunningFlagEncoder runningFlagEncoder;

    private String prevName;
    private HintsMap hintsMap;
    private GraphHopper engine;

    private double beta1 = (double)1/3;
    private double beta2 = (double)1/3;
    private double beta3 = (double)1/3;

    public VibrantWeighting(HintsMap hintsMap, FlagEncoder flagEncoder, GraphHopper engine) {
        this(hintsMap, flagEncoder, engine, new PMap(0));
    }

    public VibrantWeighting(HintsMap hintsMap, FlagEncoder flagEncoder, GraphHopper engine, PMap pMap) {

        super(flagEncoder, pMap);

        this.hintsMap = hintsMap;
        this.runningFlagEncoder = (RunningFlagEncoder) flagEncoder;
        this.engine = engine;

        initHyperParameters();
        printHyperParameters();

        double maxPriority = 1; // BEST / BEST
        minFactor = 1 / (0.5 + maxPriority);
    }

    public void initHyperParameters(){
        this.beta1 = this.hintsMap.getDouble("beta1", (double)1/3);
        this.beta2 = this.hintsMap.getDouble("beta2", (double)1/3);
        this.beta3 = this.hintsMap.getDouble("beta3", (double)1/3);
    }

    public void printHyperParameters(){
        System.out.println("Parameters: beta1="+this.beta1+", beta2="+this.beta2+", beta3="+this.beta3);
    }

    @Override
    public double getMinWeight(double distance) {
        return minFactor * super.getMinWeight(distance);
    }


//    public double getEnvironment(long osmwayid){
//        double natureSmell = WeightsStorage.getInstance().getNatureSmell(osmwayid);
//        double animalSmell = WeightsStorage.getInstance().getAnimalSmell(osmwayid);
//        double chemicalsSmell = WeightsStorage.getInstance().getChemicalSmell(osmwayid);
//        double syntheticSmell = WeightsStorage.getInstance().getSyntheticSmell(osmwayid);
//        double emissionsSmell = WeightsStorage.getInstance().getEmissionsSmell(osmwayid);
//        double foodSmell = WeightsStorage.getInstance().getFoodSmell(osmwayid);
//
//        double natureSound = WeightsStorage.getInstance().getNatureSound(osmwayid);
//        double transportSound = WeightsStorage.getInstance().getTransportSound(osmwayid);
//        double musicSound = WeightsStorage.getInstance().getMusicSound(osmwayid);
//        double humanSound = WeightsStorage.getInstance().getHumanSound(osmwayid);
//
//        double smell = 1.4 * natureSmell - 0.17 * foodSmell - 1.4 * emissionsSmell - 1.4 * chemicalsSmell - 0.81 * syntheticSmell - 0.19 * animalSmell;
//        double sound = 1.1 * natureSound - 0.68 * transportSound + 0.67 * musicSound + 0.1 * humanSound;
//        double beauty = 0.42 * WeightsStorage.getInstance().getBeauty(osmwayid);
//
//        return (smell+sound+beauty)/3;
//    }

//    public double getSafety(long osmwayid){
//        return WeightsStorage.getInstance().getCrime(osmwayid);
//
//    }

    public double getEnvironment(long osmWayID){
        return WeightsStorage.getInstance().getEnvironmentVibrant(osmWayID);
    }

    public double getSupport(long osmWayID){
        return WeightsStorage.getInstance().getSupportVibrant(osmWayID);
    }

    public double getSafety(long osmWayID){
        return WeightsStorage.getInstance().getSafetyVibrant(osmWayID);
    }


    @Override
    public double calcWeight(EdgeIteratorState edgeState, boolean reverse, int prevOrNextEdgeId) {

        double weight = super.calcWeight(edgeState, reverse, prevOrNextEdgeId);

        String currentName = edgeState.getName();
        if (!currentName.equals(prevName)) {
            prevName = currentName;
        }

        int edgeId = edgeState.getEdge();
        long osmWayID = engine.getOSMWay(edgeId);

        double environment = this.getEnvironment(osmWayID);
        double support = this.getSupport(osmWayID);
        double safety = this.getSafety(osmWayID);

        if (Double.isInfinite(weight))
            return Double.POSITIVE_INFINITY;

        double runningScore = beta1*environment+beta2*support+beta3*safety;
        double streetScore = flagEncoder.getDouble(edgeState.getFlags(), KEY);

        return weight / (0.5 + (0.5 * streetScore + 0.5 * runningScore));
    }

    @Override
    public String getName() {
        return "vibrant";
    }

}


