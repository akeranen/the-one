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
import org.junit.Before;
import routing.MessageRouter;
import routing.PassiveRouter;

import java.util.ArrayList;

/**
 * @author teemuk
 */
public class DTNHostTest extends TestCase {

    private DummyExtendedMovementModel movementModel = new DummyExtendedMovementModel();
    private static Path expectedPath = new Path();

    @Before
    public void clearGroupAndResetHostAddresses(){
        DTNHost.reset();
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
        movementModel.setCurrentMovementModel(new DummyMovement(null));
        DTNHost host = createHost(movementModel);
        movementModel.newOrders();
        host.interruptMovement();
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
            return null;
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
