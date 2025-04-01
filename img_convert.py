from PIL import Image
import sys

# import image from argv
img = Image.open(sys.argv[1])
img = img.convert("RGB")
# Get list of pixels
pixels = list(img.getdata())
#recalculate pixels to 4 bit with percentages
# Divide by 16 to get 4 bit color
pixels = [(int(r / 16), int(g / 16), int(b / 16)) for r, g, b in pixels]
# convert to 12 bit color

pixels = [(r << 8) | (g << 4) | b for r, g, b in pixels]

print(hex(pixels[0])[2:].zfill(3))

#output to bin file
with open(sys.argv[2], "wb") as f:
    for pixel in pixels:
        f.write(hex(pixel)[2:].zfill(3).encode('utf-8'))
        f.write(b"\n")