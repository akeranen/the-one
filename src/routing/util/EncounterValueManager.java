package routing.util;

import core.Settings;
import core.SettingsError;

/**
 * Manages a node's encounter value, a rating mechanism to measure a host's popularity.
 *
 * The measure is implemented as described in S. C. Nelson, M. Bakht and R. Kravets: Encounter-Based Routing in
 * Disruption-Tolerant Networks, IEEE INFOCOM, 846-854.
 *
 * Created by Britta Heymann on 14.05.2017.
 */
public class EncounterValueManager extends AbstractIntervalRatingMechanism {
    /**
     * Name space for all encounter value settings.
     */
    public static final String ENCOUNTER_VALUE_NS = "EncounterValue";

    /**
     * Importance of recent encounters relative to previous encounters -setting id ({@value}).
     * A value between 0 and 1.
     * The weight the newly computed encounters per time window rate gets when the old rate is updated.
     */
    public static final String AGING_FACTOR = "agingFactor";

    /**
     * The value of the encounter value ratio if the two compared hosts are equally social.
     */
    private static final double EQUALLY_SOCIAL = 0.5;
    /**
     * The maximal possible value when checking if the encounter value is 0.
     */
    private static final double ZERO_ENCOUNTERS = 0.000001;

    private double agingFactor;

    private double encounterValue;
    private int currentWindowCounter;

    /**
     * Initializes a new instance of the {@link EncounterValueManager} class.
     */
    public EncounterValueManager() {
        super();

        Settings settings = new Settings(ENCOUNTER_VALUE_NS);
        this.agingFactor = settings.getDouble(AGING_FACTOR);
        if (this.agingFactor < 0 || this.agingFactor > 1) {
            throw new SettingsError("Aging factor has to be between 0 and 1!");
        }
    }

    /**
     * Copy constructor. All constants are copied over, but the encounter ratios are not.
     */
    public EncounterValueManager(EncounterValueManager manager) {
        super(manager);
        this.agingFactor = manager.agingFactor;
    }

    /**
     * Returns the namespace for all settings about this rating mechanism.
     * @return The namespace.
     */
    @Override
    protected String getNamespace() {
        return ENCOUNTER_VALUE_NS;
    }

    /**
     * Adds a new encounter that will be considered when computing the encounter value.
     */
    public void addEncounter() {
        this.currentWindowCounter++;
    }

    /**
     * Gets the encounter value.
     * @return The encounter value.
     */
    public double getEncounterValue() {
        return this.encounterValue;
    }

    /**
     * Updates the encounter value if a time window has ended.
     */
    @Override
    protected void updateRatingMechanism() {
        this.encounterValue =
                this.agingFactor * this.currentWindowCounter + (1 - this.agingFactor) * this.encounterValue;
        this.currentWindowCounter = 0;
    }

    /**
     * Computes a ratio between the encounter value managed by this instance and the provided encounter value.
     * A ratio less than 0.5 signifies that the other host is less social than the one this manager is attached to, a
     * ratio higher than 0.5 signifies the opposite.
     *
     * @param otherEncounterValue The encounter value to compare the managed encounter value to.
     * @return A ratio between 0 and 1.
     */
    public double computeEncounterValueRatio(double otherEncounterValue) {
        if (this.encounterValue < ZERO_ENCOUNTERS && otherEncounterValue < ZERO_ENCOUNTERS) {
            return EQUALLY_SOCIAL;
        }
        return otherEncounterValue / (this.encounterValue + otherEncounterValue);
    }

    /**
     * Returns the aging factor, i.e. the weight new data gets on updates.
     */
    public double getAgingFactor() {
        return this.agingFactor;
    }
}
