package input;

import core.World;
import movement.VoluntaryHelperMovement;

/**
 * Created by Marius Meyer on 15.02.17.
 */
public class VHMEventStartEvent extends VHMEvent {


    public VHMEventStartEvent(VHMEvent event){
        super(event);
    }

    @Override
    public void processEvent(World world){
        VoluntaryHelperMovement.eventStarted(this);
    }


}
