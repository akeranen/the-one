package test;

import core.BroadcastMessage;
import core.Coord;
import core.DTNHost;
import core.DataMessage;
import core.DisasterData;
import core.Group;
import core.Message;
import core.MulticastMessage;
import core.SettingsError;
import core.SimClock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import routing.util.DeliveryPredictabilityStorage;

import java.util.ArrayList;

/**
 * Contains tests for the {@link routing.util.DeliveryPredictabilityStorage} class.
 *
 * Created by Britta Heymann on 19.05.2017.
 */
public class DeliveryPredictabilityStorageTest {
    /* Constants needed for delivery predictabilities that are used in most tests. */
    private static final double BETA = 0.25;
    private static final double GAMMA = 0.95;
    private static final double SUMMAND = 0.75;
    private static final double SECONDS_IN_TIME_UNIT = 1;

    /* Further constants for exception checks. */
    private static final double BELOW_ZERO = -0.1;
    private static final double GREATER_THAN_ONE = 1.1;

    /* Times needed for a scenario test. */
    private static final int FIRST_MEETING_TIME = 4;
    private static final int SECOND_MEETING_TIME = 6;

    /* Acceptable delta for double equality checks. */
    private static final double DOUBLE_COMPARISON_DELTA = 0.0001;

    private static final String EXPECTED_DIFFERENT_PREDICTABILITY = "Expected different delivery predictability.";
    private static final String EXPECTED_EMPTY_STORAGE = "No delivery predictabilities should have been set.";

    /* Objects needed for tests. */
    private TestUtils testUtils = new TestUtils(new ArrayList<>(), new ArrayList<>(), new TestSettings());
    private SimClock clock = SimClock.getInstance();
    private DTNHost attachedHost = this.testUtils.createHost();
    private DeliveryPredictabilityStorage dpStorage =
            createDeliveryPredictabilityStorage(BETA, GAMMA, SUMMAND, SECONDS_IN_TIME_UNIT, this.attachedHost);

    @After
    public void cleanUp() {
        this.clock.setTime(0);
        Group.clearGroups();
    }

    @Test(expected = SettingsError.class)
    public void testConstructorThrowsForNegativeBeta() {
        createDeliveryPredictabilityStorage(BELOW_ZERO, GAMMA, SUMMAND, SECONDS_IN_TIME_UNIT, this.attachedHost);
    }

    @Test(expected = SettingsError.class)
    public void testConstructorThrowsForBetaGreaterOne() {
        createDeliveryPredictabilityStorage(GREATER_THAN_ONE, GAMMA, SUMMAND, SECONDS_IN_TIME_UNIT, this.attachedHost);
    }

    @Test(expected = SettingsError.class)
    public void testConstructorThrowsForNegativeGamma() {
        createDeliveryPredictabilityStorage(BETA, BELOW_ZERO, SUMMAND, SECONDS_IN_TIME_UNIT, this.attachedHost);
    }

    @Test(expected = SettingsError.class)
    public void testConstructorThrowsForGammaGreaterOne() {
        createDeliveryPredictabilityStorage(BETA, GREATER_THAN_ONE, SUMMAND, SECONDS_IN_TIME_UNIT, this.attachedHost);
    }

    @Test(expected = SettingsError.class)
    public void testConstructorThrowsForNegativeSummand() {
        createDeliveryPredictabilityStorage(BETA, GAMMA, BELOW_ZERO, SECONDS_IN_TIME_UNIT, this.attachedHost);
    }

    @Test(expected = SettingsError.class)
    public void testConstructorThrowsForSummandGreaterOne() {
        createDeliveryPredictabilityStorage(BETA, GAMMA, GREATER_THAN_ONE, SECONDS_IN_TIME_UNIT, this.attachedHost);
    }

