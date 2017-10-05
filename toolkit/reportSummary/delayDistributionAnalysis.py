import matplotlib.pyplot as plt
import sys
import re
from readFileUtilities import findNextLineContaining

# Script that translates delay analysis into plots of (cumulative) delay distribution of delivered messages over time
# since message creation.
# Takes as arguments
# (1) a delay analysis file
# (2) the type of messages to analyze
# (3) the priority of messages to analyze
# (4) path to save the graphic to.
#
# The delay analysis file should have a format like:
#
# Delay distribution for delivered messages of type BROADCAST:
# For priority 2:
# Delay    0 <= x <  300:   1.21% (Total: 847)
# Delay  300 <= x <  600:   5.80% (Total: 4077)
# ...
# Delay 39000 <= x < 39300:   0.00% (Total: 1)
#
# For priority 3:
# ...
#
# Delay distribution for delivered messages of type DATA:
# ...

messageTypeIntro = "Delay distribution for delivered messages of type {}:"
priorityIntro = "For priority {}:"

def parseDelayAnalysis(fileName, messageType, messagePrio):
    """Parses a delay analysis file for information about the provided message type and priority and
    returns (in that order) the different delay classes (identified by max delay), the percentage of delivered messages
    delivered in those intervals, and the cumulative percentages.
    """
    # Read delay analysis from file
    with open(fileName) as analysis_file:
        analysis = analysis_file.readlines()

    # Only look at text for correct type
    correctMessageTypeLine = findNextLineContaining(analysis, 0, messageTypeIntro.format(messageType))
    nextMessageTypeLine = findNextLineContaining(analysis, correctMessageTypeLine + 1, "Delay distribution")
    analysis = analysis[correctMessageTypeLine:nextMessageTypeLine]

    # Only look at text for correct priority
    correctPriorityLine = findNextLineContaining(analysis, 0, priorityIntro.format(messagePrio))
    nextPriorityLine = findNextLineContaining(analysis, correctPriorityLine + 1, "priority")
    analysis = analysis[correctPriorityLine:nextPriorityLine]

    # Find data both for bar chart and cumulative chart
    delayClasses = []
    percentageDelivered = []
    cumulativePercentages = []
    sumOfPercentages = 0
    for line in analysis:
        match = re.match(".*<\s*(\d+):\s*(\d+.\d+)%", line)
        if match is None:
            continue
        maxDelay = int(match.group(1)) / 60
        percentage = float(match.group(2))
        delayClasses.append(maxDelay)
        percentageDelivered.append(percentage)
        sumOfPercentages += percentage
        cumulativePercentages.append(sumOfPercentages)

    return delayClasses, percentageDelivered, cumulativePercentages

def plotDelayDistribution(title, delayClasses, percentageDelivered):
    """Plots a delay distribution as a bar chart."""
    plt.title(title)
    plt.bar(delayClasses, percentageDelivered)
    plt.ylabel('Percentage of messages')
    plt.grid(True)
    axes = plt.gca()
    axes.set_xlim(xmin = 0)

def plotCumulativeDelay(delayClasses, cumulativePercentages):
    """Plots a cumulative delay chart."""
    plt.plot(delayClasses, cumulativePercentages)
    plt.xlabel('Delay in minutes')
    plt.ylabel('Cumulative percentage')
    plt.grid(True)
    axes = plt.gca()
    axes.set_xlim(xmin = 0)
    axes.set_ylim(ymin = 0)

def createDelayGraphicInFile(delayClasses, percentageDelivered, cumulativePercentages, title, fileName):
    # Plot bar chart
    plt.subplot(2,1,1)
    plotDelayDistribution(title, delayClasses, percentageDelivered)

    # Directly below, plot cumulative chart
    plt.subplot(2,1,2)
    plotCumulativeDelay(delayClasses, cumulativePercentages)

    # Save to file
    plt.savefig(fileName, dpi = 300)
    plt.close()

# Main function of the script. See script description at the top of the file for further information.
def main(analysisFileName, messageType, messagePrio, graphicFileName):
    delayClasses, percentageDelivered, cumulativePercentages = parseDelayAnalysis(analysisFileName, messageType, messagePrio)
    createDelayGraphicInFile(
        delayClasses, percentageDelivered, cumulativePercentages,
        title='Delay distribution of delivered {} messages\nPriority {}'.format(messageType, messagePrio),
        fileName=graphicFileName)

# Make sure script can be called from command line.
if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4])