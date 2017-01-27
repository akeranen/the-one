/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package test;

import java.io.File;
import java.io.PrintWriter;

import junit.framework.TestCase;
import movement.ExternalMovement;
import movement.MovementModel;
import core.Coord;
import core.DTNHost;
import core.SimClock;

public class ExternalMovementTest extends TestCase {
	/* two nodes moving */
	private static final String[] INPUT = {
		"0 30 0 350 0 100 0 0",
		"10 1 10 0",
		"10 2 20 0",
		"20 1 20 10",
		"20 2 300.5 10",
		"30 1 30 10",
		"30 2 40 100"
	};

	/* two stationary nodes */
	private static final String[] STATIONARY_INPUT = {
		"0 0 0 0 0 0 0 0",
		"0 1 10 10",
		"0 2 20 20"
	};

	private static final Coord[][] INPUT_COORDS = {
		{ new Coord(10,0), new Coord(20,10), new Coord(30,10) }, // h1
		{ new Coord(20,0), new Coord(300.5,10), new Coord(40,100) }  // h2
		};
	private static final Coord[] STATIONARY_INPUT_COORDS = {
		new Coord(10,10) , new Coord(20,20)
	};

	private static final double CLOCK_STEP = 10;
	private Coord c0 = new Coord(0,0);
	private DTNHost h1;
	private DTNHost h2;
	private DTNHost h3;
	private SimClock clock;

	protected void setUpUsing(String[] input) throws Exception {
		super.setUp();
		ExternalMovement.reset();
		TestSettings ts = new TestSettings();
		ts.putSetting(MovementModel.MOVEMENT_MODEL_NS + "." +
						MovementModel.WORLD_SIZE, "1000,1000");
		File outFile = File.createTempFile("eMovementTest", ".tmp");
		outFile.deleteOnExit();
		PrintWriter pw = new PrintWriter(outFile);

		for (String s : input) {
			pw.println(s);
		}

		pw.close();

		ts.putSetting(ExternalMovement.EXTERNAL_MOVEMENT_NS + "." +
				ExternalMovement.MOVEMENT_FILE_S, outFile.getAbsolutePath());

		MovementModel emProto = (MovementModel)
			ts.createIntializedObject("movement.ExternalMovement");

		TestUtils utils = new TestUtils(null, null, ts);
		h1 = utils.createHost(emProto, "h1");
		h2 = utils.createHost(emProto, "h2");
		h3 = utils.createHost(emProto, "h3");
		clock = SimClock.getInstance();
		clock.setTime(0);
	}

	public void testMovement() throws Exception {
		setUpUsing(INPUT);

		// h3 should not get any fancy coordinates
		assertEquals(c0, h3.getLocation());
		assertFalse(h3.isMovementActive());

		// test that h1 and h2 move according to input data
		for (int i=0; i<INPUT_COORDS.length; i++) {
			assertEquals((i+1) + ". coord of h1",
					INPUT_COORDS[0][i], h1.getLocation());
			assertEquals((i+1) + ". coord of h2",
					INPUT_COORDS[1][i], h2.getLocation());

			clock.advance(CLOCK_STEP);
			moveAllHosts(CLOCK_STEP);
		}
	}

	public void testStationary() throws Exception {
		setUpUsing(STATIONARY_INPUT);

		for (int i=0; i<3; i++) {
			// hosts h1 & h2 should stay in the same place all the time
			assertEquals((i+1) + ". coord of h1",
					STATIONARY_INPUT_COORDS[0], h1.getLocation());
			assertEquals((i+1) + ". coord of h2",
					STATIONARY_INPUT_COORDS[1], h2.getLocation());
			moveAllHosts(CLOCK_STEP);
			// h3 should not get any fancy coordinates
			assertEquals(c0, h3.getLocation());
			assertFalse(h3.isMovementActive());
		}

	}

	public void moveAllHosts(double time) {
		h1.move(time);
		h2.move(time);
		h3.move(time);
	}
}
