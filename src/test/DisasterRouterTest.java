package test;

import core.Group;
import core.Message;
import org.junit.Assert;
import routing.DisasterRouter;
import routing.util.DeliveryPredictabilityStorage;
import routing.util.EncounterValueManager;
import routing.util.ReplicationsDensityManager;

/**
 * Contains tests for the {@link DisasterRouter} class.
 *
 * Created by Britta Heymann on 19.05.2017.
 */
public class DisasterRouterTest extends AbstractRouterTest {
    /* Constants needed for delivery predictabilities. */
    private static final double BETA = 0.25;
    private static final double GAMMA = 0.95;
    private static final double SUMMAND = 0.75;
    private static final double SECONDS_IN_TIME_UNIT = 2;

    /* Constants needed for encounter value. */
    private static final double NEW_DATA_WEIGHT = 0.3;
    private static final double EV_WINDOW_LENGTH = 21.3;

    /* Constant needed for replications density. */
    private static final double RD_WINDOW_LENGTH = 12.0;

    /* Some time (span)s needed for tests. */
    private static final double SHORT_TIME_SPAN = 0.1;
    private static final double FIRST_MEETING_TIME = 4;
    private static final double SECOND_MEETING_TIME = 8;

    /** Assumed replications densitiy if nothing is known about a message. */
    private static final double DEFAULT_REPLICATIONS_DENSITY = 0.5;

    private static final String EXPECTED_DIFFERENT_DELIVERY_PREDICTABILITY =
            "Expected different delivery predictability.";

    /* The delta allowed on double comparisons. */
    private static final double DOUBLE_COMPARISON_DELTA = 0.0001;

    @Override
    public void setUp() throws Exception {
        ts.setNameSpace(null);
        ts.putSetting(DeliveryPredictabilityStorage.BETA_S, Double.toString(BETA));
        ts.putSetting(DeliveryPredictabilityStorage.GAMMA_S, Double.toString(GAMMA));
        ts.putSetting(DeliveryPredictabilityStorage.SUMMAND_S, Double.toString(SUMMAND));
        ts.putSetting(DeliveryPredictabilityStorage.TIME_UNIT_S, Double.toString(SECONDS_IN_TIME_UNIT));

        ts.setNameSpace(EncounterValueManager.ENCOUNTER_VALUE_NS);
        ts.putSetting(EncounterValueManager.AGING_FACTOR, Double.toString(NEW_DATA_WEIGHT));
        ts.putSetting(EncounterValueManager.WINDOW_LENGTH_S, Double.toString(EV_WINDOW_LENGTH));
        ts.restoreNameSpace();

        ts.setNameSpace(ReplicationsDensityManager.REPLICATIONS_DENSITY_NS);
        ts.putSetting(ReplicationsDensityManager.WINDOW_LENGTH_S, Double.toString(RD_WINDOW_LENGTH));
        ts.restoreNameSpace();

        setRouterProto(new DisasterRouter(ts));
        super.setUp();
    }

