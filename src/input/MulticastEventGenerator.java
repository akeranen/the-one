package input;


import core.DTNHost;
import core.Group;
import core.Settings;
import core.SimScenario;

import java.util.List;

/**
 * Multicast creation external events generator. Creates uniformly distributed
 * multicast creation patterns whose message size and inter-message intervals can
 * be configured.
 *
 * Created by Marius Meyer on 08.03.17.
 */
public class MulticastEventGenerator extends AbstractMessageEventGenerator {

    /** group count of groups used in the generator */
    private static final String GROUP_COUNT_S = "group_count";

    /**
     * range of group sizes used in the generator
     */
    private static final String GROUP_SIZE_RANGE_S = "group_size";

    //default values, if nothing is given in the settings file
    private static final int DEFAULT_GROUP_COUNT = 5;
    private static final int DEFAULT_MIN_GROUP_SIZE = 3;
    private static final int DEFAULT_MAX_GROUP_SIZE = 10;

    /**
     * Sizes of the groups, that are created for the message generator
     */
    private int[] groupSizeRange = {DEFAULT_MIN_GROUP_SIZE,DEFAULT_MAX_GROUP_SIZE};

    /**
     * address range of the created groups
     */
    private int[] groupAddressRange;

    /**
     *variable used to create groups only the first time {@link MulticastEventGenerator#nextEvent()}
     * is called.
     */
    private boolean nodesAreAssignedToGroups;

    /**
     * Constructor, initializes the interval between events,
     * and the size of messages generated, as well as number
     * of hosts in the network.
     *
     * @param s                           Settings for this generator.
     */
    public MulticastEventGenerator(Settings s) {
        super(s, true);
        int groupCount = s.getInt(GROUP_COUNT_S,DEFAULT_GROUP_COUNT);
        this.groupAddressRange = new int[Settings.EXPECTED_VALUE_NUMBER_FOR_RANGE];
        this.groupAddressRange[0] = 1;
        this.groupAddressRange[1] = groupCount;
        if (s.contains(GROUP_SIZE_RANGE_S)) {
            this.groupSizeRange = s.getCsvInts(GROUP_SIZE_RANGE_S, Settings.EXPECTED_VALUE_NUMBER_FOR_RANGE);
        }
        for (int i = 1; i <= groupCount; i++){
            Group.createGroup(i);
        }
    }

    /**
     * Assigns nodes randomly to the existing groups, respecting the defined group size range.
     */
    private void assignNodesToGroups(){
        List<DTNHost> hosts = SimScenario.getInstance().getHosts();
        for(Group g : Group.getGroups()){
            //determine the size of the next group
            int nextGroupSize = groupSizeRange[0] + rng.nextInt(groupSizeRange[1] - groupSizeRange[0]);
            //let nodes join the group
            for (int i = 0; i < nextGroupSize; i++){
                DTNHost host;
                //find node that is not already in the current group
                do {
                    host = hosts.get(rng.nextInt(hosts.size()));
                } while (g.contains(host.getAddress()));
                host.joinGroup(g);
            }
        }
    }

    /**
     * Returns the next multicast creation event.
     * @see EventQueue#nextEvent()
     */
    @Override
    public ExternalEvent nextEvent() {
        //check this when the first event is requested
        //this is needed, because access to the hosts is needed and not possible during the call of the constructor
        if (!nodesAreAssignedToGroups){
            assignNodesToGroups();
            nodesAreAssignedToGroups = true;
        }

        /* Message is a one way message */
        int responseSize = 0;

        /* Draw additional message properties and create message. */
        int interval = this.drawNextEventTimeDiff();
        int group = this.drawHostAddress(this.groupAddressRange);
        Integer[] groupMembers = Group.getGroup(group).getMembers();
        int sender = groupMembers[rng.nextInt(groupMembers.length)];
        ExternalEvent messageCreateEvent = new MulticastCreateEvent(
                sender,
                group,
                this.getID(),
                this.drawMessageSize(),
                responseSize,
                this.nextEventsTime);

        /* Update next event time before returning. */
        this.advanceToNextEvent(interval);
        return messageCreateEvent;
    }
}
