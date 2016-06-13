/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */

package input;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import core.SettingsError;


/**
 * <p>External movement reader for traces that are in path format. Uses two
 * trace files, one for the paths and one for specifying activity times.
 * Nodes will follow the paths in the trace file, and pause between paths.
 * Activity times refer to the periods of time when there is valid trace data
 * about the node.</p>
 *
 * <p>Reads external traces that are of the form:</p>
 * <code>id time_1,x_1,y_1 time_2,x_2,y_2 ... \n<code>
 *
 * <p>The first line should be:</>
 * <code>maxID minTime maxTime minX maxX minY maxY</code>
 *
 * <p>Activity trace file format is:</p>
 * <code>id activeStart activeEnd\n</code>
 *
 * <p>
 * The ID in the trace files must match IDs of nodes in the simulation, the
 * coordinates must match the ONE coordinate system (units in meters) and the
 * times must match the ONE simulation time.
 * </p>
 *
 * <p>Trace and activity files ending in .zip are assumed to be
 * compressed and will be automatically uncompressed during reading. The whole
 * trace is loaded into memory at once.</p>
 *
 * @author teemuk
 *
 */
public class ExternalPathMovementReader {
	 // Singletons are evil, but I'm lazy
	private static Map<String, ExternalPathMovementReader> singletons =
		new HashMap<String, ExternalPathMovementReader>();

	/**
	 * Represents a point on the path.
	 */
	public class Entry {
		public double time;
		public double x;
		public double y;
	}

	/**
	 * Describes a node's activity time
	 */
	public class ActiveTime {
		public double start;
		public double end;
	}

	// Path cache
	private List<List<List<Entry>>> paths = null;
	// Activity cache
	private List<List<ActiveTime>> activeTimes = null;

	// Settings
	private boolean normalize = true;
	private double minTime;
	private double maxTime;
	private double minX;
	private double maxX;
	private double minY;
	private double maxY;
	private int	maxID;

	/**
	 * Creates a new reader by parsing the given files and building the internal
	 * caches.
	 *
	 * @param traceFilePath		path to the trace file
	 * @param activityFilePath	path to the activity file
	 */
	private ExternalPathMovementReader(String traceFilePath,
			String activityFilePath) throws IOException {
		// Open the trace file for reading
		File inFile = new File(traceFilePath);
		long traceSize = inFile.length();
		long totalRead = 0;
		long readSize = 0;
		long printSize = 5*1024*1024;

		BufferedReader reader = null;
		try {
			if (traceFilePath.endsWith(".zip")) {
				// Grab the first entry from the zip file
				// TODO: try to find the correct entry based on file name
				ZipFile zf = new ZipFile(traceFilePath);
				ZipEntry ze = zf.entries().nextElement();
				reader = new BufferedReader(
						new InputStreamReader(zf.getInputStream(ze)));
				traceSize = ze.getSize();
			} else {
				reader = new BufferedReader(
					new FileReader(traceFilePath));
			}
		} catch (FileNotFoundException e1) {
			throw new SettingsError("Couldn't find external movement input " +
					"file " + inFile);
		}

		/*Scanner scanner = null;
		try {
			scanner = new Scanner(inFile);
		} catch (FileNotFoundException e) {
			throw new SettingsError("Couldn't find external movement input " +
					"file " + inFile);
		}*/

		// Parse header
		String offsets = reader.readLine();
		if (offsets == null) {
			throw new SettingsError("No offset line found.");
		}
		readSize += offsets.length() + 1;
		try {
			Scanner lineScan = new Scanner(offsets);
			this.maxID = lineScan.nextInt();
			this.minTime = lineScan.nextDouble();
			this.maxTime = lineScan.nextDouble();
			this.minX = lineScan.nextDouble();
			this.maxX = lineScan.nextDouble();
			this.minY = lineScan.nextDouble();
			this.maxY = lineScan.nextDouble();
		} catch (Exception e) {
			throw new SettingsError("Invalid offset line '" + offsets + "'");
		}

		// Initialize path cache
		this.paths = new ArrayList<List<List<Entry>>>(this.maxID + 1);
		for (int i=0; i<=this.maxID; i++) {
			this.paths.add(i, new LinkedList<List<Entry>>());
		}

		// Parse traces
		String line = reader.readLine();
		while (line != null) {

			readSize += line.length() + 1;
			if (readSize >= printSize) {
				totalRead += readSize;
				readSize = 0;
				System.out.println("Processed " + (totalRead/1024) + "KB out" +
						" of " + (traceSize/1024) + "KB (" +
						Math.round(100.0*totalRead/traceSize) + "%)");
			}

			if (line.equals("")) {
				line = reader.readLine();
				continue; // Skip empty lines
			}
			Scanner traceScan = new Scanner(line);
			int id = traceScan.nextInt();
			List<List<Entry>> paths = this.paths.get(id);
			List<Entry> path = new LinkedList<Entry>();
			while (traceScan.hasNext()) {
				String dataPoint = traceScan.next();
				int d1 = dataPoint.indexOf(',');
				int d2 = dataPoint.indexOf(',', d1+1);

				Entry e = new Entry();
				e.time = Double.parseDouble(dataPoint.substring(0, d1));
				e.x = Double.parseDouble(dataPoint.substring(d1+1, d2));
				e.y = Double.parseDouble(dataPoint.substring(d2+1));

				if (this.normalize) {
					e.time -= this.minTime;
					e.x -= this.minX;
					e.y -= this.minY;
				}

				path.add(e);
			}
			paths.add(path);

			line = reader.readLine();
		}

		// Parse activity times
		inFile = new File(activityFilePath);
		reader = null;
		try {
			if (activityFilePath.endsWith(".zip")) {
				// Grab the first entry from the zip file
				// TODO: try to find the correct entry based on file name
				ZipFile zf = new ZipFile(activityFilePath);
				ZipEntry ze = zf.entries().nextElement();
				reader = new BufferedReader(
						new InputStreamReader(zf.getInputStream(ze)));
			} else {
				reader = new BufferedReader(
					new FileReader(activityFilePath));
			}
		} catch (FileNotFoundException e) {
			throw new SettingsError("Couldn't find external activity input " +
					"file " + inFile);
		}

		// Init activity cache
		this.activeTimes = new ArrayList<List<ActiveTime>>(this.maxID + 1);
		for (int i=0; i<=this.maxID; i++) {
			this.activeTimes.add(new LinkedList<ActiveTime>());
		}

		// Parse the file
		line = reader.readLine();
		while (line != null) {
			Scanner traceScan = new Scanner(line);
			int id = traceScan.nextInt();
			double start = traceScan.nextDouble();
			double end = traceScan.nextDouble();
			List<ActiveTime> times = this.activeTimes.get(id);
			ActiveTime a = new ActiveTime();
			a.start = start;
			a.end = end;
			if (this.normalize) {
				a.start -= this.minTime;
				a.end -= this.minTime;
			}
			times.add(a);

			line = reader.readLine();
		}
	}

