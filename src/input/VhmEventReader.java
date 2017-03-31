package input;

import core.SimError;

import javax.json.JsonReader;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.json.stream.JsonParsingException;
import java.io.File;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Reader for {@link VhmEvent}s. This class reads {@link VhmEvent}s from a defined
 * file and provides an event queue
 * for the use with the ONE
 * (Vhm is the abbreviation of VoluntaryHelperMovement)
 * <p>
 * Created by Marius Meyer on 15.02.17.
 */
public class VhmEventReader implements ExternalEventsReader {

    private static final String READER_VERSION = "vhm_events_version";

    /**
     * The current version of the JSON event files.
     * This class will only read in files, where the vhm_events_version equals this value
     */
    private static final int CURRENT_VERSION = 1;

    /**
     * Is set to true the first time, events are read in from the file
     */
    private boolean allEventsRead;

    /**
     * The file that contains the events that this VhmEventReader is supposed to read
     */
    private File eventFile;

    /**
     * Creates a new reader for a specified file
     *
     * @param eventFile file the reader should read in
     */
    public VhmEventReader(File eventFile) {
        if (!isVhmEventsFile(eventFile)) {
            throw new SimError("VHM events file is not valid: " + eventFile.getAbsolutePath());
        }
        this.eventFile = eventFile;
        allEventsRead = false;
    }

    /**
     * Checks if a file is a JSON events file in a compatible version for this reader
     *
     * @param eventFile the file that should be checked
     * @return true, if the file is an JSON event file
     */
    static boolean isVhmEventsFile(File eventFile) {
        boolean correct = false;
        try (BufferedReader fileReader = createReader(eventFile);
             JsonReader jsonReader = Json.createReader(fileReader)) {
            JsonObject jsonFile = (JsonObject) jsonReader.read();

            if (jsonFile.getJsonNumber(READER_VERSION) != null
                    && jsonFile.getJsonNumber(READER_VERSION).intValue() == CURRENT_VERSION) {
                correct = true;
            }
        } catch (IOException | JsonParsingException e) {
            // It is perfectly acceptable to not handle the exception here,
            // as exceptions can be accepted by the nature of this method.
            correct = false;
        }
        return correct;
    }

    /**
     * Creates a Buffered reader for a File. Should only be used for *.json Files containing VhmEvents.
     *
     * @param eventFile The file containing the VhmEvents that are to be read
     * @return A BufferedReader  for the specified File
     * @throws FileNotFoundException If the specified file could not be found
     */
    private static BufferedReader createReader(File eventFile) throws FileNotFoundException {
        FileInputStream fileInputStream = new FileInputStream(eventFile);
        return new BufferedReader(new InputStreamReader(fileInputStream,
                StandardCharsets.UTF_8));
    }

    /**
     * Read events from the reader
     *
     * @param nrof Maximum number of events to read
     * @return Events in a List
     */
    @Override
    public List<ExternalEvent> readEvents(int nrof) {
        if (allEventsRead) {
            return new ArrayList<>();
        }
        List<VhmEvent> events;
        try {
            events = extractEvents(nrof);
            allEventsRead = true;
        } catch (IOException e) {
            throw new SimError("Events could not be extracted: " + e);
        }
        return generateEventList(events);
    }

    /**
     * Extracts the VhmEvents from the reader and returns them as a list
     *
     * @return a list of the {@link VhmEvent}s
     */
    private List<VhmEvent> extractEvents(int nrof) throws IOException {
        JsonStructure jsonFile;
        try (BufferedReader fileReader = createReader(eventFile);
             JsonReader jsonReader = Json.createReader(fileReader)) {
            jsonFile = jsonReader.read();
        } catch (IOException e) {
            throw new SimError(e);
        }
        List<VhmEvent> eventList = new ArrayList<>();
        if (jsonFile.getValueType() == JsonValue.ValueType.OBJECT) {
            JsonObject root = (JsonObject) jsonFile;
            for (Map.Entry<String, JsonValue> entry : root.entrySet()) {
                if (eventList.size() >= nrof) {
                    break;
                }
                if (entry.getValue().getValueType() == JsonValue.ValueType.OBJECT) {
                    eventList.add(new VhmEvent(entry.getKey(), (JsonObject) entry.getValue()));
                }
            }
        } else {
            throw new SimError("Wrong JSON file format! Root value should be of type OBJECT. Aborting!");
        }
        return eventList;
    }

    /**
     * Takes a list of {@link VhmEvent}s and generates a event list usable for the one simulator.
     * Therefore, special start and end events are created for every VhmEvent
     *
     * @param events list of the {@link VhmEvent}s
     * @return A list of events where {@link VhmEvent}s start or end
     */
    private static List<ExternalEvent> generateEventList(List<VhmEvent> events) {
        List<ExternalEvent> allEvents = new ArrayList<>();
        for (VhmEvent ev : events) {
            allEvents.add(new VhmEventStartEvent(ev));
            allEvents.add(new VhmEventEndEvent(ev));
        }
        Collections.sort(allEvents);
        return allEvents;
    }

    /**
     * Closes this VhmEventReader.
     */
    @Override
    public void close() {
        //No need to do anything here, everything closeable is already closed directly after usage,
        //by making use of try with resources
    }
}
