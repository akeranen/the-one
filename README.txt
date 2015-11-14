The ONE v1.6.0 - Readme
=======================

The ONE is a Opportunistic Network Environment simulator which provides a
powerful tool for generating mobility traces, running DTN messaging
simulations with different routing protocols, and visualizing both
simulations interactively in real-time and results after their completion.


Quick start
===========

Compiling
---------

You can compile ONE from the source code using the included compile.bat
script. That should work both in Windows and Unix/Linux environment with
Java 6 JDK or later.

If you want to use Eclipse for compiling the ONE, since version 1.1.0 you need
to include some jar libraries in the project's build path. The libraries are
located in the lib folder. To include them in Eclipse, assuming that you have
an Eclipse Java project whose root folder is the folder where you extracted
the ONE, do the following:

select from menus: Project -> Properties -> Java Build Path
Go to "Libraries" tab
Click "Add JARs..."
Select "DTNConsoleConnection.jar" under the "lib" folder
Add the "ECLA.jar" the same way
Press "OK".

Now Eclipse should be able to compile the ONE without warnings.


Running
-------

ONE can be started using the included one.bat (for Windows) or one.sh (for
Linux/Unix) script. Following examples assume you're using the Linux/Unix
script (just replace "./one.sh" with "one.bat" for Windows).

Synopsis:
./one.sh [-b runcount] [conf-files]

Options:
  -b Run simulation in batch mode. Doesn't start GUI but prints
information about the progress to terminal. The option must be followed
by the number of runs to perform in the batch mode or by a range of runs
to perform, delimited with a colon (e.g, value 2:4 would perform runs 2,
3 and 4). See section "Run indexing" for more information.

Parameters:
  conf-files: The configuration file names where simulation parameters
are read from. Any number of configuration files can be defined and they are
read in the order given in the command line. Values in the later config files
override values in earlier config files.


Configuring
===========

All simulation parameters are given using configuration files. These files
are normal text files that contain key-value pairs. Syntax for most of the
variables is:
Namespace.key = value

I.e., the key is (usually) prefixed by a namespace, followed by a dot, and
then key name. Key and value are separated by equals-sign. Namespaces
start with capital letter and both namespace and keys are written in
CamelCase (and are case sensitive). Namespace defines (loosely) the part
of the simulation environment where the setting has effect on. Many, but
not all, namespaces are equal to the class name where they are read.
Especially movement models, report modules and routing modules follow this
convention. In some cases the namespace is defined by the user: e.g., with
network interfaces user can pick up any idenfitier, define interface specific
settings in that namespace, and give the name of the namespace when
configuring which interface each group should use.

Numeric values use '.' as the decimal separator and can be suffixed with
kilo (k) mega (M) or giga (G) suffix. Boolean settings accept "true",
"false", "0", and "1" as values.

Many settings define paths to external data files. The paths can be relative
or absolute but the directory separator must be '/' in both Unix and Windows
environment.

Some variables contain comma-separated values, and for them the syntax is:
Namespace.key = value1, value2, value3, etc.

For run-indexed values the syntax is:
Namespace.key = [run1value; run2value; run3value; etc]
I.e., all values are given in brackets and values for different run are
separated by semicolon. Each value can also be a comma-separated value.
For more information about run indexing, go to section "Run indexing".

Setting files can contain comments too. A comment line must start with "#"
character. Rest of the line is skipped when the settings are read. This can
be also useful for disabling settings easily.

Some values (scenario and report names at the moment) support "value
filling". With this feature, you can construct e.g., scenario name
dynamically from the setting values. This is especially useful when using
run indexing. Just put setting key names in the value part prefixed and
suffixed by two percent (%) signs. These placeholders are replaces by the
current setting value from the configuration file. See the included
snw_comparison_settings.txt for an example.

File "default_settings.txt", if exists, is always read and the other
configuration files given as parameter can define more settings or override
some (or even all) settings in the previous files. The idea is that
you can define in the earlier files all the settings that are common for
all the simulations and run different, specific, simulations using
different configuration files.


Run indexing
------------

Run indexing is a feature that allows you to run large amounts of
different configurations using only single configuration file. The idea is
that you provide an array of settings (using the syntax described above)
for the variables that should be changed between runs. For example, if you
want to run the simulation using five different random number generator
seeds for movement models, you can define in the settings file the
following:

MovementModel.rngSeed = [1; 2; 3; 4; 5]

Now, if you run the simulation using command:

./one.sh -b 5 my_config.txt

