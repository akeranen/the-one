import matplotlib.pyplot as plt
import sys
import re

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


# Finds the index of the next line in the given line array containing the provided phrase. Ignoring all lines
# before the provided index.
def findNextLineContaining(lines, startIdx, phrase):
    for (idx, line) in enumerate(lines):
        if idx < startIdx:
            continue
        if phrase in line:
            return idx
    return -1

# Read delay analysis from file
with open(sys.argv[1]) as file_name:
    analysis = file_name.readlines()

# Only look at text for correct type
correctMessageTypeLine = findNextLineContaining(analysis, 0, messageTypeIntro.format(sys.argv[2]))
nextMessageTypeLine = findNextLineContaining(analysis, correctMessageTypeLine + 1, "Delay distribution")
analysis = analysis[correctMessageTypeLine:nextMessageTypeLine]

# Only look at text for correct priority
correctPriorityLine = findNextLineContaining(analysis, 0, priorityIntro.format(sys.argv[3]))
nextPriorityLine = findNextLineContaining(analysis, correctPriorityLine + 1, "priority")
analysis = analysis[correctPriorityLine:nextPriorityLine]

# Find data both for bar chart and cumulative chart
bins = []
vals = []
cumulative = {}
sumOfPercentages = 0
for line in analysis:
    match = re.match(".*<\s*(\d+):\s*(\d+.\d+)%", line)
    if match is None:
        continue
    maxDelay = int(match.group(1)) / 60
    percentage = float(match.group(2))
    bins.append(maxDelay)
    vals.append(percentage)
    sumOfPercentages += percentage
    cumulative[maxDelay] = sumOfPercentages

# Plot bar chart
plt.subplot(2,1,1)
plt.title('Delay distribution of delivered {} messages\nPriority {}'.format(sys.argv[2], sys.argv[3]))
plt.bar(bins, vals)
plt.ylabel('Percentage of messages\n falling into class')
plt.grid(True)
axes = plt.gca()
axes.set_xlim(xmin = 0)

# Directly below, plot cumulative chart
plt.subplot(2,1,2)
plt.plot(bins, [cumulative[bin] for bin in bins])
plt.xlabel('Maximum delay in minutes in delay class')
plt.ylabel('Cumulative percentage')
plt.grid(True)
axes = plt.gca()
axes.set_xlim(xmin = 0)
axes.set_ylim(ymin = 0)

# Save to file
plt.savefig(sys.argv[4], dpi = 300)