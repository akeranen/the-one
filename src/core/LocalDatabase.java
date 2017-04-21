package core;

import util.Tuple;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Local database which stores {@link DisasterData} along with
 * the size of the database. Offers function to delete old and far away data
 * while dynamically setting the deletion threshold depending on how full the
 * database is.
 *
 * Created by melanie on 07.04.17.
 */
public class LocalDatabase {
    private static final int CUBIC = 3;
    private static final int METERS_IN_KILOMETER = 1000;
    private static final int SECONDS_IN_HOUR = 3600;

    /* Parameters for utility function. */
    private static final double DEFAULT_BASE = 2;
    private static final double SLOW_AGING_BASE = 1.1;
    private static final double EQUAL_WEIGHT = 0.5;
    private static final double SMALLER_WEIGHT = 0.3;
    private static final double SLOWER_DECREASE_DIVISOR = 2;

    /** Total size of the database in bytes. */
    private long totalSize;
    /** Size used by stored data. */
    private long usedSize;

    /** The database's owner. */
    private DTNHost owner;

    /** All stored data */
    private Set<DisasterData> data = new HashSet<>();

    /**
     * Initializes a new instance of the {@link LocalDatabase} class.
     *
     * @param owner The database's owner.
     * @param totalSize Size of the database in bytes.
     */
    public LocalDatabase(DTNHost owner, long totalSize) {
        this.owner = owner;
        this.totalSize = totalSize;
    }

    /**
     * Adds a dataItem to the database and
     * checks whether any old data needs to be deleted.
     *
     * @param newDataItem The data item to add.
     */
    public void add (DisasterData newDataItem){
        this.data.add(newDataItem);
        this.usedSize += newDataItem.getSize();
        this.deleteIrrelevantData();
    }

    /**
     * Checks all data items whether their utility
     * is below or equal to the threshold and removes them if it is.
     */
    private void deleteIrrelevantData() {
        double deletionThreshold = this.computeDeletionThreshold();

        double currentTime = SimClock.getTime();
        Coord currentLocation = this.owner.getLocation();

        for (Iterator<DisasterData> dataIterator = this.data.iterator(); dataIterator.hasNext();) {
            DisasterData dataItem = dataIterator.next();
            if (LocalDatabase.computeUtility(dataItem, currentLocation, currentTime) <= deletionThreshold){
                this.usedSize -= dataItem.getSize();
                dataIterator.remove();
            }
        }
    }

    /**
     * Computes a memory-aware deletion threshold s.t. everything below will be deleted.
     *
     * @return A threshold between 0 and 1 that is 0 for empty memory and 1 for full memory.
     */
    private double computeDeletionThreshold() {
        double usedMemoryPercentage = (double)this.usedSize / this.totalSize;
        return Math.pow(usedMemoryPercentage, CUBIC);
    }

    /**
     * Returns all data which is not of type {@link DisasterData.DataType#MAP} and that has at least the specified
     * utility if utility is computed w. r. t. the current location and time.
     *
     * @param minUtility The minimum utility the data has to have for it to be returned.
     * @return All data items with their respective utility if the utility was greater or equal than the given
     * threshold.
     */
    public List<Tuple<DisasterData, Double>> getAllNonMapDataWithMinimumUtility(double minUtility){
        double currentTime = SimClock.getTime();
        Coord currentLocation = this.owner.getLocation();

        List<Tuple<DisasterData, Double>> dataWithMinUtility = new ArrayList<>();
        for (DisasterData dataItem : this.data) {
            if (dataItem.getType() != DisasterData.DataType.MAP) {
                double utility = LocalDatabase.computeUtility(dataItem, currentLocation, currentTime);
                if (utility >= minUtility) {
                    dataWithMinUtility.add(new Tuple<>(dataItem, utility));
                }
            }
        }
        return dataWithMinUtility;
    }

    /**
     * Returns all data which is of type {@link DisasterData.DataType#MAP}.
     *
     * @return All map data.
     */
    public List<DisasterData> getMapData() {
        List<DisasterData> mapData = new ArrayList<>();
        for (DisasterData dataItem : this.data) {
            if (dataItem.getType() == DisasterData.DataType.MAP) {
                mapData.add(dataItem);
            }
        }
        return mapData;
    }

    /**
     * Computes the utility value of a data item given the current location and time.
     *
     * @param dataItem The item to compute the utility value for.
     * @param location The current location.
     * @param time The current time.
     * @return The computed utility value, a value between 0 and 1.
     */
    private static double computeUtility(DisasterData dataItem, Coord location, double time) {
        // Factor how much distance influences the utility in comparison to age.
        double alpha = EQUAL_WEIGHT;
        // Factor how fast aging occurs, the higher gamma, the faster the aging.
        double gamma = DEFAULT_BASE;

        /* Adapt alpha and gamma depending on type. */
        switch (dataItem.getType()){
            case MAP:
                // For maps we just regard the distance, as map data does not become outdated
                // within the disaster time frame.
                alpha = 1;
                break;
            case MARKER:
                // For markers, both distance and age are important. -> Default values work fine.
                break;
            case SKILL:
                // For skills, the aging is slower, as skills will likely not fade
                // The importance on distance is also lower
                alpha = SMALLER_WEIGHT;
                gamma = SLOW_AGING_BASE;
                break;
            case RESOURCE:
                //For resources the importance of distance is lower
                alpha = SMALLER_WEIGHT;
                break;
            default:
                throw new UnsupportedOperationException("No implementation for data type " + dataItem.getType() + ".");
        }

        /* Determine properties to compute utility. */

        //Distance between data and current location
        //The farther away an item is, the lower its utility
        double distance = dataItem.getLocation().distance(location) / METERS_IN_KILOMETER;
        //How long it has been since the data item has been created
        //The older an item is, the lower its utility
        double age = (time - dataItem.getCreation()) / SECONDS_IN_HOUR;

        /* Compute utility. */
        return alpha * Math.pow(DEFAULT_BASE, -(distance/SLOWER_DECREASE_DIVISOR)) + (1-alpha) * Math.pow(gamma,-age);
    }
}
