#! /usr/bin/perl

package Toolkit;

# (C)CDF gnuplot plotter

use strict;
use warnings;
use Getopt::Long;

# path to the gnuplot program
my $gnuplot = "gnuplot";

my (
	$outfile, $constTotalSum, $hitCountIndex,
	$labelRe, $term,          $range,
	$params, $comp, $logscale, $help
);

my $usage = '
usage:  -out <output file name>
        [-comp]
        [-log]
        [-total <total sum>]
        [-index <hitcount index>]
        [-label <plot title extracting RE>]
        [-params <params for plots>]
        [-term <gnuplot output terminal>]
        [-range <x range of plot>]
        fileNames...
';

GetOptions(
	"total=i"  => \$constTotalSum,
	"index=i"  => \$hitCountIndex,
	"label=s"  => \$labelRe,
	"params=s" => \$params,
	"out=s"    => \$outfile,
	"term=s"   => \$term,
	"range=s"  => \$range,
	"comp!"    => \$comp,
	"log!"     => \$logscale,
	"help|?!"  => \$help
);

if ($help) {
	print '(Complementary) Cumulative Distribution plotter. Creates CDF plots
from timestamped hitcount reports using gnuplot. All given input files
are plotted to the same graph.';
	print "\n$usage";
	print '
options:

out    Output file name\'s prefix

total  If defined, the total value against which cumulative values are compared.
       By default, total is calculated from the input file.

index  Index (which of whitespace delimeted fields) of the hitcount value.
       Default = 1 (first field after time stamp). Zero value reports hitcount
       of 1 for each line.

comp   Do Complementary CDF instead of normal CDF

label  A regular expression which is used to parse labels for plots from the
       input file names. Use capture groups to get the content(s).
       Default = \'([^_]*)_\' (everything up to the first underscore)

params Gnuplot plotting style parameters. Default = \'smooth unique\'

term   Name of the terminal used for gnuplot output. Use "na" for no terminal
       (only the gnuplot output file is created). Default = emf

range  Range of x-values in the resulting graph (e.g. 0:100).
       Default = no range, i.e. automatic scaling by gnuplot
';
	exit();
}

if ( not defined $outfile or not @ARGV ) {
	print "Missing required parameter(s)\n";
	print $usage;
	exit();
}

$hitCountIndex = 1               unless defined $hitCountIndex;
$term          = "emf"           unless defined $term;
$params        = "smooth unique" unless defined $params;
$labelRe       = '([^_]*)_'      unless defined $labelRe;

my $plotfile = "$outfile.gnuplot";

open( PLOT, ">$plotfile" ) or die "Can't open plot output file $plotfile : $!";
if ($logscale) {
	print PLOT "set logscale x\n";
}
if ($comp) {
	print PLOT "set ylabel \"1-P(X <= x)\"\n";
}
else {
	print PLOT "set ylabel \"P(X <= x)\"\n";
}
if ( defined($range) ) {
	print PLOT "set xrange [$range]\n";
}

if ( not $term eq "na" ) {
	my ($suffix) = $term=~ /(\S+)/;
	print PLOT "set terminal $term\n";
	print PLOT "set output \"$outfile.$suffix\"\n";
}
print PLOT "plot ";

my $round = 0;
while ( my $infile = shift(@ARGV) ) {
	if ( $round > 0 ) {
		print PLOT ", ";
	}
	$round++;

	open( INFILE, "$infile" ) or die "Can't open $infile : $!";

	# strip 1-3 char extension (if any)
	if ( $infile =~ m/.*\.\w{1,3}/ ) {
		($infile) = $infile =~ m/(.*)\.\w{1,3}/;
	}

	my $cdffile = "$infile.cdf";

	my $totalSum;
	my $hitcount;
	my $time;
	my @values;

	open( OUTFILE, ">$cdffile" ) or die "Can't open output file $cdffile : $!";

	if ( defined $constTotalSum ) {
		$totalSum = $constTotalSum;
	}
	else {
		while (<INFILE>) {
			@values = split;
			next if $values[0] eq "#";    # skip comment lines

			if ( $hitCountIndex > 0 ) {
				$totalSum += $values[$hitCountIndex];
			}
			else {
				$totalSum += 1;
			}
		}
		seek( INFILE, 0, 0 );
	}

	my $cumSum = 0;

	while (<INFILE>) {
		@values = split;
		next if $values[0] eq "#";

		$time = $values[0];
		if ( $hitCountIndex > 0 ) {
			$hitcount = $values[$hitCountIndex];
		}
		else {
			$hitcount = 1;
		}

		if ( $hitcount > 0 ) {
			$cumSum += $hitcount;
			my $finalValue;
			if ($comp) {
				$finalValue = 1 - ( $cumSum / $totalSum );
			}
			else {
				$finalValue = ( $cumSum / $totalSum );
			}
			print OUTFILE "$time ", $finalValue, "\n";
		}
	}

	close(OUTFILE);
	close(INFILE);

	print PLOT "'$cdffile'";
	if ( defined($labelRe) ) {    # extract label for legend
		my @labels = $infile =~ m/$labelRe/;
		die "Cant' extract label using \'$labelRe\' from $infile"
		  unless @labels;
		print PLOT " title \"@labels\"";
	}
	print PLOT " $params";

}

print PLOT "\n";
close(PLOT);

if ( not $term eq "na" ) {
	system("$gnuplot $plotfile") == 0 or die "Can't run gnuplot: $?";
}
