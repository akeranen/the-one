package test;

import core.ModuleCommunicationBus;
import core.NetworkInterface;
import core.Settings;
import core.SettingsError;
import org.junit.Assert;
import org.junit.Test;
import routing.util.EnergyModel;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Contains tests for the {@link routing.util.EnergyModel} class.
 *
 * Created by Britta Heymann on 21.06.2017.
 */
public class EnergyModelTest {
    /* Energy values used in tests. */
    private static final double MIN_ENERGY = 0.2;
    public static final double MAX_ENERGY = 0.7;
    private static final double SOME_ENERGY_LEVEL = 0.1;

    /** Acceptable delta in double comparisons. */
    private static final double DOUBLE_COMPARISON_DELTA = 0.00001;

    /** Number of repetitions executed for randomized tests. */
    private static final int NUM_TRIES_IN_RANDOMIZED_TEST = 100;

    private TestSettings settings = new TestSettings();
    private ModuleCommunicationBus comBus = new ModuleCommunicationBus();

    @Test
    public void testReadInitEnergyForSingleValue() {
        this.settings.putSetting(EnergyModel.INIT_ENERGY_S, Double.toString(MIN_ENERGY));
        double[] initEnergy = EnergyModel.readInitEnergy(this.settings);
        Assert.assertEquals("Expected different number of energy values.", 1, initEnergy.length);
        Assert.assertEquals(
                "Expected different initial energy value.", MIN_ENERGY, initEnergy[0], DOUBLE_COMPARISON_DELTA);
    }

    @Test
    public void testReadInitEnergyForRange() {
        this.settings.putSetting(EnergyModel.INIT_ENERGY_S, MIN_ENERGY + "," + MAX_ENERGY);
        double[] initEnergy = EnergyModel.readInitEnergy(this.settings);
        Assert.assertEquals(
                "Expected different number of energy values.",
                Settings.EXPECTED_VALUE_NUMBER_FOR_RANGE, initEnergy.length);
        Assert.assertArrayEquals(
                "Expected different initial energy values.",
                new double[] { MIN_ENERGY, MAX_ENERGY }, initEnergy, DOUBLE_COMPARISON_DELTA);
    }

    /**
     * Tests that specifying no values for the {@link EnergyModel#INIT_ENERGY_S} key results in a {@link SettingsError}
     * when calling {@link EnergyModel#readInitEnergy(Settings)}.
     */
    @Test(expected = SettingsError.class)
    public void testReadInitEnergyForInvalidArgumentNumber() {
        this.settings.putSetting(EnergyModel.INIT_ENERGY_S, "");
        EnergyModel.readInitEnergy(this.settings);
    }

    /**
     * Tests that a call to {@link EnergyModel#chooseRandomEnergyLevel(double[], Random)} with a single element array
     * and no randomizer results in that element being returned.
     */
    @Test
    public void testChooseRandomEnergyLevelForSingleValue() {
        double energy = EnergyModel.chooseRandomEnergyLevel(new double[] {MIN_ENERGY}, null);
        Assert.assertEquals("Expected different energy level to be chosen.",
                MIN_ENERGY, energy, DOUBLE_COMPARISON_DELTA);
    }

    /**
     * Tests that a call to {@link EnergyModel#chooseRandomEnergyLevel(double[], Random)} with a range of energy values
     * results in (pseudo)random elements out of that range being returned.
     */
    @Test
    public void testChooseRandomEnergyLevelForRange() {
        double[] range = new double[] { MIN_ENERGY, MAX_ENERGY };
        Random randomizer = new Random((int)(range[0] + range[1]));

        Set<Double> chosenValues = new HashSet<>(NUM_TRIES_IN_RANDOMIZED_TEST);
        for (int i = 0; i < NUM_TRIES_IN_RANDOMIZED_TEST; i++) {
            double energy = EnergyModel.chooseRandomEnergyLevel(range, randomizer);
            Assert.assertTrue("Energy should be at least the minimum.", energy >= MIN_ENERGY);
            Assert.assertTrue("Energy should be below maximum.", energy < MAX_ENERGY);
            chosenValues.add(energy);
        }

        Assert.assertEquals("Different energy levels should have been returned.",
                NUM_TRIES_IN_RANDOMIZED_TEST, chosenValues.size());
    }

