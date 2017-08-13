import matplotlib.pyplot as plt
import sys
import re
from decimal import Decimal

# Script that translates traffic analysis into a pie chart graphic.
# Takes as arguments
# (1) a traffic analysis file
# (2) path to save the graphic to.
#
# The traffic analysis file should have a format like:
#
# Traffic by message type:
# ONE_TO_ONE: 11.98% (971373881746 Bytes)
# BROADCAST: 10.40% (843526488127 Bytes)
# MULTICAST: 72.41% (5871786867604 Bytes)
# DATA:  5.21% (422070252724 Bytes)

# Formatting the pie chart legend. Labels are in the form 23.4% (240), where 240 is the total value.
# First argument is the value, second the total value sum of all categories.
def create_label(value, total_value_sum):
    percentage = (float(value) / total_value_sum) * 100
    return '{p:5.1f}% ({v:5.2E})'.format(p=percentage, v=Decimal(value))

# Plots a pie chart out of the provided categories and labels.
# All labels & percentages are placed inside a legend to prevent overlapping.
def create_pie_chart(categories, values):
    fig1, ax1 = plt.subplots()
    ax1.pie(values)
    ax1.axis('equal')  # Equal aspect ratio ensures that pie is drawn as a circle.
    total = sum(values)
    plt.legend( loc = 'lower left', labels=['%s %s' % (create_label(size, total), l) for l, size in zip(categories, values)])

# Adds a line in the plot for the total value sum.
def add_total_sum(values):
    plt.figtext(0.7, 0.05, 'Total: {v:.2E}'.format(v = Decimal(sum(sizes))))

# Read traffic analysis from file
with open(sys.argv[1]) as file_name:
    analysis = file_name.readlines()
# Skip first line which only contains explanation.
analysis = analysis[1:]

# Interpret lines to find traffic categories and traffic size
labels = []
sizes = []
for line in analysis:
    match = re.match("(\w+):.*\((\d+) Bytes\)", line)
    if match is None:
        continue
    labels.append(match.group(1))
    sizes.append(int(match.group(2)))

# Create pie chart.
create_pie_chart(labels, sizes)
add_total_sum(sizes)

# Save to file
plt.savefig(sys.argv[2])