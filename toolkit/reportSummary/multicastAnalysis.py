import matplotlib.pyplot as plt
import sys
import re

# Script that translates multicast analysis into a plot of average and minimum delivery rate over time.
# The rates are calculated for both messages that exist at that time and all messages that ever existed
# Takes as arguments
# (1) a multicast analysis file
# (2) path to save the graphic to.
#
# The multicast analysis file should have a format like:
#
# #timeAfterMessageCreation	MinRatio	AvgRatio   MinRatioForAll AvgRatioForAll
# 300	0.0	0.00444165511837884 0.0 0.004223002342392
# 600	0.0	0.0164942207497068  0.0 0.01223002342392
# 900	0.0	0.0340330430435976  0.0 0.024223002342392

# Draws two functions over the same x values.
# Labels are selected as appropiate for multicast analysis.
def drawPlots(x, y_minimum, y_average, y_minForAll, y_avgForAll):
    plt.title('Multicast delivery rates')
    plt.xlabel('Minutes since message creation')
    plt.ylabel('Delivery rate')
    plt.plot(x, y_minimum, '.-', label='Minimum for existent messages')
    plt.plot(x, y_average, '.-',  label='Average for existent messages')
    #Currently not plotted as both minima are constantly zero and if the ever get higher it will be more noticably in
    #the minimum delivery ratio for existent messages, two overlaying graphs make viewers search for the 'missing' one
    #plt.plot(x, y_minForAll, '.-', label='Minimum for all messages ever created')
    plt.plot(x, y_avgForAll, '.-',  label='Average for all messages ever created')
    plt.legend(loc='upper left')
    plt.grid(True)

# Main function of the script. See script description at the top of the file for further information.
def main(analysisFileName, graphicFileName):
    # Read multicast analysis from file
    with open(analysisFileName) as analysis_file:
        analysis = analysis_file.readlines()
    # Skip first line which only contains explanation.
    analysis = analysis[1:]

    # Interpret lines to find minimum and average delivery rates over time
    timePoints = []
    minimum = []
    minimumForAll = []
    average = []
    averageForAll = []
    for line in analysis:
        match = re.match("(\d+)\s+(\d+.\d+)\s+(\d+(?:.\d*)?)\s+(\d+(?:.\d*)?)\s+(\d+(?:.\d*)?)", line)
        if match is None:
            continue
        timePoints.append(float(match.group(1)) / 60)
        minimum.append(float(match.group(2)))
        average.append(float(match.group(3)))
        minimumForAll.append(float(match.group(4)))
        averageForAll.append(float(match.group(5)))

    # Draw plots.
    drawPlots(timePoints, minimum, average, minimumForAll, averageForAll)

    # Save to file
    plt.savefig(graphicFileName)
    plt.close()

# Make sure script can be called from command line.
if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])