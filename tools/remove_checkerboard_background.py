import argparse
from collections import deque
from pathlib import Path

from PIL import Image


def is_background(pixel: tuple[int, int, int]) -> bool:
    # Image generators use checker previews ranging from light gray to white.
    # Flooding only from the canvas edge keeps similarly neutral body pixels safe.
    return min(pixel) >= 185 and max(pixel) - min(pixel) <= 5


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument(
        "--global-neutral-checker",
        action="store_true",
        help="Also remove enclosed neutral checker pixels left between body parts.",
    )
    parser.add_argument(
        "--keep-largest-component",
        action="store_true",
        help="Discard disconnected checker-grid remnants after background removal.",
    )
    args = parser.parse_args()

    source = Image.open(args.source).convert("RGB")
    width, height = source.size
    pixels = source.load()
    outside = bytearray(width * height)
    queue: deque[tuple[int, int]] = deque()
    for x in range(width):
        queue.append((x, 0))
        queue.append((x, height - 1))
    for y in range(height):
        queue.append((0, y))
        queue.append((width - 1, y))
    while queue:
        x, y = queue.popleft()
        offset = y * width + x
        if outside[offset] or not is_background(pixels[x, y]):
            continue
        outside[offset] = 1
        if x: queue.append((x - 1, y))
        if x + 1 < width: queue.append((x + 1, y))
        if y: queue.append((x, y - 1))
        if y + 1 < height: queue.append((x, y + 1))

    result = source.convert("RGBA")
    alpha = Image.new("L", source.size, 255)
    alpha_pixels = alpha.load()
    for y in range(height):
        for x in range(width):
            if outside[y * width + x] or (
                args.global_neutral_checker and is_background(pixels[x, y])
            ):
                alpha_pixels[x, y] = 0
    result.putalpha(alpha)
    if args.keep_largest_component:
        visible = {
            (x, y)
            for y in range(height)
            for x in range(width)
            if alpha_pixels[x, y] > 0
        }
        components: list[set[tuple[int, int]]] = []
        while visible:
            pending = [visible.pop()]
            component = {pending[0]}
            while pending:
                x, y = pending.pop()
                for neighbor in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)):
                    if neighbor in visible:
                        visible.remove(neighbor)
                        component.add(neighbor)
                        pending.append(neighbor)
            components.append(component)
        largest = max(components, key=len) if components else set()
        for y in range(height):
            for x in range(width):
                if (x, y) not in largest:
                    alpha_pixels[x, y] = 0
        result.putalpha(alpha)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    result.save(args.output)


if __name__ == "__main__":
    main()
