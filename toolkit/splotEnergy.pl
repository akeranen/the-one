#! /usr/bin/perl

# Energy level 3D plotter

package Toolkit;
use strict;
use warnings;
use FileHandle;
use Getopt::Long;

my $usage = '
usage: -name <output file name> [-every <value>] [-term <gnuplot terminal>]
       [-tcmd <terminal commands>] [-title <plot title>]
       <input file name>
';

my ($help, $name, $every, $term, $tcmd, $title);

GetOptions("name=s" => \$name, "every=i" => \$every, "term=s" => \$term,
"tcmd=s" => \$tcmd, "title=s" => \$title, "help|?!" => \$help);

if (not $help and (not $name or not @ARGV)) {
  print "Missing required parameter(s)\n";
  print $usage;
  exit();
}

if ($help) {
  print 'Energy level 3D plotter';
  print "\n$usage";
      print '
options:
name   Output filename\'s prefix. Two files are created: one with .gnuplot and
       second with the selected terminal as suffix.

every  If value bigger than 1 is given, only values from every Nth timestamp are
       used (so there will be less lines). Default = 1.

term   Name of the terminal used for gnuplot output. Default = emf

tcmd   Extra commands for the gnuplot terminal. Nothing by default.
       For example \'"Times-Roman" 24\' for postscript font selection.

title  Plot title. No title by default.
';
  exit();
}

$term = "emf" unless defined $term;
$tcmd = "" unless defined $tcmd;
$every = 1 unless defined $every;

# Input example:
# [135000]
# p0 1355697.85
# p1 1323992.33
# p2 1328161.40
# p3 1229853.70

my $gpFile = new FileHandle;
$gpFile->open(">$name.gnuplot");

my $dataFile = new FileHandle;
$dataFile->open(">$name.data");

print $gpFile "set title '$title'\n" if $title;
print $gpFile "set term $term $tcmd\n";
print $gpFile "set output \'$name.$term\'\n";
print $gpFile "unset xtics\n";
print $gpFile "unset ytics\n";
print $gpFile "splot \'$name.data\' with lines notitle\n";

my @values;
my @times;
my $round = 0;

while (<>) {
  if (m/^\s*$/ or m/^#/) {
    next; # skip empty and comment lines
  }
  if (m/^\[(\d*)\]/) { # next timestamp
    push @times, $1;

    if (@times == 1) {
      next; # the very first timestamp
    }


    if ($round % $every == 0) {
      print $dataFile " \n\n" unless @times == 2;

      @values = sort { $a <=> $b } @values;
      for (my $i=0; $i < @values; $i++) {
        # print normalized to mAh
        print $dataFile "$round $i " . $values[$i]/3600 . "\n";
      }
    }

    @values = (); # clear values
    $round++;
    next;
  }

  my ($value) = m/^\w+ ([\d\.]+)$/;
  die "No valid value at line $_" unless $value;

  push @values, $value;
}

$dataFile->close();
$gpFile->close();

system("gnuplot $name.gnuplot") == 0 or die "Error running gnuplot: $?";
