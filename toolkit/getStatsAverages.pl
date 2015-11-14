#! /usr/bin/perl

# Counts averages from multiple MessageStats reports

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
    print 'MessageStats report file value averager. Prints out average for
numeric key-value pairs.
Expected syntax for input files: <key>: <value>
Output syntax: <key> <average> [<min> <max>]';
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
print "# Average over $fileCount files: @ARGV\n";

for (my $i=0; $i < $fileCount; $i++) {
  my $inFile = $ARGV[$i];
  my $fh = new FileHandle;
  $fh->open("<$inFile") or die "Can't open $inFile: $!";
  $fileHandles[$i] = $fh;
}

my $cont = 1;
while($cont) {
  my $sum = 0;
  my ($key, $value);
  my $min = undef;
  my $max = undef;
  my $oldKey = undef;

  # read one line from each file and count average
  for (my $i=0; $i < $fileCount; $i++) {
    my $fh = $fileHandles[$i];
    $_ = <$fh>;
    if (not $_) { # no more input
      $cont = 0;
      last;
    }
    ($key, $value) = m/(\S+): ([\d\.]+)/;
	if (not defined $key or not defined $value) {
		next; # not numeric value
	}
    $sum += $value;

	if (defined $oldKey and $oldKey ne $key) {
		die "key mismatch: $key vs. $oldKey";
	}
	$oldKey = $key;

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

  if ($cont and defined $key) {
    my $avg = $sum/$fileCount;
    print "$key $avg";

    if ($error) {
      print " $min $max";
    }
    print "\n";
   }
}
