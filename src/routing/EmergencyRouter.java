package routing;

import core.Settings;

public class EmergencyRouter extends ActiveRouter {

    public EmergencyRouter(Settings s) {
        super(s);
    }

    protected EmergencyRouter(EmergencyRouter r) {
        super(r);
    }

    @Override
    public void update() {
        super.update();
        this.tryAllMessagesToAllConnections();
    }

    @Override
    public MessageRouter replicate() {
        return new EmergencyRouter(this);
    }
}