you would run first using seed 1 (run index 0), then another run using
seed 2, etc. Note that you have to run it using batch mode (-b option) if
you want to use different values. Without the batch mode flag the first
parameter (if numeric) is the run index to use when running in GUI mode.

Run indexes wrap around: used value is the value at index (runIndex %
arrayLength). Because of wrapping, you can easily run large amount of
permutations easily. For example, if you define two key-value pairs:

key1 = [1; 2]
key2 = [a; b; c]

and run simulation using run-index count 6, you would get all permutations
of the two values (1,a; 2,b; 1,c; 2,a; 1,b; 2,c). This naturally works
with any amount of arrays. Just make sure that the smallest common
nominator of all array sizes is 1 (e.g., use arrays whose sizes are primes)
-- unless you don't want all permutations but some values should be
paired.


Movement models
---------------

Movement models govern the way nodes move in the simulation. They provide
coordinates, speeds and pause times for the nodes. The basic installation
contains, e.g., random waypoint, map based movement, shortest path map based
movement, map route movement, and external movement. All these models, except
external movement, have configurable speed and pause time distributions. A
minimum and maximum values can be given and the movement model draws
uniformly distributed random values that are within the given range. Same
applies for pause times. In external movement model the speeds and pause
times are interpreted from the given data.

When a node uses the random waypoint movement model (RandomWaypoint), it is
given a random coordinate in the simulation area. Node moves directly to the
given destination at constant speed, pauses for a while, and then gets a new
destination. This continues throughout the simulations and nodes move along
these zig-zag paths.

Map-based movement models constrain the node movement to predefined paths.
Different types of paths can be defined and one can define valid paths for
all node groups. This way e.g., cars can be prevented from driving indoors or
on pedestrian paths.

The basic map-based movement model (MapBasedMovement) initially distributes
the nodes between any two adjacent (i.e., connected by a path) map nodes and
then nodes start moving from adjacent map node to another. When node reaches
the next map node, it randomly selects the next adjacent map node but chooses
the map node where it came from only if that is the only option (i.e., avoids
going back to where it came from). Once node has moved trough 10-100 map
nodes, it pauses for a while and then starts moving again.

The more sophisticated version of the map-based movement model
(ShortestPathMapBasedMovement) uses Dijkstra's shortest path algorithm to
find its way trough the map area. Once a node reaches its destination, and
has waited for the pause time, a new random map node is chosen and node moves
there using the shortest path that can be taken using only valid map nodes.

For the shortest path based movement models, map data can also contain Points
Of Interest (POIs). Instead of selecting any random map node for the next
destination, the movement model can be configured to give a POI belonging to
a certain POI group with a configurable probability. There can be unlimited
amount of POI groups and all groups can contain any amount of POIs. All node
groups can have different probabilities for all POI groups. POIs can be used
to model e.g., shops, restaurants and tourist attractions.

Route based movement model (MapRouteMovement) can be used to model nodes that
follow certain routes, e.g. bus or tram lines. Only the stops on the route
have to be defined and then the nodes using that route move from stop to stop
using shortest paths and stop on the stops for the configured time.

All movement models can also decide when the node is active (moves and can be
connected to) and when not. For all models, except for the external movement,
multiple simulation time intervals can be given and the nodes in that group
will be active only during those times.

All map-based models get their input data using files formatted with a subset
of the Well Known Text (WKT) format. LINESTRING and MULTILINESTRING
directives of WKT files are supported by the parser for map path data. For
point data (e.g. for POIs), also the POINT directive is supported. Adjacent
nodes in a (MULTI)LINESTRING are considered to form a path and if some lines
contain some vertex(es) with exactly the same coordinates, the paths are
joined from those places (this is how you create intersections). WKT files
can be edited and generated from real world map data using any suitable
Geographic Information System (GIS) program. The map data included in the
simulator distribution was converted and edited using the free, Java based
OpenJUMP GIS program.

Different map types are defined by storing the paths belonging to different
types to different files. Points Of Interest are simply defined with WKT
POINT directive and POI groups are defined by storing all POIs belonging to a
certain group in the same file. All POIs must also be part of the map data so
they are accessible using the paths. Stops for the routes are defined with
LINESTRING and the stops are traversed in the same order they appear in the
LINESTRING. One WKT file can contain multiple routes and they are given to
nodes in the same order as they appear in the file.

