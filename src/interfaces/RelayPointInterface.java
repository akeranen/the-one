package interfaces;

import core.Connection;
import core.NetworkInterface;
import core.Settings;
import routing.SamplerRouter;

/**
 * Interface used by the hosts to communicate with relay points when using 
 * SamplerRouter.
 */
public class RelayPointInterface extends SimpleBroadcastInterface {

	public RelayPointInterface(Settings s) {
		super(s);
	}
	
	public RelayPointInterface(RelayPointInterface prot) {
		super(prot);
	}
	
	@Override
	public RelayPointInterface replicate() {
		return new RelayPointInterface(this);
	}
	
	@Override
	protected void connect(Connection con, NetworkInterface anotherInterface) {
		if (!(this.getHost().getRouter() instanceof SamplerRouter) ||
			!(anotherInterface.getHost().getRouter() instanceof SamplerRouter)) {
			System.out.println("The RelayPointInterface can only be used with SamplerRouter.");
			System.exit(1);
		}
		
		if (!(anotherInterface instanceof RelayPointInterface)) {
			return;
		}
		
		SamplerRouter myRouter = (SamplerRouter) this.getHost().getRouter();
		SamplerRouter otherRouter = (SamplerRouter) anotherInterface.getHost().getRouter();

		// Only opens connection between one relay point and one node.
		if (myRouter.isRelayPoint() != otherRouter.isRelayPoint()){
			super.connect(con, anotherInterface);
		}

	}

}
