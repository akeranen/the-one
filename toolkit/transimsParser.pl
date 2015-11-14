#! /usr/local/bin/perl

package Toolkit;

# Transims vehicle shapshot file parser

use strict;
use warnings;
use Common; # parseArgs and debug methods

parseArgs("Transims vehicle shapshot parser");

# (minTime, maxTime, minX, maxX, minY, maxY, minZ, maxZ)
my @limits;
my %initLocs;
my $header = <INFILE>;
my $firstLine = <INFILE>;

my ($x, $y, $z, $time, $id) = parseLine($firstLine);
initMinMax(0,$time);
initMinMax(1, $x);
initMinMax(2, $y);
initMinMax(3, $z);
$initLocs{$id} = [$x, $y, $z];

while(<INFILE>) {
    ($x, $y, $z, $time, $id) = parseLine($_);
    if (not defined($x)) {
	debug("Invalid line '$_'");
	next;
    }

    $initLocs{$id} = [$x, $y, $z] unless $initLocs{$id};

    upMinMax(0,$time);
    upMinMax(1,$x);
    upMinMax(2,$y);
    upMinMax(3,$z);
}

print OUTFILE "@limits\n";

seek(INFILE,0,0) or die "Can't seek input file";
$header = <INFILE>;

while ( my ($k, $v) = each %initLocs) {
    print OUTFILE "$limits[0] $k @$v\n";
}

while (<INFILE>) {
    my ($x, $y, $z, $time, $id) = parseLine($_);
    next unless defined $x;
    if ($time > $limits[0]) { # min time location are already there
	print OUTFILE "$time $id $x $y $z\n";
    }
}

sub initMinMax {
    my $index = shift;
    my $val = shift;

    $index *= 2;
    $limits[$index] = $val;
    $limits[$index+1] = $val;
}

sub upMinMax {
    my $index = shift;
    my $val = shift;

    $index *= 2;

    if ($val < $limits[$index]) { # update min
	#print "up min at $index $limits[$index] to $val\n";
	$limits[$index] = $val;
    }

    if ($val > $limits[$index+1]) { # update max
	#print "up max at $index $limits[$index+1] to $val\n";
	$limits[$index+1] = $val;
    }
}

sub parseLine {
    my $line = shift;
    my @data = split(/\s+/, $line);
    return ($data[4],$data[9],$data[5],$data[11], $data[3]);
}
