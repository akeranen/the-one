package test;

import core.DTNHost;

import java.io.IOException;

/**
 * Contains tests for message reports.
 *
 * Created by Marius Meyer on 24.03.17.
 */
public abstract class AbstractMessageReportTest extends AbstractReportTest{

    /**
     * Transfers the specified message between the specified hosts.
     */
    protected static void transferMessage(String messageId, DTNHost from, DTNHost to) {
        from.sendMessage(messageId, to);
        to.messageTransferred(messageId, from);
    }

    @Override
    public abstract void reportCorrectlyHandlesWarmUpTime() throws IOException;

    @Override
    protected abstract Class getReportClass();
}
