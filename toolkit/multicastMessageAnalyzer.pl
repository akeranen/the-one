#!/usr/bin/perl
use warnings FATAL => 'all';
use strict;

# Broadcast message analyzer: Prints
# * Average reached people per time after creation for broadcasts depending on priority
# * Minimum reached people per time after creation for broadcasts depending on priority
#
# Parses the BroadcastDeliveryReport to do so, i.e. translates a file of the form
#   Time # Prio
#   <SimulatorTime> <MessageId> <MessagePrio>
#   ...
#   <SimulatorTime> <MessageId> <MessagePrio>
#   <TotalSimulatorTime>
# where lines are printed after creation and each delivery into a file of the form
#   Reached people by broadcasts of prio <prio>
#   time after creation    avg   min
#   <time after creation>  <avg> <min>
#   ...
#
#   Reached people by broadcasts of prio <prio>
#   ...

# Parse command line parameters.
if (not defined $ARGV[0] or not defined $ARGV[1]) {
    print "Usage: <inputFile> <timeStep>\n";
    exit();
}
my $infile = $ARGV[0];
my $timeStep = $ARGV[1];

# Define useful fields:

# Maps priorities to a set of maps between a single message of that priority and a sequence of numbers indicating the
# number of people reached at timeStep, 2 * timeStep, 3 * timeStep, ... time steps after creation.
my %timeToAvgs = ();


# Matches a message line.
my $messageLineMatcher = '^\D+(\d+) (\d+) (\d+.\d+) (\d+.\d+) (\d+.\d+)$';
# Matches the last report line, i.e. the total simulation time.
my $simTimeLineMatcher = '^(\d+)$';

# Read broadcast report.
open(INFILE, "$infile") or die "Can't open $infile : $!";
while(<INFILE>) {
    # Try to parse lines either of type <time> <msgId> <prio> or of type <simTime>.
    my ($msgId, $groupAddr, $createTime, $recvTime, $ratio) = m/$messageLineMatcher/;
    my ($simTime) = m/$simTimeLineMatcher/;

    # Ignore lines that are not of this kind.
    if (not defined $createTime and not defined $simTime) {
        next;
    }

    # Handle sim time lines in a special way.
    if (defined $simTime) {
        # Add possibly missing time points in the statistics to reach the simulator time and the end of simulation.
        addMissingCrossedTimePointsToStatistics($simTime);
        # Sim time line should be last line.
        last;
    }
    my $timeInterval = int(($recvTime - $createTime) / $timeStep);
    $timeToAvgs{$timeInterval}{$msgId} = $ratio;

}

close(INFILE);

my %msgToLastRatio = ();

foreach my $interval ( sort {$a <=> $b} keys %timeToAvgs){
    foreach my $msg (keys %{$timeToAvgs{$interval}}) {
        $msgToLastRatio{$msg} = $timeToAvgs{$interval}{$msg};
    }
    printNextInterval($interval);
}

sub printNextInterval{
    my $interval = shift;
    my $nextAvg = 0;
    my $nextMin = 2;
    foreach my $msgRatio (%msgToLastRatio){
        $nextAvg += $msgRatio;
        if ($nextMin > $msgRatio){
            $nextMin = $msgRatio;
        }
    }
    my $msgCount = keys %msgToLastRatio;
    print "$nextAvg";
    $nextAvg = $nextAvg / $msgCount;
    print "$interval    $nextMin    $nextAvg\n";
}

sub addMissingCrossedTimePointsToStatistics {
    # Get final simulator time.
    my $simTime = shift;

}