    /**
     * Tests that calling {@link EnergyModel#chooseRandomEnergyLevel(double[], Random)} with no energy values specified
     * throws an {@link IllegalArgumentException}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testChooseRandomEnergyLevelForInvalidArgumentNumber() {
        EnergyModel.chooseRandomEnergyLevel(new double[0], null);
    }

    /**
     * Tests that {@link EnergyModel#getEnergyRatio()} works if the {@link EnergyModel#INIT_ENERGY_S} setting consists
     * of a single value.
     */
    @Test
    public void testGetEnergyRatioForSingleInitialEnergyValue() {
        // Create energy model with single init energy value.
        EnergyModelTest.addNecessaryInterfaceSettings(this.settings);
        EnergyModelTest.addAllEnergySettings(this.settings);
        this.settings.putSetting(EnergyModel.INIT_ENERGY_S, Double.toString(MIN_ENERGY));
        EnergyModel energyModel = new EnergyModel(this.settings);
        energyModel.update(new TestInterface(this.settings), this.comBus);

        // Update energy and check energy ratio.
        this.comBus.updateProperty(EnergyModel.ENERGY_VALUE_ID, SOME_ENERGY_LEVEL);
        Assert.assertEquals("Expected different energy ratio.",
                SOME_ENERGY_LEVEL / MIN_ENERGY, energyModel.getEnergyRatio(), DOUBLE_COMPARISON_DELTA);
    }

    /**
     * Tests that {@link EnergyModel#getEnergyRatio()} works if the {@link EnergyModel#INIT_ENERGY_S} setting consists
     * of a range.
     */
    @Test
    public void testGetEnergyRatioForInitialEnergyRange() {
        // Create energy model with init energy range.
        EnergyModelTest.addNecessaryInterfaceSettings(this.settings);
        EnergyModelTest.addAllEnergySettings(this.settings);
        this.settings.putSetting(EnergyModel.INIT_ENERGY_S, MIN_ENERGY + "," + MAX_ENERGY);
        EnergyModel energyModel = new EnergyModel(this.settings);
        energyModel.update(new TestInterface(this.settings), this.comBus);

        // Update energy and check energy ratio.
        this.comBus.updateProperty(EnergyModel.ENERGY_VALUE_ID, SOME_ENERGY_LEVEL);
        Assert.assertEquals("Expected different energy ratio.",
                SOME_ENERGY_LEVEL / MAX_ENERGY, energyModel.getEnergyRatio(), DOUBLE_COMPARISON_DELTA);
    }

    /**
     * Adds values to all settings concerning energy.
     * @param settings The settings object to add settings to.
     */
    static void addAllEnergySettings(TestSettings settings) {
        settings.putSetting(EnergyModel.INIT_ENERGY_S, MIN_ENERGY + "," + MAX_ENERGY);
        settings.putSetting(EnergyModel.SCAN_ENERGY_S, "0.05");
        settings.putSetting(EnergyModel.SCAN_RSP_ENERGY_S, "0.025");
        settings.putSetting(EnergyModel.TRANSMIT_ENERGY_S, "0.1");
        settings.putSetting(EnergyModel.WARMUP_S, "0");
    }

    /**
     * Adds values to all necessary settings concerning network interfaces.
     * @param settings The settings object to add settings to.
     */
    static void addNecessaryInterfaceSettings(TestSettings settings) {
        settings.putSetting(NetworkInterface.TRANSMIT_RANGE_S, "0");
        settings.putSetting(NetworkInterface.TRANSMIT_SPEED_S, "1");
    }
}
