import matplotlib.pyplot as plt
import sys
import os

import privateMessageAnalysis
import delayDistributionAnalysis
import broadcastAnalysis
import multicastAnalysis

import readFileUtilities

# Script responsible for averaging multiple runs and outputting graphics for average delivery rates and delay
# distribution for all message types.
# Assumes perl scripts have already been called, e.g. by reportSummary.py.

def averageLists(tuples):
    """Averages a list of tuples. Each tuple is expected to consist of multiple lists that all have the same length.

    Example:
        tuples: [
        ([3, 6, 9], [4, 2, 8], [5, 1, 1]),
        ([3, 6, 9], [3, 1, 1], [7, 8, 10])]
        result: ([3, 6, 9],[3.5, 1.5, 4.5],[6, 4.5, 5.5])
    """

    # Create averages for each element in the 'matrix' created by tuples of lists.
    # Those averages are indexed by first the tuple index and then the list index.
    # First, sum up everything.
    sums = {}
    for singleTuple in tuples:
        for (tupleElementIndex, list) in enumerate(singleTuple):
            if not tupleElementIndex in sums:
                sums[tupleElementIndex] = {}
            for (listIdx, listElement) in enumerate(list):
                if not listIdx in sums[tupleElementIndex]:
                    sums[tupleElementIndex][listIdx] = listElement
                else:
                    sums[tupleElementIndex][listIdx] += listElement

    # Then, divide everything.
    averagedLists = []
    for tupleElementIndex, tupleAverages in sums.items():
        averagedList = []
        for listIdx, value in tupleAverages.items():
            averagedList.append(sums[tupleElementIndex][listIdx] / len(tuples))
        averagedLists.append(averagedList)

    return tuple(averagedLists)

def average(tuples):
    """Averages a list of tuples. Each tuple is expected to consist of numeric values.

    Example:
        tuples: [(3, 4, 5), (3, 3, 7)]
        result: (3, 3.5, 6])
    """

    # Create averages for each element in the tuples.
    # Those averages are indexed by the tuple index.
    # First, sum up everything.
    sums = {}
    for singleTuple in tuples:
        for (tupleElementIndex, element) in enumerate(singleTuple):
            if not tupleElementIndex in sums:
                sums[tupleElementIndex] = element
            else:
                sums[tupleElementIndex] += element

    # Then, divide everything.
    averaged = []
    for idx, value in sums.items():
        averaged.append(value / len(tuples))

    return tuple(averaged)

def unifyTimePoints(tuples):
    """Unifies the first list among a list of tuples of lists.
    Each tuple is expected to have a list as first element. The longest of those lists is copied over to all other tuples.

    Example:
        tuples: [
        ([3, 6, 9], [4, 2, 8], [5, 1, 1]),
        ([3, 6, 9, 12, 15], [3, 1, 1, 4, 6], [7, 8, 10, 23, 45]),
        ([3], [1], [70])]
        result: [
        ([3, 6, 9, 12, 15], [4, 2, 8], [5, 1, 1]),
        ([3, 6, 9, 12, 15], [3, 1, 1, 4, 6], [7, 8, 10, 23, 45]),
        ([3, 6, 9, 12, 15], [1], [70])]
    """

    longestFirstList = []
    for singleTuple in tuples:
        if len(singleTuple[0]) > len(longestFirstList):
            longestFirstList = singleTuple[0]

    tuplesWithUnifiedTimePoints = []
    for singleTuple in tuples:
        tupleAsList = list(singleTuple)
        tupleAsList[0] = longestFirstList
        tuplesWithUnifiedTimePoints.append(tuple(tupleAsList))

    return tuplesWithUnifiedTimePoints

