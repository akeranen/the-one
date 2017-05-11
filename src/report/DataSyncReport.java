package report;

import applications.DatabaseApplication;
import core.DTNHost;
import core.SimClock;
import core.UpdateListener;
import routing.util.DatabaseApplicationUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.DoubleSummaryStatistics;
import java.util.List;

/**
 * Reports statistics on database synchronization.
 *
 * In preset intervals, it is checked how full the
 * databases are and how distant/old/useful data items are.
 *
 * Format is as follows (without line breaks for reports for each sim_time):
 *
 * Data sync stats for scenario  dataSyncReport
 *
 * sim_time: 600.2000, avg_used_mem: 0.1448%, max_used_mem: 0.9012354941187346%,
 * med_avg_data_util: 0.7751, avg_data_util: 0.6233,
 * med_avg_data_age: 457.1799, avg_data_age: 374.9272, med_max_data_age: 600.2000,
 * med_avg_data_dist: 1915.9023, avg_data_dist: 1644.6884, med_max_data_dist: 600.2000
 * sim_time: 720.2000, ...
 *
 * Explanation for used metrics:
 * sim_time: The simulation time in seconds at which the statistics are computed
 * avg_used_mem: The percentage of available memory for data hosts use on average
 * max_used_mem: The highest percentage of used memory across all hosts
 * med_avg_data_util: The median across all hosts of the average utility of an item in a hosts database
 * avg_data_util: The average across all hosts of the average utility of an item in a hosts database
 * med_avg_data_age: The median across all hosts of the average age of an item in a hosts database (in seconds)
 * avg_data_age: The average across all hosts of the average age of an item in a hosts database (in seconds)
 * med_avg_data_dist: The median across all hosts of the average distance of an item in a hosts database (in meters)
 * avg_data_dist: The average across all hosts of the average distance of an item in a hosts database (in meters)
 * med_max_data_dist: The median across all hosts of the maximum distance of an item in a hosts database (in meters)
 *
 * Created by Melanie Bruns on 23.04.17.
 */
public class DataSyncReport extends Report implements UpdateListener{

    /* Only do snapshots every x seconds, number can be specified here */
    private static final int DATA_COLLECTION_INTERVAL =120;

    /* The last point in time we gathered data */
    private double lastDataCollection;


    public DataSyncReport() {
        //Empty constructor for SonarQube
    }

    @Override
    protected void init(){
        super.init();
        write("Data sync stats for scenario " + getScenarioName() + "\n");
    }

    @Override
    public void updated(List<DTNHost> hosts) {
        if (SimClock.getTime()- lastDataCollection <= DATA_COLLECTION_INTERVAL){
            return;
        }
        lastDataCollection = SimClock.getTime();
        List<Double> averageDataAges = new ArrayList<>();
        List<Double> averageDataDistance = new ArrayList<>();
        List<Double> averageDataUtility = new ArrayList<>();
        List<Double> usedDataBasePercentage = new ArrayList<>();
        List<Double> highestAges = new ArrayList<>();
        List<Double> highestDistance = new ArrayList<>();

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
            usedDataBasePercentage.add(app.getUsedMemoryPercentage());
            //Extract info we'd like to use for report
            averageDataAges.add(ageStats.getAverage());
            averageDataDistance.add(distanceStats.getAverage());
            averageDataUtility.add(utilityStats.getAverage());
            highestAges.add(ageStats.getMax());
            highestDistance.add(ageStats.getMax());
        }
        //Write out statistics gathered over all hosts with database
        write("sim_time: " + format(getSimTime()) +", "+
                "avg_used_mem: " + getAverage(usedDataBasePercentage) +"%, "+
                "max_used_mem: " + Collections.max(usedDataBasePercentage) +"%, "+
                "med_avg_data_util: " + getMedian(averageDataUtility)+ ", "+
                "avg_data_util: " + getAverage(averageDataUtility) + ", "+
                "med_avg_data_age: "+ getMedian(averageDataAges) + ", "+
                "avg_data_age: "+ getAverage(averageDataAges) + ", " +
                "med_max_data_age: " + getMedian(highestAges) + ", " +
                "med_avg_data_dist: " + getMedian(averageDataDistance)+ ", "+
                "avg_data_dist: " + getAverage(averageDataDistance)+ ", "+
                "med_max_data_dist: " + getMedian(highestDistance)

        );
    }

}
