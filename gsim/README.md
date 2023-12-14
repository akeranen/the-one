# GSIM

GSIM stands for GPU-based mobility Simulator.  
It can be used to accelerate The ONE with the help of a GPU.

This directory contains a pre-built binary for convenience.  
Alternatively, the source code can be found here: https://github.com/crydsch/gsim

The directory 'settings' aims to illustrate how GSIM can be enabled.  
The file 'settings/example.txt' contains explanations to all possible configurations.

More information on supported configuration parameters can be found here:  
https://github.com/Crydsch/gsim/blob/8b0610399b7f346ecb8b8611a59334ddb00403ae/src/sim/Config.cpp

In short, add the following to your scenarios settings file tp enable GPU acceleration:
```
MovementEngine.type = GSIMMovementEngine
```
