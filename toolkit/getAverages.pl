#! /usr/bin/perl

# Gets averages from multiple time-stamped files

package Toolkit;
use strict;
use warnings;
use Getopt::Long;
use FileHandle;

my $error;
my $help;

my $usage = '
usage: [-error] [-help] <input file names>
';

GetOptions("error!" => \$error, "help|?!" => \$help);

if (not $help and not @ARGV) {
    print "Missing required parameter(s)\n";
    print $usage;
    exit();
}

if ($help) {
    print 'Report file value averager. Counts and prints out average for
each line over multiple files.
Expected syntax for input files: <time stamp> <value>
Output syntax: <time stamp> <average> [<min> <max>]';
    print "\n$usage";
    print '
options:

error  Add minimum and maximum values to the output (for error bars)

';
    exit();
}

my $fileCount = @ARGV;
my @fileHandles;

# open all input files
# print "# Average over $fileCount files: @ARGV\n";

for (my $i=0; $i < $fileCount; $i++) {
  my $inFile = $ARGV[$i];
  my $fh = new FileHandle;
  $fh->open("<$inFile") or die "Can't open $inFile: $!";
  $fileHandles[$i] = $fh;
}

my $cont = 1;
while($cont) {
  my $sum = 0;
  my ($time, $value);
  my $min = undef;
  my $max = undef;
  my $oldTime = undef;

  # read one line from each file and count average
  for (my $i=0; $i < $fileCount; $i++) {
    my $fh = $fileHandles[$i];
    $_ = <$fh>;
    if (not $_) { # no more input
      $cont = 0;
      last;
    }
    ($time, $value) = m/([\d\.]+) ([\d\.]+)/;
    $sum += $value;

    if (defined $oldTime and $oldTime != $time) {
      die "Time stamps are not in sync ($oldTime vs. $time)";
    }
    $oldTime = $time;

    # update min and max values
    if (not defined $min) { # first value for the round
      $min = $value;
      $max = $value;
    }
    if ($value > $max) {
      $max = $value;
    }
    if ($value < $min) {
      $min = $value;
    }
  }

  if ($cont) {
    my $avg = $sum/$fileCount;
    print "$time $avg";

    if ($error) {
      print " $min $max";
    }
    print "\n";
   }
}
