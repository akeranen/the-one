package input;

import core.World;
import movement.VoluntaryHelperMovement;

/**
 * This is an event class that will be used for ending {@link VhmEvent}s by
 * the {@link VhmEventReader}
 *
 * Created by Marius Meyer on 15.02.17.
 */
public class VhmEventEndEvent extends VhmEvent {

    private static final long serialVersionUID = 1;

    /**
     * Creates a new event for the end of a specified VhmEvent
     * @param event the VhmEvent
     */
    public VhmEventEndEvent(VhmEvent event){
        super(event);
        time = event.getEndTime();
    }

    @Override
    public void processEvent(World world){
        VoluntaryHelperMovement.eventEnded(this);
    }
}
