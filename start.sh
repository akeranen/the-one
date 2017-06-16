#!/bin/sh
#ensure that all needed scripts are executable
chmod +x compile.sh allocsimulation.sh one.sh
#archive reports and console output of a possible previous aborted simulation run
nice -n 15 tar -czf "beforestart$(date '+%d%m%Y_%H%M%S').tar.gz" out.txt err.txt reports
#clean up reports and console output of a possible previous aborted simulation run
rm reports/* out.txt err.txt
#compile the simulator
nice -n 15 ./compile.sh
#allocate the simulation run
ccsalloc allocsimulation.sh