#!/usr/bin/env python3
"""Align a generated transparent pose to an existing 512px production frame."""

from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path)
    parser.add_argument("reference", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()

    source = Image.open(args.source).convert("RGBA")
    reference = Image.open(args.reference).convert("RGBA")
    if reference.size != (512, 512):
        raise ValueError(f"Reference must be a 512px frame: {reference.size}")
    source_bounds = source.getchannel("A").getbbox()
    reference_bounds = reference.getchannel("A").getbbox()
    if source_bounds is None or reference_bounds is None:
        raise ValueError("Source and reference both need visible alpha pixels")

    source_crop = source.crop(source_bounds).resize(
        (reference_bounds[2] - reference_bounds[0], reference_bounds[3] - reference_bounds[1]),
        Image.Resampling.LANCZOS,
    )
    output = Image.new("RGBA", reference.size)
    output.alpha_composite(source_crop, reference_bounds[:2])
    args.output.parent.mkdir(parents=True, exist_ok=True)
    output.save(args.output)


if __name__ == "__main__":
    main()
