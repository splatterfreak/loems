import argparse
from pathlib import Path

from PIL import Image


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Build a stable 3x2 idle sheet from one canonical transparent pose."
    )
    parser.add_argument("source", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--source-columns", type=int, default=1)
    parser.add_argument("--source-rows", type=int, default=1)
    parser.add_argument("--source-frame", type=int, default=0)
    parser.add_argument("--cell-size", type=int, default=512)
    parser.add_argument("--padding", type=int, default=48)
    parser.add_argument("--ground", type=int, default=448)
    parser.add_argument("--alpha-threshold", type=int, default=32)
    parser.add_argument("--breathing", action="store_true")
    args = parser.parse_args()

    source = Image.open(args.source).convert("RGBA")
    if args.source_columns > 1 or args.source_rows > 1:
        source_cell_width = source.width // args.source_columns
        source_cell_height = source.height // args.source_rows
        source_row, source_column = divmod(args.source_frame, args.source_columns)
        if source_row >= args.source_rows:
            raise ValueError("Source frame is outside the source grid")
        source = source.crop(
            (
                source_column * source_cell_width,
                source_row * source_cell_height,
                (source_column + 1) * source_cell_width,
                (source_row + 1) * source_cell_height,
            )
        )
    alpha = source.getchannel("A").point(
        lambda value: 255 if value > args.alpha_threshold else 0
    )
    bbox = alpha.getbbox()
    if bbox is None:
        raise ValueError("Canonical pose has no visible pixels")

    pose = source.crop(bbox)
    available_width = args.cell_size - 2 * args.padding
    available_height = args.ground - args.padding
    scale = min(available_width / pose.width, available_height / pose.height)
    target_size = (
        max(1, round(pose.width * scale)),
        max(1, round(pose.height * scale)),
    )
    pose = pose.resize(target_size, Image.Resampling.LANCZOS)

    sheet = Image.new("RGBA", (args.cell_size * 3, args.cell_size * 2))
    breathing_scales = [1.0, 1.008, 1.015, 1.008, 1.0, 0.998]
    width_scales = [1.0, 0.997, 0.994, 0.997, 1.0, 1.002]
    for index in range(6):
        vertical_scale = breathing_scales[index] if args.breathing else 1.0
        horizontal_scale = width_scales[index] if args.breathing else 1.0
        animated_pose = pose.resize(
            (
                max(1, round(pose.width * horizontal_scale)),
                max(1, round(pose.height * vertical_scale)),
            ),
            Image.Resampling.LANCZOS,
        )
        frame = Image.new("RGBA", (args.cell_size, args.cell_size))
        left = (args.cell_size - animated_pose.width) // 2
        top = args.ground - animated_pose.height
        frame.alpha_composite(animated_pose, (left, top))
        row, column = divmod(index, 3)
        sheet.alpha_composite(frame, (column * args.cell_size, row * args.cell_size))

    args.output.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(args.output)


if __name__ == "__main__":
    main()
