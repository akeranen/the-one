package report;

import applications.DatabaseApplication;
import core.DTNHost;
import core.DisasterData;
import routing.util.DatabaseApplicationUtil;

import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Reports statistics on database synchronization.
 *
 * In preset intervals, it is checked how full the
 * databases are and how distant/old/useful data items are.
 * It is also checked which ratio of the data items is of
 * each data type (concerning the number of items, not their size).
 * Age is only checked for non-map data as map data does not age in current scenarios.
 *
 *
 * Format is as follows (excluding some line breaks):
 *
 * Data sync stats for scenario  dataSyncReport
 *
 * sim_time: 600.2000, avg_used_mem: 0.1448%, max_used_mem: 0.9012354941187346%,
 * med_avg_data_util: 0.7751, avg_data_util: 0.6233,
 * med_avg_data_age: 457.1799, avg_data_age: 374.9272, med_max_data_age: 600.2000,
 * med_avg_data_dist: 1915.9023, avg_data_dist: 1644.6884, med_max_data_dist: 600.2000
 * avg_ratio_map: 13.70%, avg_ratio_marker: 3.80%, avg_ratio_skill: 4.97%, avg_ratio_res: 4.03%
 * sim_time: 720.2000, ...
 *
 * Explanation for used metrics:
 * sim_time: The simulation time in seconds at which the statistics are computed
 * avg_used_mem: The percentage of available memory for data hosts used on average
 * max_used_mem: The highest percentage of used memory across all hosts
 * med_avg_data_util: The median across all hosts of the average utility of an item in a host's database
 * avg_data_util: The average across all hosts of the average utility of an item in a host's database
 * med_avg_data_age: The median across all hosts of the avg age of a non-map data item in a host's database (in seconds)
 * avg_data_age: The average across all hosts of the avg age of a non-map data item in a host's database (in seconds)
 * med_max_data_age: The median across all hosts of the max age of a non-map data item in a host's database (in seconds)
 * med_avg_data_dist: The median across all hosts of the average distance of an item in a host's database (in meters)
 * avg_data_dist: The average across all hosts of the average distance of an item in a host's database (in meters)
 * med_max_data_dist: The median across all hosts of the maximum distance of an item in a host's database (in meters)
 * avg_ratio_map: The average across all hosts of what fraction of all items in a host's database is of type map
 * avg_ratio_marker: The average across all hosts of what fraction of all items in a host's database is of type marker
 * avg_ratio_skill: The average across all hosts of what fraction of all items in a host's database is of type skill
 * avg_ratio_res: The average across all hosts of what fraction of all items in a host's database is of type resource
 *
 * Created by Melanie Bruns on 23.04.17.
 */
public class DataSyncReport extends SamplingReport{
    private List<Double> averageDataAges = new ArrayList<>();
    private List<Double> averageDataDistance = new ArrayList<>();
    private List<Double> averageDataUtility = new ArrayList<>();
    private List<Double> usedDataBasePercentage = new ArrayList<>();
    private List<Double> highestAges = new ArrayList<>();
    private List<Double> highestDistance = new ArrayList<>();
    private Map<DisasterData.DataType, List<Double>> ratioByType = new EnumMap<>(DisasterData.DataType.class);

    private static final int RATIO_TO_PERCENT =100;

    public DataSyncReport(){
        writeHeader();
        for (DisasterData.DataType type : DisasterData.DataType.values()){
            ratioByType.put(type, new ArrayList<>());
        }
    }

    @Override
    protected void sample(List<DTNHost> hosts) {
        if (isWarmup()){
            return;
        }
        clearLists();
        for (DTNHost host : hosts){
            //for every host check whether they have a database application
            DatabaseApplication app = DatabaseApplicationUtil.findDatabaseApplication(host.getRouter());
            //If not we can check the next host
            if (app == null){
                continue;
            }
            //We get statistics about age, distance and utility for all data the host has
            DoubleSummaryStatistics ageStats = app.getDataAgeStatistics();
            DoubleSummaryStatistics distanceStats = app.getDataDistanceStatistics();
            DoubleSummaryStatistics utilityStats = app.getDataUtilityStatistics();
            //Get used percentage of database, i.e., how much memory is used
            usedDataBasePercentage.add(app.getUsedMemoryPercentage()* RATIO_TO_PERCENT);
            //Extract info we'd like to use for report
            averageDataAges.add(ageStats.getAverage());
            averageDataDistance.add(distanceStats.getAverage());
            averageDataUtility.add(utilityStats.getAverage());
            highestAges.add(ageStats.getMax());
            highestDistance.add(distanceStats.getMax());
            Map<DisasterData.DataType, Double> ratios = app.getRatioOfItemsPerDataType();
            for (DisasterData.DataType type : DisasterData.DataType.values()){
                List<Double> doubles = ratioByType.get(type);
                doubles.add(ratios.get(type)* RATIO_TO_PERCENT);
            }
        }
        //Write out statistics gathered over all hosts with database
        write("sim_time: " + format(getSimTime()) +", "+
                "avg_used_mem: " + getAverage(usedDataBasePercentage) +"%, "+
                "max_used_mem: " + getMaximum(usedDataBasePercentage) +"%, "+
                "med_avg_data_util: " + getMedian(averageDataUtility)+ ", "+
                "avg_data_util: " + getAverage(averageDataUtility) + ", "+
                "med_avg_data_age: "+ getMedian(averageDataAges) + ", "+
                "avg_data_age: "+ getAverage(averageDataAges) + ", " +
                "med_max_data_age: " + getMedian(highestAges) + ", " +
                "med_avg_data_dist: " + getMedian(averageDataDistance)+ ", "+
                "avg_data_dist: " + getAverage(averageDataDistance)+ ", "+
                "med_max_data_dist: " + getMedian(highestDistance)
        );
        write("avg_ratio_map: " + getAverage(ratioByType.get(DisasterData.DataType.MAP)) + "%, " +
                 "avg_ratio_marker: " + getAverage(ratioByType.get(DisasterData.DataType.MARKER)) + "%, " +
                 "avg_ratio_skill: " + getAverage(ratioByType.get(DisasterData.DataType.SKILL)) + "%, " +
                 "avg_ratio_res: " + getAverage(ratioByType.get(DisasterData.DataType.RESOURCE)) + "%\n "
        );
    }

    private void clearLists(){
        averageDataAges.clear();
        averageDataDistance.clear();
        averageDataUtility.clear();
        usedDataBasePercentage.clear();
        highestAges.clear();
        highestDistance.clear();
        for (DisasterData.DataType type : DisasterData.DataType.values()){
            ratioByType.get(type).clear();
        }
    }

    private void writeHeader(){
        write("Data sync stats for scenario " + getScenarioName() + "\n");
    }

}
