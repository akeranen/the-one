package input;

import core.Coord;
import core.Settings;
import core.SettingsError;
import movement.MovementEngine;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths
;import static java.nio.file.StandardOpenOption.*;

/**
 * Provides an interface to communicate with an external MSIM process.
 * Through this connection MSIM can be used as an external movement engine
 * and/or for detecting interface contacts.
 */
public class MSIMConnector {
    /** movement engines -setting id ({@value})*/
    public static final String NAME = "MSIMConnector";

    /** prefix of communication pipe paths -setting id ({@value})*/
    public static final String PIPES_S = "pipes";
    /** path of input communication pipe -setting id ({@value})*/
    public static final String IN_PIPE_S = "inPipe";
    /** path of output communication pipe -setting id ({@value})*/
    public static final String OUT_PIPE_S = "outPipe";

    /** Communication pipes */
    DataInputStream pipeIn = null;
    DataOutputStream pipeOut = null;

    public enum Header {
        Ok(0),
        Initialize(1),
        Move(2),
        GetEntityPositions(3),
        Count(4);

        private final int id;
        private Header(int id) {
            this.id = id;
        }

        public static Header fromID(int id) {
            switch(id) {
                case 0:
                    return Ok;
                case 1:
                    return Initialize;
                case 2:
                    return Move;
                case 3:
                    return GetEntityPositions;
            }
            throw new IllegalArgumentException("Cannot convert id (" + id + ") to enum Header.");
        }
        public int ID() {
            return id;
        }
    }

