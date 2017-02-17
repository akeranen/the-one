package input;

import core.World;
import movement.VoluntaryHelperMovement;

/**
 * This is an event class that will be used for starting VHMEvents by
 * the VHMEventGenerator
 *
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
