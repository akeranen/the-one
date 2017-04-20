package test;

import core.Coord;
import movement.MovementModel;
import movement.Path;
import movement.SwitchableMovement;

/**
 * A dummy movement model with minimal functionality for test purposes.
 * Returns a specified path, when getPath() is called
 *
 * Created by Marius Meyer on 07.04.17.
 */
public class DummyMovement extends MovementModel implements SwitchableMovement {

    /**
     *the path that should be returned
     */
    private Path returnedPath;

    /**
     * Constructor for DummyMovement. A path may be specified
     *
     * @param returnPath the path that should be returned by the movement model
     */
    public DummyMovement(Path returnPath){
        super(new TestSettings());
        this.returnedPath = returnPath;
    }

    @Override
    public Path getPath() {
        return returnedPath;
    }

    @Override
    public Coord getInitialLocation() {
        return null;
    }

    @Override
    public MovementModel replicate() {
        return new DummyMovement(returnedPath);
    }

    @Override
    public void setLocation(Coord lastWaypoint) {
        //only dummy class. Functionality for this method not necessary
    }

    @Override
    public Coord getLastLocation() {
        return null;
    }

    @Override
    public boolean isReady() {
        return true;
    }
}
