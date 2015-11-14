#! /sw/bin/perl

# MessageAvailability report output analyzer

package Toolkit;
use strict;
use warnings;
use FileHandle;
use Getopt::Long;

my $usage = '
usage: [-help] [-fname] [-perc <percentile>] [-count] [-icounts] [-ids] [-avg] [-verbose]
       <input file name>
';


my ($help, $showFileName, $perc, $showIds, $showCount, $showIndividualCounts, $avg, $verbose);

GetOptions("help|?!" => \$help,
  "fname" => \$showFileName,
	"perc=i" => \$perc,
	"ids" => \$showIds,
  "count" => \$showCount,
	"icounts" => \$showIndividualCounts,
  "avg" => \$avg,
	"verbose" => \$verbose);

if (not $help and (not @ARGV)) {
  print "Missing required parameter(s)\n";
  print $usage;
  exit();
}


if ($help) {
  print 'MessageAvailabilityReport analyzer.';
  print "\n$usage";
  print '
options:
 perc  How manyth percentiles to show
 TBD
';
  exit();
}


sub getPercs {
	my @sorted = sort {$a<=>$b} @_;
	my $lowIndex = int(@sorted * ($perc/100));
	my $highIndex = @sorted - $lowIndex - 1;
	my $medIndex = int(@sorted/2);

	return ($sorted[$medIndex], $sorted[$lowIndex], $sorted[$highIndex]);
}

my %msgCounts;
my $timeStep = 0;
my ($time, $lastTime);
my $output = "";
my $legend = "#";

# Input example:
# [120]
# n1 M2 M1
# n43 M6 M8 M1

if ($showFileName) {
  $output = $ARGV[0];
  $legend .= " file-name";
}

while (<>) {
  if (m/^\s*$/ or m/^#/) {
    next; # skip empty and comment lines
  }

  if (m/^\[(\d*)\]/) { # next timestamp
    $lastTime = $time;
    $time = $1;

	if (not defined $lastTime) { # first timestamp
		$timeStep = $time;
	}
    next;
  }

  my @vals = split(' ');
  my $hostId = shift @vals;

  foreach my $msgId (@vals) {
    $msgCounts{$msgId} = {()} unless $msgCounts{$msgId};

    $msgCounts{$msgId}{$hostId} = 0 unless $msgCounts{$msgId}{$hostId};
    $msgCounts{$msgId}{$hostId}++;
  }
}

my @counts;
my $sum = 0;

foreach my $msgId (keys %msgCounts) {
  my %hostIds = %{$msgCounts{$msgId}};
  my $count = keys(%hostIds);
  push @counts, $count;

  $sum += $count;

  if ($showIndividualCounts) {
    print "$msgId $count";
    if ($showIds) {
      print " " . join(' ', keys(%hostIds));
    }
    print "\n";
  }

  if ($verbose) {
    print map { "$_ => $hostIds{$_} " } keys %hostIds;
    print "\n";
  }
}

if (defined $perc) {
  print "Counts: @counts\n" if $verbose;
  my ($mid, $low, $high) = getPercs(@counts);
  $output = "$output $low $mid $high";
  $legend = "$legend low-perc-$perc median high-perc-$perc";
}

if ($showCount) {
  $output .= " " . scalar(keys %msgCounts);
  $legend .= " nrof-seen-messages";
}

if ($avg) {
  $output = $output . " " . $sum / (scalar(keys %msgCounts));
  $legend = "$legend avg-host-per-msg";
}

print "$legend\n";
print "$output\n";
