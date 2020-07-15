#! /usr/local/bin/perl

package Toolkit;

# dtnsim2 output parser v0.1

# Apply patches to dtnsim2 and run it with verbose mode 8.
# Then apply this script to dtnsim2's output and use the result
# as the external events file for simulation.

# This version should convert all events properly at least for
# epidemic simulations.

use strict;
use warnings;
use Common; # parseArgs and debug methods

parseArgs("dtnsim2parser");

# actions that are mapped
my %commandMapping = (
		      'Sending' => 'C',# message created
		      'C_MSG_SENDING' => 'S', # started transfer
		      'ACCEPTED' => 'DE', # Accepted -> delivered
		      'C_DOWN_SEND_ABORT' => 'A', # aborted transfer
		      'REFUSED' => 'A', # Refusing ->  aborting
		      'DROPPED epidemic' => 'DR',
		      'Removing copy of' => 'R'
		      );

# actions that are ignored at the moment
my %ignoreCommands = ('C_MSG_SENT' => 1,
		      'C_MSG_DELIVERED' => 1,
		      'DELIVERED_FIRST' => 1,
		      'DELIVERED_NEXT' => 1
		      );

while(<INFILE>) {
    #                             timestamp    action      msgId
    my ($time, $action, $id) = m/^(\d+\.\d+): ([\w\s]+):? (MSG_\d+_D_\d+)_\(\d+\)/;

    unless ($time and $action and $id) {
	debug ("Discarded: $_");
	next;
    }

    my $actionCode = $commandMapping{$action};

    die "Unknown action '" , $action,"'\n" unless $actionCode or $ignoreCommands{$action};
    next unless $actionCode;

    my $lastPart;

    if ($actionCode eq 'C') {
	m/NODE_'(\w+)' => NODE_'(\w+)'\); Size: (\d+); result: (\w+)/;
	die "Couldn't parse CREATE line $_ \n" unless $1 and $2 and $3;
	my ($h1, $h2, $size, $result) = ($1, $2, $3, $4);

	if ($result ne "true") {
	    debug ("Failed create: $_");
	    next;
	}

	$lastPart =  "$h1\t$h2\t$size";
    }
    elsif ($actionCode eq 'DR' or $actionCode eq 'R') {
	my ($host) = m/at (\w+)$/;
	die "Couldn't parse host from $_\n" unless $host;
	$lastPart = $host;
    }
    else {
	my ($h1, $h2) = m/CONTACT_'(\w+)->(\w+)'/;
	die "Couldn't parse contact from $_\n" unless $h1 and $h2;
	$lastPart = "$h1\t$h2";
    }

    print OUTFILE "$time\t$actionCode\t$id\t$lastPart\n";

}
