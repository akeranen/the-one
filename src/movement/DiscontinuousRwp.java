package movement;

import core.Coord;
import core.Settings;

/**
 * @author teemuk
 */
public class DiscontinuousRwp
extends MovementModel {

  //==========================================================================//
  // Implementation
  //==========================================================================//
  @Override
  public Path getPath() {
    // Instead of continuing from the current point, we pick two random points
    final Path p;
    p = new Path( super.generateSpeed() );

    final Coord c1 = this.randomCoord();
    final Coord c2 = this.randomCoord();

    p.addWaypoint( c1 );
    p.addWaypoint( c2 );

    // We must move the node to the start position explicitly.
    // Otherwise the simulator moves it continuously from the previous point.
    super.getHost().setLocation( c1 );

    return p;
  }

  @Override
  public Coord getInitialLocation() {
    return this.randomCoord();
  }

  @Override
  public MovementModel replicate() {
    return new DiscontinuousRwp( this );
  }

  private Coord randomCoord() {
    return new Coord( rng.nextDouble() * super.getMaxX(),
        rng.nextDouble() * super.getMaxY() );
  }
  //==========================================================================//


  //==========================================================================//
  // Construction
  //==========================================================================//
  public DiscontinuousRwp( final Settings settings ) {
    super( settings );
  }

  public DiscontinuousRwp( final DiscontinuousRwp other ) {
    super( other );
  }
  //==========================================================================//

}