	/**
	 * Returns the path for the node with the given ID.
	 *
	 * @param ID	ID of the node
	 * @return		full path for the node.
	 */
	public List<List<ExternalPathMovementReader.Entry>> getPaths(int ID) {
		return this.paths.get(ID);
	}

	/**
	 * Returns the active time for the given ID.
	 *
	 * @param ID	ID of the node
	 * @return		active times for the node.
	 */
	public List<ActiveTime> getActive(int ID) {
		return this.activeTimes.get(ID);
	}

	/**
	 * Sets normalizing of read values on/off. If on, values returned by
	 * readNextMovements() are decremented by minimum values of the
	 * offsets. Default is on (normalize).
	 * @param normalize If true, normalizing is on (false -> off).
	 */
	public void setNormalize(boolean normalize) {
		this.normalize = normalize;
	}


	/**
	 * Returns offset maxTime
	 * @return the maxTime
	 */
	public double getMaxTime() {
		return maxTime;
	}

	/**
	 * Returns offset maxX
	 * @return the maxX
	 */
	public double getMaxX() {
		return maxX;
	}

	/**
	 * Returns offset maxY
	 * @return the maxY
	 */
	public double getMaxY() {
		return maxY;
	}

	/**
	 * Returns offset minTime
	 * @return the minTime
	 */
	public double getMinTime() {
		return minTime;
	}

	/**
	 * Returns offset minX
	 * @return the minX
	 */
	public double getMinX() {
		return minX;
	}

	/**
	 * Returns offset minY
	 * @return the minY
	 */
	public double getMinY() {
		return minY;
	}


	/**
	 * Get an instance of the reader for the given file path. If the file has
	 * already been read previously it will not be read again and instead the
	 * previous instance of the reader will be returned.
	 *
	 * @param traceFilePath path where the trace file is read from
	 * @param activeFilePath path where the activity file is read from
	 * @return instance of the reader that has loaded all the paths from the
	 * 			given trace file.
	 */
	public static ExternalPathMovementReader getInstance(String traceFilePath,
			String activeFilePath) {
		if (!ExternalPathMovementReader.singletons.containsKey(traceFilePath)) {
			try {
				ExternalPathMovementReader.singletons.put(traceFilePath,
						new ExternalPathMovementReader(traceFilePath,
								activeFilePath));
			} catch (IOException e) {
				System.exit(1);
			}
		}
		return ExternalPathMovementReader.singletons.get(traceFilePath);
	}
}
