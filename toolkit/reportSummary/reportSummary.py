import sh

with open('../../reports/messageDelayAnalysis.txt', 'w', 1) as file:
    sh.perl("../messageDelayAnalyzer.pl", "../../reports/realisticScenario_ImmediateMessageDelayReport.txt", "60",
            _out=file)

