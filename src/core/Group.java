package core;

import java.util.*;

/**
 * A group of nodes that is used for group messaging
 *
 * Created by Marius Meyer on 08.03.17.
 */
public class Group {

    /**
     * Map with all existent groups. Key is the address of the group and value the group itself.
     */
    private static Map<Integer,Group> groups = new HashMap<>();

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
        if (groups.containsKey(address))
            throw new AssertionError("Group address already assigned to another group: "+address);
        this.address = address;
        members = new ArrayList<>();
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
        return groups.getOrDefault(address,null);
    }

    /**
     * Returns an existing group with the given address or creates a new group with this
     * address, when it is not exisiting yet
     * @param address group address
     * @return a group with the given address
     */
    public static Group getOrCreateGroup(int address){
        Group result = getGroup(address);
        return result == null ? createGroup(address) : result;
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
     * Let a specified node join the group
     *
     * @param node the node that should join the group
     */
    public void joinGroup(DTNHost node){
        members.add(node.getAddress());
    }

    /**
     * Returns the number of nodes joined to the group
     * @return number of nodes in the group
     */
    public int getMemberCount(){
        return members.size() - 1;
    }

    /**
     *Check, if node has joined the group
     *
     * @param address node address to check
     * @return true, if the node is in the group
     */
    public boolean isInGroup(int address){
        return members.contains(address);
    }

    /**
     * Returns the address of the group
     *
     * @return the group address
     */
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
        return "Group " + address;
    }


}
