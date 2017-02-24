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

/**
 * Reader for VHMEvents. This class reads VHMEvents from a defined
 * file and provides an event queue
 * for the use with the ONE
 *
 * Created by Marius Meyer on 15.02.17.
 */
public class VHMEventReader implements ExternalEventsReader {

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
    public VHMEventReader(File eventFile){
        if (!isVHMEventsFile(eventFile)){
            throw new SimError("VHM events file is not valid: " + eventFile.getAbsolutePath());
        }
        try{
            fileReader = new FileReader(eventFile);
            reader = Json.createReader(fileReader);
            allEventsRead = false;
        } catch (FileNotFoundException e) {
            throw new SimError(e.getMessage());
        }
    }

    /**
     * Checks if a file is a JSON events file
     * @param eventFile the file that should be checked
     * @return true, if the file is an JSON event file
     */
    public static boolean isVHMEventsFile(File eventFile){
        FileReader fileReader = null;
        JsonReader jsonReader = null;
        boolean correct = false;
        try{
            fileReader = new FileReader(eventFile);
            jsonReader = Json.createReader(fileReader);
            JsonObject jsonFile = (JsonObject) jsonReader.read();
            if (((JsonNumber)jsonFile.get(READER_VERSION)).intValue() == CURRENT_VERSION){
                correct = true;
            }
            jsonReader.close();
            fileReader.close();
        } catch (Exception e) {
            if(jsonReader != null)
                jsonReader.close();
            return false;
        }
        return correct;
    }

    @Override
    public List<ExternalEvent> readEvents(int nrof) {
        if (allEventsRead) {
            return new ArrayList<>();
        }
        List<VHMEvent> events = null;
        try {
            events = extractEvents(nrof);
            allEventsRead = true;
        } catch (IOException e) {
            throw new SimError(e.getMessage());
        }
        return generateEventList(events);
    }

    /**
     * Extracts the VHMEvents from the reader and returns them as a list
     *
     * @return a list of the VHMEvents
     */
    private List<VHMEvent> extractEvents(int nrof) throws IOException{
        JsonStructure jsonFile = reader.read();
        List<VHMEvent> eventList = new ArrayList<>();
        if (jsonFile.getValueType() == JsonValue.ValueType.OBJECT){
            JsonObject root = (JsonObject) jsonFile;
            for (String name : root.keySet()){
                if (eventList.size() >= nrof){
                    break;
                }
                if (root.get(name).getValueType() == JsonValue.ValueType.OBJECT){
                    eventList.add(new VHMEvent(name,(JsonObject) root.get(name)));
                }
            }
        }
        else{
            throw new SimError("Wrong JSON file format! Root value should be of type OBJECT. Aborting!");
        }
        return eventList;
    }

    /**
     * Takes a list of VHMEvents and generates a event list usable for the one simulator.
     * Therefore, special start and end events are created for every VHMEvent
     *
     * @param events list of the VHMEvents
     * @return A list of events where VHMEvents start or end
     */
    private List<ExternalEvent> generateEventList(List<VHMEvent> events){
        List<ExternalEvent> allEvents = new ArrayList<>();
        for (VHMEvent ev : events){
            allEvents.add(new VHMEventStartEvent(ev));
            allEvents.add(new VHMEventEndEvent(ev));
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
            throw new SimError(e.getMessage());
        }
    }
}
