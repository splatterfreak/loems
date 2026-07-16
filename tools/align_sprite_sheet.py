import argparse
from pathlib import Path

from PIL import Image


CELL = 512


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--center-x", type=int, default=256)
    parser.add_argument("--ground", type=int, default=448)
    args = parser.parse_args()

    source = Image.open(args.source).convert("RGBA")
    result = Image.new("RGBA", source.size)
    for index in range(6):
        row, column = divmod(index, 3)
        cell = source.crop((column * CELL, row * CELL, (column + 1) * CELL, (row + 1) * CELL))
        bbox = cell.getchannel("A").getbbox()
        if bbox is None:
            raise ValueError(f"Frame {index + 1} is empty")
        left, top, right, bottom = bbox
        visible = cell.crop(bbox)
        if visible.width > 416 or visible.height > 400:
            scale = min(416 / visible.width, 400 / visible.height)
            visible = visible.resize(
                (round(visible.width * scale), round(visible.height * scale)),
                Image.Resampling.LANCZOS,
            )
            cell = Image.new("RGBA", (CELL, CELL))
            cell.alpha_composite(visible, (left, top))
            bbox = cell.getchannel("A").getbbox()
            if bbox is None:
                raise ValueError(f"Frame {index + 1} became empty")
            left, top, right, bottom = bbox
        dx = round(args.center_x - (left + right) / 2)
        dy = args.ground - bottom
        aligned = Image.new("RGBA", (CELL, CELL))
        aligned.alpha_composite(cell, (dx, dy))
        result.alpha_composite(aligned, (column * CELL, row * CELL))
    args.output.parent.mkdir(parents=True, exist_ok=True)
    result.save(args.output)


if __name__ == "__main__":
    main()
