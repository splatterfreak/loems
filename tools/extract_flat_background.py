"""Conservative local extraction for approved generated cutouts with flat/checker backdrops."""
from collections import deque
from pathlib import Path
import argparse
from PIL import Image

def extract(source: Path, output: Path) -> None:
    image = Image.open(source).convert('RGBA')
    pixels = image.load(); width, height = image.size
    candidates = bytearray(width * height)
    for y in range(height):
        for x in range(width):
            r,g,b,a = pixels[x,y]
            # Backgrounds are neutral light gray/white; character outlines separate them.
            candidates[y*width+x] = int(a > 0 and max(r,g,b)-min(r,g,b) <= 8 and max(r,g,b) >= 180)
    seen = bytearray(width * height); queue = deque()
    for x in range(width): queue.extend(((x,0),(x,height-1)))
    for y in range(height): queue.extend(((0,y),(width-1,y)))
    while queue:
        x,y=queue.popleft(); i=y*width+x
        if x<0 or y<0 or x>=width or y>=height or seen[i] or not candidates[i]: continue
        seen[i]=1
        queue.extend(((x+1,y),(x-1,y),(x,y+1),(x,y-1)))
    for y in range(height):
        for x in range(width):
            if seen[y*width+x]: pixels[x,y]=(0,0,0,0)
            elif pixels[x,y][3] == 0: pixels[x,y]=(0,0,0,0)
    image.save(output, 'PNG')

if __name__ == '__main__':
    parser=argparse.ArgumentParser(); parser.add_argument('source',type=Path); parser.add_argument('output',type=Path)
    args=parser.parse_args(); args.output.parent.mkdir(parents=True,exist_ok=True); extract(args.source,args.output)
