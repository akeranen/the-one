#! /usr/bin/perl
package Toolkit;

# Distance vs. Delivery probability from DistanceDelayReport

use strict;
use warnings;

if (not defined $ARGV[0] or not defined $ARGV[1]) {
    print "Usage: <inputFile> <granularity> [plotXrangeMax]\n";
    exit();
}

my $infile = $ARGV[0];
open(INFILE, "$infile") or die "Can't open $infile : $!";

my $granularity = $ARGV[1];
my $xRange = $ARGV[2];

# strip 1-3 char extension (if any)
if ($infile =~ m/.*\.\w{1,3}/) {
    ($infile) = $infile =~ m/(.*)\.\w{1,3}/;
}

my $outfile = "$infile.dp";
my $plotfile = "$infile.dp.gnuplot";

my @nrofDelivered;
my @nrofNotDelivered;
my $maxIndex = 0;

open(PLOT, ">$plotfile") or die "Can't open plot output file $plotfile : $!";
open(OUTFILE, ">$outfile") or die "Can't open output file $outfile : $!";

while (<INFILE>) {
    if (m/^#/) {
	next; # skip comment lines
    }

    my ($dist, $time) = m/^([\d\.-]+) ([\d\.-]+)/;
    die "invalid input line $_" unless defined $dist;

    my $index = int($dist / $granularity); # map distance values to table indexes

    if ($time == -1) {
	$nrofNotDelivered[$index] = 0 unless defined $nrofNotDelivered[$index];
	$nrofNotDelivered[$index]++;
    }
    else {
	$nrofDelivered[$index] = 0 unless defined $nrofDelivered[$index];
	$nrofDelivered[$index]++;
    }

    if ($index > $maxIndex) {
	$maxIndex = $index;
    }
}

for (my $i=0; $i <= $maxIndex; $i++) {
    my $del = $nrofDelivered[$i];
    my $nDel = $nrofNotDelivered[$i];
    my $delProb;

    if (not defined($del) and not defined($nDel)) {
	next; # skip distance slots that don't have any data
    }
    elsif (not defined($del) or $del == 0) {
	$delProb = 0; # nothing delivered
    }
    elsif (not defined($nDel) or $nDel == 0) {
	$delProb = 1; # everything delivered
    }
    else {
	$delProb = $del / ($del + $nDel);
    }

    my $dist = $granularity * ($i+1);

    print OUTFILE "$dist $delProb\n";
}


print PLOT "set xlabel \"distance\"\n";
print PLOT "set ylabel \"delivery probability\"\n";
print PLOT "set yrange[0:1]\n";

if (defined $xRange) {
    print PLOT "set xrange[0:$xRange]\n";
}

print PLOT "set terminal emf\n";
print PLOT "plot '$outfile'\n";

close(INFILE);
close(OUTFILE);
close(PLOT);

system("gnuplot $plotfile > $outfile.emf") == 0 or die "Error running gnuplot: $?";
