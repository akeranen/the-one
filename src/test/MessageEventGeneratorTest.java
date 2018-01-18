package test;

import core.SettingsError;
import input.MessageCreateEvent;
import input.MessageEventGenerator;
import input.MulticastEventGenerator;

import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.*;

import input.AbstractMessageEventGenerator;

/**
 * Contains tests for the {@link MessageEventGenerator} class.
 * Test set up is handled by the extended {@link AbstractMessageEventGeneratorTest} class.
 *
 * Created by Britta Heymann on 16.02.2017.
 */
public class MessageEventGeneratorTest extends AbstractMessageEventGeneratorTest {
    @Test
    public void testNextEventCreatesOneToOneMessages() {
        AbstractMessageEventGenerator generator = new MessageEventGenerator(this.settings);
        for(int i = 0; i < AbstractMessageEventGeneratorTest.NR_TRIALS_IN_TEST; i++) {
            assertTrue(
                    "Event should have been the creation of a 1-to-1 message.",
                    generator.nextEvent() instanceof MessageCreateEvent);
        }
    }

    @Test(expected = SettingsError.class)
    public void testMessageEventGeneratorConstructorThrowsErrorIfSingleHostIsSpecified() {
        this.settings.putSetting(AbstractMessageEventGenerator.HOST_RANGE_S, "0,1");
        new MessageEventGenerator(this.settings);
    }

    @Test
    public void testMessageEventGeneratorConstructorDoesNotThrowIfAdditionalHostIsSpecifiedViaToHosts() {
        this.settings.putSetting(AbstractMessageEventGenerator.HOST_RANGE_S, "0,1");
        this.settings.putSetting(MessageEventGenerator.TO_HOST_RANGE_S, "1,2");
        AbstractMessageEventGenerator generator = new MessageEventGenerator(this.settings);
        Assert.assertNotNull(generator);
    }

    @Test(expected = SettingsError.class)
    public void testMessageEventGeneratorConstructorThrowsErrorIfSingleToHostEqualsSingleHost() {
        this.settings.putSetting(AbstractMessageEventGenerator.HOST_RANGE_S, "0,1");
        this.settings.putSetting(MessageEventGenerator.TO_HOST_RANGE_S, "0,1");
        new MessageEventGenerator(this.settings);
    }
    
    @Test
    public void testPriorities(){
        AbstractMessageEventGenerator generator = new MessageEventGenerator(this.settings);
        MessageCreateEvent event;
        for(int i = 0; i < AbstractMessageEventGeneratorTest.NR_TRIALS_IN_TEST; i++) {
            event = (MessageCreateEvent) generator.nextEvent();
            assertTrue(event.getPriority() <= 10);
            assertTrue(event.getPriority() >= 1);
        }
    }

    /**
     * Gets the class name of the class to generate message events with.
     */
    @Override
    protected String getMessageEventGeneratorClassName() {
        return MessageEventGenerator.class.toString();
    }
    
    /**
     * Gets a generator of the class to generate message events with.
     */
    @Override
    protected AbstractMessageEventGenerator createGenerator(){
        return new MessageEventGenerator(this.settings);
    }
}
