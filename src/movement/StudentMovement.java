package movement;

import core.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentMovement extends MovementModel {

    public static final List<Exit> EXITS = new ArrayList<>();

    private static Map<String, Coord> ROOMS;
    static {
        ROOMS = new HashMap<>();
        // TODO: add more rooms and adjust coords
        ROOMS.put("HS1", new Coord(310, 130));
        ROOMS.put("Bib", new Coord(90, 140));
        ROOMS.put("Rechnerhalle", new Coord(250, 100));
    }
    private State state;

    private ProhibitedPolygonRwp normalMovement;

    public StudentMovement(Settings settings) {
        super(settings);
        this.state = State.NORMAL;
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
        this.state = other.state;
        this.normalMovement = new ProhibitedPolygonRwp(other.normalMovement);
        //this.rooms = new ArrayList<>(rooms);
    }

    @Override
    public Path getPath() {
        return null;
    }

    @Override
    public Path getPath(DTNHost dtnHost) {
        return switch (state) {
            case NORMAL -> normalPath(null);
            case EMERGENCY -> emergencyPath(dtnHost);
        };
    }

    private Path normalPath(DTNHost dtnHost) {
        // Get current time
//        double currentTime = SimClock.getTime();
//        if (currentTime % 120 < 15 || currentTime % 120 > 105) { //Time between lectures
//            // Set waypoint to a lecture room
//            // Set lecture mode
//            dtnHost.setInLecture(true);
//            Path path = new Path(1);
//            path.addWaypoint(ROOMS.values().stream().toList().get(rng.nextInt(ROOMS.size())));
//            return path;
//        }
//
//        Path newPath = new Path(1);
//        newPath.addWaypoint(new Coord(250, 100));
//        return newPath;
        return normalMovement.getPath();
    }

    private Path emergencyPath(DTNHost dtnHost) {
        // Get closest Exit
        Exit closestExit = getClosestExit(dtnHost);
        Path pathToExit = calculateShortestPath(dtnHost.getLocation(), closestExit.getCoord());
        return pathToExit;
    }

    private Path calculateShortestPath(Coord start, Coord destination) {
        // Calculate path within prohibited polygon
        return null;
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

    public enum State {
        NORMAL,
        EMERGENCY
    }

    public static Map<String, Coord> getRooms() {
        return ROOMS;
    }

}


