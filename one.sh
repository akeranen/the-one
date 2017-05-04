#! /bin/sh
java -Xmx8192M -cp target/production/Simulator:lib/javax.json-1.0.4.jar:lib/ECLA.jar:lib/DTNConsoleConnection.jar core.DTNSim $*
