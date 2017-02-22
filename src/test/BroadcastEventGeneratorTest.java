package test;

import org.junit.Test;

import core.SettingsError;

import input.BroadcastCreateEvent;
import input.BroadcastEventGenerator;
import input.AbstractMessageEventGenerator;

import static org.junit.Assert.assertTrue;

/**
 * Contains tests for the {@link BroadcastEventGenerator} class.
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

    /**
     * Gets the class name of the class to generate message events with.
     */
    @Override
    protected String getMessageEventGeneratorClassName() {
        return BroadcastEventGenerator.class.toString();
    }
}
