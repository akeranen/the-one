# Finds the index of the next line in the given line array containing the provided phrase. Ignoring all lines
# before the provided index.
def findNextLineContaining(lines, startIdx, phrase):
    for (idx, line) in enumerate(lines):
        if idx < startIdx:
            continue
        if phrase in line:
            return idx
    return -1