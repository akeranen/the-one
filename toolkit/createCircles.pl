#! /usr/bin/perl
package Toolkit;

use strict;
use warnings;
use Getopt::Long;
use Math::Trig;

# Creates node settings for a circular network with given number of rings

# prefix of hosts
my $hostPrefix = "g";
# number of rings

my ($rings, $radius, $help);
    
GetOptions("rings=n" => \$rings,
           "radius=n" => \$radius,
	   "help|?!" => \$help);
    
unless (defined($help)) {
    unless (defined($rings) and defined($radius)) {
	print "Missing required parameters\n";
	$help = 1;
    }
}

if ($help) {
    print "Usage:
 -rings <nrOfRings> -radius <radius>\n";
    exit();
}

my $orig = $rings * $radius + 2*$radius;
my $hostID = 1;
for (my $ring = 0; $ring <= $rings; $ring++) {

	if ($ring==0) {
		my $nrOfHosts = 1;
		for (my $tmp = 1; $tmp <= $rings; $tmp++) {
			$nrOfHosts+= $tmp*6;
		}
		print "Scenario.nrofHostGroups = $nrOfHosts\n";
		print "Group.movementModel = StationaryMovement\n";
		print "Group.nrofHosts = 1\n";
		print "Group.groupID = $hostPrefix\n";
		print "Group$hostID.nodeLocation = $orig, $orig\n";
		$hostID++;
	} else {
		my $nodes = $ring*6;
		my $angle = 2*pi/$nodes;
		for (my $node = 0; $node < $nodes; $node++) {
			my $x = int($orig + $ring*$radius*cos($node*$angle));
			my $y = int($orig + $ring*$radius*sin($node*$angle));
			print "Group$hostID.nodeLocation = $x , $y\n";
			$hostID++;
		}
	}
	
}