    /**
     * Creates a new MSIMConnector based on a Settings object's settings.
     * @param s The Settings object where the settings are read from
     */
    public MSIMConnector(Settings s) {
        s.setNameSpace(MovementEngine.MOVEMENT_ENGINE_NS);

        Path pipeInFilepath = null; // our input pipe (aka msim's output pipe)
        Path pipeOutFilepath = null; // our output pipe (aka msim's input pipe)
        if (s.contains(PIPES_S)) {
            pipeInFilepath = Paths.get(s.getSetting(PIPES_S) + ".out");
            pipeOutFilepath = Paths.get(s.getSetting(PIPES_S) + ".in");
        }
        if (s.contains(IN_PIPE_S)) {
            pipeInFilepath = Paths.get(s.getSetting(IN_PIPE_S));
        }
        if (s.contains(OUT_PIPE_S)) {
            pipeOutFilepath = Paths.get(s.getSetting(OUT_PIPE_S));
        }
        if (pipeInFilepath == null) {
            throw new SettingsError("MSIMMovementEngine is used but setting MovementEngine.pipeIn is missing.");
        }
        if (pipeOutFilepath == null) {
            throw new SettingsError("MSIMMovementEngine is used but setting MovementEngine.pipeOut is missing.");
        }

        // Open pipes
        System.out.printf("Opening output pipe (%s)\n", pipeOutFilepath);
        try {
            pipeOut = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(pipeOutFilepath, WRITE, APPEND)));
        } catch (IOException ex) {
            throw new SettingsError("Cannot open pipe (" + pipeOutFilepath + ") for writing", ex);
        }

        System.out.printf("Opening input pipe (%s)\n", pipeInFilepath);
        try {
            pipeIn = new DataInputStream(new BufferedInputStream(Files.newInputStream(pipeInFilepath, READ)));
        } catch (IOException ex) {
            throw new SettingsError("Cannot open pipe (" + pipeInFilepath + ") for reading", ex);
        }

        s.restoreNameSpace();

        testDataExchange();
    }

    private void error_handler() {
        System.err.println("Broken Pipe: Communication failed. The other end closed the pipe; probably because of an error.");
        System.exit(1); // cannot recover
    }

    public void flushOutput() {
        try {
            pipeOut.flush();
        } catch (IOException ex) {
            error_handler();
        }
    }

    public void writeShort(int value) {
        assert(value >= Short.MIN_VALUE && value <= Short.MAX_VALUE);
        try {
            pipeOut.writeShort(value);
        } catch (IOException ex) {
            error_handler();
        }
    }

    public int readShort() {
        try {
            return pipeIn.readShort();
        } catch (IOException ex) {
            error_handler();
        }
        return 0; // never reached..
    }

    public void writeInt(int value) {
        try {
            pipeOut.writeInt(value);
        } catch (IOException ex) {
            error_handler();
        }
    }

    public int readInt() {
        try {
            return pipeIn.readInt();
        } catch (IOException ex) {
            error_handler();
        }
        return 0; // never reached..
    }

    public void writeFloat(float value) {
        try {
            pipeOut.writeFloat(value);
        } catch (IOException ex) {
            error_handler();
        }
    }

    public float readFloat() {
        try {
            return pipeIn.readFloat();
        } catch (IOException ex) {
            error_handler();
        }
        return 0.0f; // never reached..
    }

    public void writeString(String string) {
        try {
            pipeOut.writeUTF(string);
        } catch (IOException ex) {
            error_handler();
        }
    }

    public String readString() {
        try {
            return pipeIn.readUTF();
        } catch (IOException ex) {
            error_handler();
        }
        return ""; // never reached..
    }

    public void writeHeader(Header header) {
        writeShort(header.id);
    }

    public Header readHeader() {
        int id = readShort();
        assert(id < Header.Count.id);
        return Header.fromID(id);
    }

    public void writeCoord(Coord coord) {
        // Note: Precision-loss is expected. We only check that values are within bounds.
        assert(coord.getX() >= Float.MIN_VALUE && coord.getX() <= Float.MAX_VALUE);
        assert(coord.getY() >= Float.MIN_VALUE && coord.getY() <= Float.MAX_VALUE);

        writeFloat((float) coord.getX());
        writeFloat((float) coord.getY());
    }

    public void readCoordInto(Coord coord) {
        float x = readFloat();
        float y = readFloat();
        coord.setLocation(x, y);
    }

    /**
     * This method is intended for debugging purposes.
     * It sends data back and forth between processes and checks whether they are
     * transmitted and converted as expected.
     */
    public void testDataExchange() {
        System.out.print("Testing pipe data exchange\n");

        System.out.print("Testing int/uint32 exchange\n");
        // Send int value, expect value+1, send value+2
        int[] ivalues = {0, 257, 65793, 1073807617};
        for(int value : ivalues) {
            writeInt(value);
            flushOutput();
            System.out.printf("MSIMConnector.testDataExchange() Sent %d\n", value);
            int result = readInt();
            System.out.printf("MSIMConnector.testDataExchange() Received %d\n", result);
            if (result != value + 1) {
                System.err.printf("MSIMConnector.testDataExchange() failed: Expected %d, but got %d\n", value + 1, result);
            }
            writeInt(result + 1);
            flushOutput();
            System.out.printf("MSIMConnector.testDataExchange() Sent %d\n", result + 1);
        }

        System.out.print("Testing float exchange\n");
        // Send float value, expect value*2, send value*4
        float[] fvalues = {1.0f, 0.25f, -0.25f};
        for (float value : fvalues) {
            writeFloat(value);
            flushOutput();
            System.out.printf("MSIMConnector.testDataExchange() Sent %f\n", value);
            float result = readFloat();
            System.out.printf("MSIMConnector.testDataExchange() Received %f\n", result);
            if (result != value * 2.0f) {
                System.err.printf("MSIMConnector.testDataExchange() failed: Expected %f, but got %f\n", value + 1, result);
            }
            writeFloat(result * 2.0f);
            flushOutput();
            System.out.printf("MSIMConnector.testDataExchange() Sent %f\n", result * 2.0f);
        }

        System.out.print("Testing string exchange\n");
        // Send "foo", expect "foobar", send "foobarbaz"
        String value = "foo";
        writeString(value);
        flushOutput();
        System.out.printf("MSIMConnector.testDataExchange() Sent %s\n", value);
        String result = readString();
        System.out.printf("MSIMConnector.testDataExchange() Received %s\n", result);
        value += "bar";
        if (!result.equals(value)) {
            System.err.printf("MSIMConnector.testDataExchange() failed: Expected %s, but got %s\n", value, result);
        }
        result += "baz";
        writeString(result);
        flushOutput();
        System.out.printf("MSIMConnector.testDataExchange() Sent %s\n", result);

        System.out.print("Testing pipe data exchange - DONE\n");
    }
}
