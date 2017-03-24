package core;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A group of nodes that is used for group messaging
 *
 * Created by Marius Meyer on 08.03.17.
 */
public final class Group implements Addressable {

    /**
     * Map with all existent groups. Key is the address of the group and value the group itself.
     */
    private static Map<Integer,Group> groups = new ConcurrentHashMap<>();

    /**
     * The address of the group
     */
    private int address;

    /**
     * List of group member addresses.
     */
    private List<Integer> members;

    /**
     * Creates a new group
     *
     * @param address address of the new group
     */
    private Group(int address){
        if (groups.containsKey(address)) {
            throw new AssertionError("Group address already assigned to another group: " + address);
        }
        this.address = address;
        members = Collections.synchronizedList(new ArrayList<Integer>());
    }

    /**
     * Creates and returns a new group
     *
     * @param address address of the new created group
     * @return a new group
     */
    public static Group createGroup(int address){
        Group newGroup = new Group(address);
        groups.put(newGroup.getAddress(),newGroup);
        return newGroup;
    }

    /**
     *Returns the group for a specified group address
     *
     * @param address the group address
     * @return the group with the specified address or null, if not existent
     */
    public static Group getGroup(int address){
        if (groups.containsKey(address)){
            return groups.get(address);
        } else return null;
    }

    /**
     * Returns an existing group with the given address or creates a new group with this
     * address, when it is not existing yet
     * @param address group address
     * @return a group with the given address
     */
    public static Group getOrCreateGroup(int address){
        Group result = getGroup(address);
        if (result == null){
            return createGroup(address);
        }else {
            return result;
        }
    }

    /**
     *Return an array of already existent groups
     *
     * @return a array of groups
     */
    public static Group[] getGroups(){
        return groups.values().toArray(new Group[0]);
    }

    /**
     * Clears the global group registry
     */
    public static void clearGroups(){
        groups.clear();
    }

    /**
     * Lets a certain host join the group
     *
     * @param host host that should be added
     */
    public void addHost(DTNHost host){
        if (!members.contains(host.getAddress())) {
            members.add(host.getAddress());
        }
    }

    /**
     * Returns the addresses of the nodes joined to the group
     * @return list of addresses of nodes in the group
     */
    public Integer[] getMembers(){
        return members.toArray(new Integer[0]);
    }


    /**
     *Check, if a node is in the group
     *
     * @param address node address to check
     * @return true, if the group contains the node address
     */
    public boolean contains(int address){
        return members.contains(address);
    }

    /**
     * Returns the address of the group
     *
     * @return the group address
     */
    @Override
    public int getAddress(){
        return address;
    }

    /**
     * Returns a string representation of the object
     *
     * @return the string representation
     */
    @Override
    public String toString(){
        return "Group " + address + ", Members="+ Arrays.toString(members.toArray());
    }


}
