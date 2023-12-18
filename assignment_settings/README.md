# Assignment 1 - Group 4
## Trigger alarm in the GUI
We introduced an option to trigger an emergency via the GUI, for this, we added a button in the top.

<img src="guiScreenshot.png" alt="guiScreenshot" width="600" height="500"/>

When activating this option, all hosts enter emergency mode and they start going to an exit.

## Setting files
We provide 4 example settings to run our use-case and simulations.
* `assignment_settings/globalAlarming_nearestExit.txt`
* `assignment_settings/globalAlarming_randomExit.txt`
* `assignment_settings/rangeAlarming_nearestExit.txt`
* `assignment_settings/rangeAlarming_randomExit.txt`

### Alarming modes
* `globalAlarming` instantly sets all hosts to emergency state once the emergency start time is reached.
* `rangeAlarming` activates emergency state on the hosts based on emergency message routing.

### Emergency exit type
* `nearestExit` Once a host is in emergency state, it will go to the **nearest** exit to escape the FMI building.
* `randomExit` Once a host is in emergency state, it will go to a **random** exit to escape the FMI building.
