/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package test;
import core.SettingsError;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;




@RunWith(Suite.class)

@Suite.SuiteClasses({
		WorldTest.class,
		ConnectionTest.class,
		ExternalMovementReaderTest.class,
		ExternalMovementTest.class,
		WKTReaderTest.class,
		WKTPointReaderTest.class,
		MapNodeTest.class,
		MapBasedMovementTest.class,
		CoordTest.class,
		DistanceDelayReportTest.class,
		AdjacencyGraphvizReportTest.class,
		MessageGraphvizReportTest.class,
		ExternalEventsQueueTest.class,
		ContactTimesReportTest.class,
		TotalContactTimeReportTest.class,
		EpidemicRouterTest.class,
		ProphetRouterTest.class,
		SettingsTest.class,
		DijkstraPathFinderTest.class,
		PointsOfInterestTest.class,
		ActivenessHandlerTest.class,
		MaxPropDijkstraTest.class,
		MaxPropRouterTest.class,
		ScheduledUpdatesQueueTest.class,
		MessageTest.class,
		BroadcastMessageTest.class,
		ModuleCommunicationBusTest.class,
		DTNHostTest.class
})

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("the ONE tests");
		try {
			TestSettings.init(null);
		} catch (SettingsError e) {
			e.printStackTrace();
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
		suite.addTestSuite(DTNHostTest.class);
		//$JUnit-END$
		return suite;
	}

}
