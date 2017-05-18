package test;

import core.BroadcastMessage;
import core.DTNHost;
import core.Message;
import core.SettingsError;
import org.junit.Assert;
import org.junit.Test;
import routing.util.ReplicationsDensityManager;

import java.util.ArrayList;

/**
 * Contains tests for the {@link routing.util.ReplicationsDensityManager} class.
 *
 * Created by Britta Heymann on 18.05.2017.
 */
public class ReplicationsDensityManagerTest extends AbstractIntervalRatingMechanismTest{
    private static final String MESSAGE_ID = "M1";

    private static final double UNKNOWN_REPLICATIONS_DENSITY = 0.5;
    private static final double ONE_THIRD_DENSITY = 1D / 3;
    private static final double TWO_THIRD_DENSITY = 2D / 3;

    private static final String EXPECTED_DIFFERENT_VALUE = "Unexpected replications density value.";

    private TestUtils testUtils = new TestUtils(new ArrayList<>(), new ArrayList<>(), new TestSettings());
    private ReplicationsDensityManager replicationsDensityManager =
            ReplicationsDensityManagerTest.createReplicationsDensityManager(WINDOW_LENGTH);

    /**
     * Tests that {@link ReplicationsDensityManager}'s constructor throws a {@link SettingsError} if a window length of
     * 0 is provided.
     */
    @Override
    @Test(expected = SettingsError.class)
    public void testConstructorThrowsForWindowLengthZero() {
        ReplicationsDensityManagerTest.createReplicationsDensityManager(0);
    }

    /**
     * Tests that the copy constructor copies everything important over.
     */
    @Override
    @Test
    public void testCopyConstructor() {
        ReplicationsDensityManager copy = new ReplicationsDensityManager(this.replicationsDensityManager);
        Assert.assertEquals(
                "Window length was not copied.",
                this.replicationsDensityManager.getWindowLength(), copy.getWindowLength(), DOUBLE_COMPARISON_DELTA);
    }

