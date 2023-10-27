package report;

import core.Coord;
import core.DTNHost;
import core.MovementListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Records flight lengths observed in the simulation.
 *
 * @author teemuk
 */
public class FlightLengthReport
extends Report
implements MovementListener {

  final List <Double> lengths = new ArrayList <>( 10000 );
  final Map <DTNHost, Coord> previousPositions = new HashMap<>( 100 );

  @Override
  public void newDestination(
      final DTNHost host,
      final Coord destination,
      final double speed ) {
    final Coord previous = this.previousPositions.get( host );
    this.previousPositions.put( host, destination );

    // Could also read the location from the host.
    if ( previous == null ) {
      core.Debug.p( "No previous position found!" );
      return;
    }

    final double distance = previous.distance( destination );
    this.lengths.add( distance );
  }

  @Override
  public void initialLocation(
      final DTNHost host,
      final Coord location ) {
    // XXX: This is bugged and doesn't actually get called.
    this.previousPositions.put( host, location );
  }

  @Override
  public void done() {
    super.write( "Mean: " + super.getAverage( this.lengths ) );
    super.write( "Variance: " + super.getVariance( this.lengths ) );

    super.done(); // Closes the report file
  }
}
