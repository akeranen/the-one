package test;

import org.junit.Test;

import core.SettingsError;

import input.BroadcastCreateEvent;
import input.BroadcastEventGenerator;
import input.MessageEventGenerator;

import static org.junit.Assert.assertTrue;

/**
 * Contains tests for the BroadcastEventGenerator class.
 *
 * Created by Britta Heymann on 22.02.2017.
 */
public class BroadcastEventGeneratorTest extends AbstractMessageEventGeneratorTest {
    @Test
    public void testNextEventCreatesBroadcastMessages() {
        MessageEventGenerator generator = new BroadcastEventGenerator(this.settings);
        for(int i = 0; i < NR_TRIALS_IN_TEST; i++) {
            assertTrue(
                    "Event should have been the creation of a broadcast message.",
                    generator.nextEvent() instanceof BroadcastCreateEvent);
        }
    }

    @Test(expected = SettingsError.class)
    public void testBroadcastEventGeneratorConstructorThrowsOnRecipientSpecification() {
        this.settings.putSetting(MessageEventGenerator.TO_HOST_RANGE_S, "1, 24");
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
