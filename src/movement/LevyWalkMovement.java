package movement;

import core.Coord;
import core.Settings;
import util.LevyDistribution;

/**
 * Implements Levy Walk movement model.
 * Uses Levy Distribution by Mark N. Read to chose distance
 *
 */
public class LevyWalkMovement extends MovementModel {

    private Coord lastWaypoint;
    //Set max Path Length to the min(maxX,maxY)
    private double maxPathLength=(getMaxX()>getMaxY()) ? getMaxY() : getMaxX();

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
        double maxX = getMaxX();
        double maxY = getMaxY();

        Coord c;
        while (true) {

            //Get a random value from [0, 2*Pi]
            double angle = rng.nextDouble() * 2 * Math.PI;

            //Choose distance using Levy Distribution
            double distance = LevyDistribution.sample_positive(2, maxPathLength);

            double x = lastWaypoint.getX() + distance * Math.cos(angle);
            double y = lastWaypoint.getY() + distance * Math.sin(angle);

            c = new Coord(x,y);

            if (x > 0 && y > 0 && x < maxX && y < maxY) {
                break;
            }
        }

        p.addWaypoint(c);

        this.lastWaypoint = c;
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
        return new LevyWalkMovement(this);
    }

}
