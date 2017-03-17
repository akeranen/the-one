package test;

import core.DTNHost;
import core.Group;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

/**
 * Contains tests for the Group class
 *
 * Created by Marius Meyer on 10.03.17.
 */
public class GroupTest {

    private static final int MAX_GROUP_ADDRESS = 3;

    @Before
    public void setUp(){
        Group.clearGroups();
    }

    @Test
    public void testClearGroups(){
        Group.createGroup(0);
        Group.clearGroups();
        TestCase.assertEquals("Groups array should be empty after clearing groups",
                0,Group.getGroups().length);
    }

    @Test
    public void testGetGroups(){
        Group g = Group.createGroup(0);
        Group[] groups = Group.getGroups();
        TestCase.assertEquals("Exactly one group should be in group array",1,groups.length);
        TestCase.assertEquals("The group in the array should be the created one",g,groups[0]);
    }

    @Test
    public void testGetMembers(){
        Group group = Group.createGroup(0);
        DTNHost host1 = new TestDTNHost(new ArrayList<>(),null,new TestSettings());
        group.addHost(host1);
        TestCase.assertEquals("Group should have exavtly one member",1,group.getMembers().length);
        TestCase.assertEquals("Member should be the added host",host1.getAddress(),(int)group.getMembers()[0]);
    }

    @Test
    public void testContainsHost(){
        Group group = Group.createGroup(0);
        DTNHost host1 = new TestDTNHost(new ArrayList<>(),null,new TestSettings());
        group.addHost(host1);
        TestCase.assertTrue("Group should contain added host",group.contains(host1.getAddress()));
    }

    @Test
    public void testContainsNotHost(){
        Group group = Group.createGroup(0);
        DTNHost host1 = new TestDTNHost(new ArrayList<>(),null,new TestSettings());
        TestCase.assertFalse("Group should not contain host",group.contains(host1.getAddress()));
    }

    @Test
    public void testGetGroupReturnsGroupWithCorrectAddress(){
        int testGroupCount = MAX_GROUP_ADDRESS;
        for (int i = 0; i < testGroupCount; i++){
            Group.createGroup(i);
        }
        for (int i = 0; i < testGroupCount; i++){
            TestCase.assertEquals("Group address should be the same as requested address",
                    Group.getGroup(i).getAddress(),i);
        }
    }

    @Test
    public void testGetGroupReturnsNullForNotExistentGroup(){
        Group group = Group.getGroup(MAX_GROUP_ADDRESS);
        TestCase.assertNull("getGroup should return null, if no group with same address is existent",group);
    }

    @Test(expected = AssertionError.class)
    public void testCreateMultipleGroupsWithSameAddressFails(){
        Group.createGroup(0);
        Group.createGroup(0);
    }

    @Test
    public void testJoinGroup(){
        DTNHost host1 = new TestDTNHost(new ArrayList<>(),null,new TestSettings());
        DTNHost host2 = new TestDTNHost(new ArrayList<>(),null,new TestSettings());
        Group group = Group.createGroup(0);
        group.addHost(host1);
        group.addHost(host2);
        TestCase.assertTrue("Node should be in Group",group.contains(host1.getAddress()));
        TestCase.assertTrue("Node should be in Group",group.contains(host2.getAddress()));
    }

    @Test
    public void testGetOrCreateGroup(){
        Group group = Group.getOrCreateGroup(0);
        Group group2 = Group.getOrCreateGroup(0);
        TestCase.assertEquals("Groups should be references to the same object.",group,group2);
    }

    @Test
    public void testToString(){
        Group group = Group.createGroup(0);
        TestCase.assertEquals("String representation of group is not as expected: " + group.toString(),
                "Group "+0,group.toString());
    }
}
