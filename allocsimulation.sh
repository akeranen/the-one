#!/bin/sh
#parameters for the allocation of the job on OCuLUS
#CCS --island OCULUS
#CCS -N pgandiSim
#CCS --res=rset=1:ncpus=2:mem=128g
#CCS -t 21d
#CCS -m abe
#CCS --output=out.txt
#CCS --stderr=err.txt

#the simulation task
./one.sh -b 1 configurations/evaluation/settingsScenarioRealistic.txt
#archive the reports and console output. Only executed successfully, if the simulation run completes
#with enough time left for this to be executed.
nice -n 15 tar -czf "result$(date '+%d%m%Y_%H%M%S').tar.gz" out.txt err.txt reports
#remove reports and logged console output. Only executed successfully, if the simulation run completes
#with enough time left for this to be executed.
rm reports/* out.txt err.txt
