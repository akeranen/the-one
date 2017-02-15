package movement;

import core.Coord;
import core.Settings;
import util.LevyDistribution;

/**
 * Implements Levy Walk movement model.
 * Uses Levy Distribution by Mark N. Read to chose distance
 *
 */
public class LevyWalkMovement extends MovementModel implements SwitchableMovement {

    private static final double TWO = 2.0;
    private Coord lastWaypoint;
    //Initially set radius to half of max(maxX,maxY)
    private double radius = Math.max(getMaxX(), getMaxY())/TWO;
    //Initially set center to center of the map
    private Coord center=new Coord(getMaxX()/TWO, getMaxY()/TWO);

    public LevyWalkMovement(Settings settings){
        super(settings);
    }

    /**
     * Replicates a LevyWalkMovement
     * @param other LevyWalkMovement to replicate
     */
    private LevyWalkMovement(LevyWalkMovement other){
        super(other);
    }


    @Override
    public Path getPath() {

        Path p;
        p = new Path(generateSpeed());
        p.addWaypoint(lastWaypoint.clone());

        //Set impossible value so we notice if the actual choosing of
        // coordinates breaks
        Coord c = new Coord(-1,-1);

        boolean pointFound=false;
        while (!pointFound) {

            //Choose distance using Levy Distribution
            double distance = LevyDistribution.samplePositive(TWO, rng);

            //Get a random value from [0, 2*Pi]
            double angle = rng.nextDouble() * TWO * Math.PI;

            double x = lastWaypoint.getX() + distance * Math.cos(angle);
            double y = lastWaypoint.getY() + distance * Math.sin(angle);

            c = new Coord(x,y);

            //Is our new point not within the simulation area?
            // Then we need to change the angle again
            if (x < 0 || y < 0 || x > getMaxX() || y > getMaxY()) {
                continue;
            }
            // Is our new point and within our radius? Then we can stop searching and add it.
            if (c.distance(center)<=radius){
                pointFound = true;
            }
        }

        p.addWaypoint(c);

        this.lastWaypoint = c;
        return p;
    }

    @Override
    public Coord getInitialLocation() {
        this.lastWaypoint = center;
        return center;
    }

    @Override
    public MovementModel replicate() {
        return new LevyWalkMovement(this);
    }

    public boolean setRadius(double radius){
        double maxDistance = Math.max(getMaxX(),getMaxY());
        if (radius>0 && radius<=maxDistance){
            this.radius=radius;
            return true;
        }
        return false;
    }

    public boolean setCenter(Coord center){
        if (center.getX()>=0 && center.getX() <= getMaxX() && center.getY()>=0 && center.getY() <=getMaxY()){
            this.center=center;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void setLocation(Coord lastWaypoint) {
        this.lastWaypoint = lastWaypoint;
    }

    @Override
    public Coord getLastLocation() {
        return lastWaypoint;
    }

    @Override
    public boolean isReady() {
        return true;
    }
}
