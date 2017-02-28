package input;

import core.World;
import movement.VoluntaryHelperMovement;

/**
 * This is an event class that will be used for starting VHMEvents by
 * the {@link VHMEventReader}
 *
 * Created by Marius Meyer on 15.02.17.
 */
public class VhmEventStartEvent extends VhmEvent {


    /**
     * Creates a new event for the start of a specified VhmEvent
     *
     * @param event the VhmEvent
     */
    public VhmEventStartEvent(VhmEvent event){
        super(event);
        setTime(event.getStartTime());
    }

    @Override
    public void processEvent(World world){
        VoluntaryHelperMovement.eventStarted(this);
    }


}
