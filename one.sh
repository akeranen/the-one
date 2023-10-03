#! /bin/sh
java -Xmx4096M -cp target:lib/ECLA.jar:lib/DTNConsoleConnection.jar core.DTNSim $*
