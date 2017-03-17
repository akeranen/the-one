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
# Delay step size used throughout the tests.
my $delayStepSize = 10;
# Current directory, important to get absolute file names.
my $currDir = File::Basename::dirname($0);

# Print to console that testing begins.
print "Start tests for messageDelayAnalyzer.pl.\n";

# Run all tests.
testDelayClasses();
testAnalysisIsSplitByTypeAndPrio();
testDelayDistributionPercentagesAndTotals();

# Finish testing.
done_testing();

# Check delayStepSize is translated into correct delay classes and only delay classes up to the longest delay needed are
# printed.
sub testDelayClasses {
    print "\nCheck correct delay distribution classes are built.\n";

    # Call message delay analyzer for report with a single fast broadcast.
    callMessageDelayAnalyzer("immediateMessageDelayReport_fastBroadcast");

    Test::More::is(getMinDelayFromLine($outputLines[2]), 0, "Minimum delay of first delay class is correct.");
    Test::More::is(getMaxDelayFromLine($outputLines[2]), $delayStepSize, "Maximum delay of first delay class is correct.");
    Test::More::is(getMinDelayFromLine($outputLines[3]), $delayStepSize, "Minimum delay of second delay class is correct.");
    Test::More::is(getMaxDelayFromLine($outputLines[3]), 2*$delayStepSize, "Maximum delay of second delay class is correct.");
    Test::More::is($outputLines[4], "\n", "Only the two necessary delay classes are printed.");
}

# Check analysis is split by type and priority and those analyses may have different lengths.
sub testAnalysisIsSplitByTypeAndPrio {
    print "\nCheck analysis is split by type and priority.\n";

    # Call message delay analyzer for report with different types and priorities.
    callMessageDelayAnalyzer("immediateMessageDelayReport_differentTypes");

    # Find expected line numbers of headings.
    # The expected number of printed delay classes by type and priority.
    my $expectedNumDelayClassesBroadcastsPrio1 = 1;
    my $expectedNumDelayClassesBroadcastsPrio2 = 2;
    my $expectedNumDelayClassesMulticastsPrio10 = 1;
    my $expectedNumDelayClassesOneToOnePrio10 = 2;
    # Expected line numbers for headings.
    my $expectedStartBroadcastsPrio1 = 1;
    my $expectedStartBroadcastsPrio2 = $expectedStartBroadcastsPrio1 + $expectedNumDelayClassesBroadcastsPrio1 + 2;
    my $expectedStartMulticastsPrio10 = $expectedStartBroadcastsPrio2 + $expectedNumDelayClassesBroadcastsPrio2 + 3;
    my $expectedStartOneToOnePrio10 = $expectedStartMulticastsPrio10 + $expectedNumDelayClassesMulticastsPrio10 + 3;

    # Define formats for expected headers.
    my $typeHeaderFormat = "Delay distribution for delivered messages of type %s:\n";
    my $prioHeaderFormat = "For priority %d:\n";

    # Check all output lines about types and priorities and end of those analyses.
    Test::More::is(
        $outputLines[$expectedStartBroadcastsPrio1 - 1],
        sprintf($typeHeaderFormat, "BROADCAST"), "Heading for broadcasts is printed.");
    Test::More::is(
        $outputLines[$expectedStartBroadcastsPrio1],
        sprintf($prioHeaderFormat, 1), "Heading for broadcast priority 1 is printed");
    Test::More::is(
        $outputLines[$expectedStartBroadcastsPrio1 + $expectedNumDelayClassesBroadcastsPrio1 + 1],
        "\n", "Break after first delay class of priority 1.");
    Test::More::is(
        $outputLines[$expectedStartBroadcastsPrio2],
        sprintf($prioHeaderFormat, 2), "Heading for broadcast priority 2 is printed.");
    Test::More::is(
        $outputLines[$expectedStartBroadcastsPrio2 + $expectedNumDelayClassesBroadcastsPrio2 + 1],
        "\n", "Break after second delay class of priority 2.");
    Test::More::is(
        $outputLines[$expectedStartMulticastsPrio10 - 1],
        sprintf($typeHeaderFormat, "MULTICAST"), "Heading for multicasts is printed.");
    Test::More::is(
        $outputLines[$expectedStartMulticastsPrio10],
        sprintf($prioHeaderFormat, 10), "Heading for multicasts priority 10 is printed.");
    Test::More::is(
        $outputLines[$expectedStartMulticastsPrio10 + $expectedNumDelayClassesMulticastsPrio10 + 1],
        "\n", "Break after first delay class of multicasts.");
    Test::More::is(
        $outputLines[$expectedStartOneToOnePrio10 - 1],
        sprintf($typeHeaderFormat, "ONE_TO_ONE"), "Heading for one to one messages is printed.");
    Test::More::is(
        $outputLines[$expectedStartOneToOnePrio10],
        sprintf($prioHeaderFormat, 10), "Heading for one to one priority 10 is printed.");
    Test::More::is(
        $outputLines[$expectedStartOneToOnePrio10 + $expectedNumDelayClassesOneToOnePrio10 + 1],
        "\n", "Break after first delay class of one to one messages.");
}

