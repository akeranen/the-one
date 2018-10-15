set targetdir=target

IF NOT EXIST "%targetdir%" mkdir %targetdir%

javac -sourcepath src -d %targetdir% -cp lib/ECLA.jar;lib/DTNConsoleConnection.jar src/core/*.java src/movement/*.java src/report/*.java src/routing/*.java src/gui/*.java src/input/*.java src/applications/*.java src/interfaces/*.java



IF NOT EXIST "%targetdir%\gui\buttonGraphics" (
	mkdir %targetdir%\gui\buttonGraphics
	copy src\gui\buttonGraphics\* %targetdir%\gui\buttonGraphics\
)
