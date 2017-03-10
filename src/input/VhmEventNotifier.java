package input;

import core.VhmListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class holds the List of VhmListeners and notifies them when a VhmEvent started or ended.
 * Created by Ansgar Mährlein on 10.03.2017.
 */
public final class VhmEventNotifier {
    /**
     * List of VhmListeners
     */
    private static List<VhmListener> listeners = Collections.synchronizedList(new ArrayList<>());

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
     * Informs all registered VhmListeners, that a VhmEvent started and adds it to the appropriate List
     *
     * @param event The VhmEvent.
     */
    public static void eventStarted(VhmEvent event) {
        for (VhmListener l : listeners) {
            l.vhmEventStarted(event);
        }
    }

    /**
     * Informs all registered VhmListeners, that a VhmEvent ended and removes it from the appropriate list
     *
     * @param event The VhmEvent.
     */
    public static void eventEnded(VhmEvent event) {
        for (VhmListener l : listeners) {
            l.vhmEventEnded(event);
        }
    }
}
