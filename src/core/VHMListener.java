package core;

import input.VHMEvent;

/**
 * This is an Interface for all classes that want to listen to the disaster events of the VouluntaryHelperMobility model.
 * Created by Ansgar Mährlein on 15.02.2017.
 */
public interface VHMListener {

    /**
     * This Method is called when a VHMEvent starts
     * @param event The VHMEvent
     */
    void vhmEventStarted(VHMEvent event);

    /**
     * This Method is called when a VHMEvent ends
     * @param event The VHMEvent
     */
    void vhmEventEnded(VHMEvent event);
}
