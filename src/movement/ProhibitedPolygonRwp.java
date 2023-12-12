package movement;

import core.Coord;
import core.Settings;

import java.util.Arrays;
import java.util.List;

/**
 * Random Waypoint Movement with a prohibited region where nodes may not move
 * into. The polygon is defined by a *closed* (same point as first and
 * last) path, represented as a list of {@code Coord}s.
 *
 * @author teemuk
 */
public class ProhibitedPolygonRwp
extends MovementModel {

  //==========================================================================//
  // Settings
  //==========================================================================//
  /** {@code true} to confine nodes inside the polygon */
  public static final String INVERT_SETTING = "rwpInvert";
  public static final boolean INVERT_DEFAULT = false;
  //==========================================================================//


  //==========================================================================//
  // Instance vars
  //==========================================================================//
  final List <Coord> polygon = Arrays.asList(
    new Coord(67.3,129.8),
    new Coord(67.1,128.6),
    new Coord(63.9,128.2),
    new Coord(63.8,129.8),
    new Coord(48.9,130),
    new Coord(49.2,180.2),
    new Coord(95.7,180.5),
    new Coord(95.7,181.8),
    new Coord(99.1,181.3),
    new Coord(99.2,180.3),
    new Coord(108,180.2),
    new Coord(108.2,137.4),
    new Coord(117.3,138.4),
    new Coord(117.4,138.5),
    new Coord(116.9,142),
    new Coord(118,142.3),
    new Coord(118.5,140.6),
    new Coord(122.8,141),
    new Coord(122.7,142.8),
    new Coord(123.6,142.8),
    new Coord(124.1,139),
    new Coord(132.3,139.8),
    new Coord(125.5,214.6),
    new Coord(136,215.7),
    new Coord(136.3,217.8),
    new Coord(142.9,218.2),
    new Coord(143.1,216.3),
    new Coord(145.1,216.1),
    new Coord(150.7,156.6),
    new Coord(152.9,155.4),
    new Coord(152.3,164.3),
    new Coord(152.6,164.6),
    new Coord(159.3,165.2),
    new Coord(169.8,166.3),
    new Coord(177.3,166.8),
    new Coord(178.1,157.7),
    new Coord(178.2,157.7),
    new Coord(180,159.5),
    new Coord(174.6,219.2),
    new Coord(185.5,220.5),
    new Coord(185.5,222.5),
    new Coord(192.1,223),
    new Coord(192.3,221),
    new Coord(194.1,220.9),
    new Coord(199.8,162.8),
    new Coord(219,164.3),
    new Coord(219.4,155),
    new Coord(221.9,155.2),
    new Coord(221.9,156.4),
    new Coord(225,156.6),
    new Coord(225.4,155.5),
    new Coord(226.8,155.6),
    new Coord(226.8,182.9),
    new Coord(224.1,223.7),
    new Coord(234.7,225.1),
    new Coord(234.8,226.9),
    new Coord(241.4,227.6),
    new Coord(241.7,225.6),
    new Coord(243.4,225.5),
    new Coord(250,159.8),
    new Coord(253.8,160.2),
    new Coord(253.4,165),
    new Coord(253.9,166.1),
    new Coord(255,166.1),
    new Coord(275.4,162.2),
    new Coord(279.3,162.5),
    new Coord(273.1,228.3),
    new Coord(283.9,229.6),
    new Coord(284,231.7),
    new Coord(290.7,232.1),
    new Coord(290.7,230.2),
    new Coord(292.9,230.2),
    new Coord(298.3,172.2),
    new Coord(305.6,172.8),
    new Coord(324.7,166.9),
    new Coord(325.5,197.8),
    new Coord(322.6,233.3),
    new Coord(333.2,234.2),
    new Coord(333.2,236.3),
    new Coord(338.2,236.5),
    new Coord(338.3,234.6),
    new Coord(342.1,234.7),
    new Coord(347.5,178.2),
    new Coord(349.3,178.2),
    new Coord(349.8,170.5),
    new Coord(348.3,170.4),
    new Coord(349.5,153.7),
    new Coord(332.3,151.9),
    new Coord(333,145.4),
    new Coord(358.9,113.9),
    new Coord(355.1,103.3),
    new Coord(310.7,113.2),
    new Coord(298.2,113.2),
    new Coord(298,113.1),
    new Coord(298.3,107.2),
    new Coord(297.5,107.1),
    new Coord(297,109.8),
    new Coord(289.5,109.3),
    new Coord(289.6,106.3),
    new Coord(288.7,106.3),
    new Coord(287.8,113.1),
    new Coord(276.3,113.1),
    new Coord(276.4,102.5),
    new Coord(277.7,102.3),
    new Coord(277.7,101.8),
    new Coord(276.3,100.1),
    new Coord(275.8,26.2),
    new Coord(273.8,26.1),
    new Coord(273.8,24.2),
    new Coord(267.3,24.3),
    new Coord(267.2,26.2),
    new Coord(256.4,26.2),
    new Coord(256.2,84.8),
    new Coord(236.7,84.9),
    new Coord(236.7,84.8),
    new Coord(236.7,81.4),
    new Coord(234.1,81.3),
    new Coord(233.5,85),
    new Coord(222.8,85),
    new Coord(222.7,26.4),
    new Coord(220.8,26.2),
    new Coord(220.7,24.1),
    new Coord(214,24.2),
    new Coord(213.8,26.3),
    new Coord(202.8,26.6),
    new Coord(202.7,96.5),
    new Coord(186.4,96.4),
    new Coord(186.5,93.2),
    new Coord(183.7,93),
    new Coord(183,96.5),
    new Coord(169.4,96.4),
    new Coord(169.2,26.6),
    new Coord(167.4,26.1),
    new Coord(167.1,24.2),
    new Coord(160.5,24.1),
    new Coord(160.3,26.2),
    new Coord(149.6,26.3),
    new Coord(149.2,96.7),
    new Coord(130,96.4),
    new Coord(130.1,93.1),
    new Coord(127.4,92.8),
    new Coord(126.8,96.6),
    new Coord(116,96.6),
    new Coord(115.9,26.3),
    new Coord(114,26.2),
    new Coord(113.8,24),
    new Coord(107.1,24.3),
    new Coord(107,26.3),
    new Coord(96,26.6),
    new Coord(96,93.7),
    new Coord(94,93.7),
    new Coord(93.9,90.2),
    new Coord(90.9,90.3),
    new Coord(90.8,93.8),
    new Coord(67.8,93.6),
    new Coord(66.1,90.4),
    new Coord(64.8,90.3),
    new Coord(64.6,93.7),
    new Coord(62.7,93.6),
    new Coord(62.5,26.3),
    new Coord(60.7,26.2),
    new Coord(60.5,24),
    new Coord(53.9,24.1),
    new Coord(53.7,26.1),
    new Coord(42.9,26.3),
    new Coord(42.7,96),
    new Coord(40.8,96),
    new Coord(41,102.7),
    new Coord(42.8,102.8),
    new Coord(42.9,119.2),
    new Coord(43.2,119.5),
    new Coord(84,119.6),
    new Coord(83.8,120.8),
    new Coord(80.2,120.9),
    new Coord(80.2,121.8),
    new Coord(82,122),
    new Coord(81.9,126.4),
    new Coord(80.2,126.5),
    new Coord(80.1,127.3),
    new Coord(83.9,127.7),
    new Coord(83.8,129.8),
    new Coord(67.3,129.8)
  );

  private Coord lastWaypoint;
  /** Inverted, i.e., only allow nodes to move inside the polygon. */
  private final boolean invert;
  //==========================================================================//



  //==========================================================================//
  // Implementation
  //==========================================================================//
  @Override
  public Path getPath() {
    // Creates a new path from the previous waypoint to a new one.
    final Path p;
    p = new Path( super.generateSpeed() );
    p.addWaypoint( this.lastWaypoint.clone() );

    // Add only one point. An arbitrary number of Coords could be added to
    // the path here and the simulator will follow the full path before
    // asking for the next one.
    Coord c;
    do {
      c = this.randomCoord();
    } while ( pathIntersects( this.polygon, this.lastWaypoint, c ) );
    p.addWaypoint( c );

    this.lastWaypoint = c;
    return p;
  }

  @Override
  public Coord getInitialLocation() {
    do {
      this.lastWaypoint = this.randomCoord();
    } while ( ( this.invert ) ?
        isOutside( polygon, this.lastWaypoint ) :
        isInside( this.polygon, this.lastWaypoint ) );
    return this.lastWaypoint;
  }

  @Override
  public MovementModel replicate() {
    return new ProhibitedPolygonRwp( this );
  }

  private Coord randomCoord() {
    return new Coord(
        rng.nextDouble() * super.getMaxX(),
        rng.nextDouble() * super.getMaxY() );
  }
  //==========================================================================//


  //==========================================================================//
  // API
  //==========================================================================//
  public ProhibitedPolygonRwp( final Settings settings ) {
    super( settings );
    // Read the invert setting
    this.invert = settings.getBoolean( INVERT_SETTING, INVERT_DEFAULT );
  }

  public ProhibitedPolygonRwp( final ProhibitedPolygonRwp other ) {
    // Copy constructor will be used when settings up nodes. Only one
    // prototype node instance in a group is created using the Settings
    // passing constructor, the rest are replicated from the prototype.
    super( other );
    // Remember to copy any state defined in this class.
    this.invert = other.invert;
  }
  //==========================================================================//


  //==========================================================================//
  // Private - geometry
  //==========================================================================//
  private static boolean pathIntersects(
      final List <Coord> polygon,
      final Coord start,
      final Coord end ) {
    final int count = countIntersectedEdges( polygon, start, end );
    return ( count > 0 );
  }

  private static boolean isInside(
      final List <Coord> polygon,
      final Coord point ) {
    final int count = countIntersectedEdges( polygon, point,
        new Coord( -10,0 ) );
    return ( ( count % 2 ) != 0 );
  }

  private static boolean isOutside(
      final List <Coord> polygon,
      final Coord point ) {
    return !isInside( polygon, point );
  }

  private static int countIntersectedEdges(
      final List <Coord> polygon,
      final Coord start,
      final Coord end ) {
    int count = 0;
    for ( int i = 0; i < polygon.size() - 1; i++ ) {
      final Coord polyP1 = polygon.get( i );
      final Coord polyP2 = polygon.get( i + 1 );

      final Coord intersection = intersection( start, end, polyP1, polyP2 );
      if ( intersection == null ) continue;

      if ( isOnSegment( polyP1, polyP2, intersection )
            && isOnSegment( start, end, intersection ) ) {
        count++;
      }
    }
    return count;
  }

  private static boolean isOnSegment(
      final Coord L0,
      final Coord L1,
      final Coord point ) {
    final double crossProduct
        = ( point.getY() - L0.getY() ) * ( L1.getX() - L0.getX() )
        - ( point.getX() - L0.getX() ) * ( L1.getY() - L0.getY() );
    if ( Math.abs( crossProduct ) > 0.0000001 ) return false;

    final double dotProduct
        = ( point.getX() - L0.getX() ) * ( L1.getX() - L0.getX() )
        + ( point.getY() - L0.getY() ) * ( L1.getY() - L0.getY() );
    if ( dotProduct < 0 ) return false;

    final double squaredLength
        = ( L1.getX() - L0.getX() ) * ( L1.getX() - L0.getX() )
        + (L1.getY() - L0.getY() ) * (L1.getY() - L0.getY() );
    if ( dotProduct > squaredLength ) return false;

    return true;
  }

  private static Coord intersection(
      final Coord L0_p0,
      final Coord L0_p1,
      final Coord L1_p0,
      final Coord L1_p1 ) {
    final double[] p0 = getParams( L0_p0, L0_p1 );
    final double[] p1 = getParams( L1_p0, L1_p1 );
    final double D = p0[ 1 ] * p1[ 0 ] - p0[ 0 ] * p1[ 1 ];
    if ( D == 0.0 ) return null;

    final double x = ( p0[ 2 ] * p1[ 1 ] - p0[ 1 ] * p1[ 2 ] ) / D;
    final double y = ( p0[ 2 ] * p1[ 0 ] - p0[ 0 ] * p1[ 2 ] ) / D;

    return new Coord( x, y );
  }

  private static double[] getParams(
      final Coord c0,
      final Coord c1 ) {
    final double A = c0.getY() - c1.getY();
    final double B = c0.getX() - c1.getX();
    final double C = c0.getX() * c1.getY() - c0.getY() * c1.getX();
    return new double[] { A, B, C };
  }
  //==========================================================================//
}
