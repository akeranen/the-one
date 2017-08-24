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


def plotAnalysis(lines, priority, figure, plotNumber):
    # Interpret lines to find minimum and average number of reached people over time
    timePoints = []
    minimum = []
    average = []
    for line in lines:
        match = re.match("(\d+)\s+(\d+.\d+)\s+(\d+)", line)
        if match is None:
            continue
        timePoints.append(float(match.group(1)) / 60)
        average.append(float(match.group(2)))
        minimum.append(int(match.group(3)))

    # Draw plots.
    figure.add_subplot(1, 3, plotNumber)
    drawPlots(timePoints, minimum, average, priority)

# Read broadcast analysis from file
with open(sys.argv[1]) as file_name:
    analysis = file_name.readlines()

# Only look at priorities 2, 5 and 9
prio2Analysis = findLinesConcerningPriority(analysis, 2)
prio5Analysis = findLinesConcerningPriority(analysis, 5)
prio9Analysis = findLinesConcerningPriority(analysis, 9)

# Draw plots for all those priorities.
fig = plt.figure(figsize=(16, 4))
plotAnalysis(prio2Analysis, 2, fig, 1)
plotAnalysis(prio5Analysis, 5, fig, 2)
plotAnalysis(prio9Analysis, 9, fig, 3)

# Save to file
plt.tight_layout()
plt.savefig(sys.argv[2])
