/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package test;

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
		MessageStatsReportTest.class,
		ExternalEventsQueueTest.class,
		ContactTimesReportTest.class,
		TotalContactTimeReportTest.class,
		EpidemicRouterTest.class,
		ProphetRouterTest.class,
		MessageRouterTest.class,
		MessageTransferAcceptPolicyTest.class,
		SettingsTest.class,
		DijkstraPathFinderTest.class,
		PointsOfInterestTest.class,
		ActivenessHandlerTest.class,
		MaxPropDijkstraTest.class,
		MaxPropRouterTest.class,
		ScheduledUpdatesQueueTest.class,
		MessageTest.class,
		BroadcastMessageTest.class,
		BroadcastCreateEventTest.class,
		MessageEventGeneratorTest.class,
		ModuleCommunicationBusTest.class,
		DTNHostTest.class,
		LevyWalkTest.class
})

public class AllTests {

}
