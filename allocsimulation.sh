#!/usr/bin/sh
#CCS --island OCULUS
#CCS -N pgandiSim
#CCS --res=rset=1:ncpus=2:mem=256g
#CCS -t 168h
#CCS -m abe
#CCS --output=out.txt
#CCS --stderr=err.txt
./one.sh -b 1 configurations/evaluation/settingsScenarioShort.txt
