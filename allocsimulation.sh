#!/usr/bin/sh
#CCS --island OCULUS
#CCS -g HPC-SNF
#CCS -N pgandiSim
#CCS --res=rset=1:ncpus=8:mem=256g
#CCS -t 1h
#CCS -M amaehrle@mail.uni-paderborn.de
#CCS -m abe
#CCS --output=out.txt
#CCS --stderr=err.txt
./one.sh -b 1 configurations/SettingsScenarioShort.txt
