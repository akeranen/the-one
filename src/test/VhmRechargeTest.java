package test;

import core.DTNHost;
import core.SimClock;
import core.SimScenario;
import core.World;
import input.VhmEvent;
import movement.VoluntaryHelperMovement;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import routing.util.EnergyModel;

import java.util.HashSet;
import java.util.Set;

/**
 * Tests for recharging behavior of the {@link VoluntaryHelperMovement}.
 * Basic method tests are done in {@link VhmBasicTest}, other behavior tests in {@link VhmBehaviorTest}.
 *
 * Created by Britta Heymann on 21.06.2017.
 */
public class VhmRechargeTest {
    /** Number of repetitions executed for randomized tests. */
    private static final int NUM_TRIES_IN_RANDOMIZED_TEST = 100;
    private static final int HALF_OF_TRIES = NUM_TRIES_IN_RANDOMIZED_TEST / 2;

    private static final double POWER_DELTA = 0.01;

    private TestSettings testSettings = new TestSettings();
    private World world;
    private DTNHost host;

    public VhmRechargeTest() {
        // Empty constructor for "Classes and enums with private members should hava a constructor" (S1258).
        // This is dealt with by the setUp method.
    }

    @Before
    public void setUp() {
        // Enable using a SimScenario.
        SimScenario.reset();
        TestSettings.addSettingsToEnableSimScenario(this.testSettings);

        // Add correct movement model.
        this.testSettings.setNameSpace(SimScenario.GROUP_NS);
        this.testSettings.putSetting(SimScenario.MOVEMENT_MODEL_S, VoluntaryHelperMovement.class.getSimpleName());
        VhmTestHelper.addMinimalSettingsForVoluntaryHelperMovement(this.testSettings);
        this.testSettings.restoreNameSpace();

        // Initialize fields.
        this.world = SimScenario.getInstance().getWorld();
        this.host = this.world.getHosts().get(0);
    }

    @After
    public void cleanUp() {
        SimClock.reset();
        SimScenario.reset();
    }

    /**
     * Checks that a panicky host keeps being panicking even if his battery is empty.
     */
    @Test
    public void testHostWithZeroEnergyContinuesMovement() {
        // Make host panic.
        VoluntaryHelperMovement vhm = (VoluntaryHelperMovement)this.host.getMovement();
        VhmTestHelper.setToPanicMode(vhm);

        // Set energy to zero.
        this.host.getComBus().updateProperty(EnergyModel.ENERGY_VALUE_ID, 0D);
        this.world.update();

        // Check host is still in panic.
        VhmTestHelper.testPanicState(vhm);
    }

    /**
     * Tests that hosts' batteries are reset after a time span contained in the range specified in the settings passed
     * and that these time spans are selected (pseudo)randomly.
     */
    @Test
    public void testBatteryIsResetAfterAppropriateTime() {
        Set<Double> usedTimeSpans = new HashSet<>(NUM_TRIES_IN_RANDOMIZED_TEST);

        // Try several times to accord for randomness.
        for (int i = 0; i < NUM_TRIES_IN_RANDOMIZED_TEST; i++) {
            // Set energy to zero.
            this.host.getComBus().updateProperty(EnergyModel.ENERGY_VALUE_ID, 0D);
            this.world.update();
            double currTime = SimClock.getTime();

            // Then wait until it is increased again.
            this.waitUntilRecharge(this.host);
            double timeSpan = SimClock.getTime() - currTime;

            // Check time span.
            Assert.assertTrue("Host was updated too early.", timeSpan >= VhmTestHelper.TIME_BEFORE_RECHARGE[0]);
            Assert.assertTrue("Host was updated too late.", timeSpan < VhmTestHelper.TIME_BEFORE_RECHARGE[1]);
            usedTimeSpans.add(timeSpan);
        }

        // Finally check time spans are different for different tries.
        Assert.assertTrue("Different time spans should have been used.", usedTimeSpans.size() >= HALF_OF_TRIES);

    }

    /**
     * Checks that the power recharge is not only a short peak, but for real.
     */
    @Test
    public void testResetBatteryIsPersistent() {
        // Set battery to zero.
        this.host.getComBus().updateProperty(EnergyModel.ENERGY_VALUE_ID, 0D);

        // Wait until it is increased again and check new value.
        this.waitUntilRecharge(this.host);
        double powerAfterRecharge = VhmRechargeTest.getPowerOrDefault(this.host, 1);

        // Check that it stays at that value even after another update.
        this.world.update();
        Assert.assertEquals("Battery reset should be persistent.",
                powerAfterRecharge, VhmRechargeTest.getPowerOrDefault(this.host, 0), POWER_DELTA);
    }

    /**
     * Checks that the current movement is continued even when the battery is reset.
     */
    @Test
    public void testHostContinuesWithMovementAfterEnergyReset() {
        // Set battery to zero.
        this.host.getComBus().updateProperty(EnergyModel.ENERGY_VALUE_ID, 0D);
        this.world.update();

        // Make host panic.
        VoluntaryHelperMovement vhm = (VoluntaryHelperMovement)this.host.getMovement();
        VhmEvent disaster = new VhmEvent("testDisaster", VhmEventTest.createJsonForCompletelySpecifiedEvent());
        vhm.getHost().setLocation(disaster.getLocation());
        vhm.getProperties().setInjuryProbability(0);
        vhm.vhmEventStarted(disaster);

        // Wait until energy is increased again and check new value.
        this.waitUntilRecharge(this.host);

        // Check host is still in panic.
        VhmTestHelper.testPanicState(vhm);
    }

    /**
     * Updates the world until the provided host has positive battery power.
     * @param host The host to check.
     */
    private void waitUntilRecharge(DTNHost host) {
        while (VhmRechargeTest.getPowerOrDefault(host, 0) <= 0) {
            this.world.update();
        }
    }

    /**
     * Gets the provided host's battery power.
     * @param host The host to check.
     * @param defaultValue Value to return if the host has no battery power defined.
     * @return The battery power or the default value, if there is no such information.
     */
    private static double getPowerOrDefault(DTNHost host, double defaultValue) {
        return host.getComBus().getDouble(EnergyModel.ENERGY_VALUE_ID, defaultValue);
    }
}
