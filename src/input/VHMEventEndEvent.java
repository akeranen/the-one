package input;

import core.World;
import movement.VoluntaryHelperMovement;

/**
 * Created by mellich on 15.02.17.
 */
public class VHMEventEndEvent extends VHMEvent {

    public VHMEventEndEvent(VHMEvent event){
        super(event);
    }

    @Override
    public void processEvent(World world){
        VoluntaryHelperMovement.eventEnded(this);
    }
}
