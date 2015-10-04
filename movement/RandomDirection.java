package movement;

import core.Coord;
import core.Settings;

/**
 * <p>
 * Random Direction movement model as described in:
 * Elizabeth M. Royer, P. Michael Melliar-Smith, and Louise E. Moser,
 * "An Analysis of the Optimum Node Density for Ad hoc Mobile Networks"
 * </p>
 *
 * <p>
 * Nodes will start at a random place on the simulation area and pick a random
 * direction and follow it to the edge of the simulation area. They will then
 * pause and pick another direction to go in until they hit the edge again.
 * </p>
 *
 * @author teemuk
 */
public class RandomDirection
extends MovementModel {

    private Coord lastWaypoint;

    //========================================================================//
    // MovementModel implementation
    //========================================================================//
    public RandomDirection( Settings settings ) {
        super( settings );
    }

    public RandomDirection( RandomDirection other ) {
        super( other );
    }

    @Override
    public Path getPath() {
        Path p;
        p = new Path( super.generateSpeed() );
        p.addWaypoint( this.lastWaypoint.clone() );
        Coord next = this.getRandomWaypoint( this.lastWaypoint.getX(),
                                             this.lastWaypoint.getY() );
        p.addWaypoint( next );

        this.lastWaypoint = next;

        return p;
    }

    @Override
    public Coord getInitialLocation() {
        Coord c
            = new Coord( MovementModel.rng.nextDouble() * super.getMaxX(),
                         MovementModel.rng.nextDouble() * super.getMaxY() );
        this.lastWaypoint = c;

        return c;
    }

    @Override
    public MovementModel replicate() {
        return new RandomDirection( this );
    }
    //========================================================================//


    //========================================================================//
    // Sub-classable
    //========================================================================//
    /**
     * Returns the fraction of the path to follow towards the edge. This is
     * called after the direction of travel has been picked and the path
     * towards the edge calculated. Returning {@code 1.0} causes the node to
     * travel all the way to the edge, returning {@code 0.5} would cause the
     * node to travel half way, etc.
     *
     * @return  the fraction of the path to follow towards the edge of the
     *          simulation area
     */
    protected double getTravelFraction() {
        return 1.0;
    }
    //========================================================================//



    //========================================================================//
    // Calculations for selecting the next waypoint.
    //------------------------------------------------------------------------//
    // Solves the intersection of the path defined by:
    //   (x,y) = (x0,y0) + tp * (cos(angle), sin(angle))
    // And the simulation area bounds
    //   (x,y) = t1 * (1,0)
    //   (x,y) = t2 * (0,1)
    //   (x,y) = (0,areaHeight) + t3 * (1,0)
    //   (x,y) = (areaWidth,0) + t4 * (0,1)
    //========================================================================//
    private Coord getRandomWaypoint( double x0, double y0 ) {
        double[] params = null;
        double angle;
        boolean done;

        // 1. Pick a random direction
        // 2. Solve the intersection of the path with the bounds
        // 3. Pick the right solution
        // 4. Calculate the coordinate
        do {
            done = true;

            angle = MovementModel.rng.nextDouble() * 2 * Math.PI - Math.PI;

            double[] bottomParams
                    = this.getBottomParams( x0, y0, angle );
            double[] leftParams
                    = this.getLeftParams( x0, y0, angle );
            double[] topParams
                    = this.getTopParams( x0, y0, angle, super.getMaxY() );
            double[] rightParams
                    = this.getRightParams( x0, y0, angle, super.getMaxY() );

            if ( this.hitTestBottom( bottomParams, super.getMaxX() ) ) {
                params = bottomParams;
            } else if ( this.hitTestLeft( leftParams, super.getMaxY() ) ) {
                params = leftParams;
            } else if ( this.hitTestTop( topParams, super.getMaxX() ) ) {
                params = topParams;
            } else if ( this.hitTestRight( rightParams, super.getMaxY() ) ) {
                params = rightParams;
            } else {
                // Hit test can fail if we are already on the edge and picked
                // direction outwards
                done = false;
            }
        } while ( !done );

        double t = this.getTravelFraction() * params[ 1 ];

        double x = x0 + t * Math.cos( angle );
        double y = y0 + t * Math.sin( angle );

        return new Coord( x, y );
    }

    private double[] getBottomParams( final double x0, final double y0,
                                      final double angle ) {
        double t1 = x0 - y0 * Math.cos( angle ) / Math.sin( angle );
        double tp = -1.0 * y0 / Math.sin( angle );

        double[] ret = new double[ 2 ];
        ret[ 0 ] = t1;
        ret[ 1 ] = tp;

        return ret;
    }

    private double[] getLeftParams( final double x0, final double y0,
                                    final double angle ) {
        double t2 = y0 - x0 * Math.sin( angle ) / Math.cos( angle );
        double tp = -1.0 * x0 / Math.cos( angle );

        double[] ret = new double[ 2 ];
        ret[ 0 ] = t2;
        ret[ 1 ] = tp;

        return ret;
    }

    private double[] getTopParams( final double x0, final double y0,
                                   final double angle,
                                   final double areaHeight ) {
        double t3 = x0 + ( areaHeight - y0 ) *
                         Math.cos( angle ) / Math.sin( angle );
        double tp = ( areaHeight - y0 ) / Math.sin( angle );

        double[] ret = new double[ 2 ];
        ret[ 0 ] = t3;
        ret[ 1 ] = tp;

        return ret;
    }

    private double[] getRightParams( final double x0, final double y0,
                                     final double angle,
                                     final double areaWidth ) {
        double t4 = y0 + ( areaWidth - x0 ) *
                         Math.sin( angle ) / Math.cos( angle );
        double tp = ( areaWidth - x0 ) / Math.cos( angle );

        double[] ret = new double[ 2 ];
        ret[ 0 ] = t4;
        ret[ 1 ] = tp;

        return ret;
    }

    private boolean hitTestBottom( final double[] t,
                                   final double areaWidth ) {
        return ( 0 <= t[ 0 ] && t[ 0 ] <= areaWidth ) &&
               ( t[ 1 ] > 0.0 );
    }

    private boolean hitTestLeft( final double[] t,
                                 final double areaHeight ) {
        return ( 0 <= t[ 0 ] && t[ 0 ] <= areaHeight ) &&
               ( t[ 1 ] > 0.0 );
    }

    private boolean hitTestTop( final double[] t, final double areaWidth ) {
        return ( 0 <= t[ 0 ] && t[ 0 ] <= areaWidth ) &&
               ( t[ 1 ] > 0.0 );
    }

    private boolean hitTestRight( final double[] t, double areaHeight ) {
        return ( 0 <= t[ 0 ] && t[ 0 ] <= areaHeight ) &&
               ( t[ 1 ] > 0 );
    }
    //========================================================================//
}
