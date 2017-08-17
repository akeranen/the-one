from subprocess import run

with open('../../reports/messageDelayAnalysis.txt', 'w', 1) as file:
    run("perl ../messageDelayAnalyzer.pl ../../reports/realisticScenario_ImmediateMessageDelayReport.txt 300",
        stdout=file)

with open('../../reports/broadcastMessageAnalysis.txt', 'w', 1) as file:
    run("perl ../broadcastMessageAnalyzer.pl ../../reports/realisticScenario_BroadcastDeliveryReport.txt 300",
        stdout=file)

with open('../../reports/multicastMessageAnalysis.txt', 'w', 1) as file:
    run("perl ../multicastMessageAnalyzer.pl ../../reports/realisticScenario_MulticastMessageDeliveryReport.txt 300",
        stdout=file)