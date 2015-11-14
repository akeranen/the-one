/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package input;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import core.Coord;

/**
 * Class for reading "Well-known text syntax" files. See e.g.
 * <A HREF="http://en.wikipedia.org/wiki/Well-known_text">Wikipedia</A> for
 * WKT syntax details. For example, <A HREF="http://openjump.org/">Open JUMP</A>
 * GIS program can save compatible data from many other formats.<BR>
 */
public class WKTReader {
	/** known WKT type LINESTRING */
	public static final String LINESTRING = "LINESTRING";
	/** known WKT type MULTILINESTRING */
	public static final String MULTILINESTRING = "MULTILINESTRING";
	/** known WKT type POINT */
	public static final String POINT = "POINT";

	/** are all lines of the file read */
	private boolean done;
	/** reader for the data */
	private BufferedReader reader;

	/**
	 * Read point data from a file
	 * @param file The file to read points from
	 * @return A list of coordinates read from the file
	 * @throws IOException if something went wrong while reading
	 */
	public List<Coord> readPoints(File file) throws IOException {
		return readPoints(new FileReader(file));
	}

	/**
	 * Read point data from a Reader
	 * @param r The Reader to read points from
	 * @return A list of coordinates that were read
	 * @throws IOException if something went wrong while reading
	 */
	public List<Coord> readPoints(Reader r) throws IOException {
		List<Coord> points = new ArrayList<Coord>();

		String type;
		init(r);

		while((type = nextType()) != null) {
			if (type.equals(POINT)) {
				points.add(parsePoint());
			}
			else {
				// known type but not interesting -> skip
				readNestedContents();
			}
		}

		return points;
	}

	/**
	 * Read line (LINESTRING) data from a file
	 * @param file The file to read data from
	 * @return A list of coordinate lists read from the file
	 * @throws IOException if something went wrong while reading
	 */
	public List<List<Coord>> readLines(File file) throws IOException {
		List<List<Coord>> lines = new ArrayList<List<Coord>>();

		String type;
		init(new FileReader(file));

		while((type = nextType()) != null) {
			if (type.equals(LINESTRING)) {
				lines.add(parseLineString(readNestedContents()));
			}
			else {
				// known type but not interesting -> skip
				readNestedContents();
			}
		}

		return lines;
	}


	/**
	 * Initialize the reader to use a certain input reader
	 * @param input The input to use
	 */
	protected void init(Reader input) {
		setDone(false);
		reader = new BufferedReader(input);
	}

	/**
	 * Returns the next type read from the reader given at init or null
	 * if no more types can be read
	 * @return the next type read from the reader given at init
	 * @throws IOException
	 */
	protected String nextType() throws IOException {
		String type = null;

		while (!done && type == null) {
			type = readWord(reader);

			if (type.length() < 1) { // discard empty lines
				type = null;
				continue;
			}
		}

		return type;
	}

