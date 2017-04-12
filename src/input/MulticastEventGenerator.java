package input;


import core.DTNHost;
import core.Group;
import core.Settings;
import core.SimError;
import core.SimScenario;
import core.World;

/**
 * Multicast creation external events generator. Creates uniformly distributed
 * multicast creation patterns whose message size and inter-message intervals can
 * be configured.
 *
 * Created by Marius Meyer on 08.03.17.
 */
public class MulticastEventGenerator extends AbstractMessageEventGenerator {

    /** range of group count that is used in the generator */
    public static final String GROUP_COUNT_RANGE_S = "group_count";

    /**
     * range of group sizes used in the generator
     */
    public static final String GROUP_SIZE_RANGE_S = "group_size";

    //default values, if nothing is given in the settings file
    private static final int[] DEFAULT_GROUP_COUNT = {2,10};
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
    private static boolean nodesAreAssignedToGroups;

    /**
     * Constructor, initializes the interval between events,
     * and the size of messages generated, as well as number
     * of hosts in the network.
     *
     * @param s                           Settings for this generator.
     */
    public MulticastEventGenerator(Settings s) {
        super(s, true);
        int[] groupCountRange = DEFAULT_GROUP_COUNT;
        if (s.contains(GROUP_COUNT_RANGE_S)){
            groupCountRange = s.getCsvInts(GROUP_COUNT_RANGE_S,Settings.EXPECTED_VALUE_NUMBER_FOR_RANGE);
        }
        int groupCount = rng.nextInt(groupCountRange[1] - groupCountRange[0] + 1) +
                groupCountRange[0];
        this.groupAddressRange = new int[Settings.EXPECTED_VALUE_NUMBER_FOR_RANGE];
        this.groupAddressRange[0] = 1;
        this.groupAddressRange[1] = groupCount;
        if (s.contains(GROUP_SIZE_RANGE_S)) {
            this.groupSizeRange = s.getCsvInts(GROUP_SIZE_RANGE_S, Settings.EXPECTED_VALUE_NUMBER_FOR_RANGE);
        }
        if (groupSizeRange[1] > hostRange[1] - hostRange[0]){
            throw new SimError("Biggest possible group size is greater than the number"+
                    " of hosts specified in host range.");
        }
        //Create groups if no groups are existing yet
        if (Group.getGroups().length == 0) {
            for (int i = 1; i <= groupCount; i++) {
                Group.createGroup(i);
            }
            nodesAreAssignedToGroups = false;
        }
    }

    /**
     * Assigns nodes randomly to the existing groups, respecting the defined group size range.
     */
    private void assignNodesToGroups(){
        World world = SimScenario.getInstance().getWorld();
        for(Group g : Group.getGroups()){
            //determine the size of the next group
            int nextGroupSize = groupSizeRange[0] + rng.nextInt(groupSizeRange[1] - groupSizeRange[0] + 1);
            //let nodes join the group
            for (int i = 0; i < nextGroupSize; i++){
                DTNHost host;
                //find node that is not already in the current group
                do {
                    int nextHostCandidate = rng.nextInt(hostRange[1] - hostRange[0]) + hostRange[0];
                    host = world.getNodeByAddress(nextHostCandidate);
                } while (g.contains(host.getAddress()));
                g.addHost(host);
            }
        }
    }

    private static synchronized void setNodesAsAssigned(){
        nodesAreAssignedToGroups = true;
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
            setNodesAsAssigned();
        }

        /* Draw additional message properties and create message. */
        double interval = this.drawNextEventTimeDiff();
        int group = this.drawHostAddress(this.groupAddressRange);
        Integer[] groupMembers = Group.getGroup(group).getMembers();
        int sender = groupMembers[rng.nextInt(groupMembers.length)];
        ExternalEvent messageCreateEvent = new MulticastCreateEvent(
                sender,
                group,
                this.getID(),
                this.drawMessageSize(),
                this.nextEventsTime);

        /* Update next event time before returning. */
        this.advanceToNextEvent(interval);
        return messageCreateEvent;
    }
}
