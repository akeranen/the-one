#! /usr/bin/perl

# MessageCopyCount report output analyzer & plotter

package Toolkit;
use strict;
use warnings;
use FileHandle;
use Getopt::Long;

my $usage = '
usage: [-help] [-foreach] [-countplot] [-verbose]
       <input file name>
';


my ($help, $perc, $doForEach, $countPlot, $verbose);

GetOptions("help|?!" => \$help, 
	"foreach" => \$doForEach,
  "countplot" => \$countPlot,
	"verbose" => \$verbose);

if (not $help and (not @ARGV)) {
  print "Missing required parameter(s)\n";
  print $usage;
  exit();
}

$doForEach = 1 if defined $countPlot;

if ($help) {
  print 'MessageCopyCountReport analyzer.';
  print "\n$usage";
  print '
options: 
 foreach   Do analyzing per message, not per timestamp
 countplot Use output that is suitable for message lifetime plots
';
  exit();
}


my ($time, $lastTime, @ids);
my $timeSum = 0;
my %foreachSums;
my $timeStep = 0;

# Input example:
# [120]
# M1 567
# M2 678

while (<>) {
  if (m/^\s*$/ or m/^#/) {
    next; # skip empty and comment lines
  }
  if (m/^\[(\d*)\]/) { # next timestamp
    $lastTime = $time;
    $time = $1;
	
	if (not defined $lastTime) { # first timestamp
		$timeStep = $time;
		next;
	}

		next; # not doing per time stamp but per message
  }
  
  my ($msgId, $count) = m/^\D+(\d+) (\d+)$/;
  die "No valid message count at line $_" unless (defined $msgId and defined $count);
  
	$foreachSums{$msgId} = [()] unless $foreachSums{$msgId};
	push @{ $foreachSums{$msgId} }, $count;
}

if ($doForEach) {
  my @sortedKeys = sort {$a<=>$b} keys %foreachSums;
  my $valuesLeft = 1;
  print "# " . join (' ', @sortedKeys) . "\n";
  
	while ($valuesLeft) {
		my $sum = 0;
    $valuesLeft = 0;
		foreach my $id (@sortedKeys) {
			my $value = shift @{ $foreachSums{$id} };

      if (not defined $value) {
        $value = 0; 
      } else {
        $valuesLeft = 1; # still values left
      }
       print "$value ";
		}

      print "\n";
 }
}
