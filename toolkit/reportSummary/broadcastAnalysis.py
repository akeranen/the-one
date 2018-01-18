import matplotlib.pyplot as plt
import matplotlib.ticker as ticker
import sys
import re
from readFileUtilities import findNextLineContaining

# Script that translates broadcast analysis into plots of average and minimum message distribution over time.
# Takes as arguments
# (1) a broadcast analysis file
# (2) path to save the graphic to.
#
# The broadcast analysis file should have a format like:
#
# Reached people by broadcasts of prio 2 by time after creation:
# time	 avg	 min
# 300		22.75		0
# 600		156.19		0
# 900		380.53		0
# ...
#
# Reached people by broadcasts of prio 3 by time after creation:
# time	 avg	 min
# 300		22.13		0
# ...

# In a broadcast analysis file given as a collection of lines, finds all lines concerning the specified priority.
def findLinesConcerningPriority(all_lines, priority):
    correctPriorityLine = findNextLineContaining(
        all_lines, 0, "Reached people by broadcasts of prio {} by time after creation:".format(priority))
    nextPriorityLine = findNextLineContaining(all_lines, correctPriorityLine + 1, "prio")
    return all_lines[correctPriorityLine:nextPriorityLine]

def parseBroadcastAnalysis(filename, priority):
    """Parses a broadcast analysis file for the specified priority and returns (in that order) the time points, the
    minimum number of reached people, and the average number of reached people.
    """
    # Read broadcast analysis from file
    with open(filename) as analysis_file:
        analysis = analysis_file.readlines()
    relevantLines = findLinesConcerningPriority(analysis, priority)

    # Interpret lines to find minimum and average number of reached people over time
    timePoints = []
    minimum = []
    average = []
    for line in relevantLines:
        match = re.match("(\d+)\s+(\d+.\d+)\s+(\d+)", line)
        if match is None:
            continue
        timePoints.append(float(match.group(1)) / 60)
        average.append(float(match.group(2)))
        minimum.append(int(match.group(3)))

    return timePoints, minimum, average

# Draws two functions over the same x values.
# Labels are selected as appropriate for broadcast analysis.
def drawPlots(x, y_minimum, y_average, priority):
    plt.title('Broadcast distribution\n Priority {}'.format(priority))
    plt.xlabel('Minutes since message creation')
    plt.ylabel('Reached people')
    plt.plot(x, y_minimum, '.-', label='Minimum')
    plt.plot(x, y_average, '.-',  label='Average')
    plt.legend(loc='upper left')
    plt.grid(True)
    axes = plt.gca()
    axes.set_xlim(xmin = 0)
    axes.set_ylim(ymin = 0)
    axes.xaxis.set_major_locator(ticker.IndexLocator(base=60, offset=-5))

# Main function of the script. See script description at the top of the file for further information.
def main(analysisFileName, graphicFileName):
    # Only look at priorities 2, 5 and 9
    priorities = [2, 5, 9]

    # Draw plots for all those priorities.
    fig = plt.figure(figsize=(16, 4))
    for idx, priority in enumerate(priorities):
        timePoints, minimum, average = parseBroadcastAnalysis(analysisFileName, priority)
        fig.add_subplot(1, 3, idx + 1)
        drawPlots(timePoints, minimum, average, priority)

    # Save to file
    plt.tight_layout()
    plt.savefig(graphicFileName)
    plt.close()

# Make sure script can be called from command line.
if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
