package input;

import core.World;
import movement.VoluntaryHelperMovement;

/**
 * This is an event class that will be used for ending VHMEvents by
 * the {@link VHMEventReader}
 *
 * Created by Marius Meyer on 15.02.17.
 */
public class VhmEventEndEvent extends VhmEvent {

    /**
     * Creates a new event for the end of a specified VhmEvent
     * @param event the VhmEvent
     */
    public VhmEventEndEvent(VhmEvent event){
        super(event);
        setTime(event.getEndTime());
    }

    @Override
    public void processEvent(World world){
        VoluntaryHelperMovement.eventEnded(this);
    }
}
