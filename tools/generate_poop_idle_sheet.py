import argparse
from pathlib import Path

from PIL import Image


SWAY_PIXELS = (-3, -1, 1, 3, 1, -1)


def shifted(image: Image.Image, amount: int) -> Image.Image:
    result = Image.new("RGBA", image.size)
    result.alpha_composite(image, (amount, 0))
    return result


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Build the lazy Haufen-Loem idle with a controlled lower-body sway."
    )
    parser.add_argument("source", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--cell-size", type=int, default=512)
    parser.add_argument("--padding", type=int, default=56)
    parser.add_argument("--ground", type=int, default=448)
    args = parser.parse_args()

    source = Image.open(args.source).convert("RGBA")
    alpha = source.getchannel("A").point(lambda value: 255 if value > 32 else 0)
    bbox = alpha.getbbox()
    if bbox is None:
        raise ValueError("Canonical pose has no visible pixels")

    pose = source.crop(bbox)
    available_width = args.cell_size - 2 * (args.padding + max(abs(x) for x in SWAY_PIXELS))
    available_height = args.ground - args.padding
    scale = min(available_width / pose.width, available_height / pose.height)
    pose = pose.resize(
        (max(1, round(pose.width * scale)), max(1, round(pose.height * scale))),
        Image.Resampling.LANCZOS,
    )

    base = Image.new("RGBA", (args.cell_size, args.cell_size))
    left = (args.cell_size - pose.width) // 2
    top = args.ground - pose.height
    base.alpha_composite(pose, (left, top))

    # The upper pile and sad face stay locked. Only the heavy lower mound slowly
    # sags sideways, with a soft blend so the silhouette never tears at the seam.
    split_y = top + round(pose.height * 0.72)
    blend_height = max(18, round(pose.height * 0.08))
    mask = Image.new("L", base.size, 0)
    mask_pixels = mask.load()
    for y in range(split_y, args.ground + 1):
        strength = min(255, round((y - split_y) * 255 / blend_height))
        for x in range(args.cell_size):
            mask_pixels[x, y] = strength

    sheet = Image.new("RGBA", (args.cell_size * 3, args.cell_size * 2))
    for index, amount in enumerate(SWAY_PIXELS):
        frame = Image.composite(shifted(base, amount), base, mask)
        row, column = divmod(index, 3)
        sheet.alpha_composite(frame, (column * args.cell_size, row * args.cell_size))

    args.output.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(args.output)


if __name__ == "__main__":
    main()
