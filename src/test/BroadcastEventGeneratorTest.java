package test;

import org.junit.Test;

import core.SettingsError;

import input.BroadcastCreateEvent;
import input.BroadcastEventGenerator;
import input.AbstractMessageEventGenerator;

import static org.junit.Assert.assertTrue;

/**
 * Contains tests for the {@link BroadcastEventGenerator} class.
 * Test set up is handled by the extended {@link AbstractMessageEventGeneratorTest} class.
 *
 * Created by Britta Heymann on 22.02.2017.
 */
public class BroadcastEventGeneratorTest extends AbstractMessageEventGeneratorTest {
    @Test
    public void testNextEventCreatesBroadcastMessages() {
        AbstractMessageEventGenerator generator = new BroadcastEventGenerator(this.settings);
        for(int i = 0; i < AbstractMessageEventGeneratorTest.NR_TRIALS_IN_TEST; i++) {
            assertTrue(
                    "Event should have been the creation of a broadcast message.",
                    generator.nextEvent() instanceof BroadcastCreateEvent);
        }
    }

    @Test(expected = SettingsError.class)
    public void testBroadcastEventGeneratorConstructorThrowsErrorIfSingleHostIsSpecified() {
        this.settings.putSetting(AbstractMessageEventGenerator.HOST_RANGE_S, "0,1");
        new BroadcastEventGenerator(this.settings);
    }
    
    @Test
    public void testPriorities(){
        AbstractMessageEventGenerator generator = new BroadcastEventGenerator(this.settings);
        BroadcastCreateEvent event;
        for(int i = 0; i < AbstractMessageEventGeneratorTest.NR_TRIALS_IN_TEST; i++) {
            event = (BroadcastCreateEvent) generator.nextEvent();
            assertTrue(event.getPriority() <= 10);
            assertTrue(event.getPriority() >= 1);
        }
    }
    
    /**
     * Gets a generator of the class to generate message events with.
     */
    @Override
    protected void createGenerator(){
        this.generator = new BroadcastEventGenerator(this.settings);
    }

    /**
     * Gets the class name of the class to generate message events with.
     */
    @Override
    protected String getMessageEventGeneratorClassName() {
        return BroadcastEventGenerator.class.toString();
    }
}
