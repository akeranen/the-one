#! /sw/bin/perl

# MessageCopyCount report output analyzer

package Toolkit;
use strict;
use warnings;
use FileHandle;
use Getopt::Long;

my $usage = '
usage: [-help] [-perc <percentile>] [-foreach] [-verbose]
       <input file name>
';


my ($help, $perc, $doForEach, $verbose);

GetOptions("help|?!" => \$help, 
	"perc=i" => \$perc,
	"foreach" => \$doForEach,
	"verbose" => \$verbose);

if (not $help and (not @ARGV)) {
  print "Missing required parameter(s)\n";
  print $usage;
  exit();
}


if ($help) {
  print 'MessageCopyCountReport analyzer.';
  print "\n$usage";
  print '
options: 
 perc  How manyth percentiles to show (default = 10)
';
  exit();
}

$perc = 10 unless defined $perc;

sub getPercs {
	my @sorted = sort {$a<=>$b} @_;	
	my $lowIndex = int(@sorted * ($perc/100));
	my $highIndex = @sorted - $lowIndex - 1;
	my $medIndex = int(@sorted/2);

	return ($sorted[$medIndex], $sorted[$lowIndex], $sorted[$highIndex]);
}

my ($time, $lastTime, @ids, @counts);
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

	if ($doForEach) {
		next; # not doing per time stamp but per message
	}

	my ($med, $low, $high) = getPercs(@counts);
	print "$time " . int(@counts) . " $med $low $high $timeSum\n";
	$timeSum = 0;
	@counts = ();
    next;
  }
  
  my ($msgId, $count) = m/^\D+(\d+) (\d+)$/;
  die "No valid message count at line $_" unless (defined $msgId and defined $count);
  $timeSum += $count;
  push (@counts, $count);
  
  if ($doForEach) {
	$foreachSums{$msgId} = [()] unless $foreachSums{$msgId};
	push @{ $foreachSums{$msgId} }, $count;
  }
}

if ($doForEach) {
	my $relTime = 0;
	
	while (my $count = keys %foreachSums) {
		my $sum = 0;
		foreach my $id (keys %foreachSums) {
			my $value = shift @{ $foreachSums{$id} };
			delete $foreachSums{$id} unless @{ $foreachSums{$id} };

			$sum += $value;
			if ($verbose) {
				print "# $id $value\n";
			}
		}
		print "$relTime " . $sum/$count . " $count\n"; 
		$relTime += $timeStep;
	}
}