targetdir=target

if [ ! -d "$targetdir" ]; then mkdir $targetdir; fi

javac -sourcepath src -d $targetdir -extdirs lib/ src/core/*.java src/movement/*.java src/report/*.java src/routing/*.java src/gui/*.java src/input/*.java src/applications/*.java src/interfaces/*.java

if [ ! -d "$targetdir/gui/buttonGraphics" ]; then cp -R src/gui/buttonGraphics target/gui/; fi
	
