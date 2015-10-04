#! /usr/bin/perl

# Extracts statistics from MessageAvailabilityReport output

package Toolkit;
use strict;
use warnings;
use Getopt::Long;
use FileHandle;

my $help;

my $usage = '
usage: [-help] <input file name(s)>
';

GetOptions("help|?!" => \$help);

if (not $help and not @ARGV) {
    print "Missing required parameter(s)\n";
    print $usage;
    exit();
}

if ($help) {
    print 'MessageAvailabilityReport analyzer. Counts how many instances of each message ID are found in the given report file(s).
';
    print "\n$usage";
    print '
options:
   TBD
';
    exit();
}

my %mCounts;

my $fileCount = @ARGV;

while (my $inFile = shift(@ARGV)) {
    open (INFILE, $inFile) or die "Can't open $inFile: $!";

    while(<INFILE>) {
		my ($idsStr) = m/\w+\d+ (.*)/;
		next unless $idsStr;
		
		foreach my $id (split(' ', $idsStr)) {
			$id = substr($id, 1); # remove message prefix
			if (not defined $mCounts{$id}) {
				$mCounts{$id} = 0;
			}
			$mCounts{$id}++;
		}
	}
}

my $sum = 0;
my $idCount = 0;
foreach my $key (sort {$a<=>$b} keys %mCounts) {
	print $key . ": " . $mCounts{$key} . "\n";
	$sum += $mCounts{$key};
	$idCount++;
}

print "Message ID count: $idCount\n";
print "Average count per ID: " . $sum / int(keys %mCounts) . "\n";

