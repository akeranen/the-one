package input;

import core.Coord;
import core.Settings;
import core.SettingsError;
import movement.GSIMMovementEngine;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.nio.file.StandardOpenOption.*;

/**
 * Provides an interface to communicate with an external GSIM process.
 * Through this connection GSIM can be used as an external movement engine
 * and/or for detecting interface contacts.
 */
public class GSIMConnector {
    /** Class name */
    public static final String NAME = "GSIMConnector";

    /** path of gsim executable/working directory -setting id ({@value})*/
    public static final String GSIM_DIRECTORY_S = "directory";
    /** name of gsim executable -setting id ({@value})*/
    public static final String GSIM_EXECUTABLE_S = "executable";
    /** additional arguments -setting id ({@value})*/
    public static final String ARGS_S = "additionalArgs";

    /** The gsim process */
    private Process gsim = null;
    /** Directory were to find the gsim executable */
    private File gsimDirectory = null;
    /** Name of the gsim executable */
    private String gsimExecutable = null;
    /** Additional arguments passed to the process (may be used of override arguments) */
    private String additionalArgs = null;
    /** Temporary directory for the pipes */
    private Path tempDir = Paths.get("/tmp");
    /** Communication pipes */
    private DataInputStream pipeIn = null;
    private DataOutputStream pipeOut = null;

    /** Can be used to override automatic pipe and Process creation.
     *   tempDir must be configured! */
    boolean debug = false;

    public enum Header {
        // Must be 0, because 0 is correctly read even if endianness is interpreted incorrectly.
        TestDataExchange(0),
        Shutdown(1),
        Move(2),
        SetPositions(3),
        GetPositions(4),
        CollisionDetection(5),
        ConnectivityDetection(6),
        Count(6);

        private final int id;
        private Header(int id) {
            this.id = id;
        }

        public static Header fromID(int id) {
            switch(id) {
                case 0:
                    return TestDataExchange;
                case 1:
                    return Shutdown;
                case 2:
                    return Move;
                case 3:
                    return SetPositions;
                case 4:
                    return GetPositions;
                case 5:
                    return CollisionDetection;
                case 6:
                    return ConnectivityDetection;
            }
            throw new IllegalArgumentException("Cannot convert id (" + id + ") to enum Header.");
        }
        public int ID() {
            return id;
        }
    }

    /**
     * Creates a new GSIMConnector based on a Settings object's settings.
     * @param s The Settings object where the settings are read from
     */
    public GSIMConnector(Settings s) {
        s.setNameSpace(GSIMMovementEngine.NAME);

        gsimDirectory = new File(s.getSetting(GSIM_DIRECTORY_S, "gsim"));
        gsimExecutable = s.getSetting(GSIM_EXECUTABLE_S, "gsim");
        additionalArgs = s.getSetting(ARGS_S, "");

        s.restoreNameSpace();
    }

    private void preparePipes() throws IOException, InterruptedException {
        // Create temporary directory
        tempDir = Files.createTempDirectory("gsim-connector-");

        // Create named pipes
        new ProcessBuilder()
                .directory(tempDir.toFile())
                .command("mkfifo", "gsim.in")
                .start()
                .waitFor(2, TimeUnit.SECONDS);

        new ProcessBuilder()
                .directory(tempDir.toFile())
                .command("mkfifo", "gsim.out")
                .start()
                .waitFor(2, TimeUnit.SECONDS);
    }

    /**
     * Starts the gsim process and opens the communication connection
     */
    public void init(int numEntities, int worldSizeX, int worldSizeY, int waypointBufferSize, double interfaceRange) {
        if (!debug) {
            // Prepare temporary directory and named pipes
            try {
                preparePipes();
            } catch (IOException | InterruptedException e) {
                System.err.println("Cannot prepare named pipes: " + e.getMessage());
                System.exit(1); // cannot recover
            }
        }

        // Start the gsim process
        List<String> args = new ArrayList<>();
        args.add("./" + gsimExecutable); // Executable
        args.add(String.format("--pipes=%s", tempDir.resolve("gsim")));
        args.add(String.format("--num-entities=%d", numEntities));
        args.add(String.format("--map-width=%d", worldSizeX));
        args.add(String.format("--map-height=%d", worldSizeY));
        args.add(String.format("--waypoint-buffer-size=%d", waypointBufferSize));
        args.add(String.format("--interface-range=%f", interfaceRange));
        args.addAll(Arrays.asList(additionalArgs.split(" ")));

        if (debug) {
            System.out.printf("Now start gsim with: '%s'\n", String.join(" ", args));
        } else {
            System.out.printf("Starting gsim with: '%s'\n", String.join(" ", args));
            ProcessBuilder builder = new ProcessBuilder();
            builder.directory(gsimDirectory);
            //builder.inheritIO();
            builder.redirectOutput(gsimDirectory.toPath().resolve("logs/console.log").toFile());
            builder.redirectError(gsimDirectory.toPath().resolve("logs/console.log").toFile());
            builder.command(args);

            try {
                gsim = builder.start();
            } catch (IOException e) {
                System.err.println("Cannot start gsim process: " + e.getMessage());
                System.exit(1); // cannot recover
            }
        }

        // Open pipes
        Path pipeOutFilepath = tempDir.resolve("gsim.in"); // our output pipe (aka gsim's input pipe)
        Path pipeInFilepath = tempDir.resolve("gsim.out"); // our input pipe (aka gsim's output pipe)

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

        if (debug) {
            // Test binary conversions (ex. endianness) works as expected across processes
            testDataExchange();
        }
    }

