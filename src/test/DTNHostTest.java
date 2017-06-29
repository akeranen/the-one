package test;


import core.ConnectionListener;
import core.Coord;
import core.DTNHost;
import core.MessageListener;
import core.ModuleCommunicationBus;
import core.MovementListener;
import core.NetworkInterface;
import core.Settings;
import interfaces.SimpleBroadcastInterface;
import junit.framework.TestCase;
import movement.ExtendedMovementModel;
import movement.MovementModel;
import movement.Path;
import routing.MessageRouter;
import routing.PassiveRouter;

import java.util.ArrayList;

/**
 * @author teemuk
 */
public class DTNHostTest extends TestCase {

    private static final String NO_MORE_CONNECTIONS="The host should not have any connections now.";

    private static Path expectedPath = new Path();
    private TestUtils utils = new TestUtils(new ArrayList<ConnectionListener>(),
            new ArrayList<MessageListener>(),
            new TestSettings());;

    public DTNHostTest(){
        DTNHost.reset();
        expectedPath.setSpeed(1);
        expectedPath.addWaypoint(new Coord(1,1));
    }

  //==========================================================================//


  //==========================================================================//
  // Tests
  //==========================================================================//
  /**
   * Tests the case where the DTNHost has no interfaces configured.
   */
    public void testNoInterfaces() {
        final DTNHost host = createHost(new DummyMovement(null));
        // Tests
        assertFalse("Radio reported as active.", host.isRadioActive());
    }

    /**
     * Tests, if the current movement model of a host is interrupted after the interruptMovement method is called.
     * The method tests this by switching the movement model. The returned path by the host should then be the one
     * of the new movement model.
     */
    public void testInterruptMovement(){
        DummyExtendedMovementModel movementModel = new DummyExtendedMovementModel();
        movementModel.setCurrentMovementModel(new DummyMovement(null));
        DTNHost host = createHost(movementModel);
        host.move(0);
        //call interrupt on host
        host.interruptMovement();
        host.move(0);
        assertEquals("Host should switch movement model and return expected path",
                expectedPath,host.getPath());
    }

    /**
     * Checks that {@link DTNHost#getMovement()} returns the used movement model.
     */
    public void testGetMovement() {
        // Create host with specific movement model.
        Coord location = new Coord(0, 0);
        MovementModel movementModel = new StationaryMovement(location);
        DTNHost host = createHost(movementModel);

        // Test it is returned correctly.
        MovementModel returnedMovement = host.getMovement();
        assertTrue(
                "Should have returned different movement model type.",
                returnedMovement instanceof StationaryMovement);
        assertEquals("Expected different location.", location, returnedMovement.getInitialLocation());
    }

    /**
     * Checks whether {@link DTNHost#hasConnections()} works correctly
     */
    public void testHasConnections(){
        //Create a single host. Initially it shouldn't have any connections
        DTNHost host = utils.createHost();
        assertFalse("Initially hosts should not have any connections.", host.hasConnections());

        //Connect that host to another one. Both should have connections.
        DTNHost host2 = utils.createHost();
        host.connect(host2);
        assertTrue("The host should be connected now.", host.hasConnections());
        assertTrue("Both hosts should be connected", host2.hasConnections());

        //Connect the initial host to a third one. All three hosts should have connections.
        DTNHost host3 = utils.createHost();
        host.connect(host3);
        assertTrue("The host should have connections when connected to both.", host.hasConnections());
        assertTrue("The second host should still be connected.", host2.hasConnections());
        assertTrue("The third host should be connected now", host3.hasConnections());

        //Remove all connections from the initial host.
        //Since the other host weren't connected to each other, no host should have any connections.
        AbstractRouterTest.disconnect(host);
        assertFalse(NO_MORE_CONNECTIONS, host.hasConnections());
        assertFalse(NO_MORE_CONNECTIONS, host2.hasConnections());
        assertFalse(NO_MORE_CONNECTIONS, host3.hasConnections());
    }

