[![codecov](https://codecov.io/gh/Dumb-Projects-Inc/Chisel-Game/graph/badge.svg?token=YN51U70ZRL)](https://codecov.io/gh/Dumb-Projects-Inc/Chisel-Game)
# Chisel Game
Room Simulator - A pseudo 3d game written in Chisel, to be run on FPGAs.


## Running
Tested on a Basys-3 board
Timing for VGA is expected outside stimulus.
To create this in Vivado to the following:
- Open Vivado project
- Under Project Manager open IP Catalog
- Open Clocking Wizard
- Keep component name as clk_wiz_0
- Under Clocking Options switch to PLL (Phase Locked Loop)
- in Output clocks request 50 Mhz clock on clk_out1
- Press OK
### UART for multiplayer
PMOD is used for multiplayer, cross connect two cables in A14 A16 ports on the basys3

### Elaborating the chisel code
Since the game is multi player, two bitstreams are to be generated, one for each player. this is done through
`sbt runMain gameEngine.gameEngineMain 1.5 1.5` and `sbt runMain gameEngine.gameEngineMain 7.5 2.5` respectively. this places the two players at different coordinates
 

## Testing
All testing is done with verilator, if verilator is installed, it should be possible to test by simply running `sbt test`

If using Nix, the flake should set up all needed tools for testing