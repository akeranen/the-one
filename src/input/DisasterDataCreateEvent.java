package input;

import core.Coord;
import core.DTNHost;
import core.DisasterData;
import core.World;

/**
 * An {@link ExternalEvent} creating a {@link DisasterData}.
 *
 * Created by Britta Heymann on 05.04.2017.
 */
public class DisasterDataCreateEvent extends ExternalEvent {
    private static final long serialVersionUID = 1;

    /* Address of the host that will create the data. */
    private int creatorAddress;

    /* Properties of the disaster data to create. */
    private DisasterData.DataType type;
    private int size;

    /* Offset between host location and data location. */
    private transient Coord offset;

    /**
     * Initializes a new instance of the {@link DisasterDataCreateEvent} class.
     * @param creatorAddress Address of the host that will create the {@link DisasterData}.
     * @param type Type of the {@link DisasterData} to create.
     * @param size Size of the {@link DisasterData} to create.
     * @param offset Offset between host location and data location.
     * @param time Time of the event and creation time of the {@link DisasterData} to create.
     */
    public DisasterDataCreateEvent(
            int creatorAddress, DisasterData.DataType type, int size, Coord offset, double time) {
        super(time);
        this.creatorAddress = creatorAddress;
        this.type = type;
        this.size = size;
        this.offset = offset;
    }

    /**
     * Processes the external event.
     *
     * @param world World where the actors of the event are
     */
    @Override
    public void processEvent(World world) {
        DTNHost creator = world.getNodeByAddress(this.creatorAddress);
        DisasterData data = new DisasterData(this.type, this.size, this.time, this.determineLocation(creator));
        DisasterDataNotifier.dataCreated(creator, data);
    }

    /**
     * Determines the {@link DisasterData}'s location by adding the stored offset to the host's current location.
     * @param host The {@link DTNHost} creating the data.
     * @return A location.
     */
    private Coord determineLocation(DTNHost host) {
        Coord creatorLocation = host.getLocation();
        return new Coord(
                creatorLocation.getX() + offset.getX(),
                creatorLocation.getY() + offset.getY());
    }

    /**
     * Returns a String representation of the event
     *
     * @return a String representation of the event
     */
    @Override
    public String toString() {
        return String.format(
                "DATA @%.2f %s size:%s offset:%s CREATE by host %d",
                this.time, this.type, this.size, this.offset, this.creatorAddress);
    }
}
