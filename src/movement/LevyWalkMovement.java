package movement;

import core.Coord;
import core.Settings;
import util.LevyDistribution;

/**
 * Implements Levy Walk movement model.
 *
 * Levy Walk is a random movement model. It is based on the Levy distribution, a heavy-tailed distribution.
 * The direction is chosen at random and the step lengths are chosen based on the Levy distribution.
 * Speed is not mentioned in the original concept. In our simulations, speed is chosen at random from a preset range.
 *
 * This class uses the Levy Distribution class by Mark N. Read to chose the distance
 *
 * @author Melanie Bruns
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

    /**
     * Set the radius in which a node is able to move around the center point.
     * The radius may not be higher than the maximum distance between the opposing
     * simulation bounds and it may not be zero or smaller.
     * @param radius The radius in which a node should be allowed to move around the center
     * @return A boolean indicating whether the radius has successfully been set
     */
    public boolean setRadius(double radius){
        double maxDistance = Math.max(getMaxX(),getMaxY());
        if (radius>0 && radius<=maxDistance){
            this.radius=radius;
            return true;
        }
        return false;
    }

    /**
     * Set the center around which the node can move within a set radius.
     * The center has to lie within the simulation bounds
     * @param center A coordinate around which the node may move within the radius
     * @return A boolean indicating whether the center has successfully been set
     */
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
