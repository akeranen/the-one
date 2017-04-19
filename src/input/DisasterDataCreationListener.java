package input;

import core.DTNHost;
import core.DisasterData;

/**
 * Interface for classes that want to be notified of {@link DisasterData} newly created by a
 * {@link DisasterDataCreateEvent}.
 *
 * Created by Britta Heymann on 05.04.2017.
 */
@FunctionalInterface
public interface DisasterDataCreationListener {
    /**
     * Called when a {@link DisasterData} got created by a {@link DisasterDataCreateEvent}.
     * @param creator {@link DTNHost} that created the data.
     * @param data The created {@link DisasterData}.
     */
    void disasterDataCreated(DTNHost creator, DisasterData data);
}