# Check analysis correctly works in terms of delay distribution percentages and totals.
sub testDelayDistributionPercentagesAndTotals {
    print "\nCheck delay distribution percentages and totals.\n";

    # Call message delay analyzer for report with very different message delays.
    callMessageDelayAnalyzer("immediateMessageDelayReport_veryDifferentMessages");

    # Test names are very similar, so store the formats in variables.
    my $correctPercentageFormatter = "Percentage of %s with delays between %d and %d is correct.";
    my $correctTotalFormatter = "Total number of %s with delays between %d and %d is correct.";

    # Check broadcasts, prio 1.
    Test::More::is(
        getTotalFromLine($outputLines[2]), "1", sprintf($correctTotalFormatter, "broadcasts prio 1", 0, 10));
    Test::More::is(
        getPercentageFromLine($outputLines[2]), "50.00", sprintf($correctPercentageFormatter, "broadcasts prio 1", 0, 10));
    Test::More::is(
        getTotalFromLine($outputLines[3]), "1", sprintf($correctTotalFormatter, "broadcasts prio 1", 10, 20));
    Test::More::is(
        getPercentageFromLine($outputLines[3]), "50.00", sprintf($correctPercentageFormatter, "broadcasts prio 1", 10, 20));

    # Check broadcasts, prio 2.
    Test::More::is(
        getTotalFromLine($outputLines[6]), "0", sprintf($correctTotalFormatter, "broadcasts prio 2", 0, 10));
    Test::More::is(
        getPercentageFromLine($outputLines[6]), "0.00", sprintf($correctPercentageFormatter, "broadcasts prio 2", 0, 10));
    Test::More::is(
        getTotalFromLine($outputLines[7]), "1", sprintf($correctTotalFormatter, "broadcasts prio 2", 10, 20));
    Test::More::is(
        getPercentageFromLine($outputLines[7]), "100.00", sprintf($correctPercentageFormatter, "broadcasts prio 2", 10, 20));

    # Check multicasts.
    Test::More::is(
        getTotalFromLine($outputLines[11]), "1", sprintf($correctTotalFormatter, "multicasts", 0, 10));
    Test::More::is(
        getPercentageFromLine($outputLines[11]), "100.00", sprintf($correctPercentageFormatter, "multicasts", 0, 10));

    # Check OneToOne.
    Test::More::is(
        getTotalFromLine($outputLines[15]), "1", sprintf($correctTotalFormatter, "1-to-1 messages", 0, 10));
    Test::More::is(
        getPercentageFromLine($outputLines[15]), "33.33", sprintf($correctPercentageFormatter, "1-to-1 messages", 0, 10));
    Test::More::is(
        getTotalFromLine($outputLines[16]), "1", sprintf($correctTotalFormatter, "1-to-1 messages", 10, 20));
    Test::More::is(
        getPercentageFromLine($outputLines[16]), "33.33", sprintf($correctPercentageFormatter, "1-to-1 messages", 10, 20));
    Test::More::is(
        getTotalFromLine($outputLines[17]), "1", sprintf($correctTotalFormatter, "1-to-1 messages", 20, 30));
    Test::More::is(
        getPercentageFromLine($outputLines[17]), "33.33", sprintf($correctPercentageFormatter, "1-to-1 messages", 20, 30));
}

# Calls messageDelayAnalyzer.pl with the given file name.
# Expects the file to reside in toolkit/testdata.
sub callMessageDelayAnalyzer {
    # Get absolute file path.
    my $inputFile = shift;
    $inputFile = File::Spec->rel2abs("$currDir/../testdata/$inputFile");
    # Use absolute file path of analyzer script, too.
    my $rawDataName = File::Spec->rel2abs("$currDir/../messageDelayAnalyzer.pl");
    # Capture console output.
    @outputLines = IPC::System::Simple::capture("perl $rawDataName $inputFile $delayStepSize");
}

# Gets the minimum delay from a line of format "Delay <minDelay> <= x < <maxDelay>: <percentage>% (Total: <number>)" by
# returning the integer before '<='.
sub getMinDelayFromLine {
    shift =~ /(\d+)\s*<=/;
    return $1;
}

# Gets the maximum delay from a line of format "Delay <minDelay> <= x < <maxDelay>: <percentage>% (Total: <number>)" by
# returning the integer after '<'.
sub getMaxDelayFromLine {
    shift =~ /<\s*(\d+)/;
    return $1;
}

# Gets the percentage from a line of format "Delay <minDelay> <= x < <maxDelay>: <percentage>% (Total: <number>)" by
# returning the first percentage.
sub getPercentageFromLine {
    shift =~ /(\d+.\d+)%/;
    return $1;
}

# Gets the total from a line of format "Delay <minDelay> <= x < <maxDelay>: <percentage>% (Total: <number>)" by
# returning the integer after 'Total:'.
sub getTotalFromLine {
    shift =~ /Total:\s*(\d+)/;
    return $1;
}