def padByZeros(tuples, tupleIndices):
    """Takes in a list of tuples of lists. Makes sure all tuple elements matching the provided indices have the same
    length as the first tuple element by padding them by zeros.

    Example:
        tuples: [
        ([3, 6, 9, 12, 15], [4, 2, 8], [5, 1, 1]),
        ([3, 6, 9, 12, 15], [3, 1, 1, 4, 6], [7, 8, 10, 23, 45]),
        ([3, 6, 9, 12, 15], [1], [70])]
        tupleIndices: [1]
        tuples after function call: [
        ([3, 6, 9, 12, 15], [4, 2, 8, 0, 0], [5, 1, 1]),
        ([3, 6, 9, 12, 15], [3, 1, 1, 4, 6], [7, 8, 10, 23, 45]),
        ([3, 6, 9, 12, 15], [1, 0, 0, 0, 0], [70])]
    """

    for singleTuple in tuples:
        for tupleIdx in tupleIndices:
            list = singleTuple[tupleIdx]
            while len(list) < len(singleTuple[0]):
                list.append(0)

def padByReplication(tuples, tupleIndices):
    """
    Takes in a list of tuples of lists. Makes sure all tuple elements matching the provided indices have the same
    length as the first tuple element by padding them with their latest element.

    Example:
        tuples: [
        ([3, 6, 9, 12, 15], [4, 2, 8, 0, 0], [5, 1, 1]),
        ([3, 6, 9, 12, 15], [3, 1, 1, 4, 6], [7, 8, 10, 23, 45]),
        ([3, 6, 9, 12, 15], [1, 0, 0, 0, 0], [70])]
        tupleIndices: [2]
        tuples after function call: [
        ([3, 6, 9, 12, 15], [4, 2, 8, 0, 0], [5, 1, 1, 1, 1]),
        ([3, 6, 9, 12, 15], [3, 1, 1, 4, 6], [7, 8, 10, 23, 45]),
        ([3, 6, 9, 12, 15], [1, 0, 0, 0, 0], [70, 70, 70, 70, 70])]
    """

    for singleTuple in tuples:
        for tupleIdx in tupleIndices:
            list = singleTuple[tupleIdx]
            lastElement = list[len(list) - 1]
            while len(list) < len(singleTuple[0]):
                list.append(lastElement)

def averageFiles(reportsDirectory, seeds):
    """
    Parses all necessary files, adds additional information where needed, and returns (in that order) averaged
    one-to-one delivery rates, an averaged one-to-one delay distribution, averaged multicast delivery rates,
    an averaged multicast delay distribution, averaged broadcast delivery numbers, and an averaged broadcast delay
    distribution.

    :param reportsDirectory: Path to directory containing folders for different runs, where each folder contains all relevant reports /
        analysis files
    :param seeds: List of folder names containing relevant reports / analysis files, e.g. '1 2 4 5'.
    :return: averaged one-to-one delivery rates, averaged one-to-one delay distribution,
    averaged multicast delivery rates, an averaged multicast delay distribution,
    averaged broadcast delivery numbers, and an averaged broadcast delay distribution.
    """

    # Read all information from files
    oneToOneDeliveryRates = []
    oneToOneDelayDistributions = []
    multicastDeliveryRates = []
    multicastDelayDistributions = []
    broadcastDeliveryNumbers = []
    broadcastDelayDistributions = []
    for seed in seeds.split():
        directory = os.path.join(reportsDirectory, seed)
        delayAnalysis = os.path.join(directory, "messageDelayAnalysis.txt")

        oneToOneDeliveryRates.append(
            privateMessageAnalysis.parseDeliveryProbabilityReport(os.path.join(directory, "realisticScenario_DeliveryProbabilityReport.txt")))
        oneToOneDelayDistributions.append(
            delayDistributionAnalysis.parseDelayAnalysis(delayAnalysis, messageType="ONE_TO_ONE", messagePrio=0))
        multicastDeliveryRates.append(
            multicastAnalysis.parseMulticastAnalysis(os.path.join(directory, "multicastMessageAnalysis.txt")))
        multicastDelayDistributions.append(
            delayDistributionAnalysis.parseDelayAnalysis(delayAnalysis, messageType="MULTICAST", messagePrio=1))
        broadcastDeliveryNumbers.append(
            broadcastAnalysis.parseBroadcastAnalysis(os.path.join(directory, "broadcastMessageAnalysis.txt"), priority=5))
        broadcastDelayDistributions.append(
            delayDistributionAnalysis.parseDelayAnalysis(delayAnalysis, messageType="BROADCAST", messagePrio=5))

    # Make sure all time points exist for all seeds
    unifyTimePoints(oneToOneDelayDistributions)
    unifyTimePoints(multicastDelayDistributions)
    unifyTimePoints(broadcastDeliveryNumbers)
    unifyTimePoints(broadcastDelayDistributions)

    # Add missing values for delay distributions.
    padByZeros(oneToOneDelayDistributions, [1])
    padByZeros(multicastDelayDistributions, [1])
    padByZeros(broadcastDelayDistributions, [1])
    padByReplication(oneToOneDelayDistributions, [2])
    padByReplication(multicastDelayDistributions, [2])
    padByReplication(broadcastDelayDistributions, [2])

    # Add missing values for broadcast delivery numbers.
    padByReplication(broadcastDeliveryNumbers, [1, 2])

    # Return everything as averaged.
    return average(oneToOneDeliveryRates),\
           averageLists(oneToOneDelayDistributions), \
           averageLists(multicastDeliveryRates),\
           averageLists(multicastDelayDistributions),\
           averageLists(broadcastDeliveryNumbers),\
           averageLists(broadcastDelayDistributions)

