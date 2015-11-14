/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package input;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import util.Tuple;

import core.Coord;
import core.SettingsError;


/**
 * Reader for ExternalMovement movement model's time-location tuples.
 * <P>
 * First line of the file should be the offset header. Syntax of the header
 * should be:<BR>
 * <CODE>minTime maxTime minX maxX minY maxY minZ maxZ</CODE>
 * <BR>
 * Last two values (Z-axis) are ignored at the moment but can be present
 * in the file.
 * <P>
 * Following lines' syntax should be:<BR>
 * <CODE>time id xPos yPos</CODE><BR>
 * where <CODE>time</CODE> is the time when a node with <CODE>id</CODE> should
 * be at location <CODE>(xPos, yPos)</CODE>.
 * </P>
 * <P>
 * All lines must be sorted by time. Sampling interval (time difference between
 * two time instances) must be same for the whole file.
 * </P>
 */
public class ExternalMovementReader {
	/* Prefix for comment lines (lines starting with this are ignored) */
	public static final String COMMENT_PREFIX = "#";
	private Scanner scanner;
	private double lastTimeStamp = -1;
	private String lastLine;
	private double minTime;
	private double maxTime;
	private double minX;
	private double maxX;
	private double minY;
	private double maxY;
	private boolean normalize;


	/**
	 * Constructor. Creates a new reader that reads the data from a file.
	 * @param inFilePath Path to the file where the data is read
	 * @throws SettingsError if the file wasn't found
	 */
	public ExternalMovementReader(String inFilePath) {
		this.normalize = true;
		File inFile = new File(inFilePath);
		try {
			scanner = new Scanner(inFile);
		} catch (FileNotFoundException e) {
			throw new SettingsError("Couldn't find external movement input " +
					"file " + inFile);
		}

		String offsets = scanner.nextLine();

		try {
			Scanner lineScan = new Scanner(offsets);
			minTime = lineScan.nextDouble();
			maxTime = lineScan.nextDouble();
			minX = lineScan.nextDouble();
			maxX = lineScan.nextDouble();
			minY = lineScan.nextDouble();
			maxY = lineScan.nextDouble();
		} catch (Exception e) {
			throw new SettingsError("Invalid offset line '" + offsets + "'");
		}

		lastLine = scanner.nextLine();
	}

	/**
	 * Sets normalizing of read values on/off. If on, values returned by
	 * {@link #readNextMovements()} are decremented by minimum values of the
	 * offsets. Default is on (normalize).
	 * @param normalize If true, normalizing is on (false -> off).
	 */
	public void setNormalize(boolean normalize) {
		this.normalize = normalize;
	}

	/**
	 * Reads all new id-coordinate tuples that belong to the same time instance
	 * @return A list of tuples or empty list if there were no more moves
	 * @throws SettingError if an invalid line was read
	 */
	public List<Tuple<String, Coord>> readNextMovements() {
		ArrayList<Tuple<String, Coord>> moves =
			new ArrayList<Tuple<String, Coord>>();

		if (!scanner.hasNextLine()) {
			return moves;
		}

		Scanner lineScan = new Scanner(lastLine);
		double time = lineScan.nextDouble();
		String id = lineScan.next();
		double x = lineScan.nextDouble();
		double y = lineScan.nextDouble();

		if (normalize) {
			time -= minTime;
			x -= minX;
			y -= minY;
		}

		lastTimeStamp = time;

		while (scanner.hasNextLine() && lastTimeStamp == time) {
			lastLine = scanner.nextLine();

			if (lastLine.trim().length() == 0 ||
					lastLine.startsWith(COMMENT_PREFIX)) {
				continue; /* skip empty and comment lines */
			}

			// add previous line's tuple
			moves.add(new Tuple<String, Coord>(id, new Coord(x,y)));

			lineScan = new Scanner(lastLine);

			try {
				time = lineScan.nextDouble();
				id = lineScan.next();
				x = lineScan.nextDouble();
				y = lineScan.nextDouble();
			} catch (Exception e) {
				throw new SettingsError("Invalid line '" + lastLine + "'");
			}

			if (normalize) {
				time -= minTime;
				x -= minX;
				y -= minY;
			}
		}

		if (!scanner.hasNextLine()) {	// add the last tuple of the file
			moves.add(new Tuple<String, Coord>(id, new Coord(x,y)));
		}

		return moves;
	}

	/**
	 * Returns the time stamp where the last moves read with
	 * {@link #readNextMovements()} belong to.
	 * @return The time stamp
	 */
	public double getLastTimeStamp() {
		return lastTimeStamp;
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

}
