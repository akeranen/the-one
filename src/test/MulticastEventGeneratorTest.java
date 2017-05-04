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
import static org.junit.Assert.assertEquals;

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
    @Override
    public void init(){
        super.init();
        SimScenario.reset();
        Group.clearGroups();
        DTNHost.reset();
        TestSettings.addSettingsToEnableSimScenario(this.settings);

        /*redefine host range settings because we need to check an invalid maximal group size.
        For this, we need to know the host range specified in the settings.
        */
        this.settings.putSetting(AbstractMessageEventGenerator.HOST_RANGE_S, "0,"+MAX_HOST_RANGE);

        /* Make sure we have enough hosts. */
        settings.setNameSpace(SimScenario.GROUP_NS);
        settings.putSetting(SimScenario.NROF_HOSTS_S, Integer.toString(MAX_HOST_RANGE));
        settings.restoreNameSpace();

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
        new MulticastEventGenerator(this.settings);
    }
    
    @Test
    public void testPriorities(){
        AbstractMessageEventGenerator generator = new MulticastEventGenerator(this.settings);
        MulticastCreateEvent event;
        final int maxPrio=10;
        final int minPrio=1;
        for(int i = 0; i < AbstractMessageEventGeneratorTest.NR_TRIALS_IN_TEST; i++) {
            event = (MulticastCreateEvent) generator.nextEvent();
            assertTrue(event.getPriority() <= maxPrio);
            assertTrue(event.getPriority() >= minPrio);
        }
    }

    /**
     * If multiple MulticastEventGenerators exist, only one should create the groups (in the constructor)
     * and only one should fill them (in createGroups).
     * If the EventGenerators disagree on the number of groups, problematic behavior like messages to empty groups
     * might occur.
     */
    @Test
    public void testTwoGeneratorsWorkWithTheSameNumberOfGroups(){
        // Since the drawing the number of groups is random, check multiple times
        // whether using a second generator changes anything
        for (int i = 0; i< NR_TRIALS_IN_TEST; i++) {
            // The first part does not test whether generators work with the same number
            // This can only show whether a generator changes the number of groups
            AbstractMessageEventGenerator generator1 = new MulticastEventGenerator(this.settings);
            int noOfGroupsFrom1 = Group.getGroups().length;
            AbstractMessageEventGenerator generator2 = new MulticastEventGenerator(this.settings);
            int notOfGroupsFrom2 = Group.getGroups().length;
            assertEquals("Second generator should not change number of groups",
                    noOfGroupsFrom1, notOfGroupsFrom2);

            //Second part: nextEvent retrieves random groups
            //If the generators work with a different number of groups it shows here, since nextEvent will draw
            // non-existent groups
            for(int j = 0; j < NR_TRIALS_IN_TEST; j++) {
                generator1.nextEvent();
                generator2.nextEvent();
            }
        }


    }

    /**
     * Gets the class name of the class to generate message events with.
     */
    @Override
    protected String getMessageEventGeneratorClassName() {
        return MulticastEventGenerator.class.toString();
    }
    
    /**
     * Gets a generator of the class to generate message events with.
     */
    @Override
    protected AbstractMessageEventGenerator createGenerator(){
        return new MulticastEventGenerator(this.settings);
    }
}
