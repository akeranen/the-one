package tests;

use strict;
use warnings FATAL => 'all';
use IPC::System::Simple qw(system capture);
use File::Spec;
use File::Basename;

# Contains tests for multicastMessageAnalyzer.pl.

# We do not specify how many tests will follow, but we will use tsets.
use Test::More 'no_plan';

# This variable stores the output of the lastest multicastMessageAnalyzer.pl call.
my @outputLines;
# Time steps (granularity) used throughout the tests.
my $timeStep = 10;
# Current directory, important to get absolute file names.
my $currDir = File::Basename::dirname($0);

# Print to console that testing begins.
print "Start tests for multicastMessageAnalyzer.pl.\n";

# Check granularity is translated into correct time points and time points are printed
# until creation + time point > end of simulation.
callMulticastMessageAnalyzer("multicastDeliveryReport_singleCreation");
Test::More::is(getTimePointFromLine($outputLines[1]), $timeStep, "First time point is correct.");
Test::More::is(getTimePointFromLine($outputLines[2]), 2 * $timeStep, "Second time point is correct.");
Test::More::is(getTimePointFromLine($outputLines[3]), 3 * $timeStep, "Third time point is correct.");
Test::More::is(getTimePointFromLine($outputLines[4]), 4 * $timeStep, "Fourth time point is correct.");
Test::More::is($outputLines[5], undef, "Nothing afterwards.");

# Check analysis correctly works in terms of averaging and minimizing, esp. if some messages have a longer lifetime
# than others.
callMulticastMessageAnalyzer("multicastDeliveryReport_complexScenario");
Test::More::is(getAvgFromLine($outputLines[1]), 0.166666666666667, "First average of three messages is correct.");
Test::More::is(getMinFromLine($outputLines[1]), "0.0", "First minimum of three messages is correct.");
Test::More::is(getAvgFromLine($outputLines[2]), 0.333333333333333, "Second average of three messages is correct.");
Test::More::is(getMinFromLine($outputLines[2]), "0.0", "Second minimum of three messages is correct.");
Test::More::is(getAvgFromLine($outputLines[3]), 0.583333333333333, "Average of two remaining messages is correct.");
Test::More::is(getMinFromLine($outputLines[3]), 0.25, "Minimum of two remaining messages is correct.");
Test::More::is(getAvgFromLine($outputLines[4]), 0.666666666666667, "Average of single remaining message is correct.");
Test::More::is(getMinFromLine($outputLines[4]), 0.5, "Minimum of single remaining message is correct.");
Test::More::is(getAvgFromLine($outputLines[5]), 0.833333333333333, "Average of single remaining message is correct.");
Test::More::is(getMinFromLine($outputLines[5]), 0.5, "Minimum of single remaining message is correct.");
Test::More::is(getAvgFromLine($outputLines[6]), 1, "Average of single remaining message is correct.");
Test::More::is(getMinFromLine($outputLines[6]), "1.0", "Minimum of single remaining message is correct.");

# Calls multicastMessageAnalyzer.pl with the given file name.
# Expects the file to reside in toolkit/testdata.
sub callMulticastMessageAnalyzer {
    # Get absolute file path.
    my $inputFile = shift;
    $inputFile = File::Spec->rel2abs("$currDir/../testdata/$inputFile");
    # Use absolute file path of analyzer script, too.
    my $rawDataName = File::Spec->rel2abs("$currDir/../multicastMessageAnalyzer.pl");
    # Capture console output.
    @outputLines = IPC::System::Simple::capture("perl $rawDataName $inputFile $timeStep");
}

# Gets the time point from a line of format <timePoint> <min> <avg>.
sub getTimePointFromLine {
    return getWordFromLine(shift, 0);
}

# Gets average from a line of format <timePoint> <min> <avg>.
sub getAvgFromLine {
    return getWordFromLine(shift, 2);
}

# Gets minimum from a line of format <timePoint> <min> <avg>.
sub getMinFromLine {
    return getWordFromLine(shift, 1);
}

# Needs two parameters $line and $index and returns the word at index $index from line $line.
sub getWordFromLine {
    my $line = shift;
    my $index = shift;

    (my @words)=split(/\s+/, $line);
    return $words[$index];
}