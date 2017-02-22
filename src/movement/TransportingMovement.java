package movement;

import java.util.List;
import core.Settings;
import movement.map.MapNode;

public class TransportingMovement extends ShortestPathMapBasedMovement{

    /** the destination of the transport */
    private MapNode transportDestination;
    /** the location of the event */
    private MapNode eventLocation;

    /**
     * Creates a new TransportingMovement model based on a Settings object's settings.
     * @param settings The Settings object where the settings are read from
     */
    public TransportingMovement(Settings settings){
        super(settings);
        //to choose locations, specify pois in settings
        this.transportDestination = pois.selectDestination();
        this.eventLocation = pois.selectDestination();
    }

    /**
     * Copyconstructor.
     * @param tm The TransportingMovement prototype to base the new object to
     */
    protected TransportingMovement(TransportingMovement tm){
        super(tm);
        this.transportDestination = tm.transportDestination;
        this.eventLocation = tm.eventLocation;
    }

    @Override
    public Path getPath(){
        Path path = new Path(generateSpeed());
        MapNode destination = selectDestination();

        List<MapNode> nodePath = pathFinder.getShortestPath(lastMapNode, destination);

        for (MapNode node : nodePath) {
            path.addWaypoint(node.getLocation());
        }

        lastMapNode = destination;

        return path;
    }

    /**
     * Chooses the next destination depending on the current location. If the host is not
     * at the eventLocation yet, it moves there first to start the transport.
     * Stays at the same location, if host already is at the transportDestination and
     * prepares to change its movement model.
     * @return The next destination the host moves to
     */
    public MapNode selectDestination(){
        MapNode destination;
        if(host.getLocation().compareTo(eventLocation.getLocation()) != 0 && 
                host.getLocation().compareTo(transportDestination.getLocation()) != 0){
            destination = eventLocation;
        } else if(host.getLocation().compareTo(eventLocation.getLocation()) == 0){
            destination = transportDestination;
        } else {
            isReady();
            destination = transportDestination;
        }
        return destination;
    }

    /**
     * Gets the eventLocation of this TransportingMovement.
     * @return The eventLocation
     */
    public MapNode getEventLocation(){
        return this.eventLocation;
    }

    /**
     * Gets the transportDestination of this TransportingMovement.
     * @return The transportDestination
     */
    public MapNode getTransportDestination(){
        return this.transportDestination;
    }

    @Override
    public TransportingMovement replicate(){
        return new TransportingMovement(this);
    }
}