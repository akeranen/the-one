package movement;

import core.*;
import util.EmergencyExitHandler;
import util.Exit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StudentMovement extends MovementModel {

    public static final String EMERGENCY_TYPE_SETTING = "emergencyType";
    private static final int LECTURE_BLOCK_LENGTH = 240;
    private static final int LECTURE_BLOCK_OFFSET_LENGTH = 30;

    public static List<Exit> EXITS = EmergencyExitHandler.getInstance().getEmergencyExits();

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

    private final String emergencyType;

    private ProhibitedPolygonRwp normalMovement;

    public StudentMovement(Settings settings) {
        super(settings);
        this.normalMovement = new ProhibitedPolygonRwp(settings);
        this.emergencyType = settings.getSetting(EMERGENCY_TYPE_SETTING);
    }

    public StudentMovement(StudentMovement other) {
        super(other);
        this.normalMovement = new ProhibitedPolygonRwp(other.normalMovement);
        this.emergencyType = other.emergencyType;
    }

    @Override
    public Path getPath() {
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
                String roomName = new ArrayList<>(ROOMS.keySet()).get(rng.nextInt(ROOMS.size()));
                Path path = calculateShortestPath(dtnHost.getLocation(), ROOMS.get(roomName));
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
        if (!host.isInBuilding()) {
            return null;
        }
        host.setInBuilding(false);
        return switch (emergencyType) {
            case "randomExit" -> getPathToRandomExit();
            case "nearestExit" -> getPathToNearestExit();
            default -> throw new SettingsError(emergencyType + " is not an allowed emergency type.");
        };
    }

    private Path getPathToRandomExit() {
        Exit randomExit = EXITS.get(rng.nextInt(0, EXITS.size()));
        Path path = calculateShortestPath(host.getLocation(), randomExit.getCoord());
        path.setSpeed(7);
        return path;
    }

    private Path getPathToNearestExit() {
        Exit nearestExit = getClosestExit(host.getLocation());
        Path path = calculateShortestPath(host.getLocation(), nearestExit.getCoord());
        path.setSpeed(7);
        return path;
    }

    private Path calculateShortestPath(Coord start, Coord destination) {
        // TODO: Calculate path within prohibited polygon using a-star like algorithm
        Path path = new Path(1);
        path.addWaypoint(destination);
        return path;
    }

    private Exit getClosestExit(Coord currentLocation) {
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


