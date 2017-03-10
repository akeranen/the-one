#!/usr/bin/perl
use warnings FATAL => 'all';
use strict;

# Broadcast message analyzer: Prints
# * Average reached people per time for broadcasts depending on priority
# * Minimum reached people per time for broadcasts depending on priority
#
# Parses the BroadcastMessageReport to do so, i.e. translates a file of the form
#   [<Simulator time stamp>]
#   <MessageId> <MessagePrio> <Reached people>
#   ...
#   <MessageId> <MessagePrio> <Reached people>
#   [<Simulator time stamp>]
#   ...
# into a file of the form
#   Reached people by broadcasts of prio <prio>
#   time after creation    avg   min
#   <time after creation>  <avg> <min>
#   ...
#
#   Reached people by broadcasts of prio <prio>
#   ...

# Parse command line parameters.
if (not defined $ARGV[0]) {
    print "Usage: <inputFile> \n";
    exit();
}
my $infile = $ARGV[0];

# Maps priorities to a set of maps between a single message of that priority and a sequence of numbers indicating the
# number of people reached.
my %reachedMessagesByPrio = ();

# The time step to use. Will be found by reading the file.
my $timeStep;

my $messageLineMatcher = '^\D+(\d+) (\d+) (\d+)$';

# Read broadcast report.
open(INFILE, "$infile") or die "Can't open $infile : $!";
while(<INFILE>) {
    # If first time stamp is read, store it as time step.
    if (m/^\[(\d*)\]/ and not defined $timeStep) {
        $timeStep = $1;
    }

    # Try to parse line of type <messageId> <prio> <count>.
    my ($msgId, $prio, $count) = m/$messageLineMatcher/;
    # Ignore lines that are not of this kind.
    if (not defined $msgId) {
        next;
    }

    # If this is the first time the prio was observed, add it to the main map.
    $reachedMessagesByPrio{$prio} = () unless $reachedMessagesByPrio{$prio};
    # If this is the first time the message ID was printed, add it to the messages stored by its priority.
    $reachedMessagesByPrio{$prio}{$msgId} = [()] unless $reachedMessagesByPrio{$prio}{$msgId};

    # Then push the current count.
    push @{ $reachedMessagesByPrio{$prio}{$msgId} }, $count;
}

# Interpret data for each priority.
foreach my $prio (sort keys %reachedMessagesByPrio) {
    print "Reached people by broadcasts of prio $prio by time after creation:\n";
    print "time\t avg\t min\n";
    analyzeMessagesOfPrio($prio);
    print "\n";
}

close(INFILE);

sub analyzeMessagesOfPrio {
    my $prio = shift;

    # A set of maps between a single message and a sequence of numbers indicating the number of people reached,
    # ordered by time.
    my %reachedPeopleByMessageId = %{ $reachedMessagesByPrio{$prio} };

    # Next time to print: Time after creation of the message.
    my $timeAfterCreation = 0;

    # While we haven't analyzed enough time steps to have processed each value:
    while (my $count = keys %reachedPeopleByMessageId) {
        # Find the average and minimum of receivers at a certain time step.
        my $sum = 0;
        my $min;
        # For doing that, iterate through all message IDs...
        foreach my $id (keys %reachedPeopleByMessageId) {
            # ...take a look at the number of receivers at that time...
            my $numReceiversAtTime = shift @{ $reachedPeopleByMessageId{$id} };
            # ...and delete the sequence of numbers from the main dictionary if done with it.
            delete $reachedPeopleByMessageId{$id} unless @{ $reachedPeopleByMessageId{$id} };

            # Then update the sum and minimum.
            $sum += $numReceiversAtTime;
            if (not defined $min or $numReceiversAtTime < $min) {
                $min = $numReceiversAtTime;
            }
        }

        # Finally print statistics for the time step...
        print "$timeAfterCreation\t".$sum / $count."\t $min\n";

        # ...and go to the next one.
        $timeAfterCreation += $timeStep;
    }
}