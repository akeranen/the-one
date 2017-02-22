package test;

import input.MessageCreateEvent;
import org.junit.Test;
import static org.junit.Assert.*;

import input.MessageEventGenerator;

/**
 * Contains tests for the MessageEventGenerator class.
 *
 * Created by Britta Heymann on 16.02.2017.
 */
public class MessageEventGeneratorTest extends AbstractMessageEventGeneratorTest {
    @Test
    public void testNextEventCreatesOneToOneMessages() {
        MessageEventGenerator generator = new MessageEventGenerator(this.settings);
        for(int i = 0; i < this.NR_TRIALS_IN_TEST; i++) {
            assertTrue(
                    "Event should have been the creation of a 1-to-1 message.",
                    generator.nextEvent() instanceof MessageCreateEvent);
        }
    }

    /**
     * Gets the class name of the class to generate message events with.
     */
    @Override
    protected String getMessageEventGeneratorClassName() {
        return MessageEventGenerator.class.toString();
    }
}
