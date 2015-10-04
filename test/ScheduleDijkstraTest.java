/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package test;

import java.util.List;

import routing.schedule.ScheduleDijkstra;
import routing.schedule.ScheduleOracle;
import routing.schedule.ScheduleEntry;
import junit.framework.TestCase;

public class ScheduleDijkstraTest extends TestCase {

	ScheduleOracle oracle;
	ScheduleDijkstra d;
	
	protected void setUp() throws Exception {
		super.setUp();
		oracle = new ScheduleOracle();
		d = new ScheduleDijkstra(oracle);
		
		oracle.addEntry(10, 1, 2, 10);
		oracle.addEntry(20, 1, 3, 10);
		oracle.addEntry(20, 1, 4, 10);
		oracle.addEntry(30, 1, 5, 10);		
	}

	private void comparePaths(int realPath[], List<ScheduleEntry> path) {
		int i;
		assertEquals(realPath.length, path.size() + 1);
		
		for (i=0; i<realPath.length - 1; i++) {
			assertEquals(realPath[i], (int)path.get(i).getFrom());
		}
		
		assertEquals(realPath[i], (int)path.get(i-1).getTo());
	}
	
	public void testSimplePaths() {	
		assertEquals(0, d.getShortestPath(1, 10, 0).size()); /* no path */
		
		oracle.addEntry(55, 2, 10, 5); /* path via 2 (starting @ 55) */
		comparePaths(new int[]{1,2,10}, d.getShortestPath(1, 10, 0));
		
		oracle.addEntry(25, 3, 10, 5); /* this is not in time at 3 */
		/* should use same path as before */
		comparePaths(new int[]{1,2,10}, d.getShortestPath(1, 10, 0)); 

		oracle.addEntry(35, 3, 10, 5); /* now early enough, should go via 3 */	
		comparePaths(new int[]{1,3,10}, d.getShortestPath(1, 10, 0));
		
		/* starts earlier than previous, but takes longer */
		oracle.addEntry(30, 2, 10, 50); 
		comparePaths(new int[]{1,3,10}, d.getShortestPath(1, 10, 0));

		/* new fastest path */
		oracle.addEntry(30, 2, 10, 7); 
		comparePaths(new int[]{1,2,10}, d.getShortestPath(1, 10, 0));

		/* new fastest path */
		oracle.addEntry(30, 4, 10, 5); 
		comparePaths(new int[]{1,4,10}, d.getShortestPath(1, 10, 0));		
	}
	
	public void testMultipleHops() {
		oracle.addEntry(40, 3, 10, 5);
		oracle.addEntry(40, 3, 11, 10);
		oracle.addEntry(60, 10, 20, 10);
		oracle.addEntry(60, 11, 20, 15);
		
		oracle.addEntry(100, 1, 20, 5); /* late but fast */
		
		comparePaths(new int[]{1,3,10,20}, d.getShortestPath(1, 20, 0));
		
		/* bit later via 11 but faster */
		oracle.addEntry(65, 11, 20, 3);
		comparePaths(new int[]{1,3,11,20}, d.getShortestPath(1, 20, 0));

		/* faster multihop 3->12->13->14->20 */
		oracle.addEntry(45, 3, 12, 2);
		oracle.addEntry(50, 12, 13, 2);
		oracle.addEntry(55, 13, 14, 2);
		oracle.addEntry(57, 14, 20, 2);
		comparePaths(new int[]{1,3,12,13,14,20}, d.getShortestPath(1, 20, 0));
		
		/* misses the first hop to 3, takes direct late */
		comparePaths(new int[]{1, 20}, d.getShortestPath(1, 20, 30));
		
		/* starts directly at 3 but too late for multihop */
		oracle.addEntry(55, 3, 11, 5);
		comparePaths(new int[]{3, 11, 20}, d.getShortestPath(3, 20, 50));
		
		/* starts directly at 3, early enough for multihop */
		comparePaths(new int[]{3,12,13,14,20}, d.getShortestPath(3, 20, 40));		
	}

}
