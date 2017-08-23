import matplotlib.pyplot as plt
import sys
import re

# Script that translates one-to-one message deliver into a pie chart
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
    match = re.match("(\D+)(\d+(\.\d+)?)", string)
    if match is None:
        print("Formatting problem")
        exit(2)
    if (match.group(3)):
        #The number has decimal point
        return float(match.group(2))
    return int(match.group(2))

# Main function of the script. See script description at the top of the file for further information.
def main(analysisFileName, graphicFileName):
    # Read delivery probability from file
    with open(analysisFileName) as analysis_file:
        analysis = analysis_file.readlines()

    created = getValueFromString(analysis[2])
    delivered = getValueFromString(analysis[3])
    delivery_prob = getValueFromString(analysis[4])

    values=[delivery_prob, 1-delivery_prob]
    labels=["delivered:\n{p:.1f}% ({t})".format(p=delivery_prob*100, t=delivered),
            "not delivered:\n{p:.1f}% ({t})".format(p=(1-delivery_prob)*100, t=(created-delivered))]

    # Create pie chart.
    fig1, ax1 = plt.subplots()
    ax1.pie(values, labels=labels, shadow=True, explode=(0.1, 0), labeldistance=0.3)
    ax1.axis('equal')  # Equal aspect ratio ensures that pie is drawn as a circle.

    #Add total sum
    plt.figtext(0.6, 0.05, 'Total created messages: %d' % created)

    # Save to file
    plt.savefig(graphicFileName)
    plt.close()

# Make sure script can be called from command line.
if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])