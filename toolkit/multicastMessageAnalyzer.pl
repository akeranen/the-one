#!/usr/bin/perl
use warnings FATAL => 'all';
use strict;

# Multicast message analyzer: Prints
#
#* Average group message delivery ratio per time
#* Minimum group message delivery ratio per time
#
# Parses the MulticastDeliveryReport to do so, i.e. translates a file of the form
#   #message, group, sent, received, ratio
#   <MessageId> <GroupAddress> <SentTime> <RecvdTime> <DelRatio>
#   ...
#   <MessageId> <GroupAddress> <SentTime> <RecvdTime> <DelRatio>
#   <TotalSimulatorTime>
# where lines are printed after creation and each delivery into a file of the form
#   SimTime	MinRatio	AvgRatio
#   <time after creation>	<min>	<avg>
#   ...


# Parse command line parameters.
if (not defined $ARGV[0] or not defined $ARGV[1]) {
    print "Usage: <inputFile> <timeStep>\n";
    exit();
}
my $infile = $ARGV[0];
my $timeStep = $ARGV[1];

# Define useful fields:

#Maps intervals (= time after message creation) to maps of messages and their delivery ratio during this interval
my %intervalToAvgs = ();
#Maps a message to the time it was created
my %msgToCreateTime = ();
#Maps a message to the highest interval it is used in
#This is used to only take messages into account, that existed in the given time interval
my %msgToMaxInterval = ();


# Matches a message line.
my $messageLineMatcher = '^\D+(\d+) (\d+) (\d+) (\d+) (\d+([.]\d+)?)$';
# Matches the last report line, i.e. the total simulation time.
my $simTimeLineMatcher = '^(\d+([.]\d+)?)$';

# Read multicast report.
open(INFILE, "$infile") or die "Can't open $infile : $!";
while(<INFILE>) {
    # Try to parse lines either of type <msgId> <group> <ctime> <rtime <ratio> or <simtime>.
    my ($msgId, $groupAddr, $createTime, $recvTime, $ratio) = m/$messageLineMatcher/;
    my ($simTime) = m/$simTimeLineMatcher/;

    # Ignore lines that are not of this kind.
    if (not defined $createTime and not defined $simTime) {
        next;
    }

    # Handle sim time lines in a special way.
    if (defined $simTime) {
        # Calculates the highest interval for every node, in which it has to be taken to account
        calculateMaxIntervalForAllMsgs($simTime);
        # Sim time line should be last line.
        last;
    }
	#calculate the interval this message was delivered in
    my $timeInterval = int(($recvTime - $createTime) / $timeStep + 1);
	
	$msgToCreateTime{$msgId} = $createTime;
	#put the message and its ratio in the map for the calculated interval
    $intervalToAvgs{$timeInterval}{$msgId} = $ratio;
}

close(INFILE);

#Map, that stores the latest delivery ratio for every message
my %msgToLastRatio = ();

print "SimTime	MinRatio	AvgRatio\n";

#Sort intervals numerically and process every interval step by step
foreach my $interval ( sort {$a <=> $b} keys %intervalToAvgs){
	#for every message delivered during this interval, update the latest delivery ratio
    foreach my $msg (keys %{$intervalToAvgs{$interval}}) {
        $msgToLastRatio{$msg} = $intervalToAvgs{$interval}{$msg};
    }
    printNextInterval($interval);
}

#calculates and prints the min and average for the given interval
sub printNextInterval{
    my $interval = shift;
    my $total = 0;
    my $nextMin = 2;
	my $msgCount = 0;
	#check every message
    foreach my $msg (keys %msgToLastRatio){
		#ignore it, if it didn't exist anymore during the current interval
		if ($msgToMaxInterval{$msg} < $interval){
			next;
		}
		#add it to the min and avg calculation for the current interval
		$msgCount++;
		my $msgRatio = $msgToLastRatio{$msg};
        $total += $msgRatio;
        if ($nextMin > $msgRatio){
            $nextMin = $msgRatio;
        }
    }
	#calculate average
    my $nextAvg = $total / $msgCount;
	#convert the interval into simulation seconds
	my $seconds = $interval * $timeStep;
    print "$seconds    $nextMin    $nextAvg\n";
}

sub calculateMaxIntervalForAllMsgs {
    # Get final simulator time.
    my $simTime = shift;
	
	foreach my $msgId (keys %msgToCreateTime){
		$msgToMaxInterval{$msgId} = int($simTime - $msgToCreateTime{$msgId}) / $timeStep;
	}

}
