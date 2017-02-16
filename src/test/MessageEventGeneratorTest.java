package test;

import input.BroadcastCreateEvent;
import input.ExternalEvent;
import input.MessageCreateEvent;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import input.MessageEventGenerator;

import test.TestSettings;

/**
 * Contains tests for the MessageEventGenerator class.
 *
 * Created by Britta Heymann on 16.02.2017.
 */
public class MessageEventGeneratorTest {
    private TestSettings settings;

    @Before
    public void init() {
        this.settings = new TestSettings();

        this.settings.putSetting("Events.nrof", "1");

        this.settings.putSetting("class", MessageEventGenerator.class.toString());
        this.settings.putSetting(MessageEventGenerator.MESSAGE_INTERVAL_S, "1,2");
        this.settings.putSetting(MessageEventGenerator.MESSAGE_SIZE_S, "500k,1M");
        this.settings.putSetting(MessageEventGenerator.HOST_RANGE_S, "0,126");
        this.settings.putSetting(MessageEventGenerator.MESSAGE_ID_PREFIX_S, "M");
    }

    @Test
    public void testNextEventOnlyCreatesOneToOneMessagesByDefault() {
        MessageEventGenerator generator = new MessageEventGenerator(this.settings);
        for(int i = 0; i < 10; i++) {
            assertTrue(
                    "Event should have been the creation of a 1-to-1 message.",
                    generator.nextEvent() instanceof MessageCreateEvent);
        }
    }

    @Test
    public void testNextEventOnlyCreatesOneToOneMessagesIfDifferentMessageTypeSettingIsSetToFalse() {
        this.settings.putSetting(MessageEventGenerator.ENABLE_DIFFERENT_TYPES_S, "false");
        MessageEventGenerator generator = new MessageEventGenerator(this.settings);
        for(int i = 0; i < 10; i++) {
            assertTrue(
                    "Event should have been the creation of a 1-to-1 message.",
                    generator.nextEvent() instanceof MessageCreateEvent);
        }
    }

    @Test
    public void testNextEventCreatesBothBroadcastAndOneToOneMessagesIfDifferentMessageTypeSettingIsSetToTrue() {
        this.settings.putSetting(MessageEventGenerator.ENABLE_DIFFERENT_TYPES_S, "true");
        MessageEventGenerator generator = new MessageEventGenerator(this.settings);

        boolean broadcastCreated = false;
        boolean oneToOneCreated = false;
        for(int i = 0; i < 20; i++) {
            ExternalEvent event = generator.nextEvent();
            if (event instanceof BroadcastCreateEvent) {
                broadcastCreated = true;
            } else if (event instanceof  MessageCreateEvent) {
                oneToOneCreated = true;
            }
        }

        assertTrue("Broadcasts should have been created.", broadcastCreated);
        assertTrue("One to one messages should have been created.", oneToOneCreated);
    }
}
