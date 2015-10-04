package movement;

import core.Settings;

/**
 * <p>
 * Modified Random Direction movement model as described in:
 * Elizabeth M. Royer, P. Michael Melliar-Smith, and Louise E. Moser,
 * "An Analysis of the Optimum Node Density for Ad hoc Mobile Networks"
 * </p>
 *
 * <p>
 * Similar to {@link RandomDirection}, except nodes will not move all the way
 * to the edge. Instead they will pick a random direction and move in that
 * direction for a random distance before pausing and picking another
 * direction.
 * </p>
 *
 * @author teemuk
 */
public class ModifiedRandomDirection
extends RandomDirection {

    public ModifiedRandomDirection( Settings settings ) {
        super( settings );
    }

    public ModifiedRandomDirection( ModifiedRandomDirection other ) {
        super( other );
    }

    @Override
    protected double getTravelFraction() {
        // Move a random fraction in the picked direction instead of all the
        // way to the edge.
        return MovementModel.rng.nextDouble();
    }

    @Override
    public MovementModel replicate() {
        return new ModifiedRandomDirection( this );
    }

}
