#! /usr/local/bin/perl

# UMass Dieselnet -> the ONE trace converter
# Suitable (at least) for "DieselNet Fall 2007" from
# http://traces.cs.umass.edu/index.php/Network/Network

package Toolkit;
use strict;
use warnings;
use FileHandle;
use Getopt::Long;

my $usage = '
usage: -out <output file name> [-help] [-first <first time stamp>]
       <input file name>
';

my ($help, $outFileName, $first);

GetOptions("out=s"=>\$outFileName, "first=i"=>\$first, "help|?!" => \$help);

$first = -1 unless defined $first; # no adjustment by default

if (not $help and (not $outFileName or not @ARGV)) {
  print "Missing required parameter(s)\n";
  print $usage;
  exit();
}

if ($help) {
  print 'Dieselnet trace converter.';
  print "\n$usage";
   print '
options:
out   Name of the output file

first Time stamp for the first connection event. Adjusts all timestamps so
      that the first event happens at this time (by N seconds from the start
      of the simulation). By default, the times are not adjusted at all.
';
  exit();
}

# Input example:
# PVTA_3201 PVTA_3117 0:16:14 235560.0 584.0 42.38768 -72.52352

# Output example:
# 974 CONN 12 7 up
# 1558 CONN 12 7 down

my $inputFile = shift @ARGV;
my $inFh = new FileHandle;
my $outFh = new FileHandle;
$inFh->open("<$inputFile") or die "Can't open input file $inputFile";
$outFh->open(">$outFileName") or die "Can't create outputfile $outFileName";

print $outFh "# Connection trace file for the ONE. Converted from $inputFile \n";

my @lines = <$inFh>; # read whole file to array
my @output;
my $nextNodeId = 0;
my ($nodeId1, $nodeId2, %nodeIds);

foreach (@lines) {
  if (m/^\s$/) {
    next; # skip empty lines
  }

  my ($node1, $node2, $h, $m, $s, $duration) =
    m/(\w+) (\w+) (\d+):(\d+):(\d+) [\d\.E]+ (\d+\.\d+)/;
  die "Invalid input line: $_" unless ($node1 and $node2);
  my $time = $h * 3600 + $m * 60 + $s;

  # map node IDs consistently to network addresses
  if (exists $nodeIds{$node1}) {
    $nodeId1 = $nodeIds{$node1};
  }
  else {
    $nodeId1 = $nextNodeId;
    $nodeIds{$node1} = $nextNodeId;
    $nextNodeId++;
  }
  if (exists $nodeIds{$node2}) {
    $nodeId2 = $nodeIds{$node2};
  }
  else {
    $nodeId2 = $nextNodeId;
    $nodeIds{$node2} = $nextNodeId;
    $nextNodeId++;
  }

  my $conEndTime = $time + $duration;
  push(@output, "$time CONN $nodeId1 $nodeId2 up");
  push(@output, "$conEndTime CONN $nodeId1 $nodeId2 down");
}

# sort result by time stamp
@output = sort
{
  my ($t1) = $a =~ m/^(\d+)/;
  my ($t2) = $b =~ m/^(\d+)/;
  $t1 <=> $t2;
} @output;

# adjust time stamps (if requested with cmd line option)
if ($first != -1) {
  my ($firstTime) = $output[0] =~ m/^(\d+)/;
  my $diff = $firstTime - $first;
  print "Adjusting timestamps by $diff seconds\n";
  foreach (@output) {
    my ($ts) = m/^(\d+)/;
    my $newTime = $ts - $diff;
    s/$ts/$newTime/;
  }
}

# print all the result lines to output file
print $outFh join("\n", @output);

print "Node name to network ID mapping:\n";
while (my ($k,$v) = each %nodeIds ) {
    print "$k => $v\n";
}

$outFh->close();
$inFh->close();
