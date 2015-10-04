#! /usr/bin/perl
package Toolkit;

use strict;
use warnings;
use Getopt::Long;

# Creates external message create events (random uniform dist)

# smallest time step between two consecutive messages
my $step = 0.1;
# time stamp precision (number of decimals)
my $prec = 1;
# prefix of message IDs
my $msgPrefix = "M";
# prefix of hosts
my $hostPrefix = "";
# index of first message
my $msgIndex = 1;

my ($randSeed,  $nrofMessages, $times, $hosts, $sizes, $rsizes, $help);
    
GetOptions("seed=i" => \$randSeed, "time=s" => \$times, 
	   "nrof=i" => \$nrofMessages, 
	   "hosts=s" => \$hosts, 
	   "sizes=s" => \$sizes,
	   "rsizes=s" => \$rsizes,
	   "help|?!" => \$help);
    
unless (defined($help)) {
    unless (defined($times) and defined($nrofMessages)
	    and defined($hosts) and defined($sizes)) {
	print "Missing required parameters\n";
	$help = 1;
    }
}

if ($help) {
    print 'Usage:
 -time <startTime>:<endTime> -nrof <nrofMessages>
 -hosts <minHostAddr>:<maxHostAddr> -sizes <minMsgSize>:<maxMsgSize>
 [-rsizes <respMinSize>:<respMaxSize>] [-seed <randomSeed>]

 Minimum values are inclusive, max values are exclusive.
';
    exit();
}

my ($minHost, $maxHost) = $hosts =~ m/(\d+):(\d+)/;
my ($start, $end) = $times =~ m/(\d+):(\d+)/;
my ($minSize, $maxSize) = $sizes =~ m/(\d+):(\d+)/;

my ($rMinSize, $rMaxSize);

if (defined($rsizes)) {
    ($rMinSize, $rMaxSize) = $rsizes =~ m/(\d+):(\d+)/;
}

my $nrof = 0;
die "minHost >= maxHost" if $minHost >= $maxHost;

if (defined($randSeed)) {
    srand($randSeed);
}

for (my $time = $start; $time < $end; $time += $step) {
    my $prob = $step * (($nrofMessages - $nrof) / ($end - $time));
    
    if (rand() < $prob) {
	my $from = int(rand() * ($maxHost-$minHost) + $minHost);
	my $to = $from;
	while ($to == $from) { # make sure $to and $from are not the same
	  $to = int(rand() * ($maxHost-$minHost) + $minHost);
	}
	my $size = int(rand() * ($maxSize-$minSize) + $minSize);
	
	printf "%.${prec}f",$time;
	print "\tC\t$msgPrefix$msgIndex\t$hostPrefix$from\t$hostPrefix$to";
	print "\t$size";

	if (defined($rMinSize) and defined($rMaxSize)) {
	    my $rsize = int(rand() * ($rMaxSize-$rMinSize) + $rMinSize);
	    print "\t$rsize";
	}
	print "\n";

	$msgIndex++;
	$nrof++;
    }
}
