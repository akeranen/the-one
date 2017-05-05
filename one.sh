#! /bin/sh
java -Xmx1048576M -cp target/production/Simulator:lib/javax.json-1.0.4.jar:lib/ECLA.jar:lib/DTNConsoleConnection.jar core.DTNSim $*
