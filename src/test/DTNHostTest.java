package test;


import core.Coord;
import core.DTNHost;
import core.MessageListener;
import core.MovementListener;
import core.NetworkInterface;
import core.Settings;
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

    private static Path expectedPath = new Path();

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
