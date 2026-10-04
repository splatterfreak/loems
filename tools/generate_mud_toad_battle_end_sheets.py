from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageChops


CELL = 512
DRAWABLE = Path("app/src/main/res/drawable-nodpi")
INTERMEDIATE = Path("art/intermediate")


def clear_transparent_rgb(image: Image.Image) -> Image.Image:
    rgba = image.convert("RGBA")
    rgba.putdata(
        [
            (0, 0, 0, 0) if alpha == 0 else (red, green, blue, alpha)
            for red, green, blue, alpha in rgba.get_flattened_data()
        ]
    )
    return rgba


def prepare_canonical(path: Path, maximum_size: tuple[int, int]) -> Image.Image:
    source = Image.open(path).convert("RGBA")
    alpha = source.getchannel("A").point(lambda value: 0 if value <= 16 else value)
    source.putalpha(alpha)
    bbox = alpha.getbbox()
    if bbox is None:
        raise ValueError(f"Canonical pose contains no visible pixels: {path}")
    pose = source.crop(bbox)
    scale = min(maximum_size[0] / pose.width, maximum_size[1] / pose.height)
    size = (round(pose.width * scale), round(pose.height * scale))
    return clear_transparent_rgb(pose.resize(size, Image.Resampling.LANCZOS))


def positioned_frames(
    pose: Image.Image,
    offsets: tuple[tuple[int, int], ...],
    ground_y: int = 447,
) -> list[Image.Image]:
    frames: list[Image.Image] = []
    for offset_x, offset_y in offsets:
        frame = Image.new("RGBA", (CELL, CELL))
        x = (CELL - pose.width) // 2 + offset_x
        y = ground_y - pose.height + 1 + offset_y
        frame.alpha_composite(pose, (x, y))
        frames.append(clear_transparent_rgb(frame))
    return frames


def write_sheet(frames: list[Image.Image], output: Path) -> None:
    if len(frames) != 6:
        raise ValueError("Battle end sheets require exactly six frames")
    sheet = Image.new("RGBA", (CELL * 3, CELL * 2))
    for index, frame in enumerate(frames):
        row, column = divmod(index, 3)
        sheet.alpha_composite(frame, (column * CELL, row * CELL))
    sheet = clear_transparent_rgb(sheet)
    output.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(output, "WEBP", lossless=True, quality=100, method=6, exact=True)
    decoded = Image.open(output).convert("RGBA")
    if ImageChops.difference(sheet, decoded).getbbox() is not None:
        raise ValueError(f"Lossless WebP round-trip changed pixels: {output}")


def build() -> None:
    victory = prepare_canonical(
        INTERMEDIATE / "loem_mud_toad_victory_canonical.png",
        maximum_size=(390, 270),
    )
    defeat = prepare_canonical(
        INTERMEDIATE / "loem_mud_toad_defeat_canonical.png",
        maximum_size=(400, 190),
    )

    # Victory rises into a joyful hop and lands in the approved hero pose.
    victory_frames = positioned_frames(
        victory,
        offsets=((0, 0), (0, -5), (0, -11), (0, -16), (0, -7), (0, 0)),
    )
    # Defeat drops and settles without adding wounds, symbols, or detached effects.
    defeat_frames = positioned_frames(
        defeat,
        offsets=((0, -14), (-2, -6), (2, 0), (-1, -3), (0, 0), (0, 0)),
    )

    write_sheet(victory_frames, DRAWABLE / "loem_mud_toad_battle_victory_sheet.webp")
    write_sheet(defeat_frames, DRAWABLE / "loem_mud_toad_battle_defeat_sheet.webp")


if __name__ == "__main__":
    build()