	/**
	 * Returns true if type is one of the known WKT types
	 * @param type The type to check
	 * @return true if type is one of the known WKT types
	 */
	protected boolean isKnownType(String type) {
		if (type.equals(LINESTRING)) {
			return true;
		}
		else if (type.equals(MULTILINESTRING)) {
			return true;
		}
		else if (type.equals(POINT)) {
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * Reads a "word", ie whitespace delimited string of characters, from
	 * the reader
	 * @param r Reader to read the characters from
	 * @return The word that was read (or empty string if nothing was read)
	 * @throws IOException
	 */
	protected String readWord(Reader r) throws IOException {
		StringBuffer buf = new StringBuffer();
		char c = skipAllWhitespace(r);

		// read non-whitespace part
		while(c != (char)-1 && !Character.isWhitespace(c)) {
			buf.append(c);
			c = (char)r.read();
		}

		if (c == (char)-1) {
			setDone(true);
		}
		return buf.toString();
	}

	/**
	 * Parses a MULTILINESTRING statement that has nested linestrings from
	 * the current reader
	 * @return List of parsed Coord lists
	 * @throws IOException
	 */
	protected List<List<Coord>> parseMultilinestring()
			throws IOException {
		List<List<Coord>> list = new ArrayList<List<Coord>>();
		String multiContents = readNestedContents(reader);
		StringReader r2 = new StringReader(multiContents);
		String lineString = readNestedContents(r2);

		while (lineString.length() > 0) {
			list.add(parseLineString(lineString));
			lineString = readNestedContents(r2);
		}

		return list;
	}

	/**
	 * Parses a WKT point data from the intialized reader
	 * @return Point data as a Coordinate
	 * @throws IOException if couldn't parse coordinate values
	 */
	protected Coord parsePoint() throws IOException {
		String coords = readNestedContents(reader);
		Scanner s = new Scanner(coords);
		double x,y;

		try {
			x = s.nextDouble();
			y = s.nextDouble();
		} catch (RuntimeException e) {
			throw new IOException("Bad coordinate values: '" + coords + "'");
		}

		return new Coord(x,y);
	}

	/**
	 * Reads and skips all characters until character "until" is read or
	 * end of stream is reached. Also the expected character is discarded.
	 * @param r Reader to read characters from
	 * @param until What character to expect
	 * @throws IOException
	 */
	protected void skipUntil(Reader r, char until) throws IOException {
		char c;
		do {
			c = (char)r.read();
		} while (c != until && c != (char)-1);
	}

	/**
	 * Skips all consecutive whitespace characters from reader
	 * @param r Reader where the whitespace is skipped
	 * @return First non-whitespace character read from the reader
	 * @throws IOException
	 */
	protected char skipAllWhitespace(Reader r) throws IOException {
		char c;
		do {
			c = (char)r.read();
		} while (Character.isWhitespace(c) && c != (char)-1);

		return c;
	}

	/**
	 * Reads everything from the first opening parenthesis until line that
	 * ends to a closing parenthesis and returns the contents in one string
	 * @param r Reader to read the input from
	 * @return The text between the parentheses
	 */
	public String readNestedContents(Reader r) throws IOException {
		StringBuffer contents = new StringBuffer();
		int parOpen; // nrof open parentheses
		char c = '\0';

		skipUntil(r,'(');
		parOpen = 1;

		while (c != (char)-1 && parOpen > 0) {
			c = (char)r.read();
			if (c == '(') {
				parOpen++;
			}
			if (c == ')') {
				parOpen--;
			}
			if (Character.isWhitespace(c)) {
				c = ' '; // convert all whitespace to basic space
			}
			contents.append(c);
		}

		contents.deleteCharAt(contents.length()-1);	// remove last ')'
		return contents.toString();
	}

	/**
	 * Returns nested contents from the reader given at init
	 * @return nested contents from the reader given at init
	 * @throws IOException
	 * @see #readNestedContents(Reader)
	 */
	public String readNestedContents() throws IOException {
		return readNestedContents(reader);
	}

	/**
	 * Parses coordinate tuples from "LINESTRING" lines
	 * @param line String that contains the whole "LINESTRING"'s content
	 * @return List of coordinates parsed from the linestring
	 */
	protected List<Coord> parseLineString(String line) {
		List<Coord> coords = new ArrayList<Coord>();
		Scanner lineScan;
		Scanner tupleScan;
		double x,y;
		Coord c;

		lineScan = new Scanner(line);
		lineScan.useDelimiter(",");

		while (lineScan.hasNext()) {
			tupleScan = new Scanner(lineScan.next());
			x = Double.parseDouble(tupleScan.next());
			y = Double.parseDouble(tupleScan.next());
			c = new Coord(x,y);

			coords.add(c);
		}

		return coords;
	}

	/**
	 * Returns true if the whole file has been read
	 * @return true if the whole file has been read
	 */
	protected boolean isDone() {
		return this.done;
	}

	/**
	 * Sets the "is file read" state
	 * @param done If true, reading is done
	 */
	protected void setDone(boolean done) {
		this.done = done;
	}

}
