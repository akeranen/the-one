import matplotlib.pyplot as plt
import sys
import re

# Script that translates multicast analysis into a plot of average and minimum delivery rate over time.
# Takes as arguments
# (1) a multicast analysis file
# (2) path to save the graphic to.
#
# The multicast analysis file should have a format like:
#
# #timeAfterMessageCreation	MinRatio	AvgRatio
# 300	0.0	0.00444165511837884
# 600	0.0	0.0164942207497068
# 900	0.0	0.0340330430435976

# Draws two functions over the same x values.
# Labels are selected as appropiate for multicast analysis.
def drawPlots(x, y_minimum, y_average):
    plt.title('Multicast delivery rates')
    plt.xlabel('Minutes since message creation')
    plt.ylabel('Delivery rate')
    plt.plot(x, y_minimum, '.-', label='Minimum')
    plt.plot(x, y_average, '.-',  label='Average')
    plt.legend(loc='upper left')

# Read multicast analysis from file
with open(sys.argv[1]) as file_name:
    analysis = file_name.readlines()
# Skip first line which only contains explanation.
analysis = analysis[1:]

# Interpret lines to find minimum and average delivery rates over time
timePoints = []
minimum = []
average = []
for line in analysis:
    match = re.match("(\d+)\s+(\d+.\d+)\s+(\d+(?:.\d*)?)", line)
    if match is None:
        continue
    timePoints.append(float(match.group(1)) / 60)
    minimum.append(float(match.group(2)))
    average.append(float(match.group(3)))

# Draw plots.
drawPlots(timePoints, minimum, average)

# Save to file
plt.savefig(sys.argv[2])