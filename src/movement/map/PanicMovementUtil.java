package movement.map;

import core.Coord;

/**
 * 
 * @author Nils Weidmann
 * 
 * This Class provides some mathematical methods needed for the mobility model PanicMovement, as well as selecting
 * a destination node for the host 
 */
public final class PanicMovementUtil {

    private static final double RIGHT_ANGLE = 90.0;
    private static final double STRAIGHT_ANGLE = 180.0;

    /**
     * Private constructor to hide the implicit public one
     */
    private PanicMovementUtil() {

    }

    /**
     * Computes the nearest node on the map that is not in event direction as seen from the host,
     * i.e. the angle between eventLocation -> locationNode and
     * eventLocation -> returnNode should be either less than 90 and or more than 270 degrees.
     *
     * @param map The sim Map in which the destination is selected.
     * @param locationNode The MapNode that is closest to the corresponding host.
     * @param eventLocation The location of the disaster event.
     * @param safeRangeRadius The distance from the disaster event, from which on a host is safe.
     * @return The selected MapNode, that the host should flee to.
     */
    public static MapNode selectDestination(SimMap map, MapNode locationNode, Coord eventLocation,
                                            double safeRangeRadius) {
        double distance = eventLocation.distance(locationNode.getLocation());
        if (distance >= safeRangeRadius) {
            // The host is within the safe area or the host is not concerned by the event
            return locationNode;
        }

        return getBestNode(map, locationNode, eventLocation, safeRangeRadius);
    }

    /**
     * Computes the nearest Node in the target zone
     *
     * @param map          Map that the computation refers to
     * @param locationNode current location of the host
     * @param eventLocation The location of the disaster event.
     * @param safeRangeRadius The distance from the disaster event, from which on a host is safe.
     * @return The closest node outside of the disasters safe range.
     */
    private static MapNode getBestNode(SimMap map, MapNode locationNode, Coord eventLocation, double safeRangeRadius) {
        MapNode nearestSafeNode = null;
        double shortestDistance = Double.MAX_VALUE;

        for (MapNode node : map.getNodes()) {
            double distanceBetweenNodeAndEvent = eventLocation.distance(node.getLocation());
            double distanceBetweenHostAndNode = locationNode.getLocation().distance(node.getLocation());
            if (distanceBetweenNodeAndEvent >= safeRangeRadius && (distanceBetweenHostAndNode < shortestDistance)
                    && !isInEventDirection(locationNode, node, eventLocation)) {
                nearestSafeNode = node;
                shortestDistance = distanceBetweenHostAndNode;
            }
        }

        // if no better node is found, the node can stay at the current location
        if (nearestSafeNode == null) {
            return locationNode;
        } else {
            return nearestSafeNode;
        }
    }

    /**
     * Computes if the target node is in event direction from the source node's point of view
     *
     * @param sourceNode current location of the node
     * @param targetNode potential target location
     * @param eventLocation The location of the disaster event.
     * @return true if the target node is in event direction from the source node's point of view, false otherwise
     */
    public static boolean isInEventDirection(MapNode sourceNode, MapNode targetNode, Coord eventLocation) {
        double angle;

        if (eventLocation.equals(targetNode.getLocation())) {
            // event = target --> IN event direction
            return true;
        } else if (eventLocation.equals(sourceNode.getLocation())) {
            // event = source --> NOT IN event direction
            return false;
        } else {
            angle = computeAngleBetween(sourceNode.getLocation(), eventLocation, targetNode.getLocation());
        }

        return Math.abs(angle - STRAIGHT_ANGLE) < RIGHT_ANGLE;
    }

    /**
     * Computes the scalar product between the vectors v1 (source -> target1) and v2 (source -> target2)
     *
     * @param target1 end point of the vector v1
     * @param source starting point of the vectors v1 and v2
     * @param target2 end point of the vector v2
     * @return the scalar product between the vectors v1 (source -> target1) and v2 (source -> target2)
     */
    private static double scalarProduct(Coord target1, Coord source, Coord target2) {
        double scalarProduct = 0;
        double[] v1 = {target1.getX() - source.getX(), target1.getY() - source.getY()};
        double[] v2 = {target2.getX() - source.getX(), target2.getY() - source.getY()};

        for (int i = 0; i < v1.length; i++) {
            scalarProduct += v1[i] * v2[i];
        }

        return scalarProduct;
    }

    /**
     * Computes the length product between the vectors v1 (source -> target1) and v2 (source -> target2)
     *
     * @param target1 end point of the vector v1
     * @param source starting point of the vectors v1 and v2
     * @param target2 end point of the vector v2
     * @return the length product between the vectors v1 (source -> target1) and v2 (source -> target2)
     */
    private static double lengthProduct(Coord target1, Coord source, Coord target2) {

        return source.distance(target1) * source.distance(target2);
    }

    /**
     * Computes the angle between the vectors v1 (source -> target1) and v2 (source -> target2).
     * In case one distance is equal to 0, a default of 180° is returned. As this case should be handled by the caller,
     * it should never occur.
     *
     * @param target1 end point of the vector v1
     * @param source Starting point of the vectors v1 and v2
     * @param target2 end point of the vector v2
     * @return the angle between the vectors v1 (source -> target1) and v2 (source -> target2).
     * 180 if at least one vector has a length of 0.
     */
    private static double computeAngleBetween(Coord target1, Coord source, Coord target2) {
        double scalarProduct = scalarProduct(target1, source, target2);
        double lengthProduct = lengthProduct(target1, source, target2);

        if (lengthProduct <= 0) {
            // This case avoids division by zero. Since this case is also handled at the caller, it should never happen
            return STRAIGHT_ANGLE;
        } else {
            return Math.acos(scalarProduct / lengthProduct) * STRAIGHT_ANGLE / Math.PI;
        }
    }
}
