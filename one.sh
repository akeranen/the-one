#!/bin/sh
java -ms1G -mx1024G -cp target/production/Simulator:lib/javax.json-1.0.4.jar:lib/ECLA.jar:lib/DTNConsoleConnection.jar core.DTNSim $*
