package com.graphhopper.storage;

import java.io.*;
import java.util.HashMap;


public final class WeightsStorage {

    private static WeightsStorage INSTANCE;
    private HashMap<Long, Float[]> weights;

    // ENVIRONMENT
    private short SMELL_NATURE = 0;
    private short SMELL_ANIMAL = 1;
    private short SMELL_FOOD = 2;
    private short SMELL_EMISSIONS = 3;
    private short SMELL_CHEMICAL = 4;
    private short SMELL_SYNTHETICS = 5;

    private short SOUNDS_NATURE = 6;
    private short SOUNDS_TRANSPORT = 7;
    private short SOUNDS_MUSIC = 8;
    private short SOUNDS_HUMAN = 9;

    private short BEAUTY = 10;

    // SAFETY
    private short CRIME = 11;


    // AGGREGATED
    private short ENVIRONMENT_MINDFUL = 0;
    private short SUPPORT_MINDFUL = 1;
    private short SAFETY_MINDFUL = 2;
    private short ENVIRONMENT_VIBRANT = 3;
    private short SUPPORT_VIBRANT = 4;
    private short SAFETY_VIBRANT = 5;





    private WeightsStorage() {

        this.weights = new HashMap<Long, Float[]>();
        this.load();

    }

    public static WeightsStorage getInstance() {
        if(INSTANCE == null) {
            INSTANCE = new WeightsStorage();
        }
        return INSTANCE;
    }

    public void load(){

        try {

            File inputF = new File("data/weighting/wayid2weights.csv");
            InputStream inputFS = new FileInputStream(inputF);
            BufferedReader br = new BufferedReader(new InputStreamReader(inputFS));

            long wayid;

            // skip header
//          String header = br.readLine();
            String line = br.readLine();

            while (line != null) {

                String[] tt = line.split(",");

                try {
                    wayid = Long.valueOf(tt[0]);
                    Float[] values = new Float[6];

                    values[ENVIRONMENT_MINDFUL] = Float.valueOf(tt[2]);
                    values[SUPPORT_MINDFUL] = Float.valueOf(tt[3]);
                    values[SAFETY_MINDFUL] = Float.valueOf(tt[4]);
                    values[ENVIRONMENT_VIBRANT] = Float.valueOf(tt[5]);
                    values[SUPPORT_VIBRANT] = Float.valueOf(tt[6]);
                    values[SAFETY_VIBRANT] = Float.valueOf(tt[7]);

                    // LOAD VALUES!
                    this.weights.put(wayid, values);

                } catch (NumberFormatException e) {
                    System.out.println(e.getMessage());
                }

                line = br.readLine();
            }

            br.close();
            inputFS.close();

        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

    }


    // getters and setters

    private float getWeight(long wayid, short code, float defaultValue){
        if (weights.containsKey(wayid))
            return weights.get(wayid)[code];
        else return defaultValue;
    }


    // VIBRANT
    // ENVIRONMENT
    public float getEnvironmentVibrant(long wayid){
        return getWeight(wayid, ENVIRONMENT_VIBRANT, 0);
    }
    // SUPPORT
    public float getSupportVibrant(long wayid){
        return getWeight(wayid, SUPPORT_VIBRANT, 0);
    }
    // SAFETY
    public float getSafetyVibrant(long wayid){
        return getWeight(wayid, SAFETY_VIBRANT, 0);
    }


    // MINDFUL
    // ENVIRONMENT
    public float getEnvironmentMindful(long wayid){
        return getWeight(wayid, ENVIRONMENT_MINDFUL, 0);
    }
    // SUPPORT
    public float getSupportMindful(long wayid){
        return getWeight(wayid, SUPPORT_MINDFUL, 0);
    }
    // SAFETY
    public float getSafetyMindful(long wayid){
        return getWeight(wayid, SAFETY_MINDFUL, 0);
    }





    // SMELL
    public float getNatureSmell(long wayid){
        return getWeight(wayid, SMELL_NATURE, 0);
    }

    public float getAnimalSmell(long wayid){
        return getWeight(wayid, SMELL_ANIMAL, 0);
    }

    public float getFoodSmell(long wayid){
        return getWeight(wayid, SMELL_FOOD, 0);
    }

    public float getEmissionsSmell(long wayid){
        return getWeight(wayid, SMELL_EMISSIONS, 0);
    }

    public float getChemicalSmell(long wayid){
        return getWeight(wayid, SMELL_CHEMICAL, 0);
    }

    public float getSyntheticSmell(long wayid){
        return getWeight(wayid, SMELL_SYNTHETICS, 0);
    }

    // SOUND

    public float getNatureSound(long wayid){
        return getWeight(wayid, SOUNDS_NATURE, 0);
    }

    public float getTransportSound(long wayid){
        return getWeight(wayid, SOUNDS_TRANSPORT, 0);
    }

    public float getMusicSound(long wayid){
        return getWeight(wayid, SOUNDS_MUSIC, 0);
    }

    public float getHumanSound(long wayid){
        return getWeight(wayid, SOUNDS_HUMAN, 0);
    }

    // BEAUTY
    public float getBeauty(long wayid){
        return getWeight(wayid, BEAUTY, 0);
    }

    // SAFETY
    public float getCrime(long wayid){
        return getWeight(wayid, CRIME, 0);
    }


}
