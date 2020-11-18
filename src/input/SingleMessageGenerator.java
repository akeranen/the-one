package input;

import core.Settings;

/**
 * @author teemuk
 */
public class SingleMessageGenerator
extends MessageEventGenerator {
  /**
   * Constructor, initializes the interval between events,
   * and the size of messages generated, as well as number
   * of hosts in the network.
   *
   * @param s Settings for this generator.
   */
  public SingleMessageGenerator( Settings s ) {
    super( s );
    super.nextEventsTime = 0;
  }
}
