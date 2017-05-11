#!/usr/bin/sh
#CCS --island OCULUS
#CCS -N pgandiSim
#CCS --res=rset=1:ncpus=2:mem=256g
#CCS -t 1h
#CCS -m abe
#CCS --output=out.txt
#CCS --stderr=err.txt
rm reports/* out.txt err.txt
nice -n 15 ./compile.sh
./one.sh -b 1 configurations/evaluation/settingsScenarioShort.txt
nice -n 15 tar -czf "result$(date '+%d%m%Y_%H%M%S').tar.gz" out.txt err.txt reports
rm reports/* out.txt err.txt
