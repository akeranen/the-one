#!/usr/bin/perl
use strict;
use warnings;
use IPC::System::Simple qw(system capture);
use File::Spec;
use File::Basename;

# Contains tests for messageDelayAnalyzer.pl.

use Test::More;

# This variable stores the output of the lastest messageDelayAnalyzer.pl call.
my @outputLines;
# Delay granularity used throughout the tests.
my $granularity = 10;
# Current directory, important to get absolute file names.
my $currDir = File::Basename::dirname($0);

# Print to console that testing begins.
print "Start tests for messageDelayAnalyzer.pl.\n";

# Check granularity is translated into correct delay classes and only delay classes up to the longest delay needed are
# printed.
callMessageDelayAnalyzer("immediateMessageDelayReport_fastBroadcast");
Test::More::is(getMinDelayFromLine($outputLines[2]), 0, "Minimum delay of first delay class is correct.");
Test::More::is(getMaxDelayFromLine($outputLines[2]), $granularity, "Maximum delay of first delay class is correct.");
Test::More::is(getMinDelayFromLine($outputLines[3]), $granularity, "Minimum delay of second delay class is correct.");
Test::More::is(getMaxDelayFromLine($outputLines[3]), 2*$granularity, "Maximum delay of second delay class is correct.");
Test::More::is($outputLines[4], "\n", "Only the two necessary delay classes are printed.");

# Check analysis is split by type and priority and those analyses may have different lengths.
callMessageDelayAnalyzer("immediateMessageDelayReport_differentTypes");
# Find expected type starts.
# The expected number of printed delay classes by type and priority.
my $expectedNumDelayClassesBroadcastsPrio1 = 1;
my $expectedNumDelayClassesBroadcastsPrio2 = 2;
my $expectedNumDelayClassesMulticastsPrio10 = 1;
my $expectedNumDelayClassesOneToOnePrio10 = 2;
# Expected start line indices.
my $expectedStartBroadcastsPrio1 = 1;
my $expectedStartBroadcastsPrio2 = $expectedStartBroadcastsPrio1 + $expectedNumDelayClassesBroadcastsPrio1 + 2;
my $expectedStartMulticastsPrio10 = $expectedStartBroadcastsPrio2 + $expectedNumDelayClassesBroadcastsPrio2 + 3;
my $expectedStartOneToOnePrio10 = $expectedStartMulticastsPrio10 + $expectedNumDelayClassesMulticastsPrio10 + 3;
# Check all output lines about types and priorities and end of those analyses.
Test::More::is(
    $outputLines[$expectedStartBroadcastsPrio1 - 1],
    "Delay distribution for delivered messages of type BROADCAST:\n",
    "Heading for broadcasts is printed.");
Test::More::is(
    $outputLines[$expectedStartBroadcastsPrio1],
    "For priority 1:\n", "Heading for broadcast priority 1 is printed");
Test::More::is(
    $outputLines[$expectedStartBroadcastsPrio1 + $expectedNumDelayClassesBroadcastsPrio1 + 1],
    "\n", "Break after first delay class of priority 1.");
Test::More::is(
    $outputLines[$expectedStartBroadcastsPrio2],
    "For priority 2:\n", "Heading for broadcast priority 2 is printed.");
Test::More::is(
    $outputLines[$expectedStartBroadcastsPrio2 + $expectedNumDelayClassesBroadcastsPrio2 + 1],
    "\n", "Break after second delay class of priority 2.");
Test::More::is(
    $outputLines[$expectedStartMulticastsPrio10 - 1],
    "Delay distribution for delivered messages of type MULTICAST:\n",
    "Heading for multicasts is printed.");
Test::More::is(
    $outputLines[$expectedStartMulticastsPrio10],
    "For priority 10:\n", "Heading for multicasts priority 10 is printed.");
Test::More::is(
    $outputLines[$expectedStartMulticastsPrio10 + $expectedNumDelayClassesMulticastsPrio10 + 1],
    "\n", "Break after first delay class of multicasts.");
Test::More::is(
    $outputLines[$expectedStartOneToOnePrio10 - 1],
    "Delay distribution for delivered messages of type ONE_TO_ONE:\n",
    "Heading for one to one messages is printed.");
Test::More::is(
    $outputLines[$expectedStartOneToOnePrio10],
    "For priority 10:\n", "Heading for one to one priority 10 is printed.");
Test::More::is(
    $outputLines[$expectedStartOneToOnePrio10 + $expectedNumDelayClassesOneToOnePrio10 + 1],
    "\n", "Break after first delay class of one to one messages.");

# Check analysis correctly works in terms of delay distribution percentages and totals.
# TODO.


done_testing();

# Calls messageDelayAnalyzer.pl with the given file name.
# Expects the file to reside in toolkit/testdata.
sub callMessageDelayAnalyzer {
    # Get absolute file path.
    my $inputFile = shift;
    $inputFile = File::Spec->rel2abs("$currDir/../testdata/$inputFile");
    # Use absolute file path of analyzer script, too.
    my $rawDataName = File::Spec->rel2abs("$currDir/../messageDelayAnalyzer.pl");
    # Capture console output.
    @outputLines = IPC::System::Simple::capture("perl $rawDataName $inputFile $granularity");
}

# Gets the minimum delay from a line of format "Delay <minDelay> <= x < <maxDelay>: <percentage> (Total: <number>)" by
# returning the first integer.
sub getMinDelayFromLine {
    shift =~ /^\D*(\d+)/;
    return $1;
}

# Gets the maximum delay from a line of format "Delay <minDelay> <= x < <maxDelay>: <percentage> (Total: <number>)" by
# returning the second integer.
sub getMaxDelayFromLine {
    shift =~ /^\D*\d+\D*(\d+)/;
    return $1;
}