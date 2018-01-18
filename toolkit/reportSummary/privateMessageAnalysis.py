import matplotlib.pyplot as plt
import sys
import re

# Script that translates one-to-one message delivery ratio into a pie chart
# Takes as arguments
# (1) a delivery probability report
# (2) path to save the graphic to.
#
# The delivery probability report file should have a format like:
#
# Message stats for scenario realisticScenario
# sim_time: 50400.0000
# created: 2099
# delivered: 251
# delivery_prob: 0.1196

def getValueFromString(string):
    """Return the value contained in the string as an int or float depending on whether its an integer number."""
    match = re.match("(\D+)(\d+(\.\d+)?)", string)
    if match is None:
        print("Formatting problem")
        exit(2)
    if (match.group(3)):
        #The number has decimal point
        return float(match.group(2))
    return int(match.group(2))

def parseDeliveryProbabilityReport(fileName):
    """Parses a delivery probability report file and returns (in that order) the number of created messages,
    the number of delivered messages, and the delivery probability.
    """
    with open(fileName) as analysis_file:
        analysis = analysis_file.readlines()

    created = getValueFromString(analysis[2])
    delivered = getValueFromString(analysis[3])
    delivery_prob = getValueFromString(analysis[4])

    return created, delivered, delivery_prob

def createDeliveryPieChart(created, delivered, delivery_prob):
    """Creates a graphical presentation of delivery probability."""
    values=[delivery_prob, 1-delivery_prob]
    labels=["delivered:\n{p:.1f}% ({t:.1f})".format(p=delivery_prob*100, t=delivered),
            "not delivered:\n{p:.1f}% ({t:.1f})".format(p=(1-delivery_prob)*100, t=(created-delivered))]

    # Create pie chart.
    fig1, ax1 = plt.subplots()
    _, texts = ax1.pie(values, labels=labels, shadow=True, explode=(0.1, 0), labeldistance=0.2)
    for text in texts:
        text.set_fontsize(12)
    ax1.axis('equal')  # Equal aspect ratio ensures that pie is drawn as a circle.

    #Add total sum
    plt.figtext(0.6, 0.05, 'Total created messages: %d' % created)
    plt.title("Message delivery ratio for one-to-one messages")

# Main function of the script. See script description at the top of the file for further information.
def main(analysisFileName, graphicFileName):
    created, delivered, delivery_prob = parseDeliveryProbabilityReport(analysisFileName)
    createDeliveryPieChart(created, delivered, delivery_prob)

    # Save to file
    plt.savefig(graphicFileName)
    plt.close()

# Make sure script can be called from command line.
if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])