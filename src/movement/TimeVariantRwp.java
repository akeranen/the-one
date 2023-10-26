package movement;

import core.Coord;
import core.Settings;
import core.SimClock;
import core.SimScenario;

/**
 * Example of time-variant behavior. Random waypoint, but picks the waypoints
 * from different regions based on the current simulation time.
 *
 * @author teemuk
 */
public class TimeVariantRwp
extends MovementModel {

  //==========================================================================//
  // Instance vars
  //==========================================================================//
  private Coord lastWaypoint;
  //==========================================================================//



  //==========================================================================//
  // Implementation
  //==========================================================================//
  @Override
  public Coord getInitialLocation() {
    this.lastWaypoint = new Coord(
        rng.nextDouble() * super.getMaxX() / 2,
        rng.nextDouble() * super.getMaxY() / 2);
    return this.lastWaypoint;
  }

  @Override
  public Path getPath() {
    // Creates a new path from the previous waypoint to a new one.
    final Path p;
    p = new Path( super.generateSpeed() );
    p.addWaypoint( this.lastWaypoint.clone() );

    // Add only one point. An arbitrary number of Coords could be added to
    // the path here and the simulator will follow the full path before
    // asking for the next one.
    final Coord c = this.randomCoord();
    p.addWaypoint( c );

    this.lastWaypoint = c;
    return p;
  }

  protected Coord randomCoord() {
    // Get the simulation time
    final double curTime = SimClock.getTime();
    // NOTE: Cannot be called from getInitialLocation()!
    final double endTime = SimScenario.getInstance().getEndTime();
    final double t = curTime / endTime;

    // Set the bounds based on the time
    final double k = Math.sin( t * Math.PI );
    final double hx = super.getMaxX() / 2;
    final double hy = super.getMaxY() / 2;
    return new Coord(
        k * hx + ( rng.nextDouble() * hx ),
        k * hy + ( rng.nextDouble() * hy ) );
  }
  //==========================================================================//


  //==========================================================================//
  // Construction
  //==========================================================================//
  public TimeVariantRwp( final Settings settings ) {
    super( settings );
  }


  public TimeVariantRwp( final TimeVariantRwp other ) {
    super( other );
  }

  @Override
  public MovementModel replicate() {
    return new TimeVariantRwp( this );
  }
  //==========================================================================//
}
