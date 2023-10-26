package movement;

import core.Coord;
import core.Settings;
import core.SimClock;

/**
 * @author teemuk
 */
public class ActivityPeriodRwp
extends MovementModel {

  //==========================================================================//
  // Settings
  //==========================================================================//
  public static final String ACTIVE_SETTING = "rwpActivePeriod";
  //==========================================================================//


  //==========================================================================//
  // Instance vars
  //==========================================================================//
  private final double activeStart;
  private final double activeEnd;
  private Coord lastWaypoint;
  //==========================================================================//



  //==========================================================================//
  // Implementation - activity periods
  //==========================================================================//
  @Override
  public boolean isActive() {
    final double curTime = SimClock.getTime();
    return ( curTime >= this.activeStart ) && ( curTime <= this.activeEnd );
  }

  @Override
  public double nextPathAvailable() {
    final double curTime = SimClock.getTime();
    if ( curTime < this.activeStart ) {
      return this.activeStart;
    } else if ( curTime > this.activeEnd ) {
      return Double.MAX_VALUE;
    }
    return curTime;
  }
  //==========================================================================//


  //==========================================================================//
  // Implementation - Basic RWP
  //==========================================================================//
  @Override
  public Path getPath() {
    // NOTE: The path may last beyond the end of the active period.

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
    this.lastWaypoint = randomCoord();
    return this.lastWaypoint;
  }

  @Override
  public MovementModel replicate() {
    return new ActivityPeriodRwp( this );
  }

  private Coord randomCoord() {
    return new Coord( rng.nextDouble() * super.getMaxX(),
                      rng.nextDouble() * super.getMaxY() );
  }
  //==========================================================================//


  //==========================================================================//
  // Constructors
  //==========================================================================//
  public ActivityPeriodRwp( final Settings settings ) {
    super( settings );

    // Read the activity period from the settings
    final double[] active = settings.getCsvDoubles( ACTIVE_SETTING ,2 );
    this.activeStart = active[ 0 ];
    this.activeEnd = active[ 1 ];
  }

  public ActivityPeriodRwp( final ActivityPeriodRwp other ) {
    super( other );

    // Remember to copy our own state
    this.activeStart = other.activeStart;
    this.activeEnd = other.activeEnd;
  }
  //==========================================================================//
}
