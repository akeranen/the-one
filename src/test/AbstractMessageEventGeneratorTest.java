package test;

import core.Settings;
import input.AbstractMessageEventGenerator;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * Contains common initialization for both {@link MessageEventGeneratorTest} and {@link BroadcastEventGeneratorTest}.
 *
 * Created by Britta Heymann on 22.02.2017.
 */
public abstract class AbstractMessageEventGeneratorTest {
    /**
     * The number of times a test is repeated for tests that might be non-deterministic.
     */
    protected static final int NR_TRIALS_IN_TEST = 10;

    protected TestSettings settings;
    protected AbstractMessageEventGenerator generator;

    @Before
    public void init() {
        Settings.init(null);
        this.settings = new TestSettings();
        this.settings.putSetting("Events.nrof", "1");

        this.settings.putSetting("class", this.getMessageEventGeneratorClassName());
        this.settings.putSetting(AbstractMessageEventGenerator.MESSAGE_INTERVAL_S, "0.1,1");
        this.settings.putSetting(AbstractMessageEventGenerator.MESSAGE_SIZE_S, "500k,1M");
        this.settings.putSetting(AbstractMessageEventGenerator.HOST_RANGE_S, "0,14");
        this.settings.putSetting(AbstractMessageEventGenerator.MESSAGE_ID_PREFIX_S, "M");
        this.settings.putSetting(AbstractMessageEventGenerator.PRIORITY_S, "1,10");
    }

    /**
     * Gets the class name of the class to generate message events with.
     */
    protected abstract String getMessageEventGeneratorClassName();
    
    @Test
    public void testDoubleTimeEventDiff(){
        for(int i = 0; i < NR_TRIALS_IN_TEST; i++){
            double time = generator.drawNextEventTimeDiff();
            assertTrue("The next event should occur after at most one second.",
                    time <= 1);
            assertTrue("The next event should occur after at least 0.1 seconds.",
                    time >= 0.1);
        }
    }
}

