package input;

import core.World;
import movement.VoluntaryHelperMovement;

/**
 * This is an event class that will be used for starting VHMEvents by
 * the {@link VHMEventReader}
 *
 * Created by Marius Meyer on 15.02.17.
 */
public class VHMEventStartEvent extends VHMEvent {


    /**
     * Creates a new event for the start of a specified VHMEvent
     *
     * @param event the VHMEvent
     */
    public VHMEventStartEvent(VHMEvent event){
        super(event);
        setTime(event.getStartTime());
    }

    @Override
    public void processEvent(World world){
        VoluntaryHelperMovement.eventStarted(this);
    }


}
