import sys
from fpdf import FPDF
import readFileUtilities

# Script that translates a graphics folder containing images with names
# - TrafficAnalysis.png,
# - BufferOccupancy.png
# - DeliveryProbability.png,
# - PrivateMessageDelay.png,
# - BroadcastAnalysis.png,
# - BroadcastMessageDelay2.png,
# - BroadcastMessageDelay5.png,
# - BroadcastMessageDelay9.png,
# - MulticastMessageAnalysis.png,
# - MulticastMessageDelay.png, and
# - dataSyncAnalysis.png
# into a single pdf in that folder with name allGraphics.pdf.

def main(imageFolderPath):
    pdf = FPDF()
    pdf.add_page()
    pdf.image(readFileUtilities.getAbsoluteTrafficAnalysisPath(imageFolderPath), h=132)
    pdf.image(readFileUtilities.getAbsoluteBufferOccupancyAnalysisPath(imageFolderPath), h=132)
    pdf.add_page()
    pdf.image(readFileUtilities.getAbsoluteDeliveryRatePath(imageFolderPath), h=132)
    pdf.image(readFileUtilities.getAbsoluteOneToOneMessageDelayPath(imageFolderPath), h=132)
    pdf.add_page()
    pdf.image(readFileUtilities.getAbsoluteBroadcastAnalysisPath(imageFolderPath), w=185)
    pdf.image(readFileUtilities.getAbsoluteBroadcastDelayPath(imageFolderPath, 2), h=132)
    pdf.image(readFileUtilities.getAbsoluteBroadcastDelayPath(imageFolderPath, 5), h=132)
    pdf.image(readFileUtilities.getAbsoluteBroadcastDelayPath(imageFolderPath, 9), h=132)
    pdf.add_page()
    pdf.image(readFileUtilities.getAbsoluteMulticastAnalysisPath(imageFolderPath), h=132)
    pdf.image(readFileUtilities.getAbsoluteMulticastDelayPath(imageFolderPath), h=132)
    pdf.add_page()
    pdf.image(readFileUtilities.getAbsoluteDataAnalysisPath(imageFolderPath), w=185)
    pdf.output(imageFolderPath + "allGraphics.pdf", "F")

# Make sure script can be called from command line.
if __name__ == "__main__":
    main(sys.argv[1])