    /**
     * Closes the communication connection and shuts down the gsim process
     */
    public void fini() {
        // Ensure pipe is empty
        flushOutput();

        if (debug) return;

        try {
            gsim.waitFor(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            System.err.println("GSIM Process did not exit gracefully. => Killing it");
            gsim.destroyForcibly();
        }
        gsim = null;

        // Cleanup pipes and temporary directory
        try {
            pipeOut.close();
            pipeIn.close();

            // Recursively delete directory
            // Ref.: https://www.baeldung.com/java-delete-directory
            Files.walkFileTree(tempDir,
                    new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult postVisitDirectory(
                                Path dir, IOException exc) throws IOException {
                            Files.delete(dir);
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFile(
                                Path file, BasicFileAttributes attrs)
                                throws IOException {
                            Files.delete(file);
                            return FileVisitResult.CONTINUE;
                        }
                    });

        } catch (IOException e) {
            System.err.println("Cannot delete temporary directory: " + e.getMessage());
        }
        pipeIn = null;
        pipeOut = null;
        tempDir = null;
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

    public Coord readCoord() {
        float x = readFloat();
        float y = readFloat();
        return new Coord(x, y);
    }

    /**
     * This method is intended for debugging purposes.
     * It sends data back and forth between processes and checks whether they are
     * transmitted and converted as expected.
     */
    public void testDataExchange() {
        System.out.print("Testing pipe data exchange\n");
        writeHeader(GSIMConnector.Header.TestDataExchange);

        System.out.print("Testing int/uint32 exchange\n");
        // Send int value, expect value+1, send value+2
        int[] ivalues = {1, 257, 65793, 1073807617};
        for(int value : ivalues) {
            writeInt(value);
            flushOutput();
            System.out.printf("GSIMConnector.testDataExchange() Sent %d\n", value);
            int result = readInt();
            System.out.printf("GSIMConnector.testDataExchange() Received %d\n", result);
            if (result != value + 1) {
                System.err.printf("GSIMConnector.testDataExchange() failed: Expected %d, but got %d\n", value + 1, result);
                System.exit(1);
            }
            writeInt(result + 1);
            flushOutput();
            System.out.printf("GSIMConnector.testDataExchange() Sent %d\n", result + 1);
        }

        System.out.print("Testing float exchange\n");
        // Send float value, expect value*2, send value*4
        float[] fvalues = {1.0f, 0.25f, -0.25f};
        for (float value : fvalues) {
            writeFloat(value);
            flushOutput();
            System.out.printf("GSIMConnector.testDataExchange() Sent %f\n", value);
            float result = readFloat();
            System.out.printf("GSIMConnector.testDataExchange() Received %f\n", result);
            if (result != value * 2.0f) {
                System.err.printf("GSIMConnector.testDataExchange() failed: Expected %f, but got %f\n", value + 1, result);
                System.exit(1);
            }
            writeFloat(result * 2.0f);
            flushOutput();
            System.out.printf("GSIMConnector.testDataExchange() Sent %f\n", result * 2.0f);
        }

        System.out.print("Testing string exchange\n");
        // Send "foo", expect "foobar", send "foobarbaz"
        String value = "foo";
        writeString(value);
        flushOutput();
        System.out.printf("GSIMConnector.testDataExchange() Sent %s\n", value);
        String result = readString();
        System.out.printf("GSIMConnector.testDataExchange() Received %s\n", result);
        value += "bar";
        if (!result.equals(value)) {
            System.err.printf("GSIMConnector.testDataExchange() failed: Expected %s, but got %s\n", value, result);
            System.exit(1);
        }
        result += "baz";
        writeString(result);
        flushOutput();
        System.out.printf("GSIMConnector.testDataExchange() Sent %s\n", result);

        System.out.print("Testing pipe data exchange - DONE\n");
    }
}
