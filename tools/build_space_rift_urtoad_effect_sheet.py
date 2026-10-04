#!/usr/bin/env python3
"""Build a locked-anchor Raumriss-Urkroete sheet with cosmic shimmer."""

from __future__ import annotations

import argparse
import math
import random
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageFilter


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--cell-size", type=int, default=512)
    parser.add_argument("--padding", type=int, default=64)
    parser.add_argument("--ground", type=int, default=448)
    parser.add_argument("--seed", type=int, default=83117)
    parser.add_argument("--frames", type=int, default=12)
    parser.add_argument("--columns", type=int, default=4)
    parser.add_argument("--breathing", action="store_true")
    parser.add_argument("--effect-strength", type=float, default=1.0)
    return parser.parse_args()


def alpha_bounds(image: Image.Image) -> tuple[int, int, int, int]:
    # Ignore the faint soft-matte haze left by chroma removal at the canvas edge.
    bounds = image.getchannel("A").point(lambda alpha: 255 if alpha >= 24 else 0).getbbox()
    if bounds is None:
        raise ValueError("Source has no visible pixels")
    return bounds


def normalize_pose(source: Image.Image, cell: int, padding: int, ground: int) -> Image.Image:
    pose = source.crop(alpha_bounds(source))
    cleaned_alpha = pose.getchannel("A").point(lambda alpha: alpha if alpha >= 24 else 0)
    pose.putalpha(cleaned_alpha)
    max_width = cell - 2 * padding
    max_height = ground - padding
    scale = min(max_width / pose.width, max_height / pose.height)
    pose = pose.resize(
        (max(1, round(pose.width * scale)), max(1, round(pose.height * scale))),
        Image.Resampling.LANCZOS,
    )
    frame = Image.new("RGBA", (cell, cell))
    frame.alpha_composite(pose, ((cell - pose.width) // 2, ground - pose.height))
    return frame


def cosmic_mask(frame: Image.Image) -> Image.Image:
    """Select blue/violet rifts, horns, wing interiors and portal without gray skin."""
    pixels = frame.load()
    mask = Image.new("L", frame.size)
    out = mask.load()
    bounds = frame.getchannel("A").getbbox()
    if bounds is None:
        return mask
    left, top, right, bottom = bounds
    for y in range(top, bottom):
        for x in range(left, right):
            r, g, b, a = pixels[x, y]
            if a < 64:
                continue
            blue_rift = b >= 82 and b >= g + 18 and (b >= r - 24 or r >= g + 30)
            violet_rift = r >= 75 and b >= 95 and r >= g + 20 and b >= g + 30
            if blue_rift or violet_rift:
                out[x, y] = a
    return mask.filter(ImageFilter.GaussianBlur(0.7))


def screen_overlay(base: Image.Image, overlay: Image.Image) -> Image.Image:
    screened = ImageChops.screen(base.convert("RGB"), overlay.convert("RGB")).convert("RGBA")
    screened.putalpha(base.getchannel("A"))
    return Image.composite(screened, base, overlay.getchannel("A"))


def shimmer_frame(
    frame: Image.Image,
    index: int,
    frame_count: int,
    seed: int,
    strength: float,
) -> Image.Image:
    mask = cosmic_mask(frame)
    bounds = mask.getbbox()
    if bounds is None:
        return frame
    left, top, right, bottom = bounds
    width = right - left
    height = bottom - top
    phase = index / frame_count

    sheen = Image.new("L", frame.size)
    sheen_pixels = sheen.load()
    for y in range(top, bottom):
        for x in range(left, right):
            position = ((x - left) + (y - top) * 0.31) / (width + height * 0.31)
            distance = abs((position - phase + 0.5) % 1.0 - 0.5)
            value = int(150 * strength * math.exp(-((distance / 0.075) ** 2)))
            if value:
                sheen_pixels[x, y] = min(255, value)
    sheen = ImageChops.multiply(sheen, mask).filter(ImageFilter.GaussianBlur(1.8))
    overlay = Image.new("RGBA", frame.size)
    sheen_layer = Image.new("RGBA", frame.size, (205, 238, 255, 0))
    sheen_layer.putalpha(sheen)
    overlay.alpha_composite(sheen_layer)

    candidates = [
        (x, y)
        for y in range(top, bottom, 2)
        for x in range(left, right, 2)
        if mask.getpixel((x, y)) > 155
    ]
    rng = random.Random(seed)
    points = rng.sample(candidates, min(36, len(candidates))) if candidates else []
    draw = ImageDraw.Draw(overlay)
    for sparkle_index, (x, y) in enumerate(points):
        sparkle_phase = (sparkle_index * 7) % frame_count
        distance = abs((index - sparkle_phase + frame_count // 2) % frame_count - frame_count // 2)
        if distance > 2:
            continue
        pulse = 1.0 - distance / 3.0
        radius = (1.4 + 3.7 * pulse) * strength
        alpha = int(105 + 150 * pulse)
        draw.line((x - radius, y, x + radius, y), fill=(255, 255, 255, alpha), width=1)
        draw.line((x, y - radius, x, y + radius), fill=(255, 255, 255, alpha), width=1)
        if distance == 0:
            draw.line((x - 2, y - 2, x + 2, y + 2), fill=(215, 240, 255, 190), width=1)
            draw.line((x + 2, y - 2, x - 2, y + 2), fill=(255, 205, 255, 190), width=1)

    overlay.putalpha(ImageChops.multiply(overlay.getchannel("A"), mask))
    result = screen_overlay(frame, overlay)
    result.putalpha(frame.getchannel("A").point(lambda alpha: alpha if alpha >= 24 else 0))
    return result


def animated_frame(normalized: Image.Image, index: int, count: int, breathing: bool) -> Image.Image:
    bounds = alpha_bounds(normalized)
    pose = normalized.crop(bounds)
    breath = (1.0 - math.cos(2.0 * math.pi * index / count)) / 2.0
    vertical_scale = 1.0 + 0.004 * breath if breathing else 1.0
    horizontal_scale = 1.0 - 0.0015 * breath if breathing else 1.0
    pose = pose.resize(
        (max(1, round(pose.width * horizontal_scale)), max(1, round(pose.height * vertical_scale))),
        Image.Resampling.LANCZOS,
    )
    frame = Image.new("RGBA", normalized.size)
    frame.alpha_composite(pose, ((normalized.width - pose.width) // 2, 448 - pose.height))
    return frame


def clear_transparent_rgb(image: Image.Image) -> Image.Image:
    """Keep fully transparent WebP pixels black to prevent scaled-edge color streaks."""
    red, green, blue, alpha = image.split()
    visible = alpha.point(lambda value: 255 if value else 0)
    zero = Image.new("L", image.size)
    return Image.merge(
        "RGBA",
        tuple(Image.composite(channel, zero, visible) for channel in (red, green, blue)) + (alpha,),
    )


def main() -> None:
    args = parse_args()
    if args.frames < 2 or args.columns < 1:
        raise ValueError("frames must be >= 2 and columns must be >= 1")
    normalized = normalize_pose(
        Image.open(args.source).convert("RGBA"), args.cell_size, args.padding, args.ground
    )
    rows = math.ceil(args.frames / args.columns)
    sheet = Image.new("RGBA", (args.cell_size * args.columns, args.cell_size * rows))
    for index in range(args.frames):
        frame = animated_frame(normalized, index, args.frames, args.breathing)
        frame = shimmer_frame(frame, index, args.frames, args.seed, args.effect_strength)
        row, column = divmod(index, args.columns)
        sheet.alpha_composite(frame, (column * args.cell_size, row * args.cell_size))
    args.output.parent.mkdir(parents=True, exist_ok=True)
    clear_transparent_rgb(sheet).save(args.output, "WEBP", lossless=True, quality=100, method=6)


if __name__ == "__main__":
    main()
