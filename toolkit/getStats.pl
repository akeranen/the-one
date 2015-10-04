#! /usr/bin/perl

package Toolkit;
use strict;
use warnings;
use Getopt::Long;

my $statName;
my $labelRe;
my $outfile;
my $term;
my $range;
my $help;
# right margin for gnuplot
my $rmargin = 10;

my $usage = '
usage: -stat <statName> [-label <labelExtractingRe>]
       [-out <outputFileName>] [-term <gnuplotOutputTerminal>]
       [-range <rangeOfPlotValues>] [-help]
       statFileNames...
';

GetOptions("stat=s" => \$statName, "label=s" => \$labelRe, 
	   "out=s" => \$outfile, "term=s" => \$term, 
	   "range=s" => \$range, "help|?!" => \$help);

if (not $help and (not defined $statName or not @ARGV)) {
    print "Missing required parameter(s)\n";
    print $usage;
    exit();
}

if ($help) {
    print 'Statistics getter. Extracts stats from MessageStatsReports
and generates an output image using gnuplot';
    print "\n$usage";
    print '
options: 
stat   The name of the statistic value to parse from all files

label  A regular expression which is used to parse labels for bars from the 
       file names. Use capture groups to get the content(s). 
       Default = \'([^_]*)_\' (everything up to first underscore)

out    Output filename\'s prefix. Default = parsed statistic\'s name
       
term   Name of the terminal used for gnuplot output. Default = emf

range  Range of y-values in the resulting graph (e.g. 0:100). 
       Default = no range, i.e. automatic scaling by gnuplot

example: 
  getStats.pl -stat delivery_prob -label \'RC-([\w-]+)_\' -out stats \
              -term emf -range 0:1 reports/RC*MessageStats*
';
    exit();
}



if (not defined $term) {
    $term = "emf";
}

if (not defined $outfile) {
    $outfile = $statName;
}

if (not defined $labelRe) {
    $labelRe = '([^_]*)_';
}

my $valuesfile = "$outfile.values";
my $plotfile = "$outfile.gnuplot";

open (OUTFILE, ">$valuesfile") or die "Can't open $valuesfile for output: $!";
open (PLOT, ">$plotfile") or die "Can't open $plotfile for output: $!";

while (my $statFile = shift(@ARGV)) {
    open (SFILE, $statFile) or die "Can't open $statFile: $!";
    my $value;

    my @labels = $statFile =~ m/$labelRe/;
    die "Cant' extract labels using \'$labelRe\' from $statFile" unless @labels;

    while(<SFILE>) {
	if (m/$statName/) {
	    ($value) = m/$statName: (.+)/; 
	}
    }

    die "Couldn't extract value for $statName from $statFile" unless defined $value;

    close(SFILE);
    print OUTFILE "\"@labels\" $value\n";
    $value = undef;
}

close(OUTFILE);

print PLOT "set xtics rotate by -45\n";
print PLOT "set style data histogram\n";
print PLOT "set style fill solid border -1\n";

if (defined $rmargin) {
    print PLOT "set rmargin $rmargin\n";
}
if (defined $range) {
    print PLOT "set yrange [$range]\n";
}

print PLOT "set terminal $term\n";
print PLOT "plot '$valuesfile' using 2:xtic(1) title \"$statName\"\n";
close(PLOT);

system("gnuplot $plotfile > $outfile.$term") == 0 or die "Error running gnuplot: $?";
