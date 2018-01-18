import matplotlib.pyplot as plt
import sys
import re
import matplotlib.ticker as ticker

# Script that translates a data sync report into plots of
# * memory consumption,
# * average data utility,
# * data distance and
# * data age
# over time and adds a pie chart about data distribution at the end of the simulation.
#
# Takes as arguments
# (1) a data sync report file and
# (2) a path to save the resulting graphic to.
#
# Data sync stats for scenario realisticScenario
# sim_time: 60.00, avg_used_mem: 0.00%, min_used_mem: 0.00%,  max_used_mem: 0.00%, med_avg_data_util: 0.99, avg_data_util: 0.99, med_avg_data_age: 21.15, avg_data_age: 24.56, med_max_data_age: 21.15, med_avg_data_dist: 97.41, avg_data_dist: 90.05, med_max_data_dist: 97.41
# avg_ratio_map: 0.00%, avg_ratio_marker: 29.41%, avg_ratio_skill: 47.06%, avg_ratio_res: 23.53%
#
# sim_time: 120.10, avg_used_mem: 0.00%, min_used_mem: 0.00%, max_used_mem: 0.00%, med_avg_data_util: 0.99, avg_data_util: 0.98, med_avg_data_age: 42.55, avg_data_age: 54.16, med_max_data_age: 42.55, med_avg_data_dist: 117.17, avg_data_dist: 146.08, med_max_data_dist: 117.17
# avg_ratio_map: 0.00%, avg_ratio_marker: 23.64%, avg_ratio_skill: 32.73%, avg_ratio_res: 43.64%
# ...

def main(analysisFileName, graphicFileName):
    # Read data sync report from file
    with open(analysisFileName) as analysis_file:
        report = analysis_file.readlines()

    # Interpret lines to find memory consumption, data utility, data distance and data age metrics over time
    timePoints = []
    averageMemoryConsumption = []
    minimumMemoryConsumption = []
    maximumMemoryConsumption = []
    medianAverageDataUtility = []
    averageAverageDataUtility = []
    medianAverageDataAge = []
    averageAverageDataAge = []
    medianMaximumDataAge = []
    medianAverageDataDistance = []
    averageAverageDataDistance = []
    medianMaximumDataDistance = []
    nextTimePoint = 0
    for line in report:
        match = re.match("sim_time: (\d+.\d+), "
                         "avg_used_mem: (\d+.\d+)%, min_used_mem: (\d+.\d+)%, max_used_mem: (\d+.\d+)%, "
                         "med_avg_data_util: (\d+.\d+), avg_data_util: (\d+.\d+), "
                         "med_avg_data_age: (\d+.\d+), avg_data_age: (\d+.\d+), med_max_data_age: (\d+.\d+), "
                         "med_avg_data_dist: (\d+.\d+), avg_data_dist: (\d+.\d+), med_max_data_dist: (\d+.\d+)", line)
        # Skip lines not detailing desired metrics
        if match is None:
            continue
        # Only check every 10 minutes
        timePoint = float(match.group(1)) / 60
        if (timePoint < nextTimePoint):
            continue
        nextTimePoint = timePoint + 10
        # Make sure to use correct unit (minutes, kilometres)
        timePoints.append(timePoint)
        averageMemoryConsumption.append(float(match.group(2)))
        minimumMemoryConsumption.append(float(match.group(3)))
        maximumMemoryConsumption.append(float(match.group(4)))
        medianAverageDataUtility.append(float(match.group(5)))
        averageAverageDataUtility.append(float(match.group(6)))
        medianAverageDataAge.append(float(match.group(7)) / 60)
        averageAverageDataAge.append(float(match.group(8)) / 60)
        medianMaximumDataAge.append(float(match.group(9)) / 60)
        medianAverageDataDistance.append(float(match.group(10)) / 1000)
        averageAverageDataDistance.append(float(match.group(11)) / 1000)
        medianMaximumDataDistance.append(float(match.group(12)) / 1000)

    # Create four graphics over time: one each for memory consumption, data utility, data distance and data age
    fig = plt.figure(figsize=(12,12))

    # Adds a subplot over time at the specified index.
    # Title, label of y axis and plots (in the form of values and a label) can be provided.
    def addSubplot(atIndex, title, ylabel, plots):
        fig.add_subplot(3, 2, atIndex)
        plt.title(title)
        plt.xlabel('Minutes in simulation')
        plt.ylabel(ylabel)
        for (values, label) in plots:
            plt.plot(timePoints, values, '.-', label=label)
        plt.legend(loc='upper left')
        plt.grid(True)
        axes = plt.gca()
        axes.set_xlim(xmin=0)
        axes.set_ylim(ymin=0)
        axes.xaxis.set_major_locator(ticker.IndexLocator(base=60, offset=-1))

    addSubplot(3, title='Memory Consumption',
               ylabel='Used memory',
               plots=[(averageMemoryConsumption, 'Average'),
                      (maximumMemoryConsumption, 'Maximum'),
                      (minimumMemoryConsumption, 'Minimum')])
    addSubplot(2, title='Average Data Utility in Local Database',
               ylabel='Average data utility',
               plots=[(medianAverageDataUtility, 'Median'), (averageAverageDataUtility, 'Average')])
    addSubplot(4, title='Data Distance',
               ylabel='Distance in km',
               plots=[(averageAverageDataDistance, 'Average Average'),
                      (medianAverageDataDistance, 'Median Average'),
                      (medianMaximumDataDistance, 'Median Maximum')])
    addSubplot(6, title='Data Age',
               ylabel='Age in minutes',
               plots=[(averageAverageDataAge, 'Average Average'),
                      (medianAverageDataAge, 'Median Average'),
                      (medianMaximumDataAge, 'Median Maximum')])
    axes = plt.gca()
    axes.yaxis.set_major_locator(ticker.IndexLocator(base=60, offset=-0.4))

    # Add information about data distribution
    # Get information
    match = re.match("avg_ratio_map: (\d+.\d+)%, avg_ratio_marker: (\d+.\d+)%, avg_ratio_skill: (\d+.\d+)%, avg_ratio_res: (\d+.\d+)%",
                     report[-2])
    labels = ['Marker', 'Skill', 'Resource']
    values = [float(match.group(2)), float(match.group(3)), float(match.group(4))]
    fig.add_subplot(3, 2, 1)
    # Create pie chart
    plt.title('Average final distribution in local database')
    plt.pie(values)
    plt.axis('equal')  # Equal aspect ratio ensures that pie is drawn as a circle.
    plt.legend(loc = 'lower left',
               labels=['{p:4.1f}% \t {n}'.format(p=value, n=label).expandtabs() for label, value in zip(labels, values)])

    # Save to file
    plt.tight_layout()
    plt.savefig(graphicFileName, dpi = 300)
    plt.close()

# Make sure script can be called from command line.
if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])