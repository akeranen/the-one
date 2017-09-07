# Finds the index of the next line in the given line array containing the provided phrase. Ignoring all lines
# before the provided index.
def findNextLineContaining(lines, startIdx, phrase):
    for (idx, line) in enumerate(lines):
        if idx < startIdx:
            continue
        if phrase in line:
            return idx
    return -1

# Returns the path to the traffic analysis graphic saved / to be saved in the provided folder.
def getAbsoluteTrafficAnalysisPath(folder):
    return folder + "TrafficAnalysis.png"

def getAbsoluteBufferOccupancyAnalysisPath(folder):
    return folder + "BufferOccupancy.png"

# Returns the path to the one to one message delivery rate analysis graphic saved / to be saved in the provided folder.
def getAbsoluteDeliveryRatePath(folder):
    return folder + "DeliveryProbability.png"

# Returns the path to the one to one message delay graphic saved / to be saved in the provided folder.
def getAbsoluteOneToOneMessageDelayPath(folder):
    return folder + "PrivateMessageDelay.png"

# Returns the path to the broadcast analysis graphic saved / to be saved in the provided folder.
def getAbsoluteBroadcastAnalysisPath(folder):
    return folder + "BroadcastAnalysis.png"

# Returns the path to the broadcast delay graphic saved / to be saved in the provided folder.
def getAbsoluteBroadcastDelayPath(folder, prio):
    return folder + "BroadcastMessageDelay" + str(prio) + ".png"

# Returns the path to the multicast analysis graphic saved / to be saved in the provided folder.
def getAbsoluteMulticastAnalysisPath(folder):
    return folder + "MulticastMessageAnalysis.png"

# Returns the path to the multicast delay graphic saved / to be saved in the provided folder.
def getAbsoluteMulticastDelayPath(folder):
    return folder + "MulticastMessageDelay.png"

# Returns the path to the data sync analysis graphic saved / to be saved in the provided folder.
def getAbsoluteDataAnalysisPath(folder):
    return folder + "DataSyncAnalysis.png"

# Returns the path to the energy analysis graphic saved / to be saved in the provided folder.
def getAbsoluteEnergyAnalysisPath(folder):
    return folder + "EnergyAnalysis.png"