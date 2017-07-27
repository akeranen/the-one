package test;

import routing.choosers.UtilityMessageChooser;
import routing.prioritizers.DisasterPrioritization;
import routing.prioritizers.DisasterPrioritizationStrategy;
import routing.util.DeliveryPredictabilityStorage;
import routing.util.DisasterBufferComparator;
import routing.util.EncounterValueManager;
import routing.util.EnergyModel;
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
    public static final double DP_WINDOW_LENGTH = 2;

    /* Constants needed for encounter value. */
    public static final double NEW_DATA_WEIGHT = 0.3;
    public static final double EV_WINDOW_LENGTH = 21.3;

    /* Constant needed for replications density. */
    public static final double RD_WINDOW_LENGTH = 12.0;

    /* Constants needed for prioritization. */
    public static final double HEAD_START_THRESHOLD = 30.4;
    public static final int PRIORITY_THRESHOLD = 4;
    public static final double DP_WEIGHT = 0.8;

    /* Constants needed for message choosing. */
    static final double DELIVERY_PREDICTABILITY_WEIGHT = 0.95;
    static final double POWER_WEIGHT = 0.05;
    static final double PROPHET_PLUS_WEIGHT = 0.65;
    static final double REPLICATIONS_DENSITY_WEIGHT = 0.25;
    static final double ENCOUNTER_VALUE_WEIGHT = 0.1;
    static final double UTILITY_THRESHOLD = 0.2;

    /* Constants needed for buffer management. */
    public static final int HOP_THRESHOLD = 5;
    public static final double AGE_THRESHOLD = 100D;

    /**
     * Private constructor to hide the implicit public one (this is a utility class!).
     */
    private DisasterRouterTestUtils() {
        throw new IllegalAccessError("Utility class");
    }

    public static void addDisasterRouterSettings(TestSettings s) {
        s.setNameSpace(DeliveryPredictabilityStorage.DELIVERY_PREDICTABILITY_STORAGE_NS);
        s.putSetting(DeliveryPredictabilityStorage.BETA_S, Double.toString(BETA));
        s.putSetting(DeliveryPredictabilityStorage.GAMMA_S, Double.toString(GAMMA));
        s.putSetting(DeliveryPredictabilityStorage.SUMMAND_S, Double.toString(SUMMAND));
        s.putSetting(DeliveryPredictabilityStorage.WINDOW_LENGTH_S, Double.toString(DP_WINDOW_LENGTH));
        s.restoreNameSpace();

        s.setNameSpace(EncounterValueManager.ENCOUNTER_VALUE_NS);
        s.putSetting(EncounterValueManager.AGING_FACTOR, Double.toString(NEW_DATA_WEIGHT));
        s.putSetting(EncounterValueManager.WINDOW_LENGTH_S, Double.toString(EV_WINDOW_LENGTH));
        s.restoreNameSpace();

        s.setNameSpace(ReplicationsDensityManager.REPLICATIONS_DENSITY_NS);
        s.putSetting(ReplicationsDensityManager.WINDOW_LENGTH_S, Double.toString(RD_WINDOW_LENGTH));
        s.restoreNameSpace();

        s.setNameSpace(DisasterPrioritizationStrategy.DISASTER_PRIORITIZATION_NS);
        s.putSetting(DisasterPrioritizationStrategy.HEAD_START_THRESHOLD_S, Double.toString(HEAD_START_THRESHOLD));
        s.putSetting(DisasterPrioritizationStrategy.PRIORITY_THRESHOLD_S, Double.toString(PRIORITY_THRESHOLD));
        s.putSetting(DisasterPrioritization.DELIVERY_PREDICTABILITY_WEIGHT, Double.toString(DP_WEIGHT));
        s.restoreNameSpace();

        DatabaseApplicationTest.addDatabaseApplicationSettings(s);
        s.setNameSpace(UtilityMessageChooser.UTILITY_MESSAGE_CHOOSER_NS);
        s.putSetting(
                UtilityMessageChooser.DELIVERY_PREDICTABILITY_WEIGHT, Double.toString(DELIVERY_PREDICTABILITY_WEIGHT));
        s.putSetting(UtilityMessageChooser.POWER_WEIGHT, Double.toString(POWER_WEIGHT));
        s.putSetting(UtilityMessageChooser.PROPHET_PLUS_WEIGHT, Double.toString(PROPHET_PLUS_WEIGHT));
        s.putSetting(UtilityMessageChooser.REPLICATIONS_DENSITY_WEIGHT, Double.toString(REPLICATIONS_DENSITY_WEIGHT));
        s.putSetting(UtilityMessageChooser.ENCOUNTER_VALUE_WEIGHT, Double.toString(ENCOUNTER_VALUE_WEIGHT));
        s.putSetting(UtilityMessageChooser.UTILITY_THRESHOLD, Double.toString(UTILITY_THRESHOLD));
        s.restoreNameSpace();

        // Energy constants.
        s.putSetting(EnergyModel.INIT_ENERGY_S, "1");
        s.putSetting(EnergyModel.SCAN_ENERGY_S, "0");
        s.putSetting(EnergyModel.TRANSMIT_ENERGY_S, "0");
        s.putSetting(EnergyModel.WARMUP_S, "0");
        s.putSetting(EnergyModel.SCAN_RSP_ENERGY_S, "0");

        s.setNameSpace(DisasterBufferComparator.DISASTER_BUFFER_NS);
        s.putSetting(DisasterBufferComparator.AGE_THRESHOLD_S, Double.toString(AGE_THRESHOLD));
        s.putSetting(DisasterBufferComparator.HOP_THRESHOLD_S, Integer.toString(HOP_THRESHOLD));
        s.restoreNameSpace();
    }
}
