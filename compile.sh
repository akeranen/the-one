targetdir=target

if [ ! -d "$targetdir" ]; then mkdir $targetdir; fi

javac --release 8 -sourcepath src -d $targetdir -classpath lib/ECLA.jar:lib/DTNConsoleConnection.jar src/core/*.java src/movement/*.java src/report/*.java src/routing/*.java src/gui/*.java src/input/*.java src/applications/*.java src/interfaces/*.java

if [ ! -d "$targetdir/gui/buttonGraphics" ]; then cp -R src/gui/buttonGraphics target/gui/; fi
	
