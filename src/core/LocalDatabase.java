package core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

    /* Parameters for utility function. */
    private static final double DEFAULT_BASE = 2;
    private static final double SLOW_AGING_BASE = 1.1;
    private static final double EQUAL_WEIGHT = 0.5;
    private static final double SMALLER_WEIGHT = 0.3;
    private static final double SLOWER_DECREASE_DIVISOR = 2;

    /** Total size of the database in bytes. */
    private int totalSize;
    /** Size used by stored data. */
    private int usedSize;

    /** All stored data */
    private List<DisasterData> dataList = new ArrayList<>();

    /**
     * Initializes a new instance of the {@link LocalDatabase} class.
     *
     * @param totalSize Size of the database in bytes.
     */
    public LocalDatabase(int totalSize) {
        this.totalSize = totalSize;
    }

    /**
     * Adds a dataItem to the database and
     * checks whether any old data needs to be deleted.
     *
     * @param newDataItem The data item to add.
     * @param location The current location, important for deletion check.
     * @param time The current time, important for deletion check.
     */
    public void add (DisasterData newDataItem, Coord location, double time){
        this.dataList.add(newDataItem);
        this.usedSize += newDataItem.getSize();
        this.deleteIrrelevantData(location, time);
    }

    /**
     * Checks all data items whether their utility
     * is below the threshold and removes them if it is.
     *
     * @param location The current location.
     * @param time The current time.
     */
    private void deleteIrrelevantData(Coord location, double time) {
        double deletionThreshold = this.computeDeletionThreshold();
        for (Iterator<DisasterData> dataIterator = this.dataList.iterator(); dataIterator.hasNext();) {
            DisasterData dataItem = dataIterator.next();
            if (LocalDatabase.computeUtility(dataItem, location, time) < deletionThreshold){
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
     * Returns all data that has at least the specified utility if utility is computed w. r. t. the given location and
     * time.
     *
     * @param minUtility The minimum utility the data has to have for it to be returned.
     * @param location The location to use for utility computation.
     * @param time The time to use for utility computation.
     * @return All data with utility at least the given threshold.
     */
    public List<DisasterData> getAllDataWithMinimumUtility(double minUtility, Coord location, double time){
        List<DisasterData> dataWithMinUtility = new ArrayList<>();
        for (DisasterData dataItem : this.dataList) {
            if (LocalDatabase.computeUtility(dataItem, location, time) >= minUtility){
                dataWithMinUtility.add(dataItem);
            }
        }
        return dataWithMinUtility;
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
        double distance = dataItem.getLocation().distance(location);
        //How long it has been since the data item has been created
        //The older an item is, the lower its utility
        double age = time - dataItem.getCreation();

        /* Compute utility. */
        return alpha * Math.pow(DEFAULT_BASE, -(distance/SLOWER_DECREASE_DIVISOR)) + (1-alpha) * Math.pow(gamma,-age);
    }
}
