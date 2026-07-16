import argparse
from pathlib import Path

from PIL import Image


CELL = 512


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("sheet", type=Path)
    parser.add_argument("target", type=int, help="One-based target frame")
    parser.add_argument("source", type=int, help="One-based source frame")
    args = parser.parse_args()

    sheet = Image.open(args.sheet).convert("RGBA")

    def box(index: int) -> tuple[int, int, int, int]:
        row, column = divmod(index - 1, 3)
        return column * CELL, row * CELL, (column + 1) * CELL, (row + 1) * CELL

    replacement = sheet.crop(box(args.source))
    target_box = box(args.target)
    sheet.paste(Image.new("RGBA", (CELL, CELL)), target_box[:2])
    sheet.alpha_composite(replacement, target_box[:2])
    sheet.save(args.sheet)


if __name__ == "__main__":
    main()
