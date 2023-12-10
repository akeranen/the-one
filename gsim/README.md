# GSIM

GSIM stands for GPU-based mobility Simulator.  
It can be used to accelerate The ONE with the help of a GPU.

This directory contains a pre-built binary for convenience.  
Alternatively, the source code can be found here: https://github.com/crydsch/gsim

The directory 'settings' aims to illustrate how GSIM can be enabled.  
The file 'settings/example.txt' contains explanations to all possible configurations.

In short, add the following to your scenarios settings file:

```
MovementEngine.type = GSIMMovementEngine
GSIMMovementEngine.additionalArgs = --waypoint-buffer-threshold=2
```
