package test;

import core.Group;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;

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
        int nodeAddress1 = 0;
        int nodeAddress2 = 1;
        Group group = Group.createGroup(0);
        group.addAddress(nodeAddress1);
        group.addAddress(nodeAddress2);
        TestCase.assertTrue("Node should be in Group",group.contains(nodeAddress1));
        TestCase.assertTrue("Node should be in Group",group.contains(nodeAddress2));
    }

    @Test
    public void testGetOrCreateGroup(){
        Group group = Group.getOrCreateGroup(0);
        Group group2 = Group.getOrCreateGroup(0);
        TestCase.assertEquals("Groups should be references to the same object.",group,group2);
    }
}