    /**
     * Tears down the fixture, for example, close a network connection.
     * This method is called after a test is executed.
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        Group.clearGroups();
    }

    public void testEncounterValueIsComputedCorrectly() {
        this.clock.setTime(EV_WINDOW_LENGTH);
        h1.connect(h2);
        this.updateAllNodes();

        DisasterRouter router = (DisasterRouter)h2.getRouter();
        Assert.assertEquals(
                "Expected different encounter value.",
                1 * NEW_DATA_WEIGHT, router.getEncounterValue(), DOUBLE_COMPARISON_DELTA);
    }

    public void testEncounterValueIsUpdatedAtCorrectTime() {
        // Make sure an encounter exists.
        h1.connect(h2);
        DisasterRouter router = (DisasterRouter)h2.getRouter();

        // Check encounter value is not updated shortly before time window is up.
        this.clock.setTime(EV_WINDOW_LENGTH - SHORT_TIME_SPAN);
        this.updateAllNodes();
        Assert.assertEquals(
                "Encounter value should not have been updated yet.",
                0, router.getEncounterValue(), DOUBLE_COMPARISON_DELTA);

        // Check encounter value is updated shortly after time window is up.
        this.clock.setTime(EV_WINDOW_LENGTH + SHORT_TIME_SPAN);
        this.updateAllNodes();
        Assert.assertNotEquals(
                "Encounter value should have been updated.",
                0, router.getEncounterValue(), DOUBLE_COMPARISON_DELTA);
    }

    public void testEncounterValueManagersDifferBetweenHosts() {
        this.clock.setTime(EV_WINDOW_LENGTH);
        h1.connect(h2);
        this.updateAllNodes();

        DisasterRouter router = (DisasterRouter)h3.getRouter();
        Assert.assertEquals(
                "Expected different encounter value.",
                0, router.getEncounterValue(), DOUBLE_COMPARISON_DELTA);
    }

    /**
     * Tests that the replications density is computed correctly if two nodes with the same message meet each other.
     */
    public void testReplicationsDensitiesAreComputedCorrectly() {
        // Make sure it's the correct time.
        this.clock.setTime(RD_WINDOW_LENGTH);

        // Give a message to two hosts.
        Message message = new Message(h3, h4, "M1", 0);
        h1.createNewMessage(message);
        h2.createNewMessage(message);

        // They should meet each other.
        h1.connect(h2);
        disconnect(h1);
        this.updateAllNodes();

        // Make sure the replications density was computed accordingly.
        DisasterRouter router = (DisasterRouter)h1.getRouter();
        Assert.assertEquals(
                "Expected different replications density.",
                1, router.getReplicationsDensity(message), DOUBLE_COMPARISON_DELTA);
    }

    /**
     * Tests that the replications density for a message is added as soon as a host has this message in buffer.
     */
    public void testReplicationsDensityIsAddedWhenMessageIsAdded() {
        Message message = new Message(h3, h4, "M1", 0);
        h1.createNewMessage(message);

        DisasterRouter router = (DisasterRouter)h1.getRouter();
        Assert.assertEquals(
                "Expected different replications density.",
                DEFAULT_REPLICATIONS_DENSITY, router.getReplicationsDensity(message), DOUBLE_COMPARISON_DELTA);
    }

    /**
     * Tests that removing a message from a host's buffer means it will also be removed from the host's replications
     * densities.
     */
    public void testReplicationsDensityIsRemovedWhenMessageIsRemoved() {
        Message message = new Message(h3, h4, "M1", 0);
        h1.createNewMessage(message);
        h1.deleteMessage(message.getId(), false);

        DisasterRouter router = (DisasterRouter)h1.getRouter();
        try {
            router.getReplicationsDensity(message);
            fail();
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("Expected different exception.", "Asked for a non-stored message!", e.getMessage());
        }
    }

    public void testReplicationsDensitiesAreUpdatedAtCorrectTime() {
        // Make two hosts aware of a message and let them meet each other.
        Message message = new Message(h3, h4, "M1", 0);
        h1.createNewMessage(message);
        h2.createNewMessage(message);
        h1.connect(h2);
        disconnect(h1);

        // Check replications density is not updated shortly before time window is up.
        DisasterRouter router = (DisasterRouter)h2.getRouter();
        this.clock.setTime(RD_WINDOW_LENGTH - SHORT_TIME_SPAN);
        this.updateAllNodes();
        Assert.assertEquals(
                "Replications density should not have been updated yet.",
                DEFAULT_REPLICATIONS_DENSITY, router.getReplicationsDensity(message), DOUBLE_COMPARISON_DELTA);

        // Check replications density is updated shortly after time window is up.
        this.clock.setTime(RD_WINDOW_LENGTH + SHORT_TIME_SPAN);
        this.updateAllNodes();
        Assert.assertNotEquals(
                "Replications density should have been updated.",
                DEFAULT_REPLICATIONS_DENSITY, router.getReplicationsDensity(message), DOUBLE_COMPARISON_DELTA);
    }

    public void testReplicationsDensityManagersDifferBetweenHosts() {
        // Create a message h1 knows about.
        Message message = new Message(h3, h4, "M1", 0);
        h1.createNewMessage(message);

        // h2 should not be able to get a replications density for the message.
        DisasterRouter router = (DisasterRouter)h2.getRouter();
        try {
            router.getReplicationsDensity(message);
            fail();
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("Expected different exception.", "Asked for a non-stored message!", e.getMessage());
        }
    }

