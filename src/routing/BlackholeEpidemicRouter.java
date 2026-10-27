package routing;

import java.util.HashSet;
import java.util.Set;

import core.DTNHost;
import core.Message;
import core.Settings;

/** Epidemic node that accepts data messages from neighbours and drops them. */
public class BlackholeEpidemicRouter extends EpidemicRouter {

    public BlackholeEpidemicRouter(Settings s) { super(s); }

    protected BlackholeEpidemicRouter(BlackholeEpidemicRouter proto) {
        super(proto);
    }

    @Override
    public BlackholeEpidemicRouter replicate() {
        return new BlackholeEpidemicRouter(this);
    }

    private final Set<String> swallowed = new HashSet<String>();

    @Override
    protected int checkReceiving(Message m, DTNHost from) {
        if (swallowed.contains(m.getId())) return DENIED_OLD;
        return super.checkReceiving(m, from);
    }

    @Override
    public Message messageTransferred(String id, DTNHost from) {
        Message m = super.messageTransferred(id, from);
        if (m != null && m.getTo() != getHost() && hasMessage(id)) {
            swallowed.add(id);
            deleteMessage(id, true);
        }
        return m;
    }
}
