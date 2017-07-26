package test;

import applications.DatabaseApplication;
import core.SettingsError;
import org.junit.Assert;
import org.junit.Test;
import routing.DisasterRouter;
import routing.MessageChoosingStrategy;
import routing.MessageRouter;
import routing.choosers.RescueModeMessageChooser;

import java.util.Collection;
import java.util.List;

/**
 * Contains tests for the {@link RescueModeMessageChooser} class.
 *
 * Created by Britta Heymann on 26.07.2017.
 */
public class RescueModeMessageChooserTest extends AbstractMessageChoosingStrategyTest {
    /* Some values needed in tests. */
    private static final double NEGATIVE_VALUE = -0.1;
    private static final double VALUE_ABOVE_ONE = 1.1;

    public RescueModeMessageChooserTest() {
        // Empty constructor for "Classes and enums with private members should hava a constructor" (S1258).
        // This is dealt with by the setUp method.
    }

    /**
     * Adds all necessary settings to the settings object {@link #settings}.
     */
    @Override
    protected void addNecessarySettings() {
        DisasterRouterTestUtils.addDisasterRouterSettings(this.settings);
    }

    /**
     * Creates the router to use for all hosts.
     *
     * @return A prototype of the router.
     */
    @Override
    protected MessageRouter createMessageRouterPrototype() {
        MessageRouter routerProto = new DisasterRouter(this.settings);
        routerProto.addApplication(new DatabaseApplication(this.settings));
        return routerProto;
    }

    /**
     * Creates the message chooser to test.
     *
     * @return The chooser to test.
     */
    @Override
    protected MessageChoosingStrategy createMessageChooser() {
        return new RescueModeMessageChooser();
    }

    @Test(expected = SettingsError.class)
    public void testConstructorThrowsForNegativePowerThreshold() {
        this.settings.setNameSpace(RescueModeMessageChooser.RESCUE_MODE_MESSAGE_CHOOSER_NS);
        this.settings.putSetting(RescueModeMessageChooser.POWER_THRESHOLD, Double.toString(NEGATIVE_VALUE));
        this.settings.restoreNameSpace();

        new RescueModeMessageChooser();
    }

    @Test(expected = SettingsError.class)
    public void testConstructorThrowsForPowerThresholdAbove1() {
        this.settings.setNameSpace(RescueModeMessageChooser.RESCUE_MODE_MESSAGE_CHOOSER_NS);
        this.settings.putSetting(RescueModeMessageChooser.POWER_THRESHOLD, Double.toString(VALUE_ABOVE_ONE));
        this.settings.restoreNameSpace();

        new RescueModeMessageChooser();
    }

    @Test(expected = SettingsError.class)
    public void testConstructorThrowsForNegativeShortTimeSpan() {
        this.settings.setNameSpace(RescueModeMessageChooser.RESCUE_MODE_MESSAGE_CHOOSER_NS);
        this.settings.putSetting(RescueModeMessageChooser.SHORT_TIMESPAN_THRESHOLD, Double.toString(NEGATIVE_VALUE));
        this.settings.restoreNameSpace();

        new RescueModeMessageChooser();
    }

    @Test
    public void testGetPowerThreshold() {
        Assert.assertEquals("Expected different power threshold.",
                DisasterRouterTestUtils.POWER_THRESHOLD,
                this.getChooser().getPowerThreshold(), DOUBLE_COMPARISON_DELTA);
    }

    @Test
    public void testGetShortTimespanThreshold() {
        Assert.assertEquals("Expected different short timespan threshold.",
                DisasterRouterTestUtils.SHORT_TIMESPAN_THRESHOLD, this.getChooser().getShortTimespanThreshold(),
                DOUBLE_COMPARISON_DELTA);
    }


    /**
     * Checks that {@link MessageChoosingStrategy#replicate(MessageRouter)} returns a message choosing strategy of the
     * correct type.
     */
    @Override
    public void testReplicateReturnsCorrectType() {
        MessageChoosingStrategy copy = this.chooser.replicate(this.attachedHost.getRouter());
        Assert.assertTrue("Copy is of wrong class.", copy instanceof RescueModeMessageChooser);
    }

    /**
     * Checks that {@link MessageChoosingStrategy#replicate(MessageRouter)} copies all settings.
     */
    @Override
    public void testReplicateCopiesSettings() {
        MessageChoosingStrategy copy = this.chooser.replicate(this.attachedHost.getRouter());
        Assert.assertEquals("Expected different power threshold.",
                this.getChooser().getPowerThreshold(), ((RescueModeMessageChooser)copy).getPowerThreshold(),
                DOUBLE_COMPARISON_DELTA);
        Assert.assertEquals("Expected different short timespan threshold.",
                this.getChooser().getShortTimespanThreshold(),
                ((RescueModeMessageChooser)copy).getShortTimespanThreshold(),
                DOUBLE_COMPARISON_DELTA);
    }

    /***
     * Checks that {@link RescueModeMessageChooser#chooseNonDirectMessages(Collection, List)} does not return any
     * (message, connection) tuples for which the receiving host does not have sufficient power right now.
     */
    @Test
    public void testChooseNonDirectMessagesDoesNotReturnMessagesForLowPowerRouter() {

    }

    /**
     * Checks that each ordinary message is sent to every neighbor who is neither busy (transferring) nor has low
     * energy.
     */
    @Test
    public void testChooseNonDirectMessagesReturnsAllMessagesForAllAcceptingConnections() {

    }

    @Test
    public void testChooseNonDirectMessagesOnlyChoosesUsefulData() {

    }

    @Test
    public void testChooseNonDirectMessagesOnlyChoosesRecentData() {

    }

    @Test
    public void testChooseNonDirectMessagesReturnsDataMessagesForAllAcceptingConnections() {

    }

    /**
     * Returns {@link #chooser} as a {@link RescueModeMessageChooser}.
     * @return The chooser we are testing.
     */
    private RescueModeMessageChooser getChooser() {
        return (RescueModeMessageChooser)this.chooser;
    }
}
