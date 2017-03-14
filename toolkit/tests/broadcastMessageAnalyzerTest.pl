package tests;

use strict;
use warnings FATAL => 'all';
use IPC::System::Simple qw(system capture);
use File::Spec;

# Contains tests for broadcastMessageAnalyzer.pl.

# We do not specify how many tests will follow, but we will use tsets.
use Test::More 'no_plan';

# This variable stores the output of the lastest broadcastMessageAnalyzer.pl call.
my @outputLines;
# Time granularity used throughout the tests.
my $granularity = 10;

# Print to console that testing begins.
print "Start tests for broadcastMessageAnalyzer.pl.\n";

# Check granularity is translated into correct time points and time points are printed
# until creation + time point > end of simulation.
callBroadcastMessageAnalyzer("broadcastDeliveryReport_singleCreation");
Test::More::is(getTimePointFromLine($outputLines[2]), $granularity, "First time point is correct.");
Test::More::is(getTimePointFromLine($outputLines[3]), 2 * $granularity, "Second time point is correct.");
Test::More::is(getTimePointFromLine($outputLines[4]), 3 * $granularity, "Third time point is correct.");
Test::More::is(getTimePointFromLine($outputLines[5]), 4 * $granularity, "Fourth time point is correct.");
Test::More::is($outputLines[6], "\n", "Break after fourth time point.");
Test::More::is($outputLines[7], undef, "Nothing afterwards.");

# Check analysis is split by priority and analyses for priorities may have different lengths.
callBroadcastMessageAnalyzer("broadcastDeliveryReport_twoPriorities");
# The expected number of printed time points for prio 1.
my $expectedNumTimePointsPrio1 = 4;
# The expected number of printed time points for prio 2.
my $expectedNumTimePointsPrio2 = 1;
# Needed lines for prio 1: Heading + Table heading + all time points + break. Afterwards, prio 2 start.
my $startPrio2 = 3 + $expectedNumTimePointsPrio1;
# Check all output lines about priorities and end of those analyses.
Test::More::is($outputLines[0], "Reached people by broadcasts of prio 1 by time after creation:\n",
    "Heading for prio 1 is printed");
Test::More::is($outputLines[2 + $expectedNumTimePointsPrio1], "\n",
    "Break after fourth time point (last point of prio 1).");
Test::More::is($outputLines[$startPrio2], "Reached people by broadcasts of prio 2 by time after creation:\n",
    "Heading for prio 2 is printed");
Test::More::is($outputLines[$startPrio2 + 2 + $expectedNumTimePointsPrio2], "\n",
    "Break after first time point (last point of prio 2).");

# Check analysis correctly works in terms of averaging and minimizing, esp. if some messages have a longer lifetime
# than others.
callBroadcastMessageAnalyzer("broadcastDeliveryReport_veryDifferentMessages");
Test::More::is(getAvgFromLine($outputLines[2]), 1.67, "First average of three messages is correct.");
Test::More::is(getMinFromLine($outputLines[2]), 1, "First minimum of three messages is correct.");
Test::More::is(getAvgFromLine($outputLines[3]), 2.33, "Second average of three messages is correct.");
Test::More::is(getMinFromLine($outputLines[3]), 1, "Second minimum of three messages is correct.");
Test::More::is(getAvgFromLine($outputLines[4]), "3.00", "Average of two remaining messages is correct.");
Test::More::is(getMinFromLine($outputLines[4]), 1, "Minimum of two remaining messages is correct.");
Test::More::is(getAvgFromLine($outputLines[5]), "6.00", "Average of single remaining message is correct.");
Test::More::is(getMinFromLine($outputLines[5]), 6, "Minimum of single remaining message is correct.");

# Calls broadcastMessageAnalyzer.pl with the given file name.
# Expects the file to reside in toolkit/testdata.
sub callBroadcastMessageAnalyzer {
    # Get absolute file path.
    my $inputFile = shift;
    $inputFile = File::Spec->rel2abs("../toolkit/testdata/$inputFile");
    # Use absolute file path of analyzer script, too.
    my $rawDataName = File::Spec->rel2abs("../toolkit/broadcastMessageAnalyzer.pl");
    # Capture console output.
    @outputLines = IPC::System::Simple::capture("perl $rawDataName $inputFile $granularity");
}

# Gets the time point from a line of format <timePoint> <avg> <min>.
sub getTimePointFromLine {
    return getWordFromLine(shift, 0);
}

# Gets average from a line of format <timePoint> <avg> <min>.
sub getAvgFromLine {
    return getWordFromLine(shift, 1);
}

# Gets minimum from a line of format <timePoint> <avg> <min>.
sub getMinFromLine {
    return getWordFromLine(shift, 2);
}

# Needs two parameters $line and $index and returns the word at index $index from line $line.
sub getWordFromLine {
    my $line = shift;
    my $index = shift;

    (my @words)=split(/\s+/, $line);
    return $words[$index];
}