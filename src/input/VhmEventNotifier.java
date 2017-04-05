package input;

import core.VhmListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class holds the List of VhmListeners and notifies them when a VhmEvent started or ended.
 * (Vhm is the abbreviation of VoluntaryHelperMovement)
 *
 * Created by Ansgar Mährlein on 10.03.2017.
 */
public final class VhmEventNotifier {
    /**
     * List of VhmListeners
     */
    private static List<VhmListener> listeners = Collections.synchronizedList(new ArrayList<VhmListener>());

    /** Private constructor to hide the implicit public one. */
    private VhmEventNotifier() {

    }

    /**
     * Adds a VhmListener that will be notified of VhmEvents starting and ending.
     *
     * @param listener The listener that is added.
     */
    public static void addListener(VhmListener listener) {
        listeners.add(listener);
    }

    /**
     * Informs all registered VhmListeners that a VhmEvent started.
     *
     * @param event The VhmEvent.
     */
    public static void eventStarted(VhmEvent event) {
        for (VhmListener l : listeners) {
            l.vhmEventStarted(event);
        }
    }

    /**
     * Informs all registered VhmListeners that a VhmEvent ended.
     *
     * @param event The VhmEvent.
     */
    public static void eventEnded(VhmEvent event) {
        for (VhmListener l : listeners) {
            l.vhmEventEnded(event);
        }
    }
}
