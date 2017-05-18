#!/bin/sh
chmod +x compile.sh allocsimulation.sh one.sh
nice -n 15 tar -czf "beforestart$(date '+%d%m%Y_%H%M%S').tar.gz" out.txt err.txt reports
rm reports/* out.txt err.txt
nice -n 15 ./compile.sh
ccsalloc allocsimulation.sh