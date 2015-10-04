package Toolkit;

# Common methods (parseArgs & debug) for toolkit programs

use strict;
use warnings;

our @EXPORT = qw(parseArgs debug);

my $quietMode = 0;

# Parses INFILE and OUTFILE from command line arguments
# and shows help or toggles quiet mode if requested.
sub parseArgs {
    my $progName = shift;
    my $infile;
    my $outfile;

# show help with -h, -?, /h, /? or -help (etc.)
    if (defined $ARGV[0] and $ARGV[0] =~ m/^[-\/][h\?]/) {
	print "$progName\n";
print 'Usage: [-h] [-q] <infile> <outfile>
  -h : show this help
  -q : quiet mode (nothing is printed to stderr)
Default infile and outfile are stdin and stdout.
Discarded lines and "errors" are printed to stderr.
';
	exit();
}

    if (defined $ARGV[0] and $ARGV[0] =~ m/^-q/) {
	$quietMode = 1;
	shift @ARGV;
    }

    $infile = shift @ARGV;;
    $outfile = shift @ARGV;
    
    if ($infile) {
	open(INFILE, $infile) or die "Can't open $infile : $!";
    }
    else {
	open(INFILE, "<-") or die "Can't read from stdin";
    }
    
    if ($outfile) {
	open OUTFILE, ">$outfile" or die "Can't open $outfile : $!";
    }
    else {
	open(OUTFILE, ">-") or die "Can't open stdout";
    }
}

# Prints errors and debug information to stderr (unless $quietMode is set)
sub debug {
    my $txt = shift;
    print STDERR "$txt\n" unless $quietMode;
}

1;
