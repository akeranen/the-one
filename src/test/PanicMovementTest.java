package test;

import input.WKTMapReader;

import java.io.IOException;
import java.io.StringReader;

import junit.framework.*;
import movement.MovementModel;
import movement.Path;
import movement.map.MapNode;
import movement.map.PanicMovementUtil;
import movement.map.SimMap;
import movement.PanicMovement;
import core.Coord;
import core.DTNHost;
import core.Settings;

/** This class tests the PanicMovement class using this
 * topology:  n7--n5
 *            |   |
 *        n1--n2--n6--n3
 *         |
 *        n4
 **/
public class PanicMovementTest extends TestCase {

	//										   n1       n2       n6        n3
	private static final String WKT = "LINESTRING (1.0 1.0, 2.0 1.0, 3.0 1.0, 4.0 1.0) \n" +
	//              n1        n4
	"LINESTRING (1.0 1.0, 1.0 2.0)\n"+
	//              n2       n7       n3       n6
	"LINESTRING (2.0 1.0, 2.0 0.0, 3.0 0.0, 3.0 1.0)\n";
	//
	
	private MapNode[] node = new MapNode[6];
	private MapNode event;

	private PanicMovement panicMovement;
	private SimMap map;
	private TestSettings settings;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		setupMapDataAndBasicSettings();
		panicMovement.setHost(setupHost());
	}
	
	/**
	 * Sets up the map described above, the panic movement and the event, as well as speed and wait time settings
	 */
	private void setupMapDataAndBasicSettings() {
		Settings.init(null);
		StringReader input = new StringReader(WKT);

		WKTMapReader reader = new WKTMapReader(true);
		try {
			reader.addPaths(input, 0);
		} catch (IOException e) {
			fail(e.toString());
		}

		settings = new TestSettings();
		settings.putSetting(MovementModel.SPEED, "1,1");
		settings.putSetting(MovementModel.WAIT_TIME, "0,0");

		map = reader.getMap();	
		event = map.getNodeByCoord(new Coord(2,1));
		panicMovement = new PanicMovement(settings, map, 3, event.getLocation(), 1.0, 1.5); 

		node[0] = map.getNodeByCoord(new Coord(1,1));
		node[1] = map.getNodeByCoord(new Coord(2,1));
		node[2] = map.getNodeByCoord(new Coord(4,1));
		node[3] = map.getNodeByCoord(new Coord(1,2));
		node[4] = map.getNodeByCoord(new Coord(3,0));
		node[5] = map.getNodeByCoord(new Coord(3,1));
	}

	/**
	 * Tests if the angle between source node, event and target node is between 90 and 270 degrees (so the 
	 * absolute of the difference of straight angle and this angle should be less or equal to 90 degrees)
	 * to avoid running through the event 
	 */
	public void testAngle() {
		
		Path path = panicMovement.getPath();
		MapNode start = map.getNodeByCoord(path.getCoords().get(0));
		MapNode end = map.getNodeByCoord(path.getCoords().get(path.getCoords().size() - 1));
		
		double angle = PanicMovementUtil.computeAngleBetween(event.getLocation(), start, end);
		assertTrue("Angle should be between 90 and 270 degrees", Math.abs(180 - angle)  <= 90);
	}
	
	/**
	 * Tests if the target node is inside the safe area
	 */
	public void testSafeRegion() {

		Path path = panicMovement.getPath();
		MapNode end = map.getNodeByCoord(path.getCoords().get(path.getCoords().size() - 1));
		
		assertTrue("Target node should be inside the safe area", 
				end.getLocation().distance(event.getLocation()) >= panicMovement.getSafeRangeRadius() );
	}

	/**
	 * Test if closest possible node to the host is selected
	 */
	public void testOptimizationCriterion() {
		
		Path path = panicMovement.getPath();
		MapNode start = map.getNodeByCoord(path.getCoords().get(0));
		MapNode end = map.getNodeByCoord(path.getCoords().get(path.getCoords().size() - 1));
		
		for(MapNode m : node) {
			if (end.getLocation().distance(panicMovement.getHost().getLocation()) 
					> m.getLocation().distance(panicMovement.getHost().getLocation())) {
				assertTrue("Closest possible node to the host should be selected",
						m.getLocation().distance(event.getLocation()) < panicMovement.getSafeRangeRadius()
						|| panicMovement.getPanicMovementUtil().isInEventDirection(start, m));
			}
		}
	}

	/**
	 * Tests if the target node is not outside the event range
	 */
	public void testEventRange() {

		Path path = panicMovement.getPath();
		MapNode end = map.getNodeByCoord(path.getCoords().get(path.getCoords().size() - 1));
		
		assertTrue("Target node should not be outside the event range",
				end.getLocation().distance(event.getLocation()) <= panicMovement.getEventRangeRadius() );
	}
	
	/**
	 * Creates a host for the map
	 * @return created host
	 */
	private DTNHost setupHost() {
		TestUtils utils = new TestUtils(null, null, settings);
		DTNHost h1 = utils.createHost(panicMovement, null);

		h1.move(0); // get a path for the node
		// move node directly to first waypoint
		h1.setLocation(h1.getPath().getCoords().get(0));
		return h1;
	}
}
