package movement;
import core.Settings;

public class EmergencyMovement extends ProhibitedPolygonRwp {
    public EmergencyMovement(Settings settings) {
        super(settings);
    }

    public EmergencyMovement( final EmergencyMovement other ) {
        super( other );
    }

    @Override
    public Path getPath() {
        int hostId = this.getHost().getAddress();
        int hostNumberOfMessages = this.getHost().getRouter().getNrofMessages();

        if (hostNumberOfMessages > 0) {
            System.out.println("Host " + hostId + " has " + hostNumberOfMessages + " messages and should now change its movement model");
            // TODO: Change movement model here based on number of messages
        }

        return super.getPath();
    }

    @Override
    public MovementModel replicate() {
        return new EmergencyMovement( this );
    }
}
