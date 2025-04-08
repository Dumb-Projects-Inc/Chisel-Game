# Rendering

Hierachy (First is root render):
- (Game logic)
- [BSP Module](#bsp) -> has the double line buffer
- [Overlays](#overlays)
- [Pixel Selector](#pixel-selector)
- [VGA Controller](#vga-controller)

## BSP
BSP is the root module that renders all 3D, BSP stands for Binary Space partitioning and is the rendering technique used by retro 3D games like doom and wolfenstein.
we chose to use this as a root node as this simplifies rendering of 3D significantly, it also allows us to have transparent sprites if need be.

## Overlays
Overlays keep all 2D elements, probably only UI elements, we render this as an overlay by giving a pixel from bsp and checking if a sprite occupies that pixel.
## Pixel Selector 
The pixel selector just chooses what should be displayed in a single pixel, usually this is just priority for Overlays. This could simplify implementation of transparent overlays.

## VGA Controller
The VGA controller is responsible for ensuring proper signaling between the engine and game logic, and the screen. This entails ensuring correct timing for the chosen resolution, but also signalling internally for the current pixel.
