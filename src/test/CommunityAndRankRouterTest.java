/*
 * Copyright (C) 2016 Michael Dougras da Silva
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package test;

import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimScenario;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import routing.CommunityAndRankRouter;
import routing.MessageRouter;
import routing.SprayAndWaitRouter;
import static test.AbstractRouterTest.ts;

/**
 * Unit tests for CommunityAndRankRouter class.
 */
public class CommunityAndRankRouterTest extends AbstractRouterTest {

    private static final String ROUTER_NS = "RouterTest";
    
    private int[] localRank = new int[]{10, 20, 30, 40};
    private int[] globalRank = new int[]{10, 20, 30, 40};
    private ArrayList<Set<DTNHost>> communities;
    private Message m1;
    
    private class RouterTest extends CommunityAndRankRouter {

        public RouterTest(Settings set) {
            super(set, ROUTER_NS);
        }
        
        public RouterTest(RouterTest p) {
            super(p);
        }
        
        @Override
        public double getGlobalRank() {
            return globalRank[getHost().getAddress()];
        }

        @Override
        public double getLocalRank() {
            return localRank[getHost().getAddress()];
        }

        @Override
        public Set<DTNHost> getCommunity() {
            return communities.get(getHost().getAddress());
        }

        @Override
        public MessageRouter replicate() {
            return new RouterTest(this);
        }
        
    }
    
    private RouterTest r0;
    private RouterTest r1;
    private RouterTest r2;
    private RouterTest r3;
    
    @Override
    protected void setUp() throws Exception {
        ts.setNameSpace(null);
        ts.putSetting(MessageRouter.B_SIZE_S, "" + BUFFER_SIZE);
        ts.putSetting(ROUTER_NS + ".k", "3");
        ts.putSetting(ROUTER_NS + ".familiarThreshold", "700");
        setRouterProto(new RouterTest(ts));
        super.setUp();
        this.communities = new ArrayList<Set<DTNHost>>();
        this.communities.add(new HashSet<DTNHost>(Arrays.asList(new DTNHost[]{h0, h1})));
        this.communities.add(new HashSet<DTNHost>(Arrays.asList(new DTNHost[]{h2, h1})));
        this.communities.add(new HashSet<DTNHost>(Arrays.asList(new DTNHost[]{h2, h3})));
        this.communities.add(new HashSet<DTNHost>(Arrays.asList(new DTNHost[]{h2, h3})));
        m1 = new Message(h0, h3, msgId1, 1);
        r0 = (RouterTest) h0.getRouter();
        r1 = (RouterTest) h1.getRouter();
        r2 = (RouterTest) h2.getRouter();
        r3 = (RouterTest) h3.getRouter();
    }
    
    private void advanceWorld(int seconds) {
        clock.advance(1);
        updateAllNodes();
    }
    
    /**
     * Test delivery when have a contact with the destination
     */
    public void testDirectDelivery() {
        // Create a new message
        h0.createNewMessage(m1);
        checkCreates(1);
        updateAllNodes();
        
        // Contact between h0 and h3 -- source and destination of m1
        h0.forceConnection(h3, h0.getInterfaces().get(0).getInterfaceType(), true);
        
        advanceWorld(1);
        
        assertTrue(mc.next());
        // The last event was start of a message transfer
        assertEquals(mc.getLastType(), mc.TYPE_START);
        // Verify the ID of the message
        assertEquals(mc.getLastMsg().getId(), msgId1);
        // Transfer was from host
        assertEquals(mc.getLastFrom(), h0);
        // Transfer was to host
        assertEquals(mc.getLastTo(), h3);
        
        advanceWorld(1);
        
        assertTrue(mc.next());
        // The message transfer has been completed
        assertEquals(mc.getLastType(), mc.TYPE_RELAY);
        assertEquals(mc.getLastFrom(), h0);
        assertEquals(mc.getLastTo(), h3);
        // This was the first delivery from m1 to h3
        assertTrue(mc.getLastFirstDelivery());
        
    }
    
    /**
     * Test transfer when both arent in the destination community and global rank of the other node
     * is greater.
     */
    public void testTransferGlobalRank() {
        // Create a new message
        h0.createNewMessage(m1);
        checkCreates(1);
        updateAllNodes();
        
        // Contact between h0 and h1 -- h1 has greater global rank and is not in the destination community
        h0.forceConnection(h1, h0.getInterfaces().get(0).getInterfaceType(), true);
        
        advanceWorld(1);
        
        assertTrue(mc.next());
        // The last event was start of a message transfer
        assertEquals(mc.getLastType(), mc.TYPE_START);
        // Verify the ID of the message
        assertEquals(mc.getLastMsg().getId(), msgId1);
        // Transfer was from host
        assertEquals(mc.getLastFrom(), h0);
        // Transfer was to host
        assertEquals(mc.getLastTo(), h1);
        
        advanceWorld(1);
        
        assertTrue(mc.next());
        // The message transfer has been completed
        assertEquals(mc.getLastType(), mc.TYPE_RELAY);
        assertEquals(mc.getLastFrom(), h0);
        assertEquals(mc.getLastTo(), h1);
        // This was the first delivery from m1 to h3
        assertFalse(mc.getLastFirstDelivery());
    }
    