The experimental movement model that uses external movement data
(ExternalMovement) reads timestamped node locations from a file and moves the
nodes in the simulation accordingly. See javadocs of ExternalMovementReader
class from input package for details of the format. A suitable, experimental
converter script (transimsParser.pl) for TRANSIMS data is included in the
toolkit folder.

The movement model to use is defined per node group with the "movementModel"
setting. Value of the setting must be a valid movement model class name from
the movement package. Settings that are common for all movement models are
read in the MovementModel class and movement model specific settings are read
in the respective classes. See the javadoc documentation and example
configuration files for details.

Routing modules and message creation
------------------------------------

Routing modules define how the messages are handled in the simulation. Six
basic active routing modules (First Contact, Epidemic, Spray and Wait, Direct
delivery, PRoPHET and MaxProp) and also a passive router for external routing
simulation are included in the package. The active routing modules are
implementations of the well known routing algorithms for DTN routing. There
are also variants of these models and couple of different models included in
the latest versions. See the classes in the routing package for details.

Passive router is made especially for interacting with other (DTN) routing
simulators or running simulations that don't need any routing functionality.
The router doesn't do anything unless commanded by external events. These
external events are provided to the simulator by a class that implements the
EventQueue interface.

There are two basic classes that can be used as a source of message events:
ExternalEventsQueue and MessageEventGenerator. The former can read events
from a file that can be created by hand, with a suitable script (e.g.,
createCreates.pl script in the toolkit folder), or by converting e.g.,
dtnsim2's output to suitable form. See StandardEventsReader class from input
package for details of the format. MessageEventGenerator is a simple message
generator class that creates uniformly distributed message creation patterns
with configurable message creation interval, message size and
source/destination host ranges. More specific messaging scenarios can be
created with MessageBurstGenerator, and One{From,To}EachMessageGenerator
classes. See javadocs for details.

The toolkit folder contains an experimental parser script (dtnsim2parser.pl)
for dtnsim2's output (there used to be a more capable Java-based parser but
it was discarded in favor of this more easily extendable script). The script
requires a few patches to dtnsim2's code and those can be found from the
toolkit/dtnsim2patches folder.

The routing module to use is defined per node group with the setting
"router". All routers can't interact properly (e.g., PRoPHET router can only
work with other PRoPHET routers) so usually it makes sense to use the same
(or compatible) router for all groups.

Reports
-------

Reports can be used to create summary data of simulation runs, detailed data
of connections and messages, files suitable for post-processing using e.g.,
Graphviz (to create graphs) and also to interface with other programs. See
javadocs of report-package classes for details.

There can be any number of reports for any simulation run and the number of
reports to load is defined with "Report.nrofReports" setting. Report class
names are defined with "Report.reportN" setting, where N is an integer value
starting from 1. The values of the settings must be valid report class names
from the report package. The output directory of all reports (which can be
overridden per report class with the "output" setting) must be defined with
Report.reportDir -setting. If no "output" setting is given for a report
class, the resulting report file name is "ReportClassName_ScenarioName.txt".

All reports have many configurable settings which can be defined using
ReportClassName.settingKey -syntax. See javadocs of Report class and specific
report classes for details (look for "setting id" definitions).

Host groups
-----------

A host group is group of hosts (nodes) that shares movement and routing
module settings. Different groups can have different values for the settings
and this way they can represent different types of nodes. Base settings can
be defined in the "Group" namespace and different node groups can override
these settings or define new settings in their specific namespaces (Group1,
Group2, etc.).

The settings
------------

There are plenty of settings to configure; more than is meaningful to
present here. See javadocs of especially report, routing and movement
model classes for details. See also included settings files for examples.
Perhaps the most important settings are the following.


Scenario settings:
---

Scenario.name
Name of the scenario. All report files are by default prefixed with this.

Scenario.simulateConnections
Should connections be simulated. If you're only interested in movement
modeling, you can disable this to get faster simulation. Usually you want
this to be on.

Scenario.updateInterval
How many seconds are stepped on every update. Increase this to get faster
simulation, but then you'll lose some precision. Values from 0.1 to 2 are good
for simulations.

Scenario.endTime
How many simulated seconds to simulate.

Scenario.nrofHostGroups
How many hosts group are present in the simulation.


Interface settings (used to define the possible interfaces the nodes can have)
---

type
What class (from the interfaces-directory) is used for this interface

The remaining settings are class-specific.  Can be for example:

transmitRange
Range (meters) of the interface.

transmitSpeed
Transmit speed of the interface (bytes per second).


Host group settings (used in Group or GroupN namespace):
---

