package input;

import core.World;
import movement.VoluntaryHelperMovement;

/**
 * This is an event class that will be used for ending VHMEvents by
 * the VHMEventGenerator
 *
 * Created by Marius Meyer on 15.02.17.
 */
public class VHMEventEndEvent extends VHMEvent {

    public VHMEventEndEvent(VHMEvent event){
        super(event);
        setTime(event.getEndTime());
    }

    @Override
    public void processEvent(World world){
        System.out.println("Event ended: "+this.getIdentifier());
        VoluntaryHelperMovement.eventEnded(this);
    }
}
