import matplotlib.pyplot as plt
import sys
import re
import matplotlib.ticker as ticker

# Script that translates energy level analysis into plots of percentages of hosts with low or no energy.
# Takes as arguments
# (1) an energy level report file
# (2) path to save the graphic to.
#
# The energy level analysis file should have a format like:
#
# [600]
# 600,p0,0.8116
# ...
# 600,c2999,0.4448
# [1200]
# 1200,p0,0.7556
# ...
# 50400,c2999,0.0000

# Class responsible for summarizing energy data fed into it by time point and host.
class EnergyData:
    # Summarized data
    timePoints = []
    zeroEnergyHosts = []
    lowEnergyHosts = []

    # Current counters
    currentTimePoint = 0
    currentTotalHosts = 0
    currentZeroEnergyHosts = 0
    currentLowEnergyHosts = 0

    # Transfers current counters into new elements of the summarizing data vectors.
    def updateData(self):
        self.timePoints.append(self.currentTimePoint)
        self.zeroEnergyHosts.append(self.currentZeroEnergyHosts / self.currentTotalHosts)
        self.lowEnergyHosts.append(self.currentLowEnergyHosts / self.currentTotalHosts)

    # Resets current counters to 0.
    def resetCounters(self):
        self.currentTotalHosts = 0
        self.currentZeroEnergyHosts = 0
        self.currentLowEnergyHosts = 0

    # Updates counters with a new energy level at the current time point.
    def updateCounters(self, energyLevel):
        self.currentTotalHosts += 1
        if energyLevel == 0:
            self.currentZeroEnergyHosts += 1
        elif energyLevel < 0.1:
            self.currentLowEnergyHosts += 1

# Draws two functions over the same x values.
# Labels are selected as appropriate for energy analysis.
def drawPlots(x, y_lowEnergy, y_noEnergy):
    plt.title('Battery power distribution')
    plt.xlabel('Minutes in simulation')
    plt.ylabel('Percentage of hosts')
    plt.plot(x, y_lowEnergy, '.-', label='Battery 0% < x < 10%')
    plt.plot(x, y_noEnergy, '.-',  label='No battery left')
    plt.legend(loc='upper left')
    plt.grid(True)
    axes = plt.gca()
    axes.set_ylim(ymin=0,ymax=1)
    axes.set_xlim(xmin=0)
    axes.xaxis.set_major_locator(ticker.IndexLocator(base=60, offset=-10))

# Main function of the script. See script description at the top of the file for further information.
def main(analysisFileName, graphicFileName):
    # Read energy level analysis from file
    with open(analysisFileName) as analysis_file:
        analysis = analysis_file.readlines()

    # Traverse analysis line by line to find the number of zero and low energy hosts by time
    data = EnergyData()
    for line in analysis:
        timeMatch = re.match("\[(\d+)\]", line)
        energyMatch = re.match("\d+,\D+\d+,(\d+.\d+)", line)
        if timeMatch is not None:
            if data.currentTimePoint is not 0:
                data.updateData()
            data.currentTimePoint = float(timeMatch.group(1)) / 60
            data.resetCounters()
        if energyMatch is not None:
            data.updateCounters(energyLevel=float(energyMatch.group(1)))

    drawPlots(x=data.timePoints, y_lowEnergy=data.lowEnergyHosts, y_noEnergy=data.zeroEnergyHosts)

    # Save to file
    plt.savefig(graphicFileName)
    plt.close()

# Make sure script can be called from command line.
if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])