    @Test(expected = SettingsError.class)
    public void testConstructorThrowsForZeroSecondsInTimeUnit() {
        createDeliveryPredictabilityStorage(BETA, GAMMA, SUMMAND, 0, this.attachedHost);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsForMissingHost() {
        createDeliveryPredictabilityStorage(BETA, GAMMA, SUMMAND, SECONDS_IN_TIME_UNIT, null);
    }

    @Test
    public void testGetBeta() {
        Assert.assertEquals(
                "Expected different beta to be returned.",
                BETA, this.dpStorage.getBeta(), DOUBLE_COMPARISON_DELTA);
    }

    @Test
    public void testGetGamma() {
        Assert.assertEquals(
                "Expected different gamme to be returned.",
                GAMMA, this.dpStorage.getGamma(), DOUBLE_COMPARISON_DELTA);
    }

    @Test
    public void testGetSummand() {
        Assert.assertEquals(
                "Expected different summand to be returned.",
                SUMMAND, this.dpStorage.getSummand(), DOUBLE_COMPARISON_DELTA);
    }

    @Test
    public void testGetSecondsInTimeUnit() {
        Assert.assertEquals(
                "Expected different number of seconds to be returned.",
                SECONDS_IN_TIME_UNIT, this.dpStorage.getSecondsInTimeUnit(), DOUBLE_COMPARISON_DELTA);
    }

    @Test
    public void testGetAttachedHostAddress() {
        Assert.assertEquals(
                "Expected different address to be returned.",
                this.attachedHost.getAddress(), this.dpStorage.getAttachedHostAddress());
    }

    @Test
    public void testCopyConstructor() {
        DTNHost host = this.testUtils.createHost();
        DeliveryPredictabilityStorage copy = new DeliveryPredictabilityStorage(this.dpStorage, host);
        Assert.assertEquals(
                "Expected different beta.", this.dpStorage.getBeta(), copy.getBeta(), DOUBLE_COMPARISON_DELTA);
        Assert.assertEquals(
                "Expected different gamma.", this.dpStorage.getGamma(), copy.getGamma(), DOUBLE_COMPARISON_DELTA);
        Assert.assertEquals(
                "Expected different summand.", this.dpStorage.getSummand(), copy.getSummand(), DOUBLE_COMPARISON_DELTA);
        Assert.assertEquals(
                "Expected different number of seconds in time unit.",
                this.dpStorage.getSecondsInTimeUnit(), copy.getSecondsInTimeUnit(), DOUBLE_COMPARISON_DELTA);
        Assert.assertEquals("Expected different host address.", host.getAddress(), copy.getAttachedHostAddress());
    }

    /**
     * Tests that updating delivery predictabilites works as expected by playing through a small scenario.
     */
    @Test
    public void testUpdateMethodsWork() {
        // Create additional hosts.
        DTNHost b = this.testUtils.createHost();
        DTNHost c = this.testUtils.createHost();
        DeliveryPredictabilityStorage bStorage =
                createDeliveryPredictabilityStorage(BETA, GAMMA, SUMMAND, SECONDS_IN_TIME_UNIT, b);
        DeliveryPredictabilityStorage cStorage =
                createDeliveryPredictabilityStorage(BETA, GAMMA, SUMMAND, SECONDS_IN_TIME_UNIT, c);

        // Check all hosts have empty delivery predictability storages in the beginning.
        Assert.assertTrue(EXPECTED_EMPTY_STORAGE, this.dpStorage.getKnownAddresses().isEmpty());
        Assert.assertTrue(EXPECTED_EMPTY_STORAGE, bStorage.getKnownAddresses().isEmpty());
        Assert.assertTrue(EXPECTED_EMPTY_STORAGE, cStorage.getKnownAddresses().isEmpty());

        // Let own host and B meet each other at a certain time.
        this.clock.setTime(FIRST_MEETING_TIME);
        DeliveryPredictabilityStorage.updatePredictabilitiesForBothHosts(this.dpStorage, bStorage);

        // Check their delivery predictabilities to each other.
        double expectedPredictability = SUMMAND;
        Assert.assertEquals(
                EXPECTED_DIFFERENT_PREDICTABILITY,
                expectedPredictability, this.dpStorage.getDeliveryPredictability(b), DOUBLE_COMPARISON_DELTA);
        Assert.assertEquals(
                EXPECTED_DIFFERENT_PREDICTABILITY,
                expectedPredictability, bStorage.getDeliveryPredictability(this.attachedHost), DOUBLE_COMPARISON_DELTA);

        // Let B and C meet each other at a later time.
        this.clock.setTime(SECOND_MEETING_TIME);
        DeliveryPredictabilityStorage.updatePredictabilitiesForBothHosts(bStorage, cStorage);

        // Check their delivery predictabilites to each other.
        Assert.assertEquals(
                EXPECTED_DIFFERENT_PREDICTABILITY,
                expectedPredictability, cStorage.getDeliveryPredictability(b), DOUBLE_COMPARISON_DELTA);
        Assert.assertEquals(
                EXPECTED_DIFFERENT_PREDICTABILITY,
                expectedPredictability, bStorage.getDeliveryPredictability(c), DOUBLE_COMPARISON_DELTA);

        // Check their delivery predictabilites to ownHost.
        double decayedPredictability = SUMMAND * Math.pow(GAMMA, (double)SECOND_MEETING_TIME - FIRST_MEETING_TIME);
        Assert.assertEquals(
                EXPECTED_DIFFERENT_PREDICTABILITY,
                decayedPredictability, bStorage.getDeliveryPredictability(this.attachedHost), DOUBLE_COMPARISON_DELTA);
        double transitivePredictability = SUMMAND * decayedPredictability * BETA;
        Assert.assertEquals(
                EXPECTED_DIFFERENT_PREDICTABILITY,
                transitivePredictability,
                cStorage.getDeliveryPredictability(this.attachedHost),
                DOUBLE_COMPARISON_DELTA);
    }

    /**
     * Delivery predictability for unknown host should be 0.
     */
    @Test
    public void testGetDeliveryPredictabilityForUnknownHost() {
        Assert.assertEquals(
                EXPECTED_DIFFERENT_PREDICTABILITY,
                0, this.dpStorage.getDeliveryPredictability(this.testUtils.createHost()), DOUBLE_COMPARISON_DELTA);
    }

    /**
     * Tests that {@link DeliveryPredictabilityStorage#getDeliveryPredictability(Message)} returns the same for a
     * one-to-one message as {@link DeliveryPredictabilityStorage#getDeliveryPredictability(DTNHost)} does for its
     * recipient.
     */
    @Test
    public void testGetDeliveryPredictabilityForOneToOneMessage() {
        // Make sure recipient is known.
        DTNHost recipient = this.testUtils.createHost();
        DeliveryPredictabilityStorage recipientStorage = createDeliveryPredictabilityStorage(recipient);
        DeliveryPredictabilityStorage.updatePredictabilitiesForBothHosts(recipientStorage, this.dpStorage);

        // Check delivery predictability for message.
        Message oneToOneMessage = new Message(this.testUtils.createHost(), recipient, "M1", 0);
        Assert.assertEquals(
                "Delivery predictability of 1-to-1 message should equal the delivery predictability to the recipient",
                this.dpStorage.getDeliveryPredictability(recipient),
                this.dpStorage.getDeliveryPredictability(oneToOneMessage),
                DOUBLE_COMPARISON_DELTA);
    }

    /**
     * Tests that {@link DeliveryPredictabilityStorage#getDeliveryPredictability(Message)} returns the same for a
     * multicast as the maximum {@link DeliveryPredictabilityStorage#getDeliveryPredictability(DTNHost)} returns for any
     * of its recipients.
     */
    @Test
    public void testGetDeliveryPredictabilityForMulticastMessages() {
        // Create group for the multicast message.
        DTNHost recipient = this.testUtils.createHost();
        DTNHost oftenMetRecipient = this.testUtils.createHost();
        Group group = Group.createGroup(1);
        group.addHost(recipient);
        group.addHost(oftenMetRecipient);

        // Make sure we know both hosts in the group, but know one better than the other.
        DeliveryPredictabilityStorage recipientStorage = createDeliveryPredictabilityStorage(recipient);
        DeliveryPredictabilityStorage oftenMetRecipientStorage = createDeliveryPredictabilityStorage(oftenMetRecipient);
        DeliveryPredictabilityStorage.updatePredictabilitiesForBothHosts(recipientStorage, this.dpStorage);
        DeliveryPredictabilityStorage.updatePredictabilitiesForBothHosts(oftenMetRecipientStorage, this.dpStorage);
        DeliveryPredictabilityStorage.updatePredictabilitiesForBothHosts(oftenMetRecipientStorage, this.dpStorage);
        Assert.assertTrue(
                "One host should be known better than the other.",
                this.dpStorage.getDeliveryPredictability(oftenMetRecipient)
                        > this.dpStorage.getDeliveryPredictability(recipient));

        // Check delivery predictability of a multicast message.
        Message multicast = new MulticastMessage(recipient, group, "M1", 0);
        Assert.assertEquals(
                "Multicast delivery predictability should equal the maximum recipient delivery predictability.",
                this.dpStorage.getDeliveryPredictability(oftenMetRecipient),
                this.dpStorage.getDeliveryPredictability(multicast),
                DOUBLE_COMPARISON_DELTA);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetDeliveryPredictabilityThrowsForDataMessage() {
        DisasterData data = new DisasterData(DisasterData.DataType.MARKER, 0, SimClock.getTime(), new Coord(0, 0));
        Message dataMessage = new DataMessage(this.testUtils.createHost(), this.attachedHost, "M1", data, 1, 0);
        this.dpStorage.getDeliveryPredictability(dataMessage);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetDeliveryPredictabilityThrowsForBroadcastMessage() {
        Message broadcast = new BroadcastMessage(this.testUtils.createHost(), "M1", 0);
        this.dpStorage.getDeliveryPredictability(broadcast);
    }

    @Test
    public void testMetHostsAreAddedToKnownAddresses() {
        DTNHost neighbor = this.testUtils.createHost();
        DeliveryPredictabilityStorage neighborStorage = createDeliveryPredictabilityStorage(neighbor);
        DeliveryPredictabilityStorage.updatePredictabilitiesForBothHosts(neighborStorage, this.dpStorage);
        Assert.assertTrue(
                "Met hosts should be added to known addresses.",
                this.dpStorage.getKnownAddresses().contains(neighbor.getAddress()));
    }

    /**
     * Tests that hosts a connected host knows about are also added to the host we know.
     */
    @Test
    public void testKnownAddressesAreExtendedTransitivitely() {
        // Create neighbor knowing another host we know nothing about.
        DTNHost nonNeighbor = this.testUtils.createHost();
        DeliveryPredictabilityStorage nonNeighborStorage = createDeliveryPredictabilityStorage(nonNeighbor);
        DeliveryPredictabilityStorage neighborStorage =
                createDeliveryPredictabilityStorage(this.testUtils.createHost());
        DeliveryPredictabilityStorage.updatePredictabilitiesForBothHosts(nonNeighborStorage, neighborStorage);

        // Meet the neighbor.
        DeliveryPredictabilityStorage.updatePredictabilitiesForBothHosts(dpStorage, neighborStorage);

        // Check we know the other host, too.
        Assert.assertTrue(
                "Hosts known by connected hosts should be added to known addresses.",
                this.dpStorage.getKnownAddresses().contains(nonNeighbor.getAddress()));
    }

    /**
     * Tests that own address is never added to known addresses.
     */
    @Test
    public void testOwnAddressIsNotAddedToKnownAddresses() {
        // Create neighbor.
        DeliveryPredictabilityStorage neighborStorage =
                createDeliveryPredictabilityStorage(this.testUtils.createHost());

        // Meet the neighbor.
        DeliveryPredictabilityStorage.updatePredictabilitiesForBothHosts(this.dpStorage, neighborStorage);
        Assert.assertTrue(
                "Neighbor should know us.",
                neighborStorage.getKnownAddresses().contains(this.attachedHost.getAddress()));

        // Make sure our own address was not added to known hosts.
        Assert.assertFalse(
                "Own address should never be added to known hosts.",
                this.dpStorage.getKnownAddresses().contains(this.attachedHost.getAddress()));
    }

    private static DeliveryPredictabilityStorage createDeliveryPredictabilityStorage(DTNHost host) {
        return createDeliveryPredictabilityStorage(BETA, GAMMA, SUMMAND, SECONDS_IN_TIME_UNIT, host);
    }

    /**
     * Creates a {@link DeliveryPredictabilityStorage}.
     * @param beta Constant indicating the importance of transitivity updates.
     * @param gamma Constant that determines how fast delivery predictabilities decay.
     * @param summand Constant used in direct updates, also known as DP_init.
     * @param secondsInTimeUnit Constant describing how many seconds are in a time unit.
     * @param host The host to be attached to this storage.
     * @return The created {@link DeliveryPredictabilityStorage}.
     */
    private static DeliveryPredictabilityStorage createDeliveryPredictabilityStorage(
            double beta, double gamma, double summand, double secondsInTimeUnit, DTNHost host) {
        TestSettings settings = new TestSettings();
        settings.putSetting(DeliveryPredictabilityStorage.BETA_S, Double.toString(beta));
        settings.putSetting(DeliveryPredictabilityStorage.GAMMA_S, Double.toString(gamma));
        settings.putSetting(DeliveryPredictabilityStorage.SUMMAND_S, Double.toString(summand));
        settings.putSetting(DeliveryPredictabilityStorage.TIME_UNIT_S, Double.toString(secondsInTimeUnit));

        return new DeliveryPredictabilityStorage(settings, host);
    }
}