    /**
     * Tests that delivery predictabilities are correctly computed by the router by checking them after two meetings
     * between three nodes.
     */
    public void testDeliveryPredictabilitesAreComputedCorrectly() {
        // Play short scenario with two meetings.
        this.clock.setTime(FIRST_MEETING_TIME);
        h1.connect(h2);
        this.clock.setTime(SECOND_MEETING_TIME);
        h2.connect(h3);

        // Get all routers.
        DisasterRouter router1 = (DisasterRouter)h1.getRouter();
        DisasterRouter router2 = (DisasterRouter)h2.getRouter();
        DisasterRouter router3 = (DisasterRouter)h3.getRouter();

        // Check delivery predictabilities are correct:
        Message messageToH1 = new Message(h4, h1, "M1", 0);
        Message messageToH2 = new Message(h4, h2, "M2", 0);
        Message messageToH3 = new Message(h4, h3, "M3", 0);

        // Check delivery predictabilies for h1.
        Assert.assertEquals(
                EXPECTED_DIFFERENT_DELIVERY_PREDICTABILITY,
                SUMMAND, router1.getDeliveryPredictability(messageToH2), DOUBLE_COMPARISON_DELTA);
        Assert.assertEquals(
                EXPECTED_DIFFERENT_DELIVERY_PREDICTABILITY,
                0, router1.getDeliveryPredictability(messageToH3), DOUBLE_COMPARISON_DELTA);

        // Check delivery predictabilies for h2.
        double age = (SECOND_MEETING_TIME - FIRST_MEETING_TIME) / SECONDS_IN_TIME_UNIT;
        double agedPredictability = SUMMAND * Math.pow(GAMMA, age);
        Assert.assertEquals(
                EXPECTED_DIFFERENT_DELIVERY_PREDICTABILITY,
                agedPredictability, router2.getDeliveryPredictability(messageToH1), DOUBLE_COMPARISON_DELTA);
        Assert.assertEquals(
                EXPECTED_DIFFERENT_DELIVERY_PREDICTABILITY,
                SUMMAND, router2.getDeliveryPredictability(messageToH3), DOUBLE_COMPARISON_DELTA);

        // Check delivery predictabilies for h3.
        double transitivePredictability = SUMMAND * BETA * agedPredictability;
        Assert.assertEquals(
                EXPECTED_DIFFERENT_DELIVERY_PREDICTABILITY,
                transitivePredictability, router3.getDeliveryPredictability(messageToH1), DOUBLE_COMPARISON_DELTA);
        Assert.assertEquals(
                EXPECTED_DIFFERENT_DELIVERY_PREDICTABILITY,
                SUMMAND, router3.getDeliveryPredictability(messageToH2), DOUBLE_COMPARISON_DELTA);
    }

    public void testNoMessagesAreSentWhenAlreadyTransferring() {
        h1.connect(h2);

        // Try to send a non-empty message.
        Message m1 = new Message(h1, h2, "M1", 1);
        h1.createNewMessage(m1);
        this.updateAllNodes();

        // Should have started transfer.
        this.mc.next();
        this.checkTransferStart(h1, h2, m1.getId());

        // Create a new message and update routers again.
        Message m2 = new Message(h1, h2, "M2", 0);
        h1.createNewMessage(m2);
        this.updateAllNodes();

        // Check that the new message was not sent.
        while (this.mc.next()) {
            Assert.assertNotEquals("Did not expect another transfer.", mc.TYPE_START, this.mc.getLastType());
        }
    }

    // TODO: Test that direct messages (multicasts, broadcasts, one-to-ones, but NOT db messages) are sent first, both
    // to and from the neighbor
    // This can only be tested after trying to send other messages does not throw an exception.

    // TODO: Test that no messages are received when already transferring another message.
    // This can only be tested after the message chooser was implemented.

    // TODO: Test that direct messages are sent in correct order, both when sending and receiving.
    // This can only be tested after prioritization for direct messages was implemented.
    // Make sure your test is such that it is tested that replicated routers have the correct rating mechanisms linked
    // to their prioritizers.

    // TODO: Test that non-direct messages and DB messages are sorted correctly.
    // This can only be tested after prioritization was implemented.
    // Make sure your test is such that it is tested that replicated routers have the correct rating mechanisms linked
    // to their prioritizers.
}