    /**
     * Test transfer when both are in the destination community and local rank of the other node
     * is greater.
     */
    public void testTransferLocalRank() {
        globalRank[0] = 100;
        communities.get(0).add(h3);
        communities.get(1).add(h3);
        
        // Create a new message
        h0.createNewMessage(m1);
        checkCreates(1);
        updateAllNodes();
        
        
        
        // Contact between h0 and h1 -- h1 has greater lobal rank, both are in the destination community
        h0.forceConnection(h1, h0.getInterfaces().get(0).getInterfaceType(), true);
        
        advanceWorld(1);
        
        assertTrue(mc.next());
        // The last event was start of a message transfer
        assertEquals(mc.getLastType(), mc.TYPE_START);
        // Verify the ID of the message
        assertEquals(mc.getLastMsg().getId(), msgId1);
        // Transfer was from host
        assertEquals(mc.getLastFrom(), h0);
        // Transfer was to host
        assertEquals(mc.getLastTo(), h1);
        
        advanceWorld(1);
        
        assertTrue(mc.next());
        // The message transfer has been completed
        assertEquals(mc.getLastType(), mc.TYPE_RELAY);
        assertEquals(mc.getLastFrom(), h0);
        assertEquals(mc.getLastTo(), h1);
        // This was the first delivery from m1 to h3
        assertFalse(mc.getLastFirstDelivery());
    }
    
    /**
     * Test transfer when both are in the destination community and local rank of the other node
     * is lesser.
     */
    public void testNotTransferLocalRank() {
        localRank[0] = 100;
        communities.get(0).add(h3);
        communities.get(1).add(h3);
        
        // Create a new message
        h0.createNewMessage(m1);
        checkCreates(1);
        updateAllNodes();
        
        
        
        // Contact between h0 and h1 -- h1 has greater lobal rank, both are in the destination community
        h0.forceConnection(h1, h0.getInterfaces().get(0).getInterfaceType(), true);
        
        advanceWorld(1);
        
        assertFalse(mc.next());
    }
    
    /**
     * Test transfer when both arent in the destination community and global rank of the other node
     * is lesser.
     */
    public void testNotTransferGlobalRank() {
        // Create a new message
        h0.createNewMessage(m1);
        checkCreates(1);
        updateAllNodes();
        
        globalRank[1] = 5;
        // Contact between h0 and h1 -- h1 has lesser global rank and is not in the destination community
        h0.forceConnection(h1, h0.getInterfaces().get(0).getInterfaceType(), true);
        
        advanceWorld(1);
        
        assertFalse(mc.next());
    }
    
    /**
     * Test transfer when only the other node is in the destination community.
     */
    public void testTransferDestCommunity() {
        // Create a new message
        h0.createNewMessage(m1);
        checkCreates(1);
        updateAllNodes();
        
        // Contact between h0 and h2 -- h2 is in the destination community
        h0.forceConnection(h2, h0.getInterfaces().get(0).getInterfaceType(), true);
        
        advanceWorld(1);
        
        assertTrue(mc.next());
        // The last event was start of a message transfer
        assertEquals(mc.getLastType(), mc.TYPE_START);
        // Verify the ID of the message
        assertEquals(mc.getLastMsg().getId(), msgId1);
        // Transfer was from host
        assertEquals(mc.getLastFrom(), h0);
        // Transfer was to host
        assertEquals(mc.getLastTo(), h2);
        
        advanceWorld(1);
        
        assertTrue(mc.next());
        // The message transfer has been completed
        assertEquals(mc.getLastType(), mc.TYPE_RELAY);
        assertEquals(mc.getLastFrom(), h0);
        assertEquals(mc.getLastTo(), h2);
        // This was the first delivery from m1 to h3
        assertFalse(mc.getLastFirstDelivery());
    }
   
