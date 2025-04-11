[![Coverage Status](https://coveralls.io/repos/github/Dumb-Projects-Inc/Chisel-Game/badge.svg?branch=master)](https://coveralls.io/github/Dumb-Projects-Inc/Chisel-Game?branch=master)
# Chisel Game engine
for Doomlike games


## Getting started
Timing for VGA is expected outside stimulus.
To create this in Vivado to the following:
- Open Vivado project
- Under Project Manager open IP Catalog
- Open Clocking Wizard
- Keep component name as clk_wiz_0
- Under Clocking Options switch to PLL (Phase Locked Loop)
- in Output clocks request 25.175 Mhz (25.17007 on 100 Mhz) clock on clk_out1
- (If adding control we can add more clocks, this might be fun)
- Press OK


## Architecture

For information about rendering see [rendering.md](rendering.md)
