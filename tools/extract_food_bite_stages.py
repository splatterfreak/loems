#!/usr/bin/env python3
"""Extract equally scaled, progressively bitten food stages from a chroma-key strip."""

from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image, ImageChops, ImageFilter


def visible_column_groups(image: Image.Image, threshold: int = 32) -> list[tuple[int, int]]:
    alpha = image.getchannel("A").point(lambda value: 255 if value > threshold else 0)
    occupied = [alpha.crop((x, 0, x + 1, image.height)).getbbox() is not None for x in range(image.width)]
    groups: list[tuple[int, int]] = []
    start: int | None = None
    for x, visible in enumerate(occupied + [False]):
        if visible and start is None:
            start = x
        elif not visible and start is not None:
            if x - start >= 8:
                groups.append((start, x))
            start = None
    return groups


def clear_transparent_rgb(image: Image.Image) -> Image.Image:
    clean = Image.new("RGBA", image.size)
    clean.alpha_composite(image)
    return clean


def extract_right_side_bone(image: Image.Image) -> Image.Image:
    """Keep the cream bone from the first ham stage at its original pixel scale."""
    pixels = image.load()
    mask = Image.new("L", image.size)
    target = mask.load()
    right_limit = round(image.width * 0.58)
    for y in range(image.height):
        for x in range(right_limit, image.width):
            red, green, blue, alpha = pixels[x, y]
            is_bone = red >= 150 and green >= 110 and blue >= 65 and green >= blue * 0.92
            if alpha > 24 and is_bone:
                target[x, y] = 255

    # Restore the dark antialiased outline around the selected cream bone while
    # excluding the red meat that touches its left edge.
    mask = mask.filter(ImageFilter.MaxFilter(7))
    mask_pixels = mask.load()
    for y in range(image.height):
        for x in range(image.width):
            red, green, blue, _ = pixels[x, y]
            is_red_meat = red > green * 1.35 and red > blue * 1.6 and red >= 90
            if is_red_meat:
                mask_pixels[x, y] = 0
    source_alpha = image.getchannel("A")
    mask = ImageChops.multiply(mask, source_alpha)
    bbox = mask.getbbox()
    if bbox is None:
        raise ValueError("Could not isolate the right-side ham bone")
    layer = Image.new("RGBA", image.size)
    layer.paste(image, mask=mask)
    return clear_transparent_rgb(layer.crop(bbox))


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path)
    parser.add_argument("output_prefix", type=Path)
    parser.add_argument("--target-first-width", type=int, required=True)
    parser.add_argument(
        "--final-bone-from-first",
        action="store_true",
        help="Replace stage five with the unchanged right-side bone from stage one.",
    )
    args = parser.parse_args()

    strip = Image.open(args.source).convert("RGBA")
    groups = visible_column_groups(strip)
    if len(groups) != 5:
        raise ValueError(f"Expected five separated food stages, found {len(groups)}")

    crops: list[Image.Image] = []
    for left, right in groups:
        candidate = strip.crop((left, 0, right, strip.height))
        bbox = candidate.getchannel("A").getbbox()
        if bbox is None:
            raise ValueError("Empty food stage")
        crops.append(candidate.crop(bbox))

    scale = args.target_first_width / crops[0].width
    args.output_prefix.parent.mkdir(parents=True, exist_ok=True)
    resized_crops: list[Image.Image] = []
    for crop in crops:
        resized = crop.resize(
            (max(1, round(crop.width * scale)), max(1, round(crop.height * scale))),
            Image.Resampling.LANCZOS,
        )
        resized_crops.append(resized)

    if args.final_bone_from_first:
        resized_crops[-1] = extract_right_side_bone(resized_crops[0])

    for index, resized in enumerate(resized_crops):
        output = args.output_prefix.with_name(f"{args.output_prefix.name}_{index}.webp")
        clear_transparent_rgb(resized).save(output, "WEBP", lossless=True, quality=100, method=6)
        print(f"{output}: {resized.size}")


if __name__ == "__main__":
    main()
