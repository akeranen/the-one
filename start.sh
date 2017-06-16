#!/bin/sh
#ensure that all needed scripts are executable
chmod +x compile.sh allocsimulation.sh one.sh
#archive reports and console output of a possible previous aborted simulation run
#if no such run was done before, or at least one of the files out.txt and err.txt does not exist, no archive is created
#any possible output, error or otherwise is redirected to /dev/null to silence the script
nice -n 15 tar -czf "beforestart$(date '+%d%m%Y_%H%M%S').tar.gz" out.txt err.txt reports &> /dev/null
#clean up reports and console output of a possible previous aborted simulation run
#any possible output, error or otherwise is redirected to /dev/null to silence the script
rm reports/* out.txt err.txt &> /dev/null
#compile the simulator
nice -n 15 ./compile.sh
#allocate the simulation run
ccsalloc allocsimulation.sh