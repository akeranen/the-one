package test;

import core.DTNHost;
import core.Group;
import core.SettingsError;
import core.SimError;
import core.SimScenario;
import input.AbstractMessageEventGenerator;
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


    private static final int MIN_GROUP_COUNT = 1;
    private static final int MAX_GROUP_COUNT = 10;
    private static final int MIN_GROUP_SIZE = 2;
    private static final int MAX_GROUP_SIZE = 10;
    private static final int MAX_HOST_RANGE = 10;
    private static final int INVALID_MAX_GROUP_SIZE = MAX_HOST_RANGE + 1;

    @Before
    public void init(){
        super.init();
        SimScenario.reset();
        Group.clearGroups();
        DTNHost.reset();
        TestSettings.addSettingsToEnableSimScenario(this.settings);
        //overwrite host range settings specified in super class {@link AbstractMEssageEventGeneratorTest}
        // to prevent errors after super class has changed
        this.settings.putSetting(AbstractMessageEventGenerator.HOST_RANGE_S, "0,"+MAX_HOST_RANGE);

        settings.putSetting(MulticastEventGenerator.GROUP_COUNT_RANGE_S,MIN_GROUP_COUNT+", "+MAX_GROUP_COUNT);
        settings.putSetting(MulticastEventGenerator.GROUP_SIZE_RANGE_S,MIN_GROUP_SIZE+", "+MAX_GROUP_SIZE);
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
    public void testGroupSizesAndGroupCountOfGeneratedGroupsAreInSpecifiedRange(){
        AbstractMessageEventGenerator generator = new MulticastEventGenerator(this.settings);
        for(int i = 0; i < AbstractMessageEventGeneratorTest.NR_TRIALS_IN_TEST; i++) {
            generator.nextEvent();
            assertTrue("Group count must be in specified range",
                    Group.getGroups().length <= MAX_GROUP_COUNT
                            && Group.getGroups().length >= MIN_GROUP_COUNT);
            for (Group g : Group.getGroups()) {
                assertTrue("Group size should be in specified range",
                        g.getMembers().length <= MAX_GROUP_SIZE &&
                                g.getMembers().length >= MIN_GROUP_SIZE);
            }
        }
    }

    @Test(expected = SimError.class)
    public void testGeneratorThrowsExceptionWhenMaxGroupSizeIsGreaterThanHostAddressRange(){
        settings.putSetting(MulticastEventGenerator.GROUP_SIZE_RANGE_S,MIN_GROUP_SIZE+", "+
                INVALID_MAX_GROUP_SIZE);
        AbstractMessageEventGenerator generator = new MulticastEventGenerator(this.settings);
    }

    /**
     * Gets the class name of the class to generate message events with.
     */
    @Override
    protected String getMessageEventGeneratorClassName() {
        return MulticastEventGenerator.class.toString();
    }


}
