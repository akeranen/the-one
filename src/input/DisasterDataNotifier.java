package input;

import core.DTNHost;
import core.DisasterData;

import java.util.ArrayList;
import java.util.List;

/**
 * Responsible for managing {@link DisasterDataCreationListener}s.
 *
 * Created by Britta Heymann on 05.04.2017.
 */
public final class DisasterDataNotifier {
    private static List<DisasterDataCreationListener> listeners = new ArrayList<>();

    private DisasterDataNotifier() {
        throw new IllegalAccessError("Utility class");
    }

    /**
     * Adds a {@link DisasterDataCreationListener} to notify about {@link DisasterDataCreateEvent}s.
     *
     * @param listener The listener to add.
     */
    public static void addListener(DisasterDataCreationListener listener) {
        listeners.add(listener);
    }

    /**
     * Informs all registered {@link DisasterDataCreationListener}s about a {@link DisasterData} created by some
     * {@link DTNHost}.
     *
     * @param creator {@link DTNHost} which created the data.
     * @param data The created {@link DisasterData}.
     */
    public static void dataCreated(DTNHost creator, DisasterData data) {
        for (DisasterDataCreationListener l : listeners) {
            l.disasterDataCreated(creator, data);
        }
    }
}
