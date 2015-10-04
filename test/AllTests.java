/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package test;

import junit.framework.Test;
import junit.framework.TestSuite;
import core.SettingsError;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("the ONE tests");
		try {
			TestSettings.init(null);
		} catch (SettingsError e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		// set US locale to parse decimals in consistent way
		java.util.Locale.setDefault(java.util.Locale.US);
		
		//$JUnit-BEGIN$
		suite.addTestSuite(WorldTest.class);
		suite.addTestSuite(ConnectionTest.class);
		suite.addTestSuite(ExternalMovementReaderTest.class);
		suite.addTestSuite(ExternalMovementTest.class);
		suite.addTestSuite(WKTReaderTest.class);
		suite.addTestSuite(WKTPointReaderTest.class);
		suite.addTestSuite(MapNodeTest.class);
		suite.addTestSuite(MapBasedMovementTest.class);
		suite.addTestSuite(CoordTest.class);
		suite.addTestSuite(DistanceDelayReportTest.class);
		suite.addTestSuite(AdjacencyGraphvizReportTest.class);
		suite.addTestSuite(MessageGraphvizReportTest.class);
		suite.addTestSuite(ExternalEventsQueueTest.class);
		suite.addTestSuite(ContactTimesReportTest.class);
		suite.addTestSuite(TotalContactTimeReportTest.class);
		suite.addTestSuite(EpidemicRouterTest.class);
		suite.addTestSuite(ProphetRouterTest.class);
		suite.addTestSuite(SettingsTest.class);
		suite.addTestSuite(DijkstraPathFinderTest.class);
		suite.addTestSuite(PointsOfInterestTest.class);
		suite.addTestSuite(ActivenessHandlerTest.class);
		suite.addTestSuite(MaxPropDijkstraTest.class);
		suite.addTestSuite(MaxPropRouterTest.class);
		suite.addTestSuite(ScheduledUpdatesQueueTest.class);
		suite.addTestSuite(MessageTest.class);
		suite.addTestSuite(ModuleCommunicationBusTest.class);
		//$JUnit-END$
		return suite;
	}

}
