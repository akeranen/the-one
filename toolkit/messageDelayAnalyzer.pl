#!/usr/bin/perl
use strict;
use warnings FATAL => 'all';

# Message delay analyzer: Prints
# * Message delay distribution in 1-to-1 messages
# * Message delay distribution in emergency messages
# * Message delay distribution in group messages
# depending on priority.
#
# Parses the ImmediateMessageDelayReport to do so, i.e. translates a file of the form
#   Type Prio Delay
#   <MessageType> <MessagePrio> <Delay>
#   <MessageType> <MessagePrio> <Delay>
#   ...
# where lines are printed after each delivery into output of the form
#   Delay distribution for delivered messages of type <FirstType>:
#   For priority <prio>:
#   Delay 0 <= x < <delayStepSize>: <% of delays between 0 and <delayStepSize>> (Total: <# delays between 0 and <delayStepSize>)
#   Delay <delayStepSize> <= x < 2 * <delayStepSize>: <% of delays between <delayStepSize> and 2 * <delayStepSize>> (Total: <# delays between <delayStepSize> and 2 * <delayStepSize>)
#   ...
#
#   For priority <prio>:
#   ...
#
#   Delay distribution for delivered messages of type <SecondType>:
#   ...

# To begin, parse the command line parameters.
if (not defined $ARGV[0] or not defined $ARGV[1]) {
    print "Usage: <inputFile> <delayStepSize>\n";
    exit();
}
my $infile = $ARGV[0];
my $delayStepSize = $ARGV[1];

# Statistics will be stored in a dictionary that maps each message type to a map between priorities and a sequence of
# numbers indicating the number of messages of that type and priority delivered between 0 and delayStepSize seconds,
# between delayStepSize and 2 * delayStepSize seconds, between 2 * delayStepSize and 3 * delayStepSize seconds, ...
my %msgTypeToStatistics = ();

# This regular expression matches a message line.
my $messageLineMatcher = '^(\w+) (\d+) (\d+)$';

# Start with actual script: open and read delay report.
open(INFILE, "$infile") or die "Can't open $infile : $!";
while(<INFILE>) {
    # Try to parse lines of type <msgType> <prio> <delay>.
    my ($type, $prio, $delay) = m/$messageLineMatcher/;

    # Ignore lines that are not of this kind.
    if (not defined $type) {
        next;
    }

    # Otherwise, update count for correct type, priority and delay class.
    my $delayClass = int($delay / $delayStepSize);
    $msgTypeToStatistics{$type}{$prio}[$delayClass]++;
}

# Interpret data for each message type and each priority.
foreach my $type (sort keys %msgTypeToStatistics) {
    print "Delay distribution for delivered messages of type $type:\n";
    foreach my $prio (sort keys %{ $msgTypeToStatistics{$type} }) {
        print "For priority $prio:\n";

        # Extract the numbers to interpret.
        my @statistics = @{ $msgTypeToStatistics{$type}{$prio} };
        # Find the sum of delivered messages to compute percentages.
        my $deliveredMessagesSum = sumUpArray(\@statistics);

        # Then go through each delay class...
        my $nextBorderPoint = $delayStepSize;
        while (@statistics) {
            # ...find the number of messages delivered in this timespan...
            my $numDeliveredInDelayClass = shift @statistics;
            if (not defined $numDeliveredInDelayClass) {
                $numDeliveredInDelayClass = 0;
            }

            # ...and print it both as percentage and as total message count.
            printf("Delay %4d <= x < %4d: %6.2f%% (Total: %d)\n",
                $nextBorderPoint - $delayStepSize,
                $nextBorderPoint,
                100 * $numDeliveredInDelayClass / $deliveredMessagesSum,
                $numDeliveredInDelayClass);

            $nextBorderPoint += $delayStepSize;
        }

        print "\n";
    }
}

# Sums up the array that was given as a reference.
sub sumUpArray {
    my @array = @{$_[0]};

    my $sum = 0;
    foreach (@array)
    {
        if (defined $_) {
            $sum += $_;
        }
    }
    return $sum;
}