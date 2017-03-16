package test;

import core.DTNHost;
import core.Group;
import core.SettingsError;
import core.SimScenario;
import core.World;
import input.AbstractMessageEventGenerator;
import input.ExternalEvent;
import input.MulticastCreateEvent;
import input.MulticastEventGenerator;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Contains tests for the MulticastEventGenerator class
 *
 * Created by Marius Meyer on 10.03.17.
 */
public class MulticastEventGeneratorTest extends AbstractMessageEventGeneratorTest{

    private static final int TEST_NODE_COUNT = 10;

    @Before
    public void init(){
        super.init();
        SimScenario.reset();
        Group.clearGroups();
        DTNHost.reset();
        addSettingsToEnableSimScenario(this.settings);
    }

    @Test
    public void testNextEventCreatesMulticastMessages() {
        AbstractMessageEventGenerator generator = new MulticastEventGenerator(this.settings);
        for(int i = 0; i < AbstractMessageEventGeneratorTest.NR_TRIALS_IN_TEST; i++) {
            assertTrue(
                    "Event should have been the creation of a multicast message.",
                    generator.nextEvent() instanceof MulticastCreateEvent);
        }
    }

    @Test(expected = SettingsError.class)
    public void testMulticastEventGeneratorConstructorThrowsErrorIfSingleHostIsSpecified() {
        this.settings.putSetting(AbstractMessageEventGenerator.HOST_RANGE_S, "0,1");
        new MulticastEventGenerator(this.settings);
    }

    @Test
    public void testSenderNodeMustBeInSameGroupAsDestinationGroup(){
        AbstractMessageEventGenerator generator = new MulticastEventGenerator(this.settings);
        World world = SimScenario.getInstance().getWorld();
        for(int i = 0; i < AbstractMessageEventGeneratorTest.NR_TRIALS_IN_TEST; i++) {
            ExternalEvent event = generator.nextEvent();
            event.processEvent(world);
        }
    }

    /**
     * Gets the class name of the class to generate message events with.
     */
    @Override
    protected String getMessageEventGeneratorClassName() {
        return MulticastEventGenerator.class.toString();
    }

    private static void addSettingsToEnableSimScenario(TestSettings settings) {
        settings.putSetting("Group.groupID", "group");
        settings.putSetting("Group.nrofHosts", Integer.toString(TEST_NODE_COUNT));
        settings.putSetting("Group.nrofInterfaces", "0");
        settings.putSetting("Group.movementModel", "StationaryMovement");
        settings.putSetting("Group.nodeLocation", "0, 0");
        settings.putSetting("Group.router", "EpidemicRouter");
    }

}
