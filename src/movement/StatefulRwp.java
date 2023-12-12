package movement;

import core.Coord;
import core.Settings;

/**
 * Example of a state-machine driven node mobility. Each node has two states
 * LEFT and RIGHT that influence the picking of the next waypoint. Nodes
 * transition between the states with some probability defined by the state
 * transition diagram.
 *
 * @author teemuk
 */
public class StatefulRwp
extends MovementModel {

  //==========================================================================//
  // Instance vars
  //==========================================================================//
  private Coord lastWaypoint;

  private State state;
  //==========================================================================//


  //==========================================================================//
  // Implementation
  //==========================================================================//
  @Override
  public Path getPath() {
    // Update state machine every time we pick a path
    this.state = this.updateState( this.state );

    // Create the path
    final Path p;
    p = new Path( generateSpeed() );
    p.addWaypoint( lastWaypoint.clone() );

    final Coord c = this.randomCoord();
    p.addWaypoint( c );

    this.lastWaypoint = c;
    return p;
  }

  @Override
  public Coord getInitialLocation() {
    this.lastWaypoint = this.randomCoord();
    return this.lastWaypoint;
  }

  @Override
  public MovementModel replicate() {
    return new StatefulRwp( this );
  }

  private Coord randomCoord() {
    final double x;
    if ( this.state == State.LEFT ) {
      x = rng.nextDouble() * super.getMaxX() / 2;
    } else {
      x = ( rng.nextDouble() + 1 ) * super.getMaxX() / 2;
    }
    return new Coord( x, rng.nextDouble() * super.getMaxY());
  }
  //==========================================================================//


  //==========================================================================//
  // Construction
  //==========================================================================//
  public StatefulRwp( final Settings settings ) {
    super( settings );
    this.state = this.getRandomState();
  }

  public StatefulRwp( final StatefulRwp other ) {
    super( other );

    // Pick a random state every time we replicate rather than copying!
    // Otherwise every node would start in the same state.
    this.state = this.getRandomState();
  }
  //==========================================================================//


  //==========================================================================//
  // State
  //==========================================================================//
  private State getRandomState() {
    if ( rng.nextDouble() < 0.05 ) {
      return State.LEFT;
    } else {
      return State.RIGHT;
    }
  }

  /**
   * This method defines the transitions in the state machine.
   *
   * @param state
   *  the current state
   * @return
   *  the next state
   */
  private State updateState( final State state ) {
    switch ( state ) {
      case LEFT: {
        final double r = rng.nextDouble();
        return ( r < 0.05 ) ? ( State.LEFT ) : ( State.RIGHT );
      }
      case RIGHT: {
        final double r = rng.nextDouble();
        return ( r < 0.05 ) ? ( State.LEFT ) : ( State.RIGHT );
      }
      default: {
        throw new RuntimeException( "Invalid state." );
      }
    }
  }

  private static enum State {
    LEFT, RIGHT
  }
  //==========================================================================//
}
