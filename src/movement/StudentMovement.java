package movement;

import core.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentMovement extends MovementModel {
    private static final int LECTURE_BLOCK_LENGTH = 240;
    private static final int LECTURE_BLOCK_OFFSET_LENGTH = 30;

    public static final List<Exit> EXITS = new ArrayList<>();

    private static Map<String, Coord> ROOMS;
    static {
        ROOMS = new HashMap<>();
        // TODO: add more rooms and adjust coords
        ROOMS.put("HS1", new Coord(310, 130));
        ROOMS.put("Bib", new Coord(90, 140));
        ROOMS.put("Rechnerhalle", new Coord(250, 100));
        ROOMS.put("Finger 13", new Coord(50, 35));
        ROOMS.put("Finger 04", new Coord(330, 205));
    }

    private ProhibitedPolygonRwp normalMovement;

    public StudentMovement(Settings settings) {
        super(settings);
        String[] exitNames = settings.getCsvSetting("StudentMovement.exitNames");
        for (String exitName : exitNames) {
            String[] coordStrings = settings.getCsvSetting("StudentMovement." + exitName, 2);
            Coord exitCoord = new Coord(Double.parseDouble(coordStrings[0]), Double.parseDouble(coordStrings[1]));
            EXITS.add(new Exit(exitCoord, exitName, 2));
        }
        this.normalMovement = new ProhibitedPolygonRwp(settings);
    }

    public StudentMovement(StudentMovement other) {
        super(other);
        this.normalMovement = new ProhibitedPolygonRwp(other.normalMovement);
    }

    @Override
    public Path getPath() {
        int nrOfMessages = this.getHost().getRouter().getNrofMessages();

        if (nrOfMessages > 0) {
            this.getHost().setEmergencyState();
        }

        return switch (this.getHost().getState()) {
            case NORMAL -> normalPath();
            case EMERGENCY -> emergencyPath();
        };
    }

    private Path normalPath() {
        DTNHost dtnHost = this.getHost();

        double currentTime = SimClock.getTime();
        boolean inLectureBlock = currentTime % LECTURE_BLOCK_LENGTH >= LECTURE_BLOCK_OFFSET_LENGTH && currentTime % LECTURE_BLOCK_LENGTH <= LECTURE_BLOCK_LENGTH - LECTURE_BLOCK_OFFSET_LENGTH;

        if (inLectureBlock) {
            if (dtnHost.isInLecture()) {
                // Student should stay in room during the lecture
                return null;
            } else {
                // Student should go to a lecture
                dtnHost.setInLecture(true);
                String roomName = ROOMS.keySet().stream().toList().get(rng.nextInt(ROOMS.size()));
                Path path = calculateShortestPath(dtnHost.getLocation(), ROOMS.get(roomName));
                System.out.println(dtnHost + " is going to " + roomName + " at " + currentTime);
                path.setSpeed(5);
                return path;
            }
        }
        dtnHost.setInLecture(false);
        // In between lectures we do rwp
        Path rwp = normalMovement.getPath();
        rwp.setSpeed(5.0);
        return rwp;
    }

    private Path emergencyPath() {
        DTNHost dtnHost = this.getHost();
        // Get closest Exit
        Exit closestExit = getClosestExit(dtnHost);
        Path pathToExit = calculateShortestPath(dtnHost.getLocation(), closestExit.getCoord());
        pathToExit.setSpeed(7.5);
        return pathToExit;
    }

    private Path calculateShortestPath(Coord start, Coord destination) {
        // TODO: Calculate path within prohibited polygon using a-star like algorithm
        Path path = new Path(1);
        path.addWaypoint(destination);
        return path;
    }

    private Exit getClosestExit(DTNHost dtnHost) {
        Coord currentLocation = dtnHost.getLocation();
        Exit closestExit = null;
        for (Exit exit : EXITS) {
            if (closestExit == null || currentLocation.distance(exit.getCoord()) < currentLocation.distance(closestExit.getCoord())) {
                closestExit = exit;
            }
        }
        assert closestExit != null : "There must be a closest exit, otherwise the host cannot escape!";
        return closestExit;
    }

    @Override
    public Coord getInitialLocation() {
        return normalMovement.getInitialLocation();
    }

    @Override
    public MovementModel replicate() {
        return new StudentMovement(this);
    }

    public static Map<String, Coord> getRooms() {
        return ROOMS;
    }

}


