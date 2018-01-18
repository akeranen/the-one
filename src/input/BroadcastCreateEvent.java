package input;

import core.BroadcastMessage;
import core.DTNHost;
import core.World;

/**
 * External event for creating a broadcast message.
 *
 * Created by Britta Heymann on 15.02.2017.
 */
public class BroadcastCreateEvent extends MessageEvent {
    private static final long serialVersionUID = 1;
    
    private int size;
    private int responseSize;

    /**
     * Creates a broadcast creation event with a optional response request
     * @param from The creator of the message
     * @param id ID of the message
     * @param size Size of the message
     * @param responseSize Size of the requested response message or 0 if
     * no response is requested
     * @param time Time, when the message is created
     * @param prio Priority of this message
     */
    public BroadcastCreateEvent(int from, String id, int size, int responseSize, double time, int prio) {
        super(from, -1, id, time, prio);
        this.size = size;
        this.responseSize = responseSize;
    }
    
    /**
     * Creates a broadcast creation event.
     * Using the same parameters as the previous constructor, but the priority.
     * Used for MulticastCreateEvents without explicit priority. Therefore, gets
     * the INVALID_PRIORITY.
     * Calls the previous constructor.
     */
    public BroadcastCreateEvent(int from, String id, int size, int responseSize, double time){
        this(from, id, size, responseSize, time, INVALID_PRIORITY);
    }

    /**
     * Creates the broadcast message this event represents.
     */
    @Override
    public void processEvent(World world) {
        DTNHost from = world.getNodeByAddress(this.fromAddr);
        BroadcastMessage messageToCreate = new BroadcastMessage(from, this.id, this.size, this.priority);
        messageToCreate.setResponseSize(this.responseSize);
        from.createNewMessage(messageToCreate);
    }

    @Override
    public String toString() {
        return super.toString() + " [" + fromAddr + "->everyone] size:" + size + " CREATE";
    }
}
