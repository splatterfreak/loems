#!/usr/bin/env python3
"""Build a locked 12-frame Ultra one-shot feeding sequence."""

from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

from build_ultra_cosmic_effect_sheet import normalize_pose, shimmer_frame


PROGRESSIVE_BITE_KEYFRAMES = (0, 0, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("open_pose", type=Path)
    parser.add_argument("half_bite_pose", type=Path)
    parser.add_argument("closed_bite_pose", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--cell-size", type=int, default=512)
    parser.add_argument("--padding", type=int, default=64)
    parser.add_argument("--ground", type=int, default=448)
    parser.add_argument("--seed", type=int, default=73039)
    return parser.parse_args()


def mouth_mask(size: tuple[int, int]) -> Image.Image:
    """Cover the coherent head/jaw/ham interaction while leaving the body locked."""
    mask = Image.new("L", size)
    draw = ImageDraw.Draw(mask)
    draw.ellipse((204, 194, 444, 348), fill=255)
    return mask.filter(ImageFilter.GaussianBlur(5.0))


def composite_bite(open_pose: Image.Image, bite_pose: Image.Image) -> Image.Image:
    if bite_pose is open_pose:
        return open_pose.copy()
    return Image.composite(bite_pose, open_pose, mouth_mask(open_pose.size))


def main() -> None:
    args = parse_args()
    open_pose = normalize_pose(
        Image.open(args.open_pose).convert("RGBA"),
        args.cell_size,
        args.padding,
        args.ground,
    )
    half_bite_pose = normalize_pose(
        Image.open(args.half_bite_pose).convert("RGBA"),
        args.cell_size,
        args.padding,
        args.ground,
    )
    closed_bite_pose = normalize_pose(
        Image.open(args.closed_bite_pose).convert("RGBA"),
        args.cell_size,
        args.padding,
        args.ground,
    )
    sheet = Image.new("RGBA", (args.cell_size * 4, args.cell_size * 3))

    bite_poses = (open_pose, half_bite_pose, closed_bite_pose)
    for index, bite_keyframe in enumerate(PROGRESSIVE_BITE_KEYFRAMES):
        frame = composite_bite(open_pose, bite_poses[bite_keyframe])
        frame = shimmer_frame(frame, index, len(PROGRESSIVE_BITE_KEYFRAMES), args.seed, 1.0)
        row, column = divmod(index, 4)
        sheet.alpha_composite(frame, (column * args.cell_size, row * args.cell_size))

    args.output.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(args.output, "WEBP", lossless=True, quality=100, method=6)


if __name__ == "__main__":
    main()
