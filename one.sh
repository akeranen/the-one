#!/bin/sh
java -Xms1G -Xmx128G -cp target:lib/javax.json-1.0.4.jar:lib/ECLA.jar:lib/DTNConsoleConnection.jar core.DTNSim $*
