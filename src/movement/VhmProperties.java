package movement;

import core.Settings;

/**
 * Class storing all properties of the {@link VoluntaryHelperMovement} that can be modified over the settings file.
 *
 * Created by Marius Meyer on 21.04.17.
 */
public class VhmProperties {
    /**
     * setting key for the node being a local helper or a "voluntary ambulance"
     */
    public static final String IS_LOCAL_HELPER_SETTING = "isLocalHelper";
    /**
     * setting key for the time the node will help at a disaster site (seconds)
     */
    public static final String HELP_TIME_SETTING = "helpTime";
    /**
     * setting key for the time the node will stay at the hospital after transporting someone to it (seconds)
     */
    public static final String HOSPITAL_WAIT_TIME_SETTING = "hospitalWaitTime";
    /**
     * setting key for the probability that the node gets injured if an event happens to close to it [0, 1]
     */
    public static final String INJURY_PROBABILITY_SETTING = "injuryProbability";
    /**
     * setting key for the probability that the node stays at the hospital after transporting someone to it [0, 1]
     */
    public static final String HOSPITAL_WAIT_PROBABILITY_SETTING = "hospitalWaitProbability";
    /**
     * setting key for the weight of a disasters intensity for determining
     * if the node will help at the disaster site [0, 1]
     */
    public static final String INTENSITY_WEIGHT_SETTING = "intensityWeight";
    /**
     * default value for the time the node will help at a disaster site (seconds)
     */
    public static final double DEFAULT_HELP_TIME = 3600;
    /**
     * default value for the time the node will stay at the hospital after transporting someone to it (seconds)
     */
    public static final double DEFAULT_HOSPITAL_WAIT_TIME = 3600;
    /**
     * default value for the probability that the node gets injured if an event happens to close to it [0, 1]
     */
    public static final double DEFAULT_INJURY_PROBABILITY = 0.5;
    /**
     * default value for the probability that the node stays at the hospital after transporting someone to it [0, 1]
     */
    public static final double DEFAULT_HOSPITAL_WAIT_PROBABILITY = 0.5;
    /**
     * default value for the weight of a disasters intensity for determining
     * if the node will help at the disaster site [0, 1]
     */
    public static final double DEFAULT_INTENSITY_WEIGHT = 0.5;

    /**
     * tells, if the node is a local helper or a "voluntary ambulance"
     */
    private boolean isLocalHelper;
    /**
     * how long the node will stay at the hospital (seconds)
     */
    private double hospitalWaitTime;
    /**
     * how long the node will help at a disaster site (seconds)
     */
    private double helpTime;
    /**
     * probability that the node gets injured if an event happens to close to it [0, 1]
     */
    private double injuryProbability;
    /**
     * probability that the node stays at the hospital after transporting someone to it [0, 1]
     */
    private double waitProbability;

    /**
     * weight of a disasters intensity for determining if the node will help at the disaster site [0, 1]
     */
    private double intensityWeight;



    public VhmProperties(Settings settings){
        //get all of the settings from the settings file, reverting to defaults, if setting absent in the file
        isLocalHelper = settings.getBoolean(VhmProperties.IS_LOCAL_HELPER_SETTING, false);
        helpTime = settings.getDouble(VhmProperties.HELP_TIME_SETTING, DEFAULT_HELP_TIME);
        hospitalWaitTime = settings.getDouble(HOSPITAL_WAIT_TIME_SETTING,DEFAULT_HOSPITAL_WAIT_TIME);
        injuryProbability = settings.getDouble(INJURY_PROBABILITY_SETTING, DEFAULT_INJURY_PROBABILITY);
        waitProbability = settings.getDouble(HOSPITAL_WAIT_PROBABILITY_SETTING,DEFAULT_HOSPITAL_WAIT_PROBABILITY);
        intensityWeight = settings.getDouble(INTENSITY_WEIGHT_SETTING, DEFAULT_INTENSITY_WEIGHT);
    }

    public VhmProperties(VhmProperties prototype){
        isLocalHelper = prototype.isLocalHelper;
        helpTime = prototype.helpTime;
        hospitalWaitTime = prototype.hospitalWaitTime;
        injuryProbability = prototype.injuryProbability;
        waitProbability = prototype.waitProbability;
        intensityWeight = prototype.intensityWeight;
    }

    public boolean isLocalHelper() {
        return isLocalHelper;
    }

    public void setLocalHelper(boolean localHelper) {
        isLocalHelper = localHelper;
    }

    public double getHospitalWaitTime() {
        return hospitalWaitTime;
    }

    public double getHelpTime() {
        return helpTime;
    }

    public double getInjuryProbability() {
        return injuryProbability;
    }

    public void setInjuryProbability(double injuryProbability) {
        this.injuryProbability = injuryProbability;
    }

    public double getWaitProbability() {
        return waitProbability;
    }

    public void setWaitProbability(double waitProbability) {
        this.waitProbability = waitProbability;
    }

    public double getIntensityWeight() {
        return intensityWeight;
    }

    public void setIntensityWeight(double intensityWeight) {
        this.intensityWeight = intensityWeight;
    }
}
