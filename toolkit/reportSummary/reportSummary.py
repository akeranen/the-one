import sh

with open('../../reports/messageDelayAnalysis.txt', 'w', 1) as file:
    sh.perl("../messageDelayAnalyzer.pl", "../../reports/realisticScenario_ImmediateMessageDelayReport.txt", "60",
            _out=file)

with open('../../reports/broadcastMessageAnalysis.txt', 'w', 1) as file:
    sh.perl("../broadcastMessageAnalyzer.pl", "../../reports/realisticScenario_BroadcastDeliveryReport.txt", "60",
            _out=file)

with open('../../reports/multicastMessageAnalysis.txt', 'w', 1) as file:
    sh.perl("../multicastMessageAnalyzer.pl", "../../reports/realisticScenario_MulticastMessageDeliveryReport.txt", "60",
            _out=file)