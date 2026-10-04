#!/usr/bin/env python3
"""Align an edited canonical Loem and export only its closed-mouth region."""

from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageFilter


def subject_mask(image: Image.Image) -> Image.Image:
    rgba = image.convert("RGBA")
    alpha = rgba.getchannel("A")
    if alpha.getextrema()[0] < 255:
        return alpha.point(lambda value: 255 if value > 16 else 0)
    gray = image.convert("L")
    # ImageGen's preview checkerboard is brighter than the complete Loem art.
    return gray.point(lambda value: 255 if value < 225 else 0)


def subject_bbox(image: Image.Image) -> tuple[int, int, int, int]:
    bbox = subject_mask(image).getbbox()
    if bbox is None:
        raise ValueError("Generated edit has no detectable subject")
    return bbox


def parse_box(value: str) -> tuple[int, int, int, int]:
    values = tuple(int(item) for item in value.split(","))
    if len(values) != 4:
        raise argparse.ArgumentTypeError("Expected left,top,right,bottom")
    return values


def parse_polygon(value: str) -> tuple[tuple[int, int], ...]:
    points = []
    for pair in value.split(";"):
        x, y = pair.split(",")
        points.append((int(x), int(y)))
    if len(points) < 3:
        raise argparse.ArgumentTypeError("Expected at least three x,y points")
    return tuple(points)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path)
    parser.add_argument("generated", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--source-mouth", required=True, type=parse_polygon)
    parser.add_argument("--target-body", required=True, type=parse_box)
    args = parser.parse_args()

    source = Image.open(args.source).convert("RGBA")
    generated = Image.open(args.generated).convert("RGBA")
    source_bbox = source.getchannel("A").getbbox()
    if source_bbox is None:
        raise ValueError("Canonical source has no visible pixels")
    generated_bbox = subject_bbox(generated)

    aligned = Image.new("RGBA", source.size)
    generated_subject = generated.crop(generated_bbox).resize(
        (source_bbox[2] - source_bbox[0], source_bbox[3] - source_bbox[1]),
        Image.Resampling.LANCZOS,
    )
    aligned.alpha_composite(generated_subject, source_bbox[:2])
    aligned_subject_mask = Image.new("L", source.size)
    aligned_subject_mask.paste(
        subject_mask(generated).crop(generated_bbox).resize(
            (source_bbox[2] - source_bbox[0], source_bbox[3] - source_bbox[1]),
            Image.Resampling.LANCZOS,
        ),
        source_bbox[:2],
    )

    mouth_mask = Image.new("L", source.size)
    ImageDraw.Draw(mouth_mask).polygon(args.source_mouth, fill=255)
    mouth_mask = mouth_mask.filter(ImageFilter.GaussianBlur(1.2))
    edited = Image.composite(aligned, source, mouth_mask)
    edited.putalpha(ImageChops.multiply(source.getchannel("A"), aligned_subject_mask))

    target_left, target_top, target_right, target_bottom = args.target_body
    source_left, source_top, source_right, source_bottom = source_bbox
    scale_x = (target_right - target_left) / (source_right - source_left)
    scale_y = (target_bottom - target_top) / (source_bottom - source_top)
    resized = edited.crop(source_bbox).resize(
        (target_right - target_left, target_bottom - target_top),
        Image.Resampling.LANCZOS,
    )

    transformed_polygon = tuple(
        (
            round(target_left + (x - source_left) * scale_x),
            round(target_top + (y - source_top) * scale_y),
        )
        for x, y in args.source_mouth
    )
    target_mask = Image.new("L", (512, 512))
    ImageDraw.Draw(target_mask).polygon(transformed_polygon, fill=255)
    target_mask = target_mask.filter(ImageFilter.GaussianBlur(1.0))

    layer = Image.new("RGBA", (512, 512))
    layer.alpha_composite(resized, (target_left, target_top))
    target_mask = ImageChops.multiply(target_mask, layer.getchannel("A"))
    layer.putalpha(target_mask)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    layer.save(args.output, "PNG")
    print(f"saved {args.output}; target polygon={transformed_polygon}")


if __name__ == "__main__":
    main()
