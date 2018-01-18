package input;

import core.DTNHost;
import core.Group;
import core.MulticastMessage;
import core.World;

/**
 * External event for creating a multicast message
 *
 * Created by Marius Meyer on 08.03.17.
 */
public class MulticastCreateEvent extends MessageEvent {
    private static final long serialVersionUID = 1;

    /**
     * Size of the created message
     */
    private int size;

    /**
     * Creates a multicast creation event.
     *
     * @param from Where the message comes from
     * @param to   Who the message goes to
     * @param id   ID of the message
     * @param time Time when the message event occurs
     * @param prio Priority of the message
     */
    public MulticastCreateEvent(int from, int to, String id, int size, double time, int prio) {
        super(from, to, id, time, prio);
        this.size = size;
    }
    
    /**
     * Creates a multicast creation event.
     * Using the same parameters as the previous constructor, but the priority.
     * Used for MulticastCreateEvents without explicit priority. Therefore, gets
     * the INVALID_PRIORITY.
     * Calls the previous constructor.
     */
    public MulticastCreateEvent(int from, int to, String id, int size, double time){
        this(from, to, id, size, time, INVALID_PRIORITY);
    }

    /**
     * Creates the multicast message this event represents.
     */
    @Override
    public void processEvent(World world) {
        DTNHost from = world.getNodeByAddress(this.fromAddr);
        MulticastMessage messageToCreate =
                new MulticastMessage(from, Group.getGroup(toAddr), this.id, this.size, this.priority);
        from.createNewMessage(messageToCreate);
    }

    @Override
    public String toString() {
        return super.toString() + " [" + fromAddr + "->" + Group.getGroup(toAddr).toString() + "] size:" +
                size + " CREATE";
    }
}
