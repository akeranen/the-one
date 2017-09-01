import matplotlib.pyplot as plt
import sys
import re
import humanize

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

# Formatting the pie chart legend. Labels are in the form '23.4% (240KB) category', where 240,000 is the total value.
# First argument is the value, second the total value sum of all categories, and third one is the category name.
def create_label(value, total_value_sum, category_name):
    percentage = (float(value) / total_value_sum) * 100
    return '{p:4.1f}% \t ({v:>10}) \t {n}'.format(p=percentage, v=humanize.naturalsize(value), n=category_name).expandtabs()

# Plots a pie chart out of the provided categories and labels.
# All labels & percentages are placed inside a legend to prevent overlapping.
def create_pie_chart(categories, values):
    fig1, ax1 = plt.subplots()
    ax1.pie(values)
    ax1.axis('equal')  # Equal aspect ratio ensures that pie is drawn as a circle.
    total = sum(values)
    plt.legend( loc = 'lower left', labels=[create_label(size, total, category) for category, size in zip(categories, values)])

# Adds a line in the plot for the total value sum.
def add_total_sum(values):
    plt.figtext(0.7, 0.05, 'Total: {}'.format(humanize.naturalsize(sum(values))))

# Main function of the script. See script description at the top of the file for further information.
def main(analysisFileName, graphicFileName):
    # Read traffic analysis from file
    with open(analysisFileName) as analysis_file:
        analysis = analysis_file.readlines()
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
    plt.title("Traffic distribution by message type")
    plt.savefig(graphicFileName)
    plt.close()

# Make sure script can be called form command line.
if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])