import sh

with open('../../reports/messageDelayAnalysis.txt', 'wb', 0) as file:
    sh.perl("../messageDelayAnalyzer.pl", "../../reports/realisticScenario_ImmediateMessageDelayReport.txt",
            stdout=file)
