package test;

import core.VhmListener;
import input.VhmEvent;

/**
 * A VhmListener that simply records calls to its methods.
 *
 * Created by Britta Heymann on 01.04.2017.
 */
class RecordingVhmEventListener implements VhmListener {
    private VhmEvent lastEvent;
    private int numberOfEventStartedCalls;
    private int numberOfEventEndedCalls;

    RecordingVhmEventListener() {
        // Nothing to do here, defaults are good values for the fields.
    }

    /**
     * This Method is called when a VhmEvent starts
     *
     * @param event The VhmEvent
     */
    @Override
    public void vhmEventStarted(VhmEvent event) {
        this.numberOfEventStartedCalls++;
        this.lastEvent = event;
    }

    /**
     * This Method is called when a VhmEvent ends
     *
     * @param event The VhmEvent
     */
    @Override
    public void vhmEventEnded(VhmEvent event) {
        this.numberOfEventEndedCalls++;
        this.lastEvent = event;
    }

    /**
     * Gets the number of calls to vhmEventStarted.
     *
     * @return The number of calls.
     */
    int getNumberOfEventStartedCalls() {
        return numberOfEventStartedCalls;
    }

    /**
     * Gets the number of calls to vhmEventEnded.
     *
     * @return The number of calls.
     */
    int getNumberOfEventEndedCalls() {
        return this.numberOfEventEndedCalls;
    }

    /**
     * Gets the last event that was recorded.
     *
     * @return The last recorded event.
     */
    VhmEvent getLastEvent() {
        return this.lastEvent;
    }
}
