import argparse
from collections import deque
from pathlib import Path

from PIL import Image, ImageFilter


CELL = 512


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("sheet", type=Path)
    args = parser.parse_args()
    sheet = Image.open(args.sheet).convert("RGBA")
    result = Image.new("RGBA", sheet.size)
    for index in range(6):
        row, column = divmod(index, 3)
        cell = sheet.crop((column * CELL, row * CELL, (column + 1) * CELL, (row + 1) * CELL))
        alpha = cell.getchannel("A")
        visible = alpha.point(lambda value: 255 if value > 32 else 0)
        pixels = visible.load()
        visited: set[tuple[int, int]] = set()
        components: list[list[tuple[int, int]]] = []
        for y in range(CELL):
            for x in range(CELL):
                if not pixels[x, y] or (x, y) in visited:
                    continue
                queue = deque([(x, y)])
                visited.add((x, y))
                component = []
                while queue:
                    px, py = queue.popleft()
                    component.append((px, py))
                    for nx, ny in ((px - 1, py), (px + 1, py), (px, py - 1), (px, py + 1)):
                        if 0 <= nx < CELL and 0 <= ny < CELL and pixels[nx, ny] and (nx, ny) not in visited:
                            visited.add((nx, ny))
                            queue.append((nx, ny))
                components.append(component)
        largest = max(components, key=len)
        keep = Image.new("L", (CELL, CELL))
        keep_pixels = keep.load()
        for x, y in largest:
            keep_pixels[x, y] = 255
        keep = keep.filter(ImageFilter.MaxFilter(3))
        cleaned_alpha = Image.composite(alpha, Image.new("L", (CELL, CELL)), keep)
        cell.putalpha(cleaned_alpha)
        result.alpha_composite(cell, (column * CELL, row * CELL))
    result.save(args.sheet)


if __name__ == "__main__":
    main()
