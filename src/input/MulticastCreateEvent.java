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

    /**
     * Size of the created message
     */
    private int size;
    private int responseSize;

    /**
     * Creates a message event.
     *
     * @param from Where the message comes from
     * @param to   Who the message goes to
     * @param id   ID of the message
     * @param time Time when the message event occurs
     */
    public MulticastCreateEvent(int from, int to, String id, int size, int responseSize, double time) {
        super(from, to, id, time);
        this.size = size;
        this.responseSize = responseSize;
    }

    /**
     * Creates the multicast message this event represents.
     */
    @Override
    public void processEvent(World world) {
        DTNHost from = world.getNodeByAddress(this.fromAddr);
        MulticastMessage messageToCreate =
                new MulticastMessage(from, Group.getGroup(toAddr), this.id, this.size);
        messageToCreate.setResponseSize(this.responseSize);
        from.createNewMessage(messageToCreate);
    }

    @Override
    public String toString() {
        return super.toString() + " [" + fromAddr + "->" + Group.getGroup(toAddr).toString() + "] size:" +
                size + " CREATE";
    }
}