def main(reportsDirectory, seeds):
    """Main function call to create averaged graphics.

    Parameters
    ----------
    reportsDirectory
        Path to directory containing folders for different runs, where each folder contains all relevant reports /
        analysis files
    seeds
        List of folder names containing relevant reports / analysis files, e.g. '1 2 4 5'.
    """

    # Parse and average all information.
    averagedOneToOneDeliveryRates, averagedOneToOneDelayDistribution,\
    averagedMulticastDeliveryRates, averagedMulticastDelayDistribution,\
    averagedBroadcastDeliveryNumbers, averagedBroadcastDelayDistribution = averageFiles(reportsDirectory, seeds)

    # Use it to create graphics.
    graphicsDirectory = os.path.join(reportsDirectory, "graphics/")
    if not os.path.exists(graphicsDirectory):
        os.makedirs(graphicsDirectory)

    delayDistributionAnalysis.createDelayGraphicInFile(
        *averagedOneToOneDelayDistribution,
        title='Delay distribution of delivered ONE_TO_ONE messages',
        fileName=readFileUtilities.getAbsoluteOneToOneMessageDelayPath(graphicsDirectory))
    delayDistributionAnalysis.createDelayGraphicInFile(
        *averagedMulticastDelayDistribution,
        title='Delay distribution of delivered MULTICAST messages',
        fileName=readFileUtilities.getAbsoluteMulticastDelayPath(graphicsDirectory))
    delayDistributionAnalysis.createDelayGraphicInFile(
        *averagedBroadcastDelayDistribution,
        title='Delay distribution of delivered BROADCAST messages',
        fileName=readFileUtilities.getAbsoluteBroadcastDelayPath(graphicsDirectory, prio=5))
    privateMessageAnalysis.createDeliveryPieChart(*averagedOneToOneDeliveryRates)
    plt.savefig(readFileUtilities.getAbsoluteDeliveryRatePath(graphicsDirectory))
    plt.close()
    multicastAnalysis.drawPlots(*averagedMulticastDeliveryRates)
    plt.savefig(readFileUtilities.getAbsoluteMulticastAnalysisPath(graphicsDirectory))
    plt.close()
    broadcastAnalysis.drawPlots(*averagedBroadcastDeliveryNumbers, priority=5)
    plt.savefig(readFileUtilities.getAbsoluteBroadcastAnalysisPath(graphicsDirectory))
    plt.close()

# Make sure script can be called from command line.
if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])