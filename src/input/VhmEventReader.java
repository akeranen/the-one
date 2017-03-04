package input;

import core.SimError;

import javax.json.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Reader for {@link VhmEvent}s. This class reads {@link VhmEvent}s from a defined
 * file and provides an event queue
 * for the use with the ONE
 *
 * Created by Marius Meyer on 15.02.17.
 */
public class VhmEventReader implements ExternalEventsReader {

    public static final String READER_VERSION = "vhm_events_version";

    /**
     * The current version of the JSON event files.
     * This class will only read in files, where the vhm_events_version equals this value
     */
    public static final int CURRENT_VERSION = 1;



    /**
     * Reader to read events from file
     */
    private JsonReader reader;

    /**
     * FileReader for the use with the BufferedReader
     * Declared as local variable to close it in the close function.
     */
    private FileReader fileReader;

    /**
     * Is set to true the first time, events are read in from the file
     */
    private boolean allEventsRead;

    /**
     * Creates a new reader for a specified file
     *
     * @param eventFile file the reader should read in
     *
     */
    public VhmEventReader(File eventFile){
        if (!isVhmEventsFile(eventFile)){
            throw new SimError("VHM events file is not valid: " + eventFile.getAbsolutePath());
        }
        try{
            fileReader = new FileReader(eventFile);
            reader = Json.createReader(fileReader);
            allEventsRead = false;
        } catch (FileNotFoundException e) {
            throw new SimError(e);
        }
    }

    /**
     * Checks if a file is a JSON events file in a compatible version for this reader
     * @param eventFile the file that should be checked
     * @return true, if the file is an JSON event file
     */
    public static boolean isVhmEventsFile(File eventFile){
        boolean correct = false;
        try(FileReader fileReader = new FileReader(eventFile)){
            try (JsonReader jsonReader = Json.createReader(fileReader)){
            JsonObject jsonFile = (JsonObject) jsonReader.read();
            if (jsonFile.getJsonNumber(READER_VERSION).intValue() == CURRENT_VERSION){
                correct = true;
            }
        }} catch (Exception e) {
            // It is perfectly acceptable to not handle "e" here
            correct = false;
        }
        return correct;
    }

    @Override
    public List<ExternalEvent> readEvents(int nrof) {
        if (allEventsRead) {
            return new ArrayList<>();
        }
        List<VhmEvent> events = null;
        try {
            events = extractEvents(nrof);
            allEventsRead = true;
        } catch (IOException e) {
            throw new SimError("Events could not be extracted: "+e);
        }
        return generateEventList(events);
    }

    /**
     * Extracts the VhmEvents from the reader and returns them as a list
     *
     * @return a list of the {@link VhmEvent}s
     */
    private List<VhmEvent> extractEvents(int nrof) throws IOException{
        JsonStructure jsonFile = reader.read();
        List<VhmEvent> eventList = new ArrayList<>();
        if (jsonFile.getValueType() == JsonValue.ValueType.OBJECT){
            JsonObject root = (JsonObject) jsonFile;
            for (Map.Entry<String,JsonValue> entry : root.entrySet()){
                if (eventList.size() >= nrof){
                    break;
                }
                if (entry.getValue().getValueType() == JsonValue.ValueType.OBJECT){
                    eventList.add(new VhmEvent(entry.getKey(),(JsonObject) entry.getValue()));
                }
            }
        }
        else{
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
    private List<ExternalEvent> generateEventList(List<VhmEvent> events){
        List<ExternalEvent> allEvents = new ArrayList<>();
        for (VhmEvent ev : events){
            allEvents.add(new VhmEventStartEvent(ev));
            allEvents.add(new VhmEventEndEvent(ev));
        }
        Collections.sort(allEvents);
        return allEvents;
    }

    @Override
    public void close() {
        try {
            reader.close();
            fileReader.close();
        } catch (IOException e) {
            throw new SimError("Reader could not be closed: "+e);
        }
    }
}
