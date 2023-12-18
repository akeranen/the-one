package movement;

import core.*;
import util.EmergencyExitHandler;
import util.Exit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentMovement extends MovementModel {

    public static final String EMERGENCY_TYPE_SETTING = "emergencyType";
    private static final int LECTURE_BLOCK_LENGTH = 240;
    private static final int LECTURE_BLOCK_OFFSET_LENGTH = 30;

    public static List<Exit> EXITS = EmergencyExitHandler.getInstance().getEmergencyExits();

    private static Map<String, Coord> ROOMS;
    static {
        ROOMS = new HashMap<>();
        ROOMS.put("HS1 01", new Coord(320, 120));
        ROOMS.put("HS1 02", new Coord(330, 132));

        ROOMS.put("HS2", new Coord(307.65,161.60));
        ROOMS.put("HS3", new Coord(262.48,159.21));

        ROOMS.put("Tisch 01", new Coord(155.24,122));
        ROOMS.put("Tisch 02", new Coord(173.24,122));
        ROOMS.put("Tisch 03", new Coord(230.24,119));
        ROOMS.put("Tisch 04", new Coord(258.24,122));
        ROOMS.put("Tisch 05", new Coord(217,139));
        ROOMS.put("Tisch 06", new Coord(197,137));

        ROOMS.put("Bib 01", new Coord(90, 140));
        ROOMS.put("Bib 02", new Coord(50, 140));
        ROOMS.put("Bib 03", new Coord(50, 175));

        ROOMS.put("Rechnerhalle 01", new Coord(250, 100));
        ROOMS.put("Rechnerhalle 02", new Coord(225, 100));

        ROOMS.put("SeminarRaum 01", new Coord(77,100));
        ROOMS.put("SeminarRaum 02", new Coord(166,158));

        ROOMS.put("Finger 13", new Coord(50, 35));
        ROOMS.put("Finger 11", new Coord(103, 50));
        ROOMS.put("Finger 09", new Coord(158.87, 50));
        ROOMS.put("Finger 07", new Coord(211,60));
        ROOMS.put("Finger 05", new Coord(265,70));

        ROOMS.put("Finger 04", new Coord(330, 205));
        ROOMS.put("Finger 06", new Coord(286, 190));
        ROOMS.put("Finger 08", new Coord(233,192));
        ROOMS.put("Finger 10", new Coord(188,191));
        ROOMS.put("Finger 12", new Coord(142,163));
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
                path.setSpeed(8.5);
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
        path.setSpeed(10);
        return path;
    }

    private Path getPathToNearestExit() {
        Exit nearestExit = getClosestExit(host.getLocation());
        Path path = calculateShortestPath(host.getLocation(), nearestExit.getCoord());
        path.setSpeed(10);
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


