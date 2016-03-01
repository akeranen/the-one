### Why a Docker image for a java app?
So you do not need to install `java` nor play around with different versions. [See more reasons](https://stackoverflow.com/questions/34487094/why-use-docker-arent-java-files-like-war-files-already-running-on-jvm).

### How to build an `$version` image?
To build the version `1.6.0`, from this directory execute:

    version=1.6.0;docker build -t theone:$version $version/

### How to run it with the GUI and share conf/data files?
You just need to share the socket `/tmp/.X11-unix` and the variable `DISPLAY` for the GUI, as for the conf/data files, assuming that these ones are in `/path/to/theone/data/files` just execute:

    docker run --rm -it -e DISPLAY=$DISPLAY -v /path/to/theone/data/files:/one -v /tmp/.X11-unix:/tmp/.X11-unix theone:1.6.0
