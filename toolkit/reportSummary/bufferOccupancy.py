import matplotlib.pyplot as plt
import sys
import re

# Script that generates plots about buffer occupancy from buffer occupancy report
# Takes as arguments
# (1) a buffer occupancy report
# (2) path to save the graphic to.
#
# The buffer occupancy report file should have a format like:
# [Simulation time] [average buffer occupancy % [0..100] ] [variance] [min] [max]

def main(analysisFileName, graphicFileName):
    # Read delay analysis from file
    with open(analysisFileName) as analysis_file:
        analysis = analysis_file.readlines()

    times = []
    avgOccupancies = []
    minima = []
    maxima = []
    variances = []

    for line in analysis:
        match = re.match("(\d+\.\d+\s)(\d+\.\d+\s)(\d+\.\d+\s)(\d+\.\d+\s)(\d+\.\d+)", line)
        if match is None:
            print("no match")
            continue
        simTime = float(match.group(1)) / 60
        avgOccupancy = float(match.group(2))
        variance = float(match.group(3))
        min = float(match.group(4))
        max = float(match.group(5))
        times.append(simTime)
        avgOccupancies.append(avgOccupancy)
        minima.append(min)
        maxima.append(max)
        variances.append(variance)

    plt.grid(True)
    plt.plot(times, maxima, label="maximum")
    plt.plot(times, avgOccupancies, label="mean")
    plt.plot(times, minima, label="minimum")
    plt.plot(times, variances, label="variance")

    plt.xlabel('Time in minutes')
    plt.ylabel('Percentage of buffer that is occupied')
    plt.legend(loc='right')
    plt.title('Buffer occupancy')

    plt.savefig(graphicFileName, dpi=300)
    plt.close()

# Make sure script can be called from command line.
if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])