groupID
Group's identifier (a string or a character). Used as the prefix of host
names that are shown in the GUI and reports. Host's full name is
groupID+networkAddress.

nrofHosts
Number of hosts in this group.

nrofInterfaces
Number of interfaces this the nodes of this group use

interfaceX
The interface that should be used as the interface number X

movementModel
The movement model all hosts in the group use. Must be a valid class (one
that is a subclass of MovementModel class) name from the movement package.

waitTime
Minimum and maximum (two comma-separated decimal values) of the wait time
interval (seconds). Defines how long nodes should stay in the same place
after reaching the destination of the current path. A new random value within
the interval is used on every stop. Default value is 0,0.

speed
Minimum and maximum (two comma-separated decimal values) of the speed
interval (m/s). Defines how fast nodes move. A new random value is used on
every new path. Default value is 1,1.

bufferSize
Size of the nodes' message buffer (bytes). When the buffer is full, node can't
accept any more messages unless it drops some old messages from the buffer.

router
Router module which is used to route messages. Must be a valid class
(subclass of MessageRouter class) name from routing package.

activeTimes
Time intervals (comma-separated simulated time value tuples: start1, end1,
start2, end2, ...) when the nodes in the group should be active. If no
intervals are defined, nodes are active all the time.

msgTtl
Time To Live (simulated minutes) of the messages created by this host group.
Nodes (with active routing module) check every one minute whether some of
their messages' TTLs have expired and drop such messages. If no TTL is
defined, infinite TTL is used.


Group and movement model specific settings (only meaningful for certain
movement models):

pois
Points Of Interest indexes and probabilities (comma-separated
index-probability tuples: poiIndex1, poiProb1, poiIndex2, poiProb2, ... ).
Indexes are integers and probabilities are decimal values in the range of
0.0-1.0. Setting defines the POI groups where the nodes in this host group
can choose destinations from and the probabilities for choosing a certain POI
group. For example, a (random) POI from the group defined in the POI file1
(defined with PointsOfInterest.poiFile1 setting) is chosen with the
probability poiProb1. If the sum of all probabilities is less than 1.0, a
probability of choosing any random map node for the next destination is (1.0
- theSumOfProbabilities). Setting can be used only with
ShortestPathMapBasedMovement -based movement models.

okMaps
Which map node types (refers to map file indexes) are OK for the group
(comma-separated list of integers).  Nodes will not travel trough map nodes
that are not OK for them. As default, all map nodes are OK. Setting can be
used with any MapBasedMovent -based movement model.

routeFile
If MapRouteMovement movement model is used, this setting defines the route
file (path) where the route is read from. Route file should contain
LINESTRING WKT directives. Each vertex in a LINESTRING represents one stop
on the route.

routeType
If MapRouteMovement movement model is used, this setting defines the routes
type. Type can be either circular (value 1) or ping-pong (value 2). See
movement.map.MapRoute class for details.


Movement model settings:
---

MovementModel.rngSeed
The seed for all movement models' random number generator. If the seed and
all the movement model related settings are kept the same, all nodes should
move the same way in different simulations (same destinations and speed &
wait time values are used).

MovementModel.worldSize
Size of the simulation world in meters (two comma separated values:
width, height).

PointsOfInterest.poiFileN
For ShortestPathMapBasedMovement -based movement models, this setting defines
the WKT files where the POI coordinates are read from. POI coordinates are
defined using the POINT WKT directive. The "N" in the end of the setting must
be a positive integer (i.e., poiFile1, poiFile2, ...).

MapBasedMovement.nrofMapFiles
How many map file settings to look for in the settings file.

MapBasedMovement.mapFileN
Path to the Nth map file ("N" must be a positive integer). There must be at
least nrofMapFiles separate files defined in the configuration files(s). All
map files must be WKT files with LINESTRING and/or MULTILINESTRING WKT
directives. Map files can contain POINT directives too, but those are
skipped. This way the same file(s) can be used for both POI and map data. By
default the map coordinates are translated so that the upper left corner of
the map is at coordinate point (0,0). Y-coordinates are mirrored before
translation so that the map's north points up in the playfield view. Also all
POI and route files are translated to match to the map data transformation.


Report settings:
---

Report.nrofReports
How many report modules to load. Module names are defined with settings
"Report.report1", "Report.report2", etc. Following report settings can be
defined for all reports (using Report name space) or just for certain reports
(using ReportN name spaces).

