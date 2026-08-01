import argparse
from pathlib import Path

from PIL import Image


def main() -> None:
    parser = argparse.ArgumentParser(description="Convert a regular sprite sheet into a GIF preview.")
    parser.add_argument("sheet", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--columns", type=int, default=3)
    parser.add_argument("--rows", type=int, default=2)
    parser.add_argument("--duration", type=int, default=180, help="Frame duration in milliseconds")
    parser.add_argument(
        "--background",
        help="Optional opaque preview background as RRGGBB (keeps transparent sprites readable).",
    )
    parser.add_argument(
        "--once",
        action="store_true",
        help="Play once and hold the final frame instead of looping (for one-shot feeding previews)",
    )
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
            if args.background:
                if len(args.background) != 6:
                    raise ValueError("--background must be a six-digit RRGGBB color")
                color = tuple(
                    int(args.background[offset : offset + 2], 16)
                    for offset in (0, 2, 4)
                )
                backdrop = Image.new("RGBA", frame.size, (*color, 255))
                backdrop.alpha_composite(frame)
                frame = backdrop.convert("RGB")
            frame.info.pop("loop", None)
            frames.append(frame)

    args.output.parent.mkdir(parents=True, exist_ok=True)
    save_options = {
        "save_all": True,
        "append_images": frames[1:],
        "duration": args.duration,
        "disposal": 2,
    }
    if not args.background:
        save_options["transparency"] = 0
    if not args.once:
        save_options["loop"] = 0
    frames[0].save(args.output, **save_options)


if __name__ == "__main__":
    main()
