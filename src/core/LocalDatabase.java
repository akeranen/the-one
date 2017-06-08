package core;

import util.Tuple;

import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    /* Interval until utilities are recomputed in seconds */
    private static final int UTILITY_COMPUTATION_INTERVAL = 1;

    /* Parameters for utility function: Distance base. */
    private static final double DEFAULT_DISTANCE_BASE = 1.2;
    private static final double MORE_SENSITIVE_DISTANCE_BASE = 1.25;
    private static final double VERY_SENSITIVE_DISTANCE_BASE = 1.35;
    private static final double SLOWER_DECREASE_DIVISOR = 2;

    /* Parameters for utility function: Aging base. */
    private static final double DEFAULT_AGING_BASE = 1.025;
    private static final double ZERO_AGING_BASE = 1;
    private static final double VERY_SLOW_AGING_BASE = 1.001;
    private static final double SLOW_AGING_BASE = 1.005;


    /* Parameters for utility function: Aging stops. */
    private static final int HOURS_IN_WEEK = 168;
    private static final int HOURS_IN_TWO_AND_A_HALF_DAYS = 60;

    /** Total size of the database in bytes. */
    private long totalSize;
    /** Size used by stored data. */
    private long usedSize;

    /** The database's owner. */
    private DTNHost owner;

    /** All stored data with its cached utility (cache for performance reasons) */
    private HashMap<DisasterData, Double> data = new HashMap<>();

    /** Last sim time we recomputed the utilities */
    private double utilitiesLastComputed;

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
        double currentTime = SimClock.getTime();
        Coord currentLocation = this.owner.getLocation();
        this.data.put(newDataItem, computeUtility(newDataItem, currentLocation, currentTime));
        this.usedSize += newDataItem.getSize();
        this.deleteIrrelevantData();
    }

    /**
     * Checks all data items whether their utility
     * is below or equal to the threshold and removes them if it is.
     */
    private void deleteIrrelevantData() {
        double deletionThreshold = this.computeDeletionThreshold();

        recomputeUtilitiesIfNecessary();

        Iterator<Map.Entry<DisasterData, Double>> dataIterator = data.entrySet().iterator();
        while (dataIterator.hasNext()){
            Map.Entry<DisasterData, Double> dataWithUtility = dataIterator.next();
            if (dataWithUtility.getValue()<=deletionThreshold){
                this.usedSize -= dataWithUtility.getKey().getSize();
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
        return Math.pow(getUsedMemoryPercentage(), CUBIC);
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

        List<Tuple<DisasterData, Double>> dataWithMinUtility = new ArrayList<>();

        recomputeUtilitiesIfNecessary();

        for (Map.Entry<DisasterData, Double> dataWithUtility: data.entrySet()){

            if (dataWithUtility.getValue()>=minUtility &&
                    dataWithUtility.getKey().getType() != DisasterData.DataType.MAP){
                dataWithMinUtility.add(new Tuple<>(dataWithUtility.getKey(), dataWithUtility.getValue()));
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

        recomputeUtilitiesIfNecessary();

        List<DisasterData> mapData = new ArrayList<>();
        for (Map.Entry<DisasterData, Double> dataWithUtility: data.entrySet()){
            if (dataWithUtility.getKey().getType() == DisasterData.DataType.MAP){
                mapData.add(dataWithUtility.getKey());
            }
        }
        return mapData;
    }

    /**
     * Returns the total database size.
     *
     * @return The total database size.
     */
    public long getTotalSize() {
        return this.totalSize;
    }

    /**
     * Recomputes the utilities if they are needed and at most every {@link LocalDatabase#UTILITY_COMPUTATION_INTERVAL}
     * seconds in sim time.
     * The reason the utilities are cached and not computed every time is performance.
     * A host may meet multiple neighbors it may send data within short time.
     * Neither the time nor the location of the host could have changed much,
     * so utilities can be reused.
     */
    private void recomputeUtilitiesIfNecessary(){
        double currentTime = SimClock.getTime();

        if ((currentTime- utilitiesLastComputed)>= UTILITY_COMPUTATION_INTERVAL){

            Coord currentLocation = this.owner.getLocation();
            for (Map.Entry<DisasterData, Double> dataWithUtility: data.entrySet()){
                double utility = computeUtility(dataWithUtility.getKey(), currentLocation, currentTime);
                data.put(dataWithUtility.getKey(), utility);
            }
            utilitiesLastComputed =currentTime;
        }
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
        // Factor how sensitive the utility function is to distance. The higher the beta, the more sensitive.
        double beta = DEFAULT_DISTANCE_BASE;
        // Factor how fast aging occurs, the higher gamma, the faster the aging.
        double gamma = DEFAULT_AGING_BASE;
        // Number of hours after which aging stops.
        double maxAging = HOURS_IN_WEEK;

        /* Adapt alpha and gamma depending on type. */
        switch (dataItem.getType()){
            case MAP:
                // For maps we just regard the distance, as map data does not become outdated
                // within the disaster time frame.
                gamma = ZERO_AGING_BASE;
                maxAging = 0;
                break;
            case MARKER:
                // For markers, both distance and age are important.
                // We assume that markers age for a while, but markers older than 2.5 days are not more or less useful
                // depending on age only.
                beta = MORE_SENSITIVE_DISTANCE_BASE;
                maxAging = HOURS_IN_TWO_AND_A_HALF_DAYS;
                break;
            case SKILL:
                // For skills, the aging is slower, as skills will likely not fade
                // The importance on distance is high because you won't ask people for help that are very far away.
                beta = VERY_SENSITIVE_DISTANCE_BASE;
                gamma = VERY_SLOW_AGING_BASE;
                break;
            case RESOURCE:
                // Resources also need some time to age, but may age faster than skills.
                gamma = SLOW_AGING_BASE;
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
        return Math.pow(beta, -(distance/SLOWER_DECREASE_DIVISOR)) * Math.pow(gamma,-Math.min(maxAging, age));
    }

    /**
     * Computes statistics about the utility of all {@link DisasterData} items in this database
     * @return statistics about the utility across all {@link DisasterData} items
     */
    public DoubleSummaryStatistics getDataUtilityStatistics(){
        recomputeUtilitiesIfNecessary();
        return data.values().stream().collect(Collectors.summarizingDouble(Double::doubleValue));
    }

    /**
     * Computes statistics about the age of all {@link DisasterData} items in this database
     * which are not of {@link DisasterData.DataType} MAP
     * @return statistics about the age across all non-map {@link DisasterData} items
     */
    public DoubleSummaryStatistics getDataAgeStatistics(){
        double currentTime = SimClock.getTime();
        List<Double> ages = new ArrayList<>();
        for (DisasterData dataItem : data.keySet()){
            if (dataItem.getType()==DisasterData.DataType.MAP){
                continue;
            }
            ages.add(currentTime-dataItem.getCreation());
        }
        return ages.stream().mapToDouble(Double::doubleValue).summaryStatistics();
    }

    /**
     * Computes statistics about the distance of {@link DisasterData} items in this database to the host
     * @return statistics about the distance across all {@link DisasterData} items
     */
    public DoubleSummaryStatistics getDataDistanceStatistics(){
        Coord currentLocation = this.owner.getLocation();
        List<Double> distances = new ArrayList<>();
        for (DisasterData dataItem : data.keySet()){
            distances.add(dataItem.getLocation().distance(currentLocation));
        }
        return distances.stream().mapToDouble(Double::doubleValue).summaryStatistics();
    }

    /**
     * Percentage of memory for {@link DisasterData} which is used as a value between 0 and 1
     * @return percentage of available memory for {@link DisasterData} which is used as a value between 0 and 1
     */
    public double getUsedMemoryPercentage(){
        return (double)this.usedSize / this.totalSize;
    }

    /**
     * Returns the ratio of {@link DisasterData} items in the database per
     * {@link DisasterData.DataType} to the total number of items.
     * @return A hashmap containing a ratio between 0 and 1 for each {@link DisasterData.DataType}
     */
    public Map<DisasterData.DataType, Double> getRatioOfItemsPerDataType(){
        EnumMap<DisasterData.DataType, Double> ratioPerType = new EnumMap<>(DisasterData.DataType.class);
        for (DisasterData.DataType type : DisasterData.DataType.values()){
            ratioPerType.put(type, 0.0);
        }
        int totalNoOfItems = data.size();
        //If we have no items, we can stop
        if (totalNoOfItems >0){
            //Count the number of items per DataType
            for (DisasterData dataItem : data.keySet()){
                ratioPerType.put(dataItem.getType(), ratioPerType.get(dataItem.getType())+1);
            }
            //Calculate the ratio by dividing by the total number of items
            for (DisasterData.DataType type : DisasterData.DataType.values()) {
                ratioPerType.put(type, ratioPerType.get(type) / totalNoOfItems);
            }
        }
        return ratioPerType;
    }

}