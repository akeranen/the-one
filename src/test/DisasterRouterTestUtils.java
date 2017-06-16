package test;

import routing.prioritizers.DisasterPrioritization;
import routing.prioritizers.DisasterPrioritizationStrategy;
import routing.util.DeliveryPredictabilityStorage;
import routing.util.EncounterValueManager;
import routing.util.ReplicationsDensityManager;

/**
 * Useful functions and constants for all classes related to {@link routing.DisasterRouter}.
 *
 * Created by Britta Heymann on 25.05.2017.
 */
public final class DisasterRouterTestUtils {
    /* Constants needed for delivery predictabilities. */
    public static final double BETA = 0.25;
    public static final double GAMMA = 0.95;
    public static final double SUMMAND = 0.75;
    public static final double SECONDS_IN_TIME_UNIT = 2;

    /* Constants needed for encounter value. */
    public static final double NEW_DATA_WEIGHT = 0.3;
    public static final double EV_WINDOW_LENGTH = 21.3;

    /* Constant needed for replications density. */
    public static final double RD_WINDOW_LENGTH = 12.0;

    /* Constants needed for prioritization. */
    public static final double HEAD_START_THRESHOLD = 30.4;
    public static final int PRIORITY_THRESHOLD = 4;
    public static final double DP_WEIGHT = 0.8;

    /**
     * Private constructor to hide the implicit public one (this is a utility class!).
     */
    private DisasterRouterTestUtils() {
        throw new IllegalAccessError("Utility class");
    }

    public static void addDisasterRouterSettings(TestSettings s) {
        s.setNameSpace(null);
        s.putSetting(DeliveryPredictabilityStorage.BETA_S, Double.toString(BETA));
        s.putSetting(DeliveryPredictabilityStorage.GAMMA_S, Double.toString(GAMMA));
        s.putSetting(DeliveryPredictabilityStorage.SUMMAND_S, Double.toString(SUMMAND));
        s.putSetting(DeliveryPredictabilityStorage.TIME_UNIT_S, Double.toString(SECONDS_IN_TIME_UNIT));

        s.setNameSpace(EncounterValueManager.ENCOUNTER_VALUE_NS);
        s.putSetting(EncounterValueManager.AGING_FACTOR, Double.toString(NEW_DATA_WEIGHT));
        s.putSetting(EncounterValueManager.WINDOW_LENGTH_S, Double.toString(EV_WINDOW_LENGTH));
        s.restoreNameSpace();

        s.setNameSpace(ReplicationsDensityManager.REPLICATIONS_DENSITY_NS);
        s.putSetting(ReplicationsDensityManager.WINDOW_LENGTH_S, Double.toString(RD_WINDOW_LENGTH));
        s.restoreNameSpace();

        s.putSetting(DisasterPrioritizationStrategy.HEAD_START_THRESHOLD_S, Double.toString(HEAD_START_THRESHOLD));
        s.putSetting(DisasterPrioritizationStrategy.PRIORITY_THRESHOLD_S, Double.toString(PRIORITY_THRESHOLD));
        s.putSetting(DisasterPrioritization.DELIVERY_PREDICTABILITY_WEIGHT, Double.toString(DP_WEIGHT));
    }
}
