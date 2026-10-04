#!/usr/bin/env python3
"""Build a locked-anchor Ultra Loem sheet with wing-only cosmic shimmer."""

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
    parser.add_argument("--seed", type=int, default=73035)
    parser.add_argument("--frames", type=int, default=12)
    parser.add_argument("--columns", type=int, default=4)
    parser.add_argument("--breathing", action="store_true")
    parser.add_argument("--effect-strength", type=float, default=1.0)
    return parser.parse_args()


def alpha_bounds(image: Image.Image) -> tuple[int, int, int, int]:
    bounds = image.getchannel("A").getbbox()
    if bounds is None:
        raise ValueError("Source has no visible pixels")
    return bounds


def normalize_pose(source: Image.Image, cell: int, padding: int, ground: int) -> Image.Image:
    pose = source.crop(alpha_bounds(source))
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


def wing_mask(frame: Image.Image) -> Image.Image:
    alpha = frame.getchannel("A")
    bounds = alpha.getbbox()
    if bounds is None:
        return Image.new("L", frame.size)
    left, top, right, bottom = bounds
    width = right - left
    height = bottom - top
    center_x = (left + right) / 2

    side_regions = Image.new("L", frame.size)
    region_draw = ImageDraw.Draw(side_regions)
    upper = top + int(height * 0.20)
    lower = top + int(height * 0.76)
    region_draw.polygon(
        [
            (left, upper),
            (int(center_x - width * 0.10), upper),
            (int(center_x - width * 0.16), lower),
            (left, lower),
        ],
        fill=255,
    )
    region_draw.polygon(
        [
            (int(center_x + width * 0.10), upper),
            (right, upper),
            (right, lower),
            (int(center_x + width * 0.16), lower),
        ],
        fill=255,
    )

    pixels = frame.load()
    color_mask = Image.new("L", frame.size)
    color_pixels = color_mask.load()
    for y in range(top, bottom):
        for x in range(left, right):
            r, g, b, a = pixels[x, y]
            if a < 24:
                continue
            maximum = max(r, g, b)
            minimum = min(r, g, b)
            saturation = maximum - minimum
            cosmic = b >= r - 16 and b >= g - 10 and (saturation >= 18 or maximum >= 215)
            if cosmic:
                color_pixels[x, y] = a
    return ImageChops.multiply(ImageChops.multiply(side_regions, color_mask), alpha)


def screen_overlay(base: Image.Image, overlay: Image.Image) -> Image.Image:
    base_rgb = base.convert("RGB")
    overlay_rgb = overlay.convert("RGB")
    screened = ImageChops.screen(base_rgb, overlay_rgb).convert("RGBA")
    screened.putalpha(base.getchannel("A"))
    return Image.composite(screened, base, overlay.getchannel("A"))


def shimmer_frame(
    frame: Image.Image,
    index: int,
    frame_count: int,
    seed: int,
    strength: float,
) -> Image.Image:
    mask = wing_mask(frame)
    bounds = mask.getbbox()
    if bounds is None:
        return frame
    left, top, right, bottom = bounds
    width = right - left
    height = bottom - top

    overlay = Image.new("RGBA", frame.size)
    sheen = Image.new("L", frame.size)
    sheen_pixels = sheen.load()
    phase = index / frame_count
    band_width = 0.085
    for y in range(top, bottom):
        for x in range(left, right):
            diagonal_position = ((x - left) + (y - top) * 0.38) / (width + height * 0.38)
            wrapped_distance = abs((diagonal_position - phase + 0.5) % 1.0 - 0.5)
            value = int(165 * strength * math.exp(-((wrapped_distance / band_width) ** 2)))
            if value:
                sheen_pixels[x, y] = min(255, value)
    sheen = ImageChops.multiply(sheen, mask).filter(ImageFilter.GaussianBlur(2.2))
    sheen_layer = Image.new("RGBA", frame.size, (205, 245, 255, 0))
    sheen_layer.putalpha(sheen)
    overlay.alpha_composite(sheen_layer)

    candidates = [
        (x, y)
        for y in range(top, bottom, 2)
        for x in range(left, right, 2)
        if mask.getpixel((x, y)) > 150
    ]
    rng = random.Random(seed)
    sparkle_points = rng.sample(candidates, min(24, len(candidates))) if candidates else []
    draw = ImageDraw.Draw(overlay)
    for sparkle_index, (x, y) in enumerate(sparkle_points):
        sparkle_phase = (sparkle_index * 5) % frame_count
        phase_distance = abs(
            (index - sparkle_phase + frame_count // 2) % frame_count - frame_count // 2
        )
        if phase_distance > 2:
            continue
        pulse = 1.0 - phase_distance / 3.0
        radius = (1.5 + 3.5 * pulse) * strength
        alpha_value = int(110 + 145 * pulse)
        draw.line((x - radius, y, x + radius, y), fill=(255, 255, 255, alpha_value), width=1)
        draw.line((x, y - radius, x, y + radius), fill=(255, 255, 255, alpha_value), width=1)
        if phase_distance == 0:
            draw.line(
                (x - radius * 0.55, y - radius * 0.55, x + radius * 0.55, y + radius * 0.55),
                fill=(220, 245, 255, 190),
                width=1,
            )
            draw.line(
                (x + radius * 0.55, y - radius * 0.55, x - radius * 0.55, y + radius * 0.55),
                fill=(255, 205, 255, 190),
                width=1,
            )

    overlay_alpha = ImageChops.multiply(overlay.getchannel("A"), mask)
    overlay.putalpha(overlay_alpha)
    return screen_overlay(frame, overlay)


def main() -> None:
    args = parse_args()
    source = Image.open(args.source).convert("RGBA")
    normalized = normalize_pose(source, args.cell_size, args.padding, args.ground)
    if args.frames < 2 or args.columns < 1:
        raise ValueError("frames must be >= 2 and columns must be >= 1")
    rows = math.ceil(args.frames / args.columns)
    sheet = Image.new("RGBA", (args.cell_size * args.columns, args.cell_size * rows))

    for index in range(args.frames):
        bounds = alpha_bounds(normalized)
        pose = normalized.crop(bounds)
        breath_phase = (1.0 - math.cos(2.0 * math.pi * index / args.frames)) / 2.0
        vertical_scale = 1.0 + 0.004 * breath_phase if args.breathing else 1.0
        horizontal_scale = 1.0 - 0.0015 * breath_phase if args.breathing else 1.0
        pose = pose.resize(
            (
                max(1, round(pose.width * horizontal_scale)),
                max(1, round(pose.height * vertical_scale)),
            ),
            Image.Resampling.LANCZOS,
        )
        frame = Image.new("RGBA", (args.cell_size, args.cell_size))
        frame.alpha_composite(pose, ((args.cell_size - pose.width) // 2, args.ground - pose.height))
        frame = shimmer_frame(frame, index, args.frames, args.seed, args.effect_strength)
        row, column = divmod(index, args.columns)
        sheet.alpha_composite(frame, (column * args.cell_size, row * args.cell_size))

    args.output.parent.mkdir(parents=True, exist_ok=True)
    if args.output.suffix.lower() == ".webp":
        sheet.save(args.output, "WEBP", lossless=True, quality=100, method=6)
    else:
        sheet.save(args.output)


if __name__ == "__main__":
    main()