   public void testDontRemoveDeliveredMessages() throws Exception {
        ts.setNameSpace(null);
        ts.putSetting(MessageRouter.B_SIZE_S, "" + BUFFER_SIZE);
        ts.putSetting(ROUTER_NS + ".K", "3");
        ts.putSetting(ROUTER_NS + ".familiarThreshold", "700");
        setRouterProto(new RouterTest(ts));
        super.setUp();
        this.communities = new ArrayList<Set<DTNHost>>();
        this.communities.add(new HashSet<DTNHost>(Arrays.asList(new DTNHost[]{h0, h1})));
        this.communities.add(new HashSet<DTNHost>(Arrays.asList(new DTNHost[]{h2, h1})));
        this.communities.add(new HashSet<DTNHost>(Arrays.asList(new DTNHost[]{h2, h3})));
        this.communities.add(new HashSet<DTNHost>(Arrays.asList(new DTNHost[]{h2, h3})));
        m1 = new Message(h0, h3, msgId1, 1);
        r0 = (RouterTest) h0.getRouter();
        r1 = (RouterTest) h1.getRouter();
        r2 = (RouterTest) h2.getRouter();
        r3 = (RouterTest) h3.getRouter();
       
       
       // Create the message to be sent
       h0.createNewMessage(m1);
       checkCreates(1);
       updateAllNodes();
       
       // t1
       // h0 -> h1, send the message
       h0.forceConnection(h1, h0.getInterfaces().get(0).getInterfaceType(), true);
       advanceWorld(1);
       assertTrue(mc.next());
       assertEquals(mc.getLastType(), mc.TYPE_START);
       advanceWorld(1);
       assertTrue(mc.next());
       assertEquals(mc.getLastType(), mc.TYPE_RELAY);
       h0.forceConnection(h1, h0.getInterfaces().get(0).getInterfaceType(), false);
       assertTrue(r0.hasMessage(msgId1));
       assertTrue(r1.hasMessage(msgId1));
       
       // t2
       // h1 -> h2
       h1.forceConnection(h2, h1.getInterfaces().get(0).getInterfaceType(), true);
       advanceWorld(1);
       assertTrue(mc.next());
       assertEquals(mc.getLastType(), mc.TYPE_START);
       advanceWorld(1);
       assertTrue(mc.next());
       assertEquals(mc.getLastType(), mc.TYPE_RELAY);
       h1.forceConnection(h2, h1.getInterfaces().get(0).getInterfaceType(), false);
       assertTrue(r0.hasMessage(msgId1));
       assertTrue(r1.hasMessage(msgId1));
       assertTrue(r2.hasMessage(msgId1));
       
       // t3 
       // h2 -> h3
       h2.forceConnection(h3, h2.getInterfaces().get(0).getInterfaceType(), true);
       advanceWorld(1);
       assertTrue(mc.next());
       assertEquals(mc.getLastType(), mc.TYPE_START);
       advanceWorld(1);
       assertTrue(mc.next());
       assertEquals(mc.getLastType(), mc.TYPE_RELAY);
       h2.forceConnection(h3, h2.getInterfaces().get(0).getInterfaceType(), false);
       advanceWorld(1);
       assertFalse(mc.next());
       assertTrue(r0.hasMessage(msgId1));
       assertTrue(r1.hasMessage(msgId1));
       assertTrue(r2.hasMessage(msgId1));
       assertFalse(r3.hasMessage(msgId1));
   }
   
   public void testAlreadyHasTheMessage() {
       // Create a new message
        h0.createNewMessage(m1);
        checkCreates(1);
        updateAllNodes();
        
        // Contact between h0 and h2 -- h2 is in the destination community
        h0.forceConnection(h2, h0.getInterfaces().get(0).getInterfaceType(), true);
        
        advanceWorld(1);
        
        assertTrue(mc.next());
        // The last event was start of a message transfer
        assertEquals(mc.getLastType(), mc.TYPE_START);
        // Verify the ID of the message
        assertEquals(mc.getLastMsg().getId(), msgId1);
        // Transfer was from host
        assertEquals(mc.getLastFrom(), h0);
        // Transfer was to host
        assertEquals(mc.getLastTo(), h2);
        
        advanceWorld(1);
        
        assertTrue(mc.next());
        // The message transfer has been completed
        assertEquals(mc.getLastType(), mc.TYPE_RELAY);
        assertEquals(mc.getLastFrom(), h0);
        assertEquals(mc.getLastTo(), h2);
        assertFalse(mc.getLastFirstDelivery());
        
        // Close connection
        h0.forceConnection(h2, h0.getInterfaces().get(0).getInterfaceType(), false);
        advanceWorld(1);
        assertFalse(mc.next());
        
        h0.forceConnection(h2, h0.getInterfaces().get(0).getInterfaceType(), true);
        advanceWorld(1);
        assertFalse(mc.next());
        
        
   }
}
