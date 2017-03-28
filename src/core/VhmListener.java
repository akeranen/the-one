package core;

import input.VhmEvent;

/**
 * This is an Interface for all classes that want to listen to the VhmEvents of the VouluntaryHelperMobility model.
 * Those events can be disasters or hospitals.
 * (Vhm is the abbreviation of VoluntaryHelperMovement)
 *
 * Created by Ansgar Mährlein on 15.02.2017.
 */
public interface VhmListener {

    /**
     * This Method is called when a VhmEvent starts
     * @param event The VhmEvent
     */
    void vhmEventStarted(VhmEvent event);

    /**
     * This Method is called when a VhmEvent ends
     * @param event The VhmEvent
     */
    void vhmEventEnded(VhmEvent event);
}
