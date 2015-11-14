/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package input;

import java.util.List;

/**
 * Interface for external event readers.
 */
public interface ExternalEventsReader {

	/**
	 * Read events from the reader
	 * @param nrof Maximum number of events to read
	 * @return Events in a List
	 */
	public List<ExternalEvent> readEvents(int nrof);

	/**
	 * Closes the input file streams of the reader.
	 */
	public void close();


}
