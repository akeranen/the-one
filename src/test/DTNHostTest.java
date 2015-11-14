package test;


import core.Coord;
import core.DTNHost;
import core.Settings;
import movement.MovementModel;
import movement.Path;
import org.junit.Test;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import routing.MessageRouter;
import routing.PassiveRouter;

import java.util.Collections;

import static org.junit.Assert.*;

/**
 * @author teemuk
 */
public class DTNHostTest {

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
    final DTNHost host = new DTNHost(
            Collections.emptyList(),
            Collections.emptyList(),
            "",
            Collections.emptyList(),
            null,
            makeMovementModel(),
            makeMessageRouter());

    // Tests
    assertFalse("Radio reported as active.", host.isRadioActive());
  }

  private static MovementModel makeMovementModel() {
    return new MovementModel() {
      @Override
      public Path getPath() {
        return null;
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
