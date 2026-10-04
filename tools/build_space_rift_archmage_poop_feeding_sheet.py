#!/usr/bin/env python3
"""Build a locked 12-frame Space-Rift Archmage one-shot feeding sequence."""

from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

from build_space_rift_urtoad_effect_sheet import (
    clear_transparent_rgb,
    normalize_pose,
    shimmer_frame,
)


PROGRESSIVE_BITE_KEYFRAMES = (0, 0, 1, 2, 2, 3, 4, 4, 5, 5, 5, 5)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("full_open_pose", type=Path)
    parser.add_argument("bite_one_closed_pose", type=Path)
    parser.add_argument("bite_two_open_pose", type=Path)
    parser.add_argument("bite_two_closed_pose", type=Path)
    parser.add_argument("bite_three_open_pose", type=Path)
    parser.add_argument("final_empty_closed_pose", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--cell-size", type=int, default=512)
    parser.add_argument("--padding", type=int, default=64)
    parser.add_argument("--ground", type=int, default=448)
    parser.add_argument("--seed", type=int, default=99217)
    return parser.parse_args()


def eating_mask(size: tuple[int, int]) -> Image.Image:
    """Replace the coherent face, jaw, food and supporting hand; lock everything else."""
    mask = Image.new("L", size)
    draw = ImageDraw.Draw(mask)
    draw.ellipse((165, 155, 472, 355), fill=255)
    draw.rounded_rectangle((300, 205, 475, 390), radius=42, fill=255)
    return mask.filter(ImageFilter.GaussianBlur(4.0))


def composite_bite(open_pose: Image.Image, bite_pose: Image.Image) -> Image.Image:
    if bite_pose is open_pose:
        return open_pose.copy()
    return Image.composite(bite_pose, open_pose, eating_mask(open_pose.size))


def main() -> None:
    args = parse_args()
    pose_paths = (
        args.full_open_pose,
        args.bite_one_closed_pose,
        args.bite_two_open_pose,
        args.bite_two_closed_pose,
        args.bite_three_open_pose,
        args.final_empty_closed_pose,
    )
    poses = tuple(
        normalize_pose(Image.open(path).convert("RGBA"), args.cell_size, args.padding, args.ground)
        for path in pose_paths
    )
    sheet = Image.new("RGBA", (args.cell_size * 4, args.cell_size * 3))
    for index, keyframe in enumerate(PROGRESSIVE_BITE_KEYFRAMES):
        frame = composite_bite(poses[0], poses[keyframe])
        frame = shimmer_frame(frame, index, len(PROGRESSIVE_BITE_KEYFRAMES), args.seed, 1.0)
        row, column = divmod(index, 4)
        sheet.alpha_composite(frame, (column * args.cell_size, row * args.cell_size))
    args.output.parent.mkdir(parents=True, exist_ok=True)
    clear_transparent_rgb(sheet).save(args.output, "WEBP", lossless=True, quality=100, method=6)


if __name__ == "__main__":
    main()
