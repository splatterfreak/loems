import argparse
from pathlib import Path

from PIL import Image


def main() -> None:
    parser = argparse.ArgumentParser(description="Convert a regular sprite sheet into a looping GIF.")
    parser.add_argument("sheet", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--columns", type=int, default=3)
    parser.add_argument("--rows", type=int, default=2)
    parser.add_argument("--duration", type=int, default=180, help="Frame duration in milliseconds")
    args = parser.parse_args()

    sheet = Image.open(args.sheet).convert("RGBA")
    if sheet.width % args.columns or sheet.height % args.rows:
        raise ValueError("Sheet dimensions must divide evenly into the requested grid")

    cell_width = sheet.width // args.columns
    cell_height = sheet.height // args.rows
    frames = []
    for row in range(args.rows):
        for column in range(args.columns):
            left = column * cell_width
            top = row * cell_height
            frame = sheet.crop((left, top, left + cell_width, top + cell_height))
            frames.append(frame)

    args.output.parent.mkdir(parents=True, exist_ok=True)
    frames[0].save(
        args.output,
        save_all=True,
        append_images=frames[1:],
        duration=args.duration,
        loop=0,
        disposal=2,
        transparency=0,
    )


if __name__ == "__main__":
    main()
