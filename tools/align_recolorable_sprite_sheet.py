import argparse
from collections import deque
from pathlib import Path

from PIL import Image


CELL = 512
ALPHA_THRESHOLD = 32


def visible_mask(image: Image.Image) -> Image.Image:
    return image.getchannel("A").point(
        lambda value: 255 if value > ALPHA_THRESHOLD else 0
    )


def remove_tiny_components(image: Image.Image, minimum_pixels: int = 64) -> Image.Image:
    mask = visible_mask(image)
    pixels = mask.load()
    visited: set[tuple[int, int]] = set()
    keep = Image.new("L", image.size)
    keep_pixels = keep.load()
    for y in range(image.height):
        for x in range(image.width):
            if not pixels[x, y] or (x, y) in visited:
                continue
            queue = deque([(x, y)])
            visited.add((x, y))
            component: list[tuple[int, int]] = []
            while queue:
                px, py = queue.popleft()
                component.append((px, py))
                for nx, ny in (
                    (px - 1, py),
                    (px + 1, py),
                    (px, py - 1),
                    (px, py + 1),
                ):
                    if (
                        0 <= nx < image.width
                        and 0 <= ny < image.height
                        and pixels[nx, ny]
                        and (nx, ny) not in visited
                    ):
                        visited.add((nx, ny))
                        queue.append((nx, ny))
            if len(component) >= minimum_pixels:
                for px, py in component:
                    keep_pixels[px, py] = 255
    cleaned = image.copy()
    cleaned.putalpha(Image.composite(image.getchannel("A"), Image.new("L", image.size), keep))
    return cleaned


def body_center_x(image: Image.Image) -> float:
    rgba = image.load()
    positions: list[int] = []
    for y in range(image.height):
        for x in range(image.width):
            red, green, blue, alpha = rgba[x, y]
            if alpha <= ALPHA_THRESHOLD:
                continue
            maximum = max(red, green, blue)
            minimum = min(red, green, blue)
            if 80 <= maximum <= 230 and maximum - minimum <= 30:
                positions.append(x)
    if not positions:
        bbox = visible_mask(image).getbbox()
        if bbox is None:
            raise ValueError("Frame has no visible pixels")
        return (bbox[0] + bbox[2]) / 2
    return sum(positions) / len(positions)


def scale_around_anchor(image: Image.Image, scale: float, anchor_x: int, ground: int) -> Image.Image:
    if scale >= 0.999:
        return image
    scaled = image.resize(
        (round(image.width * scale), round(image.height * scale)),
        Image.Resampling.LANCZOS,
    )
    result = Image.new("RGBA", image.size)
    result.alpha_composite(
        scaled,
        (round(anchor_x * (1 - scale)), round(ground * (1 - scale))),
    )
    return result


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Align generated Loems frames by recolorable body center and ground."
    )
    parser.add_argument("source", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--center-x", type=int, default=256)
    parser.add_argument("--ground", type=int, default=448)
    parser.add_argument("--padding", type=int, default=48)
    args = parser.parse_args()

    source = Image.open(args.source).convert("RGBA")
    if source.size != (CELL * 3, CELL * 2):
        raise ValueError(f"Expected 1536x1024 sheet, got {source.size}")

    frames: list[Image.Image] = []
    for index in range(6):
        row, column = divmod(index, 3)
        frame = source.crop(
            (column * CELL, row * CELL, (column + 1) * CELL, (row + 1) * CELL)
        )
        frame = remove_tiny_components(frame)
        bbox = visible_mask(frame).getbbox()
        if bbox is None:
            raise ValueError(f"Frame {index + 1} is empty")
        dx = round(args.center_x - body_center_x(frame))
        dy = args.ground - bbox[3]
        aligned = Image.new("RGBA", frame.size)
        aligned.alpha_composite(frame, (dx, dy))
        frames.append(aligned)

    maximum_left = maximum_right = maximum_top = 1
    for frame in frames:
        bbox = visible_mask(frame).getbbox()
        if bbox is None:
            continue
        maximum_left = max(maximum_left, args.center_x - bbox[0])
        maximum_right = max(maximum_right, bbox[2] - args.center_x)
        maximum_top = max(maximum_top, args.ground - bbox[1])
    scale = min(
        1.0,
        (args.center_x - args.padding) / maximum_left,
        (CELL - args.padding - args.center_x) / maximum_right,
        (args.ground - args.padding) / maximum_top,
    )
    frames = [scale_around_anchor(frame, scale, args.center_x, args.ground) for frame in frames]

    result = Image.new("RGBA", source.size)
    for index, frame in enumerate(frames):
        row, column = divmod(index, 3)
        result.alpha_composite(frame, (column * CELL, row * CELL))
    args.output.parent.mkdir(parents=True, exist_ok=True)
    result.save(args.output)


if __name__ == "__main__":
    main()
