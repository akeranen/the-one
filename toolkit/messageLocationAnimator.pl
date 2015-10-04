#! /sw/bin/perl

# Message location animator

package Toolkit;
use strict;
use warnings;
use FileHandle;
use Getopt::Long;

my $usage = '
usage: -name <name of the animation> 
       [-xrange <xmin:xmax>]
       [-yrange <ymin:ymax>] 
       [-size <xsize,ysize>]
       [-delay <delay per animation frame>]
       [-inc <reg exp for included message IDs>]
       [-ptype <gnuplot point type value>]
       [-help]
       <input file name>
';

my ($help, $name, $xrange, $yrange, $size, $delay, $includeRe, $ptype);

GetOptions("xrange=s" => \$xrange, "yrange=s" => \$yrange, "size=s"=>\$size,
  "delay=i" => \$delay, "inc=s"=>\$includeRe, "ptype=i"=>\$ptype, 
  "name=s" => \$name, "help|?!" => \$help);

if (not $help and (not defined $name or not @ARGV)) {
  print "Missing required parameter(s)\n";
  print $usage;
  exit();
}

$delay = 100 unless defined $delay;
$size = "800,800" unless defined $size;
$includeRe = ".*" unless defined $includeRe;

if ($help) {
  print 'Message Location Animator. 
Creates an animated GIF of message locations from MessageLocationReport 
output using gnuplot. Gnuplot needs to be linked with gd 2.0.29 or newer.';
  print "\n$usage";
  print '
options: 
name    Name of the animation file and folder
xrange  Value range for the x-axis (default: variable). E.g. "0:4500"
yrange  Value range for the y-axis (default: variable). E.g. "0:3500"
size    Size (pixels) of the resulting image (default: 800,800)
delay   Delay per animation frame (default: 100)
inc     Regular expression for matching the messages that should be included
        in the animation (default: \'.*\'). E.g. use \'M(1|2)$\' for animating 
        only messages M1 and M2
ptype   Type of the point (integer value) to use in the location of the message
        (default: variable). Valid values are from 0 to 21. Type "test" in 
        gnuplot to get samples of all types.

example: 
  messageLocationAnimator.pl -name test reportfile.txt
';
  exit();
}

my $animDir = "${name}_anim";
mkdir($animDir);

# Input example:
# [100]
# (1637.34,797.68) M1
# (2829.22,402.05) M2 M1

my ($time, $lastTime, %fileHandles, @msgIds, $nextId);

my $gpFile = new FileHandle;
$gpFile->open(">$animDir/$name.gnuplot");

print $gpFile "set term gif animate transparent opt delay $delay size $size x000000 \n";
print $gpFile "set output \'$name.gif\'\n";
print $gpFile "set xrange [$xrange]\n" if defined $xrange;
print $gpFile "set yrange [$yrange]\n" if defined $yrange;

while (<>) {
  if (m/^\s*$/ or m/^#/) {
    next; # skip empty and comment lines
  }
  if (m/^\[(\d*)\]/) { # next timestamp
    $lastTime = $time;
    $time = $1;
    for (values %fileHandles) {
      $_->close(); # close all existing handles
    }
    
    # add plotting command for all file names from the previous round
    if (@msgIds) {
      writePlotCommand(@msgIds);
    }    
    
    while ( my ($key, $value) = each(%fileHandles) ) {
      my $fileName = "${time}-${key}.data";
      # open new file for all known messages
      $value->open(">${animDir}/$fileName");
    }
    next;
  }
  
  my ($xcoord, $ycoord, $messageLine) = m/^\((\d*\.\d*),(\d*\.\d*)\) (.*)$/;
  die "No valid coordinates at line $_" unless ($xcoord and $ycoord);
  
  my @messages = split(/ /, $messageLine);
 
  foreach (@messages) {
    my $fh;
    if (exists $fileHandles{$_}) { 
      $fh = $fileHandles{$_};
    }
    else { # no file handle for this message ID -> create one
      if (not m/$includeRe/) {
        next;
      }
      print "New message ID: $_\n";
      my $fileName = "${time}-${_}.data";
      $fh = new FileHandle;
      $fileHandles{$_} = $fh;
      $fh->open(">${animDir}/$fileName");
      push(@msgIds, $_);
    }
    
    print $fh "$xcoord $ycoord\n";
  }
}

# add plotting command for all file names from the last round
writePlotCommand(@msgIds);
$gpFile->close();

print "Done. All files are at folder \"$animDir\"\n";
print "To create animation, run \"gnuplot $name.gnuplot\" at the directory.\n";

# Writes a plotting command for the given message IDs
sub writePlotCommand {
  my @plotData;
  my $plotCmd;
  foreach (@_) {
    $plotCmd = "\'${lastTime}-${_}.data\' title \'$_\'";
    if (defined $ptype) {
      $plotCmd = $plotCmd ." pt $ptype";
    }
    push(@plotData, $plotCmd);
  }
  print $gpFile "set title \'time: $lastTime\'\n";
  print $gpFile "plot " . join(', ', @plotData) . "\n";
}
