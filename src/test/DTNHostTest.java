package test;


import core.Coord;
import core.DTNHost;
import core.Group;
import core.MessageListener;
import core.MovementListener;
import core.NetworkInterface;
import core.Settings;
import junit.framework.TestCase;
import movement.MovementModel;
import movement.Path;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import routing.MessageRouter;
import routing.PassiveRouter;

import java.util.ArrayList;

/**
 * @author teemuk
 */
public class DTNHostTest extends TestCase {

  @Before
  public void clearGroupAndResetHostAddresses(){
    Group.clearGroups();
    DTNHost.reset();
  }

  //==========================================================================//
  // Setup/cleanup
  //==========================================================================//
  @BeforeClass
  public static void setUpBeforeClass()
      throws Exception {

  }

  @AfterClass
  public static void tearDownAfterClass()
      throws Exception {

  }
  //==========================================================================//


  //==========================================================================//
  // Tests
  //==========================================================================//
  /**
   * Tests the case where the DTNHost has no interfaces configured.
   *
   * @throws Exception
   */
  @Test
  public void testNoInterfaces()
  throws Exception {
    final DTNHost host = createHost();

    // Tests
    assertFalse("Radio reported as active.", host.isRadioActive());
  }

    public void testInterruptMovement(){
        DTNHost host = createHost();
        host.interruptMovement();
        assertNull("Path should be null",host.getPath());
    }

  private static DTNHost createHost(){
    return new DTNHost(
            new ArrayList<MessageListener>(),
            new ArrayList<MovementListener>(),
            "",
            new ArrayList<NetworkInterface>(),
            null,
            makeMovementModel(),
            makeMessageRouter());
  }

  private static MovementModel makeMovementModel() {
    return new MovementModel() {
        @Override
        public Path getPath() {
            return new Path();
        }

      @Override
      public Coord getInitialLocation() {
        return null;
      }

      @Override
      public MovementModel replicate() {
        return makeMovementModel();
      }
    };
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
