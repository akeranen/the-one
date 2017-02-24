package input;

import core.World;
import movement.VoluntaryHelperMovement;

/**
 * This is an event class that will be used for ending VHMEvents by
 * the {@link VHMEventReader}
 *
 * Created by Marius Meyer on 15.02.17.
 */
public class VHMEventEndEvent extends VHMEvent {

    /**
     * Creates a new event for the end of a specified VHMEvent
     * @param event the VHMEvent
     */
    public VHMEventEndEvent(VHMEvent event){
        super(event);
        setTime(event.getEndTime());
    }

    @Override
    public void processEvent(World world){
        VoluntaryHelperMovement.eventEnded(this);
    }
}
