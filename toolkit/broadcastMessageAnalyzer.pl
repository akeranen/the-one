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
# where lines are printed after creation and each delivery into output of the form
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
my %prioToMsgStatistics = ();
# Maps message IDs to time of creation.
my %msgToCreation = ();
# Maps message IDs to priority.
my %msgToPrio = ();
# Maps message IDs to the number of reached people as counted up to now.
my %msgToCurrCount = ();
# Maps message IDs to the index in the sequence of numbers stored in reachedMessagesByPrio that will be added next.
my %msgToCurrTimeIdx = ();

# Matches a message line.
my $messageLineMatcher = '^(\d+) \D+(\d+) (\d+)$';
# Matches the last report line, i.e. the total simulation time.
my $simTimeLineMatcher = '^(\d+)$';

# Read broadcast report.
open(INFILE, "$infile") or die "Can't open $infile : $!";
while(<INFILE>) {
    # Try to parse lines either of type <time> <msgId> <prio> or of type <simTime>.
    my ($time, $msgId, $prio) = m/$messageLineMatcher/;
    my ($simTime) = m/$simTimeLineMatcher/;

    # Ignore lines that are not of this kind.
    if (not defined $time and not defined $simTime) {
        next;
    }

    # Handle sim time lines in a special way.
    if (defined $simTime) {
        # Add possibly missing time points in the statistics to reach the simulator time and the end of simulation.
        addMissingCrossedTimePointsToStatistics($simTime);
        # Sim time line should be last line.
        last;
    }

    # Handle lines of kind <time> <msgId> <prio>:

    # If this is the first time the message ID was printed (i. e. it was created at this time),
    # add it to the different stati stores.
    if (not defined $msgToCreation{$msgId}) {
        $msgToCreation{$msgId} = $time;
        $msgToPrio{$msgId} = $prio;
        $msgToCurrTimeIdx{$msgId} = 0;
        $msgToCurrCount{$msgId} = 0;
        next;
    }

    # Else: Advance time to new time stamp and check which time points are crossed.
    while ($time - $msgToCreation{$msgId} >= ($msgToCurrTimeIdx{$msgId} + 1) * $timeStep) {
        # If a time point which is a multiple of $timeStep was crossed,
        # store a new value for number of people reached...
        push @{ $prioToMsgStatistics{$prio}{$msgId} }, $msgToCurrCount{$msgId};
        # ...and update the index to use next.
        $msgToCurrTimeIdx{$msgId}++;
    }

    # Afterwards, update the number of reached people.
    $msgToCurrCount{$msgId}++;

    # If the currently processed time matches the last crossed time point, increase number of reached people in the
    # statistics by 1.
    if ($time - $msgToCreation{$msgId} == $msgToCurrTimeIdx{$msgId} * $timeStep and $msgToCurrTimeIdx{$msgId} > 0) {
        $prioToMsgStatistics{$prio}{$msgId}[$msgToCurrTimeIdx{$msgId} - 1]++;
    }
}

# Interpret data for each priority.
foreach my $prio (sort keys %prioToMsgStatistics) {
    print "Reached people by broadcasts of prio $prio by time after creation:\n";
    print "time\t avg\t min\n";
    analyzeMessagesOfPrio($prio);
    print "\n";
}

close(INFILE);

sub addMissingCrossedTimePointsToStatistics {
    # Get final simulator time.
    my $simTime = shift;

    # Foreach message:
    foreach my $msgId (keys %msgToCreation) {
        my $prio = $msgToPrio{$msgId};

        # Advance time and add entries in statistics until simulator end time is reached.
        while ($simTime - $msgToCreation{$msgId} >= ($msgToCurrTimeIdx{$msgId} + 1) * $timeStep) {
            push @{ $prioToMsgStatistics{$prio}{$msgId} }, $msgToCurrCount{$msgId};
            $msgToCurrTimeIdx{$msgId}++;
        }
    }
}

sub analyzeMessagesOfPrio {
    my $prio = shift;

    # A set of maps between a single message and a sequence of numbers indicating the number of people reached
    # by time point after creation.
    my %msgToStatistics = %{ $prioToMsgStatistics{$prio} };

    # Next time point: The first time step after message creation.
    my $nextTimePoint = $timeStep;

    # While we haven't analyzed enough time steps to have processed each value:
    while (my $msgCount = keys %msgToStatistics) {
        # Find the average and minimum of receivers at a certain time point after message creation.
        my $total = 0;
        my $min;
        # For doing that, iterate through all message IDs...
        foreach my $msg (keys %msgToStatistics) {
            # ...take a look at the number of receivers at that time...
            my $numReceiversAtTime = shift @{ $msgToStatistics{$msg} };
            # ...and delete the sequence of numbers from the main dictionary if done with it.
            delete $msgToStatistics{$msg} unless @{ $msgToStatistics{$msg} };

            # Then update the sum and minimum.
            $total += $numReceiversAtTime;
            if (not defined $min or $numReceiversAtTime < $min) {
                $min = $numReceiversAtTime;
            }
        }

        # Finally print statistics for the time point...
        printf("%d\t\t%.2f\t\t%d\n", $nextTimePoint, $total / $msgCount, $min);

        # ...and go to the next one.
        $nextTimePoint += $timeStep;
    }
}