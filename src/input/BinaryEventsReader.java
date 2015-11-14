/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package input;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import core.SimError;

/**
 * Reads External Events from a binary file. Can also create binary files
 * from a list of external events.
 */
public class BinaryEventsReader implements ExternalEventsReader {
	/** Extension of binary external events file */
	public static final String BINARY_EXT = ".binee";

	private ObjectInputStream in;
	private int eventsLeft;

	/**
	 * Constructor.
	 * @param eventsFile The file where the events are read
	 */
	public BinaryEventsReader(File eventsFile) {
		try {
			FileInputStream fis = new FileInputStream(eventsFile);
			in = new ObjectInputStream(fis);
			// first object should tell the amount of events
			eventsLeft = (Integer)in.readObject();
		} catch (IOException e) {
			throw new SimError(e);
		} catch (ClassNotFoundException e) {
			throw new SimError("Invalid binary input file for external " +
					"events:" + eventsFile.getAbsolutePath(), e);
		}

	}

	/**
	 * Read events from a binary file created with storeBinaryFile method
	 * @param nrof Maximum number of events to read
	 * @return Events in an ArrayList (empty list if didn't read any)
	 * @see #storeToBinaryFile(String, List)
	 */
	@SuppressWarnings("unchecked") // suppress cast warnings
	public List<ExternalEvent> readEvents(int nrof) {
		ArrayList<ExternalEvent> events = new ArrayList<ExternalEvent>(nrof);

		if (eventsLeft == 0) {
			return events;
		}

		try {
			for (int i=0; i < nrof && eventsLeft > 0; i++) {
				events.add((ExternalEvent)in.readObject());
				eventsLeft--;
			}
			if (eventsLeft == 0) {
				in.close();
			}
		} catch (Exception e) { // FIXME: quick 'n' dirty exception handling
			throw new SimError(e);
		}
		return events;
	}

	/**
	 * Checks if the given file is a binary external events file
	 * @param file The file to check
	 * @return True if the file is a binary ee file, false if not
	 */
	public static boolean isBinaryEeFile(File file) {
		if (!file.getName().endsWith(BINARY_EXT)) {
			return false;
		}

		// extension matches, try to read an event
		try {
			BinaryEventsReader r = new BinaryEventsReader(file);
			r.readEvents(1);
			r.close();
		}
		catch (SimError e) {
			return false; // read failed -> not a valid file
		}

		return true; // seems to be a valid binary ee file
	}

	/**
	 * Stores the events to a binary file
	 * @param fileName Path to the file where the events are stored
	 * @param events List of events to store
	 * @throws IOException if something in storing went wrong
	 */
	public static void storeToBinaryFile(String fileName,
			List<ExternalEvent> events) throws IOException {

		// make sure the file name ends with binary extension
		if (!fileName.endsWith(BINARY_EXT)) {
			fileName += "BINARY_EXT";
		}

		ObjectOutputStream out;
		FileOutputStream fos = new FileOutputStream(fileName);
		out = new ObjectOutputStream(fos);

		// store the number of events
		out.writeObject(new Integer(events.size()));

		// store events
		for (ExternalEvent ee : events) {
			out.writeObject(ee);
		}

		out.close();
	}

	public void close() {
		try {
			this.in.close();
		}
		catch (IOException ioe) {
			throw new SimError(ioe);
		}
	}

}