Report.reportDir
Where to store the report output files. Can be absolute path or relative to
the path where the simulation was started. If the directory doesn't exists,
it is created.

Report.warmup
Length of the warm up period (simulated seconds from the start). During the
warm up the report modules should discard the new events. The behavior is
report module specific so check the (java)documentation of different report
modules for details.


Event generator settings:
---

Events.nrof
How many event generators are loaded for the simulation. Event generator
specific settings (see below) are defined in EventsN namespaces (so
Events1.settingName configures a setting for the 1st event generator etc.).

EventsN.class
Name of the generator class to load (e.g., ExternalEventsQueue or
MessageEventGenerator). The class must be found from the input package.

For the ExternalEventsQueue you must at least define the path to the external
events file (using setting "filePath"). See input.StandardEventsReader class'
javadocs for information about different external events.


Other settings:
---

Optimization.randomizeUpdateOrder
Should the order in which the nodes' update method is called be randomized.
Call to update causes the nodes to check their connections and also update
their routing module. If set to false, node update order is the same as their
network address order. With randomizing, the order is different on every time
step.

Optimization.cellSizeMult
Adjust the trade-off between memory consumption and simulation speed.
Especially useful for large maps. See ConnectivityOptimizer class for details.


GUI
===

The GUI's main window is divided into three parts. The main part contains
the playfield view (where node movement is displayed) and simulation and
GUI control and information. The right part is used to select nodes and
the lower part is for logging and breakpoints.

The main part's topmost section is for simulation and GUI controls. The
first field shows the current simulation time. Next field shows the
simulation speed (simulated seconds per second). The following four
buttons are used to pause, step, fast forward, and fast forward simulation
to given time. Pressing step-button multiple times runs simulation
step-by-step. Fast forward (FFW) can be used to skip uninteresting parts
of simulation. In FFW, the GUI update speed is set to a large value. Next
drop-down is used to control GUI update speed. Speed 1 means that GUI is
updated on every simulated second. Speed 10 means that GUI is updated only
on every 10th second etc. Negative values slow down the simulation. The
following drop-down controls the zoom factor. The last button saves the
current view as a png-image.

Middle section, i.e., the playfield view, shows the node placement, map
paths, node identifiers, connections among nodes etc. All nodes are
displayed as small rectangles and their radio range is shown as a green
circle around the node. Node's group identifier and network address (a
number) are shown next to each node. If a node is carrying messages, each
message is represented by a green or blue filled rectangle. If node
carries more than 10 messages, another column of rectangles is drawn for
each 10 messages but every other rectangle is now red. You can center the
view to any place by clicking with mouse button on the play field. Zoom
factor can also be changed using mouse wheel on top of the playfield view.

The right part of main window is for choosing a node for closer inspection.
Simply clicking a button shows the node info in main parts lower section.
From there more information can be displayed by selecting one of the
messages the node is carrying (if any) from the drop-down menu. Pressing
the "routing info" button opens a new window where information about the
routing module is displayed. When a node is chosen, the playfield view is
also centered on that node and the current path the node is traveling is
shown in red.

Logging (the lowest part) if divided to two sections, control and log. From
the control part you can select what kind of messages are shown in the
log. You can also define if simulation should be paused on certain type of
event (using the check boxes in the "pause" column). Log part displays time
stamped events. All nodes and message names in the log messages are
buttons and you can get more information about them by clicking the
buttons.


DTN2 Reference Implementation Connectivity
==========================================

DTN2 connectivity allows bundles to be passed between the ONE and any
number of DTN2 routers. This is done through DTN2's External Convergence
Layer Interface.

When DTN2 connectivity is enabled ONE will connect to dtnd routers as
an external convergence layer adapter. ONE will also automatically configure
dtnd through a console connection with a link and route for bundles to reach
the simulator.

When a bundle is received from dtnd, ONE attempts to match the destination EID
against the regular expressions configured in the configuration file (see DTN2
Connectivity Configuration File below). For each matching node a copy of a
message is created and routed inside ONE. When the bundle reaches its destination
inside ONE it is delivered to the dtnd router instance attached to the node.
Copies of the bundle payload are stored within 'bundles' directory.

To enable this functionality the following steps must be taken:

1) DTN2 must be compiled and configured with ECL support enabled.
2) DTN2Events event generator must be configured to be loaded into ONE
   as an events class.
3) DTN2Reporter must be configured and loaded into one as a report class.
4) DTN2 connectivity configuration file must be configured as DTN2.configFile

To start the simulation:
1) Start all the dtnd router instances.
2) Start ONE.

