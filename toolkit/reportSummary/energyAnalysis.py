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
    hostsThatEverReachedLowEnergyLimit = []
    hostsThatEverReachedZeroEnergy = []

    # Current counters
    currentTimePoint = 0
    currentTotalHosts = 0
    currentZeroEnergyHosts = 0
    currentLowEnergyHosts = 0

    # Sets of hosts that ever reached the limit
    currentHostsThatEverReachedLowEnergyLimit = set()
    currentHostsThatEverReachedZeroEnergy = set()

    # Transfers current counters into new elements of the summarizing data vectors.
    def updateData(self):
        self.timePoints.append(self.currentTimePoint)
        self.zeroEnergyHosts.append(self.currentZeroEnergyHosts / self.currentTotalHosts)
        self.lowEnergyHosts.append(self.currentLowEnergyHosts / self.currentTotalHosts)
        self.hostsThatEverReachedLowEnergyLimit \
            .append(len(self.currentHostsThatEverReachedLowEnergyLimit) / self.currentTotalHosts)
        self.hostsThatEverReachedZeroEnergy \
            .append(len(self.currentHostsThatEverReachedZeroEnergy) / self.currentTotalHosts)

    # Resets current counters to 0.
    def resetCounters(self):
        self.currentTotalHosts = 0
        self.currentZeroEnergyHosts = 0
        self.currentLowEnergyHosts = 0

    # Updates counters with a new energy level at the current time point and adds the host to the energy sets according
    # to the remaining energy.
    def updateCountersAndHostSets(self, host, energyLevel):
        self.currentTotalHosts += 1
        if energyLevel < 0.1:
            self.currentHostsThatEverReachedLowEnergyLimit.add(host)
            if(energyLevel == 0 ):
                # Draws two functions over the same x values.
                self.currentHostsThatEverReachedZeroEnergy.add(host)
                self.currentZeroEnergyHosts += 1
            else:
                self.currentLowEnergyHosts += 1

# Draws two functions over the same x values.
# Labels are selected as appropriate for energy analysis.
# In the end saves the plot
def drawAndSafePlots(x, y_lowEnergy, y_noEnergy, y_totalLowEnergy, y_totalNoEnergy, graphicFileName):
    plt.title('Battery power distribution')
    plt.xlabel('Minutes in simulation')
    plt.ylabel('Percentage of hosts')
    plt.plot(x, y_lowEnergy, '.-', label='Current hosts with battery 0% < x < 10%')
    plt.plot(x, y_totalLowEnergy, '.-', label='Total hosts that reached battery 0% < x < 10%')
    plt.plot(x, y_noEnergy, '.-',  label='Current hosts with no battery left')
    plt.plot(x, y_totalNoEnergy, '.-',  label='Total hosts that had no battery left')
    legend = plt.legend(bbox_to_anchor=(0.1, -0.15), loc=2, borderaxespad=0.)
    plt.grid(True)
    axes = plt.gca()
    axes.set_ylim(ymin=0,ymax=1)
    axes.set_xlim(xmin=0)
    axes.xaxis.set_major_locator(ticker.IndexLocator(base=60, offset=-10))
    # Save to file
    plt.savefig(graphicFileName, bbox_extra_artists=(legend,), bbox_inches='tight')
    plt.close()

# Main function of the script. See script description at the top of the file for further information.
def main(analysisFileName, graphicFileName):
    # Read energy level analysis from file
    with open(analysisFileName) as analysis_file:
        analysis = analysis_file.readlines()

    # Traverse analysis line by line to find the number of zero and low energy hosts by time
    data = EnergyData()
    for line in analysis:
        timeMatch = re.match("\[(\d+)\]", line)
        energyMatch = re.match("\d+,(\D+\d+),(\d+.\d+)", line)
        # If the line looks like "[600]", i.e. the report changes to the next time frame, we have to finalize the
        # data we collected for the last time point (if there was such) and then reset our counters.
        if timeMatch is not None:
            if data.currentTimePoint is not 0:
                data.updateData()
            data.currentTimePoint = float(timeMatch.group(1)) / 60
            data.resetCounters()
        # Otherwise, if the line looks like '600,p0,0.8116', it is another data point for the current time and we
        # should update our counters using the logged energy.
        if energyMatch is not None:
            data.updateCountersAndHostSets(energyLevel=float(energyMatch.group(2)), host=energyMatch.group(1))

    drawAndSafePlots(x=data.timePoints,
                     y_lowEnergy=data.lowEnergyHosts,
                     y_noEnergy=data.zeroEnergyHosts,
                     y_totalLowEnergy=data.hostsThatEverReachedLowEnergyLimit,
                     y_totalNoEnergy=data.hostsThatEverReachedZeroEnergy, graphicFileName=graphicFileName)

# Make sure script can be called from command line.
if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])