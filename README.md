[![codecov](https://codecov.io/gh/Dumb-Projects-Inc/Chisel-Game/graph/badge.svg?token=YN51U70ZRL)](https://codecov.io/gh/Dumb-Projects-Inc/Chisel-Game)
# Chisel Game engine
Room Simulator

## Getting started
Timing for VGA is expected outside stimulus.
To create this in Vivado to the following:
- Open Vivado project
- Under Project Manager open IP Catalog
- Open Clocking Wizard
- Keep component name as clk_wiz_0
- Under Clocking Options switch to PLL (Phase Locked Loop)
- in Output clocks request 50 Mhz clock on clk_out1
- (If adding control we can add more clocks, this might be fun)
- Press OK

### Elaborating the chisel code
Since the game is multi player, two bitstreams are to be generated, one for each player. this is done through
`sbt runMain gameEngine.gameEngineMain 1` and `sbt runMain gameEngine.gameEngineMain 2` respectively.
 

## Testing
All testing is done with verilator, if verilator is installed, it should be possible to test by simply running ``sbt test`


## Architecture

For information about rendering see [rendering.md](rendering.md)
