package input;

import java.util.Random;

import core.BroadcastMessage;
import core.DTNHost;
import core.World;

/**
 * External event for creating a broadcast message.
 *
 * Created by Britta Heymann on 15.02.2017.
 */
public class BroadcastCreateEvent extends MessageEvent {
    private int size;
    private int responseSize;
    /** this priority range leads to using priorities 2-10 */
    private static final int PRIORITY_RANGE = 9;

    /**
     * Creates a broadcast creation event with a optional response request
     * @param from The creator of the message
     * @param id ID of the message
     * @param size Size of the message
     * @param responseSize Size of the requested response message or 0 if
     * no response is requested
     * @param time Time, when the message is created
     */
    public BroadcastCreateEvent(int from, String id, int size, int responseSize, double time) {
        super(from, -1, id, time);
        this.size = size;
        this.responseSize = responseSize;
    }

    /**
     * Creates the broadcast message this event represents.
     */
    @Override
    public void processEvent(World world) {
        DTNHost from = world.getNodeByAddress(this.fromAddr);
        Random rn = new Random();
        // offset 2, as message and multicast have priorities 0 and 1
        int priority = rn.nextInt(PRIORITY_RANGE)+2;
        BroadcastMessage messageToCreate = new BroadcastMessage(from, this.id, this.size, priority);
        messageToCreate.setResponseSize(this.responseSize);
        from.createNewMessage(messageToCreate);
    }

    @Override
    public String toString() {
        return super.toString() + " [" + fromAddr + "->everyone] size:" + size + " CREATE";
    }
}