    /**
     * Checks whether {@link DTNHost#hasConnections()} works correctly for hosts with multiple
     * Network Interfaces
     */
    public void testHasConnectionsWithMultipleNetworkInterfaces(){
        DTNHost hostWithTwoNI = createDTNHostWithTwoNetworkInterfaces();
        NetworkInterface interface1 = hostWithTwoNI.getInterfaces().get(0);
        NetworkInterface interface2 = hostWithTwoNI.getInterfaces().get(1);

        assertFalse("Initially hosts should not have any connections.", hostWithTwoNI.hasConnections());

        //Create a host with a single network interface to connect to
        DTNHost host1 = utils.createHost();
        interface1.connect(host1.getInterfaces().get(0));
        assertTrue("One connected interface should mean hasConnections returns true.", hostWithTwoNI.hasConnections());
        assertTrue("The host with just one interface should be connected as well.", host1.hasConnections());

        //Create another host with single network interface to connect the second interface to
        DTNHost host2 = utils.createHost();
        interface2.connect(host2.getInterfaces().get(0));
        assertTrue("Two connected interfaces should mean hasConnections returns true.", hostWithTwoNI.hasConnections());
        assertTrue("The hosts with just one interface each should be connected as well.", host1.hasConnections());
        assertTrue("The hosts with just one interface each should be connected as well.", host2.hasConnections());

        //Break the first connection
        interface1.destroyConnection(host1.getInterfaces().get(0));
        assertTrue("hasConnections should still return true.", hostWithTwoNI.hasConnections());

        //Break the other connection
        interface2.destroyConnection(host2.getInterfaces().get(0));
        assertFalse("hasConnections should still return false once all connections were destroyed.",
                hostWithTwoNI.hasConnections());
    }

    public DTNHost createDTNHostWithTwoNetworkInterfaces(){
        TestSettings settings = new TestSettings();

        ArrayList<NetworkInterface> networkInterfaces = new ArrayList<>();

        settings.setNameSpace("TestInterface");
        utils.addTransmitRangeAndSpeedSettings(settings);
        networkInterfaces.add(new TestInterface(settings));
        settings.restoreNameSpace();

        settings.setNameSpace("SimpleBroadcastInterface");
        utils.addTransmitRangeAndSpeedSettings(settings);
        networkInterfaces.add(new SimpleBroadcastInterface(settings));
        settings.restoreNameSpace();

        return new DTNHost(
                new ArrayList<MessageListener>(),
                new ArrayList<MovementListener>(),
                "",
                networkInterfaces,
                new ModuleCommunicationBus(),
                new StationaryMovement(new Coord(0,0)),
                makeMessageRouter());
    }

    /**
     * Dummy class for a {@link ExtendedMovementModel}. It is used in the {@link DTNHostTest#testInterruptMovement()}
     * test to switch movement models.
     */
    private static class DummyExtendedMovementModel extends ExtendedMovementModel{

        /**
         * Default constructor. Super constructor is called with {@link TestSettings} instance to set all needed
         * parameters.
         */
        DummyExtendedMovementModel(){
            super(new TestSettings());
        }

        /**
         * Set a DummyMovement with the expected path as the current model
         * @return true every time
         */
        @Override
        public boolean newOrders() {
            setCurrentMovementModel(new DummyMovement(expectedPath));
            return true;
        }

        /**
         * Returns the initial location of the movement model.
         * @return always returns (0,0).
         */
        @Override
        public Coord getInitialLocation() {
            return new Coord(0,0);
        }

        @Override
        public MovementModel replicate() {
            DummyExtendedMovementModel copy = new DummyExtendedMovementModel();
            copy.setCurrentMovementModel(getCurrentMovementModel());
            return copy;
        }
    }

    private static DTNHost createHost(MovementModel movementModel){

        return new DTNHost(
            new ArrayList<MessageListener>(),
            new ArrayList<MovementListener>(),
            "",
            new ArrayList<NetworkInterface>(),
            null,
            movementModel,
            makeMessageRouter());
    }

    private static MessageRouter makeMessageRouter() {
        return new PassiveRouter(new Settings());
    }
  //==========================================================================//


  //==========================================================================//
  // Private
  //==========================================================================//

  //==========================================================================//

}
