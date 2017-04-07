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

    public void testInterruptMovement(){
        DummyExtendedMovementModel movementModel = new DummyExtendedMovementModel();
        movementModel.setCurrentMovementModel(new DummyMovement(null));
        DTNHost host = createHost(movementModel);
        //switch internal movement model
        ((ExtendedMovementModel)host.getMovementModel()).getPath();
        //call interrupt on host
        host.interruptMovement();
        //new movement model should be selected and used
        host.move(0);
        assertEquals("Host should switch movement model and return expected path",
                expectedPath,host.getPath());
    }

    private static class DummyExtendedMovementModel extends ExtendedMovementModel{

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
