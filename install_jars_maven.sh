#!/bin/bash
proj_dir=$(pwd)
DTNConsoleConnection=$proj_dir/lib/DTNConsoleConnection.jar
ECLA=$proj_dir/lib/ECLA.jar

# install these two libraries to the local maven repo
mvn org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file \
  -Dfile=$DTNConsoleConnection \
  -DgroupId=fi.tkk.netlab.dtn \
  -DartifactId=DTNConsoleConnection \
  -Dversion=1.0 \
  -Dpackaging=jar \

mvn org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file \
  -Dfile=$ECLA \
  -DgroupId=fi.tkk.netlab.dtn \
  -DartifactId=ECLA \
  -Dversion=1.0 \
  -Dpackaging=jar \
