package test;

import routing.EpidemicRouter;

public class AbstractDropPolicyTest extends AbstractRouterTest {
	
	private String dropPolicyClass;

	/**
	 * Constructor.
	 * @param dropPolicyClass The drop policy class name.
	 */
	public AbstractDropPolicyTest(String dropPolicyClass) {
		this.dropPolicyClass = dropPolicyClass;
	}
	
	/** Use three routers to do the tests **/
	protected EpidemicRouter r0;
	protected EpidemicRouter r1;
	protected EpidemicRouter r2;
	protected EpidemicRouter r3;
	
	@Override
	protected void setUp() throws Exception {
		ts.setNameSpace("Group1");
		ts.putSetting("dropPolicy", this.dropPolicyClass);
		//ts.putSetting("bufferSize", "3");
		setRouterProto(new EpidemicRouter(ts));
		super.setUp();
		
		// Adjust the routers references
		r0 = (EpidemicRouter)h0.getRouter();
		r1 = (EpidemicRouter)h1.getRouter();
		r2 = (EpidemicRouter)h2.getRouter();
		r3 = (EpidemicRouter)h3.getRouter();
		
	}
	
	protected void advanceWorld(int seconds) {
		clock.advance(1);
		updateAllNodes();
	}
	
}