Example Configuration (2-4 above)
---------------------------------

Events.nrof = 1
Events1.class = DTN2Events
Report.nrofReports = 1
Report.report1 = DTN2Reporter
DTN2.configFile = cla.conf

DTN2 Connectivity Configuration File
------------------------------------

The DTN2 connectivity configuration file defines which nodes inside ONE
should connect to which DTN2 router instances. It also defines the EID's
that the nodes match.

The configuration file is composed of comment lines starting with # and
configuration lines with the following format:

<nodeID> <EID regexp> <dtnd host> <ECL port> <console port>

The fields have the following meaning:

nodeID:		The ID of a node inside ONE (integer >= 0)
EID regexp:	Incoming bundles whose destination EID matches this regexp
		will be forwarded to the node inside ONE.
		(see java.util.regex.Pattern)
dtnd host:	Hostname/IP of the dtnd router to connect to this node.
ECL port:	dtnd router's port listening to ECLAs
console port:	dtnd router's console port

Example:
# <nodeID> <EID regexp> <dtnd host> <ECL port> <console port>
1 dtn://local-1.dtn/(.*) localhost 8801 5051
2 dtn://local-2.dtn/(.*) localhost 8802 5052

Known Issues
------------

For DTN2 connectivity related issues, you can contact teemuk@netlab.tkk.fi

-Quitting dtnd router instances connected to ONE will cause ONE to quit.


Toolkit
=======

The simulation package includes a folder called "toolkit" that contains
scripts for generating input and processing the output of the simulator. All
(currently included) scripts are written with Perl (http://www.perl.com/) so
you need to have it installed before running the scripts. Some post processing
scripts use gnuplot (http://www.gnuplot.info/) for creating graphics. Both of
the programs are freely available for most of the Unix/Linux and Windows
environments. For Windows environment, you may need to change the path to the
executables for some of the scripts.

getStats.pl
This script can be used to create bar-plots of various statistics gathered by
the MessageStatsReport -report module. The only mandatory option is "-stat"
which is used to define the name of the statistics value that should be parsed
from the report files (e.g., "delivery_prob" for message delivery
probabilities). Rest of the parameters should be MessageStatsReport output
filenames (or paths). Script creates three output files: one with values from
all the files, one with the gnuplot commands used to create the graphics and
finally an image file containing the graphics. One bar is created to the plot
for each input file. The title for each bar is parsed from the report filename
using the regular expression defined with "-label" option. Run getStats.pl
with "-help" option for more help.

ccdfPlotter.pl
Script for creating Complementary(/Inverse) Cumulative Distribution Function
plots (using gluplot) from reports that contain time-hitcount-tuples. Output
filename must be defined with the "-out" option and rest of the parameters
should be (suitable) report filenames. "-label" option can be used for
defining label extracting regular expression (similar to one for the getStats
script) for the legend.

createCreates.pl
Message creation pattern for the simulation can be defined with external events
file. Such a file can be simply created with any text editor but this script
makes it easier to create a large amount of messages. Mandatory options are
the number of messages ("-nrof"), time range ("-time"), host address range
("-hosts") and message size range ("-sizes"). The number of messages is simply
an integer but the ranges are given with two integers with a colon (:) between
them. If hosts should reply to the messages that they receive, size range of
the reply messages can be defined with "-rsizes" option. If a certain random
number generator seed should be used, that can be defined with "-seed" option.
All random values are drawn from a uniform distribution with inclusive minimum
value and exclusive maximum value. Script outputs commands that are suitable
for external events file's contents. You probably want to redirect the output
to some file.

dtnsim2parser.pl and transimsParser.pl
These two (quite experimental) parsers convert data from other programs to a
form that is suitable for ONE. Both take two parameters: input and output
file. If these parameters are omitted, stdin and stdout are used for input and
output. With "-h" option a short help is printed.
dtnsim2parser converts dtnsim2's (http://watwire.uwaterloo.ca/DTN/sim/) output
(with verbose mode 8) to an external events file that can be fed to ONE. The
main idea of this parser is that you can first create a connectivity pattern
file using ONE and ConnectivityDtnsim2Report, feed that to dtnsim2 and then
observe the results visually in ONE (using the output converted with
dtnsim2parser as the external events file).
transimsParser can convert TRANSIM's (http://transims-opensource.net/) vehicle
snapshot files to external movement files that can be used as an input for
node movement. See ExternalMovement and ExternalMovementReader classes for
more information.
