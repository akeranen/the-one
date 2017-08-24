import subprocess
import sys
import getopt

# ReportSummary:
#
# Usage:
# reportSummary.py -r <reportDirectory> -s <seeds>
# e.g., -r /Simulator/reports/ -s 1,2,3
#
# 1. Calls all pre-processing perl script on a given report directory
#    The shortened reports are placed within the reports directory
# 2. TODO: Calls visualization scripts and places the results within a
#    subdirectory called images
# 3. TODO: Outputs all images into pdf in consistent order (calls another script that does)


# Retrieve command line options
relevantOptions = sys.argv[1:]
# Default values for options
reportDir = 'reports/'
seeds = []
# Try to retrieve command line parameters
try:
    opts, args = getopt.getopt(relevantOptions,"r:s:")
except getopt.GetoptError:
    print('Usage: reportSummary.py -r <reportDirectory> -s <seeds> \n e.g., -r /Simulator/reports/ -s 1,2,3')
    sys.exit(2)
for opt, arg in opts:
    if opt == "-r":
        reportDir = arg
    elif opt == "-s":
        seeds = arg.split(',')

## Call perl scripts for report pre-processing
#Find out what runs you need to do depending on seeds
granularity = "300"
perlNames = [["messageDelayAnalyzer.pl", "realisticScenario_ImmediateMessageDelayReport", "messageDelayAnalysis"],
             ["broadcastMessageAnalyzer.pl", "realisticScenario_BroadcastDeliveryReport", "broadcastMessageAnalysis"],
             ["multicastMessageAnalyzer.pl", "realisticScenario_MulticastMessageDeliveryReport", "multicastMessageAnalysis"]]

necessaryAnalyses = []
if len(seeds) == 0:
    for (script, input, output) in perlNames:
        necessaryAnalyses.append(["../"+script, reportDir+input+".txt", reportDir+output+".txt"])
else:
    for seed in seeds:
        for (script, input, output) in perlNames:
            necessaryAnalyses.append(["../"+script, reportDir+input+"_"+seed+".txt", reportDir+output+"_"+seed+".txt"])

#Execute script with input and write to output
print("You are running", sys.platform)
for (script, input, output) in necessaryAnalyses:
    with open(output, 'w', 1) as file:
        process = subprocess.run("perl "+ script + " "+ input + " " + granularity, stdout=file)
        if (sys.platform == "linux"):
            file.write(process.stdout)
        print("Successfully created ", output)

