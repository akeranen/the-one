import subprocess
import sys
import getopt
import os

import trafficAnalysis
import bufferOccupancy
import privateMessageAnalysis
import delayDistributionAnalysis
import broadcastAnalysis
import multicastAnalysis
import dataSyncAnalysis
import energyAnalysis
import reportSummaryPdf

import readFileUtilities

# ReportSummary:
#
# Usage:
# reportSummary.py -r <reportDirectory>
# e.g., -r /Simulator/reports/
#
# 1. Calls all pre-processing perl script on a given report directory
#    The shortened reports are placed within the reports directory
# 2. Calls visualization scripts and places the results within a
#    subdirectory called images
# 3. Outputs all images into pdf in consistent order (calls another script that does)


# Retrieve command line options
relevantOptions = sys.argv[1:]
# Default values for options
reportDir = 'reports/'

# Try to retrieve command line parameters
try:
    opts, args = getopt.getopt(relevantOptions,"r:")
except getopt.GetoptError:
    print('Usage: reportSummary.py -r <reportDirectory> \n e.g., -r /Simulator/reports/')
    sys.exit(2)
for opt, arg in opts:
    if opt == "-r":
        reportDir = arg

## Call perl scripts for report pre-processing
granularity = "300"
perlNames = [["messageDelayAnalyzer.pl", "realisticScenario_ImmediateMessageDelayReport", "messageDelayAnalysis"],
             ["broadcastMessageAnalyzer.pl", "realisticScenario_BroadcastDeliveryReport", "broadcastMessageAnalysis"],
             ["multicastMessageAnalyzer.pl", "realisticScenario_MulticastMessageDeliveryReport", "multicastMessageAnalysis"]]

necessaryAnalyses = []
for (script, input, output) in perlNames:
    necessaryAnalyses.append(["../"+script, reportDir+input+".txt", reportDir+output+".txt"])

#Execute script with input and write to output
print("You are running", sys.platform)
for (script, input, output) in necessaryAnalyses:
    with open(output, 'w', 1) as file:
        process = subprocess.run("perl "+ script + " "+ input + " " + granularity, stdout=file)
        if (sys.platform == "linux"):
            file.write(process.stdout)
        print("Successfully created ", output)

# Create images/ directory in reports directory if it does not exist yet
imageDirectoryName = reportDir + 'images/'
if not os.path.exists(imageDirectoryName):
    os.makedirs(imageDirectoryName)
print("Made sure directory exists: ", imageDirectoryName)

# Call all visualization scripts
trafficAnalysis.main(
    analysisFileName=reportDir+"realisticScenario_TrafficReport.txt",
    graphicFileName=readFileUtilities.getAbsoluteTrafficAnalysisPath(imageDirectoryName))
bufferOccupancy.main(
    analysisFileName=reportDir+"realisticScenario_BufferOccupancyReport.txt",
    graphicFileName=readFileUtilities.getAbsoluteBufferOccupancyAnalysisPath(imageDirectoryName))
privateMessageAnalysis.main(
    analysisFileName=reportDir+"realisticScenario_DeliveryProbabilityReport.txt",
    graphicFileName=readFileUtilities.getAbsoluteDeliveryRatePath(imageDirectoryName))
delayDistributionAnalysis.main(
    analysisFileName=reportDir+"messageDelayAnalysis.txt",
    messageType="ONE_TO_ONE",
    messagePrio=0,
    graphicFileName=readFileUtilities.getAbsoluteOneToOneMessageDelayPath(imageDirectoryName))
broadcastAnalysis.main(
    analysisFileName=reportDir+"broadcastMessageAnalysis.txt",
    graphicFileName=readFileUtilities.getAbsoluteBroadcastAnalysisPath(imageDirectoryName))
relevantPriorities = [2, 5, 9]
for prio in relevantPriorities:
    delayDistributionAnalysis.main(
        analysisFileName=reportDir+"messageDelayAnalysis.txt",
        messageType="BROADCAST",
        messagePrio=prio,
        graphicFileName=readFileUtilities.getAbsoluteBroadcastDelayPath(imageDirectoryName, prio))
multicastAnalysis.main(
    analysisFileName=reportDir+"multicastMessageAnalysis.txt",
    graphicFileName=readFileUtilities.getAbsoluteMulticastAnalysisPath(imageDirectoryName))
delayDistributionAnalysis.main(
    analysisFileName=reportDir+"messageDelayAnalysis.txt",
    messageType="MULTICAST",
    messagePrio=1,
    graphicFileName=readFileUtilities.getAbsoluteMulticastDelayPath(imageDirectoryName))
dataSyncAnalysis.main(
    analysisFileName=reportDir+"realisticScenario_DataSyncReport.txt",
    graphicFileName=readFileUtilities.getAbsoluteDataAnalysisPath(imageDirectoryName))
energyAnalysis.main(
    analysisFileName=reportDir+"realisticScenario_EnergyLevelReport.txt",
    graphicFileName=readFileUtilities.getAbsoluteEnergyAnalysisPath(imageDirectoryName))

print("Successfully created all graphics. Creating pdf...")

reportSummaryPdf.main(imageDirectoryName)