    /**
     * Checks that {@link ReplicationsDensityManager#getWindowLength()} returns the correct value.
     */
    @Override
    @Test
    public void testGetWindowLength() {
        Assert.assertEquals(
                "Expected different window length.",
                WINDOW_LENGTH, this.replicationsDensityManager.getWindowLength(), DOUBLE_COMPARISON_DELTA);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetReplicationsDensityThrowsForUnknownMessage() {
        this.replicationsDensityManager.getReplicationsDensity(MESSAGE_ID);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveMessage() {
        this.replicationsDensityManager.addMessage(MESSAGE_ID);
        this.replicationsDensityManager.removeMessage(MESSAGE_ID);
        this.replicationsDensityManager.getReplicationsDensity(MESSAGE_ID);
    }

    /**
     * Tests that {@link ReplicationsDensityManager#getReplicationsDensity(String)} return 0.5 if the value has never
     * been updated.
     */
    @Override
    @Test
    public void testNeverUpdatedRatingMechanismIsCorrect() {
        this.replicationsDensityManager.addMessage(MESSAGE_ID);
        Assert.assertEquals(
                EXPECTED_DIFFERENT_VALUE,
                UNKNOWN_REPLICATIONS_DENSITY,
                this.replicationsDensityManager.getReplicationsDensity(MESSAGE_ID),
                DOUBLE_COMPARISON_DELTA);
    }

    /**
     * Tests that calling {@link ReplicationsDensityManager#addMessage(String)} with a message that is already known
     * will not reset that message's replication density to 0.5.
     */
    @Test
    public void testAddMessageOnKnownMessage() {
        // Add a message.
        this.replicationsDensityManager.addMessage(MESSAGE_ID);

        // Meet neighbor who has that message.
        this.meetNeighborWithMessage(MESSAGE_ID);

        // Update replications densities.
        this.clock.setTime(WINDOW_LENGTH);
        this.replicationsDensityManager.update();
        double replicationsDensity = this.replicationsDensityManager.getReplicationsDensity(MESSAGE_ID);

        // Add the message again.
        this.replicationsDensityManager.addMessage(MESSAGE_ID);

        // Make sure replications density wasn't changed.
        Assert.assertEquals(
                "Replications density shouldn't have been changed.",
                replicationsDensity,
                this.replicationsDensityManager.getReplicationsDensity(MESSAGE_ID),
                DOUBLE_COMPARISON_DELTA);
    }

    /**
     * Tests that a message is not added just because we have met a neighbor with that message.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testMessageIsNotAddedThroughNeighbors() {
        this.meetNeighborWithMessage(MESSAGE_ID);
        this.clock.setTime(WINDOW_LENGTH);
        this.replicationsDensityManager.update();

        this.replicationsDensityManager.getReplicationsDensity(MESSAGE_ID);
    }

    /**
     * Checks that the replications densities are only changed on update after a time window is completed.
     */
    @Override
    @Test
    public void testUpdateHappensAfterTimeWindowCompletes() {
        // Make sure replication densities get updated in the future.
        this.replicationsDensityManager.addMessage(MESSAGE_ID);
        this.meetNeighborWithMessage(MESSAGE_ID);

        // Update shortly before time window ends.
        this.clock.setTime(WINDOW_LENGTH - SHORT_TIME_SPAN);
        this.replicationsDensityManager.update();
        // The update shouldn't have done anything.
        double replicationsDensityShortlyBeforeTimeWindow =
                this.replicationsDensityManager.getReplicationsDensity(MESSAGE_ID);
        Assert.assertEquals(
                "Expected replications density to not have been updated.",
                UNKNOWN_REPLICATIONS_DENSITY, replicationsDensityShortlyBeforeTimeWindow, DOUBLE_COMPARISON_DELTA);

        // Update again at end of time window.
        this.clock.setTime(WINDOW_LENGTH);
        this.replicationsDensityManager.update();
        // Now, the replications density should have been changed.
        double replicationsDensityAtTimeWindowEnd = this.replicationsDensityManager.getReplicationsDensity(MESSAGE_ID);
        Assert.assertNotEquals(
                "Replications density should have been updated.",
                UNKNOWN_REPLICATIONS_DENSITY, replicationsDensityAtTimeWindowEnd, DOUBLE_COMPARISON_DELTA);
    }

    @Override
    @Test
    public void testRatingMechanismIsUpdatedCorrectly() {
        // Add a message.
        this.replicationsDensityManager.addMessage(MESSAGE_ID);

        // Meet neighbor who has that message.
        this.meetNeighborWithMessage(MESSAGE_ID);

        // Meet two others who don't.
        this.replicationsDensityManager.addEncounter(this.testUtils.createHost());
        this.replicationsDensityManager.addEncounter(this.testUtils.createHost());

        // Update replications densities.
        this.clock.setTime(WINDOW_LENGTH);
        this.replicationsDensityManager.update();

        // Check the update executed correctly.
        Assert.assertEquals(
                EXPECTED_DIFFERENT_VALUE,
                ONE_THIRD_DENSITY,
                this.replicationsDensityManager.getReplicationsDensity(MESSAGE_ID),
                DOUBLE_COMPARISON_DELTA);
    }

    /**
     * Makes sure not only the update after the first time window works, but also later ones.
     */
    @Override
    @Test
    public void testConsecutiveUpdatesWork() {
        // Add a message.
        this.replicationsDensityManager.addMessage(MESSAGE_ID);

        // Meet neighbor who has that message.
        this.meetNeighborWithMessage(MESSAGE_ID);

        // Update replications densities.
        this.clock.setTime(WINDOW_LENGTH);
        this.replicationsDensityManager.update();

        // Meet three other hosts: Two have the message, one hasn't.
        this.meetNeighborWithMessage(MESSAGE_ID);
        this.meetNeighborWithMessage(MESSAGE_ID);
        this.replicationsDensityManager.addEncounter(this.testUtils.createHost());

        // Update replications densities.
        this.clock.advance(WINDOW_LENGTH);
        this.replicationsDensityManager.update();

        // Check the update executed correctly, ignoring the meeting in the first time window.
        Assert.assertEquals(
                EXPECTED_DIFFERENT_VALUE,
                TWO_THIRD_DENSITY,
                this.replicationsDensityManager.getReplicationsDensity(MESSAGE_ID),
                DOUBLE_COMPARISON_DELTA);
    }

    @Test
    public void testReplicationsDensitiesDoNotChangeWithoutEncounters() {
        // Meet someone with a message we know.
        this.replicationsDensityManager.addMessage(MESSAGE_ID);
        this.meetNeighborWithMessage(MESSAGE_ID);

        // Update replications densities.
        this.clock.setTime(WINDOW_LENGTH);
        this.replicationsDensityManager.update();
        double replicationsDensity = this.replicationsDensityManager.getReplicationsDensity(MESSAGE_ID);

        // Update them again after a time window without encounters.
        this.clock.advance(WINDOW_LENGTH);
        this.replicationsDensityManager.update();

        // Make sure they did not change.
        Assert.assertEquals(
                "Replications densities should not have been changed.",
                replicationsDensity,
                this.replicationsDensityManager.getReplicationsDensity(MESSAGE_ID),
                DOUBLE_COMPARISON_DELTA);
    }

    @Test
    public void testReplicationsDensitiesWorkForMultipleMessages() {
        // Prepare a manager knowing two messages.
        String secondMessageId = "M2";
        this.replicationsDensityManager.addMessage(MESSAGE_ID);
        this.replicationsDensityManager.addMessage(secondMessageId);

        // Meet neighbor without any messages.
        this.replicationsDensityManager.addEncounter(this.testUtils.createHost());
        // Meet neighbor with one of the messages.
        this.meetNeighborWithMessage(MESSAGE_ID);
        // Meet neighbor with two of the messages.
        DTNHost neighbor = this.testUtils.createHost();
        neighbor.createNewMessage(this.createMessage(MESSAGE_ID));
        neighbor.createNewMessage(this.createMessage(secondMessageId));
        this.replicationsDensityManager.addEncounter(neighbor);

        // Update replications densities.
        this.clock.setTime(WINDOW_LENGTH);
        this.replicationsDensityManager.update();

        // Check them.
        Assert.assertEquals(
                EXPECTED_DIFFERENT_VALUE,
                TWO_THIRD_DENSITY,
                this.replicationsDensityManager.getReplicationsDensity(MESSAGE_ID),
                DOUBLE_COMPARISON_DELTA);
        Assert.assertEquals(
                EXPECTED_DIFFERENT_VALUE,
                ONE_THIRD_DENSITY,
                this.replicationsDensityManager.getReplicationsDensity(secondMessageId),
                DOUBLE_COMPARISON_DELTA);
    }

    @Test
    public void testMeetingSameHostWithSameMessagesTwiceDoesNotChangeReplicationsDensities() {
        this.replicationsDensityManager.addMessage(MESSAGE_ID);

        // Create neighbor to meet twice.
        DTNHost neighbor = this.testUtils.createHost();
        neighbor.createNewMessage(this.createMessage(MESSAGE_ID));

        // Meet neighbor twice and two other host without any messages once.
        this.replicationsDensityManager.addEncounter(neighbor);
        this.replicationsDensityManager.addEncounter(neighbor);
        this.replicationsDensityManager.addEncounter(this.testUtils.createHost());
        this.replicationsDensityManager.addEncounter(this.testUtils.createHost());

        // Update replications densities.
        this.clock.setTime(WINDOW_LENGTH);
        this.replicationsDensityManager.update();

        // Check only one meeting of the neighbor was used for computation.
        Assert.assertEquals(
                EXPECTED_DIFFERENT_VALUE,
                ONE_THIRD_DENSITY,
                this.replicationsDensityManager.getReplicationsDensity(MESSAGE_ID),
                DOUBLE_COMPARISON_DELTA);
    }

    @Test
    public void testMeetingSameHostWithDifferentMessagesTwiceChangesReplicationsDensities() {
        this.replicationsDensityManager.addMessage(MESSAGE_ID);

        // Create neighbor to meet twice.
        DTNHost neighbor = this.testUtils.createHost();

        // Meet him once without the message.
        this.replicationsDensityManager.addEncounter(neighbor);

        // Then meet him again with the message.
        neighbor.createNewMessage(this.createMessage(MESSAGE_ID));
        this.replicationsDensityManager.addEncounter(neighbor);

        // Update replications density.
        this.clock.setTime(WINDOW_LENGTH);
        this.replicationsDensityManager.update();

        // Make sure it was influenced by the second meeting.
        Assert.assertEquals(
                EXPECTED_DIFFERENT_VALUE,
                1, this.replicationsDensityManager.getReplicationsDensity(MESSAGE_ID), DOUBLE_COMPARISON_DELTA);
    }

    @Test
    public void testMeetingSameHostWithDifferentMessageInstancesTwiceDoesNotChangeReplicationsDensities() {
        this.replicationsDensityManager.addMessage(MESSAGE_ID);

        // Create neighbor to meet twice.
        DTNHost neighbor = this.testUtils.createHost();
        neighbor.createNewMessage(this.createMessage(MESSAGE_ID));

        // Meet him once with one instance of the message.
        this.replicationsDensityManager.addEncounter(neighbor);

        // Then meet him again with another instance of the message.
        neighbor.deleteMessage(MESSAGE_ID, true);
        neighbor.createNewMessage(this.createMessage(MESSAGE_ID));
        this.replicationsDensityManager.addEncounter(neighbor);

        // Update replications density.
        this.clock.setTime(WINDOW_LENGTH);
        this.replicationsDensityManager.update();

        // Make sure it was not influenced by the second meeting.
        Assert.assertEquals(
                EXPECTED_DIFFERENT_VALUE,
                1, this.replicationsDensityManager.getReplicationsDensity(MESSAGE_ID), DOUBLE_COMPARISON_DELTA);
    }
    /**
     * Creates a new {@link ReplicationsDensityManager} using the specified window length.
     */
    private static ReplicationsDensityManager createReplicationsDensityManager(double windowLength) {
        TestSettings settings = new TestSettings();
        settings.putSetting(ReplicationsDensityManager.WINDOW_LENGTH_S, Double.toString(windowLength));
        return new ReplicationsDensityManager(settings);
    }

    /**
     * Meets a neighbor never met before that has buffered the provided message ID, and calls
     * {@link ReplicationsDensityManager#addEncounter(DTNHost)}.
     */
    private void meetNeighborWithMessage(String messageId) {
        DTNHost neighbor = this.testUtils.createHost();
        neighbor.createNewMessage(this.createMessage(messageId));
        this.replicationsDensityManager.addEncounter(neighbor);
    }

    /**
     * Creates a broadcast message with the specified message ID.
     */
    private Message createMessage(String messageId) {
        DTNHost sender = this.testUtils.createHost();
        return new BroadcastMessage(sender, messageId, 0);
    }
}
