#!/bin/sh
java -Xms1G -Xmx1000G -cp target:lib/javax.json-1.0.4.jar:lib/ECLA.jar:lib/DTNConsoleConnection.jar core.DTNSim $*
