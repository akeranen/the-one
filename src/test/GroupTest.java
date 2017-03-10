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
    public void testFetGroupReturnsGroupWithCorrectAddress(){
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
        group.joinGroup(nodeAddress1);
        group.joinGroup(nodeAddress2);
        TestCase.assertTrue("Node should be in Group",group.isInGroup(nodeAddress1));
        TestCase.assertTrue("Node should be in Group",group.isInGroup(nodeAddress2));
    }

    @Test
    public void testLeaveGroup(){
        int nodeAddress1 = 0;
        Group group = Group.createGroup(0);
        group.joinGroup(nodeAddress1);
        group.leaveGroup(nodeAddress1);
        TestCase.assertEquals("No members should be left in the group",0,group.getMemberCount());
    }

    @Test
    public void testDeleteGroup(){
        Group group = Group.createGroup(0);
        group.delete();
        TestCase.assertNull("Group should be deleted from global registry",Group.getGroup(0));
    }

}
